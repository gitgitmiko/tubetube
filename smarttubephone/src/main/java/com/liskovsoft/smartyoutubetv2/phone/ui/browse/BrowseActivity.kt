package com.liskovsoft.smartyoutubetv2.phone.ui.browse

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.ErrorFragmentData
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter
import com.liskovsoft.smartyoutubetv2.common.app.presenters.ChannelUploadsPresenter
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.VideoActionPresenter
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.AccountSettingsPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager
import com.liskovsoft.smartyoutubetv2.phone.R
import com.liskovsoft.smartyoutubetv2.phone.adapter.BrowseContentAdapter
import com.liskovsoft.smartyoutubetv2.phone.adapter.ChipAdapter
import com.liskovsoft.smartyoutubetv2.phone.adapter.PhoneChip
import com.liskovsoft.smartyoutubetv2.phone.adapter.SectionAdapter
import com.liskovsoft.smartyoutubetv2.phone.adapter.ShortsGridAdapter
import com.liskovsoft.smartyoutubetv2.phone.navigation.PhoneTab
import com.liskovsoft.smartyoutubetv2.phone.player.PhonePlaybackBridge
import com.liskovsoft.smartyoutubetv2.phone.shorts.ShortsFeedActivity
import com.liskovsoft.smartyoutubetv2.phone.shorts.ShortsFeedSession
import com.liskovsoft.smartyoutubetv2.phone.ui.PhoneBaseActivity
import com.liskovsoft.smartyoutubetv2.phone.ui.PhoneUiMetrics

class BrowseActivity : PhoneBaseActivity(), BrowseView {
    private lateinit var presenter: BrowsePresenter
    private lateinit var titleView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var contentList: RecyclerView
    private lateinit var chipList: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var accountButton: ImageButton

    private val accountChangeListener = MediaServiceManager.AccountChangeListener {
        runOnUiThread { bindAccountAvatar() }
    }

    private lateinit var sectionAdapter: SectionAdapter
    private lateinit var chipAdapter: ChipAdapter
    private lateinit var contentAdapter: BrowseContentAdapter
    private lateinit var shortsAdapter: ShortsGridAdapter

