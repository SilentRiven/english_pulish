package com.workplat.englishpulish.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workplat.englishpulish.domain.fsrs.Rating
import com.workplat.englishpulish.domain.model.ReviewCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onBack: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("今日复习") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                ReviewUiState.Loading -> LoadingContent()
                is ReviewUiState.Front -> CardContent(
                    card = s.cards[s.cursor],
                    cursor = s.cursor,
                    total = s.cards.size,
                    showBack = false,
                    onSpeak = viewModel::speakCurrent,
                    onShowAnswer = viewModel::showAnswer,
                    onRate = {},
                )
                is ReviewUiState.Back -> CardContent(
                    card = s.cards[s.cursor],
                    cursor = s.cursor,
                    total = s.cards.size,
                    showBack = true,
                    onSpeak = viewModel::speakCurrent,
                    onShowAnswer = {},
                    onRate = viewModel::rate,
                )
                is ReviewUiState.Done -> DoneContent(reviewed = s.reviewedCount, onBack = onBack)
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CardContent(
    card: ReviewCard,
    cursor: Int,
    total: Int,
    showBack: Boolean,
    onSpeak: () -> Unit,
    onShowAnswer: () -> Unit,
    onRate: (Rating) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { (cursor + 1f) / total },
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "${cursor + 1} / $total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(48.dp))

            Text(
                text = card.lemma,
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (card.phonetic != null) {
                Text(
                    text = "/${card.phonetic}/",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            IconButton(onClick = onSpeak, modifier = Modifier.padding(top = 12.dp).size(48.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = "朗读")
            }

            if (showBack) {
                Spacer(Modifier.height(32.dp))
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))
                Text(
                    text = listOfNotNull(card.partOfSpeech, card.definitionZh.ifBlank { null })
                        .joinToString(" ")
                        .ifEmpty { "（无释义）" },
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                if (card.exampleEn != null) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = card.exampleEn,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    if (card.exampleZh != null) {
                        Text(
                            text = card.exampleZh,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }

        if (showBack) {
            RatingButtons(onRate = onRate)
        } else {
            Button(
                onClick = onShowAnswer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Text("显示答案")
            }
        }
    }
}

@Composable
private fun RatingButtons(onRate: (Rating) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RatingButton(label = "记不起", rating = Rating.Again, onClick = onRate, color = Color(0xFFC62828))
        RatingButton(label = "困难", rating = Rating.Hard, onClick = onRate, color = Color(0xFFE65100))
        RatingButton(label = "记得", rating = Rating.Good, onClick = onRate, color = Color(0xFF2E7D32))
        RatingButton(label = "简单", rating = Rating.Easy, onClick = onRate, color = Color(0xFF1565C0))
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.RatingButton(
    label: String,
    rating: Rating,
    onClick: (Rating) -> Unit,
    color: Color,
) {
    OutlinedButton(
        onClick = { onClick(rating) },
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) {
        Text(label)
    }
}

@Composable
private fun DoneContent(reviewed: Int, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (reviewed == 0) "今日没有要复习的词" else "🎉 完成今日 $reviewed 词",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) {
            Text("返回词库")
        }
    }
}
