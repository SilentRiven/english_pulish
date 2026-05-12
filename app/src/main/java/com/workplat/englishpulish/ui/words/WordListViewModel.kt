package com.workplat.englishpulish.ui.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.workplat.englishpulish.data.db.WordEntity
import com.workplat.englishpulish.data.repo.WordFilter
import com.workplat.englishpulish.data.repo.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class WordListViewModel @Inject constructor(
    private val repository: WordRepository,
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

    companion object {
        private const val QUERY_DEBOUNCE_MS = 300L
    }
}
