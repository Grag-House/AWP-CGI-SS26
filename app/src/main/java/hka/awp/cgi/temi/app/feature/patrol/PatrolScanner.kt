package hka.awp.cgi.temi.app.feature.patrol

import com.robotemi.sdk.Robot
import kotlinx.coroutines.delay
import timber.log.Timber

class PatrolScanner(private val robot: Robot?) {

    private companion object {
        private const val CAMERA_TILT_SPEED = 0.4f
        private const val TURN_DELAY_MS = 2500L
        private const val SCAN_TILT_ANGLE = 0
        private const val INITIAL_STABILIZATION_DELAY_MS = 1500L
        private const val TURN_DEGREES = 90
        private const val SCAN_ROTATIONS = 4
    }

    suspend fun executeScanSequence(onPointReached: () -> Unit) {
        Timber.d("Starte Scan-Sequenz, setze Tilt auf $SCAN_TILT_ANGLE")

        robot?.tiltAngle(degrees = SCAN_TILT_ANGLE, speed = CAMERA_TILT_SPEED)
        delay(INITIAL_STABILIZATION_DELAY_MS)

        onPointReached()

        repeat(SCAN_ROTATIONS) {
            robot?.turnBy(TURN_DEGREES)
            delay(TURN_DELAY_MS)
            robot?.tiltAngle(degrees = SCAN_TILT_ANGLE, speed = CAMERA_TILT_SPEED)
        }

        robot?.tiltAngle(degrees = SCAN_TILT_ANGLE, speed = CAMERA_TILT_SPEED)

        Timber.d("Scan-Sequenz beendet")
    }
}
