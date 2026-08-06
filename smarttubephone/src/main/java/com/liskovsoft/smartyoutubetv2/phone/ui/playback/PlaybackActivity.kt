package com.liskovsoft.smartyoutubetv2.phone.ui.playback

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SparseIntArray
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.Util
import com.google.android.material.button.MaterialButton
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerConstants
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerUI
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.ChatReceiver
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.SeekBarSegment
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView
import com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.ExoPlayerController
import com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.PlayerView as ExoQualityView
import com.liskovsoft.smartyoutubetv2.common.exoplayer.other.ExoPlayerInitializer
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.ExoFormatItem
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.renderer.CustomOverridesRenderersFactory
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.selector.RestoreTrackSelector
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData
import com.liskovsoft.smartyoutubetv2.common.utils.AppDialogUtil
import com.liskovsoft.smartyoutubetv2.phone.R
import com.liskovsoft.smartyoutubetv2.phone.adapter.VideoCardAdapter
import com.liskovsoft.smartyoutubetv2.phone.ui.PhoneBaseActivity
import com.liskovsoft.smartyoutubetv2.phone.ui.PhoneUiMetrics
import java.io.InputStream

class PlaybackActivity : PhoneBaseActivity(), PlaybackView, ExoQualityView {
    private lateinit var presenter: PlaybackPresenter
    private lateinit var exoController: ExoPlayerController
    private lateinit var playerInitializer: ExoPlayerInitializer
    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var titleView: TextView
    private lateinit var suggestionsList: RecyclerView
    private lateinit var suggestionsSection: View
    private lateinit var suggestionsAdapter: VideoCardAdapter
    private lateinit var playerActions: View
    private lateinit var btnFullscreen: MaterialButton
    private lateinit var btnResize: MaterialButton

    private var player: SimpleExoPlayer? = null
    private var currentVideo: Video? = null
    private var engineBlocked = false
    private var overlayShown = true
    private var controlsShown = true
    private var suggestionsShown = true
    private var progressVisible = false
    /** YouTube-like fullscreen (immersive). Default resize is FIT so captions aren't cropped. */
    private var userFullscreen = false
    /** false = Fit (letterbox, full frame), true = Fill (zoom/crop). */
    private var fillMode = false
    private var resizeMode = PlayerConstants.RESIZE_MODE_DEFAULT
    private val buttonStates = SparseIntArray()
    private val suggestionGroups = linkedMapOf<Int, VideoGroup>()
    private var seekBarSegments: List<SeekBarSegment> = emptyList()

