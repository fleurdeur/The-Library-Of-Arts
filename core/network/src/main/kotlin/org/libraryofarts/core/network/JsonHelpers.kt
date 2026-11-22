package org.libraryofarts.core.network

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

internal fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }

internal fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull
internal fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
internal fun JsonObject.arr(key: String): JsonArray = this[key] as? JsonArray ?: JsonArray(emptyList())

internal fun JsonArray.objects(): List<JsonObject> = mapNotNull { it as? JsonObject }

internal fun JsonObject.firstString(key: String): String? = when (val value = this[key]) {
    is JsonPrimitive -> value.contentOrNull?.takeIf { it.isNotBlank() }
    is JsonArray -> value.firstOrNull().let { element ->
        when (element) {
            is JsonPrimitive -> element.contentOrNull
            is JsonObject -> element.str("value") ?: element.str("content")
            else -> null
        }
    }
    else -> null
}

internal fun JsonObject.deep(vararg path: String): JsonElement? {
    var current: JsonElement = this
    for (key in path) {
        current = (current as? JsonObject)?.get(key) ?: return null
    }
    return current.takeUnless { it is JsonNull }
}

internal fun JsonObject.deepString(vararg path: String): String? {
    val element = deep(*path) as? JsonPrimitive ?: return null
    return element.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
}

internal fun JsonObject.deepFirstString(vararg path: String): String? =
    (deep(*path) as? JsonArray)?.firstOrNull().let { element ->
        when (element) {
            is JsonPrimitive -> element.contentOrNull
            is JsonObject -> element.str("content") ?: element.firstString("value")
            else -> null
        }
    }

internal fun stripHtml(input: String?): String? = input
    ?.replace(Regex("<[^>]+>"), " ")
    ?.replace("&amp;", "&")
    ?.replace("&nbsp;", " ")
    ?.replace("&quot;", "\"")
    ?.replace("&#39;", "'")
    ?.replace(Regex("\\s+"), " ")
    ?.trim()
    ?.takeIf { it.isNotBlank() }

internal fun thumbnailFrom(url: String, width: Int = 800): String = url
