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
@Suppress("TooManyFunctions")
class MqttManager(private val robot: Robot?, private val client: Mqtt5BlockingClient) {
    companion object {
        private const val BASE_TOPIC = "innovation_lab/karlsruhe/temi"
        private const val GOTO_TOPIC = "$BASE_TOPIC/temi_goto/set"
        private const val WAKE_UP_TOPIC = "$BASE_TOPIC/temi_wake_up/set"
        private const val FOLLOW_TOPIC = "$BASE_TOPIC/temi_follow/set"
        private const val STOP_MOVEMENT_TOPIC = "$BASE_TOPIC/temi_stop_movement/set"
        private const val GET_LOCATIONS_TOPIC = "$BASE_TOPIC/temi_get_locations"
        private const val GET_READY_STATE_TOPIC = "$BASE_TOPIC/temi_get_ready_state"
        private const val PLAY_SEQUENCE_TOPIC = "$BASE_TOPIC/temi_playsequence/set"
        private const val TILT_ANGLE_TOPIC = "$BASE_TOPIC/temi_tilt_angle/set"
        private const val SPEAK_TOPIC = "$BASE_TOPIC/temi_speak/set"
        private const val TTS_LISTENER_TOPIC = "$BASE_TOPIC/ttsListener"
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
            client.subscribeWith().topicFilter(WAKE_UP_TOPIC).send()
            client.subscribeWith().topicFilter(FOLLOW_TOPIC).send()
            client.subscribeWith().topicFilter(STOP_MOVEMENT_TOPIC).send()
            client.subscribeWith().topicFilter(GET_LOCATIONS_TOPIC).send()
            client.subscribeWith().topicFilter(GET_READY_STATE_TOPIC).send()
            client.subscribeWith().topicFilter(PLAY_SEQUENCE_TOPIC).send()
            client.subscribeWith().topicFilter(TILT_ANGLE_TOPIC).send()
            Timber.d("MQTT subscriptions active")

            // Message loop using the 'publishes' stream
            val publishes = client.publishes(MqttGlobalPublishFilter.ALL)

            while (isActive) {
                val publish = publishes.receive()
                val topic = publish.topic.toString()
                val payload = publish.payloadAsBytes.toString(Charsets.UTF_8)

                when (topic) {
                    GOTO_TOPIC -> handleGoto(payload)
                    SPEAK_TOPIC -> handleSpeak(payload)
                    WAKE_UP_TOPIC -> robot?.wakeup()
                    FOLLOW_TOPIC -> robot?.beWithMe()
                    STOP_MOVEMENT_TOPIC -> robot?.stopMovement()
                    GET_LOCATIONS_TOPIC -> publishLocations()
                    GET_READY_STATE_TOPIC -> handleGetReadyState()
                    PLAY_SEQUENCE_TOPIC -> handlePlaySequence(payload)
                    TILT_ANGLE_TOPIC -> handleTiltAngle(payload)
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
            val location = cmd.payloadObject?.trim() ?: return
            if (location.isEmpty()) return
            robot?.goTo(location)
            publishStatusBlocking(status = "going", location = location)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Error parsing temi_goto payload: $payload")
        }
    }

    private fun publishStatusBlocking(status: String, location: String? = null) {
        try {
            val payload = json.encodeToString(MqttStatus(status, location))
            client.publishWith().topic("$BASE_TOPIC/onlocationsstatuschangevents").payload(payload.toByteArray()).send()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Failed to publish status")
        }
    }

    private fun handleSpeak(payload: String) {
        try {
            val cmd = json.decodeFromString<MqttCommand>(payload)
            cmd.payloadObject?.let { text ->
                robot?.speak(TtsRequest.create(speech = text, isShowOnConversationLayer = false))
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Error parsing temi_speak payload: $payload")
        }
    }

    private fun publishLocations() {
        try {
            val locations = robot?.locations.orEmpty()
            val payload = json.encodeToString(MqttLocations(payloadObject = locations))
            client.publishWith().topic("$GET_LOCATIONS_TOPIC/return").payload(payload.toByteArray()).send()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Failed to publish locations")
        }
    }

    private fun handleGetReadyState() {
        try {
            client.publishWith()
                .topic("$GET_READY_STATE_TOPIC/return")
                .payload("""{"payloadObject":${robot != null}}""".toByteArray())
                .send()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Failed to publish ready state")
        }
    }

    private fun handlePlaySequence(payload: String) {
        try {
            val cmd = json.decodeFromString<MqttCommand>(payload)
            cmd.payloadObject?.let { sequence ->
                robot?.playSequence(sequence)
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Error parsing temi_playsequence payload: $payload")
        }
    }

    private fun handleTiltAngle(payload: String) {
        try {
            val cmd = json.decodeFromString<MqttCommand>(payload)
            cmd.payloadObject?.toIntOrNull()?.let { angle ->
                robot?.tiltAngle(angle)
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Error parsing temi_tilt_angle payload: $payload")
        }
    }

    suspend fun publishAsr(text: String) = withContext(Dispatchers.IO) {
        try {
            val payload = json.encodeToString(MqttAsr(text))
            client.publishWith().topic("$BASE_TOPIC/asrListener").payload(payload.toByteArray()).send()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Failed to publish ASR")
        }
    }

    suspend fun publishStatus(status: String, location: String? = null) = withContext(Dispatchers.IO) {
        publishStatusBlocking(status = status, location = location)
    }

    suspend fun publishTtsStatus(status: String) = withContext(Dispatchers.IO) {
        try {
            val payload = json.encodeToString(MqttStatus(status = status))
            client.publishWith().topic(TTS_LISTENER_TOPIC).payload(payload.toByteArray()).send()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Failed to publish TTS status")
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
