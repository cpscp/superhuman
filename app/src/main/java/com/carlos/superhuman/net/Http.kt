package com.carlos.superhuman.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.carlos.superhuman.data.Store
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val UA = "Mozilla/5.0 (Linux; Android 14) Superhuman/1.0"

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val b = Request.Builder().url(url).header("User-Agent", UA)
        headers.forEach { (k, v) -> b.header(k, v) }
        client.newCall(b.build()).execute().use { r ->
            val body = r.body?.string() ?: ""
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}: ${body.take(300)}")
            body
        }
    }

    suspend fun postJson(url: String, json: String, headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val b = Request.Builder().url(url).header("User-Agent", UA)
            .post(json.toRequestBody("application/json".toMediaType()))
        headers.forEach { (k, v) -> b.header(k, v) }
        client.newCall(b.build()).execute().use { r ->
            val body = r.body?.string() ?: ""
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}: ${body.take(500)}")
            body
        }
    }

    suspend fun postForm(url: String, fields: Map<String, String>): String = withContext(Dispatchers.IO) {
        val fb = FormBody.Builder()
        fields.forEach { (k, v) -> fb.add(k, v) }
        val req = Request.Builder().url(url).header("User-Agent", UA).post(fb.build()).build()
        client.newCall(req).execute().use { r ->
            val body = r.body?.string() ?: ""
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}: ${body.take(300)}")
            body
        }
    }

    fun parse(text: String): JsonElement = Store.json.parseToJsonElement(text)
}

// Pequenos atalhos para ler JSON sem modelos
fun JsonElement?.obj(key: String): JsonObject? = (this as? JsonObject)?.get(key) as? JsonObject
fun JsonElement?.arr(key: String) = ((this as? JsonObject)?.get(key))?.let { runCatching { it.jsonArray }.getOrNull() }
fun JsonElement?.str(key: String): String? = ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.contentOrNull
fun JsonElement?.num(key: String): Double? = ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.doubleOrNull
fun JsonElement?.int(key: String): Int? = ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.intOrNull
fun JsonElement.asObj(): JsonObject = this.jsonObject
fun JsonElement.asDoubleOrNull(): Double? = runCatching { this.jsonPrimitive.doubleOrNull }.getOrNull()
