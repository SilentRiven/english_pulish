package com.workplat.englishpulish.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Query("SELECT * FROM words WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WordEntity>>

    @Query("SELECT COUNT(*) FROM words WHERE deletedAt IS NULL")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<WordEntity>)

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): WordEntity?
}
