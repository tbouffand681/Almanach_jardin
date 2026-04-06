package com.almanach.jardin.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almanach.jardin.data.Plant
import com.almanach.jardin.data.PlantDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlantViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = PlantDatabase.get(app).plantDao()

    // Flow observé par le fragment — Room notifie automatiquement les changements
    val plants: Flow<List<Plant>> = dao.getAllPlants()

    /**
     * Insère une plante sur IO, puis appelle onDone(true) sur le Main thread.
     * Le dialog ne dismiss() que depuis ce callback — jamais depuis le UI thread directement.
     */
    fun addPlant(plant: Plant, onDone: (success: Boolean) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { dao.insert(plant) }
            }
            // Retour garanti sur Main thread
            withContext(Dispatchers.Main) {
                onDone(result.isSuccess && (result.getOrNull() ?: -1L) > 0)
            }
        }
    }

    fun updatePlant(plant: Plant, onDone: (success: Boolean) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { dao.update(plant) }
            }
            withContext(Dispatchers.Main) {
                onDone(result.isSuccess)
            }
        }
    }

    fun deletePlant(plant: Plant, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { dao.delete(plant) } }
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    // Insertion des plantes par défaut au premier lancement
    fun populateDefaultsIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            if (dao.countDefaults() > 0) return@launch
            defaultPlants.forEach { dao.insert(it) }
        }
    }

    companion object {
        val CATEGORIES = listOf(
            "Légume", "Légume fruit", "Légume racine", "Légume feuille",
            "Légumineuse", "Aromate", "Bulbe", "Tubercule", "Fleur", "Autre"
        )
        val SUN_OPTIONS = listOf("Plein soleil", "Mi-ombre", "Ombre")
        val WATER_OPTIONS = listOf("Faible", "Moyen", "Élevé")

        private val defaultPlants = listOf(
            Plant(name="Tomate", latinName="Solanum lycopersicum", emoji="🍅", category="Légume fruit", sowingMonths="2,3,4", occupationDays=150, spacingCm=60, sunExposure="Plein soleil", waterNeeds="Élevé", germinationDays=7, notes="Semer sous abri chauffé. Repiquer après les Saints de Glace.", isDefault=true),
            Plant(name="Carotte", latinName="Daucus carota", emoji="🥕", category="Légume racine", sowingMonths="3,4,5,6,7", occupationDays=90, spacingCm=5, sunExposure="Plein soleil", waterNeeds="Moyen", germinationDays=14, notes="Ameublir le sol en profondeur.", isDefault=true),
            Plant(name="Laitue", latinName="Lactuca sativa", emoji="🥬", category="Légume feuille", sowingMonths="2,3,4,5,6,7,8", occupationDays=60, spacingCm=25, sunExposure="Mi-ombre", waterNeeds="Moyen", germinationDays=5, notes="Éviter la chaleur excessive.", isDefault=true),
            Plant(name="Radis", latinName="Raphanus sativus", emoji="🔴", category="Légume racine", sowingMonths="2,3,4,5,8,9,10", occupationDays=30, spacingCm=5, sunExposure="Plein soleil", waterNeeds="Moyen", germinationDays=3, notes="Culture rapide. Idéal en intercalaire.", isDefault=true),
            Plant(name="Courgette", latinName="Cucurbita pepo", emoji="🥒", category="Légume fruit", sowingMonths="4,5", occupationDays=120, spacingCm=80, sunExposure="Plein soleil", waterNeeds="Élevé", germinationDays=5, notes="Récolter régulièrement.", isDefault=true),
            Plant(name="Haricot vert", latinName="Phaseolus vulgaris", emoji="🫘", category="Légumineuse", sowingMonths="5,6,7", occupationDays=75, spacingCm=15, sunExposure="Plein soleil", waterNeeds="Moyen", germinationDays=8, notes="Ne pas semer avant 15°C.", isDefault=true),
            Plant(name="Basilic", latinName="Ocimum basilicum", emoji="🌿", category="Aromate", sowingMonths="3,4,5", occupationDays=120, spacingCm=20, sunExposure="Plein soleil", waterNeeds="Moyen", germinationDays=8, notes="Sensible au froid. Pincer les fleurs.", isDefault=true),
            Plant(name="Poireau", latinName="Allium porrum", emoji="🌱", category="Légume", sowingMonths="2,3,4", occupationDays=180, spacingCm=15, sunExposure="Plein soleil", waterNeeds="Moyen", germinationDays=12, notes="Repiquer quand les plants ont la taille d'un crayon.", isDefault=true),
            Plant(name="Épinard", latinName="Spinacia oleracea", emoji="🌿", category="Légume feuille", sowingMonths="2,3,9,10", occupationDays=60, spacingCm=10, sunExposure="Mi-ombre", waterNeeds="Moyen", germinationDays=10, notes="Monte en graine à la chaleur.", isDefault=true),
            Plant(name="Ail", latinName="Allium sativum", emoji="🧄", category="Bulbe", sowingMonths="1,2,3,4", occupationDays=128, spacingCm=15, sunExposure="Plein soleil", waterNeeds="Faible", germinationDays=10, isDefault=true)
        )
    }
}
