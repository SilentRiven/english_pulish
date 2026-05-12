package com.workplat.englishpulish.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ReviewLogDao {

    @Insert
    suspend fun insert(log: ReviewLogEntity): Long

    @Query("SELECT COUNT(*) FROM review_logs WHERE reviewedAt >= :sinceMillis")
    suspend fun countSince(sinceMillis: Long): Int
}
