package hka.awp.cgi.temi.app.feature.voiceRecognition

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SpeakerVectorTest {

    private fun createVector(value: Float): SpeakerVector {
        return SpeakerVector(FloatArray(SpeakerVector.VECTOR_SIZE) { value })
    }

    @Test
    fun `cosine similarity with identical vector is 1`() {
        val v1 = createVector(1.0f)
        val v2 = createVector(1.0f)
        assertEquals(1.0, v1 cosineSimilarityWith v2, 0.0001)
    }

    @Test
    fun `cosine similarity with orthogonal vectors is 0`() {
        val v1Values = FloatArray(SpeakerVector.VECTOR_SIZE) { 0f }
        v1Values[0] = 1.0f
        val v2Values = FloatArray(SpeakerVector.VECTOR_SIZE) { 0f }
        v2Values[1] = 1.0f

        val v1 = SpeakerVector(v1Values)
        val v2 = SpeakerVector(v2Values)

        assertEquals(0.0, v1 cosineSimilarityWith v2, 0.0001)
    }

    @Test
    fun `serialization as naked array (Vosk format)`() {
        val values = FloatArray(SpeakerVector.VECTOR_SIZE) { it.toFloat() }

        // This is what Vosk sends
        val jsonArray = values.joinToString(prefix = "[", postfix = "]", separator = ",") { it.toString() }

        val decoded = Json.decodeFromString<SpeakerVector>(jsonArray)
        assertArrayEquals(values, decoded.values, 0.0f)
    }

    @Test
    fun `serialization as object (DataStore format)`() {
        val values = FloatArray(SpeakerVector.VECTOR_SIZE) { it.toFloat() }

        val jsonObject = """{"values":[${values.joinToString(",")}]}"""

        val decoded = Json.decodeFromString<SpeakerVector>(jsonObject)
        assertArrayEquals(values, decoded.values, 0.0f)
    }

    @Test
    fun `constructor throws on wrong size`() {
        assertThrows(IllegalArgumentException::class.java) {
            SpeakerVector(FloatArray(10))
        }
    }
}
