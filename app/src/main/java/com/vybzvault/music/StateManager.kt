package com.vybzvault.music

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class SavedPlaybackState(
    val lastPlayedSongId: Long,
    val queueSongIds: List<Long>,
    val queueIndex: Int,
    val playbackPositionMs: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: RepeatMode,
    val wasPlaying: Boolean,
    val savedAtEpochMs: Long
)

data class ListeningStreakInsights(
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val activeDaysThisWeek: Int,
    val nextMilestoneDays: Int
)

object StateManager {

    private const val PREFS_NAME = "playback_state_store"
    private const val KEY_PAYLOAD = "playback_state_payload"
    private const val KEY_LISTENING_DAY_KEYS = "listening_day_keys"
    private const val MAX_LISTENING_DAYS = 90

    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    suspend fun savePlaybackState(state: SavedPlaybackState) {
        withContext(Dispatchers.IO) {
            prefs().edit {
                putString(KEY_PAYLOAD, encode(state))
                putString(KEY_LISTENING_DAY_KEYS, encodeListeningDays(recordListeningDay(getListeningDayKeys())))
            }
        }
    }

    suspend fun restorePlaybackState(): SavedPlaybackState? {
        return withContext(Dispatchers.IO) {
            val raw = prefs().getString(KEY_PAYLOAD, null) ?: return@withContext null
            decode(raw)
        }
    }

    suspend fun clearPlaybackState() {
        withContext(Dispatchers.IO) {
            prefs().edit {
                remove(KEY_PAYLOAD)
            }
        }
    }

    suspend fun getListeningStreakDays(nowEpochMs: Long = System.currentTimeMillis()): Int {
        return withContext(Dispatchers.IO) {
            calculateStreak(getListeningDayKeys(), nowEpochMs)
        }
    }

    suspend fun getListeningStreakInsights(nowEpochMs: Long = System.currentTimeMillis()): ListeningStreakInsights {
        return withContext(Dispatchers.IO) {
            val dayKeys = getListeningDayKeys()
            val current = calculateStreak(dayKeys, nowEpochMs)
            val best = calculateBestStreak(dayKeys)
            val activeThisWeek = calculateActiveDaysThisWeek(dayKeys, nowEpochMs)
            val milestone = nextMilestone(current)
            ListeningStreakInsights(
                currentStreakDays = current,
                bestStreakDays = best,
                activeDaysThisWeek = activeThisWeek,
                nextMilestoneDays = milestone
            )
        }
    }

    private fun prefs() = requireNotNull(appContext) {
        "StateManager is not initialized"
    }.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun encode(state: SavedPlaybackState): String {
        return JSONObject()
            .put("lastPlayedSongId", state.lastPlayedSongId)
            .put("queueSongIds", JSONArray().apply { state.queueSongIds.forEach(::put) })
            .put("queueIndex", state.queueIndex)
            .put("playbackPositionMs", state.playbackPositionMs)
            .put("shuffleEnabled", state.shuffleEnabled)
            .put("repeatMode", state.repeatMode.name)
            .put("wasPlaying", state.wasPlaying)
            .put("savedAtEpochMs", state.savedAtEpochMs)
            .toString()
    }

    private fun decode(raw: String): SavedPlaybackState? {
        return runCatching {
            val json = JSONObject(raw)
            val queueJson = json.optJSONArray("queueSongIds") ?: JSONArray()
            val queue = buildList {
                for (index in 0 until queueJson.length()) {
                    add(queueJson.getLong(index))
                }
            }

            SavedPlaybackState(
                lastPlayedSongId = json.optLong("lastPlayedSongId", -1L),
                queueSongIds = queue,
                queueIndex = json.optInt("queueIndex", -1),
                playbackPositionMs = json.optLong("playbackPositionMs", 0L),
                shuffleEnabled = json.optBoolean("shuffleEnabled", false),
                repeatMode = RepeatMode.entries.firstOrNull {
                    it.name == json.optString("repeatMode", RepeatMode.ALL.name)
                } ?: RepeatMode.ALL,
                wasPlaying = json.optBoolean("wasPlaying", false),
                savedAtEpochMs = json.optLong("savedAtEpochMs", 0L)
            )
        }.getOrNull()
    }

    private fun getListeningDayKeys(): List<Int> {
        val raw = prefs().getString(KEY_LISTENING_DAY_KEYS, "[]") ?: "[]"
        return runCatching {
            val json = JSONArray(raw)
            buildList(json.length()) {
                for (index in 0 until json.length()) {
                    add(json.optInt(index, -1))
                }
            }.filter { it > 0 }
        }.getOrElse { emptyList() }
    }

    private fun recordListeningDay(existing: List<Int>, nowEpochMs: Long = System.currentTimeMillis()): List<Int> {
        val today = dayKey(nowEpochMs)
        val merged = linkedSetOf(today)
        existing.forEach { merged.add(it) }
        return merged.take(MAX_LISTENING_DAYS)
    }

    private fun encodeListeningDays(dayKeys: List<Int>): String {
        return JSONArray().apply { dayKeys.forEach(::put) }.toString()
    }

    private fun calculateStreak(dayKeys: List<Int>, nowEpochMs: Long): Int {
        if (dayKeys.isEmpty()) return 0
        val daySet = dayKeys.toSet()
        var streak = 0
        val calendar = Calendar.getInstance().apply { timeInMillis = nowEpochMs }

        while (true) {
            val key = dayKey(calendar.timeInMillis)
            if (!daySet.contains(key)) break
            streak += 1
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        return streak
    }

    private fun calculateBestStreak(dayKeys: List<Int>): Int {
        if (dayKeys.isEmpty()) return 0
        val sorted = dayKeys.distinct().sortedDescending()
        var best = 1
        var current = 1
        for (index in 1 until sorted.size) {
            if (daysBetween(sorted[index - 1], sorted[index]) == 1) {
                current += 1
                if (current > best) best = current
            } else {
                current = 1
            }
        }
        return best
    }

    private fun calculateActiveDaysThisWeek(dayKeys: List<Int>, nowEpochMs: Long): Int {
        if (dayKeys.isEmpty()) return 0
        val calendar = Calendar.getInstance().apply { timeInMillis = nowEpochMs }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val delta = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_YEAR, -delta)
        val weekStart = dayKey(calendar.timeInMillis)
        val weekEnd = dayKey(nowEpochMs)
        return dayKeys.distinct().count { it in weekStart..weekEnd }
    }

    private fun nextMilestone(currentStreak: Int): Int {
        val milestones = intArrayOf(3, 5, 7, 10, 14, 21, 30, 45, 60, 90)
        return milestones.firstOrNull { it > currentStreak } ?: (currentStreak + 7)
    }

    private fun daysBetween(olderDayKey: Int, newerDayKey: Int): Int {
        val older = calendarFromDayKey(olderDayKey)
        val newer = calendarFromDayKey(newerDayKey)
        val millis = older.timeInMillis - newer.timeInMillis
        return (millis / (24L * 60L * 60L * 1000L)).toInt()
    }

    private fun calendarFromDayKey(key: Int): Calendar {
        val year = key / 10_000
        val month = (key / 100) % 100
        val day = key % 100
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, (month - 1).coerceAtLeast(0))
            set(Calendar.DAY_OF_MONTH, day.coerceAtLeast(1))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun dayKey(epochMs: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = epochMs }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return year * 10_000 + month * 100 + day
    }
}
