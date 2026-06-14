package com.vybzvault.music

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

internal fun encodeLongList(values: List<Long>): String {
    return JSONArray().apply { values.forEach { put(it) } }.toString()
}

internal fun decodeLongList(raw: String): List<Long> {
    val array = JSONArray(raw)
    return buildList(array.length()) {
        for (index in 0 until array.length()) {
            add(array.getLong(index))
        }
    }
}

internal fun encodeStringList(values: List<String>): String {
    return JSONArray().apply { values.forEach { put(it) } }.toString()
}

internal fun decodeStringList(raw: String): List<String> {
    val array = JSONArray(raw)
    return buildList(array.length()) {
        for (index in 0 until array.length()) {
            add(array.getString(index))
        }
    }
}

internal fun encodePlaylists(playlists: List<Playlist>): String {
    return JSONArray().apply {
        playlists.forEach { playlist ->
            put(
                JSONObject()
                    .put("name", playlist.name)
                    .put("songIds", JSONArray().apply { playlist.songIds.forEach { put(it) } })
            )
        }
    }.toString()
}

internal fun decodePlaylists(raw: String): List<Playlist> {
    val array = JSONArray(raw)
    return buildList(array.length()) {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val songIdsJson = item.optJSONArray("songIds") ?: JSONArray()
            val songIds = buildList(songIdsJson.length()) {
                for (songIndex in 0 until songIdsJson.length()) {
                    add(songIdsJson.getLong(songIndex))
                }
            }
            add(Playlist(name = item.optString("name", ""), songIds = songIds))
        }
    }
}

internal fun encodePlayCounts(playCounts: Map<Long, Int>): String {
    return JSONObject().apply {
        playCounts.forEach { (songId, count) -> put(songId.toString(), count) }
    }.toString()
}

internal fun decodePlayCounts(raw: String): Map<Long, Int> {
    val jsonObject = JSONObject(raw)
    val result = linkedMapOf<Long, Int>()
    val keys = jsonObject.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        key.toLongOrNull()?.let { songId ->
            result[songId] = jsonObject.optInt(key, 0)
        }
    }
    return result
}

internal fun encodeSongTransitions(transitions: Map<Long, Map<Long, Int>>): String {
    return JSONObject().apply {
        transitions.forEach { (fromSongId, toMap) ->
            val nested = JSONObject().apply {
                toMap.forEach { (toSongId, count) -> put(toSongId.toString(), count) }
            }
            put(fromSongId.toString(), nested)
        }
    }.toString()
}

internal fun decodeSongTransitions(raw: String): Map<Long, Map<Long, Int>> {
    val root = JSONObject(raw)
    val result = linkedMapOf<Long, Map<Long, Int>>()
    val fromKeys = root.keys()

    while (fromKeys.hasNext()) {
        val fromKey = fromKeys.next()
        val fromSongId = fromKey.toLongOrNull() ?: continue
        val nested = root.optJSONObject(fromKey) ?: continue
        val toMap = linkedMapOf<Long, Int>()
        val toKeys = nested.keys()
        while (toKeys.hasNext()) {
            val toKey = toKeys.next()
            val toSongId = toKey.toLongOrNull() ?: continue
            toMap[toSongId] = nested.optInt(toKey, 0)
        }
        result[fromSongId] = toMap
    }

    return result
}

internal fun encodeIntMap(values: Map<Long, Int>): String {
    return JSONObject().apply {
        values.forEach { (key, value) -> put(key.toString(), value) }
    }.toString()
}

internal fun decodeIntMap(raw: String): Map<Long, Int> {
    val json = JSONObject(raw)
    val result = linkedMapOf<Long, Int>()
    val keys = json.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        key.toLongOrNull()?.let { result[it] = json.optInt(key, 0) }
    }
    return result
}

internal fun dayKey(epochMs: Long): Int {
    val calendar = Calendar.getInstance().apply { timeInMillis = epochMs }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return year * 10_000 + month * 100 + day
}

