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

    /** Old cards that have been seen at least once and are due now. */
    @Query(
        """
        SELECT * FROM review_states
        WHERE state != 0 AND dueAt <= :now
        ORDER BY dueAt ASC
        """
    )
    suspend fun dueOldCards(now: Long): List<ReviewStateEntity>

    /** Fresh "new" cards, ordered by lemma (alphabetical drip-feed). */
    @Query(
        """
        SELECT rs.* FROM review_states rs
        INNER JOIN words w ON w.id = rs.wordId
        WHERE rs.state = 0 AND rs.dueAt <= :now AND w.deletedAt IS NULL
        ORDER BY w.lemma ASC
        LIMIT :limit
        """
    )
    suspend fun dueNewCards(now: Long, limit: Int): List<ReviewStateEntity>

    /**
     * Today's queue size: old due cards + capped new cards. Used by the home
     * screen badge. Re-emits when either table changes.
     */
    @Query(
        """
        SELECT
          (SELECT COUNT(*) FROM review_states WHERE state != 0 AND dueAt <= :now) +
          MIN(
            (SELECT COUNT(*) FROM review_states rs2 INNER JOIN words w2 ON w2.id = rs2.wordId
             WHERE rs2.state = 0 AND rs2.dueAt <= :now AND w2.deletedAt IS NULL),
            :newLimit
          )
        """
    )
    fun observeTodayDueCount(now: Long, newLimit: Int): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(states: List<ReviewStateEntity>)

    @Update
    suspend fun update(state: ReviewStateEntity)

    @Query("SELECT * FROM review_states WHERE wordId = :wordId LIMIT 1")
    suspend fun findByWordId(wordId: String): ReviewStateEntity?
}
