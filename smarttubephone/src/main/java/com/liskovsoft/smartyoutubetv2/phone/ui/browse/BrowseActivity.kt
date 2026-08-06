package com.liskovsoft.smartyoutubetv2.phone.ui.browse

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.ErrorFragmentData
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView
import com.liskovsoft.smartyoutubetv2.phone.R
import com.liskovsoft.smartyoutubetv2.phone.adapter.BrowseContentAdapter
import com.liskovsoft.smartyoutubetv2.phone.adapter.SectionAdapter
import com.liskovsoft.smartyoutubetv2.phone.adapter.ShortsGridAdapter
import com.liskovsoft.smartyoutubetv2.phone.shorts.ShortsFeedActivity
import com.liskovsoft.smartyoutubetv2.phone.shorts.ShortsFeedSession
import com.liskovsoft.smartyoutubetv2.phone.ui.PhoneBaseActivity
import com.liskovsoft.smartyoutubetv2.phone.ui.PhoneUiMetrics
import android.content.res.Configuration

class BrowseActivity : PhoneBaseActivity(), BrowseView {
    private lateinit var presenter: BrowsePresenter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var contentList: RecyclerView
    private lateinit var sectionList: RecyclerView

    private lateinit var sectionAdapter: SectionAdapter
    private lateinit var contentAdapter: BrowseContentAdapter
    private lateinit var shortsAdapter: ShortsGridAdapter

