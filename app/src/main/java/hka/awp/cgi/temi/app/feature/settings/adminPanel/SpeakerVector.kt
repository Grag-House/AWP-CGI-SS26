package hka.awp.cgi.temi.app.feature.settings.adminPanel

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

/**
 * Speaker identification embedding vector (i-Vector).
 *
 * Produced by Vosk Speaker Model during speech recognition.
 * Used for speaker verification via cosine similarity.
 *
 * Dimensions: Always 128 (hardcoded in Vosk)
 */
@Serializable
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
