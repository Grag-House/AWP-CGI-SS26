package hka.awp.cgi.temi.app.feature.navigation

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robotemi.sdk.Robot
import com.robotemi.sdk.SttLanguage
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.map.LOCATION
import com.robotemi.sdk.map.OnLoadMapStatusChangedListener
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener
import com.robotemi.sdk.navigation.listener.OnDistanceToLocationChangedListener
import com.robotemi.sdk.navigation.model.Position
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.feature.mqtt.MqttManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume

/** Represents a location on the map with name and X/Y coordinates from Temi map data. */
data class LocationMarker(val name: String, val x: Float, val y: Float)

/**
 * State of the currently displayed location.
 * [Resource] for known destinations with a string resource, [Custom] for unknown waypoint names.
 */
sealed class LocationState {
    data class Resource(@StringRes val resId: Int) : LocationState()
    data class Custom(val name: String) : LocationState()
}

/**
 * UI State for the Navigation screen.
 */
data class NavigationUiState(
    val currentLocation: LocationState = LocationState.Resource(R.string.location_status_locating),
    val mapLocations: List<LocationMarker> = emptyList(),
    val isMapLoading: Boolean = false,
    val hasMapError: Boolean = false,
    val robotPosition: Position? = null,
    val savedLocations: List<String> = emptyList(),
)

/**
 * ViewModel responsible for managing navigation logic and map data for the robot.
 *
 * This class tracks the robot's real-time position, determines the nearest saved location
 */
