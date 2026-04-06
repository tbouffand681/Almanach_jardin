package com.almanach.jardin.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SowingStatus {
    SOWED,        // 🌰 Semé
    GERMINATED,   // 🌱 Levée
    TRANSPLANTED, // 🪴 Repiqué
    GROWING,      // 🌿 Croissance
    HARVESTED,    // ✅ Récolté
    FAILED        // ❌ Échec
}

@Entity(
    tableName = "sowings",
    foreignKeys = [ForeignKey(
        entity = Plant::class,
        parentColumns = ["id"],
        childColumns = ["plantId"],
        onDelete = ForeignKey.CASCADE   // Supprimer la plante → supprime ses semis
    )],
    indices = [Index("plantId")]
)
data class Sowing(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val sowingDate: String,          // ISO-8601 "2026-04-04"
    val expectedHarvestDate: String, // sowingDate + occupationDays
    val location: String = "",
    val quantity: Int = 1,
    val status: SowingStatus = SowingStatus.SOWED,
    val notes: String = ""
)
