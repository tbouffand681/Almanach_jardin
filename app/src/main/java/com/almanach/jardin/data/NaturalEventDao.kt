package com.almanach.jardin.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NaturalEventDao {

    @Query("SELECT * FROM natural_events ORDER BY eventDate DESC")
    fun getAll(): Flow<List<NaturalEvent>>

    @Insert
    suspend fun insert(event: NaturalEvent): Long

    @Update
    suspend fun update(event: NaturalEvent)

    @Delete
    suspend fun delete(event: NaturalEvent)
}
