package com.enkhee.chillplayer.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.RequestManager
import com.enkhee.chillplayer.R
import com.enkhee.chillplayer.adapters.SwipeSongAdapter
import com.enkhee.chillplayer.data.entities.Song
import com.enkhee.chillplayer.exoplayer.toSong
import com.enkhee.chillplayer.other.Constants
import com.enkhee.chillplayer.other.Status
import com.enkhee.chillplayer.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var glide: RequestManager
    @Inject lateinit var swipeSongAdapter: SwipeSongAdapter

    private val mainViewModel: MainViewModel by viewModels()
    private var currentPlayingSong: Song? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        subscribeToObservers()
        songPager.adapter = swipeSongAdapter
    }

    private fun switchViewPagerToCurrentSong(song: Song) {
        val newItemIndex = swipeSongAdapter.songs.indexOf(song)
        if (newItemIndex != Constants.INVALID_INDEX) {
            songPager.currentItem = newItemIndex
            currentPlayingSong = song
        }
    }

    private fun subscribeToObservers() {
        mainViewModel.mediaItems.observe(this) {
            it?.let { result ->
                when (result.status) {
                    Status.SUCCESS -> {
                        result.data?.let { songs ->
                            swipeSongAdapter.songs = songs
                            if (songs.isNotEmpty()) {
                                val imageUrl = (currentPlayingSong
                                    ?: songs[Constants.FIRST_SONG_INDEX]).imageUrl
                                //glide.load(imageUrl).into(currentSongImage)
                            }
                            switchViewPagerToCurrentSong(currentPlayingSong ?: return@observe)
                        }
                    }
                    Status.ERROR -> Unit
                    Status.LOADING -> Unit
                }
            }
        }

        mainViewModel.curPlayingSong.observe(this) {
            if (it == null) return@observe
            currentPlayingSong = it.toSong()
            currentPlayingSong?.run { glide.load(imageUrl).into(currentSongImage)}
            switchViewPagerToCurrentSong(currentPlayingSong ?: return@observe)
        }
    }
}