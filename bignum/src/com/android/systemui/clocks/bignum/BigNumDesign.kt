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
import com.android.systemui.clocks.LayerBounds
import com.android.systemui.customization.clocks.AnalogTickMode
import com.android.systemui.customization.clocks.AnalogTimespec

private fun numbersRef(size: String) =
    AssetReference(
        light = "@drawable/analog_bignum_${size}_numbers",
        dark = "@drawable/analog_bignum_${size}_numbers",
        doze = "@drawable/analog_bignum_${size}_aod_numbers",
        lightTint = "@android:color/system_accent1_50+0",
        darkTint = "@android:color/system_accent1_800+0",
        dozeTint = "@android:color/system_accent1_100",
    )

private fun handRef(size: String, hand: String) =
    AssetReference(
        light = "@drawable/analog_bignum_${size}_$hand",
        dark = "@drawable/analog_bignum_${size}_$hand",
        doze = "@drawable/analog_bignum_${size}_aod_$hand",
        lightTint = "@android:color/system_accent3_50+200",
        darkTint = "@android:color/system_accent3_800+200",
        dozeTint = "@android:color/system_accent1_100",
    )

private fun minuteRef(size: String) =
    handRef(size, "minute")
        .copy(
            lightTint = "@android:color/system_accent2_50-300",
            darkTint = "@android:color/system_accent2_750-300",
        )

private fun secondRef(size: String) =
    handRef(size, "second")
        .copy(
            doze = null,
            lightTint = "@android:color/system_accent2_50+700",
            darkTint = "@android:color/system_accent2_800+700",
        )

private fun face(size: String) =
    ClockFace(
        layers =
            listOf(
                AssetLayer(numbersRef(size)),
                AnalogHandLayer(AnalogTimespec.HOURS, AnalogTickMode.SWEEP, handRef(size, "hour")),
                AnalogHandLayer(AnalogTimespec.MINUTES, AnalogTickMode.SWEEP, minuteRef(size)),
                AnalogHandLayer(AnalogTimespec.SECONDS, AnalogTickMode.SWEEP, secondRef(size)),
            ),
        layerBounds = LayerBounds.FIT,
    )

val bigNumDesign =
    ClockDesign(
        id = "ANALOG_CLOCK_BIGNUM",
        name = "@string/analog_clock_bignum_name",
        description = "@string/analog_clock_bignum_description",
        thumbnail = "@drawable/bignum_thumbnail",
        large = face("large"),
        small = face("small"),
    )
