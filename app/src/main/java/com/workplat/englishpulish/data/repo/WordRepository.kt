package com.workplat.englishpulish.data.repo

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.workplat.englishpulish.data.db.AppDatabase
import com.workplat.englishpulish.data.db.ReviewStateDao
import com.workplat.englishpulish.data.db.ReviewStateEntity
import com.workplat.englishpulish.data.db.WordDao
import com.workplat.englishpulish.data.db.WordEntity
import com.workplat.englishpulish.data.preload.PreloadSource
import com.workplat.englishpulish.domain.text.TextParser
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AddResult {
    data class Added(val wordId: String, val lemma: String) : AddResult
    data class Duplicate(val lemma: String) : AddResult
    data object Invalid : AddResult
}

/**
 * Filter applied to the word browser page. Empty [sources] means "no source filter"
 * (show all). [query] is matched as a case-insensitive prefix on lemma — empty
 * string matches every row.
 */
data class WordFilter(
    val query: String = "",
    val sources: Set<String> = emptySet(),
)

@Singleton
class WordRepository @Inject constructor(
    private val database: AppDatabase,
    private val wordDao: WordDao,
    private val reviewStateDao: ReviewStateDao,
    private val preloadSource: PreloadSource,
) {
    fun observeAll(): Flow<List<WordEntity>> = wordDao.observeAll()

    suspend fun count(): Int = wordDao.count()

    fun observeFilteredCount(filter: WordFilter): Flow<Int> =
        wordDao.observeFilteredCount(
            queryPrefix = filter.query.trim().lowercase(),
            sources = filter.sources.toList(),
            sourcesEmpty = filter.sources.isEmpty(),
        )

    fun pager(filter: WordFilter): Flow<PagingData<WordEntity>> =
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE * 2,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                wordDao.pagingSource(
                    queryPrefix = filter.query.trim().lowercase(),
                    sources = filter.sources.toList(),
                    sourcesEmpty = filter.sources.isEmpty(),
                )
            },
        ).flow

    suspend fun addOne(
        rawLemma: String,
        definition: String = "",
        source: String = "manual",
    ): AddResult {
        val lemma = TextParser.normalize(rawLemma)
        if (lemma.length < 2) return AddResult.Invalid

        wordDao.findByLemma(lemma)?.let { return AddResult.Duplicate(it.lemma) }

        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val word = WordEntity(
            id = id,
            lemma = lemma,
            phonetic = null,
            partOfSpeech = null,
            definitionZh = definition.trim(),
            exampleEn = null,
            exampleZh = null,
            source = source,
            createdAt = now,
            deletedAt = null,
        )
        database.withTransaction {
            wordDao.insertAll(listOf(word))
            reviewStateDao.upsertAll(
                listOf(
                    ReviewStateEntity(
                        wordId = id,
                        stability = 0.0,
                        difficulty = 0.0,
                        lastReviewAt = null,
                        dueAt = now,
                        state = 0,
                        lapses = 0,
                        reps = 0,
                    )
                )
            )
        }
        return AddResult.Added(wordId = id, lemma = lemma)
    }

    suspend fun addFromShare(rawLemma: String): AddResult =
        addOne(rawLemma, source = "share")

    /**
     * Populate the library from bundled preload.json the first time the app runs.
     * No-op once the table is non-empty. Both inserts run in a single transaction
     * so the ~6000-row seed lands as one disk write.
     */
    suspend fun seedIfEmpty() {
        if (wordDao.count() > 0) return
        val entries = preloadSource.load()
        val now = System.currentTimeMillis()

        val words = ArrayList<WordEntity>(entries.size)
        val states = ArrayList<ReviewStateEntity>(entries.size)

        entries.forEach { e ->
            val id = UUID.randomUUID().toString()
            words += WordEntity(
                id = id,
                lemma = e.lemma,
                phonetic = e.phonetic,
                partOfSpeech = e.partOfSpeech,
                definitionZh = e.definitionZh,
                exampleEn = e.exampleEn,
                exampleZh = e.exampleZh,
                source = "preload-${e.level}",
                createdAt = now,
                deletedAt = null,
            )
            states += ReviewStateEntity(
                wordId = id,
                stability = 0.0,
                difficulty = 0.0,
                lastReviewAt = null,
                dueAt = now,
                state = 0,
                lapses = 0,
                reps = 0,
            )
        }

        database.withTransaction {
            wordDao.insertAll(words)
            reviewStateDao.upsertAll(states)
        }
    }

    companion object {
        private const val PAGE_SIZE = 40
    }
}
