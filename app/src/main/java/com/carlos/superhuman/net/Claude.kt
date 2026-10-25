package com.carlos.superhuman.net

import com.carlos.superhuman.data.Store
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/** Cliente mínimo para a Claude API (Messages). A chave fica só no telemóvel. */
object Claude {
    private const val URL = "https://api.anthropic.com/v1/messages"

    fun hasKey(): Boolean = Store.data.settings.claudeApiKey.isNotBlank()

    fun webSearchTool(maxUses: Int = 6): JsonObject = buildJsonObject {
        put("type", "web_search_20250305")
        put("name", "web_search")
        put("max_uses", maxUses)
    }

    suspend fun send(
        system: String,
        messages: JsonArray,
        tools: JsonArray? = null,
        maxTokens: Int = 2048,
    ): JsonObject {
        val s = Store.data.settings
        require(s.claudeApiKey.isNotBlank()) { "Falta a chave da Claude API nas Definições." }
        val body = buildJsonObject {
            put("model", s.claudeModel.ifBlank { "claude-sonnet-5-5" })
            put("max_tokens", maxTokens)
            put("system", system)
            put("messages", messages)
            if (tools != null && tools.isNotEmpty()) put("tools", tools)
        }
        val text = Http.postJson(
            URL, body.toString(),
            mapOf("x-api-key" to s.claudeApiKey, "anthropic-version" to "2023-06-01"),
        )
        return Http.parse(text).jsonObject
    }

    /** Junta todos os blocos de texto de uma resposta. */
    fun textOf(resp: JsonObject): String =
        resp["content"]?.jsonArray?.filter { it.str("type") == "text" }?.joinToString("") { it.str("text") ?: "" }?.trim() ?: ""

    fun userMsg(text: String): JsonObject = buildJsonObject {
        put("role", "user")
        put("content", text)
    }

    /** Pedido simples de uma só volta, com pesquisa web opcional. */
    suspend fun ask(system: String, prompt: String, webSearch: Boolean = false, maxTokens: Int = 2048): String {
        var messages = buildJsonArray { add(userMsg(prompt)) }
        val tools = if (webSearch) buildJsonArray { add(webSearchTool()) } else null
        // A pesquisa web é feita no servidor; se a resposta parar em "pause_turn", continua.
        repeat(4) {
            val resp = send(system, messages, tools, maxTokens)
            val stop = resp.str("stop_reason")
            if (stop != "pause_turn") return textOf(resp)
            messages = JsonArray(messages + buildJsonObject {
                put("role", "assistant")
                put("content", resp["content"] ?: JsonArray(emptyList()))
            })
        }
        return ""
    }

    /** Extrai o primeiro objeto JSON de um texto (o modelo às vezes envolve em ```json). */
    fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else null
    }

}
