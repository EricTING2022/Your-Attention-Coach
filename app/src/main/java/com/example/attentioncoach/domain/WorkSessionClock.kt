package com.example.attentioncoach.domain

object WorkSessionClock {
    private const val MILLIS_PER_SECOND = 1_000L
    private const val MILLIS_PER_MINUTE = 60_000L
    const val PAUSE_LIMIT_MILLIS = 180_000L

    fun activeMillisAt(work: ActiveWork, nowMillis: Long): Long {
        return if (work.isPaused) {
            work.accumulatedActiveMillis
        } else {
            work.accumulatedActiveMillis + (nowMillis - work.startedAtMillis).coerceAtLeast(0L)
        }
    }

    fun workTimerText(plannedDurationMinutes: Int, activeMillis: Long): String {
        val plannedMillis = plannedDurationMinutes.coerceAtLeast(0) * MILLIS_PER_MINUTE
        val remainingMillis = plannedMillis - activeMillis.coerceAtLeast(0L)
        return if (remainingMillis >= 0L) {
            formatMinutesSeconds(remainingMillis)
        } else {
            "+${formatMinutesSeconds(-remainingMillis)}"
        }
    }

    fun pauseTimerText(pauseStartedAtMillis: Long, nowMillis: Long): String {
        val elapsed = (nowMillis - pauseStartedAtMillis).coerceAtLeast(0L)
        return formatMinutesSeconds((PAUSE_LIMIT_MILLIS - elapsed).coerceAtLeast(0L))
    }

    fun focusMinutesFromMillis(activeMillis: Long): Int {
        if (activeMillis <= 0L) return 0
        return ((activeMillis + MILLIS_PER_MINUTE - 1L) / MILLIS_PER_MINUTE).toInt()
    }

    private fun formatMinutesSeconds(millis: Long): String {
        val totalSeconds = (millis.coerceAtLeast(0L) + MILLIS_PER_SECOND - 1L) / MILLIS_PER_SECOND
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%02d:%02d".format(minutes, seconds)
    }
}