    private val sections = LinkedHashMap<Int, BrowseSection>()
    private var currentSection: BrowseSection? = null
    private var progressVisible = false
    private var shortsMode = false
    private var lastScrollEndAt = 0L
    private var currentTab = PhoneTab.HOME
    private var ignoreBottomNav = false
    private var selectedChipId: Int = MediaGroup.TYPE_HOME
    private var homeFallbackTried = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)

        titleView = findViewById(R.id.toolbar_title)
        progressBar = findViewById(R.id.progress_bar)
        errorText = findViewById(R.id.error_text)
        contentList = findViewById(R.id.content_list)
        chipList = findViewById(R.id.chip_list)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        bottomNav = findViewById(R.id.bottom_nav)

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (PhonePlaybackBridge.isVisible()) {
                    moveTaskToBack(true)
                    return
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })

        findViewById<ImageButton>(R.id.btn_search).setOnClickListener {
            SearchPresenter.instance(this).startSearch(null)
        }
        accountButton = findViewById(R.id.btn_account)
        accountButton.setOnClickListener {
            AccountSettingsPresenter.instance(this).show()
        }
        MediaServiceManager.instance().addAccountListener(accountChangeListener)
        bindAccountAvatar()

        sectionAdapter = SectionAdapter { section, index ->
            applySection(section, index)
        }

        chipAdapter = ChipAdapter { chip ->
            selectedChipId = chip.id
            chipAdapter.select(chip.id)
            when (chip.id) {
                CHIP_WATCH_LATER -> openWatchLater()
                CHIP_DOWNLOADS -> startActivity(android.content.Intent(this, com.liskovsoft.smartyoutubetv2.phone.ui.downloads.DownloadsActivity::class.java))
                else -> openSectionId(chip.sectionId)
            }
        }
        chipList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        chipList.adapter = chipAdapter

        contentAdapter = BrowseContentAdapter(
            onVideoClick = { openVideo(it) },
            onVideoLongClick = { presenter.onVideoItemLongClicked(it) },
            onScrollEnd = { requestMore(it) },
            onSettingsClick = { it.onClick?.run() }
        )
        shortsAdapter = ShortsGridAdapter(
            onClick = { _, index -> openShortsFeed(index) },
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

        swipeRefresh.setColorSchemeResources(R.color.phone_accent)
        swipeRefresh.setOnRefreshListener { presenter.refresh() }

        bottomNav.setOnItemSelectedListener { item ->
            if (ignoreBottomNav) return@setOnItemSelectedListener true
            when (item.itemId) {
                R.id.nav_home -> {
                    currentTab = PhoneTab.HOME
                    showHomeChips()
                    openSectionId(MediaGroup.TYPE_HOME)
                    true
                }
                R.id.nav_shorts -> {
                    currentTab = PhoneTab.SHORTS
                    hideChips()
                    openSectionId(MediaGroup.TYPE_SHORTS)
                    true
                }
                R.id.nav_search -> {
                    SearchPresenter.instance(this).startSearch(null)
                    false
                }
                R.id.nav_subs -> {
                    currentTab = PhoneTab.SUBSCRIPTIONS
                    hideChips()
                    openSectionId(MediaGroup.TYPE_SUBSCRIPTIONS)
                    true
                }
                R.id.nav_library -> {
                    currentTab = PhoneTab.LIBRARY
                    showLibraryChips()
                    openSectionId(MediaGroup.TYPE_HISTORY)
                    true
                }
                else -> false
            }
        }

        presenter = BrowsePresenter.instance(this)
        presenter.setView(this)
        presenter.onViewInitialized()
        currentTab = PhoneTab.HOME
        showHomeChips()
        ignoreBottomNav = true
        bottomNav.selectedItemId = R.id.nav_home
        ignoreBottomNav = false
        openSectionId(MediaGroup.TYPE_HOME)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        PhoneUiMetrics.applyCenteredMaxWidth(contentList)
        if (shortsMode) {
            (contentList.layoutManager as? GridLayoutManager)?.spanCount =
                PhoneUiMetrics.shortsGridSpan(this)
            val pad = PhoneUiMetrics.contentHorizontalPadding(this)
            contentList.setPadding(pad, pad, pad, pad * 2)
        }
    }

    private fun openSectionId(sectionId: Int) {
        val index = sectionAdapter.indexOf(sectionId)
        if (index >= 0) {
            val section = sectionAdapter.getSection(index) ?: return
            applySection(section, index)
        } else {
            presenter.selectSection(sectionId)
        }
    }

    private fun applySection(section: BrowseSection, index: Int) {
        sectionAdapter.select(index)
        currentSection = section
        titleView.text = when (currentTab) {
            PhoneTab.HOME -> getString(R.string.app_name)
            PhoneTab.SHORTS -> getString(R.string.nav_shorts)
            PhoneTab.SUBSCRIPTIONS -> getString(R.string.nav_subscriptions)
            PhoneTab.LIBRARY -> getString(R.string.nav_library)
            else -> section.title
        }
        switchContentMode(section.type == BrowseSection.TYPE_SHORTS_GRID)
        contentAdapter.clear()
        shortsAdapter.clear()
        errorText.visibility = View.GONE
        showProgressBar(true)
        if (section.id == MediaGroup.TYPE_HOME) {
            homeFallbackTried = false
        }
        presenter.onSectionFocused(section.id)
        syncBottomNav(section)
    }

    private fun syncBottomNav(section: BrowseSection) {
        val itemId = when (section.id) {
            MediaGroup.TYPE_SHORTS -> R.id.nav_shorts
            MediaGroup.TYPE_SUBSCRIPTIONS -> R.id.nav_subs
            MediaGroup.TYPE_HOME,
            MediaGroup.TYPE_TRENDING,
            MediaGroup.TYPE_MUSIC,
            MediaGroup.TYPE_GAMING,
            MediaGroup.TYPE_NEWS,
            MediaGroup.TYPE_LIVE,
            MediaGroup.TYPE_SPORTS,
            MediaGroup.TYPE_KIDS_HOME -> R.id.nav_home
            else -> R.id.nav_library
        }
        if (bottomNav.selectedItemId != itemId) {
            ignoreBottomNav = true
            bottomNav.selectedItemId = itemId
            ignoreBottomNav = false
        }
    }

    private fun showHomeChips() {
        chipList.visibility = View.VISIBLE
        val chips = listOf(
            PhoneChip(MediaGroup.TYPE_HOME, getString(R.string.chip_all), MediaGroup.TYPE_HOME),
            PhoneChip(
                MediaGroup.TYPE_TRENDING,
                getString(com.liskovsoft.smartyoutubetv2.common.R.string.header_trending),
                MediaGroup.TYPE_TRENDING
            ),
            PhoneChip(
                MediaGroup.TYPE_MUSIC,
                getString(com.liskovsoft.smartyoutubetv2.common.R.string.header_music),
                MediaGroup.TYPE_MUSIC
            ),
            PhoneChip(
                MediaGroup.TYPE_GAMING,
                getString(com.liskovsoft.smartyoutubetv2.common.R.string.header_gaming),
                MediaGroup.TYPE_GAMING
            ),
            PhoneChip(
                MediaGroup.TYPE_NEWS,
                getString(com.liskovsoft.smartyoutubetv2.common.R.string.header_news),
                MediaGroup.TYPE_NEWS
            ),
            PhoneChip(
                MediaGroup.TYPE_LIVE,
                getString(com.liskovsoft.smartyoutubetv2.common.R.string.badge_live),
                MediaGroup.TYPE_LIVE
            )
        )
        selectedChipId = MediaGroup.TYPE_HOME
        chipAdapter.submit(chips, selectedChipId)
    }

    private fun showLibraryChips() {
        chipList.visibility = View.VISIBLE
        val chips = listOf(
            PhoneChip(
                MediaGroup.TYPE_HISTORY,
                getString(com.liskovsoft.smartyoutubetv2.common.R.string.header_history),
                MediaGroup.TYPE_HISTORY
            ),
            PhoneChip(
                CHIP_WATCH_LATER,
                getString(R.string.action_watch_later),
                CHIP_WATCH_LATER
            ),
            PhoneChip(
                MediaGroup.TYPE_USER_PLAYLISTS,
                getString(com.liskovsoft.smartyoutubetv2.common.R.string.header_playlists),
                MediaGroup.TYPE_USER_PLAYLISTS
            ),
            PhoneChip(
                MediaGroup.TYPE_MY_VIDEOS,
                getString(com.liskovsoft.smartyoutubetv2.common.R.string.my_videos),
                MediaGroup.TYPE_MY_VIDEOS
            ),
            PhoneChip(
                MediaGroup.TYPE_CHANNEL_UPLOADS,
                getString(com.liskovsoft.smartyoutubetv2.common.R.string.header_channels),
                MediaGroup.TYPE_CHANNEL_UPLOADS
            ),
            PhoneChip(
                MediaGroup.TYPE_PLAYBACK_QUEUE,
                getString(com.liskovsoft.smartyoutubetv2.common.R.string.playback_queue_category_title),
                MediaGroup.TYPE_PLAYBACK_QUEUE
            ),
            PhoneChip(
                CHIP_DOWNLOADS,
                getString(R.string.downloads),
                CHIP_DOWNLOADS
            ),
            PhoneChip(
                MediaGroup.TYPE_SETTINGS,
                getString(com.liskovsoft.smartyoutubetv2.common.R.string.header_settings),
                MediaGroup.TYPE_SETTINGS
            )
        )
        selectedChipId = MediaGroup.TYPE_HISTORY
        chipAdapter.submit(chips, selectedChipId)
    }

    private fun hideChips() {
        chipList.visibility = View.GONE
    }

    private fun openWatchLater() {
        val video = Video()
        video.playlistId = "WL"
        video.title = getString(R.string.action_watch_later)
        ChannelUploadsPresenter.instance(this).openChannel(video)
    }

    private fun openVideo(video: Video) {
        if (isShortsSection()) {
            val index = shortsAdapter.items().indexOfFirst { it.videoId == video.videoId }
                .takeIf { it >= 0 } ?: 0
            openShortsFeed(index)
            return
        }
        // Music/Live shelves are often mixes, radios, or event playlists. The cover
        // videoId usually won't play; resolve the playlist (or first real video) first.
        if (shouldResolveAsPlaylist(video)) {
            VideoActionPresenter.instance(this).apply(video)
            return
        }
        if (video.hasVideo()) {
            PlaybackPresenter.instance(this).openVideo(video)
            return
        }
        VideoActionPresenter.instance(this).apply(video)
    }

    private fun shouldResolveAsPlaylist(video: Video): Boolean {
        val playlistLike = video.isMix || video.hasPlaylist() || video.hasNestedItems() || video.hasReloadPageKey()
        if (!playlistLike) return false
        val sectionId = currentSection?.id
        return sectionId == MediaGroup.TYPE_MUSIC ||
            sectionId == MediaGroup.TYPE_LIVE ||
            video.belongsToMusic()
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
        if (isShortsSection() && ShortsFeedSession.loadingMore) return
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
        bindAccountAvatar()
    }

    override fun onPause() {
        super.onPause()
        presenter.onViewPaused()
    }

    override fun onDestroy() {
        MediaServiceManager.instance().removeAccountListener(accountChangeListener)
        super.onDestroy()
        presenter.onViewDestroyed()
    }

    private fun bindAccountAvatar() {
        if (isDestroyed) return
        val url = MediaServiceManager.instance().selectedAccount?.avatarImageUrl
        if (url.isNullOrBlank()) {
            Glide.with(this).clear(accountButton)
            accountButton.scaleType = ImageView.ScaleType.CENTER_INSIDE
            accountButton.setImageResource(R.drawable.ic_account)
            return
        }
        accountButton.scaleType = ImageView.ScaleType.CENTER_CROP
        Glide.with(this)
            .load(url)
            .circleCrop()
            .placeholder(R.drawable.ic_account)
            .error(R.drawable.ic_account)
            .into(accountButton)
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
        val section = sectionAdapter.getSection(safeIndex) ?: return
        applySection(section, safeIndex)
        if (focusOnContent) {
            focusOnContent()
        }
    }

    override fun updateSection(group: VideoGroup?) {
        if (group == null) return
        val incomingSectionId = group.section?.id
        if (incomingSectionId != null && currentSection != null && incomingSectionId != currentSection?.id) {
            return
        }
        errorText.visibility = View.GONE
        swipeRefresh.isRefreshing = false
        val sectionType = group.section?.type ?: currentSection?.type ?: BrowseSection.TYPE_ROW
        val shorts = sectionType == BrowseSection.TYPE_SHORTS_GRID
        switchContentMode(shorts)

        if (shorts) {
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
        swipeRefresh.isRefreshing = false
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
        if (fallbackEmptyHomeToTrending()) {
            return
        }
        errorText.visibility = View.VISIBLE
        errorText.text = data?.message ?: getString(R.string.no_content)
        swipeRefresh.isRefreshing = false
        ShortsFeedSession.loadingMore = false
    }

    private fun fallbackEmptyHomeToTrending(): Boolean {
        if (homeFallbackTried) return false
        if (currentTab != PhoneTab.HOME) return false
        if (selectedChipId != MediaGroup.TYPE_HOME && currentSection?.id != MediaGroup.TYPE_HOME) {
            return false
        }
        homeFallbackTried = true
        selectedChipId = MediaGroup.TYPE_TRENDING
        chipAdapter.select(MediaGroup.TYPE_TRENDING)
        openSectionId(MediaGroup.TYPE_TRENDING)
        return true
    }

    override fun showProgressBar(show: Boolean) {
        if (show && isShortsSection() && !shortsAdapter.isEmpty()) {
            progressVisible = false
            progressBar.visibility = View.GONE
            return
        }
        progressVisible = show
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (!show) {
            swipeRefresh.isRefreshing = false
        }
    }

    override fun isProgressBarShowing(): Boolean = progressVisible

    override fun focusOnContent() {
        contentList.requestFocus()
    }

    override fun isEmpty(): Boolean =
        if (shortsMode) shortsAdapter.isEmpty() else contentAdapter.isEmptyContent()

    override fun updateBadge() {}

    companion object {
        private const val CHIP_WATCH_LATER = 10_001
        private const val CHIP_DOWNLOADS = 10_002
    }
}
