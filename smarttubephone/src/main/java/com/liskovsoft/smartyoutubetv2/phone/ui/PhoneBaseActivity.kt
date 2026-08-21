package com.liskovsoft.smartyoutubetv2.phone.ui

import android.os.Bundle
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity
import com.liskovsoft.smartyoutubetv2.phone.R
import com.liskovsoft.smartyoutubetv2.phone.player.PhoneMiniPlayerBinder

/**
 * Keeps Material theme on phone. MotherActivity may swap in FitSystemWindows / TV schemes
 * that break Material and ExoPlayer PlayerView inflation.
 */
open class PhoneBaseActivity : MotherActivity() {
    private var miniPlayerBinder: PhoneMiniPlayerBinder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(phoneThemeResId())
    }

    override fun onStart() {
        super.onStart()
        if (showsMiniPlayer() && miniPlayerBinder == null) {
            miniPlayerBinder = PhoneMiniPlayerBinder(this)
        }
    }

    override fun onResume() {
        super.onResume()
        miniPlayerBinder?.onResume()
    }

    override fun onPause() {
        miniPlayerBinder?.onPause()
        super.onPause()
    }

    override fun initTheme() {
        setTheme(phoneThemeResId())
    }

    protected open fun showsMiniPlayer(): Boolean = true

    protected open fun phoneThemeResId(): Int = R.style.Theme_SmartTubePhone
}
