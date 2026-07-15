package com.example.mydailyactivity.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


class AlbumDataStore(private val context: Context) {

    private val ALBUMS_KEY = stringPreferencesKey("albums_json")

    val albumsFlow = context.goalDataStore.data.map { prefs ->
        val json = prefs[ALBUMS_KEY] ?: "[]"
        Json.decodeFromString<List<Album>>(json)
    }

    suspend fun saveAlbums(albums: List<Album>) {
        context.goalDataStore.edit { prefs ->
            prefs[ALBUMS_KEY] = Json.encodeToString(albums)
        }
    }

    suspend fun clearAlbums() {
        saveAlbums(emptyList())
    }

    suspend fun removeRewardFromAlbum(albumId: Int, rewardId: Int) {
        val albums = albumsFlow.first()
        val updated = albums.map { album ->
            if (album.id == albumId) {
                album.copy(rewardIds = album.rewardIds.filter { it != rewardId })
            } else album
        }
        saveAlbums(updated)
    }

    suspend fun removeRewardFromAllAlbums(rewardId: Int) {
        val albums = albumsFlow.first()

        val updated = albums.map { album ->
            album.copy(
                rewardIds = album.rewardIds.filter { it != rewardId }
            )
        }

        saveAlbums(updated)
    }

    suspend fun addRewardToAlbum(albumId: Int, rewardId: Int) {
        val albums = albumsFlow.first()

        val updated = albums.map { album ->
            if (album.id == albumId) {
                // ⭐ Reward nur hinzufügen, wenn er noch nicht existiert
                if (!album.rewardIds.contains(rewardId)) {
                    album.copy(rewardIds = album.rewardIds + rewardId)
                } else {
                    album // unverändert
                }
            } else album
        }

        saveAlbums(updated)
    }

}