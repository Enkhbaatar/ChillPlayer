package com.enkhee.chillplayer.data.remote

import com.enkhee.chillplayer.data.entities.Song
import com.enkhee.chillplayer.other.Constants.SONG_COLLECTION
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MusicDatabase {
    private val firestore = FirebaseFirestore.getInstance()
    private val songCollection = firestore.collection(SONG_COLLECTION)

    suspend fun getAllSongs(): List<Song> {
        return try {
            songCollection.get().await().toObjects(Song::class.java)
        } catch (ex: Exception) {
            emptyList()
        }
    }
}