package com.workplat.englishpulish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.workplat.englishpulish.ui.review.ReviewScreen
import com.workplat.englishpulish.ui.theme.EnglishPulishTheme
import com.workplat.englishpulish.ui.words.WordListScreen
import dagger.hilt.android.AndroidEntryPoint

private object Routes {
    const val WORD_LIST = "word_list"
    const val REVIEW = "review"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EnglishPulishTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = Routes.WORD_LIST) {
                    composable(Routes.WORD_LIST) {
                        WordListScreen(
                            onReviewClick = { navController.navigate(Routes.REVIEW) },
                        )
                    }
                    composable(Routes.REVIEW) {
                        ReviewScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
