package com.almanach.jardin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "garden_tasks")
data class GardenTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: Int,          // 1..12
    val title: String,
    val done: Boolean = false,
    val isDefault: Boolean = false   // true = tâche verger pré-remplie
)
