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
    private var navStatusCounter: Long = 0L

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
        Timber.d("ASR Result: $asrResult ($sttLanguage)")

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
        Timber.d("Robot ready, saved locations: $locs")

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
        Timber.v("Position changed: x=${position.x}, y=${position.y}")
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
            Timber.d("Current location updated to: $systemName (distance: $distance)")
        }
    }

    /** Navigates the robot to the specified waypoint. */
    fun goToLocation(name: String) {
        Timber.d("Navigating to: $name")
        viewModelScope.launch {
            Timber.d("Publish immediate nav ack: status=going location='%s'", name)
            mqttManager.publishStatus(status = "going", text = name)
        }
        runCatching { robot?.goTo(name) }
            .onFailure {
                Timber.e(it, "Navigation failed to: $name")
                viewModelScope.launch {
                    mqttManager.publishStatus(status = "failed", text = name)
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

            val markers = fetchMarkersWithFallback()

            _uiState.update {
                it.copy(
                    isMapLoading = false,
                    mapLocations = markers ?: emptyList(),
                    hasMapError = markers == null
                )
            }
        }
    }

    /** Attempts to fetch markers directly; falls back to explicit map loading if needed. */
    private suspend fun fetchMarkersWithFallback(): List<LocationMarker>? {
        fetchMarkers()?.let { return it }
        Timber.w("Direct marker fetch failed, attempting explicit map load...")
        return if (awaitMapLoad()) fetchMarkers() else null
    }

    /** Reads location markers from current map data. Returns null if none are found. */
    private suspend fun fetchMarkers(): List<LocationMarker>? = withContext(Dispatchers.IO) {
        runCatching {
            val mapData = robot?.getMapData() ?: return@runCatching null

            mapData.locations
                .filter { it.layerCategory == LOCATION }
                .mapNotNull { layer ->
                    val pose = layer.layerPoses?.firstOrNull() ?: return@mapNotNull null
                    LocationMarker(layer.layerId, pose.x, pose.y).also {
                        Timber.v("Found marker: ${it.name} at (${it.x}, ${it.y})")
                    }
                }
                .ifEmpty { null }
        }.onFailure { Timber.e(it, "Error fetching map data") }
            .getOrNull()
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

        Timber.d("Loading map: ${target.name} (${target.id})")

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
                            Timber.w("Map load failed with status: $status")
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
        navStatusCounter += 1
        val statusEventId = navStatusCounter
        val normalizedStatus = status.lowercase()
        val normalizedLocation = location.trim()
        Timber.d(
            "[NavStatus #%d] location='%s' status='%s' descriptionId=%d description='%s'",
            statusEventId,
            normalizedLocation,
            normalizedStatus,
            descriptionId,
            description
        )

        // Any progress-like status invalidates a previously queued abort/cancel publish.
        // IMPORTANT: This must happen BEFORE checking for empty location.
        if (normalizedStatus in setOf("start", "calculating", "reposing", "going")) {
            Timber.d(
                "[NavStatus #%d] progress status '%s' -> cancel pending terminal publish",
                statusEventId,
                normalizedStatus
            )
            pendingTerminalPublishJob?.cancel()
            pendingTerminalPublishJob = null
            if (normalizedLocation.isEmpty()) return
        }

        // Ignore SDK noise without concrete destination for non-progress statuses.
        if (normalizedLocation.isEmpty()) {
            Timber.d("[NavStatus #%d] ignored: empty location for status '%s'", statusEventId, normalizedStatus)
            return
        }

        if (normalizedStatus == "complete") {
            Timber.d("[NavStatus #%d] complete received -> publish immediately", statusEventId)
            pendingTerminalPublishJob?.cancel()
            pendingTerminalPublishJob = null
            publishTerminalStatus(normalizedLocation, normalizedStatus)
            return
        }

        if (normalizedStatus !in setOf("abort", "cancel", "cancelled")) {
            Timber.d("[NavStatus #%d] ignored: non-terminal status '%s'", statusEventId, normalizedStatus)
            return
        }

        // Temi may emit transient abort/cancel before continuing. Delay publish briefly.
        Timber.d(
            "[NavStatus #%d] queue delayed terminal publish: status='%s' delayMs=1500",
            statusEventId,
            normalizedStatus
        )
        pendingTerminalPublishJob?.cancel()
        pendingTerminalPublishJob = viewModelScope.launch {
            delay(1500)
            Timber.d("[NavStatus #%d] delayed terminal publish fired", statusEventId)
            publishTerminalStatus(normalizedLocation, normalizedStatus)
        }
    }

    private fun publishTerminalStatus(location: String, status: String) {
        val eventKey = "$location|$status"
        if (lastPublishedNavEvent == eventKey) {
            Timber.d("[NavStatus publish] deduplicated eventKey='%s'", eventKey)
            return
        }
        lastPublishedNavEvent = eventKey
        Timber.d("[NavStatus publish] sending status='%s' location='%s'", status, location)

        viewModelScope.launch {
            mqttManager.publishStatus(status = status, text = location)
        }
    }
}
