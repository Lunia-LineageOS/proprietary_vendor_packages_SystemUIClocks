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

package com.android.systemui.clocks.metro

import com.android.systemui.clocks.ClockDesign
import com.android.systemui.clocks.ClockFace
import com.android.systemui.clocks.DigitalFaceLayout
import com.android.systemui.clocks.DigitalHandLayer
import com.android.systemui.clocks.LottieTextStyle
import com.android.systemui.customization.clocks.DigitalTimespec
import com.android.systemui.customization.clocks.view.DigitalAlignment
import com.android.systemui.customization.clocks.view.HorizontalAlignment
import com.android.systemui.customization.clocks.view.VerticalAlignment

private const val NUMBERS_ASSET_FMT = "lotties/%d_SIMPLIFIED_V3E.json"
private val numbers = (0..9).map { NUMBERS_ASSET_FMT.format(it) }

private fun lottieStyle() =
    LottieTextStyle(
        numbers = numbers,
        spacing = "0dp",
        colon = "@drawable/digital_metro_colon",
        fillColorLightMap =
            mapOf(
                "Color 1" to "@android:color/system_accent1_100",
                "Color 2" to "@android:color/system_accent1_550",
                "Color 3" to "@android:color/system_accent1_350",
                "Color 4" to "@android:color/system_accent1_200",
            ),
        fillColorDarkMap =
            mapOf(
                "Color 1" to "@android:color/system_accent1_800",
                "Color 2" to "@android:color/system_accent1_900",
                "Color 3" to "@android:color/system_accent1_650",
                "Color 4" to "@android:color/system_accent1_450",
            ),
        fontSizeScale = 0f,
        paddingVertical = "4dp",
        paddingHorizontal = "4dp",
    )

private val alignment = DigitalAlignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)

private fun layer(timespec: DigitalTimespec, format: String) =
    DigitalHandLayer(
        timespec = timespec,
        style = lottieStyle(),
        dateTimeFormat = format,
        alignment = alignment,
    )

val metroDesign =
    ClockDesign(
        id = "DIGITAL_CLOCK_METRO",
        name = "@string/digital_clock_metro_name",
        description = "@string/digital_clock_metro_description",
        thumbnail = "@drawable/metro_thumbnail",
        large =
            ClockFace(
                layers =
                    listOf(
                        layer(DigitalTimespec.FIRST_DIGIT, "hh"),
                        layer(DigitalTimespec.SECOND_DIGIT, "hh"),
                        layer(DigitalTimespec.FIRST_DIGIT, "mm"),
                        layer(DigitalTimespec.SECOND_DIGIT, "mm"),
                    ),
                faceLayout = DigitalFaceLayout.FOUR_DIGITS_ALIGN_CENTER,
            ),
        small =
            ClockFace(
                layers =
                    listOf(
                        layer(DigitalTimespec.FIRST_DIGIT, "h"),
                        layer(DigitalTimespec.SECOND_DIGIT, "h"),
                        layer(DigitalTimespec.FIRST_DIGIT, "mm"),
                        layer(DigitalTimespec.SECOND_DIGIT, "mm"),
                    ),
                faceLayout = DigitalFaceLayout.FOUR_DIGITS_HORIZONTAL,
            ),
    )
