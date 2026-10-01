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

package com.android.systemui.clocks.growth

import com.android.systemui.animation.GSFAxes
import com.android.systemui.clocks.ClockDesign
import com.android.systemui.clocks.ClockFace
import com.android.systemui.clocks.CustomFontTextStyle
import com.android.systemui.clocks.DigitalFaceLayout
import com.android.systemui.clocks.DigitalHandLayer
import com.android.systemui.clocks.FontSource
import com.android.systemui.clocks.FontSpec
import com.android.systemui.clocks.RenderType
import com.android.systemui.customization.clocks.DigitalTimespec
import com.android.systemui.customization.clocks.view.DigitalAlignment
import com.android.systemui.customization.clocks.view.DigitalClockTextView
import com.android.systemui.customization.clocks.view.HorizontalAlignment
import com.android.systemui.customization.clocks.view.VerticalAlignment

val fontSpec =
    FontSpec(
        FontSource.AssetFontSource("growth.ttf"),
        DigitalClockTextView.FontVariations(
            "'${GSFAxes.WEIGHT.tag}' 500",
            "'${GSFAxes.WEIGHT.tag}' 50",
        ),
    )

private val lineHeight = 221.88f

private fun pairLayer(format: String, vertical: VerticalAlignment) =
    DigitalHandLayer(
        timespec = DigitalTimespec.DIGIT_PAIR,
        font = fontSpec,
        style = CustomFontTextStyle(lineHeight = lineHeight, fontSizeScale = 1.25f),
        aodStyle =
            CustomFontTextStyle(
                borderWidth = "0dp",
                fillColorLight = "@android:color/system_accent1_100",
                fontSizeScale = 1.06f,
                renderType = RenderType.CHANGE_WEIGHT,
            ),
        dateTimeFormat = format,
        alignment = DigitalAlignment(HorizontalAlignment.CENTER, vertical),
    )

private fun smallLayer() =
    DigitalHandLayer(
        timespec = DigitalTimespec.TIME_FULL_FORMAT,
        font = fontSpec,
        style = CustomFontTextStyle(fontSizeScale = 1.04f),
        aodStyle =
            CustomFontTextStyle(
                borderWidth = "0dp",
                fillColorLight = "@android:color/system_accent1_100",
                fontSizeScale = 1.04f,
                renderType = RenderType.CHANGE_WEIGHT,
            ),
        dateTimeFormat = "h:mm",
    )

val growthDesign =
    ClockDesign(
        id = "DIGITAL_CLOCK_GROWTH",
        name = "@string/digital_clock_growth_name",
        description = "@string/digital_clock_growth_description",
        thumbnail = "@drawable/growth_thumbnail",
        large =
            ClockFace(
                layers =
                    listOf(
                        pairLayer("hh", VerticalAlignment.BOTTOM),
                        pairLayer("mm", VerticalAlignment.TOP),
                    ),
                faceLayout = DigitalFaceLayout.TWO_PAIRS_VERTICAL,
            ),
        small = ClockFace(layers = listOf(smallLayer())),
    )
