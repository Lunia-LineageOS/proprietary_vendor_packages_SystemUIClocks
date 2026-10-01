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
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.DigitalFaceLayout
import com.android.systemui.clocks.DigitalHandLayer
import com.android.systemui.clocks.lookupDigitalLayerId
import com.android.systemui.clocks.view.ICustomDigitalTextView
import com.android.systemui.customization.clocks.AnimationState
import com.android.systemui.customization.clocks.DigitalDateFormatter
import com.android.systemui.customization.clocks.DigitalTimeFormatter
import com.android.systemui.customization.clocks.DigitalTimespec
import com.android.systemui.customization.clocks.DigitalTimespecHandler
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
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig
import com.android.systemui.plugins.keyguard.ui.clocks.TimeFormatKind
import java.util.Locale
import kotlin.math.roundToInt

class DigitalHandLayerController(
    private val clockCtx: CustomClockContext,
    private val layer: DigitalHandLayer,
    view: View,
) : ClockLayerController {
    private val logger = Logger(clockCtx.messageBuffer, TAG)

    val timeFormatter =
        if (layer.timespec == DigitalTimespec.DATE_FORMAT) {
            DigitalDateFormatter(
                layer.dateTimeFormat,
                clockCtx.timeKeeper,
                enableContentDescription = true,
                textModifier = { it.uppercase(Locale.ROOT) },
            )
        } else {
            DigitalTimeFormatter(
                layer.dateTimeFormat,
                clockCtx.timeKeeper,
                enableContentDescription = true,
            )
        }
    val timespecHandler = DigitalTimespecHandler(layer.timespec, timeFormatter)

    override val view: View = view
    private var dozeState: AnimationState? = null

    override val config = ClockFaceConfig(tickRate = ClockTickRate.PER_MINUTE)

    override val animations =
        object : ClockAnimations {
            override fun enter() {
                applyLayout(layer.faceLayout)
                refreshTime()
            }

            override fun doze(fraction: Float) {
                val state =
                    dozeState
                        ?: AnimationState(fraction).also {
                            dozeState = it
                            (view as ICustomDigitalTextView).animateDoze(it.isActive, false)
                            return@also
                        }
                val (wasActive, hasJumped) = state.update(fraction)
                if (wasActive != state.isActive) {
                    (view as ICustomDigitalTextView).animateDoze(state.isActive, !hasJumped)
                }
                view.dozeFraction = fraction
            }

            override fun fold(fraction: Float) {
                applyLayout(layer.faceLayout)
                refreshTime()
            }

            override fun charge() {
                (view as ICustomDigitalTextView).animateCharge()
            }

            override fun onPickerCarouselSwiping(swipingFraction: Float) = Unit

            override fun onPositionAnimated(args: ClockPositionAnimationArgs) = Unit

            override fun onFidgetTap(x: Float, y: Float) = Unit

            override fun onFontAxesChanged(style: ClockAxisStyle) = Unit
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

            override fun onWeatherDataChanged(data: WeatherData) = Unit

            override fun onAlarmDataChanged(data: AlarmData) = Unit

            override fun onZenDataChanged(data: ZenData) = Unit
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
                (view as ICustomDigitalTextView).applyCustomTextSize(
                    fontSizePx,
                    constrainedByHeight = false,
                )
                applyMargin()
            }

            override fun onTargetRegionChanged(targetRegion: VRect) = Unit

            override fun onSecondaryDisplayChanged(onSecondaryDisplay: Boolean) = Unit
        }

    init {
        view.layoutParams =
            RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        layer.alignment?.let { alignment ->
            alignment.verticalAlignment?.let {
                (view as ICustomDigitalTextView).clockVerticalAlignment = it
            }
            alignment.horizontalAlignment?.let {
                (view as ICustomDigitalTextView).clockHorizontalAlignment = it
            }
        }
        (view as ICustomDigitalTextView).applyStyles(layer.style, layer.aodStyle)
        view.id = lookupDigitalLayerId(layer)
        layer.timeZoneId?.let {
            timeFormatter.overrideTimeZone = java.util.TimeZone.getTimeZone(it)
        }
    }

    fun refreshTime() {
        val text = timespecHandler.getText()
        val textView = view as ICustomDigitalTextView
        if (textView.text == text) return
        textView.text = text
        textView.refreshText()
        logger.d({ "refreshTime: new text=$str1" }) { str1 = text }
    }

    private fun applyLayout(faceLayout: DigitalFaceLayout?) {
        if (faceLayout == null) return
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
                    else ->
                        throw IllegalStateException(
                            "cannot apply four digits layout to view ${view.id}"
                        )
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
                        throw IllegalStateException(
                            "cannot apply FOUR_DIGITS_HORIZONTAL to view ${view.id}"
                        )
                }
            }
            DigitalFaceLayout.TWO_PAIRS_VERTICAL -> {
                layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                when (view.id) {
                    ClockViewIds.HOUR_DIGIT_PAIR ->
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                    ClockViewIds.MINUTE_DIGIT_PAIR ->
                        layoutParams.addRule(RelativeLayout.BELOW, ClockViewIds.HOUR_DIGIT_PAIR)
                    else ->
                        throw IllegalStateException(
                            "cannot apply two pairs layout to view ${view.id}"
                        )
                }
            }
            DigitalFaceLayout.TWO_PAIRS_HORIZONTAL -> {
                layoutParams.addRule(RelativeLayout.ALIGN_BASELINE)
                when (view.id) {
                    ClockViewIds.HOUR_DIGIT_PAIR -> {
                        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL)
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START)
                    }
                    ClockViewIds.MINUTE_DIGIT_PAIR -> {
                        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL)
                        layoutParams.addRule(RelativeLayout.END_OF, ClockViewIds.HOUR_DIGIT_PAIR)
                    }
                    else ->
                        throw IllegalStateException(
                            "cannot apply two pairs layout to view ${view.id}"
                        )
                }
            }
            DigitalFaceLayout.ROAMING_DUAL -> Unit
        }
        applyMargin()
    }

    private fun applyMargin() {
        val layoutParams = view.layoutParams as? RelativeLayout.LayoutParams ?: return
        layer.marginRatio.let { ratio ->
            layoutParams.setMargins(
                (ratio.left * view.measuredWidth).roundToInt(),
                (ratio.top * view.measuredHeight).roundToInt(),
                (ratio.right * view.measuredWidth).roundToInt(),
                (ratio.bottom * view.measuredHeight).roundToInt(),
            )
        }
        view.layoutParams = layoutParams
    }

    private companion object {
        const val TAG = "DigitalHandLayerController"
    }
}
