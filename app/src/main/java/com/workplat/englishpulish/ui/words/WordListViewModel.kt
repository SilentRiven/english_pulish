package com.workplat.englishpulish.ui.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workplat.englishpulish.data.db.WordEntity
import com.workplat.englishpulish.data.repo.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WordListViewModel @Inject constructor(
    private val repository: WordRepository,
) : ViewModel() {

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
    }

    val words: StateFlow<List<WordEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
