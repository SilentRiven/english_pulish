package com.workplat.englishpulish.data.repo

import com.workplat.englishpulish.data.db.ReviewLogDao
import com.workplat.englishpulish.data.db.ReviewLogEntity
import com.workplat.englishpulish.data.db.ReviewStateDao
import com.workplat.englishpulish.data.db.ReviewStateEntity
import com.workplat.englishpulish.data.db.WordDao
import com.workplat.englishpulish.domain.fsrs.CardState
import com.workplat.englishpulish.domain.fsrs.Fsrs
import com.workplat.englishpulish.domain.fsrs.FsrsState
import com.workplat.englishpulish.domain.fsrs.Rating
import com.workplat.englishpulish.domain.model.ReviewCard
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val wordDao: WordDao,
    private val reviewStateDao: ReviewStateDao,
    private val reviewLogDao: ReviewLogDao,
) {

    /**
     * Today's queue, ordered: all due "old" cards first, then up to [newLimit]
     * fresh cards. We hydrate the words in one round-trip to avoid N+1.
     */
    suspend fun todayQueue(newLimit: Int = DEFAULT_NEW_LIMIT): List<ReviewCard> {
        val now = System.currentTimeMillis()
        val oldStates = reviewStateDao.dueOldCards(now)
        val newStates = reviewStateDao.dueNewCards(now, newLimit)
        val orderedStates = oldStates + newStates
        if (orderedStates.isEmpty()) return emptyList()

        val words = wordDao.findByIds(orderedStates.map { it.wordId }).associateBy { it.id }

        return orderedStates.mapNotNull { rs ->
            val w = words[rs.wordId] ?: return@mapNotNull null
            ReviewCard(
                wordId = w.id,
                lemma = w.lemma,
                phonetic = w.phonetic,
                partOfSpeech = w.partOfSpeech,
                definitionZh = w.definitionZh,
                exampleEn = w.exampleEn,
                exampleZh = w.exampleZh,
                state = rs.toFsrsState(),
            )
        }
    }

    fun observeTodayDueCount(newLimit: Int = DEFAULT_NEW_LIMIT): Flow<Int> =
        reviewStateDao.observeTodayDueCount(System.currentTimeMillis(), newLimit)

    /**
     * Apply a rating: run FSRS, persist the new state, and append a log row so
     * the future "vocabulary profile" page has the raw history.
     */
    suspend fun rate(card: ReviewCard, rating: Rating) {
        val now = System.currentTimeMillis()
        val before = card.state
        val after = Fsrs.next(before, rating, now)

        reviewStateDao.update(after.toEntity(card.wordId))
        reviewLogDao.insert(
            ReviewLogEntity(
                wordId = card.wordId,
                reviewedAt = now,
                rating = rating.value,
                elapsedDays = before.lastReviewAt?.let { (now - it) / 86_400_000.0 } ?: 0.0,
                scheduledDays = (after.dueAt - now) / 86_400_000.0,
                stabilityBefore = before.stability,
                stabilityAfter = after.stability,
            )
        )
    }

    companion object {
        const val DEFAULT_NEW_LIMIT = 20
    }
}

private fun ReviewStateEntity.toFsrsState(): FsrsState = FsrsState(
    stability = stability,
    difficulty = difficulty,
    state = CardState.fromValue(state),
    lastReviewAt = lastReviewAt,
    dueAt = dueAt,
    lapses = lapses,
    reps = reps,
)

private fun FsrsState.toEntity(wordId: String): ReviewStateEntity = ReviewStateEntity(
    wordId = wordId,
    stability = stability,
    difficulty = difficulty,
    lastReviewAt = lastReviewAt,
    dueAt = dueAt,
    state = state.value,
    lapses = lapses,
    reps = reps,
)
