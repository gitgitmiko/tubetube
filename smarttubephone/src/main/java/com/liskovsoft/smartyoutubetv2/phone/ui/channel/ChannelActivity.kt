package com.liskovsoft.smartyoutubetv2.phone.ui.channel

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import com.liskovsoft.smartyoutubetv2.common.app.presenters.ChannelPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelView
import com.liskovsoft.smartyoutubetv2.phone.R
import com.liskovsoft.smartyoutubetv2.phone.adapter.ChipAdapter
import com.liskovsoft.smartyoutubetv2.phone.adapter.PhoneChip
import com.liskovsoft.smartyoutubetv2.phone.adapter.VideoCardAdapter
import com.liskovsoft.smartyoutubetv2.phone.ui.PhoneBaseActivity

class ChannelActivity : PhoneBaseActivity(), ChannelView {
    private lateinit var presenter: ChannelPresenter
    private lateinit var progressBar: ProgressBar
    private lateinit var contentList: RecyclerView
    private lateinit var adapter: VideoCardAdapter
    private lateinit var toolbar: MaterialToolbar
    private lateinit var chipAdapter: ChipAdapter
    private val groups = linkedMapOf<String, MutableList<com.liskovsoft.smartyoutubetv2.common.app.models.data.Video>>()
    private var selectedTab: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel)

        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        progressBar = findViewById(R.id.progress_bar)
        contentList = findViewById(R.id.content_list)

        adapter = VideoCardAdapter(
            onClick = { presenter.onVideoItemClicked(it) },
            onLongClick = { presenter.onVideoItemLongClicked(it) }
        )
        contentList.layoutManager = LinearLayoutManager(this)
        contentList.adapter = adapter
        com.liskovsoft.smartyoutubetv2.phone.ui.PhoneUiMetrics.applyCenteredMaxWidth(contentList)
        contentList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lm = recyclerView.layoutManager as LinearLayoutManager
                val last = lm.findLastVisibleItemPosition()
                val items = adapter.items()
                if (items.isNotEmpty() && last >= items.size - 4) {
                    presenter.onScrollEnd(items.last())
                }
            }
        })

        chipAdapter = ChipAdapter { chip ->
            selectedTab = chip.title
            chipAdapter.select(chip.id)
            renderTab()
        }
        findViewById<RecyclerView>(R.id.chip_list).apply {
            layoutManager = LinearLayoutManager(this@ChannelActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = chipAdapter
        }

        presenter = ChannelPresenter.instance(this)
        presenter.setView(this)
        presenter.onViewInitialized()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onViewDestroyed()
    }

    override fun update(videoGroup: VideoGroup?) {
        if (videoGroup == null) return
        if (!videoGroup.title.isNullOrBlank()) {
            toolbar.title = videoGroup.title
        }
        val key = videoGroup.title?.takeIf { it.isNotBlank() } ?: getString(R.string.nav_home)
        val replace = videoGroup.action == VideoGroup.ACTION_REPLACE
        if (replace) {
            groups[key] = (videoGroup.videos ?: emptyList()).toMutableList()
        } else {
            groups.getOrPut(key) { mutableListOf() }.addAll(videoGroup.videos ?: emptyList())
        }
        if (selectedTab == null) selectedTab = key
        val chips = groups.keys.mapIndexed { index, title -> PhoneChip(index, title, index) }
        val selectedId = groups.keys.indexOf(selectedTab).coerceAtLeast(0)
        chipAdapter.submit(chips, selectedId)
        renderTab()
    }

    private fun renderTab() {
        val videos = groups[selectedTab] ?: groups.values.firstOrNull() ?: emptyList()
        adapter.submit(videos, true)
    }

    override fun setPosition(index: Int) {
        if (index >= 0) {
            contentList.scrollToPosition(index)
        }
    }

    override fun showProgressBar(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun clear() {
        groups.clear()
        selectedTab = null
        adapter.clear()
    }
}
