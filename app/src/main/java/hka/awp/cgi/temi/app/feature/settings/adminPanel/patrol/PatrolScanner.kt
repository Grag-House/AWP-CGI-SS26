package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

import com.robotemi.sdk.Robot
import kotlinx.coroutines.delay
import timber.log.Timber

class PatrolScanner(private val robot: Robot?) {

    private companion object {
        private const val CAMERA_TILT_SPEED = 0.4f
        private const val TURN_DELAY = 2500L
        private const val SCAN_TILT_ANGLE = 0
    }

    suspend fun executeScanSequence(onPointReached: () -> Unit) {
        Timber.d("Starte Scan-Sequenz, setze Tilt auf $SCAN_TILT_ANGLE")

        // 1. Kamera neigen
        robot?.tiltAngle(degrees = SCAN_TILT_ANGLE, speed = CAMERA_TILT_SPEED)
        delay(1500L)

        // 2. Event an Server senden (über Callback)
        onPointReached()

        // 3. 360-Grad-Drehung
        repeat(4) {
            robot?.turnBy(90)
            delay(TURN_DELAY)
            robot?.tiltAngle(degrees = SCAN_TILT_ANGLE, speed = CAMERA_TILT_SPEED)
        }

        robot?.tiltAngle(degrees = SCAN_TILT_ANGLE, speed = CAMERA_TILT_SPEED)

        Timber.d("Scan-Sequenz beendet")
    }
}
