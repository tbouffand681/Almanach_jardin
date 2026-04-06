package com.almanach.jardin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EventCategory(val label: String, val emoji: String) {
    FLORA    ("Flore",      "🌸"),
    FAUNA    ("Faune",      "🦜"),
    WEATHER  ("Météo",      "🌦️"),
    INSECT   ("Insecte",    "🦋"),
    MUSHROOM ("Champignon", "🍄"),
    OTHER    ("Autre",      "📝")
}

@Entity(tableName = "natural_events")
data class NaturalEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventDate: String,       // ISO-8601
    val category: EventCategory = EventCategory.FAUNA,
    val title: String,
    val description: String = "",
    val location: String = ""
)
