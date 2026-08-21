package com.liskovsoft.smartyoutubetv2.phone.ui

import android.content.Context
import android.content.res.Configuration
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.liskovsoft.smartyoutubetv2.phone.R

object PhoneUiMetrics {
    fun videoGridSpan(context: Context): Int {
        val configured = context.resources.getInteger(R.integer.grid_span_video).coerceAtLeast(1)
        return if (!isTablet(context)) 1 else configured
    }

    fun shortsGridSpan(context: Context): Int =
        context.resources.getInteger(R.integer.grid_span_shorts).coerceAtLeast(1)

    fun isTablet(context: Context): Boolean =
        context.resources.configuration.smallestScreenWidthDp >= 600

    /**
     * Center a child with an optional max width (0 dimen = full width).
     */
    fun applyCenteredMaxWidth(view: View, maxWidthRes: Int = R.dimen.content_max_width) {
        val max = view.resources.getDimensionPixelSize(maxWidthRes)
        val parent = view.parent as? FrameLayout
        val lp = view.layoutParams
        if (parent != null) {
            val flp = (lp as? FrameLayout.LayoutParams)
                ?: FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            if (max > 0) {
                flp.width = max
                flp.gravity = Gravity.CENTER_HORIZONTAL
            } else {
                flp.width = ViewGroup.LayoutParams.MATCH_PARENT
                flp.gravity = Gravity.CENTER_HORIZONTAL
            }
            view.layoutParams = flp
        } else if (lp != null && max > 0) {
            lp.width = max
            view.layoutParams = lp
        }
    }

    fun contentHorizontalPadding(context: Context): Int =
        context.resources.getDimensionPixelSize(R.dimen.content_padding)

    fun onConfigChanged(old: Configuration, newConfig: Configuration): Boolean =
        old.smallestScreenWidthDp != newConfig.smallestScreenWidthDp ||
            old.orientation != newConfig.orientation ||
            old.screenWidthDp != newConfig.screenWidthDp
}
