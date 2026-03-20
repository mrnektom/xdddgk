package me.nektom.xdddgk.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.nektom.xdddgk.ui.screens.DnsRecordsScreen
import me.nektom.xdddgk.ui.screens.ProfilesScreen
import me.nektom.xdddgk.ui.screens.ZonesScreen

@Composable
fun App(state: AppState) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                // Error banner
                state.errorMessage?.let { err ->
                    Surface(color = MaterialTheme.colorScheme.errorContainer) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                err,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = { state.errorMessage = null }) {
                                Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    // Left panel: Profiles
                    Surface(
                        modifier = Modifier.width(280.dp).fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        ProfilesScreen(state)
                    }

                    VerticalDivider()

                    // Middle panel: Zones (only when profile selected)
                    if (state.selectedProfile != null) {
                        Surface(
                            modifier = Modifier.width(260.dp).fillMaxHeight(),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp
                        ) {
                            ZonesScreen(state)
                        }
                        VerticalDivider()
                    }

                    // Right panel: DNS records (only when zone selected)
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        when {
                            state.selectedZone != null -> DnsRecordsScreen(state)
                            state.selectedProfile != null -> Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Select a zone to manage its DNS records.",
                                    color = MaterialTheme.colorScheme.outline)
                            }
                            else -> Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Add a Cloudflare profile and select it to get started.",
                                    color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }
}
