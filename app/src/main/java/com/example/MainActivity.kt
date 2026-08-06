package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.PhrasebookScreen
import com.example.ui.screens.TwoBoxVoiceTranslatorScreen
import com.example.ui.theme.GemmaTranslatorTheme
import com.example.ui.viewmodel.TranslationViewModel

enum class AppNavDestination(val route: String, val title: String, val icon: ImageVector) {
    VOICE("voice", "Offline Translator", Icons.Default.RecordVoiceOver),
    PHRASEBOOK("phrasebook", "Saved Phrases", Icons.Default.Bookmarks)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GemmaTranslatorTheme {
                OfflineVoiceTranslatorApp()
            }
        }
    }
}

@Composable
fun OfflineVoiceTranslatorApp() {
    val viewModel: TranslationViewModel = viewModel()

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TwoBoxVoiceTranslatorScreen(viewModel = viewModel)
        }
    }
}
