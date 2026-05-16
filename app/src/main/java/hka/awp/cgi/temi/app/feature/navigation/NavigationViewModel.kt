package hka.awp.cgi.temi.app.feature.navigation

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.map.LOCATION
import com.robotemi.sdk.map.OnLoadMapStatusChangedListener
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener
import com.robotemi.sdk.navigation.listener.OnDistanceToLocationChangedListener
import com.robotemi.sdk.navigation.model.Position
import hka.awp.cgi.temi.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume

/** Repräsentiert einen Ort auf der Karte mit Name und X/Y-Koordinaten aus den Temi-Kartendaten. */
data class LocationMarker(val name: String, val x: Float, val y: Float)

/**
 * Zustand des aktuell angezeigten Standorts.
 * [Resource] für bekannte Ziele mit String-Ressource, [Custom] für unbekannte Wegpunkt-Namen.
 */
sealed class LocationState {
    data class Resource(@StringRes val resId: Int) : LocationState()
    data class Custom(val name: String) : LocationState()
}

class NavigationViewModel(
    private val robot: Robot?,
    private val application: Application
) : ViewModel(),
    OnRobotReadyListener,
    OnDistanceToLocationChangedListener,
    OnCurrentPositionChangedListener {

    private val _currentLocation = MutableStateFlow<LocationState>(
        LocationState.Resource(R.string.location_status_locating)
    )
    val currentLocation: StateFlow<LocationState> = _currentLocation.asStateFlow()

    private val _mapLocations = MutableStateFlow<List<LocationMarker>>(emptyList())
    val mapLocations: StateFlow<List<LocationMarker>> = _mapLocations.asStateFlow()

    private val _isMapLoading = MutableStateFlow(false)
    val isMapLoading: StateFlow<Boolean> = _isMapLoading.asStateFlow()

    private val _hasMapError = MutableStateFlow(false)
    val hasMapError: StateFlow<Boolean> = _hasMapError.asStateFlow()

    private val _robotPosition = MutableStateFlow<Position?>(null)
    val robotPosition: StateFlow<Position?> = _robotPosition.asStateFlow()

    private val _savedLocations = MutableStateFlow<List<String>>(emptyList())
    val savedLocations: StateFlow<List<String>> = _savedLocations.asStateFlow()

    private var loadingJob: Job? = null

    init {
        robot?.addOnRobotReadyListener(this)
        robot?.addOnDistanceToLocationChangedListener(this)
        robot?.addOnCurrentPositionChangedListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        robot?.removeOnRobotReadyListener(this)
        robot?.removeOnDistanceToLocationChangedListener(this)
        robot?.removeOnCurrentPositionChangedListener(this)
    }

    /** Lädt gespeicherte Orte und setzt die initiale Roboterposition, sobald der Roboter bereit ist. */
    override fun onRobotReady(isReady: Boolean) {
        if (!isReady) return
        val locs = robot?.locations ?: emptyList()
        Timber.d(application.getString(R.string.log_robot_ready, locs.toString()))
        _savedLocations.value = locs
        if (locs.isEmpty()) {
            _currentLocation.value = LocationState.Resource(R.string.location_status_none)
        }
        robot?.getPosition()?.let { _robotPosition.value = it }
    }

    /** Aktualisiert die Roboterposition bei jeder Positionsänderung. */
    override fun onCurrentPositionChanged(position: Position) {
        _robotPosition.value = position
        Timber.v(application.getString(R.string.log_position, position.x, position.y))
    }

    /** Ermittelt den nächstgelegenen Ort anhand der Abstände und aktualisiert den angezeigten Standort. */
    override fun onDistanceToLocationChanged(distances: Map<String, Float>) {
        if (distances.isEmpty()) return
        val nearest = distances.minByOrNull { it.value } ?: return
        val (systemName, distance) = nearest
        val destination = DestinationItems.fromSystemName(systemName)

        val newState = if (destination != null) {
            LocationState.Resource(destination.stringResource)
        } else {
            LocationState.Custom(systemName)
        }

        if (_currentLocation.value != newState) {
            _currentLocation.value = newState
            val logName = when (newState) {
                is LocationState.Resource -> application.getString(newState.resId)
                is LocationState.Custom -> newState.name
            }
            Timber.d(application.getString(R.string.log_location_distance, logName, distance))
        }
    }

    /** Schickt den Roboter zum angegebenen Wegpunkt-Namen. */
    fun goToLocation(name: String) {
        Timber.d(application.getString(R.string.log_navigating_to, name))
        runCatching { robot?.goTo(name) }
            .onFailure { Timber.e(it, application.getString(R.string.navigation_failed_with_name, name)) }
    }

    /** Startet den Ladevorgang der Karte und setzt den Dialog-Zustand zurück. */
    fun showMap() {
        Timber.d(application.getString(R.string.log_show_map, robot.toString(), robot?.locations.toString()))
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            _isMapLoading.value = true
            _hasMapError.value = false
            _mapLocations.value = emptyList()
            val markers = fetchMarkersWithFallback()
            _isMapLoading.value = false
            if (markers != null) {
                _mapLocations.value = markers
            } else {
                _hasMapError.value = true
            }
        }
    }

    /** Versucht Markierungen direkt zu laden; fällt auf explizites Kartenladen zurück, falls nötig. */
    private suspend fun fetchMarkersWithFallback(): List<LocationMarker>? {
        fetchMarkers()?.let { return it }
        Timber.w(application.getString(R.string.log_get_map_data_null_retry))
        val loaded = awaitMapLoad()
        if (!loaded) return null
        return fetchMarkers()
    }

    /** Liest Ortsmarkierungen aus den aktuellen Kartendaten. Gibt null zurück, wenn keine vorhanden sind. */
    private suspend fun fetchMarkers(): List<LocationMarker>? = withContext(Dispatchers.IO) {
        runCatching {
            Timber.d(application.getString(R.string.log_fetch_map_data, Thread.currentThread().name))
            val mapData = robot?.getMapData()
            Timber.d(application.getString(R.string.log_get_map_data_is_null, mapData == null))
            if (mapData == null) return@withContext null
            Timber.d(application.getString(R.string.log_layers_total, mapData.locations.size))
            mapData.locations.forEach { layer ->
                Timber.d(
                    application.getString(
                        R.string.log_layer_info,
                        layer.layerId,
                        layer.layerCategory,
                        layer.layerPoses?.size ?: 0
                    )
                )
            }
            val markers = mapData.locations
                .filter { it.layerCategory == LOCATION }
                .mapNotNull { layer ->
                    val pose = layer.layerPoses?.firstOrNull() ?: return@mapNotNull null
                    LocationMarker(layer.layerId, pose.x, pose.y)
                        .also {
                            Timber.d(
                                application.getString(R.string.log_marker_info, layer.layerId, pose.x, pose.y)
                            )
                        }
                }
            Timber.d(application.getString(R.string.log_markers_after_filter, LOCATION, markers.size))
            markers.ifEmpty {
                Timber.w(application.getString(R.string.log_no_location_layers))
                null
            }
        }.onFailure { Timber.e(it, application.getString(R.string.log_error_fetch_map_data)) }
            .getOrNull()
    }

    /**
     * Lädt die Karte explizit vom Roboter und wartet auf den SDK-Callback.
     * Zuerst wird die Kartenliste geholt und die Karte per [Robot.loadMap] gestartet.
     * Der SDK-Callback [OnLoadMapStatusChangedListener] wird per [suspendCancellableCoroutine]
     * in eine suspend-Funktion umgewandelt — gibt true bei Erfolg, false bei Fehler zurück.
     */
    private suspend fun awaitMapLoad(): Boolean {
        val maps = withContext(Dispatchers.IO) {
            runCatching { robot?.getMapList().orEmpty() }
                .onFailure { Timber.e(it, application.getString(R.string.log_error_fetch_map_list)) }
                .getOrElse { emptyList() }
        }
        Timber.d(application.getString(R.string.log_available_maps, maps.map { "${it.id}:${it.name}" }.toString()))
        val target = maps.firstOrNull { it.name == application.getString(R.string.default_map_name) }
            ?: maps.firstOrNull()
        if (target == null) {
            Timber.w(application.getString(R.string.log_no_maps_found))
            return false
        }
        Timber.d(application.getString(R.string.log_loading_map, target.id, target.name))
        return suspendCancellableCoroutine { cont ->
            var triggeredRequestId: String? = null
            val listener = object : OnLoadMapStatusChangedListener {
                override fun onLoadMapStatusChanged(status: Int, requestId: String) {
                    if (requestId != triggeredRequestId) return
                    Timber.d(application.getString(R.string.log_map_status_changed, status))
                    when (status) {
                        OnLoadMapStatusChangedListener.COMPLETE -> {
                            robot?.removeOnLoadMapStatusChangedListener(this)
                            if (cont.isActive) cont.resume(true)
                        }

                        OnLoadMapStatusChangedListener.START -> Unit
                        else -> {
                            Timber.w(application.getString(R.string.log_load_map_failed_status, status))
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

    /** Bricht einen laufenden Ladevorgang ab und setzt alle kartenbezogenen Zustände zurück. */
    fun dismissMap() {
        loadingJob?.cancel()
        loadingJob = null
        _mapLocations.value = emptyList()
        _isMapLoading.value = false
        _hasMapError.value = false
    }

}
