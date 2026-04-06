package com.almanach.jardin.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almanach.jardin.data.PlantDatabase
import com.almanach.jardin.data.Sowing
import com.almanach.jardin.data.SowingStatus
import com.almanach.jardin.data.SowingWithPlant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SowingViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = PlantDatabase.get(app).sowingDao()

    val sowings: Flow<List<SowingWithPlant>> = dao.getAllSowingsWithPlant()

    fun addSowing(
        plantId: Long,
        sowingDate: LocalDate,
        location: String,
        quantity: Int,
        notes: String,
        occupationDays: Int,
        onDone: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
                    dao.insert(Sowing(
                        plantId = plantId,
                        sowingDate = sowingDate.format(fmt),
                        expectedHarvestDate = sowingDate.plusDays(occupationDays.toLong()).format(fmt),
                        location = location,
                        quantity = quantity,
                        notes = notes
                    ))
                }
            }
            withContext(Dispatchers.Main) { onDone(result.isSuccess) }
        }
    }

    fun updateSowing(
        sowingId: Long,
        plantId: Long,
        sowingDate: LocalDate,
        location: String,
        quantity: Int,
        notes: String,
        occupationDays: Int,
        onDone: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
                    dao.update(Sowing(
                        id = sowingId,
                        plantId = plantId,
                        sowingDate = sowingDate.format(fmt),
                        expectedHarvestDate = sowingDate.plusDays(occupationDays.toLong()).format(fmt),
                        location = location,
                        quantity = quantity,
                        notes = notes,
                        // Récupérer le statut existant pour ne pas l'écraser
                        status = dao.getById(sowingId)?.status ?: SowingStatus.SOWED
                    ))
                }
            }
            withContext(Dispatchers.Main) { onDone(result.isSuccess) }
        }
    }

    fun updateStatus(sowingId: Long, status: SowingStatus) {
        viewModelScope.launch(Dispatchers.IO) { dao.updateStatus(sowingId, status) }
    }

    fun deleteSowing(sowingId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dao.deleteById(sowingId) }
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    companion object {
        val STATUS_LABELS = listOf(
            SowingStatus.SOWED        to "🌰 Semé",
            SowingStatus.GERMINATED   to "🌱 Levée observée",
            SowingStatus.TRANSPLANTED to "🪴 Repiqué",
            SowingStatus.GROWING      to "🌿 En croissance",
            SowingStatus.HARVESTED    to "✅ Récolté",
            SowingStatus.FAILED       to "❌ Échec"
        )

        fun statusLabel(s: SowingStatus) =
            STATUS_LABELS.firstOrNull { it.first == s }?.second ?: s.name

        fun statusColor(s: SowingStatus) = when (s) {
            SowingStatus.SOWED        -> 0xFF795548.toInt()
            SowingStatus.GERMINATED   -> 0xFF4CAF50.toInt()
            SowingStatus.TRANSPLANTED -> 0xFF8BC34A.toInt()
            SowingStatus.GROWING      -> 0xFF2E7D32.toInt()
            SowingStatus.HARVESTED    -> 0xFF1565C0.toInt()
            SowingStatus.FAILED       -> 0xFFB71C1C.toInt()
        }
    }
}
