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
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import com.android.systemui.clocks.ClockFace
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.DigitalHandLayer
import com.android.systemui.clocks.view.DigitalFaceViewGroup
import com.android.systemui.customization.clocks.DigitalTimeFormatter
import com.android.systemui.customization.clocks.DigitalTimespec
import com.android.systemui.customization.clocks.DigitalTimespecHandler
import com.android.systemui.customization.clocks.R as clocksR
import com.android.systemui.customization.clocks.utils.ViewUtils.computeLayoutDiff
import com.android.systemui.customization.clocks.utils.ViewUtils.translation
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
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPositionAnimationArgs
import com.android.systemui.plugins.keyguard.ui.clocks.ClockTickRate
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig
import com.android.systemui.plugins.keyguard.ui.clocks.TimeFormatKind
import java.util.Locale
import kotlin.math.max

class CustomClockFaceController(
    clockCtx: CustomClockContext,
    face: ClockFace,
    isLargeClock: Boolean,
) : ClockFaceController {
    interface ClockEventUnion : ClockEvents, ClockFaceEvents

    override var theme = ThemeConfig(true, clockCtx.assets.seedColor)
    val layers = mutableListOf<ClockLayerController>()
    val timeFormatter = DigitalTimeFormatter("hh:mm", clockCtx.timeKeeper, true)
    val timespecHandler = DigitalTimespecHandler(DigitalTimespec.TIME_FULL_FORMAT, timeFormatter)

    override val view: View = run {
        val layoutParams =
            FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT).apply { gravity = Gravity.CENTER }

        if (face.layers.size == 1) {
            val layer =
                clockCtx.factory.createLayerController(clockCtx, face.layers[0], isLargeClock)
            layers.add(layer)
            layer.view.layoutParams = layoutParams
            layer.view
        } else {
            clockCtx.factory.createViewGroup(clockCtx, face).apply {
                this.layoutParams = layoutParams
                clipChildren = false
                for (layer in face.layers) {
                    val faceLayer =
                        if (layer is DigitalHandLayer && layer.faceLayout == null) {
                            layer.copy(faceLayout = face.faceLayout)
                        } else {
                            layer
                        }
                    val controller =
                        clockCtx.factory.createLayerController(clockCtx, faceLayer, isLargeClock)
                    addView(controller.view)
                    layers.add(controller)
                }
            }
        }
    }

    override val config =
        ClockFaceConfig(
            tickRate = tickRate,
            hasCustomWeatherDataDisplay = layers.any { it.config.hasCustomWeatherDataDisplay },
            hasCustomPositionUpdatedAnimation =
                layers.any { it.config.hasCustomPositionUpdatedAnimation },
            useCustomClockScene = layers.any { it.config.useCustomClockScene },
        )

    init {
        view.id =
            if (isLargeClock) {
                ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE
            } else {
                ClockViewIds.LOCKSCREEN_CLOCK_VIEW_SMALL
            }
    }

    override val layout = clockCtx.factory.createLayout(clockCtx, face, view)

    override val events =
        object : ClockEventUnion {
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

            override fun onFontSettingChanged(fontSizePx: Float) {
                layers.forEach { it.faceEvents.onFontSettingChanged(fontSizePx) }
            }

            override fun onThemeChanged(theme: ThemeConfig) {
                this@CustomClockFaceController.theme = theme
                clockCtx.assets.setSeedColor(theme.seedColor, clockCtx.assets.style)
                layers.forEach { it.faceEvents.onThemeChanged(theme) }
            }

            override fun onTargetRegionChanged(targetRegion: VRect) {
                if (
                    view is DigitalFaceViewGroup<*> &&
                        (view as DigitalFaceViewGroup<*>).isAlignedWithScreen
                ) {
                    val topMargin =
                        view.context.resources.getDimensionPixelSize(
                            clocksR.dimen.keyguard_large_clock_top_margin
                        )
                    val (_, dy) = view.computeLayoutDiff(targetRegion, isLargeClock)
                    if (dy.toInt() != 0) {
                        view.translationY = dy - topMargin / 2
                    }
                    return
                }

                var maxHeight = 0f
                var maxWidth = 0f
                for (layer in layers) {
                    layer.faceEvents.onTargetRegionChanged(targetRegion)
                    maxWidth = max(maxWidth, layer.view.layoutParams.width.toFloat())
                    maxHeight = max(maxHeight, layer.view.layoutParams.height.toFloat())
                }

                val layoutParams =
                    if (maxHeight <= 0f || maxWidth <= 0f) {
                        FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    } else {
                        val scale =
                            if (maxWidth / maxHeight > targetRegion.width / targetRegion.height) {
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
                view.translation = view.computeLayoutDiff(targetRegion, isLargeClock)
            }

            override fun onSecondaryDisplayChanged(onSecondaryDisplay: Boolean) {}

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

    override val animations =
        object : ClockAnimations {
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
                face.pickerScale?.let {
                    view.scaleX = (1f - it.scaleX) * swipingFraction + it.scaleX
                    view.scaleY = (1f - it.scaleY) * swipingFraction + it.scaleY
                }
                if (
                    view !is DigitalFaceViewGroup<*> ||
                        !(view as DigitalFaceViewGroup<*>).isAlignedWithScreen
                ) {
                    view.translationY =
                        view.context.resources.getDimensionPixelSize(
                            clocksR.dimen.keyguard_large_clock_top_margin
                        ) / 2f * swipingFraction
                }
                layers.forEach { it.animations.onPickerCarouselSwiping(swipingFraction) }
                view.invalidate()
            }

            override fun onPositionAnimated(anim: ClockPositionAnimationArgs) {
                layers.forEach { it.animations.onPositionAnimated(anim) }
            }

            override fun onFidgetTap(x: Float, y: Float) {}

            override fun onFontAxesChanged(style: ClockAxisStyle) {}
        }

    private val tickRate: ClockTickRate
        get() =
            layers.map { it.config.tickRate }.minByOrNull { it.value } ?: ClockTickRate.PER_MINUTE
}
