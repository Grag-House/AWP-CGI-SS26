package hka.awp.cgi.temi.app.feature.mqtt

import kotlinx.serialization.Serializable

/**
 * Represents a command received via MQTT.
 * Matches the 'payloadObject' and 'speed' fields from the Dart protocol.
 */
@Serializable
data class MqttCommand(
    val payloadObject: String? = null,
    val speed: String? = null
)

/**
 * Represents a status update sent via MQTT.
 */
@Serializable
data class MqttStatus(
    val status: String,
    val text: String? = null
)

/**
 * Represents an ASR (Speech Recognition) result sent via MQTT.
 */
@Serializable
data class MqttAsr(
    val text: String
)

/**
 * Represents a list response sent via MQTT.
 */
@Serializable
data class MqttLocations(
    val payloadObject: List<String>
)
