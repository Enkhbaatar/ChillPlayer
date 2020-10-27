package com.enkhee.chillplayer.exoplayer

import android.app.PendingIntent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject

private const val SERVICE_TAG = "MUSIC_SERVICE"

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject lateinit var mDataSourceFactory: DefaultDataSourceFactory
    @Inject lateinit var mExoPlayer: SimpleExoPlayer

    private val mServiceJob = Job()
    private val mServiceScope = CoroutineScope(Dispatchers.Main + mServiceJob)

    private lateinit var mMediaSession: MediaSessionCompat
    private lateinit var mMediaSessionConnector: MediaSessionConnector

    override fun onCreate() {
        super.onCreate()
        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let{
            PendingIntent.getActivity(this, 0, it, 0)
        }

        mMediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }

        sessionToken = mMediaSession.sessionToken
        mMediaSessionConnector = MediaSessionConnector(mMediaSession)
        mMediaSessionConnector.setPlayer(mExoPlayer)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        TODO("Not yet implemented")
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        mServiceScope.cancel()
    }
}