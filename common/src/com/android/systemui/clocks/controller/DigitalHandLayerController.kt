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
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.RelativeLayout
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.DigitalFaceLayout
import com.android.systemui.clocks.DigitalHandLayer
import com.android.systemui.clocks.lookupDigitalLayerId
import com.android.systemui.clocks.view.ICustomDigitalTextView
import com.android.systemui.customization.clocks.AnimationState
import com.android.systemui.customization.clocks.DigitalDateFormatter
import com.android.systemui.customization.clocks.DigitalFormatter
import com.android.systemui.customization.clocks.DigitalTimeFormatter
import com.android.systemui.customization.clocks.DigitalTimespec
import com.android.systemui.customization.clocks.DigitalTimespecHandler
import com.android.systemui.customization.clocks.view.IDigitalClockTextView
import com.android.systemui.customization.clocks.view.IDigitalClockView
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
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig
import com.android.systemui.plugins.keyguard.ui.clocks.TimeFormatKind
import java.util.Locale

open class DigitalHandLayerController(
    clockCtx: CustomClockContext,
    private val layer: DigitalHandLayer,
    final override val view: View,
) : ClockLayerController {
    private val logger = Logger(clockCtx.messageBuffer, TAG)

    val timeFormatter: DigitalFormatter =
        if (layer.timespec == DigitalTimespec.DATE_FORMAT) {
            DigitalDateFormatter(layer.dateTimeFormat, clockCtx.timeKeeper, true) {
                it.uppercase(Locale.ROOT)
            }
        } else {
            DigitalTimeFormatter(layer.dateTimeFormat, clockCtx.timeKeeper, true)
        }

    val timespecHandler = DigitalTimespecHandler(layer.timespec, timeFormatter)
    override val config = ClockFaceConfig()
    var dozeState: AnimationState? = null

    override var onViewBoundsChanged: ((VRectF) -> Unit)?
        get() = (view as IDigitalClockView).onViewBoundsChanged
        set(value) {
            (view as IDigitalClockView).onViewBoundsChanged = value
        }

    override var onViewMaxSizeChanged: ((VPointF) -> Unit)?
        get() = (view as IDigitalClockView).onViewMaxSizeChanged
        set(value) {
            (view as IDigitalClockView).onViewMaxSizeChanged = value
        }

    init {
        view.layoutParams = RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        layer.alignment?.let { alignment ->
            alignment.verticalAlignment?.let {
                (view as ICustomDigitalTextView).verticalAlignment = it
            }
            alignment.horizontalAlignment?.let {
                (view as ICustomDigitalTextView).horizontalAlignment = it
            }
        }
        (view as ICustomDigitalTextView).applyStyles(layer.style, layer.aodStyle)
        view.id = lookupDigitalLayerId(layer)
        layer.timeZoneId?.let { timeFormatter.overrideTimeZone = TimeZone.getTimeZone(it) }
    }

    override val events =
        object : ClockEvents {
            override fun onTimeZoneChanged(timeZone: TimeZone) {
                timeFormatter.timeKeeper.timeZone = timeZone
                refreshTime()
            }

            override fun onTimeFormatChanged(formatKind: TimeFormatKind) {
                timeFormatter.formatKind = formatKind
                refreshTime()
            }

            override fun onLocaleChanged(locale: Locale) {
                timeFormatter.locale = locale
                refreshTime()
            }

            override fun onWeatherDataChanged(data: WeatherData) {}

            override fun onAlarmDataChanged(data: AlarmData) {}

            override fun onZenDataChanged(data: ZenData) {}
        }

    override val animations =
        object : ClockAnimations {
            override fun enter() {
                applyLayout(layer.faceLayout)
                refreshTime()
            }

            override fun doze(fraction: Float) {
                if (dozeState == null) {
                    dozeState = AnimationState(fraction)
                    (view as ICustomDigitalTextView).animateDoze(dozeState!!.isActive, false)
                } else {
                    val (hasChanged, hasJumped) = dozeState!!.update(fraction)
                    if (hasChanged) {
                        (view as ICustomDigitalTextView).animateDoze(
                            dozeState!!.isActive,
                            !hasJumped,
                        )
                    }
                }
                (view as IDigitalClockView).dozeFraction = fraction
            }

            override fun fold(fraction: Float) {
                applyLayout(layer.faceLayout)
                refreshTime()
            }

            override fun charge() {
                (view as IDigitalClockTextView).animateCharge()
            }

            override fun onPositionAnimated(anim: ClockPositionAnimationArgs) {}

            override fun onPickerCarouselSwiping(swipingFraction: Float) {}

            override fun onFidgetTap(x: Float, y: Float) {}

            override fun onFontAxesChanged(style: ClockAxisStyle) {}
        }

    override val faceEvents =
        object : ClockFaceEvents {
            override fun onTimeTick() {
                refreshTime()
                if (
                    layer.timespec == DigitalTimespec.TIME_FULL_FORMAT ||
                        layer.timespec == DigitalTimespec.DATE_FORMAT
                ) {
                    view.contentDescription = timespecHandler.getContentDescription()
                }
            }

            override fun onThemeChanged(theme: ThemeConfig) {
                (view as ICustomDigitalTextView).updateTheme(theme)
            }

            override fun onFontSettingChanged(fontSizePx: Float) {
                (view as ICustomDigitalTextView).applyTextSize(fontSizePx, false)
                applyMargin()
            }

            override fun onTargetRegionChanged(targetRegion: VRect) {}

            override fun onSecondaryDisplayChanged(onSecondaryDisplay: Boolean) {}
        }

    fun applyLayout(faceLayout: DigitalFaceLayout?) {
        when (faceLayout) {
            DigitalFaceLayout.FOUR_DIGITS_ALIGN_CENTER,
            DigitalFaceLayout.FOUR_DIGITS_HORIZONTAL -> applyFourDigitsLayout(faceLayout)
            DigitalFaceLayout.TWO_PAIRS_HORIZONTAL,
            DigitalFaceLayout.TWO_PAIRS_VERTICAL -> applyTwoPairsLayout(faceLayout)
            else -> {}
        }
        applyMargin()
    }

    private fun applyMargin() {
        val layoutParams = view.layoutParams as? RelativeLayout.LayoutParams ?: return
        layer.marginRatio?.let {
            layoutParams.setMargins(
                (it.left * view.measuredWidth).toInt(),
                (it.top * view.measuredHeight).toInt(),
                (it.right * view.measuredWidth).toInt(),
                (it.bottom * view.measuredHeight).toInt(),
            )
        }
        view.layoutParams = layoutParams
    }

    private fun applyTwoPairsLayout(faceLayout: DigitalFaceLayout) {
        val layoutParams = view.layoutParams as? RelativeLayout.LayoutParams ?: return
        layoutParams.addRule(RelativeLayout.ALIGN_BASELINE)
        if (faceLayout == DigitalFaceLayout.TWO_PAIRS_HORIZONTAL) {
            when (view.id) {
                ClockViewIds.HOUR_DIGIT_PAIR -> {
                    layoutParams.addRule(RelativeLayout.CENTER_VERTICAL)
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START)
                }
                ClockViewIds.MINUTE_DIGIT_PAIR -> {
                    layoutParams.addRule(RelativeLayout.CENTER_VERTICAL)
                    layoutParams.addRule(RelativeLayout.END_OF, ClockViewIds.HOUR_DIGIT_PAIR)
                }
                else -> throw Exception("cannot apply two pairs layout to view ${view.id}")
            }
        } else {
            when (view.id) {
                ClockViewIds.HOUR_DIGIT_PAIR -> {
                    layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                }
                ClockViewIds.MINUTE_DIGIT_PAIR -> {
                    layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                    layoutParams.addRule(RelativeLayout.BELOW, ClockViewIds.HOUR_DIGIT_PAIR)
                }
                else -> throw Exception("cannot apply two pairs layout to view ${view.id}")
            }
        }
        view.layoutParams = layoutParams
    }

    private fun applyFourDigitsLayout(faceLayout: DigitalFaceLayout) {
        val layoutParams = view.layoutParams as? RelativeLayout.LayoutParams ?: return
        when (faceLayout) {
            DigitalFaceLayout.FOUR_DIGITS_ALIGN_CENTER -> {
                when (view.id) {
                    ClockViewIds.HOUR_FIRST_DIGIT -> {
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START)
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                    }
                    ClockViewIds.HOUR_SECOND_DIGIT -> {
                        layoutParams.addRule(RelativeLayout.END_OF, ClockViewIds.HOUR_FIRST_DIGIT)
                        layoutParams.addRule(
                            RelativeLayout.ALIGN_TOP,
                            ClockViewIds.HOUR_FIRST_DIGIT,
                        )
                    }
                    ClockViewIds.MINUTE_FIRST_DIGIT -> {
                        layoutParams.addRule(
                            RelativeLayout.ALIGN_START,
                            ClockViewIds.HOUR_FIRST_DIGIT,
                        )
                        layoutParams.addRule(RelativeLayout.BELOW, ClockViewIds.HOUR_FIRST_DIGIT)
                    }
                    ClockViewIds.MINUTE_SECOND_DIGIT -> {
                        layoutParams.addRule(
                            RelativeLayout.ALIGN_START,
                            ClockViewIds.HOUR_SECOND_DIGIT,
                        )
                        layoutParams.addRule(RelativeLayout.BELOW, ClockViewIds.HOUR_SECOND_DIGIT)
                    }
                    else -> throw Exception("cannot apply four digits layout to view ${view.id}")
                }
            }
            DigitalFaceLayout.FOUR_DIGITS_HORIZONTAL -> {
                when (view.id) {
                    ClockViewIds.HOUR_FIRST_DIGIT -> {
                        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL)
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START)
                    }
                    ClockViewIds.HOUR_SECOND_DIGIT -> {
                        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL)
                        layoutParams.addRule(RelativeLayout.END_OF, ClockViewIds.HOUR_FIRST_DIGIT)
                    }
                    ClockViewIds.MINUTE_FIRST_DIGIT -> {
                        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL)
                        layoutParams.addRule(RelativeLayout.END_OF, ClockViewIds.HOUR_SECOND_DIGIT)
                    }
                    ClockViewIds.MINUTE_SECOND_DIGIT -> {
                        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL)
                        layoutParams.addRule(RelativeLayout.END_OF, ClockViewIds.MINUTE_FIRST_DIGIT)
                    }
                    else ->
                        throw Exception("cannot apply FOUR_DIGITS_HORIZONTAL to view ${view.id}")
                }
            }
            else ->
                throw IllegalArgumentException(
                    "applyFourDigitsLayout function should not have parameters as ${layer.faceLayout}"
                )
        }
        view.layoutParams = layoutParams
    }

    fun refreshTime() {
        val text = timespecHandler.getText()
        if ((view as IDigitalClockTextView).text == text) return

        (view as IDigitalClockTextView).text = text
        (view as IDigitalClockTextView).refreshText()
        logger.d({ "refreshTime: new text=$str1" }) { str1 = text }
    }

    companion object {
        private val TAG = DigitalHandLayerController::class.java.simpleName
    }
}
