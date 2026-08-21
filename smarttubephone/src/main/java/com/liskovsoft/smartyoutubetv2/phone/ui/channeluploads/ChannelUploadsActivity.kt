package com.liskovsoft.smartyoutubetv2.phone.ui.channeluploads

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import com.liskovsoft.smartyoutubetv2.common.app.presenters.ChannelUploadsPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelUploadsView
import com.liskovsoft.smartyoutubetv2.phone.R
import com.liskovsoft.smartyoutubetv2.phone.adapter.VideoCardAdapter
import com.liskovsoft.smartyoutubetv2.phone.ui.PhoneBaseActivity

class ChannelUploadsActivity : PhoneBaseActivity(), ChannelUploadsView {
    private lateinit var presenter: ChannelUploadsPresenter
    private lateinit var progressBar: ProgressBar
    private lateinit var contentList: RecyclerView
    private lateinit var adapter: VideoCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel)

        findViewById<MaterialToolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { finish() }
            title = getString(R.string.app_name)
        }
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

        presenter = ChannelUploadsPresenter.instance(this)
        presenter.setView(this)
        presenter.onViewInitialized()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onViewDestroyed()
    }

    override fun update(videoGroup: VideoGroup?) {
        if (videoGroup == null) return
        val replace = videoGroup.action == VideoGroup.ACTION_REPLACE
        adapter.submit(videoGroup.videos, replace)
    }

    override fun showProgressBar(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun clear() {
        adapter.clear()
    }
}
