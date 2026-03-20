package me.nektom.xdddgk.ui.components

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

private val jsonParser = Json { prettyPrint = false }
private val jsonPrinter = Json { prettyPrint = true }

sealed class JsonContentType {
    /** Content is a raw JSON object or array: {"key":"value"} */
    data object Raw : JsonContentType()
    /** Content is a JSON-encoded string wrapping JSON: "{\"key\":\"value\"}" */
    data object Escaped : JsonContentType()
    /** Not JSON */
    data object None : JsonContentType()
}

fun detectJsonType(content: String): JsonContentType {
    val trimmed = content.trim()
    if (trimmed.isEmpty()) return JsonContentType.None

    return when (trimmed[0]) {
        '{', '[' -> {
            try {
                jsonParser.parseToJsonElement(trimmed)
                JsonContentType.Raw
            } catch (_: Exception) {
                JsonContentType.None
            }
        }
        '"' -> {
            try {
                val element = jsonParser.parseToJsonElement(trimmed)
                val inner = element.jsonPrimitive.content.trim()
                if (inner.isNotEmpty() && (inner[0] == '{' || inner[0] == '[')) {
                    jsonParser.parseToJsonElement(inner)
                    JsonContentType.Escaped
                } else {
                    JsonContentType.None
                }
            } catch (_: Exception) {
                JsonContentType.None
            }
        }
        else -> JsonContentType.None
    }
}

/** Возвращает внутренний JSON-контент для редактирования.
 *  Для Escaped — распаковывает строку; для Raw — возвращает как есть. */
fun unwrapForEditing(content: String): String {
    return when (detectJsonType(content)) {
        JsonContentType.Escaped -> {
            val inner = jsonParser.parseToJsonElement(content.trim()).jsonPrimitive.content
            prettyPrintJson(inner)
        }
        JsonContentType.Raw -> prettyPrintJson(content)
        JsonContentType.None -> content
    }
}

fun prettyPrintJson(content: String): String {
    val element = jsonParser.parseToJsonElement(content.trim())
    return jsonPrinter.encodeToString(JsonElement.serializer(), element)
}

fun minifyJson(content: String): String {
    val element = jsonParser.parseToJsonElement(content.trim())
    return jsonParser.encodeToString(JsonElement.serializer(), element)
}

/** Упаковывает JSON обратно в исходный формат перед сохранением в API. */
fun rewrapForSaving(editedContent: String, originalType: JsonContentType): String {
    return when (originalType) {
        JsonContentType.Escaped -> {
            val minified = minifyJson(editedContent)
            jsonParser.encodeToString(JsonPrimitive.serializer(), JsonPrimitive(minified))
        }
        JsonContentType.Raw -> minifyJson(editedContent)
        JsonContentType.None -> editedContent.trim()
    }
}
