package com.naze.files.ui.browser.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naze.files.ui.browser.Breadcrumb

@Composable
fun BreadcrumbBar(
    breadcrumbs: List<Breadcrumb>,
    onCrumbClick: (Breadcrumb) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        breadcrumbs.forEachIndexed { index, crumb ->
            val isLast = index == breadcrumbs.lastIndex
            Text(
                text = crumb.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isLast) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.clickableCrumb(enabled = !isLast) { onCrumbClick(crumb) },
            )
            if (!isLast) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}

private fun Modifier.clickableCrumb(enabled: Boolean, onClick: () -> Unit): Modifier =
    if (enabled) {
        this.then(
            androidx.compose.foundation.clickable(onClick = onClick),
        )
    } else {
        this
    }
