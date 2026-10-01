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

import android.content.Context
import android.util.TypedValue
import com.android.systemui.customization.clocks.AnalogTickMode
import com.android.systemui.customization.clocks.AnalogTimespec
import com.android.systemui.customization.clocks.DigitalTimespec
import com.android.systemui.customization.clocks.TextStyle
import com.android.systemui.customization.clocks.view.DigitalAlignment
import com.android.systemui.customization.clocks.view.DigitalClockTextView

enum class LayerBounds {
    FIT,
    FILL,
    STRETCH,
}

data class ClockFaceScaleInPicker(val scaleX: Float = 1f, val scaleY: Float = 1f)

enum class DigitalFaceLayout {
    TWO_PAIRS_VERTICAL,
    TWO_PAIRS_HORIZONTAL,
    FOUR_DIGITS_ALIGN_CENTER,
    FOUR_DIGITS_HORIZONTAL,
    ROAMING_DUAL,
}

data class DigitalMarginRatio(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
)

data class AssetReference(
    val light: String,
    val dark: String,
    val doze: String? = null,
    val lightTint: String? = null,
    val darkTint: String? = null,
    val dozeTint: String? = null,
)

sealed interface ClockLayer {
    val layerBounds: LayerBounds?
}

data class AssetLayer(val asset: AssetReference, override val layerBounds: LayerBounds? = null) :
    ClockLayer

data class AnalogHandLayer(
    val timespec: AnalogTimespec,
    val tickMode: AnalogTickMode,
    val asset: AssetReference,
    override val layerBounds: LayerBounds? = null,
) : ClockLayer

data class DigitalHandLayer(
    val timespec: DigitalTimespec,
    val font: FontSpec? = null,
    val style: TextStyle,
    val aodStyle: TextStyle? = null,
    val layerBounds: LayerBounds? = null,
    val faceLayout: DigitalFaceLayout? = null,
    val dateTimeFormat: String,
    val alignment: DigitalAlignment? = null,
    val marginRatio: DigitalMarginRatio = DigitalMarginRatio(),
    val timeZoneId: String? = null,
) : ClockLayer

data class ComposedDigitalHandLayer(
    val customizedView: String,
    val font: FontSpec? = null,
    val digitalLayers: List<DigitalHandLayer> = emptyList(),
    override val layerBounds: LayerBounds? = null,
) : ClockLayer

data class FontSpec(val source: FontSource, val variations: DigitalClockTextView.FontVariations)

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
    override val transitionDuration: Long = 300,
    override val transitionInterpolator: android.view.animation.Interpolator? = null,
) : com.android.systemui.customization.clocks.FontTextStyle

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

data class ClockFace(
    val layers: List<ClockLayer> = emptyList(),
    val layerBounds: LayerBounds = LayerBounds.FIT,
    val wallpaper: String? = null,
    val faceLayout: DigitalFaceLayout? = null,
    val pickerScale: ClockFaceScaleInPicker = ClockFaceScaleInPicker(),
)

data class ClockDesign(
    val id: String,
    val name: String? = null,
    val description: String? = null,
    val thumbnail: String? = null,
    val large: ClockFace,
    val small: ClockFace? = null,
    val colorPalette: Int? = null,
)

class DimensionParser(private val ctx: Context) {
    fun convert(dimen: String?): Float {
        if (dimen == null) return 0f
        val match = PATTERN.matchEntire(dimen.trim())
        requireNotNull(match) { "Failed to parse dimension: $dimen" }
        val value = match.groupValues[1].toFloat()
        val unit =
            UNIT_MAP[match.groupValues[3]]
                ?: throw IllegalArgumentException("Unknown dimension unit: $dimen")
        return TypedValue.applyDimension(unit, value, ctx.resources.displayMetrics)
    }

    private companion object {
        val PATTERN = Regex("(\\d+(\\.\\d+)?)([a-z]+)")
        val UNIT_MAP =
            mapOf(
                "dp" to TypedValue.COMPLEX_UNIT_DIP,
                "dip" to TypedValue.COMPLEX_UNIT_DIP,
                "sp" to TypedValue.COMPLEX_UNIT_SP,
                "px" to TypedValue.COMPLEX_UNIT_PX,
                "pt" to TypedValue.COMPLEX_UNIT_PT,
                "in" to TypedValue.COMPLEX_UNIT_IN,
                "mm" to TypedValue.COMPLEX_UNIT_MM,
            )
    }
}

fun lookupDigitalLayerId(layer: DigitalHandLayer): Int {
    val isHour = layer.dateTimeFormat.contains("h")
    return layer.timespec.getViewId(isHour)
}
