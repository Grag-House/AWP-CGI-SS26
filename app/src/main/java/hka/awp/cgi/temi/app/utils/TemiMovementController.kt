package hka.awp.cgi.temi.app.utils

import com.robotemi.sdk.Robot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val MOVEMENT_REPEAT_DELAY_MS = 250L

class TemiMovementController(
    private val robot: Robot?,
    private val scope: CoroutineScope,
) {
    private var movementJob: Job? = null

    fun move(linear: Float, angular: Float) {
        movementJob?.cancel()

        movementJob = scope.launch {
            while (isActive) {
                robot?.skidJoy(
                    linear.coerceIn(-1f, 1f),
                    angular.coerceIn(-1f, 1f)
                )
                delay(MOVEMENT_REPEAT_DELAY_MS)
            }
        }
    }

    fun stop() {
        movementJob?.cancel()
        movementJob = null
        robot?.skidJoy(0f, 0f)
    }
}
