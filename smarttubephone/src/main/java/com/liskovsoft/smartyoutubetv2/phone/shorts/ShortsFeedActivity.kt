package com.liskovsoft.smartyoutubetv2.phone.shorts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.ui.PlayerView
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo
import com.liskovsoft.sharedutils.rx.RxHelper
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter
import com.liskovsoft.smartyoutubetv2.common.exoplayer.ExoMediaSourceFactory
import com.liskovsoft.smartyoutubetv2.common.exoplayer.other.ExoPlayerInitializer
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.renderer.CustomOverridesRenderersFactory
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.selector.RestoreTrackSelector
import com.liskovsoft.smartyoutubetv2.phone.R
import com.liskovsoft.smartyoutubetv2.phone.ui.PhoneBaseActivity
import com.liskovsoft.smartyoutubetv2.phone.ui.PhoneUiMetrics
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager
import io.reactivex.disposables.Disposable
import android.content.res.Configuration

/**
 * Vertical Shorts feed (TikTok / YouTube Shorts style).
 * Swipe up = next, swipe down = previous (natural vertical pager).
 */
class ShortsFeedActivity : PhoneBaseActivity() {
    override fun showsMiniPlayer(): Boolean = false
    private lateinit var pager: RecyclerView
    private lateinit var footerProgress: ProgressBar
    private lateinit var adapter: ShortsPagerAdapter

    private var player: SimpleExoPlayer? = null
    private var playerInitializer: ExoPlayerInitializer? = null
    private var mediaSourceFactory: ExoMediaSourceFactory? = null
    private var formatDisposable: Disposable? = null
    private var currentIndex = RecyclerView.NO_POSITION
    private var currentVideoId: String? = null
    private var loadingVideoId: String? = null
    private var pageSettledRunnable: Runnable? = null

    private val sessionListener = { refreshFromSession() }

