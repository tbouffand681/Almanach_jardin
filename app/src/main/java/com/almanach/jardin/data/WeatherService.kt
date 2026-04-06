package com.almanach.jardin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

// ─── Suggestion ville ─────────────────────────────────────────────────────────

data class CitySuggestion(
    val displayLabel: String,  // Ex : "Chambéry, 73000, Savoie, France"
    val searchName: String,    // Ce qu'on passe à fetchByCity
    val lat: Double,
    val lon: Double
)

// ─── Résultat météo ───────────────────────────────────────────────────────────

data class WeatherResult(
    val cityName: String,
    val temperature: Double,
    val feelsLike: Double,
    val tempMin: Double,
    val tempMax: Double,
    val humidity: Int,
    val windSpeed: Double,
    val precipitation: Double,
    val weatherCode: Int,
    val et0Today: Double,
    val et0Cumul2: Double,
    val et0Cumul5: Double
) {
    val weatherEmoji get() = when (weatherCode) {
        0            -> "☀️"
        1, 2         -> "🌤️"
        3            -> "☁️"
        45, 48       -> "🌫️"
        in 51..67    -> "🌧️"
        in 71..77    -> "❄️"
        in 80..82    -> "🌦️"
        in 85..86    -> "🌨️"
        in 95..99    -> "⛈️"
        else         -> "🌡️"
    }

    val weatherDescription get() = when (weatherCode) {
        0            -> "Ciel dégagé"
        1, 2         -> "Partiellement nuageux"
        3            -> "Couvert"
        45, 48       -> "Brouillard"
        51, 53, 55   -> "Bruine"
        61, 63, 65   -> "Pluie"
        71, 73, 75   -> "Neige"
        80, 81, 82   -> "Averses"
        95           -> "Orage"
        96, 99       -> "Orage avec grêle"
        else         -> "Conditions variables"
    }

    fun sowingAdvice() = when {
        temperature < 3           -> "❄️ Gel possible — aucun semis en pleine terre."
        temperature < 8           -> "🥶 Trop froid pour semer. Réservez la serre."
        temperature in 8.0..12.0  -> "🌡️ Frais. Espèces rustiques : épinard, mâche, radis."
        temperature in 12.0..17.0 -> "✅ Bonnes conditions : carotte, laitue, poireau."
        temperature in 17.0..27.0 -> "🌞 Idéal pour la plupart des semis."
        temperature > 32          -> "🔥 Trop chaud — semez tôt le matin."
        else                      -> "✅ Conditions favorables."
    }

    fun irrigationAdvice(): String {
        val deficit = (et0Today - precipitation).coerceAtLeast(0.0)
        return when {
            precipitation >= et0Today -> "💧 Pluie suffisante (${f(precipitation)} L/m²) — pas d'arrosage nécessaire."
            deficit < 1.5             -> "💧 Déficit faible (${f(deficit)} L/m²) — vérifiez si le sol est sec."
            deficit < 3.5             -> "💧💧 Apportez environ ${f(deficit)} L/m²."
            else                      -> "💧💧💧 Arrosage important : ${f(deficit)} L/m²."
        }
    }

    private fun f(v: Double) = "%.1f".format(v)
}

// ─── Service réseau ───────────────────────────────────────────────────────────

object WeatherService {

    /**
     * Autocomplétion : retourne jusqu'à 5 suggestions pour une requête partielle.
     * Utilise Nominatim avec countrycodes=fr pour privilégier la France.
     */
    suspend fun fetchSuggestions(query: String): Result<List<CitySuggestion>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                // On cherche prioritairement en France mais on accepte aussi les autres pays
                val url = "https://nominatim.openstreetmap.org/search" +
                    "?q=$encoded" +
                    "&format=json" +
                    "&limit=5" +
                    "&accept-language=fr" +
                    "&featuretype=city,town,village"
                val json = get(url, userAgent = "AlmanachDuJardin/1.0 Android")
                val arr  = JSONArray(json)

