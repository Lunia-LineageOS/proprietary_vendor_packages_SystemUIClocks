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
import android.widget.ImageView
import android.widget.RelativeLayout
import com.android.systemui.clocks.AssetDrawable
import com.android.systemui.clocks.AssetLayer
import com.android.systemui.clocks.CustomClockContext
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
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig
import com.android.systemui.plugins.keyguard.ui.clocks.TimeFormatKind
import java.util.Locale

class AssetLayerController(clockCtx: CustomClockContext, layer: AssetLayer) : ClockLayerController {
    override val view: View = ImageView(clockCtx.context)
    val asset = AssetDrawable(clockCtx.assets, layer.asset)
    override val config = ClockFaceConfig()
    override var onViewBoundsChanged: ((VRectF) -> Unit)? = null
    override var onViewMaxSizeChanged: ((VPointF) -> Unit)? = null

    init {
        (view as ImageView).setImageDrawable(asset)
        view.layoutParams =
            RelativeLayout.LayoutParams(asset.intrinsicWidth, asset.intrinsicHeight).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            }
    }

    override val events =
        object : ClockEvents {
            override fun onTimeZoneChanged(timeZone: TimeZone) {}

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
            override fun onTimeTick() {}

            override fun onThemeChanged(theme: ThemeConfig) {
                asset.lightFraction = if (theme.isDarkTheme) 1f else 0f
                asset.updateTints()
            }

            override fun onFontSettingChanged(fontSizePx: Float) {}

            override fun onTargetRegionChanged(targetRegion: VRect) {}

            override fun onSecondaryDisplayChanged(onSecondaryDisplay: Boolean) {}
        }
}
