package com.liskovsoft.smartyoutubetv2.phone.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.liskovsoft.smartyoutubetv2.phone.R
import com.liskovsoft.smartyoutubetv2.phone.ui.playback.PlaybackActivity

/**
 * Keeps playback in the status bar / notification shade while the mini player is active.
 */
class PhonePlaybackService : Service(), PhonePlaybackBridge.Listener {
    private var mediaSession: MediaSessionCompat? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        mediaSession = MediaSessionCompat(this, "PhonePlayback").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    toggleIfNeeded(wantPlaying = true)
                }

                override fun onPause() {
                    toggleIfNeeded(wantPlaying = false)
                }

                override fun onStop() {
                    PhonePlaybackBridge.host?.closeFromMiniPlayer()
                }
            })
            isActive = true
        }
        startInForeground()
        PhonePlaybackBridge.addListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> PhonePlaybackBridge.host?.togglePlayPause()
            ACTION_STOP -> PhonePlaybackBridge.host?.closeFromMiniPlayer()
            ACTION_OPEN -> PhonePlaybackBridge.host?.expandFromMiniPlayer()
        }
        if (!PhonePlaybackBridge.isVisible()) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground()
        return START_STICKY
    }

    override fun onDestroy() {
        PhonePlaybackBridge.removeListener(this)
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onMiniPlayerChanged() {
        if (!PhonePlaybackBridge.isVisible()) {
            stopSelf()
            return
        }
        startInForeground()
    }

    private fun toggleIfNeeded(wantPlaying: Boolean) {
        val playing = PhonePlaybackBridge.host?.isPlaying() == true
        if (playing != wantPlaying) {
            PhonePlaybackBridge.host?.togglePlayPause()
        }
    }

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val video = PhonePlaybackBridge.host?.currentVideo()
        val playing = PhonePlaybackBridge.host?.isPlaying() == true
        val session = mediaSession
        session?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP
                )
                .setState(
                    if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    1f
                )
                .build()
        )

        val playPauseIcon = if (playing) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseText = getString(R.string.mini_player_play)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_play)
            .setContentTitle(video?.title ?: getString(R.string.app_name))
            .setContentText(video?.getAuthor() ?: getString(R.string.mini_player))
            .setContentIntent(activityIntent(ACTION_OPEN))
            .setDeleteIntent(serviceIntent(ACTION_STOP))
            .addAction(playPauseIcon, playPauseText, serviceIntent(ACTION_TOGGLE))
            .addAction(R.drawable.ic_close, getString(R.string.mini_player_close), serviceIntent(ACTION_STOP))
            .setOngoing(playing)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        if (session != null) {
            builder.setStyle(
                MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1)
            )
        }
        return builder.build()
    }

    private fun serviceIntent(action: String): PendingIntent {
        val intent = Intent(this, PhonePlaybackService::class.java).setAction(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getService(this, action.hashCode(), intent, flags)
    }

    private fun activityIntent(action: String): PendingIntent {
        val intent = Intent(this, PlaybackActivity::class.java)
            .setAction(action)
            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(this, 0, intent, flags)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.mini_player),
            NotificationManager.IMPORTANCE_LOW
        )
        channel.setShowBadge(false)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_TOGGLE = "com.liskovsoft.smartyoutubetv2.phone.action.TOGGLE"
        const val ACTION_STOP = "com.liskovsoft.smartyoutubetv2.phone.action.STOP"
        const val ACTION_OPEN = "com.liskovsoft.smartyoutubetv2.phone.action.OPEN"
        private const val CHANNEL_ID = "phone_playback"
        private const val NOTIFICATION_ID = 1008

        fun start(context: Context) {
            val intent = Intent(context, PhonePlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PhonePlaybackService::class.java))
        }
    }
}
