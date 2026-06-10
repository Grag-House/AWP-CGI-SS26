package hka.awp.cgi.temi.app

import android.content.Context
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.SoundPool
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
import hka.awp.cgi.temi.app.utils.LanguageHelper
import hka.awp.cgi.temi.app.utils.hideTopBar
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.viewmodel.koinViewModel
import timber.log.Timber
import java.util.Locale
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

    private lateinit var soundPool: SoundPool
    private var hornSoundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // this will hide the android topBar and only show if in case the user swipes down
        hideTopBar(window)

        enableEdgeToEdge()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        hornSoundId = soundPool.load(this, R.raw.horn, 1)

        setContent {
            val displayViewModel: DisplayViewModel = koinViewModel()
            val isDarkMode by displayViewModel.isDarkMode.collectAsState()

            CgiTheme(darkTheme = isDarkMode) {
                MainShell()
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val langCode = LanguageHelper.getLocale(newBase)
        val locale = Locale(langCode)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.isGameControllerEvent()) {
            val leftY = event.getCenteredAxis(MotionEvent.AXIS_Y)
            val rightX = event.getCenteredAxis(MotionEvent.AXIS_Z)

            controllerViewModel.onControllerInput(
                x = -rightX,
                y = leftY,
            )

            return true
        }

        return super.dispatchGenericMotionEvent(event)
    }

    override fun onDestroy() {
        soundPool.release()
        super.onDestroy()
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        if (event?.isGameControllerEvent() == true) {
            when (keyCode) {
                CONTROLLER_KEYCODE_TRIANGLE -> {
                    if (controllerViewModel.controllerEnabled.value) {
                        soundPool.play(
                            hornSoundId,
                            1f,
                            1f,
                            1,
                            0,
                            1f,
                        )
                    }
                }

                else -> {
                    Timber.d("Ignored controller keyCode=$keyCode")
                    return true
                }
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        if (event?.isGameControllerEvent() == true) {
            return true
        }

        return super.onKeyUp(keyCode, event)
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
private const val CONTROLLER_KEYCODE_TRIANGLE = 99
