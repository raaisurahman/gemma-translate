package com.example.data.ondevice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class ModelDownloadStatus {
    object NotDownloaded : ModelDownloadStatus()
    data class Downloading(
        val progress: Float, // 0.0 to 1.0
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val speedMbSec: Float
    ) : ModelDownloadStatus()
    object Ready : ModelDownloadStatus()
    data class Error(val message: String) : ModelDownloadStatus()
}

class GemmaModelManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var downloadJob: Job? = null

    val modelFileName = "gemma-2b-it-cpu-int4.bin"
    val totalModelSizeBytes = 1_450_000_000L // ~1.35 GB

    private val _downloadStatus = MutableStateFlow<ModelDownloadStatus>(ModelDownloadStatus.NotDownloaded)
    val downloadStatus: StateFlow<ModelDownloadStatus> = _downloadStatus.asStateFlow()

    private val _isOnDeviceActive = MutableStateFlow(false)
    val isOnDeviceActive: StateFlow<Boolean> = _isOnDeviceActive.asStateFlow()

    init {
        checkLocalModelFile()
    }

    fun getModelFile(): File {
        val dir = File(context.filesDir, "gemma_models")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, modelFileName)
    }

    fun checkLocalModelFile(): Boolean {
        val file = getModelFile()
        val exists = file.exists() && file.length() > 0
        if (exists) {
            _downloadStatus.value = ModelDownloadStatus.Ready
            _isOnDeviceActive.value = true
        } else {
            if (_downloadStatus.value is ModelDownloadStatus.Ready) {
                _downloadStatus.value = ModelDownloadStatus.NotDownloaded
            }
            _isOnDeviceActive.value = false
        }
        return exists
    }

    fun startDownload() {
        if (_downloadStatus.value is ModelDownloadStatus.Downloading || _downloadStatus.value is ModelDownloadStatus.Ready) {
            return
        }

        downloadJob?.cancel()
        downloadJob = scope.launch {
            try {
                val file = getModelFile()
                if (file.exists()) file.delete()

                val total = totalModelSizeBytes
                var downloaded = 0L
                val chunkSize = 25_000_000L // 25MB chunks per step for smooth UI updates

                while (downloaded < total) {
                    delay(300) // Realistic chunk interval
                    downloaded += chunkSize
                    if (downloaded > total) downloaded = total

                    val progress = downloaded.toFloat() / total.toFloat()
                    val speed = 15.5f + (Math.random().toFloat() * 4f) // ~16 MB/s download speed simulation

                    _downloadStatus.value = ModelDownloadStatus.Downloading(
                        progress = progress,
                        bytesDownloaded = downloaded,
                        totalBytes = total,
                        speedMbSec = speed
                    )
                }

                // Write dummy marker header to signify model file ready on device storage
                file.writeText("Gemma-2B-IT-INT4-Local-Weights-Verified")

                _downloadStatus.value = ModelDownloadStatus.Ready
                _isOnDeviceActive.value = true
            } catch (e: Exception) {
                Log.e("GemmaModelManager", "Error downloading model", e)
                _downloadStatus.value = ModelDownloadStatus.Error(e.message ?: "Download failed")
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        val file = getModelFile()
        if (file.exists()) file.delete()
        _downloadStatus.value = ModelDownloadStatus.NotDownloaded
        _isOnDeviceActive.value = false
    }

    fun deleteModel() {
        downloadJob?.cancel()
        downloadJob = null
        val file = getModelFile()
        if (file.exists()) file.delete()
        _downloadStatus.value = ModelDownloadStatus.NotDownloaded
        _isOnDeviceActive.value = false
    }

    suspend fun generateOnDeviceTranslation(
        prompt: String,
        sourceLang: String,
        targetLang: String,
        contextHistory: List<com.example.data.model.ConversationTurn> = emptyList()
    ): String {
        // Simulate On-Device LLM Inference processing latency (250ms)
        delay(250)
        val contextInfo = if (contextHistory.isNotEmpty()) {
            " (Context: ${contextHistory.size} turns)"
        } else ""
        return "Gemma 2B On-Device [$targetLang]$contextInfo: $prompt"
    }
}
