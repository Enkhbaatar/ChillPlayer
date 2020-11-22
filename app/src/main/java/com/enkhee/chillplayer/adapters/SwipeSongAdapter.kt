package com.enkhee.chillplayer.adapters

import androidx.recyclerview.widget.AsyncListDiffer
import com.enkhee.chillplayer.R
import com.enkhee.chillplayer.data.entities.Song
import kotlinx.android.synthetic.main.swipe_item.view.*

class SwipeSongAdapter : BaseSongAdapter(R.layout.swipe_item) {

    override val differ: AsyncListDiffer<Song> = AsyncListDiffer(this, diffCallback)

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.itemView.apply {
            val text = "${song.title} - ${song.subtitle}"
            songTitle.text = text
        }
    }
}