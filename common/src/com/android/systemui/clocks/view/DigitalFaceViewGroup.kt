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

package com.android.systemui.clocks.view

import android.annotation.SuppressLint
import android.view.View
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.customization.clocks.view.DigitalClockViewGroup
import com.android.systemui.plugins.keyguard.data.model.AlarmData
import com.android.systemui.plugins.keyguard.data.model.WeatherData
import com.android.systemui.plugins.keyguard.data.model.ZenData
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAxisStyle
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPositionAnimationArgs
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig
import java.util.Locale

@SuppressLint("ViewConstructor")
abstract class DigitalFaceViewGroup<TChild>(clockCtx: CustomClockContext) :
    DigitalClockViewGroup<TChild>(clockCtx) where TChild : View, TChild : ICustomDigitalTextView {
    val dozeControlState = DozeControlState()

    open val hasCustomWeatherDataDisplay: Boolean
        get() = false

    open val hasCustomPositionUpdatedAnimation: Boolean
        get() = false

    open val useCustomClockScene: Boolean
        get() = false

    open val isAlignedWithScreen: Boolean
        get() = false

    override fun refreshTime() {
        logger.refreshTime()
        children.forEach { it.refreshText() }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        dozeControlState.animateReady = true
    }

    override fun animateDoze(isDozing: Boolean, isAnimated: Boolean) {
        children.forEach { it.animateDoze(isDozing, isAnimated) }
    }

    override fun animateCharge() {
        children.forEach { it.animateCharge() }
    }

    fun updateTheme(theme: ThemeConfig) {
        children.forEach { it.updateTheme(theme) }
        invalidate()
    }

    override fun onFontSettingChanged(fontSizePx: Float) {
        children.forEach { it.applyTextSize(fontSizePx, false) }
    }

    override fun onLocaleChanged(locale: Locale) {}

    override fun updateAxes(axes: ClockAxisStyle, isAnimated: Boolean) {}

    override fun updateColor(lockscreenColor: Int, aodColor: Int) {}

    open fun onWeatherDataChanged(data: WeatherData) {}

    open fun onAlarmDataChanged(data: AlarmData) {}

    open fun onZenDataChanged(data: ZenData) {}

    open fun onPositionAnimated(anim: ClockPositionAnimationArgs) {}

    open fun onPickerCarouselSwiping(swipingFraction: Float) {}

    class DozeControlState {
        var animateDoze: () -> Unit = {}
            set(value) {
                if (animateReady) {
                    value()
                    field = {}
                } else {
                    field = value
                }
            }

        var animateReady = false
            set(value) {
                if (value) {
                    animateDoze()
                    animateDoze = {}
                }
                field = value
            }
    }
}
