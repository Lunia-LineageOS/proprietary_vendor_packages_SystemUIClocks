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

package com.android.systemui.clocks.weather

import com.android.systemui.animation.GSFAxes
import com.android.systemui.clocks.ClockDesign
import com.android.systemui.clocks.ClockFace
import com.android.systemui.clocks.ClockFaceScaleInPicker
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

val weatherTextFont =
    FontSpec(
        FontSource.AssetFontSource("GoogleSansFlex-Custom-wght94-420.ttf"),
        DigitalClockTextView.FontVariations(
            "'${GSFAxes.WEIGHT.tag}' 420",
            "'${GSFAxes.WEIGHT.tag}' 94",
        ),
    )

val weatherIconFont =
    FontSpec(
        FontSource.AssetFontSource("FrameWeatherVF.ttf"),
        DigitalClockTextView.FontVariations(
            "'${GSFAxes.WEIGHT.tag}' 1000",
            "'${GSFAxes.WEIGHT.tag}' 0",
        ),
    )

private const val LARGE_LINE_HEIGHT = 83.88f

private fun largeTextStyle() =
    CustomFontTextStyle(lineHeight = LARGE_LINE_HEIGHT, borderWidth = "0dp", fontSizeScale = 0.325f)

private fun largeAodStyle() =
    CustomFontTextStyle(
        borderWidth = "0dp",
        fillColorLight = "@android:color/system_accent1_100",
        fontSizeScale = 0.325f,
        renderType = RenderType.CHANGE_WEIGHT,
    )

private fun smallTextStyle() = CustomFontTextStyle(borderWidth = "0dp", fontSizeScale = 0.86f)

private fun smallAodStyle() =
    CustomFontTextStyle(
        borderWidth = "0dp",
        fillColorLight = "@android:color/system_accent1_100",
        fontSizeScale = 0.86f,
        renderType = RenderType.CHANGE_WEIGHT,
    )

val weatherDesign =
    ClockDesign(
        id = "DIGITAL_CLOCK_WEATHER",
        name = "@string/digital_clock_weather_name",
        description = "@string/digital_clock_weather_description",
        thumbnail = "@drawable/weather_thumbnail",
        large =
            ClockFace(
                layers =
                    listOf(
                        ComposedDigitalHandLayer(
                            customizedView = "WeatherClock",
                            font = weatherTextFont,
                            digitalLayers =
                                listOf(
                                    DigitalHandLayer(
                                        timespec = DigitalTimespec.TIME_FULL_FORMAT,
                                        font = weatherTextFont,
                                        style = largeTextStyle(),
                                        aodStyle = largeAodStyle(),
                                        dateTimeFormat = "hh\u00A0mm",
                                        alignment =
                                            DigitalAlignment(
                                                HorizontalAlignment.LEFT,
                                                VerticalAlignment.TOP,
                                            ),
                                    ),
                                    DigitalHandLayer(
                                        timespec = DigitalTimespec.DATE_FORMAT,
                                        font = weatherTextFont,
                                        style = largeTextStyle(),
                                        aodStyle = largeAodStyle(),
                                        dateTimeFormat = "EEE MMM d",
                                        alignment =
                                            DigitalAlignment(
                                                HorizontalAlignment.RIGHT,
                                                VerticalAlignment.TOP,
                                            ),
                                    ),
                                ),
                        )
                    ),
                pickerScale = ClockFaceScaleInPicker(0.5f, 0.5f),
            ),
        small =
            ClockFace(
                layers =
                    listOf(
                        ComposedDigitalHandLayer(
                            customizedView = "WeatherClockSmall",
                            font = weatherTextFont,
                            digitalLayers =
                                listOf(
                                    DigitalHandLayer(
                                        timespec = DigitalTimespec.TIME_FULL_FORMAT,
                                        font = weatherTextFont,
                                        style = smallTextStyle(),
                                        aodStyle = smallAodStyle(),
                                        dateTimeFormat = "hh\u00A0mm",
                                        alignment =
                                            DigitalAlignment(
                                                HorizontalAlignment.LEFT,
                                                VerticalAlignment.CENTER,
                                            ),
                                    )
                                ),
                        )
                    )
            ),
    )
