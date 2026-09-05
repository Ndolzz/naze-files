package com.naze.files.data.recent

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.recentDataStore by preferencesDataStore(name = "naze_recent")
private val RECENT_JSON = stringPreferencesKey("recent_entries")
private const val MAX_ENTRIES = 50

data class RecentEntry(val path: String, val openedAtMillis: Long)

/** Keeps a small, capped history of opened files so the database/prefs stay light. */
class RecentFilesRepository(private val context: Context) {

    val recentEntries: Flow<List<RecentEntry>> = context.recentDataStore.data.map { prefs ->
        parse(prefs[RECENT_JSON])
    }

    suspend fun recordOpened(path: String) {
        context.recentDataStore.edit { prefs ->
            val current = parse(prefs[RECENT_JSON]).toMutableList()
            current.removeAll { it.path == path }
            current.add(0, RecentEntry(path, System.currentTimeMillis()))
            val trimmed = current.take(MAX_ENTRIES)
            prefs[RECENT_JSON] = serialize(trimmed)
        }
    }

    suspend fun clear() {
        context.recentDataStore.edit { prefs -> prefs[RECENT_JSON] = serialize(emptyList()) }
    }

    private fun parse(json: String?): List<RecentEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                RecentEntry(o.getString("path"), o.getLong("openedAt"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serialize(entries: List<RecentEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject().apply { put("path", entry.path); put("openedAt", entry.openedAtMillis) })
        }
        return array.toString()
    }
}
