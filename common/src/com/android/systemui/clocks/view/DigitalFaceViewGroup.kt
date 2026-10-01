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

import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.customization.clocks.view.DigitalClockTextView
import com.android.systemui.customization.clocks.view.DigitalClockViewGroup
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig

abstract class DigitalFaceViewGroup(clockCtx: CustomClockContext) :
    DigitalClockViewGroup<CustomDigitalTextView>(clockCtx) {
    protected val dozeControlState = DozeControlState()

    open val hasCustomPositionUpdatedAnimation: Boolean
        get() = false

    open val hasCustomWeatherDataDisplay: Boolean
        get() = false

    open val useCustomClockScene: Boolean
        get() = false

    open val isAlignedWithScreen: Boolean
        get() = false

    open fun onPickerCarouselSwiping(swipingFraction: Float) = Unit

    open fun onPositionAnimated(
        args: com.android.systemui.plugins.keyguard.ui.clocks.ClockPositionAnimationArgs
    ) = Unit

    open fun onWeatherDataChanged(
        data: com.android.systemui.plugins.keyguard.data.model.WeatherData
    ) = Unit

    open fun onAlarmDataChanged(data: com.android.systemui.plugins.keyguard.data.model.AlarmData) =
        Unit

    open fun onZenDataChanged(data: com.android.systemui.plugins.keyguard.data.model.ZenData) = Unit

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        dozeControlState.animateReady = true
    }

    override fun onLocaleChanged(locale: java.util.Locale) {
        refreshTime()
    }

    override fun animateDoze(isDozing: Boolean, isAnimated: Boolean) {
    }

    override fun updateAxes(
        axes: com.android.systemui.plugins.keyguard.ui.clocks.ClockAxisStyle,
        isAnimated: Boolean,
    ) {
        children.forEach { it.notifyAxesChanged(axes, isAnimated) }
    }

    override fun animateCharge() {
        children.forEach { it.animateCharge() }
    }

    override fun onFontSettingChanged(fontSizePx: Float) {
        children.forEach { it.applyCustomTextSize(fontSizePx, constrainedByHeight = false) }
    }

    override fun updateColor(lockscreenColor: Int, aodColor: Int) {
        children.forEach { child ->
            (child as DigitalClockTextView).updateColor(lockscreenColor, aodColor)
        }
    }

    fun updateTheme(theme: ThemeConfig) {
        children.forEach { it.updateTheme(theme) }
        invalidate()
    }

    protected class DozeControlState {
        private var animateDoze: () -> Unit = {}
        private var ready = false

        var animateReady: Boolean
            get() = ready
            set(value) {
                if (value) {
                    animateDoze()
                    animateDoze = {}
                }
                ready = value
            }

        fun setAnimateDoze(block: () -> Unit) {
            if (!ready) {
                animateDoze = block
            } else {
                block()
            }
        }
    }
}
