package com.naze.files.data.favorites

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore by preferencesDataStore(name = "naze_favorites")
private val FAVORITE_PATHS_KEY = stringSetPreferencesKey("favorite_paths")

/** Persists the set of favorited file/folder paths across app restarts. */
class FavoritesRepository(private val context: Context) {

    val favoritePaths: Flow<Set<String>> =
        context.favoritesDataStore.data.map { prefs -> prefs[FAVORITE_PATHS_KEY] ?: emptySet() }

    suspend fun toggle(path: String) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[FAVORITE_PATHS_KEY] ?: emptySet()
            prefs[FAVORITE_PATHS_KEY] = if (path in current) current - path else current + path
        }
    }

    suspend fun remove(path: String) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[FAVORITE_PATHS_KEY] ?: emptySet()
            prefs[FAVORITE_PATHS_KEY] = current - path
        }
    }
}
