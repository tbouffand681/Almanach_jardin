package com.almanach.jardin.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.almanach.jardin.data.NaturalEvent
import com.almanach.jardin.data.Plant
import com.almanach.jardin.data.PlantDatabase
import com.almanach.jardin.data.Sowing
import com.almanach.jardin.data.SowingStatus
import com.almanach.jardin.data.EventCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ExportResult(val message: String, val isSuccess: Boolean)

class ExportViewModel(app: Application) : AndroidViewModel(app) {

    private val db = PlantDatabase.get(app)
    private val plantDao  = db.plantDao()
    private val sowingDao = db.sowingDao()
    private val eventDao  = db.naturalEventDao()

    private val _result = MutableLiveData<ExportResult?>()
    val result: LiveData<ExportResult?> = _result

    fun clearResult() { _result.value = null }

    // ─── Export JSON ──────────────────────────────────────────────────────────

    fun buildExportJson(): suspend () -> String = {
        withContext(Dispatchers.IO) {
            val plants  = plantDao.getAllPlants().first()
            val sowings = sowingDao.getAllSowingsWithPlant().first()
            val events  = eventDao.getAll().first()
            val date    = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

            val root = JSONObject()
            root.put("exportDate", date)
            root.put("appVersion", "Almanach du jardin")

            // Plantes
            val plantsArr = JSONArray()
            plants.forEach { p ->
                plantsArr.put(JSONObject().apply {
                    put("nom", p.name)
                    put("nomLatin", p.latinName)
                    put("emoji", p.emoji)
                    put("categorie", p.category)
                    put("moisSemis", p.sowingMonths)
                    put("occupation", p.occupationDays)
                    put("espacement", p.spacingCm)
                    put("exposition", p.sunExposure)
                    put("eau", p.waterNeeds)
                    put("germination", p.germinationDays)
                    put("notes", p.notes)
                    put("defaut", p.isDefault)
                })
            }
            root.put("plantes", plantsArr)

            // Semis
            val semisArr = JSONArray()
            sowings.forEach { s ->
                semisArr.put(JSONObject().apply {
                    put("plante", s.plantName)
                    put("date", s.sowingDate)
                    put("recolte", s.expectedHarvestDate)
                    put("emplacement", s.location)
                    put("quantite", s.quantity)
                    put("statut", s.status.name)
                    put("notes", s.notes)
                })
            }
            root.put("semis", semisArr)

            // Observations
            val obsArr = JSONArray()
            events.forEach { e ->
                obsArr.put(JSONObject().apply {
                    put("date", e.eventDate)
                    put("categorie", e.category.name)
                    put("titre", e.title)
                    put("description", e.description)
                    put("lieu", e.location)
                })
            }
            root.put("observations", obsArr)

            root.toString(2)  // JSON indenté
        }
    }

    // ─── Import JSON ──────────────────────────────────────────────────────────

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val content = getApplication<Application>().contentResolver
                        .openInputStream(uri)
                        ?.use { it.bufferedReader(Charsets.UTF_8).readText() }
                        ?: error("Impossible de lire le fichier")

                    if (content.isBlank()) error("Le fichier est vide")
                    val json = JSONObject(content)

                    var plants = 0; var semis = 0; var obs = 0

                    // Importer les plantes — ignorer les doublons (même nom)
                    var plantsSkipped = 0
                    json.optJSONArray("plantes")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            runCatching {
                                val o = arr.getJSONObject(i)
                                val name = o.optString("nom", "").trim()
                                if (name.isEmpty()) return@runCatching
                                // Vérifier si une plante de ce nom existe déjà
                                if (plantDao.findByName(name) != null) {
                                    plantsSkipped++
                                    return@runCatching
                                }
                                plantDao.insert(Plant(
                                    name            = name,
                                    latinName       = o.optString("nomLatin", ""),
                                    emoji           = o.optString("emoji", "🌱"),
                                    category        = o.optString("categorie", "Légume"),
                                    sowingMonths    = o.optString("moisSemis", ""),
                                    occupationDays  = o.optInt("occupation", 90),
                                    spacingCm       = o.optInt("espacement", 30),
                                    sunExposure     = o.optString("exposition", "Plein soleil"),
                                    waterNeeds      = o.optString("eau", "Moyen"),
                                    germinationDays = o.optInt("germination", 10),
                                    notes           = o.optString("notes", ""),
                                    isDefault       = o.optBoolean("defaut", false)
                                ))
                                plants++
                            }
                        }
                    }

                    // Importer les semis — retrouver la plante par nom
                    json.optJSONArray("semis")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            runCatching {
                                val o = arr.getJSONObject(i)
                                val plantName = o.optString("plante", "").trim()
                                val dateStr   = o.optString("date", "").trim()
                                if (plantName.isEmpty() || dateStr.isEmpty()) return@runCatching

                                val plant = plantDao.getAllPlants().first()
                                    .firstOrNull { it.name.equals(plantName, ignoreCase = true) }
                                    ?: return@runCatching

                                val fmt     = DateTimeFormatter.ISO_LOCAL_DATE
                                val sowDate = LocalDate.parse(dateStr, fmt)
                                val harvest = LocalDate.parse(
                                    o.optString("recolte", sowDate.plusDays(plant.occupationDays.toLong()).format(fmt)), fmt
                                )
                                val status = runCatching {
                                    SowingStatus.valueOf(o.optString("statut", "SOWED"))
                                }.getOrDefault(SowingStatus.SOWED)

                                sowingDao.insert(Sowing(
                                    plantId             = plant.id,
                                    sowingDate          = sowDate.format(fmt),
                                    expectedHarvestDate = harvest.format(fmt),
                                    location            = o.optString("emplacement", ""),
                                    quantity            = o.optInt("quantite", 1),
                                    status              = status,
                                    notes               = o.optString("notes", "")
                                ))
                                semis++
                            }
                        }
                    }

                    // Importer les observations
                    json.optJSONArray("observations")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            runCatching {
                                val o = arr.getJSONObject(i)
                                val titre = o.optString("titre", "").trim()
                                if (titre.isEmpty()) return@runCatching
                                val cat = runCatching {
                                    EventCategory.valueOf(o.optString("categorie", "OTHER"))
                                }.getOrDefault(EventCategory.OTHER)
                                eventDao.insert(NaturalEvent(
                                    eventDate   = o.optString("date", LocalDate.now().toString()),
                                    category    = cat,
                                    title       = titre,
                                    description = o.optString("description", ""),
                                    location    = o.optString("lieu", "")
                                ))
                                obs++
                            }
                        }
                    }

                    listOf(plants, semis, obs, plantsSkipped)
                }
            }

            result.fold(
                onSuccess = { list ->
                    val (p, s, o, skipped) = list
                    val msg = if (p == 0 && s == 0 && o == 0 && skipped == 0)
                        "⚠️ Aucune donnée importée — vérifiez le fichier"
                    else buildString {
                        append("✅ Importé : $p plante(s), $s semis, $o observation(s)")
                        if (skipped > 0) append(" · $skipped doublon(s) ignoré(s)")
                    }
                    _result.postValue(ExportResult(msg, p + s + o > 0))
                },
                onFailure = { e ->
                    _result.postValue(ExportResult("❌ Erreur : ${e.message}", false))
                }
            )
        }
    }
}
