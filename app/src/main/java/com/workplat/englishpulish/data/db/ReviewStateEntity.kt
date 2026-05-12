package com.workplat.englishpulish.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_states",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("dueAt")],
)
data class ReviewStateEntity(
    @PrimaryKey val wordId: String,
    val stability: Double,
    val difficulty: Double,
    val lastReviewAt: Long?,
    val dueAt: Long,
    val state: Int,
    val lapses: Int,
    val reps: Int,
)
