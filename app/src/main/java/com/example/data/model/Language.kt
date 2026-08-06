package com.example.data.model

enum class SpeakerRole {
    TOURIST, LOCAL
}

data class ConversationTurn(
    val id: Long = System.currentTimeMillis(),
    val speakerRole: SpeakerRole,
    val sourceLang: Language,
    val targetLang: Language,
    val sourceText: String,
    val translatedText: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagEmoji: String,
    val isPopular: Boolean = false
) {
    companion object {
        val AUTO = Language("auto", "Auto Detect", "Detect Language", "✨")

        val SUPPORTED_LANGUAGES = listOf(
            Language("en", "English", "English", "🇺🇸", isPopular = true),
            Language("es", "Spanish", "Español", "🇪🇸", isPopular = true),
            Language("fr", "French", "Français", "🇫🇷", isPopular = true),
            Language("de", "German", "Deutsch", "🇩🇪", isPopular = true),
            Language("zh", "Chinese (Simplified)", "中文(简体)", "🇨🇳", isPopular = true),
            Language("zh-TW", "Chinese (Traditional)", "中文(繁體)", "🇭🇰"),
            Language("ja", "Japanese", "日本語", "🇯🇵", isPopular = true),
            Language("ko", "Korean", "한국어", "🇰🇷", isPopular = true),
            Language("ar", "Arabic", "العربية", "🇸🇦", isPopular = true),
            Language("pt", "Portuguese", "Português", "🇧🇷", isPopular = true),
            Language("ru", "Russian", "Русский", "🇷🇺", isPopular = true),
            Language("hi", "Hindi", "हिन्दी", "🇮🇳", isPopular = true),
            Language("it", "Italian", "Italiano", "🇮🇹"),
            Language("nl", "Dutch", "Nederlands", "🇳🇱"),
            Language("tr", "Turkish", "Türkçe", "🇹🇷"),
            Language("vi", "Vietnamese", "Tiếng Việt", "🇻🇳"),
            Language("pl", "Polish", "Polski", "🇵🇱"),
            Language("uk", "Ukrainian", "Українська", "🇺🇦"),
            Language("id", "Indonesian", "Bahasa Indonesia", "🇮🇩"),
            Language("th", "Thai", "ไทย", "🇹🇭"),
            Language("bn", "Bengali", "বাংলা", "🇧🇩"),
            Language("sv", "Swedish", "Svenska", "🇸🇪"),
            Language("el", "Greek", "Ελληνικά", "🇬🇷"),
            Language("cs", "Czech", "Čeština", "🇨🇿"),
            Language("hu", "Hungarian", "Magyar", "🇭🇺"),
            Language("ro", "Romanian", "Română", "🇷🇴"),
            Language("da", "Danish", "Dansk", "🇩🇰"),
            Language("fi", "Finnish", "Suomi", "🇫🇮"),
            Language("no", "Norwegian", "Norsk", "🇳🇴"),
            Language("he", "Hebrew", "עברית", "🇮🇱"),
            Language("ms", "Malay", "Bahasa Melayu", "🇲🇾"),
            Language("fa", "Persian", "فارسی", "🇮🇷"),
            Language("sw", "Swahili", "Kiswahili", "🇰🇪"),
            Language("ta", "Tamil", "தமிழ்", "🇮🇳"),
            Language("te", "Telugu", "తెలుగు", "🇮🇳"),
            Language("mr", "Marathi", "मराठी", "🇮🇳"),
            Language("ur", "Urdu", "اردو", "🇵🇰"),
            Language("sk", "Slovak", "Slovenčina", "🇸🇰"),
            Language("bg", "Bulgarian", "Български", "🇧🇬"),
            Language("hr", "Croatian", "Hrvatski", "🇭🇷"),
            Language("sr", "Serbian", "Српски", "🇷🇸"),
            Language("lt", "Lithuanian", "Lietuvių", "🇱🇹"),
            Language("lv", "Latvian", "Latviešu", "🇱🇻"),
            Language("et", "Estonian", "Eesti", "🇪🇪"),
            Language("sl", "Slovenian", "Slovenščina", "🇸🇮"),
            Language("fil", "Filipino", "Tagalog", "🇵🇭"),
            Language("ca", "Catalan", "Català", "🇪🇸"),
            Language("gl", "Galician", "Galego", "🇪🇸"),
            Language("eu", "Basque", "Euskara", "🇪🇸"),
            Language("la", "Latin", "Latina", "🏛️")
        )

        fun findByCode(code: String): Language {
            if (code == "auto") return AUTO
            return SUPPORTED_LANGUAGES.find { it.code.equals(code, ignoreCase = true) }
                ?: Language(code, code.uppercase(), code.uppercase(), "🌐")
        }
    }
}

enum class TranslationTone(val displayName: String, val description: String, val icon: String) {
    STANDARD("Standard", "Natural balanced translation", "💬"),
    FORMAL("Formal", "Polite, business & academic style", "👔"),
    CASUAL("Casual", "Friendly, conversational & informal", "☕"),
    TECHNICAL("Technical", "Precise terminology & clear domain jargon", "⚙️"),
    CREATIVE("Creative", "Expressive, literary & localized nuance", "🎨"),
    IDIOMATIC("Native / Idiomatic", "Natural local expressions & sayings", "🌟"),
    SIMPLIFIED("Simplified (ELI5)", "Easy to read & clear basic words", "🌱")
}

enum class TranslationModel(val id: String, val title: String, val badge: String, val isLocal: Boolean) {
    GEMMA_3_12B("translategemma:12b", "Gemma 3 12B", "TranslateGemma Core", true),
    GEMMA_3_4B("translategemma:4b", "Gemma 3 4B", "Gemma Ultra-Fast", true),
    GEMMA_3_27B("translategemma:27b", "Gemma 3 27B", "Gemma Deep Precision", true),
    GEMINI_FLASH("gemini-3.5-flash", "Gemini 3.5 Flash", "Cloud AI Engine", false)
}

data class DictionaryEntry(
    val word: String,
    val partOfSpeech: String,
    val definition: String,
    val example: String? = null
)

data class TranslationResult(
    val translatedText: String,
    val sourceLangCode: String,
    val targetLangCode: String,
    val detectedLanguage: String? = null,
    val tone: TranslationTone = TranslationTone.STANDARD,
    val modelUsed: TranslationModel = TranslationModel.GEMMA_3_12B,
    val grammarNotes: List<String> = emptyList(),
    val dictionaryEntries: List<DictionaryEntry> = emptyList(),
    val alternativePhrases: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class ChunkStatus {
    PENDING,
    TRANSLATING,
    COMPLETED,
    ERROR
}

data class DocumentChunk(
    val id: Int,
    val chunkIndex: Int,
    val totalChunks: Int,
    val originalText: String,
    val translatedText: String = "",
    val status: ChunkStatus = ChunkStatus.PENDING,
    val errorMessage: String? = null
)
