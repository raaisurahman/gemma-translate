package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechRecognizerHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var lastRecognizedText: String = ""
    private val mainHandler = Handler(Looper.getMainLooper())

    interface SpeechListener {
        fun onSpeechStarted()
        fun onRmsChanged(rmsdB: Float)
        fun onPartialResults(partialText: String)
        fun onResults(resultText: String)
        fun onError(errorMsg: String)
    }

    fun startListening(languageCode: String, listener: SpeechListener) {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                listener.onError("Speech recognition not available on this device")
                return@post
            }

            cleanup()
            lastRecognizedText = ""

            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                            listener.onSpeechStarted()
                        }

                        override fun onBeginningOfSpeech() {}

                        override fun onRmsChanged(rmsdB: Float) {
                            if (isListening) {
                                listener.onRmsChanged(rmsdB)
                            }
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            isListening = false
                        }

                        override fun onError(error: Int) {
                            isListening = false
                            Log.d("SpeechRecognizerHelper", "SpeechRecognizer onError code: $error")

                            val fallbackText = lastRecognizedText.trim()
                            if (fallbackText.isNotBlank()) {
                                listener.onResults(fallbackText)
                            } else {
                                when (error) {
                                    SpeechRecognizer.ERROR_NO_MATCH,
                                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                                    SpeechRecognizer.ERROR_CLIENT,
                                    8 -> { // BUSY
                                        listener.onResults("")
                                    }
                                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                                        listener.onError("Microphone permission required")
                                    }
                                    SpeechRecognizer.ERROR_NETWORK,
                                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                                        listener.onError("Network error during speech input")
                                    }
                                    SpeechRecognizer.ERROR_AUDIO -> {
                                        listener.onError("Audio recording error")
                                    }
                                    else -> {
                                        listener.onResults("")
                                    }
                                }
                            }
                            cleanup()
                        }

                        override fun onResults(results: Bundle?) {
                            isListening = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognized = matches?.firstOrNull()?.trim()
                            val finalText = if (!recognized.isNullOrBlank()) recognized else lastRecognizedText.trim()
                            Log.d("SpeechRecognizerHelper", "onResults final text: '$finalText'")
                            listener.onResults(finalText)
                            cleanup()
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()?.trim() ?: ""
                            if (text.isNotBlank()) {
                                lastRecognizedText = text
                                listener.onPartialResults(text)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val formattedLanguage = formatLanguageCode(languageCode)

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, formattedLanguage)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, formattedLanguage)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("SpeechRecognizerHelper", "Failed to start listening", e)
                listener.onError("Could not access speech recognition service")
                cleanup()
            }
        }
    }

    private fun formatLanguageCode(code: String): String {
        return when (code.lowercase()) {
            "en" -> "en-US"
            "es" -> "es-ES"
            "fr" -> "fr-FR"
            "de" -> "de-DE"
            "zh" -> "zh-CN"
            "zh-tw" -> "zh-TW"
            "ja" -> "ja-JP"
            "ko" -> "ko-KR"
            "ar" -> "ar-SA"
            "pt" -> "pt-BR"
            "ru" -> "ru-RU"
            "hi" -> "hi-IN"
            "bn" -> "bn-BD"
            "it" -> "it-IT"
            "nl" -> "nl-NL"
            "tr" -> "tr-TR"
            "vi" -> "vi-VN"
            "pl" -> "pl-PL"
            "id" -> "id-ID"
            "th" -> "th-TH"
            else -> code
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                if (isListening) {
                    speechRecognizer?.stopListening()
                    isListening = false
                }
            } catch (e: Exception) {
                Log.e("SpeechRecognizerHelper", "Error stopping speech recognizer", e)
            }
        }
    }

    private fun cleanup() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
                isListening = false
            } catch (e: Exception) {
                Log.e("SpeechRecognizerHelper", "Error cleaning up speech recognizer", e)
            }
        }
    }
}

