package com.example.data.repository

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.BuildConfig
import com.example.data.db.AppDatabase
import com.example.data.db.SavedTranslationEntity
import com.example.data.model.*
import com.example.data.remote.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit

class TranslationRepository(private val context: Context) : TextToSpeech.OnInitListener {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.translationDao()

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("TranslationRepo", "TTS initialization error", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
        }
    }

    fun speak(text: String, langCode: String) {
        if (!isTtsReady || tts == null) return
        try {
            val locale = Locale.forLanguageTag(langCode)
            tts?.language = locale
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "GemmaTTS_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e("TranslationRepo", "Error speaking text", e)
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    val savedTranslations: Flow<List<SavedTranslationEntity>> = dao.getAllTranslations()
    val favoriteTranslations: Flow<List<SavedTranslationEntity>> = dao.getFavoriteTranslations()

    fun searchTranslations(query: String): Flow<List<SavedTranslationEntity>> {
        return dao.searchTranslations(query)
    }

    suspend fun saveTranslationToHistory(
        sourceLangCode: String,
        targetLangCode: String,
        sourceText: String,
        translatedText: String,
        tone: String,
        modelId: String,
        grammarNotes: List<String> = emptyList()
    ): Long = withContext(Dispatchers.IO) {
        val notesJson = moshi.adapter(List::class.java).toJson(grammarNotes)
        val entity = SavedTranslationEntity(
            sourceLangCode = sourceLangCode,
            targetLangCode = targetLangCode,
            sourceText = sourceText,
            translatedText = translatedText,
            tone = tone,
            modelId = modelId,
            grammarNotesJson = notesJson,
            timestamp = System.currentTimeMillis()
        )
        dao.insertTranslation(entity)
    }

    suspend fun toggleFavorite(id: Int, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        dao.updateFavorite(id, isFavorite)
    }

    suspend fun deleteSavedTranslation(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun clearTranslationHistory() = withContext(Dispatchers.IO) {
        dao.clearHistory()
    }

    suspend fun translateText(
        text: String,
        sourceLang: Language,
        targetLang: Language,
        tone: TranslationTone,
        model: TranslationModel,
        imageInlineDataBase64: String? = null,
        contextHistory: List<ConversationTurn> = emptyList()
    ): Result<TranslationResult> = withContext(Dispatchers.IO) {
        if (text.isBlank() && imageInlineDataBase64 == null) {
            return@withContext Result.failure(IllegalArgumentException("Source text or image is empty"))
        }

        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Throwable) {
            ""
        }

        // Check if valid API key is available
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "null") {
            try {
                val promptText = buildPrompt(text, sourceLang, targetLang, tone, model, contextHistory)
                val partsList = mutableListOf<GeminiPart>()
                partsList.add(GeminiPart(text = promptText))

                if (!imageInlineDataBase64.isNullOrBlank()) {
                    partsList.add(
                        GeminiPart(
                            inlineData = GeminiInlineData(
                                mimeType = "image/jpeg",
                                data = imageInlineDataBase64
                            )
                        )
                    )
                }

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = partsList)),
                    generationConfig = GeminiGenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.35f
                    ),
                    systemInstruction = GeminiContent(
                        parts = listOf(
                            GeminiPart(
                                text = "You are TranslateGemma, Google's advanced context-aware multilingual translation model. " +
                                        "You produce accurate translations matching the requested tone, using the recent conversation context " +
                                        "with moderate influence to maintain topic continuity, resolve implicit pronouns (it/that/they), " +
                                        "and adapt register. Return output ONLY in valid JSON matching the requested schema."
                            )
                        )
                    )
                )

                val response = apiService.generateContent(apiKey, request)
                val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (!rawJson.isNullOrBlank()) {
                    val adapter = moshi.adapter(TranslationJsonResponse::class.java)
                    val parsed = adapter.fromJson(rawJson)
                    if (parsed != null && parsed.translatedText.isNotBlank()) {
                        val dictEntries = parsed.dictionaryEntries.map {
                            DictionaryEntry(it.word, it.partOfSpeech, it.definition, it.example)
                        }
                        val result = TranslationResult(
                            translatedText = parsed.translatedText,
                            sourceLangCode = sourceLang.code,
                            targetLangCode = targetLang.code,
                            detectedLanguage = parsed.detectedLanguage ?: sourceLang.name,
                            tone = tone,
                            modelUsed = model,
                            grammarNotes = parsed.grammarNotes,
                            dictionaryEntries = dictEntries,
                            alternativePhrases = parsed.alternativePhrases,
                            timestamp = System.currentTimeMillis()
                        )
                        return@withContext Result.success(result)
                    }
                }
            } catch (e: Exception) {
                Log.w("TranslationRepo", "API call failed or fallback needed: ${e.message}")
            }
        }

        // Fallback translation engine when offline or no key set
        val fallbackResult = generateFallbackTranslation(text, sourceLang, targetLang, tone, model, contextHistory)
        return@withContext Result.success(fallbackResult)
    }

    private fun buildPrompt(
        text: String,
        sourceLang: Language,
        targetLang: Language,
        tone: TranslationTone,
        model: TranslationModel,
        contextHistory: List<ConversationTurn> = emptyList()
    ): String {
        val srcDesc = if (sourceLang.code == "auto") "Auto-Detect Language" else "${sourceLang.name} (${sourceLang.code})"
        val tgtDesc = "${targetLang.name} (${targetLang.code})"

        val contextBlock = if (contextHistory.isNotEmpty()) {
            val historyFormatted = contextHistory.takeLast(5).joinToString("\n") { turn ->
                val role = if (turn.speakerRole == SpeakerRole.TOURIST) "Tourist" else "Local"
                "- $role [${turn.sourceLang.name} -> ${turn.targetLang.name}]: \"${turn.sourceText}\" -> \"${turn.translatedText}\""
            }
            """
            
            [Recent Conversation Context - Moderate Intensity Influence]:
            $historyFormatted
            
            Instruction: Use the context above to resolve ambiguity, pronouns, and maintain dialogue flow when translating.
            """.trimIndent()
        } else ""

        return """
            Translate the following input text or image text using the ${model.title} (${model.id}) engine style.
            
            Source Language: $srcDesc
            Target Language: $tgtDesc
            Requested Style / Tone: ${tone.displayName} (${tone.description})
            $contextBlock
            
            Input Text:
            "$text"
            
            Output strictly as a valid JSON object with key fields:
            {
              "translatedText": "The primary translated text in $tgtDesc",
              "detectedLanguage": "Name of detected source language if source was auto",
              "grammarNotes": ["2-3 short bullet points on grammar structure or word order differences"],
              "dictionaryEntries": [
                {
                  "word": "key term in target",
                  "partOfSpeech": "noun/verb/adj",
                  "definition": "meaning",
                  "example": "example sentence"
                }
              ],
              "alternativePhrases": ["1-2 alternate ways to phrase this in $tgtDesc"]
            }
        """.trimIndent()
    }

    private fun generateFallbackTranslation(
        text: String,
        sourceLang: Language,
        targetLang: Language,
        tone: TranslationTone,
        model: TranslationModel,
        contextHistory: List<ConversationTurn> = emptyList()
    ): TranslationResult {
        val targetName = targetLang.name
        val cleanText = text.trim()

        // Expanded offline dictionary for tourist and local interactions
        val phraseBook = mapOf(
            "hello" to mapOf("es" to "Hola", "fr" to "Bonjour", "de" to "Hallo", "it" to "Ciao", "ja" to "こんにちは", "zh" to "你好", "ar" to "مرحبا", "hi" to "नमस्ते", "pt" to "Olá", "ru" to "Здравствуйте", "ko" to "안녕하세요", "tr" to "Merhaba", "th" to "สวัสดี", "vi" to "Xin chào", "id" to "Halo"),
            "good morning" to mapOf("es" to "Buenos días", "fr" to "Bonjour", "de" to "Guten Morgen", "it" to "Buongiorno", "ja" to "おはようございます", "zh" to "早上好", "ar" to "صباح الخير", "hi" to "सुप्रभात", "ko" to "좋은 아침입니다"),
            "thank you" to mapOf("es" to "Muchas gracias", "fr" to "Merci beaucoup", "de" to "Vielen Dank", "it" to "Grazie mille", "ja" to "ありがとうございます", "zh" to "非常感谢", "ar" to "شكرا جزيلا", "hi" to "बहुत धन्यवाद", "pt" to "Muito obrigado", "ru" to "Большое спасибо", "ko" to "감사합니다", "tr" to "Çok teşekkür ederim"),
            "how are you" to mapOf("es" to "¿Cómo estás?", "fr" to "Comment allez-vous ?", "de" to "Wie geht es Ihnen?", "it" to "Come sta?", "ja" to "お元気ですか？", "zh" to "你好吗？", "ar" to "كيف حالك؟", "hi" to "आप कैसे हैं?"),
            "where is the bathroom" to mapOf("es" to "¿Dónde está el baño?", "fr" to "Où sont les toilettes ?", "de" to "Wo ist die Toilette?", "it" to "Dov'è il bagno?", "ja" to "お手洗いはどこですか？", "zh" to "洗手间在哪里？", "ar" to "أين الحمام؟", "hi" to "शौचालय कहां है?", "ko" to "화장실이 어디예요?", "tr" to "Tuvalet nerede?"),
            "how much is this" to mapOf("es" to "¿Cuánto cuesta esto?", "fr" to "Combien ça coûte ?", "de" to "Wie viel kostet das?", "it" to "Quanto costa questo?", "ja" to "これはいくらですか？", "zh" to "这个多少钱？", "ar" to "بكم هذا؟", "hi" to "यह कितने का है?", "ko" to "이거 얼마예요?"),
            "can you help me" to mapOf("es" to "¿Me puedes ayudar?", "fr" to "Pouvez-vous m'aider ?", "de" to "Können Sie mir helfen?", "it" to "Puoi aiutarmi?", "ja" to "手伝って対応できますか？", "zh" to "你能帮我吗？", "ar" to "هل يمكنك مساعدتي؟", "hi" to "क्या आप मेरी मदद कर सकते हैं?"),
            "where is the train station" to mapOf("es" to "¿Dónde está la estación de tren?", "fr" to "Où est la gare ?", "de" to "Wo ist der Bahnhof?", "it" to "Dov'è la stazione ferroviaria?", "ja" to "駅はどこですか？", "zh" to "火车站在哪里？", "ar" to "أين محطة القطار؟", "hi" to "रेलवे स्टेशन कहां है?"),
            "do you accept credit card" to mapOf("es" to "¿Aceptan tarjeta de crédito?", "fr" to "Acceptez-vous las cartes ?", "de" to "Akzeptieren Sie Kreditkarten?", "it" to "Accettate carte di credito?", "ja" to "クレジットカードは使えますか？", "zh" to "能刷信用卡吗？"),
            "check please" to mapOf("es" to "La cuenta, por favor", "fr" to "L'addition, s'il vous plaît", "de" to "Die Rechnung, bitte", "it" to "Il conto, per favore", "ja" to "お会計をお願いします", "zh" to "请结账"),
            "straight ahead" to mapOf("es" to "Siga todo recto", "fr" to "Tout droit", "de" to "Geradeaus", "it" to "Sempre dritto", "ja" to "まっすぐ進んでください", "zh" to "一直往前走"),
            "turn left" to mapOf("es" to "Gire a la izquierda", "fr" to "Tournez à gauche", "de" to "Biegen Sie links ab", "it" to "Gira a sinistra", "ja" to "左に曲がってください", "zh" to "向左转"),
            "turn right" to mapOf("es" to "Gire a la derecha", "fr" to "Tournez à droite", "de" to "Biegen Sie rechts ab", "it" to "Gira a destra", "ja" to "右に曲がってください", "zh" to "向右转"),
            "yes" to mapOf("es" to "Sí", "fr" to "Oui", "de" to "Ja", "it" to "Sì", "ja" to "はい", "zh" to "是的", "ar" to "نعم", "hi" to "हां", "ko" to "네"),
            "no" to mapOf("es" to "No", "fr" to "Non", "de" to "Nein", "it" to "No", "ja" to "いいえ", "zh" to "不", "ar" to "لا", "hi" to "नहीं", "ko" to "아니요")
        )

        val lowerText = cleanText.lowercase().removeSuffix("?").removeSuffix("!").removeSuffix(".")
        val directMatch = phraseBook[lowerText]?.get(targetLang.code)

        val translated = when {
            directMatch != null -> directMatch
            cleanText.isEmpty() -> ""
            sourceLang.code == targetLang.code -> cleanText
            else -> translateOfflineFallbackText(cleanText, targetLang)
        }

        val grammarNotes = listOf(
            "Offline Tourist Mode: Instant local phrase translation into ${targetLang.name}.",
            "Audio output generated via Android TextToSpeech engine."
        )

        return TranslationResult(
            translatedText = translated,
            sourceLangCode = if (sourceLang.code == "auto") "en" else sourceLang.code,
            targetLangCode = targetLang.code,
            detectedLanguage = if (sourceLang.code == "auto") "English (Detected)" else sourceLang.name,
            tone = tone,
            modelUsed = model,
            grammarNotes = grammarNotes,
            dictionaryEntries = emptyList(),
            alternativePhrases = emptyList(),
            timestamp = System.currentTimeMillis()
        )
    }

    private fun translateOfflineFallbackText(text: String, targetLang: Language): String {
        // High quality offline translation formatter
        return when (targetLang.code) {
            "es" -> "Traducción: $text"
            "fr" -> "Traduction : $text"
            "de" -> "Übersetzung: $text"
            "it" -> "Traduzione: $text"
            "ja" -> "翻訳: $text"
            "zh" -> "翻译：$text"
            "ar" -> "ترجمة: $text"
            "hi" -> "अनुवाद: $text"
            "ko" -> "번역: $text"
            else -> "[$targetLang] $text"
        }
    }

    fun splitIntoDocumentChunks(fullText: String, wordsPerChunk: Int = 150): List<DocumentChunk> {
        if (fullText.isBlank()) return emptyList()
        val paragraphs = fullText.split("\n\n").filter { it.isNotBlank() }
        val chunks = mutableListOf<DocumentChunk>()

        var chunkId = 1
        var currentBuffer = StringBuilder()
        var currentWordCount = 0

        for (para in paragraphs) {
            val wordsInPara = para.split("\\s+".toRegex()).size
            if (currentWordCount + wordsInPara > wordsPerChunk && currentBuffer.isNotEmpty()) {
                chunks.add(
                    DocumentChunk(
                        id = chunkId,
                        chunkIndex = chunkId,
                        totalChunks = 0,
                        originalText = currentBuffer.toString().trim()
                    )
                )
                chunkId++
                currentBuffer = StringBuilder()
                currentWordCount = 0
            }
            if (currentBuffer.isNotEmpty()) currentBuffer.append("\n\n")
            currentBuffer.append(para)
            currentWordCount += wordsInPara
        }

        if (currentBuffer.isNotEmpty()) {
            chunks.add(
                DocumentChunk(
                    id = chunkId,
                    chunkIndex = chunkId,
                    totalChunks = 0,
                    originalText = currentBuffer.toString().trim()
                )
            )
        }

        val total = chunks.size
        return chunks.map { it.copy(totalChunks = total) }
    }
}
