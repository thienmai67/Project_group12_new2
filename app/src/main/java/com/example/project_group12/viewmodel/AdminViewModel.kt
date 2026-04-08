package com.example.project_group12.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_group12.data.Song
import com.example.project_group12.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.Normalizer

sealed class AdminState {
    object Idle : AdminState()
    object Loading : AdminState()
    object SuccessAdd : AdminState()
    object SuccessDelete : AdminState()
    object SuccessUpdate : AdminState()
    data class Error(val message: String) : AdminState()
}

class AdminViewModel(private val repository: SongRepository) : ViewModel() {

    private val _adminState = MutableStateFlow<AdminState>(AdminState.Idle)
    val adminState: StateFlow<AdminState> = _adminState

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs
    private var allSongsBackup = listOf<Song>()

    init {
        loadSongs()
    }

    fun loadSongs() {
        viewModelScope.launch {
            _adminState.value = AdminState.Loading
            val localSongs = repository.getLocalSongs()
            allSongsBackup = localSongs
            _songs.value = localSongs

            val result = repository.syncSongsFromFirebase()
            result.onSuccess { syncedSongs ->
                allSongsBackup = syncedSongs
                _songs.value = syncedSongs
                _adminState.value = AdminState.Idle
            }
        }
    }

    fun searchSongs(query: String) {
        if (query.isEmpty()) {
            _songs.value = allSongsBackup
            return
        }
        val normalizedQuery = query.removeAccents()
        val filteredList = allSongsBackup.filter {
            it.title.removeAccents().contains(normalizedQuery) ||
                    it.artist.removeAccents().contains(normalizedQuery)
        }
        _songs.value = filteredList
    }

    private fun String.removeAccents(): String {
        val str = this.replace("đ", "d").replace("Đ", "D")
        val normalized = Normalizer.normalize(str, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(normalized, "").lowercase()
    }

    // ---> CẬP NHẬT Ở ĐÂY: Thêm lyrics: String <---
    fun uploadNewSong(title: String, artist: String, coverUrl: String, mp3Url: String, genre: String, lyrics: String) {
        if (title.isBlank() || mp3Url.isBlank()) {
            _adminState.value = AdminState.Error("Tên bài hát và Link MP3 không được để trống!")
            return
        }
        _adminState.value = AdminState.Loading
        viewModelScope.launch {
            // Truyền thêm lyrics xuống cho Repository
            val result = repository.addSong(title, artist, coverUrl, mp3Url, genre, lyrics)
            result.onSuccess {
                _adminState.value = AdminState.SuccessAdd
                loadSongs()
            }.onFailure { error ->
                _adminState.value = AdminState.Error(error.message ?: "Lỗi thêm nhạc")
            }
        }
    }

    // --- Xử lý cập nhật bài hát ---
    fun updateSongData(song: Song) {
        _adminState.value = AdminState.Loading
        viewModelScope.launch {
            val result = repository.updateSong(song)
            result.onSuccess {
                _adminState.value = AdminState.SuccessUpdate
                loadSongs()
            }.onFailure { error ->
                _adminState.value = AdminState.Error(error.message ?: "Lỗi sửa nhạc")
            }
        }
    }

    fun deleteSong(song: Song) {
        _adminState.value = AdminState.Loading
        viewModelScope.launch {
            val result = repository.deleteSong(song)
            result.onSuccess {
                _adminState.value = AdminState.SuccessDelete
                loadSongs()
            }.onFailure { error ->
                _adminState.value = AdminState.Error(error.message ?: "Lỗi xóa nhạc")
            }
        }
    }

    fun resetState() { _adminState.value = AdminState.Idle }
}