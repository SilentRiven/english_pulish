package com.workplat.englishpulish.ui.words

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
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
    SourceChip("manual", "手动加"),
)

private fun shortSourceLabel(source: String): String = when (source) {
    "preload-gaozhong" -> "高"
    "preload-kaoyan" -> "研"
    "preload-both" -> "共"
    "share" -> "分享"
    "manual" -> "手动"
    else -> source
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    viewModel: WordListViewModel = hiltViewModel(),
) {
    val words = viewModel.words.collectAsLazyPagingItems()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val count by viewModel.filteredCount.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("English Pulish") },
                actions = {
                    Text(
                        text = "$count 词",
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "手动加词")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                query = filter.query,
                onQueryChange = viewModel::setQuery,
                onClear = { viewModel.setQuery("") },
            )
            SourceChipRow(filter = filter, onToggle = viewModel::toggleSource)
            Box(modifier = Modifier.fillMaxSize()) {
                val refreshState = words.loadState.refresh
                if (refreshState is LoadState.NotLoading && words.itemCount == 0) {
                    EmptyState(filter = filter, onClear = viewModel::clearFilters)
                } else {
                    WordList(
                        words = words,
                        onSpeak = viewModel::speak,
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddWordSheet(
            onDismiss = { showAdd = false },
            onConfirm = { lemma, def ->
                viewModel.addManual(lemma, def)
                showAdd = false
            },
        )
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
    onSpeak: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            count = words.itemCount,
            key = words.itemKey { it.id },
        ) { index ->
            val word = words[index] ?: return@items
            WordRow(word = word, onSpeak = { onSpeak(word.lemma) })
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordRow(word: WordEntity, onSpeak: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = word.lemma,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (word.phonetic != null) {
                    Text(
                        text = "  /${word.phonetic}/",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = listOfNotNull(word.partOfSpeech, word.definitionZh.ifBlank { null })
                    .joinToString(" ")
                    .ifEmpty { "（待补释义）" },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        AssistChip(
            onClick = {},
            enabled = false,
            label = { Text(shortSourceLabel(word.source)) },
            colors = AssistChipDefaults.assistChipColors(),
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        IconButton(onClick = onSpeak, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.PlayArrow, contentDescription = "朗读")
        }
    }
}

@Composable
private fun EmptyState(filter: WordFilter, onClear: () -> Unit) {
    val msg = when {
        filter.query.isNotBlank() -> "没有匹配「${filter.query}」的单词"
        filter.sources.isNotEmpty() -> "这个分类下没有单词"
        else -> "词库还没有单词"
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = msg, style = MaterialTheme.typography.bodyLarge)
        if (filter.query.isNotBlank() || filter.sources.isNotEmpty()) {
            TextButton(onClick = onClear, modifier = Modifier.padding(top = 8.dp)) {
                Text("清除筛选")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWordSheet(
    onDismiss: () -> Unit,
    onConfirm: (lemma: String, definition: String) -> Unit,
) {
    var lemma by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("手动加词", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = lemma,
                onValueChange = { lemma = it },
                label = { Text("单词") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = definition,
                onValueChange = { definition = it },
                label = { Text("释义（可选）") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onConfirm(lemma, definition) },
                enabled = lemma.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存")
            }
        }
    }
}
