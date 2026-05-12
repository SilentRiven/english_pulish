package com.workplat.englishpulish.ui.words

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.workplat.englishpulish.data.db.WordEntity
import com.workplat.englishpulish.data.repo.WordFilter

private data class SourceChip(val source: String, val label: String)

private val SOURCE_CHIPS = listOf(
    SourceChip("preload-gaozhong", "高中"),
    SourceChip("preload-kaoyan", "考研"),
    SourceChip("preload-both", "共有"),
    SourceChip("share", "我加的"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    viewModel: WordListViewModel = hiltViewModel(),
) {
    val words = viewModel.words.collectAsLazyPagingItems()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("English Pulish") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                query = filter.query,
                onQueryChange = viewModel::setQuery,
                onClear = { viewModel.setQuery("") },
            )
            SourceChipRow(
                filter = filter,
                onToggle = viewModel::toggleSource,
            )
            WordList(words = words, modifier = Modifier.fillMaxSize())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("搜索单词") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "清除")
                }
            }
        },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = ImeAction.Search,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceChipRow(
    filter: WordFilter,
    onToggle: (String) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        items(
            count = SOURCE_CHIPS.size,
            key = { SOURCE_CHIPS[it].source },
        ) { index ->
            val chip = SOURCE_CHIPS[index]
            FilterChip(
                selected = chip.source in filter.sources,
                onClick = { onToggle(chip.source) },
                label = { Text(chip.label) },
            )
        }
    }
}

@Composable
private fun WordList(
    words: LazyPagingItems<WordEntity>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(
            count = words.itemCount,
            key = words.itemKey { it.id },
        ) { index ->
            val word = words[index] ?: return@items
            WordRow(word)
            HorizontalDivider()
        }
    }
}

@Composable
private fun WordRow(word: WordEntity) {
    Column(Modifier.padding(16.dp)) {
        Text(text = word.lemma)
        Text(
            text = listOfNotNull(word.partOfSpeech, word.definitionZh).joinToString(" "),
        )
    }
}
