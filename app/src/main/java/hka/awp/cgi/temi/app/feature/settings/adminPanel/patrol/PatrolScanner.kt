package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

import com.robotemi.sdk.Robot
import kotlinx.coroutines.delay
import timber.log.Timber

class PatrolScanner(private val robot: Robot?) {

    private companion object {
        private const val MIN_TILT_ANGLE = -30
        private const val MAX_TILT_ANGLE = 50
        private const val CAMERA_TILT_SPEED = 0.4f
        private const val TURN_DELAY = 2500L
    }

    suspend fun executeScanSequence(cameraTiltAngle: Int, onPointReached: () -> Unit) {
        Timber.d("Starte Scan-Sequenz")

        // 1. Kamera neigen
        val safeAngle = cameraTiltAngle.coerceIn(MIN_TILT_ANGLE, MAX_TILT_ANGLE)
        robot?.tiltAngle(degrees = safeAngle, speed = CAMERA_TILT_SPEED)
        delay(1500L)

        // 2. Event an Server senden (über Callback)
        onPointReached()

        // 3. 360-Grad-Drehung
        repeat(4) {
            robot?.turnBy(90)
            delay(TURN_DELAY)
        }

        Timber.d("Scan-Sequenz beendet")
    }
}