    override fun phoneThemeResId(): Int = R.style.Theme_SmartTubePhone_Player

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shorts_feed)

        pager = findViewById(R.id.shorts_pager)
        footerProgress = findViewById(R.id.shorts_footer_progress)
        findViewById<ImageButton>(R.id.btn_shorts_close).setOnClickListener { finish() }

        adapter = ShortsPagerAdapter()
        val lm = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        pager.layoutManager = lm
        pager.adapter = adapter
        pager.setHasFixedSize(true)
        pager.itemAnimator = null
        PagerSnapHelper().attachToRecyclerView(pager)

        pager.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    schedulePageSettle()
                }
            }
        })

        PhoneUiMetrics.applyCenteredMaxWidth(pager, R.dimen.shorts_feed_max_width)

        ShortsFeedSession.addListener(sessionListener)
        refreshFromSession(scrollToStart = true)
        initPlayer()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        PhoneUiMetrics.applyCenteredMaxWidth(pager, R.dimen.shorts_feed_max_width)
        pager.post { playAt(currentIndex.coerceAtLeast(0), force = false) }
    }

    override fun onResume() {
        super.onResume()
        player?.playWhenReady = true
        if (currentIndex >= 0) {
            playAt(currentIndex, force = false)
        }
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        ShortsFeedSession.removeListener(sessionListener)
        pageSettledRunnable?.let { pager.removeCallbacks(it) }
        RxHelper.disposeActions(formatDisposable)
        releasePlayer()
    }

    private fun refreshFromSession(scrollToStart: Boolean = false) {
        val items = ShortsFeedSession.snapshot()
        adapter.submit(items)
        footerProgress.visibility =
            if (ShortsFeedSession.loadingMore) View.VISIBLE else View.GONE
        if (scrollToStart && items.isNotEmpty()) {
            val index = ShortsFeedSession.startIndex.coerceIn(0, items.lastIndex)
            pager.scrollToPosition(index)
            pager.post { playAt(index, force = true) }
        } else if (currentIndex >= items.size && items.isNotEmpty()) {
            playAt(items.lastIndex, force = true)
        }
        maybeRequestMore(currentIndex.coerceAtLeast(0))
    }

    private fun schedulePageSettle() {
        pageSettledRunnable?.let { pager.removeCallbacks(it) }
        val r = Runnable {
            val lm = pager.layoutManager as? LinearLayoutManager ?: return@Runnable
            val index = lm.findFirstCompletelyVisibleItemPosition()
                .takeIf { it != RecyclerView.NO_POSITION }
                ?: lm.findFirstVisibleItemPosition()
            if (index != RecyclerView.NO_POSITION) {
                playAt(index, force = false)
                maybeRequestMore(index)
            }
        }
        pageSettledRunnable = r
        // Debounce rapid flings while still loading.
        pager.postDelayed(r, 120)
    }

    private fun maybeRequestMore(index: Int) {
        val size = ShortsFeedSession.size()
        if (size == 0 || ShortsFeedSession.loadingMore) return
        if (index < size - 3) return
        val last = ShortsFeedSession.getOrNull(size - 1) ?: return
        ShortsFeedSession.loadingMore = true
        footerProgress.visibility = View.VISIBLE
        try {
            BrowsePresenter.instance(this).onScrollEnd(last)
        } catch (e: Exception) {
            Log.e(TAG, "request more shorts failed", e)
            ShortsFeedSession.loadingMore = false
            footerProgress.visibility = View.GONE
        }
    }

    private fun initPlayer() {
        try {
            playerInitializer = ExoPlayerInitializer(this)
            mediaSourceFactory = ExoMediaSourceFactory(this)
            val trackSelector = RestoreTrackSelector(AdaptiveTrackSelection.Factory())
            val renderersFactory = CustomOverridesRenderersFactory(this)
            player = playerInitializer!!.createPlayer(this, renderersFactory, trackSelector)
            player?.repeatMode = Player.REPEAT_MODE_ONE
            player?.addListener(object : Player.EventListener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    val holder = findHolder(currentIndex) ?: return
                    holder.loading.visibility =
                        if (playbackState == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                    if (playbackState == Player.STATE_READY) {
                        holder.thumb.visibility = View.GONE
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "initPlayer failed", e)
        }
    }

    private fun releasePlayer() {
        try {
            detachPlayerView()
            player?.release()
            playerInitializer?.release()
            mediaSourceFactory?.release()
        } catch (_: Exception) {
        } finally {
            player = null
            playerInitializer = null
            mediaSourceFactory = null
        }
    }

    private fun playAt(index: Int, force: Boolean) {
        val video = adapter.getVideo(index) ?: return
        if (!force && index == currentIndex && video.videoId == currentVideoId && player?.playbackState == Player.STATE_READY) {
            player?.playWhenReady = true
            return
        }
        if (!force && video.videoId != null && video.videoId == loadingVideoId) {
            return
        }

        currentIndex = index
        attachPlayerTo(index)

        val videoId = video.videoId
        if (videoId.isNullOrBlank()) return
        if (!force && videoId == currentVideoId && player?.containsMedia() == true) {
            player?.playWhenReady = true
            return
        }

        currentVideoId = videoId
        loadingVideoId = videoId
        loadAndPlay(video)
    }

    private fun SimpleExoPlayer.containsMedia(): Boolean =
        playbackState != Player.STATE_IDLE

    private fun loadAndPlay(video: Video) {
        val exo = player ?: return
        val factory = mediaSourceFactory ?: return
        val videoId = video.videoId ?: return

        findHolder(currentIndex)?.let {
            it.loading.visibility = View.VISIBLE
            it.thumb.visibility = View.VISIBLE
        }

        RxHelper.disposeActions(formatDisposable)
        val service = YouTubeServiceManager.instance().mediaItemService
        formatDisposable = service.getFormatInfoObserve(videoId)
            .subscribe({ info: MediaItemFormatInfo ->
                if (videoId != currentVideoId) return@subscribe
                loadingVideoId = null
                openFormat(exo, factory, info)
            }, { error ->
                Log.e(TAG, "format load error", error)
                loadingVideoId = null
                findHolder(currentIndex)?.loading?.visibility = View.GONE
            })
    }

    private fun openFormat(
        exo: SimpleExoPlayer,
        factory: ExoMediaSourceFactory,
        info: MediaItemFormatInfo
    ) {
        try {
            val source = when {
                info.containsDashFormats() -> factory.fromDashFormatInfo(info)
                info.containsSabrFormats() -> factory.fromSabrFormatInfo(info)
                info.containsHlsUrl() -> factory.fromHlsPlaylist(info.hlsManifestUrl)
                info.containsUrlFormats() -> factory.fromUrlList(info.createUrlList())
                else -> null
            }
            if (source == null) {
                findHolder(currentIndex)?.loading?.visibility = View.GONE
                return
            }
            exo.prepare(source)
            exo.playWhenReady = true
        } catch (e: Exception) {
            Log.e(TAG, "openFormat failed", e)
            findHolder(currentIndex)?.loading?.visibility = View.GONE
        }
    }

    private fun attachPlayerTo(index: Int) {
        detachPlayerView()
        val holder = findHolder(index) ?: return
        holder.playerView.player = player
    }

    private fun detachPlayerView() {
        for (i in 0 until adapter.itemCount) {
            findHolder(i)?.playerView?.player = null
        }
    }

    private fun findHolder(index: Int): ShortsPagerAdapter.Holder? {
        if (index < 0) return null
        return pager.findViewHolderForAdapterPosition(index) as? ShortsPagerAdapter.Holder
    }

    private class ShortsPagerAdapter : RecyclerView.Adapter<ShortsPagerAdapter.Holder>() {
        private val items = mutableListOf<Video>()

        fun submit(videos: List<Video>) {
            items.clear()
            items.addAll(videos)
            notifyDataSetChanged()
        }

        fun getVideo(index: Int): Video? = items.getOrNull(index)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_shorts_page, parent, false)
            view.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val video = items[position]
            holder.title.text = video.title ?: ""
            holder.channel.text = video.author ?: video.secondTitle ?: ""
            holder.thumb.visibility = View.VISIBLE
            holder.loading.visibility = View.GONE
            Glide.with(holder.thumb.context)
                .load(video.cardImageUrl)
                .centerCrop()
                .into(holder.thumb)
        }

        override fun onViewRecycled(holder: Holder) {
            holder.playerView.player = null
            super.onViewRecycled(holder)
        }

        override fun getItemCount(): Int = items.size

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val playerView: PlayerView = view.findViewById(R.id.shorts_player)
            val thumb: ImageView = view.findViewById(R.id.shorts_thumb)
            val title: TextView = view.findViewById(R.id.shorts_title)
            val channel: TextView = view.findViewById(R.id.shorts_channel)
            val loading: ProgressBar = view.findViewById(R.id.shorts_loading)
        }
    }

    companion object {
        private const val TAG = "ShortsFeedActivity"

        fun start(context: Context, videos: List<Video>, index: Int) {
            ShortsFeedSession.set(videos, index)
            context.startActivity(Intent(context, ShortsFeedActivity::class.java))
        }
    }
}
