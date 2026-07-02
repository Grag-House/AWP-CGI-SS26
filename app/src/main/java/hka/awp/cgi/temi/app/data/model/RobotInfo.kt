package hka.awp.cgi.temi.app.data.model

/**
 * Data class representing various hardware and software version identifiers for the Temi robot.
 *
 * @property ip The current network IP address of the robot.
 * @property model The hardware model identifier.
 * @property serial The unique serial number of the robot.
 * @property appVersion The version name of this application.
 * @property roboxVersion The version of the robot's operating system (Robox).
 * @property launcherVersion The version of the Temi launcher application.
 */
data class RobotInfo(
    val ip: String,
    val model: String,
    val serial: String,
    val appVersion: String,
    val roboxVersion: String,
    val launcherVersion: String,
)
