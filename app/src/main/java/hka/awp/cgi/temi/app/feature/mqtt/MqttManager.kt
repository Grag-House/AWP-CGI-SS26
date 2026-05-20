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

    private var lastGotoLocation: String? = null
    private var lastGotoAtMs: Long = 0L
    private var activeGotoLocation: String? = null
    private var activeGotoSinceMs: Long = 0L
    private var lastSpeakText: String? = null
    private var lastSpeakAtMs: Long = 0L
    private var messageCounter: Long = 0L

    companion object {
        private const val BASE_TOPIC = "innovation_lab/karlsruhe/temi"

        private const val GOTO_TOPIC = "$BASE_TOPIC/temi_goto/set"
        private const val WAKE_UP_TOPIC = "$BASE_TOPIC/temi_wake_up/set"
        private const val FOLLOW_TOPIC = "$BASE_TOPIC/temi_follow/set"
        private const val STOP_MOVEMENT_TOPIC = "$BASE_TOPIC/temi_stop_movement/set"
        private const val GET_LOCATIONS_TOPIC = "$BASE_TOPIC/temi_get_locations"
        private const val GET_LOCATIONS_SET_TOPIC = "$BASE_TOPIC/temi_get_locations/set"
        private const val PLAY_SEQUENCE_TOPIC = "$BASE_TOPIC/temi_playsequence/set"
        private const val TILT_ANGLE_TOPIC = "$BASE_TOPIC/temi_tilt_angle/set"

        private const val SPEAK_TOPIC = "$BASE_TOPIC/temi_speak/set"
        private const val LOCATIONS_EVENTS_TOPIC = "$BASE_TOPIC/onlocationschangevents"
        private const val TTS_LISTENER_TOPIC = "$BASE_TOPIC/ttsListener"
        private const val DUPLICATE_GOTO_WINDOW_MS = 4_000L
        private const val ACTIVE_GOTO_LOCK_MS = 120_000L
        private const val DUPLICATE_SPEAK_WINDOW_MS = 3_000L
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
            client.subscribeWith().topicFilter(GET_LOCATIONS_SET_TOPIC).send()
            client.subscribeWith().topicFilter(PLAY_SEQUENCE_TOPIC).send()
            client.subscribeWith().topicFilter(TILT_ANGLE_TOPIC).send()
            Timber.d(
                "MQTT subscriptions active: %s",
                listOf(
                    GOTO_TOPIC,
                    SPEAK_TOPIC,
                    WAKE_UP_TOPIC,
                    FOLLOW_TOPIC,
                    STOP_MOVEMENT_TOPIC,
                    GET_LOCATIONS_TOPIC,
                    GET_LOCATIONS_SET_TOPIC,
                    PLAY_SEQUENCE_TOPIC,
                    TILT_ANGLE_TOPIC
                ).joinToString()
            )

            // Message loop using the 'publishes' stream
            val publishes = client.publishes(MqttGlobalPublishFilter.ALL)

            while (isActive) {
                // In a production app, you might want to use receive(timeout, unit) to check isActive more frequently.
                val publish = publishes.receive()
                val topic = publish.topic.toString()
                val payload = publish.payloadAsBytes.toString(Charsets.UTF_8)
                messageCounter += 1
                val messageId = messageCounter

                Timber.d("[MQTT #%d] recv topic='%s' payload=%s", messageId, topic, payload)

                when (topic) {
                    GOTO_TOPIC -> handleGoto(payload, messageId)
                    SPEAK_TOPIC -> handleSpeak(payload, messageId)
                    WAKE_UP_TOPIC -> handleWakeUp(messageId)
                    FOLLOW_TOPIC -> handleFollow(messageId)
                    STOP_MOVEMENT_TOPIC -> handleStopMovement(messageId)
                    GET_LOCATIONS_TOPIC,
                    GET_LOCATIONS_SET_TOPIC -> publishLocations(messageId)
                    PLAY_SEQUENCE_TOPIC -> handlePlaySequence(payload, messageId)
                    TILT_ANGLE_TOPIC -> handleTiltAngle(payload, messageId)
                    else -> Timber.w("[MQTT #%d] no handler for topic='%s'", messageId, topic)
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "MQTT error or connection failed")
        } finally {
            disconnect()
        }
    }

    private fun handleGoto(payload: String, messageId: Long) {
        try {
            val cmd = json.decodeFromString<MqttCommand>(payload)
            cmd.payloadObject?.let { location ->
                val normalizedLocation = location.trim()
                if (normalizedLocation.isEmpty()) {
                    Timber.w("[MQTT #%d] ignore goto: empty payloadObject", messageId)
                    return
                }

                val now = System.currentTimeMillis()
                val delta = now - lastGotoAtMs
                val isDuplicate =
                    lastGotoLocation == normalizedLocation && now - lastGotoAtMs < DUPLICATE_GOTO_WINDOW_MS
                if (isDuplicate) {
                    Timber.d(
                        "[MQTT #%d] ignore duplicate goto='%s' deltaMs=%d (sending ack anyway)",
                        messageId,
                        normalizedLocation,
                        delta
                    )
                    publishStatusBlocking(status = "going", text = normalizedLocation)
                    return
                }

                val activeDelta = now - activeGotoSinceMs
                val isSameActiveTarget =
                    activeGotoLocation == normalizedLocation && activeDelta < ACTIVE_GOTO_LOCK_MS
                if (isSameActiveTarget) {
                    Timber.d(
                        "[MQTT #%d] ignore goto='%s': active target lock deltaMs=%d",
                        messageId,
                        normalizedLocation,
                        activeDelta
                    )
                    return
                }

                lastGotoLocation = normalizedLocation
                lastGotoAtMs = now
                activeGotoLocation = normalizedLocation
                activeGotoSinceMs = now

                Timber.d("[MQTT #%d] execute goto='%s'", messageId, normalizedLocation)
                robot?.goTo(normalizedLocation)

                // Immediate ack helps external controllers avoid command retries.
                publishStatusBlocking(status = "going", text = normalizedLocation)
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Error parsing temi_goto payload: $payload")
        }
    }

    private fun publishStatusBlocking(status: String, text: String? = null) {
        try {
            val payload = json.encodeToString(MqttStatus(status, text))
            Timber.d("[MQTT publish] topic='%s/onlocationsstatuschangevents' payload=%s", BASE_TOPIC, payload)
            client.publishWith()
                .topic("$BASE_TOPIC/onlocationsstatuschangevents")
                .payload(payload.toByteArray())
                .send()

            val normalizedStatus = status.lowercase()
            val normalizedText = text?.trim()
            if (normalizedStatus in setOf("complete", "abort", "cancel", "cancelled") &&
                !normalizedText.isNullOrEmpty() && normalizedText == activeGotoLocation
            ) {
                Timber.d(
                    "[MQTT publish] clear active goto lock for '%s' after status='%s'",
                    normalizedText,
                    normalizedStatus
                )
                activeGotoLocation = null
                activeGotoSinceMs = 0L
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Failed to publish status")
        }
    }

    private fun handleSpeak(payload: String, messageId: Long) {
        try {
            val cmd = json.decodeFromString<MqttCommand>(payload)
            cmd.payloadObject?.let { text ->
                val normalizedText = text.trim()
                if (normalizedText.isEmpty()) {
                    Timber.w("[MQTT #%d] ignore speak: empty payloadObject", messageId)
                    return
                }

                val now = System.currentTimeMillis()
                val isDuplicateSpeak =
                    lastSpeakText == normalizedText && (now - lastSpeakAtMs) < DUPLICATE_SPEAK_WINDOW_MS
                if (isDuplicateSpeak) {
                    Timber.d(
                        "[MQTT #%d] ignore duplicate speak len=%d deltaMs=%d",
                        messageId,
                        normalizedText.length,
                        now - lastSpeakAtMs
                    )
                    return
                }

                lastSpeakText = normalizedText
                lastSpeakAtMs = now

                Timber.d("[MQTT #%d] execute speak len=%d", messageId, normalizedText.length)
                robot?.speak(TtsRequest.create(speech = normalizedText, isShowOnConversationLayer = false))
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Error parsing temi_speak payload: $payload")
        }
    }

    private fun handleWakeUp(messageId: Long) {
        Timber.d("[MQTT #%d] execute wakeUp", messageId)
        robot?.wakeup()
    }

    private fun handleFollow(messageId: Long) {
        Timber.d("[MQTT #%d] execute beWithMe", messageId)
        robot?.beWithMe()
    }

    private fun handleStopMovement(messageId: Long) {
        Timber.d("[MQTT #%d] execute stopMovement", messageId)
        robot?.stopMovement()
    }

    private fun publishLocations(messageId: Long) {
        try {
            val locations = robot?.locations.orEmpty()
            val payload = json.encodeToString(MqttLocations(payloadObject = locations))

            client.publishWith()
                .topic(LOCATIONS_EVENTS_TOPIC)
                .payload(payload.toByteArray())
                .send()

            Timber.d("[MQTT #%d] published %d locations to %s", messageId, locations.size, LOCATIONS_EVENTS_TOPIC)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Failed to publish locations")
        }
    }

    private fun handlePlaySequence(payload: String, messageId: Long) {
        try {
            val cmd = json.decodeFromString<MqttCommand>(payload)
            cmd.payloadObject?.let { sequence ->
                Timber.d("[MQTT #%d] execute playSequence='%s'", messageId, sequence)
                robot?.playSequence(sequence)
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Error parsing temi_playsequence payload: $payload")
        }
    }

    private fun handleTiltAngle(payload: String, messageId: Long) {
        try {
            val cmd = json.decodeFromString<MqttCommand>(payload)
            cmd.payloadObject?.toIntOrNull()?.let { angle ->
                Timber.d("[MQTT #%d] execute tiltAngle=%d", messageId, angle)
                robot?.tiltAngle(angle)
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Error parsing temi_tilt_angle payload: $payload")
        }
    }

    suspend fun publishAsr(text: String) = withContext(Dispatchers.IO) {
        try {
            val payload = json.encodeToString(MqttAsr(text))
            Timber.d("Publishing ASR result: '$text' to $BASE_TOPIC/asrListener")
            client.publishWith()
                .topic("$BASE_TOPIC/asrListener")
                .payload(payload.toByteArray())
                .send()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Failed to publish ASR")
        }
    }

    suspend fun publishStatus(status: String, text: String? = null) = withContext(Dispatchers.IO) {
        publishStatusBlocking(status = status, text = text)
    }

    suspend fun publishTtsStatus(status: String) = withContext(Dispatchers.IO) {
        try {
            val payload = json.encodeToString(MqttStatus(status = status))
            client.publishWith()
                .topic(TTS_LISTENER_TOPIC)
                .payload(payload.toByteArray())
                .send()
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
