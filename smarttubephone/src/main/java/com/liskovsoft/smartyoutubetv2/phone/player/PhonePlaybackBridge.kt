package com.liskovsoft.smartyoutubetv2.phone.player

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video

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

    fun attach(host: Host) {
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
    }
}
