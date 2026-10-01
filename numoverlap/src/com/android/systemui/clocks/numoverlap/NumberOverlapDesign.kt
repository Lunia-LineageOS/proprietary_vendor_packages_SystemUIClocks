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
import com.android.systemui.customization.clocks.view.DigitalClockTextView.FontVariations
import com.android.systemui.customization.clocks.view.HorizontalAlignment
import com.android.systemui.customization.clocks.view.VerticalAlignment

private val fontSpec =
    FontSpec(
        FontSource.AssetFontSource("google_sans_flex.ttf"),
        FontVariations("'wght' 1000, 'wdth' 108, 'opsz' 90", "'wght' 878, 'wdth' 100, 'opsz' 144"),
    )

val NumberOverlapDesign =
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
                                    DigitalHandLayer(
                                        timespec = DigitalTimespec.FIRST_DIGIT,
                                        font = fontSpec,
                                        style =
                                            CustomFontTextStyle(
                                                fillColorLight =
                                                    "@android:color/system_accent1_40+0",
                                                fillColorDark =
                                                    "@android:color/system_accent1_730+0",
                                            ),
                                        aodStyle =
                                            CustomFontTextStyle(
                                                borderWidth = "4dp",
                                                renderType = RenderType.HOLLOW_TEXT,
                                                outlineColor = "@android:color/system_accent1_100",
                                            ),
                                        dateTimeFormat = "hh",
                                        alignment =
                                            DigitalAlignment(
                                                HorizontalAlignment.CENTER,
                                                VerticalAlignment.CENTER,
                                            ),
                                    ),
                                    DigitalHandLayer(
                                        timespec = DigitalTimespec.SECOND_DIGIT,
                                        font = fontSpec,
                                        style =
                                            CustomFontTextStyle(
                                                fillColorLight =
                                                    "@android:color/system_accent1_40+120",
                                                fillColorDark =
                                                    "@android:color/system_accent1_730+120",
                                            ),
                                        aodStyle =
                                            CustomFontTextStyle(
                                                borderWidth = "4dp",
                                                renderType = RenderType.HOLLOW_TEXT,
                                                outlineColor = "@android:color/system_accent1_100",
                                            ),
                                        dateTimeFormat = "hh",
                                        alignment =
                                            DigitalAlignment(
                                                HorizontalAlignment.CENTER,
                                                VerticalAlignment.CENTER,
                                            ),
                                    ),
                                    DigitalHandLayer(
                                        timespec = DigitalTimespec.FIRST_DIGIT,
                                        font = fontSpec,
                                        style =
                                            CustomFontTextStyle(
                                                fillColorLight =
                                                    "@android:color/system_accent1_40+120",
                                                fillColorDark =
                                                    "@android:color/system_accent1_730+120",
                                            ),
                                        aodStyle =
                                            CustomFontTextStyle(
                                                borderWidth = "4dp",
                                                renderType = RenderType.HOLLOW_TEXT,
                                                outlineColor = "@android:color/system_accent1_100",
                                            ),
                                        dateTimeFormat = "mm",
                                        alignment =
                                            DigitalAlignment(
                                                HorizontalAlignment.CENTER,
                                                VerticalAlignment.CENTER,
                                            ),
                                    ),
                                    DigitalHandLayer(
                                        timespec = DigitalTimespec.SECOND_DIGIT,
                                        font = fontSpec,
                                        style =
                                            CustomFontTextStyle(
                                                fillColorLight =
                                                    "@android:color/system_accent1_40+0",
                                                fillColorDark =
                                                    "@android:color/system_accent1_730+0",
                                            ),
                                        aodStyle =
                                            CustomFontTextStyle(
                                                borderWidth = "4dp",
                                                renderType = RenderType.HOLLOW_TEXT,
                                                outlineColor = "@android:color/system_accent1_100",
                                            ),
                                        dateTimeFormat = "mm",
                                        alignment =
                                            DigitalAlignment(
                                                HorizontalAlignment.CENTER,
                                                VerticalAlignment.CENTER,
                                            ),
                                    ),
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
                                    DigitalHandLayer(
                                        timespec = DigitalTimespec.FIRST_DIGIT,
                                        font = fontSpec,
                                        style =
                                            CustomFontTextStyle(
                                                fillColorLight =
                                                    "@android:color/system_accent1_40+0",
                                                fillColorDark =
                                                    "@android:color/system_accent1_730+0",
                                            ),
                                        aodStyle =
                                            CustomFontTextStyle(
                                                borderWidth = "3dp",
                                                renderType = RenderType.HOLLOW_TEXT,
                                                outlineColor = "@android:color/system_accent1_100",
                                            ),
                                        dateTimeFormat = "h",
                                        alignment =
                                            DigitalAlignment(
                                                HorizontalAlignment.CENTER,
                                                VerticalAlignment.CENTER,
                                            ),
                                    ),
                                    DigitalHandLayer(
                                        timespec = DigitalTimespec.SECOND_DIGIT,
                                        font = fontSpec,
                                        style =
                                            CustomFontTextStyle(
                                                fillColorLight =
                                                    "@android:color/system_accent1_40+120",
                                                fillColorDark =
                                                    "@android:color/system_accent1_730+120",
                                            ),
                                        aodStyle =
                                            CustomFontTextStyle(
                                                borderWidth = "3dp",
                                                renderType = RenderType.HOLLOW_TEXT,
                                                outlineColor = "@android:color/system_accent1_100",
                                            ),
                                        dateTimeFormat = "h",
                                        alignment =
                                            DigitalAlignment(
                                                HorizontalAlignment.CENTER,
                                                VerticalAlignment.CENTER,
                                            ),
                                    ),
                                    DigitalHandLayer(
                                        timespec = DigitalTimespec.FIRST_DIGIT,
                                        font = fontSpec,
                                        style =
                                            CustomFontTextStyle(
                                                fillColorLight =
                                                    "@android:color/system_accent1_40+120",
                                                fillColorDark =
                                                    "@android:color/system_accent1_730+120",
                                            ),
                                        aodStyle =
                                            CustomFontTextStyle(
                                                borderWidth = "3dp",
                                                renderType = RenderType.HOLLOW_TEXT,
                                                outlineColor = "@android:color/system_accent1_100",
                                            ),
                                        dateTimeFormat = "mm",
                                        alignment =
                                            DigitalAlignment(
                                                HorizontalAlignment.CENTER,
                                                VerticalAlignment.CENTER,
                                            ),
                                    ),
                                    DigitalHandLayer(
                                        timespec = DigitalTimespec.SECOND_DIGIT,
                                        font = fontSpec,
                                        style =
                                            CustomFontTextStyle(
                                                fillColorLight =
                                                    "@android:color/system_accent1_40+0",
                                                fillColorDark =
                                                    "@android:color/system_accent1_730+0",
                                            ),
                                        aodStyle =
                                            CustomFontTextStyle(
                                                borderWidth = "3dp",
                                                renderType = RenderType.HOLLOW_TEXT,
                                                outlineColor = "@android:color/system_accent1_100",
                                            ),
                                        dateTimeFormat = "mm",
                                        alignment =
                                            DigitalAlignment(
                                                HorizontalAlignment.CENTER,
                                                VerticalAlignment.CENTER,
                                            ),
                                    ),
                                ),
                        )
                    )
            ),
    )
