package com.almanach.jardin.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants ORDER BY name ASC")
    fun getAllPlants(): Flow<List<Plant>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(plant: Plant): Long

    @Update
    suspend fun update(plant: Plant)

    @Delete
    suspend fun delete(plant: Plant)

    @Query("SELECT COUNT(*) FROM plants WHERE isDefault = 1")
    suspend fun countDefaults(): Int

    @Query("SELECT * FROM plants WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): Plant?
}
