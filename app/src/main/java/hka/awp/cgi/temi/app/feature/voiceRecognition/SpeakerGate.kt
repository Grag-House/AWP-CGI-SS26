package hka.awp.cgi.temi.app.feature.voiceRecognition

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

/**
 * Manages the "Speaker Gate" which allows Temi ASR results to be processed
 * only if a speaker has been verified or if verification is disabled.
 */
class SpeakerGate(
    private val scope: CoroutineScope,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS
) {
    companion object {
        private const val DEFAULT_TIMEOUT_MS = 10_000L
    }

    private val _isOpen = MutableStateFlow(false)
    val isOpen: StateFlow<Boolean> = _isOpen.asStateFlow()

    private var timeoutJob: Job? = null

    /**
     * Opens the gate and starts a timeout.
     */
    fun open(onTimeout: (() -> Unit)? = null) {
        _isOpen.value = true
        Timber.v("Speaker gate opened (%d ms)", timeoutMs)
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(timeoutMs.milliseconds)
            if (isOpen.value) {
                Timber.v("Speaker gate timed out")
                close()
                onTimeout?.invoke()
            }
        }
    }

    /**
     * Consumes the gate (closes it and returns true if it was open).
     */
    fun consume(): Boolean {
        if (!isOpen.value) return false
        timeoutJob?.cancel()
        close()
        return true
    }

    /**
     * Closes the gate immediately.
     */
    fun close() {
        _isOpen.value = false
        timeoutJob?.cancel()
        timeoutJob = null
    }

    fun release() {
        close()
    }
}
