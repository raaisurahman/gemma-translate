package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChunkStatus
import com.example.ui.viewmodel.TranslationViewModel

@Composable
fun DocumentTranslateScreen(
    viewModel: TranslationViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val documentText by viewModel.documentText.collectAsState()
    val documentChunks by viewModel.documentChunks.collectAsState()
    val isChunkTranslating by viewModel.isChunkTranslating.collectAsState()
    val sourceLang by viewModel.sourceLanguage.collectAsState()
    val targetLang by viewModel.targetLanguage.collectAsState()

    val completedChunksCount = remember(documentChunks) {
        documentChunks.count { it.status == ChunkStatus.COMPLETED }
    }
    val totalChunksCount = documentChunks.size

    val progressFraction = if (totalChunksCount > 0) {
        completedChunksCount.toFloat() / totalChunksCount.toFloat()
    } else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Header
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Article,
                        contentDescription = "Document Translator",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Document 'Divide & Conquer' Chunking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Optimized for large documents, contracts, articles, and long texts. Translates chunk-by-chunk using Gemma AI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Document Text Input Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Document Content (${sourceLang.name} → ${targetLang.name})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Quick Sample Document Loader
                    TextButton(onClick = {
                        val sampleDoc = """
                            Google Gemma models are a family of lightweight, state-of-the-art open models built from the same research and technology used to create the Gemini models.
                            
                            TranslateGemma models are specifically tailored for high-quality translation across 55+ global languages. They support multi-model sizes including 4B, 12B, and 27B parameter variants.
                            
                            The divide and conquer chunking strategy enables efficient translation of long-form documents, user manuals, and technical reports without exceeding token window constraints or losing context coherence.
                        """.trimIndent()
                        viewModel.setDocumentText(sampleDoc)
                    }) {
                        Text("Load Sample Doc", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = documentText,
                    onValueChange = { viewModel.setDocumentText(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("document_text_input"),
                    placeholder = { Text("Paste or type document text here...") },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (totalChunksCount > 0) "$totalChunksCount Chunks Prepared" else "Ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { viewModel.translateDocumentChunks() },
                        enabled = totalChunksCount > 0 && !isChunkTranslating,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("translate_chunks_button")
                    ) {
                        if (isChunkTranslating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Translating Chunks...")
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Process All Chunks")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress bar if chunks exist
        if (totalChunksCount > 0) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Chunk Translation Progress",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$completedChunksCount / $totalChunksCount Done",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    if (completedChunksCount == totalChunksCount && totalChunksCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val fullDoc = documentChunks.joinToString("\n\n") { it.translatedText }
                                clipboardManager.setText(AnnotatedString(fullDoc))
                                Toast.makeText(context, "Full document copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Complete Exported Document")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Chunks Output List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(documentChunks, key = { it.id }) { chunk ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (chunk.status) {
                            ChunkStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            ChunkStatus.TRANSLATING -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            ChunkStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                            ChunkStatus.PENDING -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chunk ${chunk.chunkIndex} of ${chunk.totalChunks}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = when (chunk.status) {
                                            ChunkStatus.PENDING -> "Pending"
                                            ChunkStatus.TRANSLATING -> "Translating..."
                                            ChunkStatus.COMPLETED -> "Completed ✓"
                                            ChunkStatus.ERROR -> "Error"
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    if (chunk.status == ChunkStatus.TRANSLATING) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Original: \"${chunk.originalText}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (chunk.translatedText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = chunk.translatedText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
