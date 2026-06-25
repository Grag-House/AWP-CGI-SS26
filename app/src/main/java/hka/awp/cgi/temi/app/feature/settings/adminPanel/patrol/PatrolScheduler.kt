package hka.awp.cgi.temi.app.feature.settings.adminPanel.patrol

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random

class PatrolScheduler(
    private val scope: CoroutineScope,
    private val onTriggerPatrol: suspend (List<String>) -> Unit
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

    private suspend fun runRandomSchedule(settings: PatrolSettings) {
        while (scope.isActive) {
            val min = settings.minMinutes.coerceAtLeast(MIN_MINUTES_LIMIT)
            val max = settings.maxMinutes.coerceAtLeast(min)
            val delayMinutes = Random.nextInt(from = min, until = max + 1)
            val totalDelayMs = delayMinutes * MILLIS_PER_MINUTE
            val waitBeforeCountdownMs = (totalDelayMs - COUNTDOWN_MS).coerceAtLeast(0L)

            Timber.d("Nächste zufällige Kontrollfahrt in $delayMinutes Minuten.")
            delay(waitBeforeCountdownMs)
            onTriggerPatrol(settings.route)
        }
    }

    private suspend fun runFixedSchedule(settings: PatrolSettings) {
        if (settings.hours.isEmpty()) return

        while (scope.isActive) {
            val nextRun = getNextFullHourRun(settings.hours)
            val countdownStart = nextRun.minusSeconds(COUNTDOWN_SECONDS)
            val delayMs = ChronoUnit.MILLIS.between(
                LocalDateTime.now(),
                countdownStart
            ).coerceAtLeast(0)

            Timber.d("Nächste feste Kontrollfahrt um $nextRun.")
            delay(delayMs)
            onTriggerPatrol(settings.route)
        }
    }

    private fun getNextFullHourRun(hours: Set<Int>): LocalDateTime {
        val validHours = hours.filter { it in HOUR_RANGE }.sorted()
        val now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)

        val nextToday = validHours
            .map { now.withHour(it) }
            .firstOrNull { it.isAfter(LocalDateTime.now()) }

        return nextToday ?: now.plusDays(1).withHour(validHours.firstOrNull() ?: DEFAULT_START_HOUR)
    }

    private companion object {
        private const val COUNTDOWN_SECONDS = 30L
        private const val COUNTDOWN_MS = COUNTDOWN_SECONDS * 1_000L
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val MIN_MINUTES_LIMIT = 1
        private const val DEFAULT_START_HOUR = 0
        private val HOUR_RANGE = 0..23
    }
}