    private val playbackStateListener = object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> showProgressBar(true)
                Player.STATE_READY, Player.STATE_ENDED -> showProgressBar(false)
                Player.STATE_IDLE -> { /* keep current */ }
            }
            if (playWhenReady && playbackState == Player.STATE_READY) {
                showProgressBar(false)
            }
        }
    }

    override fun phoneThemeResId(): Int = R.style.Theme_SmartTubePhone_Player

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_playback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inflate playback layout", e)
            superFinish()
            return
        }

        ensureDefault1080p()

        playerView = findViewById(R.id.player_view)
        progressBar = findViewById(R.id.player_progress)
        titleView = findViewById(R.id.player_title)
        suggestionsList = findViewById(R.id.suggestions_list)
        suggestionsSection = findViewById(R.id.suggestions_section)
        playerActions = findViewById(R.id.player_actions)
        btnFullscreen = findViewById(R.id.btn_fullscreen)
        btnResize = findViewById(R.id.btn_resize)

        setupPlayerHud()
        applyPlayerLayout()

        playerInitializer = ExoPlayerInitializer(this)
        presenter = PlaybackPresenter.instance(this)
        presenter.setView(this)
        exoController = ExoPlayerController(this, presenter)
        exoController.setPlayerView(this)

        suggestionsAdapter = VideoCardAdapter(
            onClick = { video -> presenter.onSuggestionItemClicked(video) },
            onLongClick = { video -> presenter.onSuggestionItemLongClicked(video) }
        )
        val tabletLand = PhoneUiMetrics.isTablet(this) &&
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        suggestionsList.layoutManager = LinearLayoutManager(
            this,
            if (tabletLand) LinearLayoutManager.VERTICAL else LinearLayoutManager.HORIZONTAL,
            false
        )
        suggestionsList.adapter = suggestionsAdapter

        findViewById<MaterialButton>(R.id.btn_quality).setOnClickListener {
            val dialog = AppDialogPresenter.instance(this)
            dialog.appendCategory(
                AppDialogUtil.createVideoPresetsCategory(this) {
                    applyPreferredFormat()
                    ensurePlaying()
                }
            )
            dialog.showDialog(getString(R.string.quality))
        }
        findViewById<MaterialButton>(R.id.btn_speed).setOnClickListener {
            val dialog = AppDialogPresenter.instance(this)
            dialog.appendCategory(AppDialogUtil.createSpeedListCategory(this, this))
            dialog.showDialog(getString(R.string.speed))
        }
        btnResize.setOnClickListener {
            fillMode = !fillMode
            applyPlayerLayout()
        }
        btnFullscreen.setOnClickListener {
            toggleFullscreen()
        }

        presenter.onViewInitialized()
    }

    private fun ensureDefault1080p() {
        val playerData = PlayerData.instance(this)
        val current = playerData.getFormat(FormatItem.TYPE_VIDEO)
        if (current == null || !current.isPreset) {
            playerData.setFormat(ExoFormatItem.fromVideoSpec("1920,1080,30,avc", true))
        }
    }

    private fun setupPlayerHud() {
        playerView.useController = true
        playerView.controllerShowTimeoutMs = 4_000
        playerView.setControllerHideOnTouch(true)
        playerView.setControllerVisibilityListener(
            PlayerControlView.VisibilityListener { visibility ->
                val visible = visibility == View.VISIBLE
                controlsShown = visible
                overlayShown = visible
                titleView.visibility = if (visible) View.VISIBLE else View.GONE
                playerActions.visibility = if (visible) View.VISIBLE else View.GONE
                if (::presenter.isInitialized) {
                    presenter.onControlsShown(visible)
                }
            }
        )
        // Start with HUD visible so user sees controls immediately.
        playerView.showController()
        titleView.visibility = View.VISIBLE
        playerActions.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if ((Util.SDK_INT <= 23 || player == null) && ::presenter.isInitialized) {
            initializePlayer()
        }
        if (::presenter.isInitialized) {
            presenter.onViewResumed()
        }
        // Keep controller enabled after dialog/settings return.
        if (::playerView.isInitialized) {
            playerView.useController = true
        }
        ensurePlaying()
    }

    override fun onPause() {
        super.onPause()
        if (::presenter.isInitialized) {
            presenter.onViewPaused()
        }
    }

    override fun onStop() {
        super.onStop()
        // Keep engine alive while quality/speed dialog is open (separate Activity).
        if (AppDialogPresenter.instance(this).isDialogShown) {
            return
        }
        if (Util.SDK_INT > 23) {
            maybeReleasePlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        maybeReleasePlayer()
        if (::presenter.isInitialized) {
            presenter.onViewDestroyed()
        }
    }

    private fun initializePlayer() {
        if (player != null || !::exoController.isInitialized) return
        try {
            ensureDefault1080p()
            val trackSelector = RestoreTrackSelector(AdaptiveTrackSelection.Factory())
            exoController.setTrackSelector(trackSelector)
            val renderersFactory = CustomOverridesRenderersFactory(this)
            val newPlayer = playerInitializer.createPlayer(this, renderersFactory, trackSelector)
            player = newPlayer
            exoController.setPlayer(newPlayer)
            playerView.player = newPlayer
            playerView.useController = true
            newPlayer.addListener(playbackStateListener)
            applyPreferredFormat()
            applyPlayerLayout()
            presenter.setView(this)
            presenter.onEngineInitialized()
            ensurePlaying()
        } catch (e: Exception) {
            Log.e(TAG, "initializePlayer failed", e)
        }
    }

    private fun applyPreferredFormat() {
        if (!::exoController.isInitialized) return
        val format = PlayerData.instance(this).getFormat(FormatItem.TYPE_VIDEO)
            ?: ExoFormatItem.fromVideoSpec("1920,1080,30,avc", true)
        exoController.selectFormat(format)
    }

    private fun ensurePlaying() {
        if (!::exoController.isInitialized || player == null) return
        try {
            exoController.setPlayWhenReady(true)
        } catch (e: Exception) {
            Log.e(TAG, "ensurePlaying failed", e)
        }
    }

    private fun maybeReleasePlayer() {
        if (engineBlocked) return
        releasePlayer()
    }

    private fun releasePlayer() {
        if (player == null) return
        try {
            player?.removeListener(playbackStateListener)
            if (::presenter.isInitialized) {
                presenter.onEngineReleased()
            }
            if (::playerView.isInitialized) {
                playerView.player = null
            }
            if (::exoController.isInitialized) {
                exoController.release()
            }
            if (::playerInitializer.isInitialized) {
                playerInitializer.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "releasePlayer failed", e)
        } finally {
            player = null
            showProgressBar(false)
        }
    }

    override fun setVideo(item: Video?) {
        currentVideo = item
        if (::exoController.isInitialized) {
            exoController.setVideo(item)
        }
        if (::titleView.isInitialized) {
            titleView.text = item?.title ?: ""
        }
    }

    override fun getVideo(): Video? =
        currentVideo ?: if (::exoController.isInitialized) exoController.video else null

    override fun finish() {
        superFinish()
    }

    override fun finishReally() {
        maybeReleasePlayer()
        superFinish()
    }

    private fun superFinish() {
        try {
            super.finish()
        } catch (e: Exception) {
            Log.e(TAG, "super.finish failed", e)
            try {
                finishAndRemoveTask()
            } catch (_: Exception) {
            }
        }
    }

    override fun showBackground(url: String?) {}
    override fun showBackgroundColor(colorResId: Int) {}

    override fun resetPlayerState() {
        if (::exoController.isInitialized) {
            exoController.resetPlayerState()
        }
        if (::suggestionsAdapter.isInitialized) {
            suggestionsAdapter.clear()
        }
        suggestionGroups.clear()
    }

    override fun isEmbed(): Boolean = false

    private fun openAndPlay(block: () -> Unit) {
        block()
        ensurePlaying()
        // Format opened — hide "fetching stream" spinner; buffering spinner handled by player state.
        showProgressBar(false)
        if (::playerView.isInitialized) {
            playerView.useController = true
            playerView.showController()
        }
    }

    override fun openSabr(formatInfo: MediaItemFormatInfo?) = openAndPlay {
        exoController.openSabr(formatInfo)
    }

    override fun openDash(formatInfo: MediaItemFormatInfo?) = openAndPlay {
        exoController.openDash(formatInfo)
    }

    override fun openDash(dashManifest: InputStream?) = openAndPlay {
        exoController.openDash(dashManifest)
    }

    override fun openDashUrl(dashManifestUrl: String?) = openAndPlay {
        exoController.openDashUrl(dashManifestUrl)
    }

    override fun openHlsUrl(hlsPlaylistUrl: String?) = openAndPlay {
        exoController.openHlsUrl(hlsPlaylistUrl)
    }

    override fun openUrlList(urlList: MutableList<String>?) = openAndPlay {
        exoController.openUrlList(urlList)
    }

    override fun openMerged(formatInfo: MediaItemFormatInfo?, hlsPlaylistUrl: String?) = openAndPlay {
        exoController.openMerged(formatInfo, hlsPlaylistUrl)
    }

    override fun openMerged(dashManifest: InputStream?, hlsPlaylistUrl: String?) = openAndPlay {
        exoController.openMerged(dashManifest, hlsPlaylistUrl)
    }

    override fun getPositionMs(): Long = exoController.positionMs
    override fun setPositionMs(positionMs: Long) = exoController.setPositionMs(positionMs)
    override fun getDurationMs(): Long = exoController.durationMs
    override fun setPlayWhenReady(play: Boolean) = exoController.setPlayWhenReady(play)
    override fun getPlayWhenReady(): Boolean = exoController.playWhenReady
    override fun isPlaying(): Boolean = exoController.isPlaying
    override fun isLoading(): Boolean = exoController.isLoading

    override fun getVideoFormats(): MutableList<FormatItem> =
        exoController.videoFormats?.toMutableList() ?: mutableListOf()

    override fun getAudioFormats(): MutableList<FormatItem> =
        exoController.audioFormats?.toMutableList() ?: mutableListOf()

    override fun getSubtitleFormats(): MutableList<FormatItem> =
        exoController.subtitleFormats?.toMutableList() ?: mutableListOf()

    override fun setFormat(option: FormatItem?) {
        if (option == null) return
        PlayerData.instance(this).setFormat(option)
        exoController.selectFormat(option)
        ensurePlaying()
    }

    override fun getVideoFormat(): FormatItem? = exoController.videoFormat
    override fun getAudioFormat(): FormatItem? = exoController.audioFormat
    override fun getSubtitleFormat(): FormatItem? = exoController.subtitleFormat
    override fun isEngineInitialized(): Boolean = player != null

    override fun restartEngine() {
        releasePlayer()
        initializePlayer()
    }

    override fun reloadPlayback() {
        if (player != null) {
            presenter.onEngineReleased()
            presenter.onEngineInitialized()
            ensurePlaying()
        }
    }

    override fun blockEngine(block: Boolean) {
        engineBlocked = block
    }

    override fun isEngineBlocked(): Boolean = engineBlocked

    override fun isInPIPMode(): Boolean =
        if (android.os.Build.VERSION.SDK_INT >= 24) isInPictureInPictureMode else false

    override fun containsMedia(): Boolean =
        if (::exoController.isInitialized) exoController.containsMedia() else false

    override fun setSpeed(speed: Float) {
        exoController.speed = speed
        setButtonState(
            com.liskovsoft.smartyoutubetv2.common.R.id.action_video_speed,
            if (speed != 1.0f) PlayerUI.BUTTON_ON else PlayerUI.BUTTON_OFF
        )
    }

    override fun getSpeed(): Float = exoController.speed
    override fun setPitch(pitch: Float) {
        exoController.pitch = pitch
    }

    override fun getPitch(): Float = exoController.pitch
    override fun setVolume(volume: Float) {
        exoController.volume = volume
    }

    override fun getVolume(): Float = exoController.volume

    override fun setResizeMode(mode: Int) {
        resizeMode = mode
        fillMode = mode == PlayerConstants.RESIZE_MODE_FIT_BOTH ||
            mode == PlayerConstants.RESIZE_MODE_STRETCH
        applyResizeMode()
    }

    override fun getResizeMode(): Int = resizeMode
    override fun setZoomPercents(percents: Int) {}
    override fun setAspectRatio(ratio: Float) {
        // Aspect is applied via resize mode on phone PlayerView.
    }

    override fun setRotationAngle(angle: Int) {
        if (::playerView.isInitialized) {
            playerView.rotation = angle.toFloat()
        }
    }

    override fun setVideoFlipEnabled(enabled: Boolean) {
        if (::playerView.isInitialized) {
            playerView.scaleX = if (enabled) -1f else 1f
        }
    }

    override fun setVideoGravity(gravity: Int) {}

    override fun updateSuggestions(group: VideoGroup?) {
        if (group == null || !::suggestionsAdapter.isInitialized) return
        suggestionGroups[group.id] = group
        val all = suggestionGroups.values.flatMap { it.videos ?: emptyList() }
        suggestionsAdapter.submit(all, true)
    }

    override fun removeSuggestions(group: VideoGroup?) {
        if (group == null || !::suggestionsAdapter.isInitialized) return
        suggestionGroups.remove(group.id)
        val all = suggestionGroups.values.flatMap { it.videos ?: emptyList() }
        suggestionsAdapter.submit(all, true)
    }

    override fun getSuggestionsIndex(group: VideoGroup?): Int {
        if (group == null) return -1
        return suggestionGroups.keys.indexOf(group.id)
    }

    override fun getSuggestionsByIndex(index: Int): VideoGroup? =
        suggestionGroups.values.elementAtOrNull(index)

    override fun focusSuggestedItem(index: Int) {}
    override fun focusSuggestedItem(video: Video?) {}
    override fun resetSuggestedPosition() {}
    override fun isSuggestionsEmpty(): Boolean =
        !::suggestionsAdapter.isInitialized || suggestionsAdapter.itemCount == 0

    override fun clearSuggestions() {
        suggestionGroups.clear()
        if (::suggestionsAdapter.isInitialized) {
            suggestionsAdapter.clear()
        }
    }

    override fun showOverlay(show: Boolean) {
        if (!::playerView.isInitialized) return
        overlayShown = show
        // Never disable useController — that kills tap-to-show HUD on phone.
        playerView.useController = true
        titleView.visibility = if (show) View.VISIBLE else View.GONE
        playerActions.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            playerView.showController()
        } else {
            playerView.hideController()
        }
    }

    override fun isOverlayShown(): Boolean = overlayShown

    override fun showSuggestions(show: Boolean) {
        if (!::suggestionsSection.isInitialized) return
        suggestionsShown = show
        if (isFullscreenUi()) {
            suggestionsSection.visibility = View.GONE
            return
        }
        suggestionsSection.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun isSuggestionsShown(): Boolean =
        suggestionsShown &&
            ::suggestionsSection.isInitialized &&
            suggestionsSection.visibility == View.VISIBLE

    override fun showControls(show: Boolean) {
        if (!::playerView.isInitialized) return
        controlsShown = show
        playerView.useController = true
        if (show) {
            playerView.showController()
            titleView.visibility = View.VISIBLE
            playerActions.visibility = View.VISIBLE
            overlayShown = true
        } else {
            playerView.hideController()
            titleView.visibility = View.GONE
            playerActions.visibility = View.GONE
            overlayShown = false
        }
        if (::presenter.isInitialized) {
            presenter.onControlsShown(show)
        }
    }

    override fun isControlsShown(): Boolean = controlsShown
    override fun getButtonState(buttonId: Int): Int = buttonStates.get(buttonId, -1)
    override fun setButtonState(buttonId: Int, buttonState: Int) {
        buttonStates.put(buttonId, buttonState)
    }

    override fun setChannelIcon(iconUrl: String?) {}
    override fun setSeekPreviewTitle(title: String?) {}
    override fun setNextTitle(nextVideo: Video?) {}
    override fun showDebugInfo(show: Boolean) {}
    override fun showSubtitles(show: Boolean) {}
    override fun loadStoryboard() {}

    override fun setTitle(title: String?) {
        if (::titleView.isInitialized) {
            titleView.text = title ?: currentVideo?.title ?: ""
        }
    }

    override fun showProgressBar(show: Boolean) {
        // Don't keep spinner once media is ready/playing.
        val shouldShow = show &&
            (player == null ||
                player?.playbackState == Player.STATE_BUFFERING ||
                player?.playbackState == Player.STATE_IDLE)
        progressVisible = shouldShow
        if (::progressBar.isInitialized) {
            progressBar.visibility = if (shouldShow) View.VISIBLE else View.GONE
        }
    }

    override fun setSeekBarSegments(segments: MutableList<SeekBarSegment>?) {
        seekBarSegments = segments ?: emptyList()
    }

    override fun updateEndingTime() {}
    override fun setChatReceiver(chatReceiver: ChatReceiver?) {}

    override fun setQualityInfo(info: String?) {
        if (!info.isNullOrBlank() && ::titleView.isInitialized) {
            titleView.text = "${currentVideo?.title ?: ""}\n$info"
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyPlayerLayout()
    }

    override fun onBackPressed() {
        if (userFullscreen) {
            setFullscreen(false)
            return
        }
        super.onBackPressed()
    }

    private fun toggleFullscreen() {
        setFullscreen(!userFullscreen)
    }

    private fun setFullscreen(enabled: Boolean) {
        userFullscreen = enabled
        requestedOrientation = if (enabled) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        applyPlayerLayout()
    }

    private fun isFullscreenUi(): Boolean =
        userFullscreen ||
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private fun applyPlayerLayout() {
        if (!::playerView.isInitialized || !::suggestionsSection.isInitialized) return

        val fullscreen = isFullscreenUi()
        suggestionsSection.visibility = if (fullscreen) {
            View.GONE
        } else if (suggestionsShown) {
            View.VISIBLE
        } else {
            View.GONE
        }

        applyResizeMode()
        setImmersiveMode(fullscreen)
        updateActionButtons()

        playerView.requestLayout()
        playerView.post { playerView.requestLayout() }
    }

    private fun applyResizeMode() {
        if (!::playerView.isInitialized) return
        // Default Fit = full frame visible (captions/translations not cropped).
        // Fill = zoom to cover screen (may crop edges).
        if (fillMode) {
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            resizeMode = PlayerConstants.RESIZE_MODE_FIT_BOTH
        } else {
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            resizeMode = PlayerConstants.RESIZE_MODE_DEFAULT
        }
    }

    private fun updateActionButtons() {
        if (::btnFullscreen.isInitialized) {
            btnFullscreen.setText(
                if (userFullscreen) R.string.exit_fullscreen else R.string.fullscreen
            )
        }
        if (::btnResize.isInitialized) {
            // Button shows the action you can switch TO.
            btnResize.setText(if (fillMode) R.string.video_fit else R.string.video_fill)
        }
    }

    private fun setImmersiveMode(enabled: Boolean) {
        val decor = window.decorView
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            @Suppress("DEPRECATION")
            decor.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            if (Build.VERSION.SDK_INT >= 28) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            @Suppress("DEPRECATION")
            decor.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    companion object {
        private const val TAG = "PlaybackActivity"
    }
}
