package com.enkhee.chillplayer.adapters

import android.view.View
import androidx.recyclerview.widget.AsyncListDiffer
import com.enkhee.chillplayer.R
import com.enkhee.chillplayer.data.entities.Song
import kotlinx.android.synthetic.main.list_item.view.*

class SwipeSongAdapter : BaseSongAdapter(R.layout.list_item) {

    override val differ: AsyncListDiffer<Song> = AsyncListDiffer(this, diffCallback)

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.itemView.apply {
            val text = "${song.title} - ${song.subtitle}"
            tvTitle.text = "${song.title} - ${song.subtitle}"
            tvSubtitle.visibility = View.GONE
            ivSongImage.visibility = View.GONE
        }
    }
}