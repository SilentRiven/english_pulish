package com.workplat.englishpulish.data.db

import androidx.paging.PagingSource
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

    @Query(
        """
        SELECT COUNT(*) FROM words
        WHERE deletedAt IS NULL
          AND lemma LIKE :queryPrefix || '%'
          AND (:sourcesEmpty OR source IN (:sources))
        """
    )
    fun observeFilteredCount(
        queryPrefix: String,
        sources: List<String>,
        sourcesEmpty: Boolean,
    ): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<WordEntity>)

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): WordEntity?

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun findByIds(ids: List<String>): List<WordEntity>

    @Query("SELECT * FROM words WHERE lemma = :lemma AND deletedAt IS NULL LIMIT 1")
    suspend fun findByLemma(lemma: String): WordEntity?

    /**
     * Paginated lemma list with optional prefix search and source filter.
     *
     * - When [query] is empty, the LIKE branch matches everything (`%` prefix).
     * - When [sources] is empty (size 0), the filter branch is bypassed via
     *   the `:sourcesEmpty` flag so we don't have to spread an empty IN list.
     *
     * Orders alphabetically — stable for a browse experience. Index on `lemma`
     * (unique, already declared) makes the prefix scan O(log n + page size).
     */
    @Query(
        """
        SELECT * FROM words
        WHERE deletedAt IS NULL
          AND lemma LIKE :queryPrefix || '%'
          AND (:sourcesEmpty OR source IN (:sources))
        ORDER BY lemma ASC
        """
    )
    fun pagingSource(
        queryPrefix: String,
        sources: List<String>,
        sourcesEmpty: Boolean,
    ): PagingSource<Int, WordEntity>
}
