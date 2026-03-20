package me.nektom.xdddgk.ui

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.nektom.xdddgk.api.CloudflareApi
import me.nektom.xdddgk.api.models.CloudflareProfile
import me.nektom.xdddgk.api.models.DnsRecord
import me.nektom.xdddgk.api.models.Zone
import me.nektom.xdddgk.storage.ProfileStorage

class AppState(private val scope: CoroutineScope) {
    var profiles by mutableStateOf(ProfileStorage.load())
    var selectedProfile by mutableStateOf<CloudflareProfile?>(null)

    var zones by mutableStateOf<List<Zone>>(emptyList())
    var selectedZone by mutableStateOf<Zone?>(null)

    var dnsRecords by mutableStateOf<List<DnsRecord>>(emptyList())

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private var api: CloudflareApi? = null

    fun selectProfile(profile: CloudflareProfile) {
        api?.close()
        api = CloudflareApi(profile.token)
        selectedProfile = profile
        selectedZone = null
        zones = emptyList()
        dnsRecords = emptyList()
        loadZones()
    }

    fun loadZones() {
        scope.launch {
            withLoading {
                zones = api!!.getZones().sortedBy { it.name }
            }
        }
    }

    fun selectZone(zone: Zone) {
        selectedZone = zone
        loadDnsRecords(zone.id)
    }

    fun loadDnsRecords(zoneId: String) {
        scope.launch {
            withLoading {
                dnsRecords = api!!.getDnsRecords(zoneId).sortedWith(
                    compareBy({ it.type }, { it.name })
                )
            }
        }
    }

    fun createRecord(record: DnsRecord, onDone: () -> Unit) {
        val zoneId = selectedZone?.id ?: return
        scope.launch {
            withLoading {
                api!!.createDnsRecord(zoneId, record)
                loadDnsRecords(zoneId)
            }
            onDone()
        }
    }

    fun updateRecord(record: DnsRecord, onDone: () -> Unit) {
        val zoneId = selectedZone?.id ?: return
        scope.launch {
            withLoading {
                api!!.updateDnsRecord(zoneId, record.id, record)
                loadDnsRecords(zoneId)
            }
            onDone()
        }
    }

    fun deleteRecord(record: DnsRecord) {
        val zoneId = selectedZone?.id ?: return
        scope.launch {
            withLoading {
                api!!.deleteDnsRecord(zoneId, record.id)
                dnsRecords = dnsRecords.filter { it.id != record.id }
            }
        }
    }

    fun saveProfiles(updated: List<CloudflareProfile>) {
        profiles = updated
        ProfileStorage.save(updated)
    }

    private suspend fun withLoading(block: suspend () -> Unit) {
        isLoading = true
        errorMessage = null
        try {
            block()
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unknown error"
        } finally {
            isLoading = false
        }
    }
}
