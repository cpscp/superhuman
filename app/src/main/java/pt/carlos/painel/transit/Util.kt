package pt.carlos.painel.transit

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Http {
    private const val UA = "Mozilla/5.0 (Linux; Android 15; Pixel 9a) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36 Painel/1.0"

    /** Abre a ligação e valida o código HTTP. Quem chama tem de fazer disconnect(). */
    fun open(url: String, timeoutMs: Int = 15_000, headers: Map<String, String> = emptyMap()): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = timeoutMs
        c.readTimeout = timeoutMs
        c.setRequestProperty("User-Agent", UA)
        c.setRequestProperty("Accept", "application/json, text/plain, */*")
        headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
        val code = c.responseCode
        if (code !in 200..299) {
            c.disconnect()
            throw IOException("HTTP $code em ${c.url.host}")
        }
        return c
    }

    fun get(url: String, timeoutMs: Int = 15_000, headers: Map<String, String> = emptyMap()): String {
        val c = open(url, timeoutMs, headers)
        try {
            return c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            c.disconnect()
        }
    }
}

object Text {
    private val PT: Locale = Locale.forLanguageTag("pt-PT")
    private val small = setOf("de", "do", "da", "dos", "das", "e", "a", "o")
    private val marks = Regex("\\p{Mn}+")
    private val hm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** minúsculas e sem acentos, para comparar/pesquisar */
    fun norm(s: String): String =
        marks.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "").lowercase(Locale.ROOT)

    /** Como o norm() do Glass TV: "Alcântara-Mar" -> "alcantara mar". */
    fun key(s: String): String =
        norm(s).replace(Regex("[^a-z0-9]+"), " ").trim()

    /** "CAIS DO SODRE" -> "Cais do Sodre" */
    fun title(s: String): String {
        if (s.isBlank()) return s.trim()
        return s.trim().lowercase(PT).split(Regex("\\s+")).mapIndexed { i, w ->
            if (i > 0 && w in small) w else w.replaceFirstChar { it.titlecase(PT) }
        }.joinToString(" ")
    }

    /**
     * Nomes com hífen partem ANTES do hífen e nunca depois: "Alcântara" / "-Mar".
     * (U+200B = ponto de quebra invisível; U+2060 = proíbe quebra a seguir ao hífen.)
     */
    fun breakBeforeHyphens(s: String): String = s.replace("-", "\u200B-\u2060")

    fun hhmm(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(hm)
}

/** optString do Android devolve "null" para valores JSON null; isto devolve "". */
fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key, "")

fun org.json.JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }

/** null se vazio (como o str() do Glass TV). */
fun JSONObject.strOrNull(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
