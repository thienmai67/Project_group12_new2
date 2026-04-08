package com.example.project_group12.repository

import com.example.project_group12.data.AppDao
import com.example.project_group12.data.Song
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SongRepository(private val dao: AppDao) {
    private val firestore = FirebaseFirestore.getInstance()
    private val songCollection = firestore.collection("songs")

    suspend fun syncSongsFromFirebase(): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            // Lấy danh sách ID các bài hát ĐÃ YÊU THÍCH ở máy hiện tại
            val localFavorites = dao.getFavoriteSongs().map { it.id }

            val snapshot = songCollection.get().await()
            val songs = snapshot.documents.mapNotNull { doc ->
                val id = doc.id
                val title = doc.getString("title") ?: ""
                val artist = doc.getString("artist") ?: ""
                val coverUrl = doc.getString("coverUrl") ?: ""
                val mp3Url = doc.getString("mp3Url") ?: ""
                val genre = doc.getString("genre") ?: ""
                val lyrics = doc.getString("lyrics") ?: "" // CẬP NHẬT 1: Lấy lyrics từ Firebase về

                val isFav = localFavorites.contains(id)

                // CẬP NHẬT 2: Truyền thêm lyrics vào để tạo Song
                Song(
                    id = id,
                    title = title,
                    artist = artist,
                    coverUrl = coverUrl,
                    mp3Url = mp3Url,
                    genre = genre,
                    isFavorite = isFav,
                    lyrics = lyrics
                )
            }

            // Ghi đè dữ liệu mới nhưng vẫn giữ được trạng thái isFavorite của người dùng
            songs.forEach { dao.insertSong(it) }
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLocalSongs(): List<Song> = withContext(Dispatchers.IO) {
        dao.getAllSongs()
    }

    // CẬP NHẬT 3: Thêm lyrics: String vào tham số truyền vào
    suspend fun addSong(title: String, artist: String, coverUrl: String, mp3Url: String, genre: String, lyrics: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val newDocRef = songCollection.document()
            val songData = hashMapOf(
                "title" to title,
                "artist" to artist,
                "coverUrl" to coverUrl,
                "mp3Url" to mp3Url,
                "genre" to genre,
                "lyrics" to lyrics // Đẩy lyrics lên Firebase
            )

            newDocRef.set(songData).await()

            // Tạo đối tượng Song để lưu xuống Room Local
            val newSong = Song(
                id = newDocRef.id,
                title = title,
                artist = artist,
                coverUrl = coverUrl,
                mp3Url = mp3Url,
                genre = genre,
                isFavorite = false,
                lyrics = lyrics // Lưu lyrics vào Room
            )
            dao.insertSong(newSong)

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSong(song: Song): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            songCollection.document(song.id).delete().await()
            dao.deleteSong(song)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSong(song: Song): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val songData = hashMapOf(
                "title" to song.title,
                "artist" to song.artist,
                "coverUrl" to song.coverUrl,
                "mp3Url" to song.mp3Url,
                "genre" to song.genre,
                "lyrics" to song.lyrics // CẬP NHẬT 4: Cập nhật lời bài hát mới lên Firebase khi Admin sửa
            )
            songCollection.document(song.id).update(songData as Map<String, Any>).await()
            dao.updateSong(song)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}