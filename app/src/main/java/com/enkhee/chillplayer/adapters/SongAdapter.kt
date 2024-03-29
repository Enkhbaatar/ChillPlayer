package com.enkhee.chillplayer.adapters

import androidx.recyclerview.widget.AsyncListDiffer
import com.bumptech.glide.RequestManager
import com.enkhee.chillplayer.R
import com.enkhee.chillplayer.data.entities.Song
import kotlinx.android.synthetic.main.list_item.view.*
import javax.inject.Inject

class SongAdapter @Inject constructor(
    private val glide: RequestManager
) : BaseSongAdapter(R.layout.list_item) {

    override val differ: AsyncListDiffer<Song> = AsyncListDiffer(this, diffCallback)

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.itemView.apply {
            tvTitle.text = song.title
            tvSubtitle.text = song.subtitle
            glide.load(song.imageUrl).into(ivSongImage)
            setOnClickListener { onSongItemClickListener?.let { click -> click(song) } }
        }
    }
}