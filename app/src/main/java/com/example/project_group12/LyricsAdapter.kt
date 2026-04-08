package com.example.project_group12

import com.example.project_group12.R
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project_group12.data.LyricLine


class LyricsAdapter(private val lyrics: List<LyricLine>) : RecyclerView.Adapter<LyricsAdapter.LyricViewHolder>() {

    // Biến lưu vị trí câu hát hiện tại, tự động cập nhật UI khi thay đổi giá trị
    var currentLineIndex = -1
        set(value) {
            if (field != value) {
                val oldIndex = field
                field = value
                notifyItemChanged(oldIndex) // Làm mờ dòng cũ vừa hát xong
                notifyItemChanged(value)    // Làm sáng dòng mới đang hát
            }
        }

    class LyricViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLyricLine: TextView = view.findViewById(R.id.tvLyricLine)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LyricViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lyric, parent, false)
        return LyricViewHolder(view)
    }

    override fun onBindViewHolder(holder: LyricViewHolder, position: Int) {
        // Gán text cho dòng lời nhạc
        holder.tvLyricLine.text = lyrics[position].text

        // Xử lý Highlight dòng đang hát
        if (position == currentLineIndex) {
            // Dòng đang hát: Màu trắng sáng, chữ to hơn, in đậm
            holder.tvLyricLine.setTextColor(Color.WHITE)
            holder.tvLyricLine.textSize = 20f
            holder.tvLyricLine.setTypeface(null, Typeface.BOLD)
        } else {
            // Dòng chưa hát / đã hát qua: Màu trắng mờ (Alpha 50%), chữ nhỏ hơn, in thường
            holder.tvLyricLine.setTextColor(Color.parseColor("#80FFFFFF"))
            holder.tvLyricLine.textSize = 16f
            holder.tvLyricLine.setTypeface(null, Typeface.NORMAL)
        }
    }

    override fun getItemCount(): Int {
        return lyrics.size
    }
}