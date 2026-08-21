package com.liskovsoft.smartyoutubetv2.phone.navigation

/**
 * Centralized phone navigation identifiers. Keep route strings here rather than
 * scattering them across activities.
 */
object PhoneRoutes {
    const val HOME = "home"
    const val SHORTS = "shorts"
    const val SEARCH = "search"
    const val SUBSCRIPTIONS = "subscriptions"
    const val LIBRARY = "library"
    const val HISTORY = "history"
    const val WATCH_LATER = "watch-later"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"
    const val VIDEO = "video"
    const val CHANNEL = "channel"
    const val PLAYLIST = "playlist"

    fun video(videoId: String): String = "$VIDEO/$videoId"
    fun channel(channelId: String): String = "$CHANNEL/$channelId"
    fun playlist(playlistId: String): String = "$PLAYLIST/$playlistId"
    fun search(query: String): String = "$SEARCH/$query"
}

enum class PhoneTab {
    HOME,
    SHORTS,
    SEARCH,
    SUBSCRIPTIONS,
    LIBRARY
}
