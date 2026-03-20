package me.nektom.xdddgk.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import me.nektom.xdddgk.api.models.DnsRecord
import me.nektom.xdddgk.api.models.DNS_RECORD_TYPES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsRecordDialog(
    initial: DnsRecord?,
    zoneName: String,
    onConfirm: (DnsRecord) -> Unit,
    onDismiss: () -> Unit
) {
    val isEdit = initial != null
    var type by remember { mutableStateOf(initial?.type ?: "A") }
    var name by remember { mutableStateOf(initial?.name ?: "") }

    // Запоминаем исходный тип JSON при открытии диалога (задача 13)
    val originalJsonType = remember(initial) {
        if (initial?.type == "TXT") detectJsonType(initial.content) else JsonContentType.None
    }
    var content by remember(initial) {
        mutableStateOf(
            if (initial?.type == "TXT" && originalJsonType != JsonContentType.None) {
                // Распаковываем для редактирования: escaped → inner pretty JSON, raw → pretty JSON
                try { unwrapForEditing(initial.content) } catch (_: Exception) { initial.content }
            } else {
                initial?.content ?: ""
            }
        )
    }

    var ttl by remember { mutableStateOf((initial?.ttl ?: 1).toString()) }
    var proxied by remember { mutableStateOf(initial?.proxied ?: false) }
    var comment by remember { mutableStateOf(initial?.comment ?: "") }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var jsonError by remember { mutableStateOf<String?>(null) }

    val isTxt = type == "TXT"
    LaunchedEffect(type) { jsonError = null }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit DNS Record" else "New DNS Record") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Zone: $zoneName", style = MaterialTheme.typography.bodySmall)

                ExposedDropdownMenuBox(
                    expanded = typeMenuExpanded,
                    onExpandedChange = { typeMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenuExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        DNS_RECORD_TYPES.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = { type = t; typeMenuExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. subdomain or @") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isTxt) {
                    Column {
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it; jsonError = null },
                            label = { Text("Content") },
                            isError = jsonError != null,
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                            minLines = 5,
                            maxLines = 12,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Статус формата + ошибка (задача 14 — подсказка пользователю)
                            if (jsonError != null) {
                                Text(
                                    jsonError!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                            } else if (originalJsonType == JsonContentType.Escaped) {
                                Text(
                                    "Stored as escaped JSON string",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }

                            val looksLikeJson = content.trim().let { it.startsWith("{") || it.startsWith("[") }
                            if (looksLikeJson) {
                                TextButton(
                                    onClick = {
                                        try {
                                            content = prettyPrintJson(content)
                                            jsonError = null
                                        } catch (e: Exception) {
                                            jsonError = "Invalid JSON: ${e.message?.take(60)}"
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Format JSON", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Content") },
                        placeholder = { Text("e.g. 1.2.3.4") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = ttl,
                    onValueChange = { ttl = it.filter { c -> c.isDigit() } },
                    label = { Text("TTL (1 = Auto)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = proxied, onCheckedChange = { proxied = it })
                    Spacer(Modifier.width(4.dp))
                    Text("Proxied (orange cloud)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Задача 14: перепаковываем в исходный формат при сохранении
                    val finalContent = if (isTxt && detectJsonType(content) != JsonContentType.None) {
                        try { rewrapForSaving(content, originalJsonType) } catch (_: Exception) { content.trim() }
                    } else {
                        content.trim()
                    }
                    onConfirm(
                        DnsRecord(
                            id = initial?.id ?: "",
                            zoneId = initial?.zoneId ?: "",
                            type = type,
                            name = name.trim(),
                            content = finalContent,
                            ttl = ttl.toIntOrNull() ?: 1,
                            proxied = proxied,
                            comment = comment.trim().ifEmpty { null }
                        )
                    )
                },
                enabled = name.isNotBlank() && content.isNotBlank()
            ) {
                Text(if (isEdit) "Save" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
