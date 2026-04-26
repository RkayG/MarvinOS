package com.marvinos.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QuickChipsBar(
    onChipSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = listOf(
        "Turn off WiFi",
        "Check my specs",
        "Free up space",
        "Turn on flashlight",
        "Dark mode on"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { suggestion ->
            ElevatedSuggestionChip(
                onClick = { onChipSelected(suggestion) },
                label = { Text(suggestion) }
            )
        }
    }
}
