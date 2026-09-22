package com.valb.copyguru.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CopyGuruDao {

    @Query("SELECT * FROM segments ORDER BY position ASC, name ASC")
    fun observeSegments(): Flow<List<Segment>>

    @Query(
        """
        SELECT s.id, s.name, s.color, s.position,
               (SELECT COUNT(*) FROM copies c WHERE c.segmentId = s.id) AS copyCount
        FROM segments s
        ORDER BY s.position ASC, s.name ASC
        """
    )
    fun observeSegmentsWithCount(): Flow<List<SegmentWithCount>>

    @Query("SELECT * FROM segments ORDER BY position ASC, name ASC")
    suspend fun getSegments(): List<Segment>

    @Insert
    suspend fun insertSegment(segment: Segment): Long

    @Update
    suspend fun updateSegment(segment: Segment)

    @Delete
    suspend fun deleteSegment(segment: Segment)

    @Query("SELECT * FROM copies ORDER BY favorite DESC, updatedAt DESC")
    fun observeAllCopies(): Flow<List<Copy>>

    @Query("SELECT * FROM copies WHERE segmentId = :segmentId ORDER BY favorite DESC, updatedAt DESC")
    fun observeCopiesBySegment(segmentId: Long): Flow<List<Copy>>

    @Query("SELECT * FROM copies WHERE favorite = 1 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<Copy>>

    @Query("SELECT * FROM copies WHERE segmentId = :segmentId ORDER BY favorite DESC, updatedAt DESC")
    suspend fun getCopiesBySegment(segmentId: Long): List<Copy>

    @Query("SELECT * FROM copies WHERE favorite = 1 ORDER BY updatedAt DESC")
    suspend fun getFavorites(): List<Copy>

    @Query("SELECT * FROM copies WHERE id = :id")
    suspend fun getCopy(id: Long): Copy?

    @Insert
    suspend fun insertCopy(copy: Copy): Long

    @Update
    suspend fun updateCopy(copy: Copy)

    @Delete
    suspend fun deleteCopy(copy: Copy)

    @Query("UPDATE copies SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE copies SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun bumpUsage(id: Long)

    @Query("SELECT COUNT(*) FROM segments")
    suspend fun segmentCount(): Int
}
