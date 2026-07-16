package hka.awp.cgi.temi.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import hka.awp.cgi.temi.app.data.repository.GeneralConfigRepository
import hka.awp.cgi.temi.app.feature.controller.ControllerViewModel
import hka.awp.cgi.temi.app.feature.settings.display.DisplayViewModel
import hka.awp.cgi.temi.app.feature.voiceRecognition.TemiVoiceRecognitionViewModel
import hka.awp.cgi.temi.app.ui.shell.MainShell
import hka.awp.cgi.temi.app.ui.theme.CgiTheme
import hka.awp.cgi.temi.app.utils.hideTopBar
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.viewmodel.koinViewModel
import timber.log.Timber
import java.util.Locale
import kotlin.math.abs

/**
 * The main entry point of the application.
 *
 * This activity acts as the primary container for the Jetpack Compose UI,
 * manages hardware interfaces (game controller inputs, audio effects),
 * and handles runtime permissions.
 */
class MainActivity : ComponentActivity() {

    private val controllerViewModel: ControllerViewModel by viewModel()
    private val temiVoiceRecognitionViewModel: TemiVoiceRecognitionViewModel by viewModel()

    private lateinit var soundPool: SoundPool
    private var hornSoundId: Int = -1

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

        Timber.d("Permissions -> Camera: $cameraGranted, Mic: $micGranted")

        if (micGranted) {
            initTemiVoiceRecognition()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        hideTopBar(window)
        enableEdgeToEdge()

        initSoundPool()

        setContent {
            val displayViewModel: DisplayViewModel = koinViewModel()
            val isDarkMode by displayViewModel.isDarkMode.collectAsState()
            CgiTheme(darkTheme = isDarkMode) {
                MainShell()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        } else {
            initTemiVoiceRecognition()
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun initTemiVoiceRecognition() {
        temiVoiceRecognitionViewModel.initializeVoiceAi()
    }

    private fun initSoundPool() {
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        hornSoundId = soundPool.load(this, R.raw.horn, 1)
    }

    override fun attachBaseContext(newBase: Context) {
        // SharedPreferences-based language lookup for early bootstrap, mirrored by
        // GeneralConfigRepository.updateLanguage whenever the DataStore value changes.
        val prefs = newBase.getSharedPreferences(
            GeneralConfigRepository.SETTINGS_PREFS_NAME,
            MODE_PRIVATE
        )
        val langCode = prefs.getString(
            GeneralConfigRepository.LANGUAGE_PREF_KEY,
            GeneralConfigRepository.DEFAULT_LANGUAGE
        ) ?: GeneralConfigRepository.DEFAULT_LANGUAGE

        val config = Configuration(newBase.resources.configuration)
        config.setLocale(Locale.Builder().setLanguage(langCode).build())
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (!event.isFromGameController()) return super.dispatchGenericMotionEvent(event)

        controllerViewModel.onControllerInput(
            x = -event.getCenteredAxis(MotionEvent.AXIS_Z),
            y = event.getCenteredAxis(MotionEvent.AXIS_Y),
        )
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.isFromGameController() != true) return super.onKeyDown(keyCode, event)

        when (keyCode) {
            CONTROLLER_KEYCODE_ENABLE -> controllerViewModel.setControllerEnabled(true)
            CONTROLLER_KEYCODE_DISABLE -> controllerViewModel.setControllerEnabled(false)
            CONTROLLER_KEYCODE_TRIANGLE -> {
                if (controllerViewModel.controllerEnabled.value) {
                    soundPool.play(hornSoundId, 1f, 1f, 1, 0, 1f)
                }
            }
            else -> {
                Timber.d("Ignored controller keyCode=$keyCode")
                return true
            }
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return if (event?.isFromGameController() == true) true else super.onKeyUp(keyCode, event)
    }

    override fun onDestroy() {
        soundPool.release()
        super.onDestroy()
    }
}

private fun MotionEvent.isFromGameController(): Boolean =
    source and (InputDevice.SOURCE_JOYSTICK or InputDevice.SOURCE_GAMEPAD) != 0

private fun KeyEvent.isFromGameController(): Boolean =
    source and (InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK) != 0

private fun MotionEvent.getCenteredAxis(axis: Int): Float {
    val flat = device?.getMotionRange(axis, source)?.flat ?: DEFAULT_CONTROLLER_DEAD_ZONE
    val value = getAxisValue(axis)
    return if (abs(value) > flat) value else 0f
}

private const val DEFAULT_CONTROLLER_DEAD_ZONE = 0.08f
private const val CONTROLLER_KEYCODE_TRIANGLE = 99
private const val CONTROLLER_KEYCODE_ENABLE = 101
private const val CONTROLLER_KEYCODE_DISABLE = 100
