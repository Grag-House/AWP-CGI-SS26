package hka.awp.cgi.temi.app

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import hka.awp.cgi.temi.app.feature.controller.ControllerViewModel
import hka.awp.cgi.temi.app.feature.settings.display.DisplayViewModel
import hka.awp.cgi.temi.app.ui.shell.MainShell
import hka.awp.cgi.temi.app.ui.theme.CgiTheme
import hka.awp.cgi.temi.app.utils.hideTopBar
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.viewmodel.koinViewModel
import timber.log.Timber
import kotlin.math.abs

/**
 * The main entry point of the application.
 *
 * This activity is responsible for:
 * - Configuring system UI visibility, such as hiding the status bar for a full-screen experience.
 * - Setting up the Jetpack Compose UI layout within the [CgiTheme].
 */
class MainActivity : ComponentActivity() {

    private val controllerViewModel: ControllerViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // this will hide the android topBar and only show if in case the user swipes down
        hideTopBar(window)

        enableEdgeToEdge()

        setContent {
            val displayViewModel: DisplayViewModel = koinViewModel()
            val isDarkMode by displayViewModel.isDarkMode.collectAsState()

            CgiTheme(darkTheme = isDarkMode) {
                MainShell()
            }
        }
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        Timber.d(
            "MotionEvent source=${event.source} device=${event.device?.name}"
        )

        if (event.isGameControllerEvent()) {
            val x = event.getCenteredAxis(MotionEvent.AXIS_X)
            val y = event.getCenteredAxis(MotionEvent.AXIS_Y)

            Timber.d("Controller motion x=$x y=$y")

            controllerViewModel.onControllerInput(
                x = x,
                y = y,
            )

            return true
        }

        return super.dispatchGenericMotionEvent(event)
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        Timber.d(
            "KeyDown keyCode=$keyCode device=${event?.device?.name}"
        )
        if (event?.isGameControllerEvent() == true) {
            Timber.d("Controller button keyCode=$keyCode")

            when (keyCode) {
                KeyEvent.KEYCODE_BUTTON_B -> {
                    controllerViewModel.onControllerReleased()
                    return true
                }
            }
        }

        return super.onKeyDown(keyCode, event)
    }
}

private fun MotionEvent.isGameControllerEvent(): Boolean {
    return source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK ||
        source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD
}

private fun KeyEvent.isGameControllerEvent(): Boolean {
    return source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
        source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
}

private fun MotionEvent.getCenteredAxis(axis: Int): Float {
    val range = device?.getMotionRange(axis, source)
    val flat = range?.flat ?: DEFAULT_CONTROLLER_DEAD_ZONE
    val value = getAxisValue(axis)

    return if (abs(value) > flat) value else 0f
}

private const val DEFAULT_CONTROLLER_DEAD_ZONE = 0.08f
