package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {
    @Query("SELECT * FROM saved_translations ORDER BY timestamp DESC")
    fun getAllTranslations(): Flow<List<SavedTranslationEntity>>

    @Query("SELECT * FROM saved_translations WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteTranslations(): Flow<List<SavedTranslationEntity>>

    @Query("SELECT * FROM saved_translations WHERE sourceText LIKE '%' || :query || '%' OR translatedText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchTranslations(query: String): Flow<List<SavedTranslationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(translation: SavedTranslationEntity): Long

    @Query("UPDATE saved_translations SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFavorite: Boolean)

    @Query("DELETE FROM saved_translations WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM saved_translations WHERE isFavorite = 0")
    suspend fun clearHistory()
}
