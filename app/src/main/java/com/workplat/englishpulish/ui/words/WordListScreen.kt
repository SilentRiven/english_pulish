package com.workplat.englishpulish.ui.words

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workplat.englishpulish.data.db.WordEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    viewModel: WordListViewModel = hiltViewModel(),
) {
    val words by viewModel.words.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("English Pulish") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(words, key = { it.id }) { word ->
                WordRow(word)
                HorizontalDivider()
            }
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
