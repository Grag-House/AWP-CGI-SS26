package hka.awp.cgi.temi.app.feature.settings.adminPanel

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
     * Typical verification threshold: 0.82
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
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "SpeakerVector",
        PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: SpeakerVector) {
        check(encoder is JsonEncoder) { "Only JSON format supported" }
        val jsonArray = JsonArray(value.values.map { JsonPrimitive(it) })
        encoder.encodeJsonElement(JsonObject(mapOf("values" to jsonArray)))
    }

    override fun deserialize(decoder: Decoder): SpeakerVector {
        check(decoder is JsonDecoder) { "Only JSON format supported" }

        return when (val element = decoder.decodeJsonElement()) {
            is JsonArray -> {
                // Vosk sends: [-0.905, 0.228, ...]
                val array = element.map { it.jsonPrimitive.content.toFloat() }.toFloatArray()
                SpeakerVector(array)
            }
            is JsonObject -> {
                // DataStore format: {"values":[...]} for backward compatibility
                val valuesElement: JsonElement = element["values"]
                    ?: throw IllegalArgumentException("SpeakerVector JSON object missing 'values'")
                val valuesArray = valuesElement as? JsonArray
                    ?: throw IllegalArgumentException("SpeakerVector 'values' must be JSON array")
                val array = valuesArray.map { it.jsonPrimitive.content.toFloat() }.toFloatArray()
                SpeakerVector(array)
            }
            else -> throw IllegalArgumentException("Expected JSON array for spk, got ${element::class.simpleName}")
        }
    }
}
