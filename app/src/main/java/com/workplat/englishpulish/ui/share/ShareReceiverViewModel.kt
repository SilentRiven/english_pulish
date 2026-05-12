package com.workplat.englishpulish.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workplat.englishpulish.data.repo.AddResult
import com.workplat.englishpulish.data.repo.WordRepository
import com.workplat.englishpulish.domain.text.ParseResult
import com.workplat.englishpulish.domain.text.TextParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ShareUiState {
    data object Loading : ShareUiState
    data class PickTokens(val tokens: List<String>) : ShareUiState
    data class Done(val message: String) : ShareUiState
}

@HiltViewModel
class ShareReceiverViewModel @Inject constructor(
    private val repository: WordRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ShareUiState>(ShareUiState.Loading)
    val state: StateFlow<ShareUiState> = _state.asStateFlow()

    fun handleSharedText(raw: String?) {
        if (raw.isNullOrBlank()) {
            _state.value = ShareUiState.Done("没有可识别的文本")
            return
        }
        when (val parsed = TextParser.parse(raw)) {
            ParseResult.Empty -> {
                _state.value = ShareUiState.Done("没有英文单词可添加")
            }
            is ParseResult.Word -> addOne(parsed.lemma)
            is ParseResult.Sentence -> {
                _state.value = ShareUiState.PickTokens(parsed.tokens)
            }
        }
    }

    fun addSelected(lemmas: List<String>) {
        if (lemmas.isEmpty()) {
            _state.value = ShareUiState.Done("未选择单词")
            return
        }
        viewModelScope.launch {
            var added = 0
            var duplicate = 0
            lemmas.forEach { lemma ->
                when (repository.addFromShare(lemma)) {
                    is AddResult.Added -> added++
                    is AddResult.Duplicate -> duplicate++
                    AddResult.Invalid -> Unit
                }
            }
            val msg = buildString {
                if (added > 0) append("已加入 $added 个")
                if (duplicate > 0) {
                    if (isNotEmpty()) append("，")
                    append("跳过重复 $duplicate 个")
                }
                if (isEmpty()) append("未加入")
            }
            _state.value = ShareUiState.Done(msg)
        }
    }

    private fun addOne(lemma: String) {
        viewModelScope.launch {
            val msg = when (val result = repository.addFromShare(lemma)) {
                is AddResult.Added -> "已加入「${result.lemma}」"
                is AddResult.Duplicate -> "「${result.lemma}」已在词库"
                AddResult.Invalid -> "无效输入"
            }
            _state.value = ShareUiState.Done(msg)
        }
    }
}
