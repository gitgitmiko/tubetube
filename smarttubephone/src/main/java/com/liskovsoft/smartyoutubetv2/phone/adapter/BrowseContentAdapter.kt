package com.liskovsoft.smartyoutubetv2.phone.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import com.liskovsoft.smartyoutubetv2.phone.R

sealed class BrowseContentItem {
    data class VideoRow(val groupId: Int, var title: String?, var videos: MutableList<Video>) : BrowseContentItem()
    data class VideoGrid(val videos: MutableList<Video>) : BrowseContentItem()
    data class SettingsList(val items: List<SettingsItem>) : BrowseContentItem()
}

class BrowseContentAdapter(
    private val onVideoClick: (Video) -> Unit,
    private val onVideoLongClick: (Video) -> Unit,
    private val onScrollEnd: (Video) -> Unit,
    private val onSettingsClick: (SettingsItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<BrowseContentItem>()

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    fun showSettings(settings: List<SettingsItem>) {
        items.clear()
        items.add(BrowseContentItem.SettingsList(settings))
        notifyDataSetChanged()
    }

    fun applyVideoGroup(group: VideoGroup, asGrid: Boolean) {
        val videos = group.videos ?: emptyList()
        when (group.action) {
            VideoGroup.ACTION_REPLACE -> {
                items.clear()
                if (asGrid) {
                    items.add(BrowseContentItem.VideoGrid(videos.toMutableList()))
                } else {
                    items.add(
                        BrowseContentItem.VideoRow(
                            group.id,
                            group.title,
                            videos.toMutableList()
                        )
                    )
                }
                notifyDataSetChanged()
            }
            VideoGroup.ACTION_PREPEND -> {
                if (asGrid) {
                    val grid = items.filterIsInstance<BrowseContentItem.VideoGrid>().firstOrNull()
                    if (grid != null) {
                        grid.videos.addAll(0, videos)
                        notifyDataSetChanged()
                    } else {
                        items.add(0, BrowseContentItem.VideoGrid(videos.toMutableList()))
                        notifyItemInserted(0)
                    }
                } else {
                    val row = items.filterIsInstance<BrowseContentItem.VideoRow>()
                        .firstOrNull { it.groupId == group.id }
                    if (row != null) {
                        row.videos.addAll(0, videos)
                        notifyDataSetChanged()
                    } else {
                        items.add(
                            0,
                            BrowseContentItem.VideoRow(group.id, group.title, videos.toMutableList())
                        )
                        notifyItemInserted(0)
                    }
                }
            }
            else -> { // APPEND and others
                if (asGrid) {
                    val grid = items.filterIsInstance<BrowseContentItem.VideoGrid>().firstOrNull()
                    if (grid != null) {
                        val start = grid.videos.size
                        grid.videos.addAll(videos)
                        notifyItemChanged(items.indexOf(grid))
                    } else {
                        items.add(BrowseContentItem.VideoGrid(videos.toMutableList()))
                        notifyItemInserted(items.size - 1)
                    }
                } else {
                    val existingIndex = items.indexOfFirst {
                        it is BrowseContentItem.VideoRow && it.groupId == group.id
                    }
                    if (existingIndex >= 0) {
                        val row = items[existingIndex] as BrowseContentItem.VideoRow
                        row.videos.addAll(videos)
                        if (group.title != null) row.title = group.title
                        notifyItemChanged(existingIndex)
                    } else {
                        items.add(
                            BrowseContentItem.VideoRow(
                                group.id,
                                group.title,
                                videos.toMutableList()
                            )
                        )
                        notifyItemInserted(items.size - 1)
                    }
                }
            }
        }
    }

    fun isEmptyContent(): Boolean = items.isEmpty()

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is BrowseContentItem.VideoRow -> TYPE_ROW
        is BrowseContentItem.VideoGrid -> TYPE_GRID
        is BrowseContentItem.SettingsList -> TYPE_SETTINGS
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_ROW -> RowHolder(inflater.inflate(R.layout.item_video_row, parent, false))
            TYPE_GRID -> GridHolder(RecyclerView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                layoutManager = androidx.recyclerview.widget.GridLayoutManager(
                    context,
                    com.liskovsoft.smartyoutubetv2.phone.ui.PhoneUiMetrics.videoGridSpan(context)
                )
                isNestedScrollingEnabled = false
            })
            else -> SettingsHolder(RecyclerView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                layoutManager = LinearLayoutManager(context)
                isNestedScrollingEnabled = false
            })
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is BrowseContentItem.VideoRow -> {
                val rowHolder = holder as RowHolder
                rowHolder.title.text = item.title ?: ""
                rowHolder.title.visibility = if (item.title.isNullOrBlank()) View.GONE else View.VISIBLE
                val adapter = VideoCardAdapter(onVideoClick, onVideoLongClick)
                adapter.submit(item.videos, true)
                rowHolder.list.layoutManager =
                    LinearLayoutManager(rowHolder.list.context, LinearLayoutManager.HORIZONTAL, false)
                rowHolder.list.adapter = adapter
                rowHolder.list.clearOnScrollListeners()
                rowHolder.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                        val last = lm.findLastVisibleItemPosition()
                        if (last >= item.videos.size - 3 && item.videos.isNotEmpty()) {
                            onScrollEnd(item.videos.last())
                        }
                    }
                })
            }
            is BrowseContentItem.VideoGrid -> {
                val gridHolder = holder as GridHolder
                val adapter = VideoCardAdapter(onVideoClick, onVideoLongClick)
                adapter.submit(item.videos, true)
                gridHolder.list.adapter = adapter
                gridHolder.list.clearOnScrollListeners()
                gridHolder.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        val lm = recyclerView.layoutManager as? androidx.recyclerview.widget.GridLayoutManager
                            ?: return
                        val last = lm.findLastVisibleItemPosition()
                        if (last >= item.videos.size - 4 && item.videos.isNotEmpty()) {
                            onScrollEnd(item.videos.last())
                        }
                    }
                })
            }
            is BrowseContentItem.SettingsList -> {
                val settingsHolder = holder as SettingsHolder
                settingsHolder.list.adapter = SettingsItemAdapter(item.items, onSettingsClick)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class RowHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.row_title)
        val list: RecyclerView = view.findViewById(R.id.row_list)
    }

    class GridHolder(val list: RecyclerView) : RecyclerView.ViewHolder(list)

    class SettingsHolder(val list: RecyclerView) : RecyclerView.ViewHolder(list)

    companion object {
        private const val TYPE_ROW = 1
        private const val TYPE_GRID = 2
        private const val TYPE_SETTINGS = 3
    }
}

class SettingsItemAdapter(
    private val items: List<SettingsItem>,
    private val onClick: (SettingsItem) -> Unit
) : RecyclerView.Adapter<SettingsItemAdapter.Holder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_settings, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.settings_title)
    }
}
