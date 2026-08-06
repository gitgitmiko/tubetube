package com.liskovsoft.smartyoutubetv2.phone.ui

import android.os.Bundle
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity
import com.liskovsoft.smartyoutubetv2.phone.R

/**
 * Keeps Material theme on phone. MotherActivity may swap in FitSystemWindows / TV schemes
 * that break Material and ExoPlayer PlayerView inflation.
 */
open class PhoneBaseActivity : MotherActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(phoneThemeResId())
    }

    override fun initTheme() {
        setTheme(phoneThemeResId())
    }

    protected open fun phoneThemeResId(): Int = R.style.Theme_SmartTubePhone
}
