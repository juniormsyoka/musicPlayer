package com.vybzvault.music.time

import com.vybzvault.music.home.HomeTimeSnapshot
import java.util.Calendar

interface TimeProvider {
    fun currentSnapshot(): HomeTimeSnapshot
    fun millisUntilNextHour(): Long
}

class SystemTimeProvider : TimeProvider {
    override fun currentSnapshot(): HomeTimeSnapshot {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        val weekend = day == Calendar.SATURDAY || day == Calendar.SUNDAY
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val dayKey = year * 10_000 + month * 100 + dayOfMonth
        return HomeTimeSnapshot(hour24 = hour, isWeekend = weekend, dayKey = dayKey)
    }

    override fun millisUntilNextHour(): Long {
        val calendar = Calendar.getInstance()
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)
        val millis = calendar.get(Calendar.MILLISECOND)
        val elapsedInHour = ((minutes * 60L) + seconds) * 1000L + millis
        val millisInHour = 60L * 60L * 1000L
        return (millisInHour - elapsedInHour).coerceAtLeast(1_000L)
    }
}


