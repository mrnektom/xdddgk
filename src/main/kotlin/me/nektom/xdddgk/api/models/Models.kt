package me.nektom.xdddgk.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CloudflareProfile(
    val name: String,
    val token: String
)

@Serializable
data class Zone(
    val id: String,
    val name: String,
    val status: String
)

@Serializable
data class DnsRecord(
    val id: String = "",
    @SerialName("zone_id") val zoneId: String = "",
    val type: String,
    val name: String,
    val content: String,
    val ttl: Int = 1,
    val proxied: Boolean = false,
    val comment: String? = null
)

val DNS_RECORD_TYPES = listOf("A", "AAAA", "CNAME", "MX", "TXT", "NS", "SRV", "CAA", "PTR", "HTTPS", "TLSA")
