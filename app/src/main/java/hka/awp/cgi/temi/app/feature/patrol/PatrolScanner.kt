package hka.awp.cgi.temi.app.feature.patrol

import com.robotemi.sdk.Robot
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

/**
 * Handles the scanning behavior of the robot at patrol points.
 * The scanner performs a series of rotations and tilt adjustments to
 * visually inspect the surrounding area once a target location has been reached.
 *
 * @param robot The [Robot] instance used for hardware commands.
 */
class PatrolScanner(private val robot: Robot?) {

    private companion object {
        private const val CAMERA_TILT_SPEED = 0.4f
        private const val TURN_DELAY_MS = 2500L
        private const val SCAN_TILT_ANGLE = 0
        private const val INITIAL_STABILIZATION_DELAY_MS = 1500L
        private const val TURN_DEGREES = 90
        private const val SCAN_ROTATIONS = 4
    }

    /**
     * Executes the complete scan sequence.
     * Resets the camera tilt, waits for stabilization, triggers the [onPointReached] callback,
     * and performs a sequence of rotations to scan the environment.
     *
     * @param onPointReached Callback triggered once the robot has reached the patrol point
     * and performed initial stabilization.
     */
    suspend fun executeScanSequence(onPointReached: () -> Unit) {
        Timber.d("Starting scan sequence, setting tilt to %d", SCAN_TILT_ANGLE)

        robot?.tiltAngle(degrees = SCAN_TILT_ANGLE, speed = CAMERA_TILT_SPEED)
        delay(INITIAL_STABILIZATION_DELAY_MS.milliseconds)

        // Signal that the target point is reached and stabilized
        onPointReached()

        // Perform rotations to scan 360 degrees
        repeat(SCAN_ROTATIONS) {
            robot?.turnBy(TURN_DEGREES)
            delay(TURN_DELAY_MS.milliseconds)
            robot?.tiltAngle(degrees = SCAN_TILT_ANGLE, speed = CAMERA_TILT_SPEED)
        }

        // Final tilt reset
        robot?.tiltAngle(degrees = SCAN_TILT_ANGLE, speed = CAMERA_TILT_SPEED)

        Timber.d("Scan sequence completed")
    }
}
