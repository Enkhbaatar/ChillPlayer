package com.enkhee.chillplayer.ui.fragments

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.RequestManager
import com.enkhee.chillplayer.R
import com.enkhee.chillplayer.data.entities.Song
import com.enkhee.chillplayer.exoplayer.isPlaying
import com.enkhee.chillplayer.exoplayer.toSong
import com.enkhee.chillplayer.other.Constants
import com.enkhee.chillplayer.other.Status
import com.enkhee.chillplayer.ui.viewmodels.MainViewModel
import com.enkhee.chillplayer.ui.viewmodels.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_song.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SongFragment : Fragment(R.layout.fragment_song) {

    @Inject
    lateinit var glide: RequestManager

    private lateinit var mainViewModel: MainViewModel
    private val songViewModel: SongViewModel by viewModels()

    private var curPlayingSong: Song? = null
    private var playbackState:PlaybackStateCompat? = null
    private var shouldUpdateSeekBar = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        subscribeToObservers()
        setupClickListeners()
        setupSeekBarChangeListener()
    }

    private fun setupClickListeners() {
        ivPlayPauseDetail.setOnClickListener {
            curPlayingSong?.let {
                mainViewModel.playOrToggleSong(it, true)
            }
        }

        ivSkipPrevious.setOnClickListener { mainViewModel.skipToPreviousSong() }
        ivSkip.setOnClickListener { mainViewModel.skipToNextSong() }
    }

    private fun setupSeekBarChangeListener() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser) {
                    setCurPlayerTimeToTextView(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                shouldUpdateSeekBar = false
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                seekBar?.let {
                    mainViewModel.seekTo(it.progress.toLong())
                    shouldUpdateSeekBar = true
                }
            }
        })
    }


    private fun updateTitleAndSongImage(song: Song) {
        val title = "${song.title} - ${song.subtitle}"
        tvSongName.text = title
        glide.load(song.imageUrl).into(ivSongImage)
    }

    private fun subscribeToObservers() {
        mainViewModel.mediaItems.observe(viewLifecycleOwner) {
            it?.let { result ->
                when (result.status) {
                    Status.SUCCESS -> {
                        result.data?.let { songs ->
                            if (curPlayingSong == null && songs.isNotEmpty()) {
                                curPlayingSong = songs[Constants.FIRST_SONG_INDEX]
                                updateTitleAndSongImage(songs[Constants.FIRST_SONG_INDEX])
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }

        mainViewModel.curPlayingSong.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            curPlayingSong = it.toSong()
            curPlayingSong?.let { song -> updateTitleAndSongImage(song) }
        }

        mainViewModel.playbackState.observe(viewLifecycleOwner) {
            playbackState = it
            ivPlayPauseDetail.setImageResource(
                if (playbackState?.isPlaying == true) R.drawable.ic_pause_24 else R.drawable.ic_play_24
            )
            seekBar.progress = it?.position?.toInt() ?: 0
        }

        songViewModel.curPlayerPosition.observe(viewLifecycleOwner) {
            if (shouldUpdateSeekBar) {
                seekBar.progress = it.toInt()
                setCurPlayerTimeToTextView(it)
            }
        }

        songViewModel.curSongDuration.observe(viewLifecycleOwner) {
            seekBar.max = it.toInt()
            val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
            tvSongDuration.text = dateFormat.format(it)
        }
    }

    private fun setCurPlayerTimeToTextView(ms: Long) {
        val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
        tvCurTime.text = dateFormat.format(ms)
    }
}