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

package com.android.systemui.clocks

import android.view.animation.Interpolator
import com.android.systemui.customization.clocks.AnalogTickMode
import com.android.systemui.customization.clocks.AnalogTimespec
import com.android.systemui.customization.clocks.DigitalTimespec
import com.android.systemui.customization.clocks.FontTextStyle
import com.android.systemui.customization.clocks.TextStyle
import com.android.systemui.customization.clocks.view.DigitalAlignment
import com.android.systemui.customization.clocks.view.DigitalClockTextView.FontVariations

data class ClockDesign(
    val id: String,
    val name: String? = null,
    val description: String? = null,
    val thumbnail: String? = null,
    val large: ClockFace,
    val small: ClockFace? = null,
    val colorPalette: Int? = null,
)

data class ClockFace(
    val layers: List<ClockLayer> = emptyList(),
    val faceLayout: DigitalFaceLayout? = null,
    val pickerScale: ClockFaceScaleInPicker? = ClockFaceScaleInPicker(1f, 1f),
)

data class ClockFaceScaleInPicker(val scaleX: Float, val scaleY: Float)

enum class DigitalFaceLayout {
    TWO_PAIRS_VERTICAL,
    TWO_PAIRS_HORIZONTAL,
    FOUR_DIGITS_ALIGN_CENTER,
    FOUR_DIGITS_HORIZONTAL,
    ROAMING_DUAL,
}

interface ClockLayer

data class AssetReference(
    val light: String,
    val dark: String,
    val doze: String? = null,
    val lightTint: String? = null,
    val darkTint: String? = null,
    val dozeTint: String? = null,
)

data class AssetLayer(val asset: AssetReference) : ClockLayer

data class AnalogHandLayer(
    val timespec: AnalogTimespec,
    val tickMode: AnalogTickMode,
    val asset: AssetReference,
) : ClockLayer

data class DigitalMarginRatio(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
)

data class DigitalHandLayer(
    val timespec: DigitalTimespec,
    val font: FontSpec? = null,
    val style: TextStyle,
    val aodStyle: TextStyle? = null,
    val faceLayout: DigitalFaceLayout? = null,
    val dateTimeFormat: String,
    val alignment: DigitalAlignment? = null,
    val marginRatio: DigitalMarginRatio? = DigitalMarginRatio(),
    val timeZoneId: String? = null,
) : ClockLayer

data class ComposedDigitalHandLayer(
    val customizedView: String,
    val font: FontSpec?,
    val digitalLayers: List<DigitalHandLayer> = emptyList(),
) : ClockLayer

data class FontSpec(val source: FontSource, val variations: FontVariations)

sealed interface FontSource {
    data class AssetFontSource(val file: String) : FontSource
}

enum class RenderType {
    CHANGE_WEIGHT,
    HOLLOW_TEXT,
    STROKE_TEXT,
    OUTER_OUTLINE_TEXT,
}

data class CustomFontTextStyle(
    override val lineHeight: Float? = null,
    val borderWidth: String? = null,
    val fillColorLight: String? = null,
    val fillColorDark: String? = null,
    override val fontSizeScale: Float = 1f,
    override val fontFeatureSettings: String = "",
    val renderType: RenderType = RenderType.STROKE_TEXT,
    val outlineColor: String? = null,
    override val transitionDuration: Long = 300L,
    override val transitionInterpolator: Interpolator? = null,
) : FontTextStyle

data class LottieTextStyle(
    val numbers: List<String> = emptyList(),
    val spacing: String = "0dp",
    val colon: String? = null,
    val fillColorLightMap: Map<String, String>? = null,
    val fillColorDarkMap: Map<String, String>? = null,
    override val fontSizeScale: Float = 1f,
    val paddingVertical: String = "0dp",
    val paddingHorizontal: String = "0dp",
) : TextStyle

fun lookupDigitalLayerId(layer: DigitalHandLayer): Int {
    return layer.timespec.getViewId(layer.dateTimeFormat.contains("h"))
}
