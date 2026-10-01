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
import com.android.systemui.clocks.ClockDesign
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.customization.clocks.view.IDigitalClockView
import com.android.systemui.plugins.keyguard.data.model.AlarmData
import com.android.systemui.plugins.keyguard.data.model.WeatherData
import com.android.systemui.plugins.keyguard.data.model.ZenData
import com.android.systemui.plugins.keyguard.ui.clocks.ClockConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockController
import com.android.systemui.plugins.keyguard.ui.clocks.ClockEventListeners
import com.android.systemui.plugins.keyguard.ui.clocks.ClockEvents
import com.android.systemui.plugins.keyguard.ui.clocks.ClockMessageBuffers
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig
import com.android.systemui.plugins.keyguard.ui.clocks.TimeFormatKind
import java.io.PrintWriter
import java.util.Locale

class CustomClockController(
    private val clockCtx: CustomClockContext,
    private val design: ClockDesign,
    messageBuffers: ClockMessageBuffers?,
) : ClockController {
    override val smallClock: CustomClockFaceController
    override val largeClock: CustomClockFaceController

    override val config: ClockConfig
    override val eventListeners = ClockEventListeners()

    override val events =
        object : ClockEvents {
            override fun onTimeZoneChanged(timeZone: TimeZone) {
                smallClock.events.onTimeZoneChanged(timeZone)
                largeClock.events.onTimeZoneChanged(timeZone)
            }

            override fun onTimeFormatChanged(formatKind: TimeFormatKind) {
                smallClock.events.onTimeFormatChanged(formatKind)
                largeClock.events.onTimeFormatChanged(formatKind)
            }

            override fun onLocaleChanged(locale: Locale) {
                smallClock.events.onLocaleChanged(locale)
                largeClock.events.onLocaleChanged(locale)
            }

            override fun onWeatherDataChanged(data: WeatherData) {
                smallClock.events.onWeatherDataChanged(data)
                largeClock.events.onWeatherDataChanged(data)
            }

            override fun onAlarmDataChanged(data: AlarmData) {
                smallClock.events.onAlarmDataChanged(data)
                largeClock.events.onAlarmDataChanged(data)
            }

            override fun onZenDataChanged(data: ZenData) {
                smallClock.events.onZenDataChanged(data)
                largeClock.events.onZenDataChanged(data)
            }
        }

    init {
        val smallBuffer =
            messageBuffers?.smallClockMessageBuffer ?: messageBuffers?.infraMessageBuffer
        smallClock =
            CustomClockFaceController(
                clockCtx.copy(smallBuffer ?: clockCtx.messageBuffer),
                design.small ?: design.large,
                isLargeClock = false,
            )
        val largeBuffer =
            messageBuffers?.largeClockMessageBuffer ?: messageBuffers?.infraMessageBuffer
        largeClock =
            CustomClockFaceController(
                clockCtx.copy(largeBuffer ?: clockCtx.messageBuffer),
                design.large,
                isLargeClock = true,
            )

        config =
            ClockConfig(
                design.id,
                clockCtx.assets.getString(design.name),
                clockCtx.assets.getString(design.description),
                smallClock.config.hasCustomWeatherDataDisplay ||
                    largeClock.config.hasCustomWeatherDataDisplay,
                smallClock.config.useCustomClockScene || largeClock.config.useCustomClockScene,
            )
    }

    private fun registerCallbacks(face: CustomClockFaceController, isLarge: Boolean) {
        val view = face.view
        if (view is IDigitalClockView) {
            view.onViewBoundsChanged = { bounds ->
                eventListeners.fire { it.onBoundsChanged(bounds, isLarge) }
            }
            view.onViewMaxSizeChanged = { size ->
                eventListeners.fire { it.onMaxSizeChanged(size, isLarge) }
            }
        } else {
            face.layers.forEach { layer ->
                layer.onViewBoundsChanged = { bounds ->
                    eventListeners.fire { it.onBoundsChanged(bounds, isLarge) }
                }
                layer.onViewMaxSizeChanged = { size ->
                    eventListeners.fire { it.onMaxSizeChanged(size, isLarge) }
                }
            }
        }
    }

    override fun initialize(isDarkTheme: Boolean, dozeFraction: Float, foldFraction: Float) {
        registerCallbacks(smallClock, false)
        smallClock.run {
            events.onThemeChanged(ThemeConfig(isDarkTheme = isDarkTheme, theme.seedColor))
            animations.doze(dozeFraction)
            animations.fold(foldFraction)
            events.onTimeTick()
        }
        registerCallbacks(largeClock, true)
        largeClock.run {
            events.onThemeChanged(ThemeConfig(isDarkTheme = isDarkTheme, theme.seedColor))
            animations.doze(dozeFraction)
            animations.fold(foldFraction)
            events.onTimeTick()
        }
    }

    override fun dump(pw: PrintWriter) {}
}
