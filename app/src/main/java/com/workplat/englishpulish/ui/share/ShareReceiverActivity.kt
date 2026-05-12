package com.workplat.englishpulish.ui.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workplat.englishpulish.ui.theme.EnglishPulishTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    private val viewModel: ShareReceiverViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            viewModel.handleSharedText(extractSharedText(intent))
        }
        setContent {
            EnglishPulishTheme {
                ShareReceiverScreen(
                    viewModel = viewModel,
                    onAddSelected = viewModel::addSelected,
                    onFinish = { msg ->
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        finish()
                    },
                )
            }
        }
    }

    private fun extractSharedText(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (intent.type != "text/plain") return null
        return intent.getStringExtra(Intent.EXTRA_TEXT)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareReceiverScreen(
    viewModel: ShareReceiverViewModel,
    onAddSelected: (List<String>) -> Unit,
    onFinish: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state) {
        if (state is ShareUiState.Done) {
            onFinish((state as ShareUiState.Done).message)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onFinish("已取消") },
        sheetState = sheetState,
    ) {
        when (val s = state) {
            ShareUiState.Loading -> LoadingContent()
            is ShareUiState.PickTokens -> PickTokensContent(
                tokens = s.tokens,
                onConfirm = onAddSelected,
            )
            is ShareUiState.Done -> Unit
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickTokensContent(
    tokens: List<String>,
    onConfirm: (List<String>) -> Unit,
) {
    var selected by remember { mutableStateOf(setOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("挑选要加入的单词")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(tokens, key = { it }) { token ->
                FilterChip(
                    selected = token in selected,
                    onClick = {
                        selected = if (token in selected) selected - token else selected + token
                    },
                    label = { Text(token) },
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }
        }
        Button(
            onClick = { onConfirm(selected.toList()) },
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("加入 ${selected.size} 个")
        }
    }
}
