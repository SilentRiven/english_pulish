package com.workplat.englishpulish.ui.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.workplat.englishpulish.data.db.WordEntity
import com.workplat.englishpulish.data.repo.AddResult
import com.workplat.englishpulish.data.repo.ReviewRepository
import com.workplat.englishpulish.data.repo.WordFilter
import com.workplat.englishpulish.data.repo.WordRepository
import com.workplat.englishpulish.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class WordListViewModel @Inject constructor(
    private val repository: WordRepository,
    private val reviewRepository: ReviewRepository,
    private val tts: TtsManager,
) : ViewModel() {

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
    }

    private val _filter = MutableStateFlow(WordFilter())
    val filter: StateFlow<WordFilter> = _filter.asStateFlow()

    val words: Flow<PagingData<WordEntity>> = _filter
        .debounce { f -> if (f.query.isEmpty()) 0L else QUERY_DEBOUNCE_MS }
        .distinctUntilChanged()
        .flatMapLatest { repository.pager(it) }
        .cachedIn(viewModelScope)

    val filteredCount: StateFlow<Int> = _filter
        .debounce { f -> if (f.query.isEmpty()) 0L else QUERY_DEBOUNCE_MS }
        .distinctUntilChanged()
        .flatMapLatest { repository.observeFilteredCount(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val todayDueCount: StateFlow<Int> = reviewRepository.observeTodayDueCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _events = Channel<String>(Channel.BUFFERED)
    val events: Flow<String> = _events.receiveAsFlow()

    fun setQuery(value: String) {
        _filter.value = _filter.value.copy(query = value)
    }

    fun toggleSource(source: String) {
        val current = _filter.value.sources
        _filter.value = _filter.value.copy(
            sources = if (source in current) current - source else current + source
        )
    }

    fun clearFilters() {
        _filter.value = WordFilter()
    }

    fun speak(lemma: String) {
        tts.speak(lemma)
    }

    fun addManual(lemma: String, definition: String) {
        viewModelScope.launch {
            val msg = when (val r = repository.addOne(lemma, definition = definition, source = "manual")) {
                is AddResult.Added -> "已加入「${r.lemma}」"
                is AddResult.Duplicate -> "「${r.lemma}」已在词库"
                AddResult.Invalid -> "无效输入"
            }
            _events.send(msg)
        }
    }

    companion object {
        private const val QUERY_DEBOUNCE_MS = 300L
    }
}
