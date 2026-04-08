package com.example.project_group12

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation
import com.example.project_group12.data.AppDatabase
import com.example.project_group12.data.DownloadedSong
import com.example.project_group12.data.LyricLine
import com.example.project_group12.databinding.ActivityPlayerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var isGuest = true
    private val handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false
    private var rotateAnimation: Animation? = null

    // Adapter và Danh sách lời nhạc
    private lateinit var lyricsAdapter: LyricsAdapter
    private var parsedLyricsList: List<LyricLine> = emptyList() // Khởi tạo rỗng để tránh lỗi văng app

    private val updateSeekbarRunnable = object : Runnable {
        override fun run() {
            if (!isUserSeeking) {
                try {
                    val duration = MusicPlayerManager.getDuration()
                    if (duration > 0 && binding.seekBarPlayer.max != duration) {
                        binding.seekBarPlayer.max = duration
                        binding.tvTotalTime.text = formatTime(duration)
                    }

                    val currentPos = MusicPlayerManager.getCurrentPosition()
                    binding.seekBarPlayer.progress = currentPos
                    binding.tvCurrentTime.text = formatTime(currentPos)

                    // --- LOGIC CUỘN LỜI NHẠC KHỚP THỜI GIAN THẬT ---
                    if (::lyricsAdapter.isInitialized && binding.rvLyrics.visibility == View.VISIBLE && parsedLyricsList.isNotEmpty()) {
                        val activeIndex = parsedLyricsList.indexOfLast { it.startTimeMs <= currentPos }
                        if (activeIndex != -1 && activeIndex != lyricsAdapter.currentLineIndex) {
                            lyricsAdapter.currentLineIndex = activeIndex
                            (binding.rvLyrics.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(activeIndex, 100)
                        }
                    }

                    if (isGuest && currentPos >= 30000 && MusicPlayerManager.isPlaying) {
                        MusicPlayerManager.pause()
                        updatePlayPauseStatus()
                        Toast.makeText(this@PlayerActivity, "Tài khoản Khách chỉ được nghe 30s!", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // TĂNG TỐC ĐỘ CẬP NHẬT LÊN 300ms ĐỂ CHỮ CHẠY MƯỢT HƠN
            handler.postDelayed(this, 300)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_disk)
        val dao = AppDatabase.getDatabase(this).appDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val user = dao.getCurrentUser()
            isGuest = (user?.role == "guest" || user == null)
        }

        val currentSong = MusicPlayerManager.currentSong
        if (currentSong != null) {
            binding.tvPlayerTitle.text = currentSong.title
            binding.tvPlayerArtist.text = currentSong.artist

            // ========================================================
            // ĐỌC LỜI NHẠC TỪ DATABASE (FIREBASE/ROOM) CỦA BÀI HÁT
            // ========================================================
            val songLrc = if (currentSong.lyrics.isNotEmpty()) {
                currentSong.lyrics
            } else {
                "[00:00.00] Bài hát này chưa có lời\n[00:05.00] Vui lòng chờ Admin cập nhật thêm..."
            }

            // Lấy độ dài bài hát để chia thời gian cho chữ trơn
            val duration = try { MusicPlayerManager.getDuration() } catch (e: Exception) { 180000 } // Mặc định 3 phút nếu lỗi

            parsedLyricsList = parseLrc(songLrc, duration)
            lyricsAdapter = LyricsAdapter(parsedLyricsList)
            binding.rvLyrics.layoutManager = LinearLayoutManager(this)
            binding.rvLyrics.adapter = lyricsAdapter
            // ========================================================

            Glide.with(this)
                .load(currentSong.coverUrl)
                .placeholder(android.R.drawable.ic_media_play)
                .into(binding.imgPlayerCover)

            Glide.with(this)
                .load(currentSong.coverUrl)
                .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 3)))
                .into(binding.imgBlurredBackground)

            try {
                val durationMs = MusicPlayerManager.getDuration()
                binding.seekBarPlayer.max = durationMs
                binding.tvTotalTime.text = formatTime(durationMs)
            } catch (e: Exception) {
                binding.seekBarPlayer.max = 0
            }
        }

        updatePlayPauseStatus()
        handler.postDelayed(updateSeekbarRunnable, 0)
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnPlayerBack.setOnClickListener { finish() }

        updateLikeButtonIcon(MusicPlayerManager.currentSong?.isFavorite == true)

        binding.btnPlayerLike.setOnClickListener {
            if (isGuest) {
                Toast.makeText(this, "Đăng nhập để thêm vào Yêu thích!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val song = MusicPlayerManager.currentSong ?: return@setOnClickListener
            val newFavStatus = !song.isFavorite
            MusicPlayerManager.currentSong = song.copy(isFavorite = newFavStatus)

            lifecycleScope.launch(Dispatchers.IO) {
                val dao = AppDatabase.getDatabase(this@PlayerActivity).appDao()
                dao.updateFavoriteStatus(song.id, newFavStatus)

                runOnUiThread {
                    updateLikeButtonIcon(newFavStatus)
                    val msg = if (newFavStatus) "Đã thêm vào Yêu thích" else "Đã bỏ Yêu thích"

                    val snackbar = com.google.android.material.snackbar.Snackbar.make(binding.root, msg, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    snackbar.setBackgroundTint(Color.parseColor("#222222"))
                    snackbar.setTextColor(Color.parseColor("#00E5FF"))
                    snackbar.show()
                }
            }
        }

        binding.seekBarPlayer.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.tvCurrentTime.text = formatTime(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val newPosition = seekBar?.progress ?: 0
                if (isGuest && newPosition >= 30000) {
                    MusicPlayerManager.seekTo(29000)
                    binding.seekBarPlayer.progress = 29000
                    binding.tvCurrentTime.text = formatTime(29000)
                } else {
                    MusicPlayerManager.seekTo(newPosition)
                    binding.tvCurrentTime.text = formatTime(newPosition)
                }

                handler.postDelayed({
                    isUserSeeking = false
                }, 800)
            }
        })

        // Nút Phát/Dừng
        binding.btnPlayerPlayPause.setOnClickListener {
            if (MusicPlayerManager.isPlaying) {
                MusicPlayerManager.pause()
            } else {
                MusicPlayerManager.resume()
            }
            updatePlayPauseStatus()
        }

        // Nút Next
        binding.btnPlayerNext.setOnClickListener {
            Toast.makeText(this, "Tính năng chuyển bài tiếp theo đang được phát triển!", Toast.LENGTH_SHORT).show()
        }

        // Nút Prev
        binding.btnPlayerPrev.setOnClickListener {
            Toast.makeText(this, "Tính năng lùi bài đang được phát triển!", Toast.LENGTH_SHORT).show()
        }

        // --- SỰ KIỆN BẤM NÚT LỜI NHẠC ---
        binding.btnPlayerLyrics.setOnClickListener {
            if (isGuest) {
                Toast.makeText(this, "Đăng nhập để xem lời nhạc!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (binding.rvLyrics.visibility == View.GONE) {
                binding.rvLyrics.visibility = View.VISIBLE
                binding.btnPlayerLyrics.setTextColor(Color.parseColor("#FF8C42"))
            } else {
                binding.rvLyrics.visibility = View.GONE
                binding.btnPlayerLyrics.setTextColor(Color.WHITE)
            }
        }

        binding.btnPlayerDownload.setOnClickListener {
            if (isGuest) {
                Toast.makeText(this, "Vui lòng đăng nhập để tải bài hát!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val currentSong = MusicPlayerManager.currentSong
            if (currentSong == null) return@setOnClickListener

            Toast.makeText(this, "Đang tải bài hát: ${currentSong.title}...", Toast.LENGTH_SHORT).show()

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val url = URL(currentSong.mp3Url)
                    val fileName = "${currentSong.id}.mp3"
                    val file = File(this@PlayerActivity.filesDir, fileName)

                    url.openStream().use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }

                    val dao = AppDatabase.getDatabase(this@PlayerActivity).appDao()
                    val downloadedSong = DownloadedSong(
                        id = currentSong.id,
                        title = currentSong.title,
                        artist = currentSong.artist,
                        coverUrl = currentSong.coverUrl,
                        localPath = file.absolutePath
                    )
                    dao.insertDownloadedSong(downloadedSong)

                    runOnUiThread {
                        Toast.makeText(this@PlayerActivity, "Tải thành công: ${currentSong.title}!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this@PlayerActivity, "Lỗi khi tải bài hát!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updatePlayPauseStatus() {
        if (MusicPlayerManager.isPlaying) {
            binding.btnPlayerPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            startRotateDisk()
        } else {
            binding.btnPlayerPlayPause.setImageResource(android.R.drawable.ic_media_play)
            stopRotateDisk()
        }
    }

    private fun startRotateDisk() {
        if (binding.cardPlayerCover.animation == null) {
            binding.cardPlayerCover.startAnimation(rotateAnimation)
        }
    }

    private fun stopRotateDisk() {
        binding.cardPlayerCover.clearAnimation()
    }

    private fun updateLikeButtonIcon(isFavorite: Boolean) {
        if (isFavorite) {
            binding.btnPlayerLike.setImageResource(R.drawable.ic_heart_filled)
            binding.btnPlayerLike.imageTintList = ColorStateList.valueOf(Color.parseColor("#FF007F"))
        } else {
            binding.btnPlayerLike.setImageResource(R.drawable.ic_heart_outline)
            binding.btnPlayerLike.imageTintList = ColorStateList.valueOf(Color.WHITE)
        }
    }

    private fun formatTime(millis: Int): String {
        val totalSeconds = millis / 1000
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    // --- HÀM PHÂN TÍCH VÀ TỰ ĐỘNG CHIA THỜI GIAN LỜI NHẠC ---
    private fun parseLrc(lrcText: String, songDuration: Int): List<LyricLine> {
        val lyricLines = mutableListOf<LyricLine>()
        val lines = lrcText.split("\n")

        val isLrcFormat = lines.any { it.contains("[") && it.contains("]") }

        if (!isLrcFormat) {
            val validLines = lines.filter { it.trim().isNotEmpty() }
            if (validLines.isNotEmpty()) {
                val safeDuration = if (songDuration <= 0) 210000 else songDuration

                val introTime = 12000
                val outroTime = 25000

                val timeToSing = if (safeDuration > (introTime + outroTime)) {
                    safeDuration - introTime - outroTime
                } else {
                    safeDuration
                }

                // ĐÃ CHỈNH LẠI: Đổi 0.95f thành 0.88f để chữ cuộn nhanh hơn nữa!
                val timePerLine = (timeToSing / validLines.size.toFloat() * 0.77f).toInt()

                var currentTime = introTime
                for (line in validLines) {
                    lyricLines.add(LyricLine(currentTime, line.trim()))
                    currentTime += timePerLine
                }
            }
            return lyricLines
        }

        // NẾU LÀ LRC CHUẨN CÓ THỜI GIAN
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("[") && trimmedLine.contains("]")) {
                try {
                    val timeString = trimmedLine.substringAfter("[").substringBefore("]")
                    val text = trimmedLine.substringAfter("]").trim()

                    if (timeString.contains(":")) {
                        val parts = timeString.split(":")
                        val min = parts[0].toIntOrNull() ?: 0
                        val secParts = parts[1].split(".")
                        val sec = secParts[0].toIntOrNull() ?: 0

                        val ms = if (secParts.size > 1) {
                            val msPart = secParts[1]
                            when (msPart.length) {
                                1 -> msPart.toInt() * 100
                                2 -> msPart.toInt() * 10
                                else -> msPart.substring(0, 3).toInt()
                            }
                        } else 0

                        val totalMs = (min * 60 * 1000) + (sec * 1000) + ms
                        lyricLines.add(LyricLine(totalMs, text))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return lyricLines.sortedBy { it.startTimeMs }
    }


    override fun onResume() {
        super.onResume()
        updatePlayPauseStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekbarRunnable)
    }
}