package hka.awp.cgi.temi.app.feature.mqtt

import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Manages MQTT communication for the Temi robot.
 * Subscribes to command topics and publishes status/ASR events.
 */
class MqttManager(private val robot: Robot?, private val client: Mqtt5BlockingClient) {

    companion object {
        private const val BASE_TOPIC = "innovation_lab/karlsruhe/temi"

        private const val GOTO_TOPIC = "$BASE_TOPIC/temi_goto/set"

        private const val SPEAK_TOPIC = "$BASE_TOPIC/temi_speak/set"
        private val json = Json { ignoreUnknownKeys = true }
    }

    /**
     * Connects to the MQTT broker and starts a blocking message loop.
     * This function will suspend until the message loop is finished or cancelled.
     */
    suspend fun connect() = withContext(Dispatchers.IO) {
        Timber.d("Connecting to MQTT broker at ${client.config.serverHost}")

        try {
            client.connect()
            Timber.i("MQTT connected successfully")

            // Subscribe to topics
            client.subscribeWith().topicFilter(GOTO_TOPIC).send()
            client.subscribeWith().topicFilter(SPEAK_TOPIC).send()

            // Message loop using the 'publishes' stream
            val publishes = client.publishes(MqttGlobalPublishFilter.ALL)

            while (isActive) {
                // In a production app, you might want to use receive(timeout, unit) to check isActive more frequently.
                val publish = publishes.receive()
                val topic = publish.topic.toString()
                val payload = String(publish.payloadAsBytes)

                when (topic) {
                    GOTO_TOPIC -> handleGoto(payload)
                    SPEAK_TOPIC -> handleSpeak(payload)
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "MQTT error or connection failed")
        } finally {
            disconnect()
        }
    }

    private fun handleGoto(payload: String) {
        try {
            val cmd = json.decodeFromString<MqttCommand>(payload)
            cmd.payloadObject?.let { location ->
                Timber.d("MQTT Command: Go to $location")
                robot?.goTo(location)
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Error parsing temi_goto payload: $payload")
        }
    }

    private fun handleSpeak(payload: String) {
        try {
            val cmd = json.decodeFromString<MqttCommand>(payload)
            cmd.payloadObject?.let { text ->
                Timber.d("MQTT Command: Speak '$text'")
                robot?.speak(TtsRequest.create(speech = text, isShowOnConversationLayer = false))
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Error parsing temi_speak payload: $payload")
        }
    }

    suspend fun publishAsr(text: String) = withContext(Dispatchers.IO) {
        try {
            val payload = json.encodeToString(MqttAsr(text))
            client.publishWith()
                .topic("$BASE_TOPIC/asrListener")
                .payload(payload.toByteArray())
                .send()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Failed to publish ASR")
        }
    }

    suspend fun publishStatus(status: String, text: String? = null) = withContext(Dispatchers.IO) {
        try {
            val payload = json.encodeToString(MqttStatus(status, text))
            client.publishWith()
                .topic("$BASE_TOPIC/onlocationsstatuschangevents")
                .payload(payload.toByteArray())
                .send()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Failed to publish status")
        }
    }

    fun disconnect() {
        try {
            client.disconnect()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Error during MQTT disconnect")
        }
    }
}
