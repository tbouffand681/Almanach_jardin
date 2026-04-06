package com.almanach.jardin.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almanach.jardin.data.EventCategory
import com.almanach.jardin.data.NaturalEvent
import com.almanach.jardin.data.PlantDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JournalViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = PlantDatabase.get(app).naturalEventDao()

    val events: Flow<List<NaturalEvent>> = dao.getAll()

    fun addEvent(event: NaturalEvent, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { dao.insert(event) }
            }
            withContext(Dispatchers.Main) { onDone(result.isSuccess) }
        }
    }

    fun updateEvent(event: NaturalEvent, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { dao.update(event) }
            }
            withContext(Dispatchers.Main) { onDone(result.isSuccess) }
        }
    }

    fun deleteEvent(event: NaturalEvent, onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dao.delete(event) }
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    companion object {
        val CATEGORIES = EventCategory.values().toList()
    }
}
