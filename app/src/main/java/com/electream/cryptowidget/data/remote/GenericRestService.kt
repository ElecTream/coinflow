package com.electream.cryptowidget.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches a numeric value or list of numeric values from any REST endpoint using a
 * dot-notation JSON path. Used for BYOC [com.electream.cryptowidget.data.model.PriceSource.GenericRest]
 * and [com.electream.cryptowidget.data.model.WalletConfig.GenericRest] sources.
 *
 * Path examples:
 *  - `"price"`          → top-level key
 *  - `"data.rates.USD"` → nested object keys
 *  - `"result.0.price"` → array index (as string) then object key
 */
@Singleton
class GenericRestService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun fetchDouble(url: String, jsonPath: String): Double = withContext(Dispatchers.IO) {
        val body = executeGet(url)
        val element = json.parseToJsonElement(body)
        resolveJsonPath(element, jsonPath)?.jsonPrimitive?.doubleOrNull
            ?: throw RuntimeException("Could not resolve path '$jsonPath' in response from $url")
    }

    suspend fun fetchDoubleList(url: String, jsonPath: String): List<Double> = withContext(Dispatchers.IO) {
        val body = executeGet(url)
        val element = json.parseToJsonElement(body)
        val target = resolveJsonPath(element, jsonPath)
            ?: throw RuntimeException("Could not resolve path '$jsonPath' in response from $url")
        (target as? JsonArray)?.mapNotNull { it.jsonPrimitive.doubleOrNull } ?: emptyList()
    }

    private fun executeGet(url: String): String {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RuntimeException("HTTP ${response.code} from $url")
            return response.body?.string() ?: throw RuntimeException("Empty response from $url")
        }
    }

    /**
     * Traverses a [kotlinx.serialization.json.JsonElement] using dot-notation [path].
     * Array indices are represented as numeric strings, e.g. `"result.0.price"`.
     */
    private fun resolveJsonPath(
        element: kotlinx.serialization.json.JsonElement,
        path: String
    ): kotlinx.serialization.json.JsonElement? {
        if (path.isBlank()) return element
        var current: kotlinx.serialization.json.JsonElement = element
        for (key in path.split(".")) {
            current = when (current) {
                is JsonObject -> current[key] ?: return null
                is JsonArray  -> current.getOrNull(key.toIntOrNull() ?: return null) ?: return null
                else          -> return null
            }
        }
        return current
    }
}
