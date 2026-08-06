package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.SavedTranslationEntity
import com.example.data.model.*
import com.example.data.ondevice.GemmaModelManager
import com.example.data.ondevice.ModelDownloadStatus
import com.example.data.repository.TranslationRepository
import com.example.util.SpeechRecognizerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TranslationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TranslationRepository(application)
    private val speechHelper = SpeechRecognizerHelper(application)
    val gemmaModelManager = GemmaModelManager(application)

    val modelDownloadStatus: StateFlow<ModelDownloadStatus> = gemmaModelManager.downloadStatus
    val isOnDeviceGemmaActive: StateFlow<Boolean> = gemmaModelManager.isOnDeviceActive

    fun startGemmaModelDownload() {
        gemmaModelManager.startDownload()
    }

    fun cancelGemmaModelDownload() {
        gemmaModelManager.cancelDownload()
    }

    fun deleteGemmaModel() {
        gemmaModelManager.deleteModel()
    }

    // Two-Box Offline Voice Translator States
    private val _topSpeakerLang = MutableStateFlow(Language.findByCode("en"))
    val topSpeakerLang: StateFlow<Language> = _topSpeakerLang.asStateFlow()

    private val _bottomSpeakerLang = MutableStateFlow(Language.findByCode("es"))
    val bottomSpeakerLang: StateFlow<Language> = _bottomSpeakerLang.asStateFlow()

    private val _topSpeakerInput = MutableStateFlow("")
    val topSpeakerInput: StateFlow<String> = _topSpeakerInput.asStateFlow()

    private val _topSpeakerTranslation = MutableStateFlow("")
    val topSpeakerTranslation: StateFlow<String> = _topSpeakerTranslation.asStateFlow()

    private val _bottomSpeakerInput = MutableStateFlow("")
    val bottomSpeakerInput: StateFlow<String> = _bottomSpeakerInput.asStateFlow()

    private val _bottomSpeakerTranslation = MutableStateFlow("")
    val bottomSpeakerTranslation: StateFlow<String> = _bottomSpeakerTranslation.asStateFlow()

    private val _isTopListening = MutableStateFlow(false)
    val isTopListening: StateFlow<Boolean> = _isTopListening.asStateFlow()

    private val _isBottomListening = MutableStateFlow(false)
    val isBottomListening: StateFlow<Boolean> = _isBottomListening.asStateFlow()

    private val _topRms = MutableStateFlow(0f)
    val topRms: StateFlow<Float> = _topRms.asStateFlow()

    private val _bottomRms = MutableStateFlow(0f)
    val bottomRms: StateFlow<Float> = _bottomRms.asStateFlow()

    private val _autoSpeakEnabled = MutableStateFlow(true)
    val autoSpeakEnabled: StateFlow<Boolean> = _autoSpeakEnabled.asStateFlow()

    private val _conversationTurns = MutableStateFlow<List<ConversationTurn>>(emptyList())
    val conversationTurns: StateFlow<List<ConversationTurn>> = _conversationTurns.asStateFlow()

    private val _sourceLanguage = MutableStateFlow(Language.findByCode("en"))
    val sourceLanguage: StateFlow<Language> = _sourceLanguage.asStateFlow()

    private val _targetLanguage = MutableStateFlow(Language.findByCode("es"))
    val targetLanguage: StateFlow<Language> = _targetLanguage.asStateFlow()

    private val _selectedTone = MutableStateFlow(TranslationTone.STANDARD)
    val selectedTone: StateFlow<TranslationTone> = _selectedTone.asStateFlow()

    private val _selectedModel = MutableStateFlow(TranslationModel.GEMMA_3_12B)
    val selectedModel: StateFlow<TranslationModel> = _selectedModel.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _translationResult = MutableStateFlow<TranslationResult?>(null)
    val translationResult: StateFlow<TranslationResult?> = _translationResult.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setTopLanguage(lang: Language) {
        _topSpeakerLang.value = lang
    }

    fun setBottomLanguage(lang: Language) {
        _bottomSpeakerLang.value = lang
    }

    fun toggleAutoSpeak() {
        _autoSpeakEnabled.value = !_autoSpeakEnabled.value
    }

    fun swapSpeakerLanguages() {
        val tempLang = _topSpeakerLang.value
        _topSpeakerLang.value = _bottomSpeakerLang.value
        _bottomSpeakerLang.value = tempLang

        val tempInput = _topSpeakerInput.value
        val tempTrans = _topSpeakerTranslation.value
        _topSpeakerInput.value = _bottomSpeakerInput.value
        _topSpeakerTranslation.value = _bottomSpeakerTranslation.value
        _bottomSpeakerInput.value = tempInput
        _bottomSpeakerTranslation.value = tempTrans
    }

    fun startListeningTop() {
        _isTopListening.value = true
        _errorMessage.value = null
        _topSpeakerInput.value = ""
        _topSpeakerTranslation.value = ""
        speechHelper.startListening(
            languageCode = _topSpeakerLang.value.code,
            listener = object : SpeechRecognizerHelper.SpeechListener {
                override fun onSpeechStarted() {
                    _isTopListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    _topRms.value = rmsdB
                }

                override fun onPartialResults(partialText: String) {
                    if (partialText.isNotBlank()) {
                        _topSpeakerInput.value = partialText
                    }
                }

                override fun onResults(resultText: String) {
                    _isTopListening.value = false
                    _topRms.value = 0f
                    val finalText = if (resultText.isNotBlank()) resultText else _topSpeakerInput.value
                    if (finalText.isNotBlank()) {
                        _topSpeakerInput.value = finalText
                        translateAndSpeak(SpeakerRole.TOURIST, finalText)
                    }
                }

                override fun onError(errorMsg: String) {
                    _isTopListening.value = false
                    _topRms.value = 0f
                    val capturedText = _topSpeakerInput.value
                    if (capturedText.isNotBlank()) {
                        translateAndSpeak(SpeakerRole.TOURIST, capturedText)
                    } else if (errorMsg.isNotBlank()) {
                        _errorMessage.value = errorMsg
                    }
                }
            }
        )
    }

    fun stopListeningTop() {
        if (_isTopListening.value) {
            speechHelper.stopListening()
            viewModelScope.launch {
                kotlinx.coroutines.delay(400)
                if (_isTopListening.value) {
                    _isTopListening.value = false
                    _topRms.value = 0f
                    val text = _topSpeakerInput.value
                    if (text.isNotBlank() && _topSpeakerTranslation.value.isBlank()) {
                        translateAndSpeak(SpeakerRole.TOURIST, text)
                    }
                }
            }
        }
    }

    fun startListeningBottom() {
        _isBottomListening.value = true
        _errorMessage.value = null
        _bottomSpeakerInput.value = ""
        _bottomSpeakerTranslation.value = ""
        speechHelper.startListening(
            languageCode = _bottomSpeakerLang.value.code,
            listener = object : SpeechRecognizerHelper.SpeechListener {
                override fun onSpeechStarted() {
                    _isBottomListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    _bottomRms.value = rmsdB
                }

                override fun onPartialResults(partialText: String) {
                    if (partialText.isNotBlank()) {
                        _bottomSpeakerInput.value = partialText
                    }
                }

                override fun onResults(resultText: String) {
                    _isBottomListening.value = false
                    _bottomRms.value = 0f
                    val finalText = if (resultText.isNotBlank()) resultText else _bottomSpeakerInput.value
                    if (finalText.isNotBlank()) {
                        _bottomSpeakerInput.value = finalText
                        translateAndSpeak(SpeakerRole.LOCAL, finalText)
                    }
                }

                override fun onError(errorMsg: String) {
                    _isBottomListening.value = false
                    _bottomRms.value = 0f
                    val capturedText = _bottomSpeakerInput.value
                    if (capturedText.isNotBlank()) {
                        translateAndSpeak(SpeakerRole.LOCAL, capturedText)
                    } else if (errorMsg.isNotBlank()) {
                        _errorMessage.value = errorMsg
                    }
                }
            }
        )
    }

    fun stopListeningBottom() {
        if (_isBottomListening.value) {
            speechHelper.stopListening()
            viewModelScope.launch {
                kotlinx.coroutines.delay(400)
                if (_isBottomListening.value) {
                    _isBottomListening.value = false
                    _bottomRms.value = 0f
                    val text = _bottomSpeakerInput.value
                    if (text.isNotBlank() && _bottomSpeakerTranslation.value.isBlank()) {
                        translateAndSpeak(SpeakerRole.LOCAL, text)
                    }
                }
            }
        }
    }

    fun processDirectText(speakerRole: SpeakerRole, text: String) {
        if (speakerRole == SpeakerRole.TOURIST) {
            _topSpeakerInput.value = text
        } else {
            _bottomSpeakerInput.value = text
        }
        translateAndSpeak(speakerRole, text)
    }

    fun translateAndSpeak(speakerRole: SpeakerRole, text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val sourceLang = if (speakerRole == SpeakerRole.TOURIST) _topSpeakerLang.value else _bottomSpeakerLang.value
            val targetLang = if (speakerRole == SpeakerRole.TOURIST) _bottomSpeakerLang.value else _topSpeakerLang.value

            // Moderate context intensity: Pass up to 5 recent turns in chronological order
            val recentTurns = _conversationTurns.value.take(5).reversed()

            val result = repository.translateText(
                text = text,
                sourceLang = sourceLang,
                targetLang = targetLang,
                tone = TranslationTone.STANDARD,
                model = if (gemmaModelManager.isOnDeviceActive.value) TranslationModel.GEMMA_3_12B else _selectedModel.value,
                contextHistory = recentTurns
            )

            result.onSuccess { res ->
                val translated = res.translatedText
                if (speakerRole == SpeakerRole.TOURIST) {
                    _topSpeakerTranslation.value = translated
                } else {
                    _bottomSpeakerTranslation.value = translated
                }

                val turn = ConversationTurn(
                    speakerRole = speakerRole,
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    sourceText = text,
                    translatedText = translated
                )
                _conversationTurns.value = listOf(turn) + _conversationTurns.value

                // Save to Room offline database
                repository.saveTranslationToHistory(
                    sourceLangCode = sourceLang.code,
                    targetLangCode = targetLang.code,
                    sourceText = text,
                    translatedText = translated,
                    tone = "STANDARD",
                    modelId = "translategemma:12b"
                )

                // Auto speak translated result out loud in target language
                if (_autoSpeakEnabled.value && translated.isNotBlank()) {
                    repository.speak(translated, targetLang.code)
                }
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Translation error"
            }
        }
    }

    fun speakText(text: String, langCode: String) {
        if (text.isNotBlank()) {
            repository.speak(text, langCode)
        }
    }

    fun clearConversation() {
        _topSpeakerInput.value = ""
        _topSpeakerTranslation.value = ""
        _bottomSpeakerInput.value = ""
        _bottomSpeakerTranslation.value = ""
        _conversationTurns.value = emptyList()
        _errorMessage.value = null
    }

    // Comparison Mode
    private val _comparisonResults = MutableStateFlow<Map<TranslationModel, String>>(emptyMap())
    val comparisonResults: StateFlow<Map<TranslationModel, String>> = _comparisonResults.asStateFlow()
    private val _isComparing = MutableStateFlow(false)
    val isComparing: StateFlow<Boolean> = _isComparing.asStateFlow()

    // Document Chunking Mode
    private val _documentText = MutableStateFlow("")
    val documentText: StateFlow<String> = _documentText.asStateFlow()
    private val _documentChunks = MutableStateFlow<List<DocumentChunk>>(emptyList())
    val documentChunks: StateFlow<List<DocumentChunk>> = _documentChunks.asStateFlow()
    private val _isChunkTranslating = MutableStateFlow(false)
    val isChunkTranslating: StateFlow<Boolean> = _isChunkTranslating.asStateFlow()

    // Phrasebook / Saved History
    val savedTranslations: StateFlow<List<SavedTranslationEntity>> = repository.savedTranslations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTranslations: StateFlow<List<SavedTranslationEntity>> = repository.favoriteTranslations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSourceLanguage(lang: Language) {
        _sourceLanguage.value = lang
    }

    fun setTargetLanguage(lang: Language) {
        _targetLanguage.value = lang
    }

    fun swapLanguages() {
        if (_sourceLanguage.value.code == "auto") return
        val temp = _sourceLanguage.value
        _sourceLanguage.value = _targetLanguage.value
        _targetLanguage.value = temp

        val currentResult = _translationResult.value
        if (currentResult != null && currentResult.translatedText.isNotBlank()) {
            _inputText.value = currentResult.translatedText
            _translationResult.value = null
        }
    }

    fun setTone(tone: TranslationTone) {
        _selectedTone.value = tone
        if (_inputText.value.isNotBlank() && _translationResult.value != null) {
            translateText()
        }
    }

    fun setModel(model: TranslationModel) {
        _selectedModel.value = model
    }

    fun setInputText(text: String) {
        _inputText.value = text
        if (text.isBlank()) {
            _translationResult.value = null
            _errorMessage.value = null
        }
    }

    fun translateText(imageInlineBase64: String? = null) {
        val text = _inputText.value
        if (text.isBlank() && imageInlineBase64 == null) return

        viewModelScope.launch {
            _isTranslating.value = true
            _errorMessage.value = null

            val result = repository.translateText(
                text = text,
                sourceLang = _sourceLanguage.value,
                targetLang = _targetLanguage.value,
                tone = _selectedTone.value,
                model = _selectedModel.value,
                imageInlineDataBase64 = imageInlineBase64
            )

            _isTranslating.value = false
            result.onSuccess { res ->
                _translationResult.value = res
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Translation failed"
            }
        }
    }

    fun runComparison() {
        val text = _inputText.value
        if (text.isBlank()) return

        viewModelScope.launch {
            _isComparing.value = true
            val models = listOf(
                TranslationModel.GEMMA_3_4B,
                TranslationModel.GEMMA_3_12B,
                TranslationModel.GEMMA_3_27B,
                TranslationModel.GEMINI_FLASH
            )

            val resultsMap = mutableMapOf<TranslationModel, String>()

            for (m in models) {
                val res = repository.translateText(
                    text = text,
                    sourceLang = _sourceLanguage.value,
                    targetLang = _targetLanguage.value,
                    tone = _selectedTone.value,
                    model = m
                )
                res.onSuccess { r ->
                    resultsMap[m] = r.translatedText
                }.onFailure {
                    resultsMap[m] = "Translation error on ${m.title}"
                }
            }

            _comparisonResults.value = resultsMap
            _isComparing.value = false
        }
    }

    fun setDocumentText(text: String) {
        _documentText.value = text
        _documentChunks.value = repository.splitIntoDocumentChunks(text)
    }

    fun translateDocumentChunks() {
        val chunks = _documentChunks.value
        if (chunks.isEmpty()) return

        viewModelScope.launch {
            _isChunkTranslating.value = true
            val updatedChunks = chunks.toMutableList()

            for (i in updatedChunks.indices) {
                val chunk = updatedChunks[i]
                updatedChunks[i] = chunk.copy(status = ChunkStatus.TRANSLATING)
                _documentChunks.value = updatedChunks.toList()

                val res = repository.translateText(
                    text = chunk.originalText,
                    sourceLang = _sourceLanguage.value,
                    targetLang = _targetLanguage.value,
                    tone = _selectedTone.value,
                    model = _selectedModel.value
                )

                res.onSuccess { translation ->
                    updatedChunks[i] = chunk.copy(
                        translatedText = translation.translatedText,
                        status = ChunkStatus.COMPLETED
                    )
                }.onFailure { err ->
                    updatedChunks[i] = chunk.copy(
                        status = ChunkStatus.ERROR,
                        errorMessage = err.message
                    )
                }
                _documentChunks.value = updatedChunks.toList()
            }
            _isChunkTranslating.value = false
        }
    }

    fun speakSource() {
        val text = _inputText.value
        if (text.isNotBlank()) {
            repository.speak(text, _sourceLanguage.value.code)
        }
    }

    fun speakTarget() {
        val res = _translationResult.value
        if (res != null && res.translatedText.isNotBlank()) {
            repository.speak(res.translatedText, _targetLanguage.value.code)
        }
    }

    fun saveCurrentToPhrasebook() {
        val res = _translationResult.value ?: return
        val source = _inputText.value
        if (source.isBlank() || res.translatedText.isBlank()) return

        viewModelScope.launch {
            repository.saveTranslationToHistory(
                sourceLangCode = res.sourceLangCode,
                targetLangCode = res.targetLangCode,
                sourceText = source,
                translatedText = res.translatedText,
                tone = res.tone.name,
                modelId = res.modelUsed.id,
                grammarNotes = res.grammarNotes
            )
        }
    }

    fun toggleFavorite(id: Int, currentFavStatus: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(id, !currentFavStatus)
        }
    }

    fun deleteTranslation(id: Int) {
        viewModelScope.launch {
            repository.deleteSavedTranslation(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearTranslationHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopSpeaking()
    }
}
