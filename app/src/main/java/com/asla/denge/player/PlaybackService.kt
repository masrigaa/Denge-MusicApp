package com.asla.denge.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.asla.denge.R
import org.koin.android.ext.android.inject

/**
 * Foreground service for audio playback.
 * Connects ExoPlayer instance and MediaSession for notification, lock screen controls,
 * and Xiaomi HyperOS Hyper Island media notifications.
 */
class PlaybackService : MediaSessionService() {

    private val playerManager: PlayerManager by inject()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.notification_channel_name)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_music_notification)

        setMediaNotificationProvider(notificationProvider)

        playerManager.mediaSession?.let { session ->
            if (!isSessionAdded(session)) {
                addSession(session)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return playerManager.mediaSession
    }

    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        playerManager.mediaSession?.let { session ->
            if (!isSessionAdded(session)) {
                addSession(session)
            }
            if (intent?.action == ACTION_REFRESH_NOTIFICATION) {
                onUpdateNotification(session, false)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = playerManager.exoPlayer
        if (!player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == androidx.media3.common.Player.STATE_ENDED || player.playbackState == androidx.media3.common.Player.STATE_IDLE) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Kontrol pemutaran musik dan status bar Hyper Island"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "adsfree_music_playback_v2"
        const val ACTION_REFRESH_NOTIFICATION = "com.asla.denge.action.REFRESH_NOTIFICATION"
    }
}
