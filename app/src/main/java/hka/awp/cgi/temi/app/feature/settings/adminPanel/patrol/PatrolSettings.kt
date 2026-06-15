package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

enum class PatrolMode {
    RANDOM,
    FIXED
}

data class PatrolSettings(
    val isEnabled: Boolean = false,
    val mode: PatrolMode = PatrolMode.RANDOM,
    val minMinutes: Int = 40,
    val maxMinutes: Int = 60,
    val hours: Set<Int> = emptySet(),
    val route: List<String> = emptyList()
)
