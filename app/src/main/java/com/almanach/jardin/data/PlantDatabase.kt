package com.almanach.jardin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Plant::class, Sowing::class, NaturalEvent::class],
    version = 3,
    exportSchema = false
)
abstract class PlantDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun sowingDao(): SowingDao
    abstract fun naturalEventDao(): NaturalEventDao

    companion object {
        @Volatile private var INSTANCE: PlantDatabase? = null

        fun get(context: Context): PlantDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    PlantDatabase::class.java,
                    "almanach.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}
