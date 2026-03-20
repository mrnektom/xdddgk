package me.nektom.xdddgk.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import me.nektom.xdddgk.api.models.DnsRecord
import me.nektom.xdddgk.ui.AppState
import me.nektom.xdddgk.ui.components.ConfirmDialog
import me.nektom.xdddgk.ui.components.DnsRecordDialog
import me.nektom.xdddgk.ui.components.JsonContentType
import me.nektom.xdddgk.ui.components.detectJsonType

@Composable
fun DnsRecordsScreen(state: AppState) {
    val zone = state.selectedZone ?: return
    var showRecordDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<DnsRecord?>(null) }
    var deleteTarget by remember { mutableStateOf<DnsRecord?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = state.dnsRecords.filter { record ->
        searchQuery.isBlank() ||
            record.name.contains(searchQuery, ignoreCase = true) ||
            record.content.contains(searchQuery, ignoreCase = true) ||
            record.type.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(zone.name, style = MaterialTheme.typography.headlineSmall)
                Text("${state.dnsRecords.size} records", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
            Row {
                IconButton(onClick = { state.loadDnsRecords(zone.id) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                FloatingActionButton(
                    onClick = { editingRecord = null; showRecordDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add record")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name, content or type…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (searchQuery.isBlank()) "No DNS records found." else "No records match \"$searchQuery\".",
                    color = MaterialTheme.colorScheme.outline
                )
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(filtered, key = { it.id }) { record ->
                    DnsRecordRow(
                        record = record,
                        onEdit = { editingRecord = record; showRecordDialog = true },
                        onDelete = { deleteTarget = record }
                    )
                }
            }
        }
    }

    if (showRecordDialog) {
        DnsRecordDialog(
            initial = editingRecord,
            zoneName = zone.name,
            onConfirm = { record ->
                if (editingRecord == null) {
                    state.createRecord(record) { showRecordDialog = false }
                } else {
                    state.updateRecord(record) { showRecordDialog = false }
                }
            },
            onDismiss = { showRecordDialog = false }
        )
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "Delete DNS record",
            text = "Delete ${target.type} record \"${target.name}\"?\n\nContent: ${target.content}",
            onConfirm = { state.deleteRecord(target); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun DnsRecordRow(record: DnsRecord, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type badge
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.width(56.dp)
            ) {
                Text(
                    record.type,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(record.name, style = MaterialTheme.typography.bodyMedium)
                    if (record.proxied) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                "Proxied",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    val isJsonContent = record.type == "TXT" && detectJsonType(record.content) != JsonContentType.None
                    if (isJsonContent) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "JSON",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                val displayContent = if (record.content.length > 80)
                    record.content.take(80) + "…"
                else
                    record.content
                Text(
                    displayContent,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.outline
                )
                if (!record.comment.isNullOrBlank()) {
                    Text(
                        record.comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Text(
                if (record.ttl == 1) "Auto" else "${record.ttl}s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
