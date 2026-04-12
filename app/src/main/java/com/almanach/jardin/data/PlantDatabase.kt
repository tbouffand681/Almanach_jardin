package com.almanach.jardin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Plant::class, Sowing::class, NaturalEvent::class, GardenTask::class],
    version = 5,
    exportSchema = false
)
abstract class PlantDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun sowingDao(): SowingDao
    abstract fun naturalEventDao(): NaturalEventDao
    abstract fun gardenTaskDao(): GardenTaskDao

    companion object {
        @Volatile private var INSTANCE: PlantDatabase? = null

        fun get(context: Context): PlantDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    PlantDatabase::class.java,
                    "almanach.db"
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5
                )
                .build().also { INSTANCE = it }
            }

        // v1 → v2 : création de la table garden_tasks (sans isDefault)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS garden_tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        month INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        done INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        // v2 → v3 : ajout colonne isDefault dans garden_tasks
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE garden_tasks ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        // v3 → v4 : pas de changement de schéma (bump de version uniquement)
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Aucune modification structurelle
            }
        }

        // v4 → v5 : ajout germinationTempMin et germinationTempMax dans plants
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE plants ADD COLUMN germinationTempMin INTEGER NOT NULL DEFAULT 10"
                )
                db.execSQL(
                    "ALTER TABLE plants ADD COLUMN germinationTempMax INTEGER NOT NULL DEFAULT 25"
                )
            }
        }
    }
}
