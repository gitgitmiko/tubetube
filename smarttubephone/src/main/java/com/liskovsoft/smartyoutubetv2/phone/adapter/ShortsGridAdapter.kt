package com.liskovsoft.smartyoutubetv2.phone.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import com.liskovsoft.smartyoutubetv2.phone.R

/**
 * Flat 2-column Shorts grid — avoids nested RecyclerView that caused ANR while scrolling.
 */
class ShortsGridAdapter(
    private val onClick: (Video, Int) -> Unit,
    private val onLongClick: ((Video) -> Unit)? = null,
    private val onNearEnd: () -> Unit
) : RecyclerView.Adapter<ShortsGridAdapter.Holder>() {
    private val items = mutableListOf<Video>()
    private var lastNearEndAt = 0L

    fun applyGroup(group: VideoGroup) {
        val videos = group.videos ?: emptyList()
        when (group.action) {
            VideoGroup.ACTION_REPLACE -> {
                items.clear()
                items.addAll(videos)
                notifyDataSetChanged()
            }
            VideoGroup.ACTION_PREPEND -> {
                items.addAll(0, videos)
                notifyItemRangeInserted(0, videos.size)
            }
            else -> {
                val start = items.size
                val existing = items.mapNotNull { it.videoId }.toHashSet()
                val fresh = videos.filter { it.videoId == null || it.videoId !in existing }
                if (fresh.isEmpty()) return
                items.addAll(fresh)
                notifyItemRangeInserted(start, fresh.size)
            }
        }
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    fun items(): List<Video> = items.toList()

    fun isEmpty(): Boolean = items.isEmpty()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shorts_card, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val video = items[position]
        holder.title.text = video.title ?: ""
        Glide.with(holder.image.context)
            .load(video.cardImageUrl)
            .centerCrop()
            .into(holder.image)
        holder.itemView.setOnClickListener { onClick(video, holder.adapterPosition.coerceAtLeast(0)) }
        holder.itemView.setOnLongClickListener {
            onLongClick?.invoke(video)
            onLongClick != null
        }
        if (position >= items.size - 4) {
            val now = System.currentTimeMillis()
            if (now - lastNearEndAt > 1_200) {
                lastNearEndAt = now
                onNearEnd()
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.shorts_card_image)
        val title: TextView = view.findViewById(R.id.shorts_card_title)
    }
}
