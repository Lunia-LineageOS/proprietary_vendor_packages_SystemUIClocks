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

package com.android.systemui.clocks.numoverlap

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
        FontSource.AssetFontSource("google_sans_flex.ttf"),
        DigitalClockTextView.FontVariations(
            "'wght' 1000, 'wdth' 108, 'opsz' 90",
            "'wght' 878, 'wdth' 100, 'opsz' 144",
        ),
    )

private val alignment = DigitalAlignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)

private fun fillStyle(fillLight: String, fillDark: String) =
    CustomFontTextStyle(fillColorLight = fillLight, fillColorDark = fillDark)

private fun hollowStyle(borderWidth: String) =
    CustomFontTextStyle(
        borderWidth = borderWidth,
        renderType = RenderType.HOLLOW_TEXT,
        outlineColor = "@android:color/system_accent1_100",
    )

private fun layer(timespec: DigitalTimespec, format: String, offset: Int, border: String) =
    DigitalHandLayer(
        timespec = timespec,
        font = fontSpec,
        style =
            fillStyle(
                "@android:color/system_accent1_40+$offset",
                "@android:color/system_accent1_730+$offset",
            ),
        aodStyle = hollowStyle(border),
        dateTimeFormat = format,
        alignment = alignment,
    )

val numberOverlapDesign =
    ClockDesign(
        id = "DIGITAL_CLOCK_NUMBEROVERLAP",
        name = "@string/digital_clock_numberoverlap_name",
        description = "@string/digital_clock_numberoverlap_description",
        thumbnail = "@drawable/numoverlap_thumbnail",
        large =
            ClockFace(
                layers =
                    listOf(
                        ComposedDigitalHandLayer(
                            customizedView = "NumberOverlap",
                            font = fontSpec,
                            digitalLayers =
                                listOf(
                                    layer(DigitalTimespec.FIRST_DIGIT, "hh", 0, "4dp"),
                                    layer(DigitalTimespec.SECOND_DIGIT, "hh", 120, "4dp"),
                                    layer(DigitalTimespec.FIRST_DIGIT, "mm", 120, "4dp"),
                                    layer(DigitalTimespec.SECOND_DIGIT, "mm", 0, "4dp"),
                                ),
                        )
                    )
            ),
        small =
            ClockFace(
                layers =
                    listOf(
                        ComposedDigitalHandLayer(
                            customizedView = "NumberOverlapSmall",
                            font = fontSpec,
                            digitalLayers =
                                listOf(
                                    layer(DigitalTimespec.FIRST_DIGIT, "h", 0, "3dp"),
                                    layer(DigitalTimespec.SECOND_DIGIT, "h", 120, "3dp"),
                                    layer(DigitalTimespec.FIRST_DIGIT, "mm", 0, "3dp"),
                                    layer(DigitalTimespec.SECOND_DIGIT, "mm", 120, "3dp"),
                                ),
                        )
                    )
            ),
    )
