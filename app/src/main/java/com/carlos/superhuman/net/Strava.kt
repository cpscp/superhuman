package com.carlos.superhuman.net

import android.net.Uri
import com.carlos.superhuman.data.Store
import com.carlos.superhuman.data.StravaActivity
import com.carlos.superhuman.data.StravaAuth

/**
 * Strava via OAuth. Cria uma app em https://www.strava.com/settings/api com
 * "Authorization Callback Domain" = localhost e coloca o Client ID/Secret nas Definições.
 */
object Strava {
    const val REDIRECT = "superhuman://localhost/strava"

    fun authorizeUrl(): String {
        val id = Store.data.settings.stravaClientId
        return Uri.parse("https://www.strava.com/oauth/mobile/authorize").buildUpon()
            .appendQueryParameter("client_id", id)
            .appendQueryParameter("redirect_uri", REDIRECT)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("approval_prompt", "auto")
            .appendQueryParameter("scope", "activity:read_all")
            .build().toString()
    }

    suspend fun exchange(code: String): Boolean = runCatching {
        val s = Store.data.settings
        val root = Http.parse(
            Http.postForm(
                "https://www.strava.com/oauth/token",
                mapOf("client_id" to s.stravaClientId, "client_secret" to s.stravaClientSecret, "code" to code, "grant_type" to "authorization_code"),
            )
        )
        saveTokens(root, root.obj("athlete").str("firstname") ?: "")
        true
    }.getOrDefault(false)

    private fun saveTokens(root: kotlinx.serialization.json.JsonElement, athlete: String?) {
        val auth = StravaAuth(
            accessToken = root.str("access_token") ?: "",
            refreshToken = root.str("refresh_token") ?: "",
            expiresAt = (root.num("expires_at") ?: 0.0).toLong(),
            athlete = athlete ?: Store.data.strava.athlete,
        )
        Store.update { it.copy(strava = auth) }
    }

    private suspend fun validToken(): String? {
        val a = Store.data.strava
        if (a.refreshToken.isBlank()) return null
        if (a.expiresAt * 1000 > System.currentTimeMillis() + 60_000) return a.accessToken
        val s = Store.data.settings
        val root = Http.parse(
            Http.postForm(
                "https://www.strava.com/oauth/token",
                mapOf("client_id" to s.stravaClientId, "client_secret" to s.stravaClientSecret, "refresh_token" to a.refreshToken, "grant_type" to "refresh_token"),
            )
        )
        saveTokens(root, null)
        return Store.data.strava.accessToken
    }

    suspend fun refreshActivities(): Boolean = runCatching {
        val token = validToken() ?: return@runCatching false
        val root = Http.parse(Http.get("https://www.strava.com/api/v3/athlete/activities?per_page=15", mapOf("Authorization" to "Bearer $token")))
        val list = (root as? kotlinx.serialization.json.JsonArray)?.map {
            StravaActivity(
                name = it.str("name") ?: "",
                type = it.str("sport_type") ?: it.str("type") ?: "",
                distanceKm = (it.num("distance") ?: 0.0) / 1000.0,
                movingMin = ((it.num("moving_time") ?: 0.0) / 60).toInt(),
                startLocal = it.str("start_date_local") ?: "",
                elevation = it.num("total_elevation_gain") ?: 0.0,
            )
        } ?: emptyList()
        Store.update { it.copy(stravaActivities = list) }
        true
    }.getOrDefault(false)

    fun disconnect() = Store.update { it.copy(strava = StravaAuth(), stravaActivities = emptyList()) }
}
