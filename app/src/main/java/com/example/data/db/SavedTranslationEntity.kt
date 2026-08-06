package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_translations")
data class SavedTranslationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sourceLangCode: String,
    val targetLangCode: String,
    val sourceText: String,
    val translatedText: String,
    val tone: String,
    val modelId: String,
    val isFavorite: Boolean = false,
    val tag: String = "General", // e.g. Travel, Business, Daily, Slang
    val grammarNotesJson: String = "[]",
    val timestamp: Long = System.currentTimeMillis()
)
