package hka.awp.cgi.temi.app.feature.mqtt

import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.robotemi.sdk.Robot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.time.Instant

/**
 * Direction of MQTT traffic.
 */
enum class MqttTrafficDirection {
    /** Inbound from broker. */
    INBOUND,

    /** Outbound to broker. */
    OUTBOUND
}

/**
 * Represents a single MQTT traffic event for debugging/logging.
 *
 * @property timestampEpochMillis The epoch timestamp in milliseconds.
 * @property direction The [MqttTrafficDirection].
 * @property topic The MQTT topic.
 * @property payload The message payload.
 */
data class MqttTrafficEvent(
    val timestampEpochMillis: Long,
    val direction: MqttTrafficDirection,
    val topic: String,
    val payload: String
)

/**
 * Manages MQTT communication for the Temi robot.
 * Subscribes to command topics and publishes status/ASR events.
 *
 * @property client The HiveMQ blocking MQTT client.
 */
class MqttManager(
    private val client: Mqtt5BlockingClient,
    robot: Robot?
) {
    private val commandHandler = MqttCommandHandler(robot, ::publishMessage)

    companion object {
        private const val BASE_TOPIC = "innovation_lab/karlsruhe/temi"
        private const val TTS_LISTENER_TOPIC = "$BASE_TOPIC/ttsListener"

        /** Topic for battery level reports. */
        const val BATTERY_TOPIC = "$BASE_TOPIC/temi_battery_level"

        private val json = Json { ignoreUnknownKeys = true }
        private const val MAX_TRAFFIC_EVENTS = 200

        private val _latestTtsStatus = MutableStateFlow<String?>(null)

        /** StateFlow providing the latest TTS status received from MQTT. */
        val latestTtsStatus: StateFlow<String?> = _latestTtsStatus.asStateFlow()

        /** Set of topics that should be reported or monitored. */
        val reportTopics: Set<String> = setOf(
            MqttCommandHandler.GOTO_TOPIC,
            MqttCommandHandler.SPEAK_TOPIC,
            MqttCommandHandler.WAKE_UP_TOPIC,
            MqttCommandHandler.FOLLOW_TOPIC,
            MqttCommandHandler.STOP_MOVEMENT_TOPIC,
            MqttCommandHandler.GET_LOCATIONS_TOPIC,
            "${MqttCommandHandler.GET_LOCATIONS_TOPIC}/return",
            MqttCommandHandler.GET_READY_STATE_TOPIC,
            "${MqttCommandHandler.GET_READY_STATE_TOPIC}/return",
            MqttCommandHandler.PLAY_SEQUENCE_TOPIC,
            MqttCommandHandler.TILT_ANGLE_TOPIC,
            "$BASE_TOPIC/onlocationsstatuschangevents",
            "$BASE_TOPIC/asrListener",
            TTS_LISTENER_TOPIC
        )
    }

    private val _trafficEvents = MutableStateFlow<List<MqttTrafficEvent>>(emptyList())

    /** StateFlow providing a history of MQTT traffic events. */
    val trafficEvents: StateFlow<List<MqttTrafficEvent>> = _trafficEvents.asStateFlow()

    /**
     * Connects to the MQTT broker and starts a blocking message loop.
     * This function will suspend until the message loop is finished or canceled.
     */
    suspend fun connect() = withContext(Dispatchers.IO) {
        Timber.d("Connecting to MQTT broker at %s", client.config.serverHost)

        try {
            client.connect()
            Timber.i("MQTT connected successfully")

            client.subscribeWith().topicFilter(MqttCommandHandler.GOTO_TOPIC).send()
            client.subscribeWith().topicFilter(MqttCommandHandler.SPEAK_TOPIC).send()
            client.subscribeWith().topicFilter(MqttCommandHandler.WAKE_UP_TOPIC).send()
            client.subscribeWith().topicFilter(MqttCommandHandler.FOLLOW_TOPIC).send()
            client.subscribeWith().topicFilter(MqttCommandHandler.STOP_MOVEMENT_TOPIC).send()
            client.subscribeWith().topicFilter(MqttCommandHandler.GET_LOCATIONS_TOPIC).send()
            client.subscribeWith().topicFilter(MqttCommandHandler.GET_READY_STATE_TOPIC).send()
            client.subscribeWith().topicFilter(MqttCommandHandler.PLAY_SEQUENCE_TOPIC).send()
            client.subscribeWith().topicFilter(MqttCommandHandler.TILT_ANGLE_TOPIC).send()
            client.subscribeWith().topicFilter(TTS_LISTENER_TOPIC).send()
            Timber.d("MQTT subscriptions active")

            // Message loop using the 'publishes' stream
            val publishes = client.publishes(MqttGlobalPublishFilter.ALL)

            while (isActive) {
                val publish = publishes.receive()
                val topic = publish.topic.toString()
                val payload = publish.payloadAsBytes.toString(Charsets.UTF_8)
                appendTrafficEvent(MqttTrafficDirection.INBOUND, topic, payload)

                if (topic == TTS_LISTENER_TOPIC) {
                    runCatching {
                        val status = json.decodeFromString<MqttStatus>(payload).status
                        _latestTtsStatus.value = status
                    }.onFailure {
                        Timber.e(it, "Error parsing ttsListener payload: %s", payload)
                    }
                } else {
                    commandHandler.handleMessage(topic, payload)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "MQTT error or connection failed")
        } finally {
            disconnect()
        }
    }

    /**
     * Suspends until the TTS status becomes "completed" or the timeout is reached.
     *
     * @param timeoutMs Maximum time to wait in milliseconds.
     * @return True if "completed" was received, false otherwise.
     */
    suspend fun waitForTtsCompleted(timeoutMs: Long = 10_000L): Boolean {
        return withTimeoutOrNull(timeoutMs) {
            latestTtsStatus.first { status ->
                status.equals("completed", ignoreCase = true)
            }
            true
        } ?: false
    }

    /**
     * Publishes an ASR (Automated Speech Recognition) text to the broker.
     *
     * @param text The recognized text.
     */
    suspend fun publishAsr(text: String) = withContext(Dispatchers.IO) {
        runCatching {
            val payload = json.encodeToString(MqttAsr(text))
            publishMessage("$BASE_TOPIC/asrListener", payload)
        }.onFailure {
            Timber.e(it, "Failed to publish ASR")
        }
    }

    /**
     * Publishes a robot status and optional location.
     *
     * @param status The status string (e.g., "going", "arrived").
     * @param location Optional location name.
     * @param topic The target MQTT topic.
     */
    suspend fun publishStatus(
        status: String,
        location: String? = null,
        topic: String = "$BASE_TOPIC/onlocationsstatuschangevents"
    ) = withContext(Dispatchers.IO) {
        runCatching {
            val payload = json.encodeToString(MqttStatus(status, location))
            publishMessage(topic, payload)
        }.onFailure {
            Timber.e(it, "Failed to publish status")
        }
    }

    /**
     * Publishes a TTS status message.
     *
     * @param status The TTS status (e.g., "started", "completed").
     */
    suspend fun publishTtsStatus(status: String) = withContext(Dispatchers.IO) {
        runCatching {
            val payload = json.encodeToString(MqttStatus(status = status))
            publishMessage(TTS_LISTENER_TOPIC, payload)
        }.onFailure {
            Timber.e(it, "Failed to publish TTS status")
        }
    }

    /**
     * Publishes a prompt for patrol announcement to be handled by an LLM/subscriber.
     */
    suspend fun publishPatrolAnnouncementPrompt() = withContext(Dispatchers.IO) {
        runCatching {
            val prompt =
                "Kündige auf kreative, freundliche, lustige und kurze Art eine automatische Kontrollfahrt an." +
                    " Sage, dass Temi jetzt eine Kontrollfahrt startet. Maximal zwei kurze Sätze.".trimIndent()

            val payload = json.encodeToString(MqttAsr(prompt))
            publishMessage("$BASE_TOPIC/asrListener", payload)
        }.onFailure {
            Timber.e(it, "Failed to publish patrol announcement prompt")
        }
    }

    /**
     * Clears the recorded traffic events.
     */
    fun clearTrafficEvents() {
        _trafficEvents.value = emptyList()
    }

    /**
     * Publishes a raw message to the given topic.
     *
     * @param topic The target topic.
     * @param payload The message payload.
     */
    fun publishMessage(topic: String, payload: String) {
        runCatching {
            client.publishWith().topic(topic).payload(payload.toByteArray()).send()
        }.onFailure {
            Timber.e(it, "Failed to publish raw message to %s", topic)
        }
        appendTrafficEvent(MqttTrafficDirection.OUTBOUND, topic, payload)
    }

    private fun appendTrafficEvent(direction: MqttTrafficDirection, topic: String, payload: String) {
        val event = MqttTrafficEvent(
            timestampEpochMillis = Instant.now().toEpochMilli(),
            direction = direction,
            topic = topic,
            payload = payload
        )

        _trafficEvents.value = (_trafficEvents.value + event).takeLast(MAX_TRAFFIC_EVENTS)
    }

    /**
     * Disconnects from the MQTT broker.
     */
    fun disconnect() {
        runCatching {
            client.disconnect()
        }.onFailure {
            Timber.e(it, "Error during MQTT disconnect")
        }
    }
}
