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
import com.android.systemui.customization.clocks.ClockLogger
import com.android.systemui.customization.clocks.view.IDigitalClockView
import com.android.systemui.plugins.keyguard.data.model.AlarmData
import com.android.systemui.plugins.keyguard.data.model.WeatherData
import com.android.systemui.plugins.keyguard.data.model.ZenData
import com.android.systemui.plugins.keyguard.ui.clocks.ClockConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockController
import com.android.systemui.plugins.keyguard.ui.clocks.ClockEventListeners
import com.android.systemui.plugins.keyguard.ui.clocks.ClockEvents
import com.android.systemui.plugins.keyguard.ui.clocks.ClockMessageBuffers
import com.android.systemui.plugins.keyguard.ui.clocks.TimeFormatKind
import java.io.PrintWriter
import java.util.Locale

class CustomClockController(
    clockCtx: CustomClockContext,
    design: ClockDesign,
    messageBuffers: ClockMessageBuffers?,
) : ClockController {
    override val smallClock =
        CustomClockFaceController(
            clockCtx.copy(
                messageBuffers?.smallClockMessageBuffer ?: ClockLogger.DEFAULT_MESSAGE_BUFFER
            ),
            design.small ?: design.large,
            isLargeClock = false,
        )

    override val largeClock =
        CustomClockFaceController(
            clockCtx.copy(
                messageBuffers?.largeClockMessageBuffer ?: ClockLogger.DEFAULT_MESSAGE_BUFFER
            ),
            design.large,
            isLargeClock = true,
        )

    override val config =
        ClockConfig(
            id = design.id,
            name = design.name?.let { clockCtx.assets.tryReadString(it) ?: it } ?: "",
            description = design.description?.let { clockCtx.assets.tryReadString(it) ?: it } ?: "",
            useAlternateSmartspaceAODTransition =
                smallClock.config.hasCustomWeatherDataDisplay ||
                    largeClock.config.hasCustomWeatherDataDisplay,
            useCustomClockScene =
                smallClock.config.useCustomClockScene || largeClock.config.useCustomClockScene,
        )

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

    override fun initialize(isDarkTheme: Boolean, dozeFraction: Float, foldFraction: Float) {
        fun CustomClockFaceController.registerCallbacks(isLargeClock: Boolean) {
            if (view is IDigitalClockView) {
                (view as IDigitalClockView).onViewBoundsChanged = { bounds ->
                    eventListeners.fire { onBoundsChanged(bounds, isLargeClock) }
                }
                (view as IDigitalClockView).onViewMaxSizeChanged = { maxSize ->
                    eventListeners.fire { onMaxSizeChanged(maxSize, isLargeClock) }
                }
                return
            }

            layers.forEach { layer ->
                layer.onViewBoundsChanged = { bounds ->
                    eventListeners.fire { onBoundsChanged(bounds, true) }
                }
                layer.onViewMaxSizeChanged = { maxSize ->
                    eventListeners.fire { onMaxSizeChanged(maxSize, true) }
                }
            }
        }

        smallClock.run {
            registerCallbacks(isLargeClock = false)
            events.onThemeChanged(theme.copy(isDarkTheme = isDarkTheme))
            animations.doze(dozeFraction)
            animations.fold(foldFraction)
            events.onTimeTick()
        }

        largeClock.run {
            registerCallbacks(isLargeClock = true)
            events.onThemeChanged(theme.copy(isDarkTheme = isDarkTheme))
            animations.doze(dozeFraction)
            animations.fold(foldFraction)
            events.onTimeTick()
        }
    }

    override fun dump(pw: PrintWriter) {}
}
