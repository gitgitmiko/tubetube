package com.liskovsoft.smartyoutubetv2.phone.shorts

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video

/**
 * In-memory playlist for Shorts vertical feed (avoids huge Intent extras).
 */
object ShortsFeedSession {
    private val lock = Any()
    private val videos = mutableListOf<Video>()
    private val listeners = mutableListOf<() -> Unit>()
    @Volatile var startIndex: Int = 0
        private set
    @Volatile var loadingMore: Boolean = false

    fun set(items: List<Video>, index: Int) {
        synchronized(lock) {
            videos.clear()
            videos.addAll(items.distinctBy { it.videoId })
            startIndex = index.coerceIn(0, (videos.size - 1).coerceAtLeast(0))
        }
        notifyChanged()
    }

    fun append(items: List<Video>) {
        if (items.isEmpty()) return
        synchronized(lock) {
            val existing = videos.mapNotNull { it.videoId }.toHashSet()
            val fresh = items.filter { it.videoId != null && it.videoId !in existing }
            if (fresh.isEmpty()) return
            videos.addAll(fresh)
        }
        loadingMore = false
        notifyChanged()
    }

    fun replace(items: List<Video>) {
        synchronized(lock) {
            videos.clear()
            videos.addAll(items.distinctBy { it.videoId })
            startIndex = startIndex.coerceIn(0, (videos.size - 1).coerceAtLeast(0))
        }
        loadingMore = false
        notifyChanged()
    }

    fun snapshot(): List<Video> = synchronized(lock) { videos.toList() }

    fun size(): Int = synchronized(lock) { videos.size }

    fun getOrNull(index: Int): Video? = synchronized(lock) { videos.getOrNull(index) }

    fun addListener(listener: () -> Unit) {
        synchronized(lock) { listeners.add(listener) }
    }

    fun removeListener(listener: () -> Unit) {
        synchronized(lock) { listeners.remove(listener) }
    }

    private fun notifyChanged() {
        val copy = synchronized(lock) { listeners.toList() }
        copy.forEach { it.invoke() }
    }
}
