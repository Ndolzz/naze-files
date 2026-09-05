package com.naze.files.data.settings

import com.naze.files.data.model.SortOrder
import com.naze.files.data.model.ViewMode
import com.naze.files.ui.theme.NazeThemeMode

data class NazeSettings(
    val themeMode: NazeThemeMode = NazeThemeMode.Dark,
    val defaultViewMode: ViewMode = ViewMode.LIST,
    val defaultSortOrder: SortOrder = SortOrder.NAME_ASC,
    val foldersFirst: Boolean = true,
    val showHiddenFiles: Boolean = false,
    val showFileExtensions: Boolean = true,
    val confirmDelete: Boolean = true,
)
