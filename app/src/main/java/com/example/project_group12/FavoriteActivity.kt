package com.example.project_group12

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project_group12.data.AppDao
import com.example.project_group12.data.AppDatabase
import com.example.project_group12.data.Song
import com.example.project_group12.databinding.ActivityFavoriteBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoriteBinding
    private lateinit var dao: AppDao
    private lateinit var adapter: FavoriteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dao = AppDatabase.getDatabase(this).appDao()

        binding.btnFavoriteBack.setOnClickListener { finish() }

        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadFavoriteSongs()
    }

    private fun setupRecyclerView() {
        binding.rvFavoriteSongs.layoutManager = LinearLayoutManager(this)

        adapter = FavoriteAdapter(
            songs = mutableListOf(),
            onItemClick = { song ->
                MusicPlayerManager.playSong(song) {}
                startActivity(Intent(this, PlayerActivity::class.java))
            },
            onRemoveClick = { song, position ->
                showRemoveConfirmDialog(song, position)
            }
        )
        binding.rvFavoriteSongs.adapter = adapter
    }

    private fun showRemoveConfirmDialog(song: Song, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Xóa khỏi Yêu thích")
            .setMessage("Bạn có chắc muốn xóa \"${song.title}\" khỏi danh sách yêu thích?")
            .setPositiveButton("Xóa") { _, _ ->
                removeFavoriteSong(song, position)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun removeFavoriteSong(song: Song, position: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            dao.updateFavoriteStatus(song.id, false)
            withContext(Dispatchers.Main) {
                adapter.removeItem(position)
                Toast.makeText(
                    this@FavoriteActivity,
                    "Đã xóa \"${song.title}\" khỏi yêu thích",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadFavoriteSongs() {
        lifecycleScope.launch(Dispatchers.IO) {
            val favSongs = dao.getFavoriteSongs()
            withContext(Dispatchers.Main) {
                adapter.updateData(favSongs)
            }
        }
    }
}