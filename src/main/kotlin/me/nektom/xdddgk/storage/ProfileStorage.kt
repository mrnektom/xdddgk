package me.nektom.xdddgk.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.nektom.xdddgk.api.models.CloudflareProfile
import java.io.File

object ProfileStorage {
    private val configDir = File(System.getProperty("user.home"), ".config/cloudflare-dns-editor")
    private val profilesFile = File(configDir, "profiles.json")
    private val json = Json { prettyPrint = true }

    fun load(): List<CloudflareProfile> {
        if (!profilesFile.exists()) return emptyList()
        return try {
            json.decodeFromString(profilesFile.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(profiles: List<CloudflareProfile>) {
        configDir.mkdirs()
        profilesFile.writeText(json.encodeToString(profiles))
    }
}
