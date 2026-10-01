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
import com.android.systemui.clocks.ComposedDigitalHandLayer
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.view.DigitalFaceViewGroup
import com.android.systemui.customization.clocks.AnimationState
import com.android.systemui.plugins.keyguard.VRect
import com.android.systemui.plugins.keyguard.data.model.AlarmData
import com.android.systemui.plugins.keyguard.data.model.WeatherData
import com.android.systemui.plugins.keyguard.data.model.ZenData
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAnimations
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAxisStyle
import com.android.systemui.plugins.keyguard.ui.clocks.ClockEvents
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceEvents
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPositionAnimationArgs
import com.android.systemui.plugins.keyguard.ui.clocks.ClockTickRate
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig
import com.android.systemui.plugins.keyguard.ui.clocks.TimeFormatKind
import java.util.Locale

class ComposedDigitalLayerController(
    private val clockCtx: CustomClockContext,
    layer: ComposedDigitalHandLayer,
    isLargeClock: Boolean,
) : ClockLayerController {
    private val layerControllers = mutableListOf<ClockLayerController>()
    val dozeState = AnimationState(1f)
    override val view: DigitalFaceViewGroup =
        clockCtx.factory.createComposedView(clockCtx, layer, dozeState)

    override val config: ClockFaceConfig

    override val animations =
        object : ClockAnimations {
            override fun enter() {
                refreshTime()
            }

            override fun doze(fraction: Float) {
                val (wasActive, hasJumped) = dozeState.update(fraction)
                if (wasActive != dozeState.isActive) {
                    view.animateDoze(dozeState.isActive, !hasJumped)
                }
                view.dozeFraction = fraction
                view.invalidate()
            }

            override fun fold(fraction: Float) {
                refreshTime()
            }

            override fun charge() {
                view.animateCharge()
            }

            override fun onPickerCarouselSwiping(swipingFraction: Float) {
                view.onPickerCarouselSwiping(swipingFraction)
            }

            override fun onPositionAnimated(args: ClockPositionAnimationArgs) {
                view.onPositionAnimated(args)
            }

            override fun onFidgetTap(x: Float, y: Float) = Unit

            override fun onFontAxesChanged(style: ClockAxisStyle) = Unit
        }
    override val events =
        object : ClockEvents {
            override fun onTimeZoneChanged(timeZone: TimeZone) {
                layerControllers.forEach { it.events.onTimeZoneChanged(timeZone) }
                refreshTime()
            }

            override fun onTimeFormatChanged(formatKind: TimeFormatKind) {
                layerControllers.forEach { it.events.onTimeFormatChanged(formatKind) }
                refreshTime()
            }

            override fun onLocaleChanged(locale: Locale) {
                layerControllers.forEach { it.events.onLocaleChanged(locale) }
                view.onLocaleChanged(locale)
                refreshTime()
            }

            override fun onWeatherDataChanged(data: WeatherData) {
                view.onWeatherDataChanged(data)
            }

            override fun onAlarmDataChanged(data: AlarmData) {
                view.onAlarmDataChanged(data)
            }

            override fun onZenDataChanged(data: ZenData) {
                view.onZenDataChanged(data)
            }
        }
    override val faceEvents =
        object : ClockFaceEvents {
            override fun onTimeTick() {
                refreshTime()
            }

            override fun onThemeChanged(theme: ThemeConfig) {
                view.updateTheme(theme)
            }

            override fun onFontSettingChanged(fontSizePx: Float) {
                view.onFontSettingChanged(fontSizePx)
            }

            override fun onTargetRegionChanged(targetRegion: VRect) = Unit

            override fun onSecondaryDisplayChanged(onSecondaryDisplay: Boolean) = Unit
        }

    init {
        layer.digitalLayers.forEach { digitalLayer ->
            val controller =
                clockCtx.factory.createLayerController(clockCtx, digitalLayer, isLargeClock)
            view.addView(controller.view)
            layerControllers.add(controller)
        }
        config =
            ClockFaceConfig(
                tickRate = ClockTickRate.PER_MINUTE,
                hasCustomWeatherDataDisplay = view.hasCustomWeatherDataDisplay,
                hasCustomPositionUpdatedAnimation = view.hasCustomPositionUpdatedAnimation,
                useCustomClockScene = view.useCustomClockScene,
            )
    }

    fun refreshTime() {
        layerControllers.forEach { it.faceEvents.onTimeTick() }
        view.refreshTime()
    }
}
