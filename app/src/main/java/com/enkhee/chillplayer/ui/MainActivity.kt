package com.enkhee.chillplayer.ui

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.RequestManager
import com.enkhee.chillplayer.R
import com.enkhee.chillplayer.adapters.SwipeSongAdapter
import com.enkhee.chillplayer.data.entities.Song
import com.enkhee.chillplayer.exoplayer.isPlaying
import com.enkhee.chillplayer.exoplayer.toSong
import com.enkhee.chillplayer.other.Constants
import com.enkhee.chillplayer.other.Constants.UNKNOWN_ERROR_OCCURRED
import com.enkhee.chillplayer.other.Status
import com.enkhee.chillplayer.ui.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var glide: RequestManager
    @Inject
    lateinit var swipeSongAdapter: SwipeSongAdapter

    private val mainViewModel: MainViewModel by viewModels()
    private var currentPlayingSong: Song? = null
    private var playBackState: PlaybackStateCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        subscribeToObservers()
        setPlayPauseListener()
        setSwipeListener()
        songPager.adapter = swipeSongAdapter
    }

    private fun switchViewPagerToCurrentSong(song: Song) {
        val newItemIndex = swipeSongAdapter.songs.indexOf(song)
        if (newItemIndex != Constants.INVALID_INDEX) {
            songPager.currentItem = newItemIndex
            currentPlayingSong = song
        }
    }

    private fun setPlayPauseListener() {
        ivPlayPause.setOnClickListener {
            currentPlayingSong?.let { mainViewModel.playOrToggleSong(it, true) }
        }
    }

    private fun setSwipeListener() {
        songPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if(playBackState?.isPlaying == true) {
                    mainViewModel.playOrToggleSong(swipeSongAdapter.songs[position])
                } else {
                    currentPlayingSong = swipeSongAdapter.songs[position]
                    currentPlayingSong?.run { glide.load(imageUrl).into(currentSongImage) }
                }
            }
        })
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
            currentPlayingSong?.run { glide.load(imageUrl).into(currentSongImage) }
            switchViewPagerToCurrentSong(currentPlayingSong ?: return@observe)
        }

        mainViewModel.playbackState.observe(this) {
            playBackState = it
            ivPlayPause.setImageResource(if (playBackState?.isPlaying == true) R.drawable.ic_pause_24 else R.drawable.ic_play_24)
        }

        mainViewModel.isConnected.observe(this) {
            it?.getContentIfNotHandled()?.let { result ->
                when(result.status) {
                    Status.ERROR -> Snackbar.make(rootLayout, result.message ?: UNKNOWN_ERROR_OCCURRED, Snackbar.LENGTH_LONG).show()
                    else -> Unit
                }
            }
        }

        mainViewModel.networkError.observe(this) {
            it?.getContentIfNotHandled()?.let { result ->
                when(result.status) {
                    Status.ERROR -> Snackbar.make(rootLayout, result.message ?: UNKNOWN_ERROR_OCCURRED, Snackbar.LENGTH_LONG).show()
                    else -> Unit
                }
            }
        }
    }
}