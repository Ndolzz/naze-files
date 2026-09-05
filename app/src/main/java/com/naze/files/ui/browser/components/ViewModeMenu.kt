package com.naze.files.ui.browser.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.naze.files.data.model.ViewMode

@Composable
fun ViewModeMenu(current: ViewMode, onSelected: (ViewMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(imageVector = Icons.Filled.ViewModule, contentDescription = "View mode")
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        val options = listOf(
            ViewMode.LIST to "List",
            ViewMode.GRID to "Grid",
            ViewMode.COMPACT to "Compact list",
        )
        options.forEach { (mode, label) ->
            DropdownMenuItem(
                text = { Text(label) },
                leadingIcon = {
                    if (current == mode) Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                },
                onClick = {
                    onSelected(mode)
                    expanded = false
                },
            )
        }
    }
}
