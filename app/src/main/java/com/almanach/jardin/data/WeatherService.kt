package com.almanach.jardin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.net.ssl.HttpsURLConnection

// ─── Suggestion ville ─────────────────────────────────────────────────────────

data class CitySuggestion(
    val displayLabel: String,
    val searchName: String,
    val lat: Double,
    val lon: Double
)

// ─── Météo d'un jour ──────────────────────────────────────────────────────────

data class DayWeather(
    val label: String,          // "Aujourd'hui", "Demain", "Lun. 21/04"…
    val weatherCode: Int,
    val tempMin: Double,
    val tempMax: Double,
    val humidity: Int,          // moyenne du jour (%)
    val windSpeed: Double,      // max du jour (km/h)
    val precipitation: Double   // mm
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
}

// ─── Avertissement agronomique ────────────────────────────────────────────────

data class AgroWarning(val emoji: String, val message: String)

// ─── Résultat complet ─────────────────────────────────────────────────────────

data class WeatherResult(
    val cityName: String,
    val days: List<DayWeather>,         // J à J+5 (6 entrées)
    // ET₀
    val et0Today: Double,
    val et0Cumul48h: Double,            // J-1 + J (48h passées)
    val et0Cumul5d: Double,             // J-4 + J (5 jours passés)
    // Précipitations cumulées passées
    val precipToday: Double,
    val precipCumul48h: Double,
    val precipCumul5d: Double,
    // Avertissements
    val warnings: List<AgroWarning>
)

// ─── Service réseau ───────────────────────────────────────────────────────────

object WeatherService {

