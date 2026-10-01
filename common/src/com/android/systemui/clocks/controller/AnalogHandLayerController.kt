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
import android.widget.ImageView
import android.widget.RelativeLayout
import com.android.systemui.clocks.AnalogHandLayer
import com.android.systemui.clocks.AssetDrawable
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.customization.clocks.AnalogTimespec
import com.android.systemui.customization.clocks.AnalogTimespecHandler
import com.android.systemui.log.core.Logger
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
import kotlin.math.max

class AnalogHandLayerController(
    private val clockCtx: CustomClockContext,
    private val layer: AnalogHandLayer,
) : ClockLayerController {
    private val logger = Logger(clockCtx.messageBuffer, TAG)
    val timespec = AnalogTimespecHandler(layer.timespec, layer.tickMode, clockCtx.timeKeeper)

    val asset: AssetDrawable = AssetDrawable(clockCtx.assets, layer.asset)
    override val view: ImageView = ImageView(clockCtx.context)

    override val config: ClockFaceConfig

    override val animations =
        object : ClockAnimations {
            override fun enter() = Unit

            override fun doze(fraction: Float) {
                asset.dozeFraction = fraction
            }

            override fun fold(fraction: Float) = Unit

            override fun charge() = Unit

            override fun onPickerCarouselSwiping(swipingFraction: Float) = Unit

            override fun onPositionAnimated(args: ClockPositionAnimationArgs) = Unit

            override fun onFidgetTap(x: Float, y: Float) = Unit

            override fun onFontAxesChanged(style: ClockAxisStyle) = Unit
        }
    override val events =
        object : ClockEvents {
            override fun onTimeZoneChanged(timeZone: TimeZone) {
                timespec.timeKeeper.timeZone = timeZone
                faceEvents.onTimeTick()
            }

            override fun onTimeFormatChanged(formatKind: TimeFormatKind) = Unit

            override fun onLocaleChanged(locale: Locale) = Unit

            override fun onWeatherDataChanged(data: WeatherData) = Unit

            override fun onAlarmDataChanged(data: AlarmData) = Unit

            override fun onZenDataChanged(data: ZenData) = Unit
        }
    override val faceEvents =
        object : ClockFaceEvents {
            override fun onTimeTick() {
                timespec.timeKeeper.updateTime()
                view.rotation = timespec.rotation * 360f
                logger.d({ "onTimeTick: new rotation=$double1 (degrees)" }) {
                    double1 = view.rotation.toDouble()
                }
            }

            override fun onThemeChanged(theme: ThemeConfig) {
                asset.lightFraction = if (theme.isDarkTheme) 1f else 0f
                asset.updateTints()
            }

            override fun onFontSettingChanged(fontSizePx: Float) = Unit

            override fun onTargetRegionChanged(targetRegion: VRect) = Unit

            override fun onSecondaryDisplayChanged(onSecondaryDisplay: Boolean) = Unit
        }

    init {
        view.setImageDrawable(asset)
        val size = max(asset.intrinsicHeight, asset.intrinsicWidth)
        val layoutParams = RelativeLayout.LayoutParams(size, size)
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        view.layoutParams = layoutParams
        config =
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
    }

    private companion object {
        const val TAG = "AnalogHandLayerController"
    }
}
