package com.liskovsoft.smartyoutubetv2.phone.ui.downloads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter
import com.liskovsoft.smartyoutubetv2.phone.R
import com.liskovsoft.smartyoutubetv2.phone.downloads.PhoneDownloadItem
import com.liskovsoft.smartyoutubetv2.phone.downloads.PhoneDownloadStore
import com.liskovsoft.smartyoutubetv2.phone.ui.PhoneBaseActivity

class DownloadsActivity : PhoneBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        val items = PhoneDownloadStore.list(this)
        val list = findViewById<RecyclerView>(R.id.downloads_list)
        val empty = findViewById<TextView>(R.id.downloads_empty)
        if (items.isEmpty()) {
            list.visibility = View.GONE
            empty.visibility = View.VISIBLE
            return
        }
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = DownloadsAdapter(items) { item ->
            if (item.videoId.isNotBlank()) {
                val video = Video()
                video.videoId = item.videoId
                video.title = item.title
                PlaybackPresenter.instance(this).openVideo(video)
            }
        }
    }
}

private class DownloadsAdapter(
    private val items: List<PhoneDownloadItem>,
    private val onClick: (PhoneDownloadItem) -> Unit
) : RecyclerView.Adapter<DownloadsAdapter.Holder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_settings, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.title.text = listOf(item.title, item.quality).filter { it.isNotBlank() }.joinToString("\n")
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.settings_title)
    }
}
