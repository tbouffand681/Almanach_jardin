package com.almanach.jardin.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GardenTaskDao {

    @Query("SELECT * FROM garden_tasks WHERE month = :month ORDER BY done ASC, id ASC")
    fun getTasksForMonth(month: Int): Flow<List<GardenTask>>

    @Insert
    suspend fun insert(task: GardenTask): Long

    @Query("UPDATE garden_tasks SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Update
    suspend fun update(task: GardenTask)

    @Delete
    suspend fun delete(task: GardenTask)
}
