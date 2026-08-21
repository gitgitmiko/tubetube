package com.liskovsoft.smartyoutubetv2.phone.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.phone.ui.browse.BrowseActivity
import com.liskovsoft.smartyoutubetv2.phone.ui.playback.PlaybackActivity

/**
 * Lets Browse show a mini player while PlaybackActivity stays alive (engine blocked)
 * so ExoPlayerController keeps playing.
 */
object PhonePlaybackBridge {
    interface Host {
        fun togglePlayPause()
        fun isPlaying(): Boolean
        fun expandFromMiniPlayer()
        fun closeFromMiniPlayer()
        fun currentVideo(): Video?
    }

    interface Listener {
        fun onMiniPlayerChanged()
    }

    @Volatile
    var host: Host? = null
        private set

    @Volatile
    var minimized: Boolean = false
        private set

    private val listeners = mutableListOf<Listener>()
    private var appContext: Context? = null

    fun attach(host: Host, context: Context) {
        appContext = context.applicationContext
        this.host = host
        notifyListeners()
    }

    fun detach(host: Host) {
        if (this.host === host) {
            this.host = null
            minimized = false
            notifyListeners()
        }
    }

    fun setMinimized(value: Boolean) {
        if (minimized == value) return
        minimized = value
        notifyListeners()
    }

    fun notifyChanged() {
        notifyListeners()
    }

    fun openBrowseKeepingPlayer(activity: Activity) {
        val intent = Intent(activity, BrowseActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        activity.startActivity(intent)
    }

    fun openFullPlayer(activity: Activity) {
        val intent = Intent(activity, PlaybackActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        activity.startActivity(intent)
    }

    fun addListener(listener: Listener) {
        if (!listeners.contains(listener)) listeners.add(listener)
        listener.onMiniPlayerChanged()
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun isVisible(): Boolean = minimized && host?.currentVideo() != null

    private fun notifyListeners() {
        listeners.toList().forEach { it.onMiniPlayerChanged() }
        syncService()
    }

    private fun syncService() {
        val context = appContext ?: return
        if (isVisible()) {
            PhonePlaybackService.start(context)
        } else {
            PhonePlaybackService.stop(context)
        }
    }
}
