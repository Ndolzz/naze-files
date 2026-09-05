package com.naze.files.ui.browser.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naze.files.ui.browser.ClipboardOperation
import com.naze.files.ui.browser.ClipboardState

@Composable
fun ClipboardBar(
    clipboard: ClipboardState,
    onPaste: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val verb = if (clipboard.operation == ClipboardOperation.CUT) "Move" else "Copy"
            Text(
                text = "${clipboard.items.size} item(s) ready to $verb here",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Button(onClick = onPaste) { Text("Paste") }
            }
        }
    }
}
