package com.liskovsoft.smartyoutubetv2.phone.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import com.liskovsoft.smartyoutubetv2.phone.R

sealed class BrowseContentItem {
    data class Header(val title: String) : BrowseContentItem()
    data class VideoItem(val video: Video) : BrowseContentItem()
    data class SettingsItemRow(val item: SettingsItem) : BrowseContentItem()
}

class BrowseContentAdapter(
    private val onVideoClick: (Video) -> Unit,
    private val onVideoLongClick: (Video) -> Unit,
    private val onScrollEnd: (Video) -> Unit,
    private val onSettingsClick: (SettingsItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private data class RowBucket(var title: String?, val videos: MutableList<Video>)

    private val rows = linkedMapOf<Int, RowBucket>()
    private val gridVideos = mutableListOf<Video>()
    private var settings = emptyList<SettingsItem>()
    private val items = mutableListOf<BrowseContentItem>()
    private var asGrid = false
    private var asSettings = false
    private var lastScrollAt = 0L

    fun clear() {
        rows.clear()
        gridVideos.clear()
        settings = emptyList()
        asGrid = false
        asSettings = false
        items.clear()
        notifyDataSetChanged()
    }

    fun showSettings(values: List<SettingsItem>) {
        rows.clear()
        gridVideos.clear()
        settings = values
        asSettings = true
        asGrid = false
        rebuild()
    }

    fun applyVideoGroup(group: VideoGroup, grid: Boolean) {
        asSettings = false
        asGrid = grid
        settings = emptyList()
        val videos = group.videos ?: emptyList()
        when (group.action) {
            VideoGroup.ACTION_REPLACE -> {
                rows.clear()
                gridVideos.clear()
                if (grid) {
                    gridVideos.addAll(videos)
                } else {
                    rows[group.id] = RowBucket(group.title, videos.toMutableList())
                }
            }
            VideoGroup.ACTION_PREPEND -> {
                if (grid) {
                    gridVideos.addAll(0, videos)
                } else {
                    val bucket = rows[group.id]
                    if (bucket != null) {
                        bucket.videos.addAll(0, videos)
                        if (group.title != null) bucket.title = group.title
                    } else {
                        val rebuilt = linkedMapOf<Int, RowBucket>()
                        rebuilt[group.id] = RowBucket(group.title, videos.toMutableList())
                        rebuilt.putAll(rows)
                        rows.clear()
                        rows.putAll(rebuilt)
                    }
                }
            }
            else -> {
                if (grid) {
                    gridVideos.addAll(videos)
                } else {
                    val bucket = rows[group.id]
                    if (bucket != null) {
                        bucket.videos.addAll(videos)
                        if (group.title != null) bucket.title = group.title
                    } else {
                        rows[group.id] = RowBucket(group.title, videos.toMutableList())
                    }
                }
            }
        }
        rebuild()
    }

    fun isEmptyContent(): Boolean = items.isEmpty()

    private fun rebuild() {
        items.clear()
        when {
            asSettings -> settings.forEach { items.add(BrowseContentItem.SettingsItemRow(it)) }
            asGrid -> gridVideos.forEach { items.add(BrowseContentItem.VideoItem(it)) }
            else -> {
                rows.values.forEach { bucket ->
                    if (bucket.videos.isEmpty()) return@forEach
                    if (!bucket.title.isNullOrBlank()) {
                        items.add(BrowseContentItem.Header(bucket.title!!))
                    }
                    bucket.videos.forEach { items.add(BrowseContentItem.VideoItem(it)) }
                }
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is BrowseContentItem.Header -> TYPE_HEADER
        is BrowseContentItem.VideoItem -> TYPE_VIDEO
        is BrowseContentItem.SettingsItemRow -> TYPE_SETTINGS
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderHolder(inflater.inflate(R.layout.item_row_header, parent, false))
            TYPE_VIDEO -> {
                val view = inflater.inflate(R.layout.item_video_card, parent, false)
                VideoCardAdapter.Holder(view)
            }
            else -> SettingsHolder(inflater.inflate(R.layout.item_settings, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is BrowseContentItem.Header -> {
                (holder as HeaderHolder).title.text = item.title
            }
            is BrowseContentItem.VideoItem -> {
                VideoCardBinder.bind(
                    holder as VideoCardAdapter.Holder,
                    item.video,
                    onVideoClick,
                    onVideoLongClick
                )
                maybeRequestMore(position, item.video)
            }
            is BrowseContentItem.SettingsItemRow -> {
                val settingsHolder = holder as SettingsHolder
                settingsHolder.title.text = item.item.title
                settingsHolder.itemView.setOnClickListener { onSettingsClick(item.item) }
            }
        }
    }

    private fun maybeRequestMore(position: Int, video: Video) {
        if (position < items.size - 4) return
        val now = System.currentTimeMillis()
        if (now - lastScrollAt < 700) return
        lastScrollAt = now
        onScrollEnd(video)
    }

    override fun getItemCount(): Int = items.size

    class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.row_title)
    }

    class SettingsHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.settings_title)
    }

    companion object {
        private const val TYPE_HEADER = 1
        private const val TYPE_VIDEO = 2
        private const val TYPE_SETTINGS = 3
    }
}
