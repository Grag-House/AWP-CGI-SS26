package hka.awp.cgi.temi.app.data.model

/**
 * Represents the configuration settings for the robot's patrol functionality.
 *
 * @property isEnabled Whether the automated patrol system is currently active.
 * @property mode The [PatrolMode] determining how locations are selected.
 * @property minMinutes The minimum time interval (in minutes) between patrol sessions.
 * @property maxMinutes The maximum time interval (in minutes) between patrol sessions.
 * @property hours The specific hours of the day (0-23) during which patrols are permitted.
 * @property route The list of location names that define the patrol path.
 */
data class PatrolSettings(
    val isEnabled: Boolean = false,
    val mode: PatrolMode = PatrolMode.RANDOM,
    val minMinutes: Int = 40,
    val maxMinutes: Int = 60,
    val hours: Set<Int> = emptySet(),
    val route: List<String> = emptyList()
)
