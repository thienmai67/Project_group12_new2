package com.example.project_group12

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.project_group12.data.Song

class FavoriteAdapter(
    private var songs: MutableList<Song>,
    private val onItemClick: (Song) -> Unit,
    private val onRemoveClick: (Song, Int) -> Unit
) : RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder>() {

    class FavoriteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvArtist: TextView = view.findViewById(R.id.tvArtist)
        val imgCover: ImageView = view.findViewById(R.id.imgCover)
        val btnRemoveFavorite: ImageView = view.findViewById(R.id.btnRemoveFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return FavoriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        val song = songs[position]
        holder.tvTitle.text = song.title
        holder.tvArtist.text = song.artist

        Glide.with(holder.itemView.context)
            .load(song.coverUrl)
            .placeholder(android.R.drawable.ic_media_play)
            .error(android.R.drawable.ic_dialog_alert)
            .into(holder.imgCover)

        holder.itemView.setOnClickListener {
            onItemClick(song)
        }

        holder.btnRemoveFavorite.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_ID.toInt()) {
                onRemoveClick(song, currentPosition)
            }
        }
    }

    override fun getItemCount() = songs.size

    fun updateData(newSongs: List<Song>) {
        songs = newSongs.toMutableList()
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        songs.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, songs.size)
    }
}