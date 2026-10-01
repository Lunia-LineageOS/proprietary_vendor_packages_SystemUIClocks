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

package com.android.systemui.clocks.inflate

import com.android.systemui.clocks.ClockDesign
import com.android.systemui.clocks.ClockFace
import com.android.systemui.clocks.ComposedDigitalHandLayer
import com.android.systemui.clocks.CustomFontTextStyle
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
        FontSource.AssetFontSource("InflateVF.ttf"),
        DigitalClockTextView.FontVariations("'wght' 1000", "'wght' 0"),
    )

private val alignment = DigitalAlignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)

private fun largeLayer(timespec: DigitalTimespec, format: String, fill: String, outline: String) =
    DigitalHandLayer(
        timespec = timespec,
        font = fontSpec,
        style =
            CustomFontTextStyle(
                lineHeight = 121.9f,
                borderWidth = "12dp",
                fillColorLight = fill,
                fillColorDark = fill,
                fontSizeScale = 0f,
                renderType = RenderType.OUTER_OUTLINE_TEXT,
                outlineColor = outline,
            ),
        aodStyle =
            CustomFontTextStyle(
                borderWidth = "6dp",
                fontSizeScale = 0f,
                renderType = RenderType.HOLLOW_TEXT,
                outlineColor = "@android:color/system_accent1_100",
            ),
        dateTimeFormat = format,
        alignment = alignment,
    )

private fun smallLayer(timespec: DigitalTimespec, format: String, fill: String, outline: String) =
    DigitalHandLayer(
        timespec = timespec,
        font = fontSpec,
        style =
            CustomFontTextStyle(
                lineHeight = 121.9f,
                borderWidth = "5.2dp",
                fillColorLight = fill,
                fillColorDark = fill,
                fontSizeScale = 0.96f,
                renderType = RenderType.OUTER_OUTLINE_TEXT,
                outlineColor = outline,
            ),
        aodStyle =
            CustomFontTextStyle(
                borderWidth = "3dp",
                fontSizeScale = 0.96f,
                renderType = RenderType.HOLLOW_TEXT,
                outlineColor = "@android:color/system_accent1_100",
            ),
        dateTimeFormat = format,
        alignment = alignment,
    )

private fun largeLayers() =
    listOf(
        largeLayer(
            DigitalTimespec.FIRST_DIGIT,
            "hh",
            "@android:color/system_accent1_600",
            "@android:color/system_accent2_100",
        ),
        largeLayer(
            DigitalTimespec.SECOND_DIGIT,
            "hh",
            "@android:color/system_accent1_100",
            "@android:color/system_accent1_700",
        ),
        largeLayer(
            DigitalTimespec.FIRST_DIGIT,
            "mm",
            "@android:color/system_accent3_200",
            "@android:color/system_accent1_800",
        ),
        largeLayer(
            DigitalTimespec.SECOND_DIGIT,
            "mm",
            "@android:color/system_accent2_550",
            "@android:color/system_accent1_50",
        ),
    )

private fun smallLayers() =
    listOf(
        smallLayer(
            DigitalTimespec.FIRST_DIGIT,
            "h",
            "@android:color/system_accent1_600",
            "@android:color/system_accent2_100",
        ),
        smallLayer(
            DigitalTimespec.SECOND_DIGIT,
            "h",
            "@android:color/system_accent1_100",
            "@android:color/system_accent1_700",
        ),
        smallLayer(
            DigitalTimespec.FIRST_DIGIT,
            "mm",
            "@android:color/system_accent3_200",
            "@android:color/system_accent1_800",
        ),
        smallLayer(
            DigitalTimespec.SECOND_DIGIT,
            "mm",
            "@android:color/system_accent2_550",
            "@android:color/system_accent1_50",
        ),
    )

val inflateDesign =
    ClockDesign(
        id = "DIGITAL_CLOCK_INFLATE",
        name = "@string/digital_clock_inflate_name",
        description = "@string/digital_clock_inflate_description",
        thumbnail = "@drawable/inflate_thumbnail",
        large =
            ClockFace(
                layers =
                    listOf(
                        ComposedDigitalHandLayer(
                            customizedView = "InflateClockView",
                            font = fontSpec,
                            digitalLayers = largeLayers(),
                        )
                    )
            ),
        small =
            ClockFace(
                layers =
                    listOf(
                        ComposedDigitalHandLayer(
                            customizedView = "InflateClockViewSmall",
                            font = fontSpec,
                            digitalLayers = smallLayers(),
                        )
                    )
            ),
        colorPalette = 9,
    )
