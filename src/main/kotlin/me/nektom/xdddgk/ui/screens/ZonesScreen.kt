package me.nektom.xdddgk.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.nektom.xdddgk.api.models.Zone
import me.nektom.xdddgk.ui.AppState

@Composable
fun ZonesScreen(state: AppState) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Zones — ${state.selectedProfile?.name}", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = { state.loadZones() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.zones.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No zones found.", color = MaterialTheme.colorScheme.outline)
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.zones) { zone ->
                    ZoneCard(
                        zone = zone,
                        isSelected = state.selectedZone == zone,
                        onClick = { state.selectZone(zone) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoneCard(zone: Zone, isSelected: Boolean, onClick: () -> Unit) {
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(zone.name, style = MaterialTheme.typography.titleMedium)
                Text("Status: ${zone.status}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
            if (isSelected) {
                Badge { Text("Selected") }
            }
        }
    }
}
