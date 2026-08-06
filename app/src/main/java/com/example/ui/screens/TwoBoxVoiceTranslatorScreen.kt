package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Language
import com.example.data.model.SpeakerRole
import com.example.ui.components.GemmaModelDownloadCard
import com.example.ui.components.LanguageSelectionSheet
import com.example.ui.viewmodel.TranslationViewModel
import com.example.data.ondevice.ModelDownloadStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoBoxVoiceTranslatorScreen(
    viewModel: TranslationViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val topLang by viewModel.topSpeakerLang.collectAsState()
    val bottomLang by viewModel.bottomSpeakerLang.collectAsState()

    val topInput by viewModel.topSpeakerInput.collectAsState()
    val topTranslation by viewModel.topSpeakerTranslation.collectAsState()
    val isTopListening by viewModel.isTopListening.collectAsState()
    val topRms by viewModel.topRms.collectAsState()

    val bottomInput by viewModel.bottomSpeakerInput.collectAsState()
    val bottomTranslation by viewModel.bottomSpeakerTranslation.collectAsState()
    val isBottomListening by viewModel.isBottomListening.collectAsState()
    val bottomRms by viewModel.bottomRms.collectAsState()

    val autoSpeak by viewModel.autoSpeakEnabled.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val downloadStatus by viewModel.modelDownloadStatus.collectAsState()
    val isOnDeviceActive by viewModel.isOnDeviceGemmaActive.collectAsState()
    val conversationTurns by viewModel.conversationTurns.collectAsState()

    var showTopLangSheet by remember { mutableStateOf(false) }
    var showBottomLangSheet by remember { mutableStateOf(false) }
    var showModelManagerSheet by remember { mutableStateOf(false) }

    var isColorSwapped by remember { mutableStateOf(false) }

    val topContainerColor by animateColorAsState(
        targetValue = if (isColorSwapped) Color(0xFF2E7D32) else Color(0xFFD32F2F),
        animationSpec = tween(durationMillis = 350),
        label = "TopBoxColor"
    )
    val bottomContainerColor by animateColorAsState(
        targetValue = if (isColorSwapped) Color(0xFFD32F2F) else Color(0xFF2E7D32),
        animationSpec = tween(durationMillis = 350),
        label = "BottomBoxColor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        // Top Status Header: Gemma Model Download & Status Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = { showModelManagerSheet = true },
                    color = if (isOnDeviceActive) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("gemma_model_status_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isOnDeviceActive) Color(0xFF10B981) else MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Text(
                            text = if (isOnDeviceActive) "Gemma 2B On-Device Active" else "Download Gemma AI Model",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnDeviceActive) Color(0xFF047857) else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Model Manager",
                            modifier = Modifier.size(16.dp),
                            tint = if (isOnDeviceActive) Color(0xFF047857) else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                if (conversationTurns.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Context Active",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Context (${conversationTurns.size.coerceAtMost(5)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Auto Read",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = autoSpeak,
                    onCheckedChange = { viewModel.toggleAutoSpeak() },
                    modifier = Modifier
                        .scale(0.8f)
                        .testTag("auto_speak_switch")
                )
                IconButton(
                    onClick = { viewModel.clearConversation() },
                    modifier = Modifier.testTag("clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear All",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Inline Gemma Download prompt card if model is not yet ready or currently downloading
        if (downloadStatus !is ModelDownloadStatus.Ready) {
            Spacer(modifier = Modifier.height(6.dp))
            GemmaModelDownloadCard(
                downloadStatus = downloadStatus,
                onStartDownload = { viewModel.startGemmaModelDownload() },
                onCancelDownload = { viewModel.cancelGemmaModelDownload() },
                onDeleteModel = { viewModel.deleteGemmaModel() },
                modifier = Modifier.testTag("inline_gemma_download_card")
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // BOX 1: TOURIST SPEAKER (TOP BOX - RED / GREEN ANIMATED)
        SpeakerBoxCard(
            modifier = Modifier.weight(1f),
            title = "Tourist Speaker",
            language = topLang,
            inputText = topInput,
            translatedText = topTranslation,
            targetLangCode = bottomLang.code,
            isListening = isTopListening,
            rms = topRms,
            containerColor = topContainerColor,
            contentColor = Color.White,          // Crisp High Contrast White Text
            testTagPrefix = "top_speaker",
            onChangeLanguage = { showTopLangSheet = true },
            onPressStart = { viewModel.startListeningTop() },
            onPressStop = { viewModel.stopListeningTop() },
            onSpeakOutput = { text ->
                viewModel.speakText(text, bottomLang.code)
            },
            onCopyOutput = { text ->
                clipboardManager.setText(AnnotatedString(text))
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        )

        // CENTER CONTROL DIVIDER / SWAP
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Surface(
                onClick = {
                    viewModel.swapSpeakerLanguages()
                    isColorSwapped = !isColorSwapped
                },
                color = Color.Black,
                contentColor = Color.White,
                shape = CircleShape,
                border = BorderStroke(3.dp, Color.White),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(60.dp)
                    .testTag("swap_languages_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Swap Languages and Box Colors",
                        modifier = Modifier.size(34.dp),
                        tint = Color.White
                    )
                }
            }
        }

        // BOX 2: LOCAL SPEAKER (BOTTOM BOX - GREEN / RED ANIMATED)
        SpeakerBoxCard(
            modifier = Modifier.weight(1f),
            title = "Local Speaker",
            language = bottomLang,
            inputText = bottomInput,
            translatedText = bottomTranslation,
            targetLangCode = topLang.code,
            isListening = isBottomListening,
            rms = bottomRms,
            containerColor = bottomContainerColor,
            contentColor = Color.White,          // Crisp High Contrast White Text
            testTagPrefix = "bottom_speaker",
            onChangeLanguage = { showBottomLangSheet = true },
            onPressStart = { viewModel.startListeningBottom() },
            onPressStop = { viewModel.stopListeningBottom() },
            onSpeakOutput = { text ->
                viewModel.speakText(text, topLang.code)
            },
            onCopyOutput = { text ->
                clipboardManager.setText(AnnotatedString(text))
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showTopLangSheet) {
        LanguageSelectionSheet(
            title = "Select Tourist Language",
            selectedLanguage = topLang,
            onLanguageSelected = {
                viewModel.setTopLanguage(it)
                showTopLangSheet = false
            },
            onDismissRequest = { showTopLangSheet = false }
        )
    }

    if (showBottomLangSheet) {
        LanguageSelectionSheet(
            title = "Select Local Language",
            selectedLanguage = bottomLang,
            onLanguageSelected = {
                viewModel.setBottomLanguage(it)
                showBottomLangSheet = false
            },
            onDismissRequest = { showBottomLangSheet = false }
        )
    }

    if (showModelManagerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelManagerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "On-Device Gemma Model Manager",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                GemmaModelDownloadCard(
                    downloadStatus = downloadStatus,
                    onStartDownload = { viewModel.startGemmaModelDownload() },
                    onCancelDownload = { viewModel.cancelGemmaModelDownload() },
                    onDeleteModel = { viewModel.deleteGemmaModel() },
                    modifier = Modifier.testTag("sheet_gemma_download_card")
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SpeakerBoxCard(
    modifier: Modifier = Modifier,
    title: String,
    language: Language,
    inputText: String,
    translatedText: String,
    targetLangCode: String,
    isListening: Boolean,
    rms: Float,
    containerColor: Color,
    contentColor: Color,
    testTagPrefix: String,
    onChangeLanguage: () -> Unit,
    onPressStart: () -> Unit,
    onPressStop: () -> Unit,
    onSpeakOutput: (String) -> Unit,
    onCopyOutput: (String) -> Unit
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_anim"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Header: Role & Language Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.7f)
                )

                Surface(
                    onClick = onChangeLanguage,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.testTag("${testTagPrefix}_lang_picker")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = language.flagEmoji, fontSize = 16.sp)
                        Text(
                            text = language.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Language",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text Displays (Source + Translation)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Source Speech Input
                    Column {
                        Text(
                            text = if (isListening) "🎙️ Listening..." else if (inputText.isNotBlank()) "Speech Input:" else "Press and hold microphone to speak...",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (inputText.isNotBlank()) inputText else "Tap or hold mic to speak...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 2
                        )
                    }

                    // Translated Output Text
                    if (translatedText.isNotBlank()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Translation:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = translatedText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = { onSpeakOutput(translatedText) },
                                    modifier = Modifier.testTag("${testTagPrefix}_speak_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Speak Out Loud",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { onCopyOutput(translatedText) },
                                    modifier = Modifier.testTag("${testTagPrefix}_copy_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Translation",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Press and Hold Microphone Action Row with Repeat Voice Button beside it
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Invisible balance box on left to keep mic centered
                Box(modifier = Modifier.size(48.dp))

                Spacer(modifier = Modifier.width(12.dp))

                // Microphone Button (Enlarged 2x for Outdoor Usability)
                Box(contentAlignment = Alignment.Center) {
                    if (isListening) {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .scale(pulseScale)
                                .background(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                                    shape = CircleShape
                                )
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (isListening) MaterialTheme.colorScheme.error else Color.Black,
                        contentColor = Color.White,
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .size(150.dp)
                            .testTag("${testTagPrefix}_mic_button")
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        onPressStart()
                                        try {
                                            awaitRelease()
                                        } finally {
                                            onPressStop()
                                        }
                                    },
                                    onTap = {
                                        // Single tap fallback toggles speech
                                        if (isListening) onPressStop() else onPressStart()
                                    }
                                )
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.MicNone else Icons.Default.Mic,
                                contentDescription = "Hold to Speak in ${language.name}",
                                modifier = Modifier.size(110.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Repeat Output Voice Button beside the mic
                FilledTonalIconButton(
                    onClick = {
                        if (translatedText.isNotBlank()) {
                            onSpeakOutput(translatedText)
                        } else {
                            Toast.makeText(context, "No translated text to repeat", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("${testTagPrefix}_repeat_voice_button"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Repeat Translated Voice",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = if (isListening) "RELEASE TO TRANSLATE & SPEAK" else "PRESS & HOLD TO SPEAK (${language.name.uppercase()})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}
