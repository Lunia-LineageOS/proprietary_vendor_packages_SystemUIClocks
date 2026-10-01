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
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.android.systemui.clocks.ClockFace
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.view.DigitalFaceViewGroup
import com.android.systemui.customization.clocks.DigitalTimeFormatter
import com.android.systemui.customization.clocks.DigitalTimespec
import com.android.systemui.customization.clocks.DigitalTimespecHandler
import com.android.systemui.customization.clocks.R as clocksR
import com.android.systemui.customization.clocks.utils.ViewUtils
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.VRect
import com.android.systemui.plugins.keyguard.data.model.AlarmData
import com.android.systemui.plugins.keyguard.data.model.WeatherData
import com.android.systemui.plugins.keyguard.data.model.ZenData
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAnimations
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAxisStyle
import com.android.systemui.plugins.keyguard.ui.clocks.ClockEvents
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceController
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceEvents
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceLayout
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPositionAnimationArgs
import com.android.systemui.plugins.keyguard.ui.clocks.ClockTickRate
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig
import com.android.systemui.plugins.keyguard.ui.clocks.TimeFormatKind
import java.util.Locale
import kotlin.math.max

class CustomClockFaceController(
    private val clockCtx: CustomClockContext,
    private val clockFace: ClockFace,
    private val isLargeClock: Boolean,
) : ClockFaceController {
    interface ClockEventUnion : ClockEvents, ClockFaceEvents

    override var theme = ThemeConfig(isDarkTheme = true, clockCtx.assets.seedColor)
    val layers = mutableListOf<ClockLayerController>()

    val timeFormatter =
        DigitalTimeFormatter("hh:mm", clockCtx.timeKeeper, enableContentDescription = true)
    val timespecHandler = DigitalTimespecHandler(DigitalTimespec.TIME_FULL_FORMAT, timeFormatter)

    override val view: View
    override val config: ClockFaceConfig
    override val layout: ClockFaceLayout
    override val events: ClockEventUnion
    override val animations: ClockAnimations

    init {
        val layoutParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        layoutParams.gravity = Gravity.CENTER
        if (clockFace.layers.size == 1) {
            val controller =
                clockCtx.factory.createLayerController(clockCtx, clockFace.layers[0], isLargeClock)
            layers.add(controller)
            controller.view.layoutParams = layoutParams
            view = controller.view
        } else {
            val viewGroup = clockCtx.factory.createViewGroup(clockCtx, clockFace)
            viewGroup.layoutParams = layoutParams
            viewGroup.clipChildren = false
            clockFace.layers.forEach { layer ->
                val controller =
                    clockCtx.factory.createLayerController(clockCtx, layer, isLargeClock)
                viewGroup.addView(controller.view)
                layers.add(controller)
            }
            view = viewGroup
        }

        config =
            ClockFaceConfig(
                tickRate = computeTickRate(),
                hasCustomWeatherDataDisplay = layers.any { it.config.hasCustomWeatherDataDisplay },
                hasCustomPositionUpdatedAnimation =
                    layers.any { it.config.hasCustomPositionUpdatedAnimation },
                useCustomClockScene = layers.any { it.config.useCustomClockScene },
            )
        view.id =
            if (isLargeClock) {
                ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE
            } else {
                ClockViewIds.LOCKSCREEN_CLOCK_VIEW_SMALL
            }
        layout = clockCtx.factory.createLayout(clockCtx, clockFace, view)
        events = createEvents()
        animations = createAnimations()
    }

    private fun computeTickRate(): ClockTickRate {
        return layers.map { it.config.tickRate }.minByOrNull { it.value }
            ?: ClockTickRate.PER_MINUTE
    }

    private fun createEvents(): ClockEventUnion {
        return object : ClockEventUnion {
            override fun onTimeTick() {
                timeFormatter.timeKeeper.updateTime()
                if (
                    config.tickRate == ClockTickRate.PER_MINUTE ||
                        view.contentDescription != timespecHandler.getContentDescription()
                ) {
                    view.contentDescription = timespecHandler.getContentDescription()
                }
                layers.forEach { it.faceEvents.onTimeTick() }
            }

            override fun onThemeChanged(theme: ThemeConfig) {
                this@CustomClockFaceController.theme = theme
                clockCtx.assets.setSeedColor(theme.seedColor, clockCtx.assets.style)
                layers.forEach { it.faceEvents.onThemeChanged(theme) }
            }

            override fun onFontSettingChanged(fontSizePx: Float) {
                layers.forEach { it.faceEvents.onFontSettingChanged(fontSizePx) }
            }

            override fun onTargetRegionChanged(targetRegion: VRect) {
                if (view is DigitalFaceViewGroup && view.isAlignedWithScreen) {
                    val topMargin =
                        view.context.resources.getDimensionPixelSize(
                            clocksR.dimen.keyguard_large_clock_top_margin
                        )
                    val diff = view.computeLayoutDiff(targetRegion, isLargeClock)
                    if (diff.y.toInt() != 0) {
                        view.translationY = diff.y - topMargin / 2f
                    }
                    return
                }
                var maxWidth = 0f
                var maxHeight = 0f
                layers.forEach { controller ->
                    controller.faceEvents.onTargetRegionChanged(targetRegion)
                    maxWidth = max(maxWidth, controller.view.layoutParams.width.toFloat())
                    maxHeight = max(maxHeight, controller.view.layoutParams.height.toFloat())
                }
                val layoutParams =
                    if (maxHeight <= 0f || maxWidth <= 0f) {
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                        )
                    } else {
                        val scale =
                            if (
                                maxWidth / maxHeight >
                                    targetRegion.width.toFloat() / targetRegion.height.toFloat()
                            ) {
                                targetRegion.width / maxWidth
                            } else {
                                targetRegion.height / maxHeight
                            }
                        FrameLayout.LayoutParams(
                            (maxWidth * scale).toInt(),
                            (maxHeight * scale).toInt(),
                        )
                    }
                layoutParams.gravity = Gravity.CENTER
                view.layoutParams = layoutParams
                view.translationY = view.computeLayoutDiff(targetRegion, isLargeClock).y
            }

            override fun onSecondaryDisplayChanged(onSecondaryDisplay: Boolean) = Unit

            override fun onTimeZoneChanged(timeZone: TimeZone) {
                timeFormatter.timeKeeper.timeZone = timeZone
                layers.forEach { it.events.onTimeZoneChanged(timeZone) }
            }

            override fun onTimeFormatChanged(formatKind: TimeFormatKind) {
                timeFormatter.formatKind = formatKind
                layers.forEach { it.events.onTimeFormatChanged(formatKind) }
            }

            override fun onLocaleChanged(locale: Locale) {
                timeFormatter.locale = locale
                layers.forEach { it.events.onLocaleChanged(locale) }
            }

            override fun onWeatherDataChanged(data: WeatherData) {
                layers.forEach { it.events.onWeatherDataChanged(data) }
            }

            override fun onAlarmDataChanged(data: AlarmData) {
                layers.forEach { it.events.onAlarmDataChanged(data) }
            }

            override fun onZenDataChanged(data: ZenData) {
                layers.forEach { it.events.onZenDataChanged(data) }
            }
        }
    }

    private fun View.computeLayoutDiff(targetRegion: VRect, largeClock: Boolean): VPointF {
        return with(ViewUtils) { computeLayoutDiff(targetRegion, largeClock) }
    }

    private fun createAnimations(): ClockAnimations {
        return object : ClockAnimations {
            override fun enter() {
                layers.forEach { it.animations.enter() }
            }

            override fun doze(fraction: Float) {
                layers.forEach { it.animations.doze(fraction) }
            }

            override fun fold(fraction: Float) {
                layers.forEach { it.animations.fold(fraction) }
            }

            override fun charge() {
                layers.forEach { it.animations.charge() }
            }

            override fun onPickerCarouselSwiping(swipingFraction: Float) {
                clockFace.pickerScale.let { pickerScale ->
                    view.scaleX = (1f - pickerScale.scaleX) * swipingFraction + pickerScale.scaleX
                    view.scaleY = (1f - pickerScale.scaleY) * swipingFraction + pickerScale.scaleY
                }
                if (view !is DigitalFaceViewGroup || !view.isAlignedWithScreen) {
                    view.translationY =
                        (view.context.resources.getDimensionPixelSize(
                            clocksR.dimen.keyguard_large_clock_top_margin
                        ) / 2f) * swipingFraction
                }
                layers.forEach { it.animations.onPickerCarouselSwiping(swipingFraction) }
                view.invalidate()
            }

            override fun onPositionAnimated(args: ClockPositionAnimationArgs) {
                layers.forEach { it.animations.onPositionAnimated(args) }
            }

            override fun onFidgetTap(x: Float, y: Float) = Unit

            override fun onFontAxesChanged(style: ClockAxisStyle) = Unit
        }
    }
}
