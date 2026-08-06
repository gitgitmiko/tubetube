package com.liskovsoft.smartyoutubetv2.phone

import android.app.Activity
import androidx.multidex.MultiDexApplication
import com.liskovsoft.smartyoutubetv2.common.app.views.AppDialogView
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelUploadsView
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelView
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView
import com.liskovsoft.smartyoutubetv2.common.app.views.SearchView
import com.liskovsoft.smartyoutubetv2.common.app.views.SignInView
import com.liskovsoft.smartyoutubetv2.common.app.views.SplashView
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.ExoFormatItem
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem
import com.liskovsoft.smartyoutubetv2.common.prefs.NetworkData
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData
import com.liskovsoft.smartyoutubetv2.phone.ui.browse.BrowseActivity
import com.liskovsoft.smartyoutubetv2.phone.ui.channel.ChannelActivity
import com.liskovsoft.smartyoutubetv2.phone.ui.channeluploads.ChannelUploadsActivity
import com.liskovsoft.smartyoutubetv2.phone.ui.dialogs.AppDialogActivity
import com.liskovsoft.smartyoutubetv2.phone.ui.playback.PlaybackActivity
import com.liskovsoft.smartyoutubetv2.phone.ui.search.SearchActivity
import com.liskovsoft.smartyoutubetv2.phone.ui.signin.SignInActivity
import com.liskovsoft.smartyoutubetv2.phone.ui.splash.SplashActivity
import org.conscrypt.Conscrypt
import java.security.Security

class PhoneApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        try {
            val provider = Conscrypt.newProvider()
            if (NetworkData.instance(this).isConscryptEnabled()) {
                Security.insertProviderAt(provider, 1)
            }
        } catch (_: Throwable) {
            // Native conscrypt may be unavailable on some devices.
        }

        // Phone default: 1080p AVC preset so playback starts without opening quality dialog.
        val playerData = PlayerData.instance(this)
        val current = playerData.getFormat(FormatItem.TYPE_VIDEO)
        if (current == null || !current.isPreset) {
            playerData.setFormat(ExoFormatItem.fromVideoSpec("1920,1080,30,avc", true))
        }

        setupViewManager()
    }

    @Suppress("UNCHECKED_CAST")
    private fun setupViewManager() {
        val viewManager = ViewManager.instance(this)
        viewManager.setRoot(BrowseActivity::class.java as Class<out Activity>)
        viewManager.register(SplashView::class.java, SplashActivity::class.java as Class<out Activity>)
        viewManager.register(BrowseView::class.java, BrowseActivity::class.java as Class<out Activity>)
        viewManager.register(
            PlaybackView::class.java,
            PlaybackActivity::class.java as Class<out Activity>,
            BrowseActivity::class.java as Class<out Activity>
        )
        viewManager.register(
            AppDialogView::class.java,
            AppDialogActivity::class.java as Class<out Activity>,
            BrowseActivity::class.java as Class<out Activity>
        )
        viewManager.register(
            SearchView::class.java,
            SearchActivity::class.java as Class<out Activity>,
            BrowseActivity::class.java as Class<out Activity>
        )
        viewManager.register(
            SignInView::class.java,
            SignInActivity::class.java as Class<out Activity>,
            BrowseActivity::class.java as Class<out Activity>
        )
        viewManager.register(
            ChannelView::class.java,
            ChannelActivity::class.java as Class<out Activity>,
            BrowseActivity::class.java as Class<out Activity>
        )
        viewManager.register(
            ChannelUploadsView::class.java,
            ChannelUploadsActivity::class.java as Class<out Activity>,
            BrowseActivity::class.java as Class<out Activity>
        )
    }

    companion object {
        init {
            System.setProperty("http.keepAlive", "false")
        }
    }
}
