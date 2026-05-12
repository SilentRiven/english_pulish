package com.workplat.englishpulish.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("wordId"), Index("reviewedAt")],
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: String,
    val reviewedAt: Long,
    val rating: Int,
    val elapsedDays: Double,
    val scheduledDays: Double,
    val stabilityBefore: Double,
    val stabilityAfter: Double,
)
