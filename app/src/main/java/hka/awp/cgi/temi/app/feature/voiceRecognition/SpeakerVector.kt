package hka.awp.cgi.temi.app.feature.voiceRecognition

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.sqrt

/**
 * Speaker identification embedding vector (i-Vector).
 *
 * Produced by Vosk Speaker Model during speech recognition.
 * Used for speaker verification via cosine similarity.
 *
 * Dimensions: Always 128 (hardcoded in Vosk)
 *
 * Vosk sends spk as naked float array: [-1.253, 0.309, ...], not as {values: [...]}
 */
@Serializable(with = SpeakerVectorSerializer::class)
data class SpeakerVector(val values: FloatArray) {

    init {
        require(values.size == VECTOR_SIZE) {
            "Speaker vector must be $VECTOR_SIZE dimensions, got ${values.size}. " +
                "Did Vosk model version change?"
        }
    }

    /**
     * Calculates cosine similarity with another speaker vector.
     * Range: 0.0 (completely different) to 1.0 (identical)
     * Typical verification threshold: 0.6
     * see: https://en.wikipedia.org/wiki/Cosine_similarity
     */
    infix fun cosineSimilarityWith(other: SpeakerVector): Double {
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in values.indices) {
            val a = values[i].toDouble()
            val b = other.values[i].toDouble()
            dotProduct += a * b
            normA += a * a
            normB += b * b
        }

        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator != 0.0) dotProduct / denominator else 0.0
    }

    // Required for Map/Set operations (FloatArray default equals compares reference, not content)
    override fun equals(other: Any?) =
        other is SpeakerVector && values.contentEquals(other.values)

    override fun hashCode() = values.contentHashCode()

    // FloatArray is mutable but we treat it as immutable
    override fun toString() = "SpeakerVector(size=$VECTOR_SIZE)"

    companion object {
        // Vosk Speaker Model outputs 128-dimensional i-Vectors
        // This is NOT configurable - hardcoded in the Vosk model
        const val VECTOR_SIZE = 128
    }
}

/**
 * Custom serializer for SpeakerVector.
 * Vosk sends spk as: "spk" : [-0.905319, 0.228561, ...]  (naked array)
 * Not as: "spk" : { "values": [...] }
 */
data object SpeakerVectorSerializer : KSerializer<SpeakerVector> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun serialize(encoder: Encoder, value: SpeakerVector) {
        check(encoder is JsonEncoder) { "Only JSON format supported" }
        // Serialize as object with "values" key for DataStore compatibility
        val valuesArray = JsonArray(value.values.map { JsonPrimitive(it) })
        encoder.encodeJsonElement(JsonObject(mapOf("values" to valuesArray)))
    }

    override fun deserialize(decoder: Decoder): SpeakerVector {
        check(decoder is JsonDecoder) { "Only JSON format supported" }

        val floatList = when (val element = decoder.decodeJsonElement()) {
            is JsonArray -> element.map { it.jsonPrimitive.float }
            is JsonObject -> {
                val valuesElement = element["values"] as? JsonArray
                    ?: throw IllegalArgumentException("SpeakerVector object missing 'values' array")
                valuesElement.map { it.jsonPrimitive.float }
            }

            else -> throw IllegalArgumentException("Unexpected JSON for SpeakerVector: $element")
        }

        return SpeakerVector(floatList.toFloatArray())
    }
}
