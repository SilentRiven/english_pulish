package com.workplat.englishpulish.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewStateDao {

    @Query("SELECT * FROM review_states WHERE dueAt <= :now ORDER BY dueAt ASC")
    fun observeDue(now: Long): Flow<List<ReviewStateEntity>>

    @Query("SELECT COUNT(*) FROM review_states WHERE dueAt <= :now")
    suspend fun countDue(now: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(states: List<ReviewStateEntity>)

    @Update
    suspend fun update(state: ReviewStateEntity)

    @Query("SELECT * FROM review_states WHERE wordId = :wordId LIMIT 1")
    suspend fun findByWordId(wordId: String): ReviewStateEntity?
}
