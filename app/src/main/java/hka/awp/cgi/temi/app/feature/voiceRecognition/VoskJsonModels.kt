package hka.awp.cgi.temi.app.feature.voiceRecognition

import hka.awp.cgi.temi.app.feature.settings.adminPanel.SpeakerVector
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoskPartialResult(
    val partial: String = "",
    val spk: SpeakerVector? = null,
    @SerialName("spk_frames") val spkFrames: Float = 0f
)

@Serializable
data class VoskFinalResult(
    val text: String = "",
    val spk: SpeakerVector? = null,
    @SerialName("spk_frames") val spkFrames: Float = 0f
)
