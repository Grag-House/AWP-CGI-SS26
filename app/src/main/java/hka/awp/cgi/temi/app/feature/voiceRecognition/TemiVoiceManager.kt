package hka.awp.cgi.temi.app.feature.voiceRecognition

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.SpeakerModel
import org.vosk.android.StorageService
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * Manages the initialization and access to the Vosk speech recognition model
 * and the speaker identification model.
 */
class TemiVoiceManager(private val context: Context) {

    private var voskModel: Model? = null
    private var spkModel: SpeakerModel? = null

    val model: Model?
        get() = voskModel

    val speakerModel: SpeakerModel?
        get() = spkModel

    /**
     * Initializes the Vosk model and Speaker model asynchronously in the background.
     */
    suspend fun initModel(): Boolean = withContext(Dispatchers.IO) {
        if (voskModel != null && spkModel != null) return@withContext true

        try {
            Timber.v("Starting unpacking of the Vosk models...")

            val mainModelLoaded = suspendCancellableCoroutine { continuation ->
                StorageService.unpack(
                    context,
                    "model-de",
                    "model",
                    { loadedModel ->
                        voskModel = loadedModel
                        Timber.d("Vosk main model successfully loaded!")
                        if (continuation.isActive) continuation.resume(true)
                    },
                    { exception ->
                        Timber.e(exception, "Error unpacking the Vosk main model")
                        if (continuation.isActive) continuation.resume(false)
                    }
                )
            }

            if (!mainModelLoaded) return@withContext false

            Timber.v("Starting unpacking of the Vosk speaker model...")
            val spkPath = StorageService.sync(context, "model-spk", "model-spk")
            spkModel = SpeakerModel(spkPath)
            Timber.d("Vosk speaker model successfully loaded from $spkPath")

            return@withContext voskModel != null && spkModel != null
        } catch (
            @Suppress("TooGenericExceptionCaught")
            e: Exception
        ) {
            Timber.e(e, "Critical error during Vosk initialization")
            return@withContext false
        }
    }

    /**
     * Checks if the system is ready to listen.
     */
    fun isReady(): Boolean = voskModel != null && spkModel != null

    /**
     * Releases the models to free resources.
     */
    fun release() {
        voskModel?.close()
        voskModel = null
        spkModel?.close()
        spkModel = null
        Timber.d("Vosk models released.")
    }
}
