package com.plantia.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.plantia.reminders.WaterReminder

@Composable
fun WaterTodayCard(
    duePlants: List<WaterReminder>,
    modifier: Modifier = Modifier,
) {
    if (duePlants.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "💧 Plantas que tocan regar hoy",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            duePlants.forEach { r ->
                Text(
                    "• ${r.plantName}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun DetailInfoSection(
    title: String,
    items: List<Pair<String, String>>,
) {
    val visible = items.filter { it.second.isNotBlank() }
    if (visible.isEmpty()) return

    SectionCard(title = title) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            visible.forEach { (label, value) ->
                Column {
                    Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                    Text(value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
