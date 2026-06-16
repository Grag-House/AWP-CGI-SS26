package hka.awp.cgi.temi.app.feature.voiceRecognition

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoskPartialResult(
    val partial: String = "",
    val spk: List<Float>? = null,
    @SerialName("spk_frames") val spkFrames: Int = 0
)

@Serializable
data class VoskFinalResult(
    val text: String = "",
    val spk: List<Float>? = null,
    @SerialName("spk_frames") val spkFrames: Int = 0
)
