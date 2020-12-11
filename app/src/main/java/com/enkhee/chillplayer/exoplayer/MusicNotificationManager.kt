package com.enkhee.chillplayer.exoplayer

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.enkhee.chillplayer.R
import com.enkhee.chillplayer.other.Constants.NOTIFICATION_CHANEL_ID
import com.enkhee.chillplayer.other.Constants.NOTIFICATION_ID
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager

class MusicNotificationManager(
        private val mContext: Context,
        mSessionToken: MediaSessionCompat.Token,
        mNotificationListener: PlayerNotificationManager.NotificationListener,
        private val newSongCallback: () -> Unit
) {
    private val mNotificationManager: PlayerNotificationManager

    init {
        val mediaController = MediaControllerCompat(mContext, mSessionToken)
        mNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                mContext,
                NOTIFICATION_CHANEL_ID,
                R.string.notification_channel_name,
                R.string.notification_channel_descriptoin,
                NOTIFICATION_ID,
                DescriptionAdapter(mediaController),
                mNotificationListener
        ).apply {
            setSmallIcon(R.drawable.ic_launcher_background)
            setMediaSessionToken(mSessionToken)
        }
    }

    fun showNotification(player: Player) {
        mNotificationManager.setPlayer(player)
    }

    private inner class DescriptionAdapter(
            private val mMediaController: MediaControllerCompat
    ) : PlayerNotificationManager.MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player): CharSequence {
            newSongCallback()
            return mMediaController.metadata.description.title.toString()
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? = mMediaController.sessionActivity

        override fun getCurrentContentText(player: Player): CharSequence? = mMediaController.metadata.description.subtitle.toString()

        override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
            Glide.with(mContext).asBitmap()
                    .load(mMediaController.metadata.description.iconUri)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            callback.onBitmap(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) = Unit
                    })
            return null
        }

    }
}