@Suppress("TooManyFunctions")
class NavigationViewModel(
    private val robot: Robot?,
    private val mqttManager: MqttManager,
    private val defaultMapName: String
) : ViewModel(),
    OnRobotReadyListener,
    OnDistanceToLocationChangedListener,
    OnGoToLocationStatusChangedListener,
    OnCurrentPositionChangedListener,
    Robot.AsrListener,
    Robot.TtsListener {

    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    private var loadingJob: Job? = null
    private var lastPublishedNavEvent: String? = null
    private var pendingTerminalPublishJob: Job? = null

    companion object {
        private const val ABORT_DEBOUNCE_MS = 1500L
    }

    init {
        robot?.addOnRobotReadyListener(this)
        robot?.addOnDistanceToLocationChangedListener(this)
        robot?.addOnCurrentPositionChangedListener(this)
        robot?.addOnGoToLocationStatusChangedListener(this)
        robot?.addAsrListener(this)
        robot?.addTtsListener(this)
        viewModelScope.launch {
            mqttManager.connect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        pendingTerminalPublishJob?.cancel()
        pendingTerminalPublishJob = null
        robot?.removeOnRobotReadyListener(this)
        robot?.removeOnDistanceToLocationChangedListener(this)
        robot?.removeOnCurrentPositionChangedListener(this)
        robot?.removeOnGoToLocationStatusChangedListener(this)
        robot?.removeAsrListener(this)
        robot?.removeTtsListener(this)
        mqttManager.disconnect()
    }

    override fun onAsrResult(asrResult: String, sttLanguage: SttLanguage) {
        robot?.finishConversation()
        Timber.d("ASR Result: %s (%s)", asrResult, sttLanguage)

        viewModelScope.launch {
            mqttManager.publishAsr(asrResult)
        }

        // Simple local NLP example: "Go to [Location]"
        val textLower = asrResult.lowercase()
        if (textLower.contains("gehe zu") || textLower.contains("go to")) {
            val location = textLower.split("gehe zu", "go to").last().trim()
            if (location.isNotEmpty()) {
                robot?.speak(TtsRequest.create(speech = "Ich fahre zu $location", isShowOnConversationLayer = false))
                goToLocation(location)
            }
        }
    }

    override fun onTtsStatusChanged(ttsRequest: TtsRequest) {
        if (ttsRequest.status == TtsRequest.Status.COMPLETED) {
            viewModelScope.launch {
                mqttManager.publishTtsStatus(status = "completed")
            }
        }
    }

    /** Loads saved locations and sets the initial robot position once the robot is ready. */
    override fun onRobotReady(isReady: Boolean) {
        if (!isReady) return
        val locs = robot?.locations ?: emptyList()
        Timber.d("Robot ready, saved locations: %s", locs)

        _uiState.update { state ->
            state.copy(
                savedLocations = locs,
                currentLocation = if (locs.isEmpty()) {
                    LocationState.Resource(R.string.location_status_none)
                } else {
                    state.currentLocation
                },
                robotPosition = robot?.getPosition() ?: state.robotPosition
            )
        }
    }

    /** Updates the robot position on every change. */
    override fun onCurrentPositionChanged(position: Position) {
        _uiState.update { it.copy(robotPosition = position) }
    }

    /** Determines the nearest location based on distances and updates the displayed state. */
    override fun onDistanceToLocationChanged(distances: Map<String, Float>) {
        val nearest = distances.minByOrNull { it.value } ?: return
        val (systemName, distance) = nearest
        val destination = DestinationItems.fromSystemName(systemName)

        val newState = destination?.let { LocationState.Resource(it.stringResource) }
            ?: LocationState.Custom(systemName)

        if (_uiState.value.currentLocation != newState) {
            _uiState.update { it.copy(currentLocation = newState) }
            Timber.v("Current location updated to: %s (distance: %s)", systemName, distance)
        }
    }

    /** Navigates the robot to the specified waypoint. */
    fun goToLocation(name: String) {
        Timber.d("Navigating to: %s", name)
        viewModelScope.launch {
            mqttManager.publishStatus(status = "going", location = name)
        }
        runCatching { robot?.goTo(name) }
            .onFailure {
                Timber.e(it, "Navigation failed to: %s", name)
                viewModelScope.launch {
                    mqttManager.publishStatus(status = "failed", location = name)
                }
            }
    }

    /** Starts loading the map and resets the dialog state. */
    fun showMap() {
        Timber.d("Showing map locations...")
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isMapLoading = true, hasMapError = false, mapLocations = emptyList())
            }

            suspend fun fetch(): List<LocationMarker>? = withContext(Dispatchers.IO) {
                runCatching {
                    val mapData = robot?.getMapData() ?: return@runCatching null
                    mapData.locations
                        .filter { it.layerCategory == LOCATION }
                        .mapNotNull { layer ->
                            val pose = layer.layerPoses?.firstOrNull() ?: return@mapNotNull null
                            LocationMarker(layer.layerId, pose.x, pose.y)
                        }
                        .ifEmpty { null }
                }.onFailure { Timber.e(it, "Error fetching map data") }.getOrNull()
            }

            val markers = fetch() ?: run {
                Timber.w("Direct marker fetch failed, attempting explicit map load...")
                if (awaitMapLoad()) fetch() else null
            }

            _uiState.update {
                it.copy(
                    isMapLoading = false,
                    mapLocations = markers ?: emptyList(),
                    hasMapError = markers == null
                )
            }
        }
    }

    /**
     * Loads the map from the robot and waits for the SDK callback.
     */
    private suspend fun awaitMapLoad(): Boolean {
        val maps = withContext(Dispatchers.IO) {
            runCatching { robot?.getMapList().orEmpty() }
                .getOrElse { emptyList() }
        }

        val target = maps.firstOrNull { it.name == defaultMapName }
            ?: maps.firstOrNull() ?: return false

        Timber.d("Loading map: %s (%s)", target.name, target.id)

        return suspendCancellableCoroutine { cont ->
            var triggeredRequestId: String? = null

            val listener = object : OnLoadMapStatusChangedListener {
                override fun onLoadMapStatusChanged(status: Int, requestId: String) {
                    if (requestId != triggeredRequestId) return

                    when (status) {
                        OnLoadMapStatusChangedListener.COMPLETE -> {
                            robot?.removeOnLoadMapStatusChangedListener(this)
                            if (cont.isActive) cont.resume(true)
                        }

                        OnLoadMapStatusChangedListener.START -> Unit
                        else -> {
                            Timber.w("Map load failed with status: %s", status)
                            robot?.removeOnLoadMapStatusChangedListener(this)
                            if (cont.isActive) cont.resume(false)
                        }
                    }
                }
            }

            robot?.addOnLoadMapStatusChangedListener(listener)
            val requestId = robot?.loadMap(target.id, withoutUI = true) ?: ""

            if (requestId.isEmpty()) {
                robot?.removeOnLoadMapStatusChangedListener(listener)
                if (cont.isActive) cont.resume(false)
            } else {
                triggeredRequestId = requestId
            }

            cont.invokeOnCancellation {
                robot?.removeOnLoadMapStatusChangedListener(listener)
            }
        }
    }

    /** Cancels ongoing load and resets map-related states. */
    fun dismissMap() {
        loadingJob?.cancel()
        loadingJob = null
        _uiState.update {
            it.copy(mapLocations = emptyList(), isMapLoading = false, hasMapError = false)
        }
    }

    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
    ) {
        val normalizedStatus = status.lowercase()
        val normalizedLocation = location.trim()

        if (normalizedStatus in setOf("start", "going", "calculating", "reposing")) {
            pendingTerminalPublishJob?.cancel()
            pendingTerminalPublishJob = null
            if (normalizedLocation.isNotEmpty()) {
                viewModelScope.launch {
                    mqttManager.publishStatus(status = normalizedStatus, location = normalizedLocation)
                }
            }
            return
        }

        if (normalizedLocation.isEmpty()) return

        fun publishIfNew(s: String, l: String) {
            val key = "$l|$s"
            if (lastPublishedNavEvent == key) return
            lastPublishedNavEvent = key
            viewModelScope.launch { mqttManager.publishStatus(status = s, location = l) }
        }

        if (normalizedStatus == "complete") {
            pendingTerminalPublishJob?.cancel()
            pendingTerminalPublishJob = null
            publishIfNew(normalizedStatus, normalizedLocation)
            return
        }

        if (normalizedStatus !in setOf("abort", "cancel", "cancelled")) return

        pendingTerminalPublishJob?.cancel()
        pendingTerminalPublishJob = viewModelScope.launch {
            delay(ABORT_DEBOUNCE_MS)
            publishIfNew(normalizedStatus, normalizedLocation)
        }
    }
}
