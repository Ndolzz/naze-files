package com.naze.files.ui.browser.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.naze.files.data.model.SortOrder
import com.naze.files.data.model.SortPreference

private data class SortOption(val order: SortOrder, val label: String)

private val sortOptions = listOf(
    SortOption(SortOrder.NAME_ASC, "Name A-Z"),
    SortOption(SortOrder.NAME_DESC, "Name Z-A"),
    SortOption(SortOrder.DATE_NEWEST, "Date newest"),
    SortOption(SortOrder.DATE_OLDEST, "Date oldest"),
    SortOption(SortOrder.SIZE_LARGEST, "Size largest"),
    SortOption(SortOrder.SIZE_SMALLEST, "Size smallest"),
    SortOption(SortOrder.TYPE, "Type"),
)

@Composable
fun SortMenu(
    current: SortPreference,
    onOrderSelected: (SortOrder) -> Unit,
    onToggleFoldersFirst: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(imageVector = Icons.Filled.SwapVert, contentDescription = "Sort")
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        sortOptions.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.label) },
                leadingIcon = {
                    if (current.order == option.order) {
                        Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                    }
                },
                onClick = {
                    onOrderSelected(option.order)
                    expanded = false
                },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Folders first") },
            leadingIcon = {
                if (current.foldersFirst) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                }
            },
            onClick = onToggleFoldersFirst,
        )
    }
}
