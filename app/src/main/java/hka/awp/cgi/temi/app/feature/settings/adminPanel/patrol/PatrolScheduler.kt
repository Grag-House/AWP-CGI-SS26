package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

import kotlinx.coroutines.*
import timber.log.Timber
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random

class PatrolScheduler(
    private val scope: CoroutineScope,
    private val onTriggerPatrol: (List<String>) -> Unit
) {
    private var schedulerJob: Job? = null

    fun updateSchedule(settings: PatrolSettings) {
        schedulerJob?.cancel()

        if (!settings.isEnabled || settings.route.isEmpty()) {
            Timber.d("Scheduler deaktiviert oder Route leer.")
            return
        }

        schedulerJob = scope.launch {
            when (settings.mode) {
                PatrolMode.RANDOM -> runRandomSchedule(settings)
                PatrolMode.FIXED -> runFixedSchedule(settings)
            }
        }
    }

    fun cancel() {
        schedulerJob?.cancel()
        schedulerJob = null
    }

    private suspend fun runRandomSchedule(settings: PatrolSettings) {
        while (scope.isActive) {
            val min = settings.minMinutes.coerceAtLeast(1)
            val max = settings.maxMinutes.coerceAtLeast(min)
            val delayMinutes = Random.nextInt(from = min, until = max + 1)

            Timber.d("Nächste zufällige Kontrollfahrt in $delayMinutes Minuten.")
            delay(delayMinutes * 60_000L)

            onTriggerPatrol(settings.route)
        }
    }

    private suspend fun runFixedSchedule(settings: PatrolSettings) {
        if (settings.hours.isEmpty()) return

        while (scope.isActive) {
            val nextRun = getNextFullHourRun(settings.hours)
            val delayMs = ChronoUnit.MILLIS.between(LocalDateTime.now(), nextRun).coerceAtLeast(0)

            Timber.d("Nächste feste Kontrollfahrt um $nextRun in ${delayMs / 1000} Sekunden.")
            delay(delayMs)

            onTriggerPatrol(settings.route)
        }
    }

    private fun getNextFullHourRun(hours: Set<Int>): LocalDateTime {
        val validHours = hours.filter { it in 0..23 }.sorted()
        val now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)

        val nextToday = validHours
            .map { now.withHour(it) }
            .firstOrNull { it.isAfter(LocalDateTime.now()) }

        if (nextToday != null) return nextToday

        return now.plusDays(1).withHour(validHours.firstOrNull() ?: 0)
    }
}
