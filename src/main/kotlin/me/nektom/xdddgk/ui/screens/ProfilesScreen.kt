package me.nektom.xdddgk.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.nektom.xdddgk.api.models.CloudflareProfile
import me.nektom.xdddgk.ui.AppState

@Composable
fun ProfilesScreen(state: AppState) {
    var showDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<CloudflareProfile?>(null) }
    var deleteTarget by remember { mutableStateOf<CloudflareProfile?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Profiles", style = MaterialTheme.typography.headlineSmall)
            FloatingActionButton(
                onClick = { editingProfile = null; showDialog = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add profile")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (state.profiles.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No profiles yet. Add a Cloudflare API key.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.profiles) { profile ->
                    ProfileCard(
                        profile = profile,
                        isSelected = state.selectedProfile == profile,
                        onSelect = { state.selectProfile(profile) },
                        onEdit = { editingProfile = profile; showDialog = true },
                        onDelete = { deleteTarget = profile }
                    )
                }
            }
        }
    }

    if (showDialog) {
        ProfileDialog(
            initial = editingProfile,
            onConfirm = { updated ->
                val list = state.profiles.toMutableList()
                val idx = list.indexOfFirst { it.name == editingProfile?.name }
                if (idx >= 0) list[idx] = updated else list.add(updated)
                state.saveProfiles(list)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete profile") },
            text = { Text("Delete profile \"${target.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        state.saveProfiles(state.profiles.filter { it.name != target.name })
                        if (state.selectedProfile == target) state.selectedProfile = null
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: CloudflareProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "…${profile.token.takeLast(6)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
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

@Composable
private fun ProfileDialog(
    initial: CloudflareProfile?,
    onConfirm: (CloudflareProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var token by remember { mutableStateOf(initial?.token ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Profile" else "Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("API Token") },
                    placeholder = { Text("Cloudflare API Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(CloudflareProfile(name.trim(), token.trim())) },
                enabled = name.isNotBlank() && token.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
