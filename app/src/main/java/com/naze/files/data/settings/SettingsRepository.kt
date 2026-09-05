package com.naze.files.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.naze.files.data.model.SortOrder
import com.naze.files.data.model.ViewMode
import com.naze.files.ui.theme.NazeThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "naze_settings")

private val THEME_MODE = stringPreferencesKey("theme_mode")
private val VIEW_MODE = stringPreferencesKey("view_mode")
private val SORT_ORDER = stringPreferencesKey("sort_order")
private val FOLDERS_FIRST = booleanPreferencesKey("folders_first")
private val SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
private val SHOW_EXTENSIONS = booleanPreferencesKey("show_extensions")
private val CONFIRM_DELETE = booleanPreferencesKey("confirm_delete")

class SettingsRepository(private val context: Context) {

    val settings: Flow<NazeSettings> = context.settingsDataStore.data.map { prefs ->
        NazeSettings(
            themeMode = prefs[THEME_MODE]?.let { runCatching { NazeThemeMode.valueOf(it) }.getOrNull() } ?: NazeThemeMode.Dark,
            defaultViewMode = prefs[VIEW_MODE]?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() } ?: ViewMode.LIST,
            defaultSortOrder = prefs[SORT_ORDER]?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() } ?: SortOrder.NAME_ASC,
            foldersFirst = prefs[FOLDERS_FIRST] ?: true,
            showHiddenFiles = prefs[SHOW_HIDDEN] ?: false,
            showFileExtensions = prefs[SHOW_EXTENSIONS] ?: true,
            confirmDelete = prefs[CONFIRM_DELETE] ?: true,
        )
    }

    suspend fun setThemeMode(mode: NazeThemeMode) = context.settingsDataStore.edit { it[THEME_MODE] = mode.name }
    suspend fun setDefaultViewMode(mode: ViewMode) = context.settingsDataStore.edit { it[VIEW_MODE] = mode.name }
    suspend fun setDefaultSortOrder(order: SortOrder) = context.settingsDataStore.edit { it[SORT_ORDER] = order.name }
    suspend fun setFoldersFirst(value: Boolean) = context.settingsDataStore.edit { it[FOLDERS_FIRST] = value }
    suspend fun setShowHiddenFiles(value: Boolean) = context.settingsDataStore.edit { it[SHOW_HIDDEN] = value }
    suspend fun setShowFileExtensions(value: Boolean) = context.settingsDataStore.edit { it[SHOW_EXTENSIONS] = value }
    suspend fun setConfirmDelete(value: Boolean) = context.settingsDataStore.edit { it[CONFIRM_DELETE] = value }
}
