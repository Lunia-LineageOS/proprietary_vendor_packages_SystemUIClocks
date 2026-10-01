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

private fun transitTextStyle() =
    LottieTextStyle(
        numbers =
            listOf(
                "lotties/0_SIMPLIFIED_V3E.json",
                "lotties/1_SIMPLIFIED_V3E.json",
                "lotties/2_SIMPLIFIED_V3E.json",
                "lotties/3_SIMPLIFIED_V3E.json",
                "lotties/4_SIMPLIFIED_V3E.json",
                "lotties/5_SIMPLIFIED_V3E.json",
                "lotties/6_SIMPLIFIED_V3E.json",
                "lotties/7_SIMPLIFIED_V3E.json",
                "lotties/8_SIMPLIFIED_V3E.json",
                "lotties/9_SIMPLIFIED_V3E.json",
            ),
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
        paddingVertical = "4dp",
        paddingHorizontal = "4dp",
    )

val MetroDesign =
    ClockDesign(
        id = "DIGITAL_CLOCK_METRO",
        name = "@string/digital_clock_metro_name",
        description = "@string/digital_clock_metro_description",
        thumbnail = "@drawable/metro_thumbnail",
        large =
            ClockFace(
                layers =
                    listOf(
                        DigitalHandLayer(
                            timespec = DigitalTimespec.FIRST_DIGIT,
                            style = transitTextStyle(),
                            dateTimeFormat = "hh",
                            alignment =
                                DigitalAlignment(
                                    HorizontalAlignment.CENTER,
                                    VerticalAlignment.CENTER,
                                ),
                        ),
                        DigitalHandLayer(
                            timespec = DigitalTimespec.SECOND_DIGIT,
                            style = transitTextStyle(),
                            dateTimeFormat = "hh",
                            alignment =
                                DigitalAlignment(
                                    HorizontalAlignment.CENTER,
                                    VerticalAlignment.CENTER,
                                ),
                        ),
                        DigitalHandLayer(
                            timespec = DigitalTimespec.FIRST_DIGIT,
                            style = transitTextStyle(),
                            dateTimeFormat = "mm",
                            alignment =
                                DigitalAlignment(
                                    HorizontalAlignment.CENTER,
                                    VerticalAlignment.CENTER,
                                ),
                        ),
                        DigitalHandLayer(
                            timespec = DigitalTimespec.SECOND_DIGIT,
                            style = transitTextStyle(),
                            dateTimeFormat = "mm",
                            alignment =
                                DigitalAlignment(
                                    HorizontalAlignment.CENTER,
                                    VerticalAlignment.CENTER,
                                ),
                        ),
                    ),
                faceLayout = DigitalFaceLayout.FOUR_DIGITS_ALIGN_CENTER,
            ),
        small =
            ClockFace(
                layers =
                    listOf(
                        DigitalHandLayer(
                            timespec = DigitalTimespec.FIRST_DIGIT,
                            style = transitTextStyle(),
                            dateTimeFormat = "h",
                            alignment =
                                DigitalAlignment(
                                    HorizontalAlignment.CENTER,
                                    VerticalAlignment.CENTER,
                                ),
                        ),
                        DigitalHandLayer(
                            timespec = DigitalTimespec.SECOND_DIGIT,
                            style = transitTextStyle(),
                            dateTimeFormat = "h",
                            alignment =
                                DigitalAlignment(
                                    HorizontalAlignment.CENTER,
                                    VerticalAlignment.CENTER,
                                ),
                        ),
                        DigitalHandLayer(
                            timespec = DigitalTimespec.FIRST_DIGIT,
                            style = transitTextStyle(),
                            dateTimeFormat = "mm",
                            alignment =
                                DigitalAlignment(
                                    HorizontalAlignment.CENTER,
                                    VerticalAlignment.CENTER,
                                ),
                        ),
                        DigitalHandLayer(
                            timespec = DigitalTimespec.SECOND_DIGIT,
                            style = transitTextStyle(),
                            dateTimeFormat = "mm",
                            alignment =
                                DigitalAlignment(
                                    HorizontalAlignment.CENTER,
                                    VerticalAlignment.CENTER,
                                ),
                        ),
                    ),
                faceLayout = DigitalFaceLayout.FOUR_DIGITS_HORIZONTAL,
            ),
    )