    private val sections = LinkedHashMap<Int, BrowseSection>()
    private var currentSection: BrowseSection? = null
    private var progressVisible = false
    private var shortsMode = false
    private var lastScrollEndAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)

        drawerLayout = findViewById(R.id.drawer_layout)
        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progress_bar)
        errorText = findViewById(R.id.error_text)
        contentList = findViewById(R.id.content_list)
        sectionList = findViewById(R.id.section_list)

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.sections, R.string.sections)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        findViewById<ImageButton>(R.id.btn_search).setOnClickListener {
            SearchPresenter.instance(this).startSearch(null)
        }

        sectionAdapter = SectionAdapter { section, index ->
            sectionAdapter.select(index)
            currentSection = section
            toolbar.title = section.title
            switchContentMode(section.type == BrowseSection.TYPE_SHORTS_GRID)
            contentAdapter.clear()
            shortsAdapter.clear()
            drawerLayout.closeDrawer(GravityCompat.START)
            presenter.onSectionFocused(section.id)
        }
        sectionList.layoutManager = LinearLayoutManager(this)
        sectionList.adapter = sectionAdapter

        contentAdapter = BrowseContentAdapter(
            onVideoClick = { openVideo(it) },
            onVideoLongClick = { presenter.onVideoItemLongClicked(it) },
            onScrollEnd = { requestMore(it) },
            onSettingsClick = { it.onClick?.run() }
        )
        shortsAdapter = ShortsGridAdapter(
            onClick = { video, index -> openShortsFeed(index) },
            onLongClick = { presenter.onVideoItemLongClicked(it) },
            onNearEnd = {
                val last = shortsAdapter.items().lastOrNull() ?: return@ShortsGridAdapter
                requestMore(last)
            }
        )
        contentList.layoutManager = LinearLayoutManager(this)
        contentList.adapter = contentAdapter
        contentList.itemAnimator = null
        PhoneUiMetrics.applyCenteredMaxWidth(contentList)

        presenter = BrowsePresenter.instance(this)
        presenter.setView(this)
        presenter.onViewInitialized()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        PhoneUiMetrics.applyCenteredMaxWidth(contentList)
        // Refresh span counts when rotating tablet / folding.
        if (shortsMode) {
            (contentList.layoutManager as? GridLayoutManager)?.spanCount =
                PhoneUiMetrics.shortsGridSpan(this)
            val pad = PhoneUiMetrics.contentHorizontalPadding(this)
            contentList.setPadding(pad, pad, pad, pad * 2)
        }
    }

    private fun openVideo(video: Video) {
        if (isShortsSection()) {
            val index = shortsAdapter.items().indexOfFirst { it.videoId == video.videoId }
                .takeIf { it >= 0 } ?: 0
            openShortsFeed(index)
        } else {
            presenter.onVideoItemClicked(video)
        }
    }

    private fun openShortsFeed(index: Int) {
        val videos = shortsAdapter.items()
        if (videos.isEmpty()) return
        ShortsFeedActivity.start(this, videos, index)
    }

    private fun isShortsSection(): Boolean =
        currentSection?.type == BrowseSection.TYPE_SHORTS_GRID

    private fun switchContentMode(shorts: Boolean) {
        if (shortsMode == shorts && contentList.adapter != null) {
            if (shorts) {
                (contentList.layoutManager as? GridLayoutManager)?.spanCount =
                    PhoneUiMetrics.shortsGridSpan(this)
            }
            return
        }
        shortsMode = shorts
        val pad = PhoneUiMetrics.contentHorizontalPadding(this)
        if (shorts) {
            contentList.layoutManager =
                GridLayoutManager(this, PhoneUiMetrics.shortsGridSpan(this))
            contentList.adapter = shortsAdapter
            contentList.setPadding(pad, pad, pad, pad * 2)
        } else {
            contentList.layoutManager = LinearLayoutManager(this)
            contentList.adapter = contentAdapter
            contentList.setPadding(0, 0, 0, pad * 2)
        }
        PhoneUiMetrics.applyCenteredMaxWidth(contentList)
    }

    private fun requestMore(video: Video) {
        val now = System.currentTimeMillis()
        if (now - lastScrollEndAt < 700) return
        lastScrollEndAt = now
        if (isShortsSection()) {
            ShortsFeedSession.loadingMore = true
        }
        presenter.onScrollEnd(video)
    }

    override fun onResume() {
        super.onResume()
        presenter.onViewResumed()
    }

    override fun onPause() {
        super.onPause()
        presenter.onViewPaused()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onViewDestroyed()
    }

    override fun addSection(index: Int, section: BrowseSection?) {
        if (section == null) return
        sections[section.id] = section
        sectionAdapter.addSection(index, section)
    }

    override fun removeSection(category: BrowseSection?) {
        if (category == null) return
        sections.remove(category.id)
        sectionAdapter.removeSection(category)
    }

    override fun removeAllSections() {
        sections.clear()
        sectionAdapter.clearSections()
        contentAdapter.clear()
        shortsAdapter.clear()
    }

    override fun selectSection(index: Int, focusOnContent: Boolean) {
        if (sectionAdapter.size() == 0) return
        val safeIndex = index.coerceIn(0, sectionAdapter.size() - 1)
        sectionAdapter.select(safeIndex)
        val section = sectionAdapter.getSection(safeIndex) ?: return
        currentSection = section
        toolbar.title = section.title
        switchContentMode(section.type == BrowseSection.TYPE_SHORTS_GRID)
        contentAdapter.clear()
        shortsAdapter.clear()
        presenter.onSectionFocused(section.id)
        if (focusOnContent) {
            focusOnContent()
        }
    }

    override fun updateSection(group: VideoGroup?) {
        if (group == null) return
        errorText.visibility = View.GONE
        val sectionType = group.section?.type ?: currentSection?.type ?: BrowseSection.TYPE_ROW
        val shorts = sectionType == BrowseSection.TYPE_SHORTS_GRID
        switchContentMode(shorts)

        if (shorts) {
            // Never block the whole screen while appending Shorts pages.
            if (group.action != VideoGroup.ACTION_REPLACE) {
                showProgressBar(false)
            }
            shortsAdapter.applyGroup(group)
            when (group.action) {
                VideoGroup.ACTION_REPLACE -> ShortsFeedSession.replace(shortsAdapter.items())
                else -> ShortsFeedSession.append(group.videos ?: emptyList())
            }
            ShortsFeedSession.loadingMore = false
        } else {
            val asGrid = sectionType == BrowseSection.TYPE_GRID
                || sectionType == BrowseSection.TYPE_MULTI_GRID
            contentAdapter.applyVideoGroup(group, asGrid)
        }
    }

    override fun updateSection(group: SettingsGroup?) {
        if (group == null || group.isEmpty) return
        errorText.visibility = View.GONE
        switchContentMode(false)
        contentAdapter.showSettings(group.items)
    }

    override fun clearSection(section: BrowseSection?) {
        contentAdapter.clear()
        shortsAdapter.clear()
    }

    override fun selectSectionItem(index: Int) {}

    override fun selectSectionItem(item: Video?) {}

    override fun showError(data: ErrorFragmentData?) {
        errorText.visibility = View.VISIBLE
        errorText.text = data?.message ?: getString(R.string.no_content)
        ShortsFeedSession.loadingMore = false
    }

    override fun showProgressBar(show: Boolean) {
        // Shorts pagination uses footer/session flag — avoid fullscreen spinner ANR feel.
        if (show && isShortsSection() && !shortsAdapter.isEmpty()) {
            progressVisible = false
            progressBar.visibility = View.GONE
            return
        }
        progressVisible = show
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun isProgressBarShowing(): Boolean = progressVisible

    override fun focusOnContent() {
        contentList.requestFocus()
    }

    override fun isEmpty(): Boolean =
        if (shortsMode) shortsAdapter.isEmpty() else contentAdapter.isEmptyContent()

    override fun updateBadge() {}
}
