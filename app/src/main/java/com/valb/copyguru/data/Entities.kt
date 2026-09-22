package com.valb.copyguru.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "segments")
data class Segment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    val position: Int = 0
)

@Entity(
    tableName = "copies",
    foreignKeys = [
        ForeignKey(
            entity = Segment::class,
            parentColumns = ["id"],
            childColumns = ["segmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("segmentId")]
)
data class Copy(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val segmentId: Long,
    val title: String,
    val content: String,
    val favorite: Boolean = false,
    val usageCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

data class SegmentWithCount(
    val id: Long,
    val name: String,
    val color: Int,
    val position: Int,
    val copyCount: Int
)
