package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class TranslationJsonResponse(
    @Json(name = "translatedText") val translatedText: String = "",
    @Json(name = "detectedLanguage") val detectedLanguage: String? = null,
    @Json(name = "grammarNotes") val grammarNotes: List<String> = emptyList(),
    @Json(name = "dictionaryEntries") val dictionaryEntries: List<JsonDictionaryEntry> = emptyList(),
    @Json(name = "alternativePhrases") val alternativePhrases: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JsonDictionaryEntry(
    @Json(name = "word") val word: String = "",
    @Json(name = "partOfSpeech") val partOfSpeech: String = "",
    @Json(name = "definition") val definition: String = "",
    @Json(name = "example") val example: String? = null
)
