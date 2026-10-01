/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.clocks.controller

import android.icu.util.TimeZone
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.Modifier
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import com.android.systemui.clocks.AnalogHandLayer
import com.android.systemui.clocks.AssetDrawable
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.customization.clocks.AnalogTickMode
import com.android.systemui.customization.clocks.AnalogTimespec
import com.android.systemui.customization.clocks.AnalogTimespecHandler
import com.android.systemui.customization.clocks.R as clocksR
import com.android.systemui.customization.clocks.view.DefaultClockFaceLayout
import com.android.systemui.log.core.Logger
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.VRect
import com.android.systemui.plugins.keyguard.VRectF
import com.android.systemui.plugins.keyguard.data.model.AlarmData
import com.android.systemui.plugins.keyguard.data.model.WeatherData
import com.android.systemui.plugins.keyguard.data.model.ZenData
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAnimations
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAxisStyle
import com.android.systemui.plugins.keyguard.ui.clocks.ClockEvents
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceEvents
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPositionAnimationArgs
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPreviewConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockTickRate
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig
import com.android.systemui.plugins.keyguard.ui.clocks.TimeFormatKind
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class AnalogClockFaceLayout(view: View) : DefaultClockFaceLayout(view) {
    private var maxHeight = -1
    private var maxWidth = -1

    init {
        if (view is ViewGroup) {
            for (child in view.children) {
                maxHeight = max(maxHeight, child.layoutParams.height)
                maxWidth = max(maxWidth, child.layoutParams.width)
            }
        }
        smallClockModifier = { Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true) }
    }

    override fun applyConstraints(constraints: ConstraintSet): ConstraintSet {
        constraints.constrainHeight(
            ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE,
            view.context.resources.getDimensionPixelSize(clocksR.dimen.large_clock_text_size) * 2,
        )

        val heightScale =
            if (constraints.getHeight(view.id) > 0) {
                constraints.getHeight(view.id).toFloat() / maxHeight
            } else {
                -1f
            }
        val widthScale =
            if (constraints.getWidth(view.id) > 0) {
                constraints.getWidth(view.id).toFloat() / maxWidth
            } else {
                -1f
            }
        val scale =
            when {
                heightScale >= 0f && widthScale >= 0f -> min(heightScale, widthScale)
                heightScale >= 0f -> heightScale
                widthScale >= 0f -> widthScale
                else -> 1f
            }

        constraints.constrainHeight(view.id, (maxHeight * scale).toInt())
        constraints.constrainWidth(view.id, (scale * maxWidth).toInt())
        return constraints
    }

    override fun applyExternalDisplayPresentationConstraints(
        constraints: ConstraintSet
    ): ConstraintSet {
        super.applyExternalDisplayPresentationConstraints(constraints)
        applyConstraints(constraints)
        constraints.connect(
            ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
        )
        constraints.connect(
            ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
        )
        constraints.connect(
            ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
        )
        constraints.connect(
            ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
        )
        return constraints
    }

    override fun applyPreviewConstraints(
        clockPreviewConfig: ClockPreviewConfig,
        constraints: ConstraintSet,
    ): ConstraintSet {
        super.applyPreviewConstraints(clockPreviewConfig, constraints)
        applyConstraints(constraints)
        return constraints
    }
}

class AnalogHandLayerController(clockCtx: CustomClockContext, private val layer: AnalogHandLayer) :
    ClockLayerController {
    val timespec = AnalogTimespecHandler(layer.timespec, layer.tickMode, clockCtx.timeKeeper)
    private val logger = Logger(clockCtx.messageBuffer, TAG)
    private var lastLogTime = 0L

    override val config =
        ClockFaceConfig(
            tickRate =
                when (layer.timespec) {
                    AnalogTimespec.SECONDS ->
                        if (timespec.isSweeping) ClockTickRate.PER_FRAME
                        else ClockTickRate.PER_SECOND
                    AnalogTimespec.MINUTES ->
                        if (timespec.isSweeping) ClockTickRate.PER_SECOND
                        else ClockTickRate.PER_MINUTE
                    else -> ClockTickRate.PER_MINUTE
                }
        )

    override val view: View = ImageView(clockCtx.context)
    val asset = AssetDrawable(clockCtx.assets, layer.asset)
    override var onViewBoundsChanged: ((VRectF) -> Unit)? = null
    override var onViewMaxSizeChanged: ((VPointF) -> Unit)? = null

    init {
        (view as ImageView).setImageDrawable(asset)
        val size = max(asset.intrinsicHeight, asset.intrinsicWidth)
        view.layoutParams =
            RelativeLayout.LayoutParams(size, size).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            }
    }

    override val events =
        object : ClockEvents {
            override fun onTimeZoneChanged(timeZone: TimeZone) {
                timespec.timeKeeper.timeZone = timeZone
                faceEvents.onTimeTick()
            }

            override fun onTimeFormatChanged(formatKind: TimeFormatKind) {}

            override fun onLocaleChanged(locale: Locale) {}

            override fun onWeatherDataChanged(data: WeatherData) {}

            override fun onAlarmDataChanged(data: AlarmData) {}

            override fun onZenDataChanged(data: ZenData) {}
        }

    override val animations =
        object : ClockAnimations {
            override fun enter() {}

            override fun doze(fraction: Float) {
                asset.dozeFraction = fraction
            }

            override fun fold(fraction: Float) {}

            override fun charge() {}

            override fun onPositionAnimated(anim: ClockPositionAnimationArgs) {}

            override fun onPickerCarouselSwiping(swipingFraction: Float) {}

            override fun onFidgetTap(x: Float, y: Float) {}

            override fun onFontAxesChanged(style: ClockAxisStyle) {}
        }

    override val faceEvents =
        object : ClockFaceEvents {
            override fun onTimeTick() {
                timespec.timeKeeper.updateTime()
                view.rotation = timespec.getRotation() * 360f

                val now = System.currentTimeMillis()
                if (
                    layer.tickMode == AnalogTickMode.SWEEP &&
                        layer.timespec == AnalogTimespec.SECONDS &&
                        now - lastLogTime <= 10000
                ) {
                    return
                }

                lastLogTime = now
                logger.d({ "onTimeTick: new rotation=$double1 (degrees)" }) {
                    double1 = view.rotation.toDouble()
                }
            }

            override fun onThemeChanged(theme: ThemeConfig) {
                asset.lightFraction = if (theme.isDarkTheme) 1f else 0f
                asset.updateTints()
            }

            override fun onFontSettingChanged(fontSizePx: Float) {}

            override fun onTargetRegionChanged(targetRegion: VRect) {}

            override fun onSecondaryDisplayChanged(onSecondaryDisplay: Boolean) {}
        }

    companion object {
        private val TAG = AnalogHandLayerController::class.simpleName!!
    }
}