    private val ISO = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun fetchSuggestions(query: String): Result<List<CitySuggestion>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "https://nominatim.openstreetmap.org/search" +
                    "?q=$encoded&format=json&limit=5&accept-language=fr&featuretype=city,town,village"
                val arr = JSONArray(get(url, userAgent = "AlmanachDuJardin/1.0 Android"))
                (0 until arr.length()).mapNotNull { i ->
                    val obj  = arr.getJSONObject(i)
                    val name = obj.optString("display_name", "")
                    val lat  = obj.optDouble("lat", 0.0)
                    val lon  = obj.optDouble("lon", 0.0)
                    if (name.isEmpty()) return@mapNotNull null
                    val parts = name.split(",").map { it.trim() }
                    CitySuggestion(
                        displayLabel = buildLabel(parts),
                        searchName   = parts.firstOrNull() ?: name,
                        lat = lat, lon = lon
                    )
                }
            }
        }

    private fun buildLabel(parts: List<String>): String {
        val city   = parts.firstOrNull() ?: ""
        val postal = parts.firstOrNull { it.matches(Regex("\\d{4,6}")) } ?: ""
        val country= parts.lastOrNull() ?: ""
        return buildString {
            append(city)
            if (postal.isNotEmpty()) append(", $postal")
            if (country.isNotEmpty() && country != city) append(", $country")
        }.take(60)
    }

    suspend fun fetchByCity(city: String): Result<WeatherResult> =
        withContext(Dispatchers.IO) {
            runCatching {
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

    suspend fun fetchByCoords(lat: Double, lon: Double, cityName: String): WeatherResult =
        withContext(Dispatchers.IO) {
            val today = LocalDate.now()

            // ── 1. Prévisions J à J+5 ──────────────────────────────────────
            val forecastUrl = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=$lat&longitude=$lon" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min," +
                "precipitation_sum,wind_speed_10m_max,relative_humidity_2m_mean," +
                "et0_fao_evapotranspiration" +
                "&timezone=auto&forecast_days=6"

            val forecast = JSONObject(get(forecastUrl))
            val fd       = forecast.getJSONObject("daily")
            val fDates   = fd.getJSONArray("time")
            val fCodes   = fd.getJSONArray("weather_code")
            val fTmax    = fd.getJSONArray("temperature_2m_max")
            val fTmin    = fd.getJSONArray("temperature_2m_min")
            val fPrecip  = fd.getJSONArray("precipitation_sum")
            val fWind    = fd.getJSONArray("wind_speed_10m_max")
            val fHumid   = fd.getJSONArray("relative_humidity_2m_mean")
            val fEt0     = fd.getJSONArray("et0_fao_evapotranspiration")

            val days = (0 until fDates.length()).map { i ->
                val date = LocalDate.parse(fDates.getString(i), ISO)
                val label = when (i) {
                    0    -> "Aujourd'hui"
                    1    -> "Demain"
                    else -> {
                        val dayName = date.dayOfWeek.getDisplayName(
                            java.time.format.TextStyle.SHORT, java.util.Locale.FRENCH
                        ).replaceFirstChar { it.uppercase() }
                        "$dayName ${date.dayOfMonth}/${date.monthValue}"
                    }
                }
                DayWeather(
                    label         = label,
                    weatherCode   = fCodes.optInt(i, 0),
                    tempMin       = fTmin.optDouble(i, 0.0),
                    tempMax       = fTmax.optDouble(i, 0.0),
                    humidity      = fHumid.optInt(i, 0),
                    windSpeed     = fWind.optDouble(i, 0.0),
                    precipitation = fPrecip.optDouble(i, 0.0)
                )
            }

            // ET₀ aujourd'hui (depuis prévisions J=0)
            val et0Today = fEt0.optDouble(0, 0.0)

            // ── 2. Historique J-4 → J-1 pour ET₀ et précip cumulées ────────
            val startDate = today.minusDays(4).format(ISO)
            val endDate   = today.minusDays(1).format(ISO)

            val histUrl = "https://archive-api.open-meteo.com/v1/archive?" +
                "latitude=$lat&longitude=$lon" +
                "&start_date=$startDate&end_date=$endDate" +
                "&daily=precipitation_sum,et0_fao_evapotranspiration" +
                "&timezone=auto"

            val hist   = JSONObject(get(histUrl))
            val hd     = hist.getJSONObject("daily")
            val hEt0   = hd.getJSONArray("et0_fao_evapotranspiration")
            val hPrecip= hd.getJSONArray("precipitation_sum")

            // hEt0[0]=J-4, [1]=J-3, [2]=J-2, [3]=J-1
            val et0Cumul48h = hEt0.optDouble(3, 0.0) + et0Today           // J-1 + J
            val et0Cumul5d  = (0 until hEt0.length()).sumOf { hEt0.optDouble(it, 0.0) } + et0Today

            val precipToday    = fPrecip.optDouble(0, 0.0)
            val precipCumul48h = hPrecip.optDouble(3, 0.0) + precipToday   // J-1 + J
            val precipCumul5d  = (0 until hPrecip.length()).sumOf { hPrecip.optDouble(it, 0.0) } + precipToday

            // ── 3. Avertissements agronomiques ─────────────────────────────
            val warnings = mutableListOf<AgroWarning>()

            // Gel dans les 6 jours
            val gelJours = days.filter { it.tempMin < 2.0 }
            if (gelJours.isNotEmpty()) {
                val jours = gelJours.joinToString(", ") { it.label }
                warnings += AgroWarning("❄️", "Risque de gel : $jours (T° min < 2°C). Protégez semis et fruitiers en fleurs.")
            }

            // Canicule
            val caniculeJours = days.filter { it.tempMax > 35.0 }
            if (caniculeJours.isNotEmpty()) {
                val jours = caniculeJours.joinToString(", ") { it.label }
                warnings += AgroWarning("🔥", "Canicule : $jours (T° max > 35°C). Arrosez tôt le matin, paillez.")
            }

            // Carpocapse : T° > 10°C plusieurs jours consécutifs
            val carpoJours = days.count { it.tempMax > 10.0 && it.tempMin > 10.0 }
            if (carpoJours >= 3) {
                warnings += AgroWarning("🐛", "Conditions favorables au carpocapse ($carpoJours jours > 10°C). Vérifiez vos pièges à phéromones.")
            }

            // Rouille / mildiou : humidité > 80% + T° entre 10 et 25°C
            val rouilleJours = days.filter { it.humidity > 80 && it.tempMax in 10.0..25.0 }
            if (rouilleJours.isNotEmpty()) {
                val jours = rouilleJours.joinToString(", ") { it.label }
                warnings += AgroWarning("🍄", "Risque rouille/mildiou : $jours (humidité > 80%, T° 10-25°C). Envisagez un traitement préventif.")
            }

            // Sécheresse : ET₀ 5j >> précipitations 5j
            val deficitHydrique = et0Cumul5d - precipCumul5d
            if (deficitHydrique > 20.0) {
                warnings += AgroWarning("💧", "Déficit hydrique important sur 5 jours : ET₀ ${f(et0Cumul5d)} mm vs ${f(precipCumul5d)} mm de pluie. Arrosage recommandé.")
            }

            WeatherResult(
                cityName       = cityName,
                days           = days,
                et0Today       = et0Today,
                et0Cumul48h    = et0Cumul48h,
                et0Cumul5d     = et0Cumul5d,
                precipToday    = precipToday,
                precipCumul48h = precipCumul48h,
                precipCumul5d  = precipCumul5d,
                warnings       = warnings
            )
        }

    private fun f(v: Double) = "%.1f".format(v)

    private fun get(url: String, userAgent: String = "AlmanachDuJardin/1.0"): String {
        val conn = URL(url).openConnection() as HttpsURLConnection
        conn.setRequestProperty("User-Agent", userAgent)
        conn.connectTimeout = 10_000
        conn.readTimeout    = 15_000
        return conn.inputStream.bufferedReader().readText()
    }
}
