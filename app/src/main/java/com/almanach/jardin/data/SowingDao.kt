package com.almanach.jardin.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class SowingWithPlant(
    val sowingId: Long,
    val plantId: Long,
    val plantName: String,
    val plantEmoji: String,
    val sowingDate: String,
    val expectedHarvestDate: String,
    val location: String,
    val quantity: Int,
    val status: SowingStatus,
    val notes: String
)

@Dao
interface SowingDao {

    @Query("""
        SELECT s.id AS sowingId, s.plantId, p.name AS plantName, p.emoji AS plantEmoji,
               s.sowingDate, s.expectedHarvestDate, s.location, s.quantity, s.status, s.notes
        FROM sowings s
        INNER JOIN plants p ON s.plantId = p.id
        ORDER BY s.sowingDate DESC
    """)
    fun getAllSowingsWithPlant(): Flow<List<SowingWithPlant>>

    @Insert
    suspend fun insert(sowing: Sowing): Long

    @Update
    suspend fun update(sowing: Sowing)

    @Query("UPDATE sowings SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: SowingStatus)

    @Query("DELETE FROM sowings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM sowings WHERE id = :id")
    suspend fun getById(id: Long): Sowing?
}
