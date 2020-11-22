package com.enkhee.chillplayer.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.enkhee.chillplayer.exoplayer.callbacks.MusicPlaybackPreparer
import com.enkhee.chillplayer.exoplayer.callbacks.MusicPlayerEventListener
import com.enkhee.chillplayer.exoplayer.callbacks.MusicPlayerNotificationListener
import com.enkhee.chillplayer.other.Constants.FIRST_SONG_INDEX
import com.enkhee.chillplayer.other.Constants.MEDIA_ROOT_ID
import com.enkhee.chillplayer.other.Constants.NETWORK_ERROR
import com.enkhee.chillplayer.other.Constants.SONG_START_POSITION
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

private const val SERVICE_TAG = "MUSIC_SERVICE"

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject lateinit var mDataSourceFactory: DefaultDataSourceFactory
    @Inject lateinit var mExoPlayer: SimpleExoPlayer
    @Inject lateinit var mFirebaseMusicSource: FirebaseMusicSource

    private lateinit var mMusicNotificationManger: MusicNotificationManager
    private lateinit var mMusicPlayerEventListener: MusicPlayerEventListener

    private val mServiceJob = Job()
    private val mServiceScope = CoroutineScope(Dispatchers.Main + mServiceJob)

    private lateinit var mMediaSession: MediaSessionCompat
    private lateinit var mMediaSessionConnector: MediaSessionConnector

    var isForegroundService = false

    private var curPlayingSong: MediaMetadataCompat? = null
    private var isPlayerInitialized = false

    companion object {
        var curSongDuration = 0L
            private set
    }

    override fun onCreate() {
        super.onCreate()
        mServiceScope.launch { mFirebaseMusicSource.fetchMediaData() }
        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, 0)
        }

        mMediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }

        sessionToken = mMediaSession.sessionToken

        initMusicNotificationManager()
        val musicPlaybackPreparer = initMusicPlaybackPreparer()

        mMediaSessionConnector = MediaSessionConnector(mMediaSession)
        mMediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer)
        mMediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mMediaSessionConnector.setPlayer(mExoPlayer)
        mMusicPlayerEventListener = MusicPlayerEventListener(this)
        mExoPlayer.addListener(mMusicPlayerEventListener)
        mMusicNotificationManger.showNotification(mExoPlayer)
    }

    private fun initMusicNotificationManager() {
        mMusicNotificationManger = MusicNotificationManager(
            this,
            mMediaSession.sessionToken,
            MusicPlayerNotificationListener(this)
        ) {
            curSongDuration = mExoPlayer.duration
        }
    }

    private fun initMusicPlaybackPreparer(): MusicPlaybackPreparer {
        return MusicPlaybackPreparer(mFirebaseMusicSource) {
            curPlayingSong = it
            preparePlayer(
                mFirebaseMusicSource.songs,
                it,
                true
            )
        }
    }

    private fun preparePlayer(
        songs: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playNow: Boolean
    ) {
        val currSongIndex =
            if (curPlayingSong == null) FIRST_SONG_INDEX else songs.indexOf(itemToPlay)
        mExoPlayer.prepare(mFirebaseMusicSource.asMediaSource(mDataSourceFactory))
        mExoPlayer.seekTo(currSongIndex, SONG_START_POSITION)
        mExoPlayer.playWhenReady = playNow
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? = BrowserRoot(MEDIA_ROOT_ID, null)

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when (parentId) {
            MEDIA_ROOT_ID -> {
                val resultsSent = mFirebaseMusicSource.whenReady { isInitialized ->
                    if (isInitialized) {
                        result.sendResult(mFirebaseMusicSource.asMediaItems().toMutableList())
                        if (!isPlayerInitialized && mFirebaseMusicSource.isMusicSourceNotEmpty()) {
                            val songs = mFirebaseMusicSource.songs
                            mServiceScope.launch {
                                withContext(Dispatchers.Main) {
                                    preparePlayer(songs, songs[FIRST_SONG_INDEX], false)
                                }
                            }
                            isPlayerInitialized = true
                        }
                    } else {
                        mMediaSession.sendSessionEvent(NETWORK_ERROR, null)
                        result.sendResult(null)
                    }
                }

                if (!resultsSent) {
                    result.detach()
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mExoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mServiceScope.cancel()
        mExoPlayer.removeListener(mMusicPlayerEventListener)
        mExoPlayer.release()
    }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mMediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return mFirebaseMusicSource.songs[windowIndex].description
        }
    }
}