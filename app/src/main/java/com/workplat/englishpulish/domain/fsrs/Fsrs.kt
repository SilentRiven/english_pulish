package com.workplat.englishpulish.domain.fsrs

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * FSRS-4.5 spaced repetition algorithm — pure functions, no Android dependencies.
 *
 * Reference: https://github.com/open-spaced-repetition/fsrs4anki/wiki
 *
 * Inputs/outputs use [FsrsState] (snapshot of a card's memory state) plus a
 * [Rating]. We don't depend on Room types here, so the algorithm is unit-testable
 * standalone and reusable if we ever move to a non-Android target.
 *
 * Conventions:
 *  - All days are doubles (a card seen "0.5 days ago" is valid).
 *  - All timestamps the caller passes in are epoch millis; we convert internally.
 */
object Fsrs {

    /** Default FSRS-4.5 weights, 19 floats. Public so eventual tuning can rebind. */
    val DEFAULT_WEIGHTS: DoubleArray = doubleArrayOf(
        0.4072, 1.1829, 3.1262, 15.4722, 7.2102, 0.5316, 1.0651, 0.0234,
        1.616, 0.1544, 1.0824, 1.9813, 0.0953, 0.2975, 2.2042, 0.2407,
        2.9466, 0.5034, 0.6567,
    )

    /** Target probability of recall when we schedule the next review. */
    const val DEFAULT_DESIRED_RETENTION = 0.9

    /** Schedule a card's next review based on the new [rating]. */
    fun next(
        state: FsrsState,
        rating: Rating,
        nowMillis: Long,
        weights: DoubleArray = DEFAULT_WEIGHTS,
        desiredRetention: Double = DEFAULT_DESIRED_RETENTION,
    ): FsrsState {
        require(weights.size == 19) { "FSRS-4.5 expects 19 weights" }
        val w = weights

        // ─────────────────────────────────────────────────────────────────────
        // Branch 1: card has never been reviewed. Bootstrap initial S/D from w.
        // ─────────────────────────────────────────────────────────────────────
        if (state.lastReviewAt == null) {
            val newDifficulty = clampDifficulty(initDifficulty(w, rating))
            val newStability = max(initStability(w, rating), 0.1)
            val interval = nextInterval(newStability, desiredRetention)
            return state.copy(
                stability = newStability,
                difficulty = newDifficulty,
                state = if (rating == Rating.Again) CardState.Relearning else CardState.Learning,
                lastReviewAt = nowMillis,
                dueAt = nowMillis + daysToMillis(interval),
                lapses = if (rating == Rating.Again) state.lapses + 1 else state.lapses,
                reps = state.reps + 1,
            )
        }

        // ─────────────────────────────────────────────────────────────────────
        // Branch 2: a real review. Recompute S/D from the recall probability.
        // ─────────────────────────────────────────────────────────────────────
        val elapsedDays = millisToDays(nowMillis - state.lastReviewAt)
        val retrievability = currentRetrievability(state.stability, elapsedDays)

        val newDifficulty = clampDifficulty(nextDifficulty(w, state.difficulty, rating))
        val newStability = if (rating == Rating.Again) {
            // Forgetting curve: stability drops, but never below 0.1.
            max(
                w[11] *
                    state.difficulty.pow(-w[12]) *
                    ((state.stability + 1.0).pow(w[13]) - 1.0) *
                    exp((1.0 - retrievability) * w[14]),
                0.1,
            )
        } else {
            val hardPenalty = if (rating == Rating.Hard) w[15] else 1.0
            val easyBonus = if (rating == Rating.Easy) w[16] else 1.0
            state.stability * (
                1.0 +
                    exp(w[8]) *
                    (11.0 - newDifficulty) *
                    state.stability.pow(-w[9]) *
                    (exp((1.0 - retrievability) * w[10]) - 1.0) *
                    hardPenalty *
                    easyBonus
                )
        }
        val interval = nextInterval(newStability, desiredRetention)

        return state.copy(
            stability = newStability,
            difficulty = newDifficulty,
            state = if (rating == Rating.Again) CardState.Relearning else CardState.Review,
            lastReviewAt = nowMillis,
            dueAt = nowMillis + daysToMillis(interval),
            lapses = if (rating == Rating.Again) state.lapses + 1 else state.lapses,
            reps = state.reps + 1,
        )
    }

    /** Probability the user remembers the card right now. */
    fun currentRetrievability(stability: Double, elapsedDays: Double): Double {
        if (stability <= 0.0) return 0.0
        // FSRS-4.5 forgetting curve: R = (1 + t/(9*S))^(-1)
        return (1.0 + elapsedDays / (9.0 * stability)).pow(-1.0)
    }

    private fun initStability(w: DoubleArray, rating: Rating): Double =
        when (rating) {
            Rating.Again -> w[0]
            Rating.Hard -> w[1]
            Rating.Good -> w[2]
            Rating.Easy -> w[3]
        }

    private fun initDifficulty(w: DoubleArray, rating: Rating): Double {
        val ratingNum = rating.value
        // FSRS-4.5: D₀ = w[4] - exp(w[5] * (rating - 1)) + 1
        return w[4] - exp(w[5] * (ratingNum - 1.0)) + 1.0
    }

    private fun nextDifficulty(w: DoubleArray, oldD: Double, rating: Rating): Double {
        // Linear damping toward the "Good" anchor, blended with the per-rating delta.
        val delta = -w[6] * (rating.value - 3.0)
        val target = oldD + delta * ((10.0 - oldD) / 9.0)
        return meanReversion(w, initDifficultyForGood(w), target)
    }

    private fun initDifficultyForGood(w: DoubleArray): Double = w[4]

    private fun meanReversion(w: DoubleArray, init: Double, current: Double): Double =
        w[7] * init + (1.0 - w[7]) * current

    private fun clampDifficulty(d: Double): Double = min(max(d, 1.0), 10.0)

    /** Solve for t such that R(t) = desiredRetention. */
    private fun nextInterval(stability: Double, desiredRetention: Double): Double {
        if (stability <= 0.0) return 0.0
        // From R = (1 + t/(9*S))^(-1), invert: t = 9*S*(R^(-1) - 1)
        val raw = 9.0 * stability * (desiredRetention.pow(-1.0) - 1.0)
        // Clamp absurd values so the first "Good" doesn't schedule 5 years out.
        return min(max(raw, 1.0), 36_500.0)
    }

    private fun daysToMillis(days: Double): Long = (days * 86_400_000.0).toLong()
    private fun millisToDays(millis: Long): Double = millis.toDouble() / 86_400_000.0
}

/** User's self-rating after seeing the answer. Numeric values match FSRS spec. */
enum class Rating(val value: Int) {
    Again(1), Hard(2), Good(3), Easy(4);

    companion object {
        fun fromValue(value: Int): Rating =
            entries.firstOrNull { it.value == value } ?: Good
    }
}

/** Memory state of a single card. Mirrors `review_states` row, minus the wordId. */
data class FsrsState(
    val stability: Double,
    val difficulty: Double,
    val state: CardState,
    val lastReviewAt: Long?,
    val dueAt: Long,
    val lapses: Int,
    val reps: Int,
) {
    companion object {
        /** Fresh state for a card the user has never seen. */
        fun newCard(dueAt: Long): FsrsState = FsrsState(
            stability = 0.0,
            difficulty = 0.0,
            state = CardState.New,
            lastReviewAt = null,
            dueAt = dueAt,
            lapses = 0,
            reps = 0,
        )
    }
}

enum class CardState(val value: Int) {
    New(0), Learning(1), Review(2), Relearning(3);

    companion object {
        fun fromValue(value: Int): CardState =
            entries.firstOrNull { it.value == value } ?: New
    }
}
