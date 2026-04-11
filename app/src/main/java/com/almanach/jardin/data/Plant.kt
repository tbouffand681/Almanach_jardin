package com.almanach.jardin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latinName: String = "",
    val emoji: String = "🌱",
    val category: String = "Légume",
    val sowingMonths: String = "",      // ex: "3,4,5"
    val occupationDays: Int = 90,
    val spacingCm: Int = 30,
    val sunExposure: String = "Plein soleil",
    val waterNeeds: String = "Moyen",
    val germinationDays: Int = 10,
    val germinationTempMin: Int = 10,
    val germinationTempMax: Int = 25,
    val notes: String = "",
    val isDefault: Boolean = false
)
