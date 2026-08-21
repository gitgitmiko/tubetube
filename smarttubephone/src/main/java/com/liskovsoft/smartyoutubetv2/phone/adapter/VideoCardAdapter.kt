package com.liskovsoft.smartyoutubetv2.phone.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.phone.R

class VideoCardAdapter(
    private val onClick: (Video) -> Unit,
    private val onLongClick: ((Video) -> Unit)? = null,
    private val compact: Boolean = false
) : RecyclerView.Adapter<VideoCardAdapter.Holder>() {
    private val items = mutableListOf<Video>()

    fun submit(videos: List<Video>?, replace: Boolean) {
        if (replace) {
            items.clear()
        }
        if (videos != null) {
            items.addAll(videos)
        }
        notifyDataSetChanged()
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    fun items(): List<Video> = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val layout = if (compact) R.layout.item_video_card_compact else R.layout.item_video_card
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        VideoCardBinder.bind(holder, items[position], onClick, onLongClick)
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.card_image)
        val title: TextView = view.findViewById(R.id.card_title)
        val subtitle: TextView = view.findViewById(R.id.card_subtitle)
        val duration: TextView? = view.findViewById(R.id.card_duration)
        val avatar: TextView? = view.findViewById(R.id.card_avatar)
        val more: ImageButton? = view.findViewById(R.id.card_more)
    }
}

object VideoCardBinder {
    fun bind(
        holder: VideoCardAdapter.Holder,
        video: Video,
        onClick: (Video) -> Unit,
        onLongClick: ((Video) -> Unit)?
    ) {
        holder.title.text = video.title
        val subtitle = video.getAuthor()?.takeIf { it.isNotBlank() }
            ?: video.secondTitle?.toString().orEmpty()
        holder.subtitle.text = subtitle
        Glide.with(holder.image)
            .load(video.cardImageUrl)
            .centerCrop()
            .into(holder.image)

        val durationText = durationLabel(video)
        holder.duration?.let { view ->
            if (durationText.isNullOrBlank()) {
                view.visibility = View.GONE
            } else {
                view.visibility = View.VISIBLE
                view.text = durationText
            }
        }

        holder.avatar?.let { avatar ->
            val letter = (video.getAuthor() ?: video.title ?: "?").trim()
                .firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: '?'
            avatar.text = letter.toString()
        }

        holder.itemView.setOnClickListener { onClick(video) }
        holder.itemView.setOnLongClickListener {
            onLongClick?.invoke(video)
            onLongClick != null
        }
        holder.more?.setOnClickListener { onLongClick?.invoke(video) }
    }

    fun durationLabel(video: Video): String? {
        if (!video.badge.isNullOrBlank() && video.getDurationMs() <= 0) {
            return video.badge
        }
        val ms = video.getDurationMs()
        if (ms <= 0) return video.badge
        val total = (ms / 1000L).toInt()
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val seconds = total % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }
}
