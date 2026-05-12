package com.workplat.englishpulish.domain.model

import com.workplat.englishpulish.domain.fsrs.FsrsState

/**
 * One card in the review queue: the word's display data plus its FSRS state.
 * Built on demand by [com.workplat.englishpulish.data.repo.ReviewRepository.todayQueue],
 * not stored in the DB.
 */
data class ReviewCard(
    val wordId: String,
    val lemma: String,
    val phonetic: String?,
    val partOfSpeech: String?,
    val definitionZh: String,
    val exampleEn: String?,
    val exampleZh: String?,
    val state: FsrsState,
)
