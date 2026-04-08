package com.example.project_group12

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project_group12.data.AppDatabase
import com.example.project_group12.data.Song
import com.example.project_group12.databinding.ActivityAdminBinding
import com.example.project_group12.repository.SongRepository
import com.example.project_group12.viewmodel.AdminState
import com.example.project_group12.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var viewModel: AdminViewModel
    private lateinit var adapter: AdminSongAdapter
    private var addSongDialog: AlertDialog? = null
    private var editSongDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dao = AppDatabase.getDatabase(this).appDao()
        val repository = SongRepository(dao)
        viewModel = AdminViewModel(repository)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvAdminSongs.layoutManager = LinearLayoutManager(this)

        adapter = AdminSongAdapter(
            songs = emptyList(),
            onEditClick = { songToEdit ->
                showEditSongDialog(songToEdit)
            },
            onDeleteClick = { songToDelete ->
                AlertDialog.Builder(this)
                    .setTitle("Xóa bài hát")
                    .setMessage("Bạn có chắc muốn xóa bài '${songToDelete.title}' không?")
                    .setPositiveButton("Xóa") { _, _ -> viewModel.deleteSong(songToDelete) }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        )
        binding.rvAdminSongs.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnAdminBack.setOnClickListener { finish() }
        binding.fabAddSong.setOnClickListener { showAddSongDialog() }

        binding.edtAdminSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchSongs(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showAddSongDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_song, null)
        val edtTitle = dialogView.findViewById<EditText>(R.id.edtDialogTitle)
        val edtArtist = dialogView.findViewById<EditText>(R.id.edtDialogArtist)
        val edtGenre = dialogView.findViewById<EditText>(R.id.edtDialogGenre)
        val edtCover = dialogView.findViewById<EditText>(R.id.edtDialogCover)
        val edtMp3 = dialogView.findViewById<EditText>(R.id.edtDialogMp3)
        val edtLyrics = dialogView.findViewById<EditText>(R.id.edtDialogLyrics) // THÊM DÒNG NÀY
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogSave)

        addSongDialog = AlertDialog.Builder(this).setView(dialogView).create()
        btnSave.setOnClickListener {
            val title = edtTitle.text.toString().trim()
            val artist = edtArtist.text.toString().trim()
            val genre = edtGenre.text.toString().trim()
            val coverUrl = edtCover.text.toString().trim()
            val mp3Url = edtMp3.text.toString().trim()
            val lyrics = edtLyrics.text.toString().trim() // THÊM DÒNG NÀY

            // LƯU Ý: Phải gọi hàm upload có chứa lyrics (Xem bước 3 bên dưới)
            viewModel.uploadNewSong(title, artist, coverUrl, mp3Url, genre, lyrics)
        }
        addSongDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        addSongDialog?.show()
    }

    private fun showEditSongDialog(song: Song) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_song, null)

        val edtTitle = dialogView.findViewById<EditText>(R.id.edtDialogTitle)
        val edtArtist = dialogView.findViewById<EditText>(R.id.edtDialogArtist)
        val edtGenre = dialogView.findViewById<EditText>(R.id.edtDialogGenre)
        val edtCover = dialogView.findViewById<EditText>(R.id.edtDialogCover)
        val edtMp3 = dialogView.findViewById<EditText>(R.id.edtDialogMp3)
        val edtLyrics = dialogView.findViewById<EditText>(R.id.edtDialogLyrics) // THÊM DÒNG NÀY
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogSave)

        edtTitle.setText(song.title)
        edtArtist.setText(song.artist)
        edtGenre.setText(song.genre)
        edtCover.setText(song.coverUrl)
        edtMp3.setText(song.mp3Url)
        edtLyrics.setText(song.lyrics) // ĐỔI LỜI BÀI HÁT CŨ VÀO Ô NHẬP

        btnSave.text = "CẬP NHẬT BÀI HÁT"

        editSongDialog = AlertDialog.Builder(this).setView(dialogView).create()

        btnSave.setOnClickListener {
            val updatedSong = Song(
                id = song.id,
                title = edtTitle.text.toString().trim(),
                artist = edtArtist.text.toString().trim(),
                coverUrl = edtCover.text.toString().trim(),
                mp3Url = edtMp3.text.toString().trim(),
                genre = edtGenre.text.toString().trim(),
                lyrics = edtLyrics.text.toString().trim() // CẬP NHẬT LỜI BÀI HÁT MỚI
            )
            viewModel.updateSongData(updatedSong)
        }

        editSongDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        editSongDialog?.show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.songs.collect { songList ->
                adapter.updateData(songList)
            }
        }

        lifecycleScope.launch {
            viewModel.adminState.collect { state ->
                when (state) {
                    is AdminState.Idle -> binding.progressBarAdminList.visibility = View.GONE
                    is AdminState.Loading -> binding.progressBarAdminList.visibility = View.VISIBLE
                    is AdminState.SuccessAdd -> {
                        binding.progressBarAdminList.visibility = View.GONE
                        Toast.makeText(this@AdminActivity, "Thêm bài hát thành công!", Toast.LENGTH_SHORT).show()
                        addSongDialog?.dismiss()
                        viewModel.resetState()
                    }
                    is AdminState.SuccessDelete -> {
                        binding.progressBarAdminList.visibility = View.GONE
                        Toast.makeText(this@AdminActivity, "Đã xóa bài hát", Toast.LENGTH_SHORT).show()
                        viewModel.resetState()
                    }
                    is AdminState.SuccessUpdate -> {
                        binding.progressBarAdminList.visibility = View.GONE
                        Toast.makeText(this@AdminActivity, "Đã cập nhật bài hát!", Toast.LENGTH_SHORT).show()
                        editSongDialog?.dismiss()
                        viewModel.resetState()
                    }
                    is AdminState.Error -> {
                        binding.progressBarAdminList.visibility = View.GONE
                        Toast.makeText(this@AdminActivity, "Lỗi: ${state.message}", Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                }
            }
        }
    }
}