                (0 until arr.length()).mapNotNull { i ->
                    val obj      = arr.getJSONObject(i)
                    val name     = obj.optString("display_name", "")
                    val lat      = obj.optDouble("lat", 0.0)
                    val lon      = obj.optDouble("lon", 0.0)
                    if (name.isEmpty()) return@mapNotNull null

                    // Construire un label court : "Ville, Code postal, Département, Pays"
                    val parts    = name.split(",").map { it.trim() }
                    val label    = buildLabel(parts)
                    // Pour la recherche météo, on utilise les coordonnées directement
                    CitySuggestion(
                        displayLabel = label,
                        searchName   = parts.firstOrNull() ?: name,
                        lat          = lat,
                        lon          = lon
                    )
                }
            }
        }

    private fun buildLabel(parts: List<String>): String {
        // Garder ville + premier élément numérique (code postal) + dernier (pays), max 60 chars
        val city    = parts.firstOrNull() ?: ""
        val postal  = parts.firstOrNull { it.matches(Regex("\\d{4,6}")) } ?: ""
        val country = parts.lastOrNull() ?: ""
        return buildString {
            append(city)
            if (postal.isNotEmpty()) append(", $postal")
            if (country.isNotEmpty() && country != city) append(", $country")
        }.take(60)
    }

    /**
     * Météo complète pour une ville (nom) — géocode puis récupère Open-Meteo.
     */
    suspend fun fetchByCity(city: String): Result<WeatherResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Géocodage
                val encoded = java.net.URLEncoder.encode(city, "UTF-8")
                val geoJson = get(
                    "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1&accept-language=fr",
                    userAgent = "AlmanachDuJardin/1.0 Android"
                )
                val geoArr = JSONArray(geoJson)
                if (geoArr.length() == 0) error("Ville \"$city\" introuvable.")

                val geo      = geoArr.getJSONObject(0)
                val lat      = geo.getDouble("lat")
                val lon      = geo.getDouble("lon")
                val cityName = geo.optString("display_name", city).split(",").first().trim()

                fetchByCoords(lat, lon, cityName)
            }
        }

    /**
     * Météo directement par coordonnées (utilisé quand on sélectionne une suggestion).
     */
    suspend fun fetchByCoords(lat: Double, lon: Double, cityName: String): WeatherResult =
        withContext(Dispatchers.IO) {
            val url = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature," +
                "precipitation,wind_speed_10m,weather_code" +
                "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum," +
                "et0_fao_evapotranspiration" +
                "&timezone=auto&forecast_days=5"

            val root    = JSONObject(get(url))
            val current = root.getJSONObject("current")
            val daily   = root.getJSONObject("daily")
            val et0Arr  = daily.getJSONArray("et0_fao_evapotranspiration")

            var et0Sum2 = 0.0; var et0Sum5 = 0.0
            for (i in 0 until minOf(et0Arr.length(), 5)) {
                val v = et0Arr.optDouble(i, 0.0)
                if (i < 2) et0Sum2 += v
                et0Sum5 += v
            }

            WeatherResult(
                cityName      = cityName,
                temperature   = current.getDouble("temperature_2m"),
                feelsLike     = current.getDouble("apparent_temperature"),
                tempMin       = daily.getJSONArray("temperature_2m_min").getDouble(0),
                tempMax       = daily.getJSONArray("temperature_2m_max").getDouble(0),
                humidity      = current.getInt("relative_humidity_2m"),
                windSpeed     = current.getDouble("wind_speed_10m"),
                precipitation = current.optDouble("precipitation", 0.0),
                weatherCode   = current.getInt("weather_code"),
                et0Today      = et0Arr.optDouble(0, 0.0),
                et0Cumul2     = et0Sum2,
                et0Cumul5     = et0Sum5
            )
        }

    private fun get(url: String, userAgent: String = "AlmanachDuJardin/1.0"): String {
        val conn = URL(url).openConnection() as HttpsURLConnection
        conn.setRequestProperty("User-Agent", userAgent)
        conn.connectTimeout = 10_000
        conn.readTimeout    = 10_000
        return conn.inputStream.bufferedReader().readText()
    }
}
