package hka.awp.cgi.temi.app.feature.hideandseek

import com.robotemi.sdk.Robot
import com.robotemi.sdk.map.LOCATION
import com.robotemi.sdk.navigation.model.Position
import com.robotemi.sdk.permission.OnRequestPermissionResultListener
import com.robotemi.sdk.permission.Permission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val REQUEST_CODE_MAP = 1001

class HideAndSeekNavigator(
    private val robot: Robot?,
    private val scope: CoroutineScope
) : OnRequestPermissionResultListener {

    private var locationPositions: Map<String, Position> = emptyMap()

    init {
        robot?.addOnRequestPermissionResultListener(this)
        requestMapPermissionIfNeeded()
    }

    fun release() {
        robot?.removeOnRequestPermissionResultListener(this)
    }

    private fun requestMapPermissionIfNeeded() {
        val r = robot ?: return
        if (r.checkSelfPermission(Permission.MAP) == Permission.GRANTED) {
            loadLocationPositions()
        } else {
            Timber.d("MAP permission not granted, requesting...")
            r.requestPermissions(listOf(Permission.MAP), REQUEST_CODE_MAP)
        }
    }

    override fun onRequestPermissionResult(permission: Permission, grantResult: Int, requestCode: Int) {
        if (permission == Permission.MAP && grantResult == Permission.GRANTED) {
            Timber.d("MAP permission granted, loading location positions")
            loadLocationPositions()
        }
    }

    private fun loadLocationPositions() {
        scope.launch {
            runCatching {
                val mapData = withContext(Dispatchers.IO) { robot?.getMapData() } ?: return@runCatching
                locationPositions = mapData.locations
                    .filter { it.layerCategory == LOCATION }
                    .mapNotNull { layer ->
                        val pose = layer.layerPoses?.firstOrNull() ?: return@mapNotNull null
                        layer.layerId to Position(x = pose.x, y = pose.y, yaw = pose.theta)
                    }
                    .toMap()
                Timber.d("Loaded %d location positions", locationPositions.size)
            }.onFailure { Timber.e(it, "Failed to load location positions from map") }
        }
    }

    fun navigateTo(hidingSpot: String) {
        val r = robot ?: return
        val position = locationPositions[hidingSpot]
        if (position != null) {
            Timber.d("Navigating via goToPosition (no UI overlay): %s", hidingSpot)
            runCatching { r.goToPosition(position) }
                .onFailure {
                    Timber.e(it, "goToPosition failed for %s, falling back to goTo", hidingSpot)
                    runCatching { r.goTo(hidingSpot) }
                        .onFailure { ex -> Timber.e(ex, "goTo fallback also failed for: %s", hidingSpot) }
                }
        } else {
            Timber.w("No position cached for %s, using goTo", hidingSpot)
            runCatching { r.goTo(hidingSpot) }
                .onFailure { Timber.e(it, "Navigation failed for: %s", hidingSpot) }
        }
    }
}
