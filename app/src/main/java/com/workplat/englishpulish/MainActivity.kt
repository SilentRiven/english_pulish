package com.workplat.englishpulish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.workplat.englishpulish.ui.theme.EnglishPulishTheme
import com.workplat.englishpulish.ui.words.WordListScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EnglishPulishTheme {
                WordListScreen()
            }
        }
    }
}
