package com.workplat.englishpulish.data.repo

import com.workplat.englishpulish.data.db.ReviewStateEntity
import com.workplat.englishpulish.data.db.WordDao
import com.workplat.englishpulish.data.db.WordEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordRepository @Inject constructor(
    private val wordDao: WordDao,
    private val reviewStateDao: com.workplat.englishpulish.data.db.ReviewStateDao,
) {
    fun observeAll(): Flow<List<WordEntity>> = wordDao.observeAll()

    suspend fun count(): Int = wordDao.count()

    suspend fun seedIfEmpty() {
        if (wordDao.count() > 0) return
        val now = System.currentTimeMillis()
        val sample = listOf(
            Triple("abandon", "v.", "放弃；抛弃"),
            Triple("ability", "n.", "能力；才能"),
            Triple("abroad", "adv.", "在国外；到国外"),
            Triple("absence", "n.", "缺席；不在"),
            Triple("absolute", "adj.", "绝对的；完全的"),
        )
        val words = sample.map { (lemma, pos, def) ->
            WordEntity(
                id = UUID.randomUUID().toString(),
                lemma = lemma,
                phonetic = null,
                partOfSpeech = pos,
                definitionZh = def,
                exampleEn = null,
                exampleZh = null,
                source = "seed",
                createdAt = now,
                deletedAt = null,
            )
        }
        wordDao.insertAll(words)
        reviewStateDao.upsertAll(
            words.map {
                ReviewStateEntity(
                    wordId = it.id,
                    stability = 0.0,
                    difficulty = 0.0,
                    lastReviewAt = null,
                    dueAt = now,
                    state = 0,
                    lapses = 0,
                    reps = 0,
                )
            }
        )
    }
}
