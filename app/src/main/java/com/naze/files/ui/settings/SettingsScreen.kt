package com.naze.files.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naze.files.data.model.SortOrder
import com.naze.files.data.model.ViewMode
import com.naze.files.data.settings.NazeSettings
import com.naze.files.ui.theme.NazeThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: NazeSettings,
    onThemeModeChange: (NazeThemeMode) -> Unit,
    onDefaultViewModeChange: (ViewMode) -> Unit,
    onDefaultSortOrderChange: (SortOrder) -> Unit,
    onFoldersFirstChange: (Boolean) -> Unit,
    onShowHiddenChange: (Boolean) -> Unit,
    onShowExtensionsChange: (Boolean) -> Unit,
    onConfirmDeleteChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("Appearance")
            DropdownRow(
                label = "Theme",
                value = when (settings.themeMode) {
                    NazeThemeMode.Dark -> "Dark"
                    NazeThemeMode.Light -> "Light"
                    NazeThemeMode.System -> "System"
                },
                options = listOf("Dark" to NazeThemeMode.Dark, "Light" to NazeThemeMode.Light, "System" to NazeThemeMode.System),
                onSelected = onThemeModeChange,
            )

            Divider()
            SectionHeader("File Browser")
            DropdownRow(
                label = "Default view",
                value = when (settings.defaultViewMode) {
                    ViewMode.LIST -> "List"
                    ViewMode.GRID -> "Grid"
                    ViewMode.COMPACT -> "Compact"
                },
                options = listOf("List" to ViewMode.LIST, "Grid" to ViewMode.GRID, "Compact" to ViewMode.COMPACT),
                onSelected = onDefaultViewModeChange,
            )
            DropdownRow(
                label = "Default sort",
                value = sortLabel(settings.defaultSortOrder),
                options = SortOrder.entries.map { sortLabel(it) to it },
                onSelected = onDefaultSortOrderChange,
            )
            SwitchRow("Folders first", settings.foldersFirst, onFoldersFirstChange)
            SwitchRow("Show hidden files", settings.showHiddenFiles, onShowHiddenChange)
            SwitchRow("Show file extensions", settings.showFileExtensions, onShowExtensionsChange)
            SwitchRow("Confirm before delete", settings.confirmDelete, onConfirmDeleteChange)

            Divider()
            SectionHeader("About")
            ListItem(headlineContent = { Text("Naze Files") }, supportingContent = { Text("Version 1.0 (Phase 6)") })
            ListItem(
                headlineContent = { Text("Open-source libraries") },
                supportingContent = { Text("AndroidX Jetpack, Media3/ExoPlayer, Coil, java.util.zip") },
            )
        }
    }
}

private fun sortLabel(order: SortOrder): String = when (order) {
    SortOrder.NAME_ASC -> "Name A-Z"
    SortOrder.NAME_DESC -> "Name Z-A"
    SortOrder.DATE_NEWEST -> "Date newest"
    SortOrder.DATE_OLDEST -> "Date oldest"
    SortOrder.SIZE_LARGEST -> "Size largest"
    SortOrder.SIZE_SMALLEST -> "Size smallest"
    SortOrder.TYPE -> "Type"
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownRow(label: String, value: String, options: List<Pair<String, T>>, onSelected: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            TextButton(onClick = { expanded = true }) { Text(value) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (optionLabel, optionValue) ->
                    DropdownMenuItem(
                        text = { Text(optionLabel) },
                        onClick = { expanded = false; onSelected(optionValue) },
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
