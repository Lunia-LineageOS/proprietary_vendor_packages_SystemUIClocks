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

package com.android.systemui.clocks.calligraphy

import com.android.systemui.clocks.ClockDesign
import com.android.systemui.clocks.ClockFace
import com.android.systemui.clocks.ClockFaceScaleInPicker
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
        FontSource.AssetFontSource("RotationGX.ttf"),
        DigitalClockTextView.FontVariations("'wght' 500"),
    )

private val alignment = DigitalAlignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)

private fun pairLayer(format: String) =
    DigitalHandLayer(
        timespec = DigitalTimespec.DIGIT_PAIR,
        font = fontSpec,
        style = CustomFontTextStyle(fontSizeScale = 0.95f),
        aodStyle =
            CustomFontTextStyle(
                borderWidth = "4dp",
                fontSizeScale = 0.95f,
                renderType = RenderType.HOLLOW_TEXT,
                outlineColor = "@android:color/system_accent1_100",
            ),
        dateTimeFormat = format,
        alignment = alignment,
    )

private fun smallLayer() =
    DigitalHandLayer(
        timespec = DigitalTimespec.TIME_FULL_FORMAT,
        font = fontSpec,
        style = CustomFontTextStyle(fontSizeScale = 0.77f),
        aodStyle =
            CustomFontTextStyle(
                borderWidth = "4dp",
                fontSizeScale = 0.77f,
                renderType = RenderType.HOLLOW_TEXT,
                outlineColor = "@android:color/system_accent1_100",
            ),
        dateTimeFormat = "h\u00A0mm",
    )

val calligraphyDesign =
    ClockDesign(
        id = "DIGITAL_CLOCK_CALLIGRAPHY",
        name = "@string/digital_clock_calligraphy_name",
        description = "@string/digital_clock_calligraphy_description",
        thumbnail = "@drawable/calligraphy_thumbnail",
        large =
            ClockFace(
                layers = listOf(pairLayer("hh"), pairLayer("mm")),
                faceLayout = DigitalFaceLayout.TWO_PAIRS_VERTICAL,
                pickerScale = ClockFaceScaleInPicker(0.8f, 0.8f),
            ),
        small = ClockFace(layers = listOf(smallLayer())),
    )
