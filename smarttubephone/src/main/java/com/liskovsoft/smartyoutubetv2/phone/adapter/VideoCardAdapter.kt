package com.liskovsoft.smartyoutubetv2.phone.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.phone.R

class VideoCardAdapter(
    private val onClick: (Video) -> Unit,
    private val onLongClick: ((Video) -> Unit)? = null
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video_card, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val video = items[position]
        holder.title.text = video.title
        holder.subtitle.text = video.secondTitle
        Glide.with(holder.image)
            .load(video.cardImageUrl)
            .centerCrop()
            .into(holder.image)
        holder.itemView.setOnClickListener { onClick(video) }
        holder.itemView.setOnLongClickListener {
            onLongClick?.invoke(video)
            onLongClick != null
        }
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.card_image)
        val title: TextView = view.findViewById(R.id.card_title)
        val subtitle: TextView = view.findViewById(R.id.card_subtitle)
    }
}
