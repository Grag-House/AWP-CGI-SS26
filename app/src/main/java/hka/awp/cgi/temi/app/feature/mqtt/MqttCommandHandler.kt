package hka.awp.cgi.temi.app.feature.mqtt

import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Handles incoming MQTT commands and interacts with the Temi Robot SDK.
 *
 * @property robot The [Robot] instance to control.
 * @property publishMessage Callback to publish a message back to MQTT via [MqttManager].
 */
class MqttCommandHandler(
    private val robot: Robot?,
    private val publishMessage: (topic: String, payload: String) -> Unit
) {
    companion object {
        private const val BASE_TOPIC = "innovation_lab/karlsruhe/temi"

        /** Topic for robot goto commands. */
        const val GOTO_TOPIC = "$BASE_TOPIC/temi_goto/set"

        /** Topic for robot wake up commands. */
        const val WAKE_UP_TOPIC = "$BASE_TOPIC/temi_wake_up/set"

        /** Topic for robot follow commands. */
        const val FOLLOW_TOPIC = "$BASE_TOPIC/temi_follow/set"

        /** Topic for robot stop movement commands. */
        const val STOP_MOVEMENT_TOPIC = "$BASE_TOPIC/temi_stop_movement/set"

        /** Topic for getting robot locations. */
        const val GET_LOCATIONS_TOPIC = "$BASE_TOPIC/temi_get_locations"

        /** Topic for getting robot ready state. */
        const val GET_READY_STATE_TOPIC = "$BASE_TOPIC/temi_get_ready_state"

        /** Topic for playing a sequence. */
        const val PLAY_SEQUENCE_TOPIC = "$BASE_TOPIC/temi_playsequence/set"

        /** Topic for setting tilt angle. */
        const val TILT_ANGLE_TOPIC = "$BASE_TOPIC/temi_tilt_angle/set"

        /** Topic for robot speak commands. */
        const val SPEAK_TOPIC = "$BASE_TOPIC/temi_speak/set"

        private val json = Json { ignoreUnknownKeys = true }
    }

    /**
     * Handles an incoming MQTT message based on its topic.
     *
     * @param topic The MQTT topic the message was received on.
     * @param payload The message payload as a UTF-8 string.
     */
    fun handleMessage(topic: String, payload: String) {
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

    private fun handleGoto(payload: String) {
        runCatching {
            val cmd = json.decodeFromString<MqttCommand>(payload)
            val location = cmd.payloadObject?.trim() ?: return@runCatching
            if (location.isEmpty()) return@runCatching
            robot?.goTo(location)
            publishStatusBlocking(location)
        }.onFailure {
            Timber.e(it, "Error parsing temi_goto payload: %s", payload)
        }
    }

    private fun publishStatusBlocking(location: String? = null) {
        runCatching {
            val status = "going"
            val payload = json.encodeToString(MqttStatus(status, location))
            publishMessage("$BASE_TOPIC/onlocationsstatuschangevents", payload)
        }.onFailure {
            Timber.e(it, "Failed to publish status")
        }
    }

    private fun handleSpeak(payload: String) {
        runCatching {
            val sanitized = sanitizeJsonString(payload)
            val cmd = json.decodeFromString<MqttCommand>(sanitized)
            cmd.payloadObject?.let { text ->
                robot?.speak(TtsRequest.create(speech = text, isShowOnConversationLayer = false))
            }
        }.onFailure {
            Timber.e(it, "Error parsing temi_speak payload: %s", payload)
        }
    }

    private fun publishLocations() {
        runCatching {
            val locations = robot?.locations.orEmpty()
            val payload = json.encodeToString(MqttLocations(payloadObject = locations))
            publishMessage("$GET_LOCATIONS_TOPIC/return", payload)
        }.onFailure {
            Timber.e(it, "Failed to publish locations")
        }
    }

    /**
     * Escapes single backslashes in JSON string that aren't valid escape sequences.
     */
    private fun sanitizeJsonString(input: String): String {
        val validEscapes = setOf('"', '\\', '/', 'b', 'f', 'n', 'r', 't', 'u')
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '\\' && i + 1 < input.length) {
                val next = input[i + 1]
                if (next !in validEscapes) {
                    sb.append("\\\\") // Escape the backslash
                } else {
                    sb.append('\\') // Keep valid escape
                }
            } else if (c == '\\' && i + 1 == input.length) {
                sb.append("\\\\") // Escape trailing backslash
            } else {
                sb.append(c)
            }
            i++
        }
        return sb.toString()
    }

    private fun handleGetReadyState() {
        runCatching {
            publishMessage("$GET_READY_STATE_TOPIC/return", """{"payloadObject":${robot != null}}""")
        }.onFailure {
            Timber.e(it, "Failed to publish ready state")
        }
    }

    private fun handlePlaySequence(payload: String) {
        runCatching {
            val cmd = json.decodeFromString<MqttCommand>(payload)
            cmd.payloadObject?.let { sequence ->
                robot?.playSequence(sequence)
            }
        }.onFailure {
            Timber.e(it, "Error parsing temi_playsequence payload: %s", payload)
        }
    }

    private fun handleTiltAngle(payload: String) {
        runCatching {
            val cmd = json.decodeFromString<MqttCommand>(payload)
            cmd.payloadObject?.toIntOrNull()?.let { angle ->
                robot?.tiltAngle(angle)
            }
        }.onFailure {
            Timber.e(it, "Error parsing temi_tilt_angle payload: %s", payload)
        }
    }
}
