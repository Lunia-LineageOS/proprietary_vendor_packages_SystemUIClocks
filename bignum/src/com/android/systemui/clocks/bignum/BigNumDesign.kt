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

package com.android.systemui.clocks.bignum

import com.android.systemui.clocks.AnalogHandLayer
import com.android.systemui.clocks.AssetLayer
import com.android.systemui.clocks.AssetReference
import com.android.systemui.clocks.ClockDesign
import com.android.systemui.clocks.ClockFace
import com.android.systemui.customization.clocks.AnalogTickMode
import com.android.systemui.customization.clocks.AnalogTimespec

val BigNumDesign =
    ClockDesign(
        id = "ANALOG_CLOCK_BIGNUM",
        name = "@string/analog_clock_bignum_name",
        description = "@string/analog_clock_bignum_description",
        thumbnail = "@drawable/bignum_thumbnail",
        large =
            ClockFace(
                layers =
                    listOf(
                        AssetLayer(
                            AssetReference(
                                light = "@drawable/analog_bignum_large_numbers",
                                dark = "@drawable/analog_bignum_large_numbers",
                                doze = "@drawable/analog_bignum_large_aod_numbers",
                                lightTint = "@android:color/system_accent1_50+0",
                                darkTint = "@android:color/system_accent1_800+0",
                                dozeTint = "@android:color/system_accent1_100",
                            )
                        ),
                        AnalogHandLayer(
                            timespec = AnalogTimespec.HOURS,
                            tickMode = AnalogTickMode.SWEEP,
                            asset =
                                AssetReference(
                                    light = "@drawable/analog_bignum_large_hour",
                                    dark = "@drawable/analog_bignum_large_hour",
                                    doze = "@drawable/analog_bignum_large_aod_hour",
                                    lightTint = "@android:color/system_accent3_50+200",
                                    darkTint = "@android:color/system_accent3_800+200",
                                    dozeTint = "@android:color/system_accent1_100",
                                ),
                        ),
                        AnalogHandLayer(
                            timespec = AnalogTimespec.MINUTES,
                            tickMode = AnalogTickMode.SWEEP,
                            asset =
                                AssetReference(
                                    light = "@drawable/analog_bignum_large_minute",
                                    dark = "@drawable/analog_bignum_large_minute",
                                    doze = "@drawable/analog_bignum_large_aod_minute",
                                    lightTint = "@android:color/system_accent2_50-300",
                                    darkTint = "@android:color/system_accent2_750-300",
                                    dozeTint = "@android:color/system_accent1_100",
                                ),
                        ),
                        AnalogHandLayer(
                            timespec = AnalogTimespec.SECONDS,
                            tickMode = AnalogTickMode.SWEEP,
                            asset =
                                AssetReference(
                                    light = "@drawable/analog_bignum_large_second",
                                    dark = "@drawable/analog_bignum_large_second",
                                    lightTint = "@android:color/system_accent2_50+700",
                                    darkTint = "@android:color/system_accent2_800+700",
                                    dozeTint = "@android:color/system_accent1_100",
                                ),
                        ),
                    )
            ),
        small =
            ClockFace(
                layers =
                    listOf(
                        AssetLayer(
                            AssetReference(
                                light = "@drawable/analog_bignum_small_numbers",
                                dark = "@drawable/analog_bignum_small_numbers",
                                doze = "@drawable/analog_bignum_small_aod_numbers",
                                lightTint = "@android:color/system_accent1_50+0",
                                darkTint = "@android:color/system_accent1_800+0",
                                dozeTint = "@android:color/system_accent1_100",
                            )
                        ),
                        AnalogHandLayer(
                            timespec = AnalogTimespec.HOURS,
                            tickMode = AnalogTickMode.SWEEP,
                            asset =
                                AssetReference(
                                    light = "@drawable/analog_bignum_small_hour",
                                    dark = "@drawable/analog_bignum_small_hour",
                                    doze = "@drawable/analog_bignum_small_aod_hour",
                                    lightTint = "@android:color/system_accent3_50+200",
                                    darkTint = "@android:color/system_accent3_800+200",
                                    dozeTint = "@android:color/system_accent1_100",
                                ),
                        ),
                        AnalogHandLayer(
                            timespec = AnalogTimespec.MINUTES,
                            tickMode = AnalogTickMode.SWEEP,
                            asset =
                                AssetReference(
                                    light = "@drawable/analog_bignum_small_minute",
                                    dark = "@drawable/analog_bignum_small_minute",
                                    doze = "@drawable/analog_bignum_small_aod_minute",
                                    lightTint = "@android:color/system_accent2_50-300",
                                    darkTint = "@android:color/system_accent2_750-300",
                                    dozeTint = "@android:color/system_accent1_100",
                                ),
                        ),
                        AnalogHandLayer(
                            timespec = AnalogTimespec.SECONDS,
                            tickMode = AnalogTickMode.SWEEP,
                            asset =
                                AssetReference(
                                    light = "@drawable/analog_bignum_small_second",
                                    dark = "@drawable/analog_bignum_small_second",
                                    lightTint = "@android:color/system_accent2_50+700",
                                    darkTint = "@android:color/system_accent2_800+700",
                                    dozeTint = "@android:color/system_accent1_100",
                                ),
                        ),
                    )
            ),
    )
