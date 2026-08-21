package com.liskovsoft.smartyoutubetv2.phone.player

import android.app.Activity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.liskovsoft.smartyoutubetv2.phone.R

/**
 * YouTube-style mini player bar at the bottom of any phone screen (above bottom nav).
 */
class PhoneMiniPlayerBinder(
    private val activity: Activity
) : PhonePlaybackBridge.Listener {
    private val bar: View

    init {
        val existing = activity.findViewById<View>(R.id.mini_player)
        bar = existing ?: inflateOverlay()
        bar.elevation = 12f * activity.resources.displayMetrics.density
        bar.setOnClickListener { PhonePlaybackBridge.host?.expandFromMiniPlayer() }
        bar.findViewById<ImageButton>(R.id.mini_player_play).setOnClickListener {
            PhonePlaybackBridge.host?.togglePlayPause()
            bind()
        }
        bar.findViewById<ImageButton>(R.id.mini_player_close).setOnClickListener {
            PhonePlaybackBridge.host?.closeFromMiniPlayer()
        }
    }

    private fun inflateOverlay(): View {
        val content = activity.findViewById<FrameLayout>(android.R.id.content)
        val view = LayoutInflater.from(activity).inflate(R.layout.view_mini_player, content, false)
        view.id = R.id.mini_player
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        )
        content.addView(view, lp)
        view.post { applyBottomInset(view) }
        return view
    }

    private fun applyBottomInset(view: View) {
        val nav = activity.findViewById<View>(R.id.bottom_nav)
        val lp = view.layoutParams as? FrameLayout.LayoutParams ?: return
        lp.bottomMargin = nav?.height ?: 0
        view.layoutParams = lp
    }

    fun onResume() {
        PhonePlaybackBridge.addListener(this)
        bind()
    }

    fun onPause() {
        PhonePlaybackBridge.removeListener(this)
    }

    override fun onMiniPlayerChanged() {
        bind()
    }

    private fun bind() {
        val visible = PhonePlaybackBridge.isVisible()
        bar.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) return
        applyBottomInset(bar)
        val video = PhonePlaybackBridge.host?.currentVideo()
        bar.findViewById<TextView>(R.id.mini_player_title).text = video?.title ?: ""
        val thumb = bar.findViewById<ImageView>(R.id.mini_player_thumb)
        if (!activity.isDestroyed) {
            Glide.with(thumb).load(video?.cardImageUrl).centerCrop().into(thumb)
        }
        val playing = PhonePlaybackBridge.host?.isPlaying() == true
        bar.findViewById<ImageButton>(R.id.mini_player_play)
            .setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
    }
}
