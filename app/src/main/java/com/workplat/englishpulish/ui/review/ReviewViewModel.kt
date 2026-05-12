package com.workplat.englishpulish.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workplat.englishpulish.data.repo.ReviewRepository
import com.workplat.englishpulish.domain.fsrs.Rating
import com.workplat.englishpulish.domain.model.ReviewCard
import com.workplat.englishpulish.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State machine of the review screen.
 *
 * - [Loading]: initial queue fetch in flight.
 * - [Front]: showing the lemma, awaiting "show answer" tap.
 * - [Back]: answer revealed, awaiting a rating.
 * - [Done]: queue exhausted (or empty from the start).
 *
 * The queue position lives in [ReviewUiState.cursor]; we don't pop cards from
 * the list so we can show "card 5 of 20"-style progress without re-querying.
 */
sealed interface ReviewUiState {
    data object Loading : ReviewUiState
    data class Front(val cards: List<ReviewCard>, val cursor: Int, val reviewedCount: Int) : ReviewUiState
    data class Back(val cards: List<ReviewCard>, val cursor: Int, val reviewedCount: Int) : ReviewUiState
    data class Done(val reviewedCount: Int) : ReviewUiState
}

val ReviewUiState.currentCard: ReviewCard?
    get() = when (this) {
        is ReviewUiState.Front -> cards.getOrNull(cursor)
        is ReviewUiState.Back -> cards.getOrNull(cursor)
        else -> null
    }

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repository: ReviewRepository,
    private val tts: TtsManager,
) : ViewModel() {

    private val _state = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    init {
        loadQueue()
    }

    private fun loadQueue() {
        viewModelScope.launch {
            val queue = repository.todayQueue()
            _state.value = if (queue.isEmpty()) {
                ReviewUiState.Done(reviewedCount = 0)
            } else {
                ReviewUiState.Front(cards = queue, cursor = 0, reviewedCount = 0)
            }
        }
    }

    fun showAnswer() {
        _state.update { s ->
            if (s is ReviewUiState.Front) ReviewUiState.Back(s.cards, s.cursor, s.reviewedCount) else s
        }
    }

    fun rate(rating: Rating) {
        val current = _state.value
        if (current !is ReviewUiState.Back) return
        val card = current.cards.getOrNull(current.cursor) ?: return

        viewModelScope.launch {
            repository.rate(card, rating)
            advance(current)
        }
    }

    private fun advance(from: ReviewUiState.Back) {
        val nextCursor = from.cursor + 1
        val reviewed = from.reviewedCount + 1
        _state.value = if (nextCursor >= from.cards.size) {
            ReviewUiState.Done(reviewed)
        } else {
            ReviewUiState.Front(from.cards, nextCursor, reviewed)
        }
    }

    fun speakCurrent() {
        val c = _state.value.currentCard ?: return
        tts.speak(c.lemma)
    }
}
