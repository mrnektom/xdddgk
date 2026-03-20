package me.nektom.xdddgk.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.nektom.xdddgk.api.models.DnsRecord
import me.nektom.xdddgk.api.models.Zone

private const val BASE_URL = "https://api.cloudflare.com/client/v4"

@Serializable
private data class CloudflareResponse<T>(
    val result: T? = null,
    val success: Boolean,
    val errors: List<CloudflareError> = emptyList()
)

@Serializable
private data class CloudflareError(
    val code: Int,
    val message: String
)

@Serializable
private data class DnsRecordBody(
    val type: String,
    val name: String,
    val content: String,
    val ttl: Int,
    val proxied: Boolean,
    val comment: String? = null
)

@Serializable
private data class ZoneResult(
    val id: String,
    val name: String,
    val status: String
)

@Serializable
private data class DnsRecordResult(
    val id: String = "",
    @SerialName("zone_id") val zoneId: String = "",
    val type: String,
    val name: String,
    val content: String,
    val ttl: Int = 1,
    val proxied: Boolean = false,
    val comment: String? = null
)

class CloudflareApiException(message: String) : Exception(message)

class CloudflareApi(private val token: String) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.ALL
        }
    }

    private fun HttpRequestBuilder.auth() {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    suspend fun getZones(): List<Zone> {
        val response: CloudflareResponse<List<ZoneResult>> = client.get("$BASE_URL/zones") {
            auth()
            parameter("per_page", 100)
        }.body()
        if (!response.success) throw CloudflareApiException(response.errors.joinToString { it.message })
        return response.result.orEmpty().map { Zone(it.id, it.name, it.status) }
    }

    suspend fun getDnsRecords(zoneId: String): List<DnsRecord> {
        val response: CloudflareResponse<List<DnsRecordResult>> =
            client.get("$BASE_URL/zones/$zoneId/dns_records") {
                auth()
                parameter("per_page", 500)
            }.body()
        if (!response.success) throw CloudflareApiException(response.errors.joinToString { it.message })
        return response.result.orEmpty().map {
            DnsRecord(it.id, it.zoneId, it.type, it.name, it.content, it.ttl, it.proxied, it.comment)
        }
    }

    suspend fun createDnsRecord(zoneId: String, record: DnsRecord): DnsRecord {
        val response: CloudflareResponse<DnsRecordResult> =
            client.post("$BASE_URL/zones/$zoneId/dns_records") {
                auth()
                contentType(ContentType.Application.Json)
                setBody(DnsRecordBody(record.type, record.name, record.content, record.ttl, record.proxied, record.comment))
            }.body()
        if (!response.success) throw CloudflareApiException(response.errors.joinToString { it.message })
        val r = response.result!!
        return DnsRecord(r.id, r.zoneId, r.type, r.name, r.content, r.ttl, r.proxied, r.comment)
    }

    suspend fun updateDnsRecord(zoneId: String, recordId: String, record: DnsRecord): DnsRecord {
        val response: CloudflareResponse<DnsRecordResult> =
            client.put("$BASE_URL/zones/$zoneId/dns_records/$recordId") {
                auth()
                contentType(ContentType.Application.Json)
                setBody(DnsRecordBody(record.type, record.name, record.content, record.ttl, record.proxied, record.comment))
            }.body()
        if (!response.success) throw CloudflareApiException(response.errors.joinToString { it.message })
        val r = response.result!!
        return DnsRecord(r.id, r.zoneId, r.type, r.name, r.content, r.ttl, r.proxied, r.comment)
    }

    suspend fun deleteDnsRecord(zoneId: String, recordId: String) {
        @Serializable
        data class DeleteResult(val id: String)
        val response: CloudflareResponse<DeleteResult> =
            client.delete("$BASE_URL/zones/$zoneId/dns_records/$recordId") {
                auth()
            }.body()
        if (!response.success) throw CloudflareApiException(response.errors.joinToString { it.message })
    }

    fun close() = client.close()
}
