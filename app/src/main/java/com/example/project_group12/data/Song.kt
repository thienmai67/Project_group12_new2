package com.example.project_group12.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey
    val id: String,              // ID bài hát (khớp với Firebase để dễ đồng bộ)
    val title: String,           // Tên bài hát
    val artist: String,          // Tên ca sĩ
    val coverUrl: String,        // Link ảnh bìa
    val mp3Url: String,          // Link nhạc t
    val genre: String,           // Thể loại nhạc
    var isFavorite: Boolean = false, // Trạng thái yêu thích
    val lyrics: String = ""      // Lời bài hát
)