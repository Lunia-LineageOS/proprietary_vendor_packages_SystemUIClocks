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

import com.android.systemui.customization.clocks.DigitalTimespec
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds
import java.text.SimpleDateFormat

class ClockDesignValidator(private val loader: AssetLoader) {
    val errors = mutableListOf<String>()

    val isValid: Boolean
        get() = errors.size <= 0

    fun validate(design: ClockDesign) {
        validateResource(design.thumbnail)
        validate(design.large)
        validate(design.small)
    }

    private fun validate(face: ClockFace?) {
        if (face == null) return
        face.layers.forEach { validate(it) }
        validateDigitalClockFace(face)
    }

    private fun validate(layer: ClockLayer) {
        when (layer) {
            is AssetLayer -> validateAsset(layer.asset)
            is AnalogHandLayer -> validateAsset(layer.asset)
            is DigitalHandLayer -> validateDigitalClockHandLayer(layer)
            is ComposedDigitalHandLayer -> validateComposedDigitalHandLayer(layer)
            else -> throw IllegalArgumentException("Unrecognized layer: ${layer::class.simpleName}")
        }
    }

    private fun validateAsset(asset: AssetReference) {
        validateResource(asset.light)
        validateResource(asset.dark)
        validateResource(asset.doze)
        validateColor(asset.lightTint)
        validateColor(asset.darkTint)
        validateColor(asset.dozeTint)
    }

    private fun validateResource(path: String?) {
        if (path == null) return
        check(loader.assetExists(path), "Couldn't find resource at $path")
    }

    private fun validateColor(color: String?) {
        if (color == null) return
        check(loader.tryReadColor(color) != null, "Couldn't find color at $color")
    }

    private fun check(isValid: Boolean, error: String) {
        if (isValid) return
        errors.add(error)
    }

    private fun validateDigitalClockFace(face: ClockFace) {
        validateDigitalTimespecSetValid(face)
    }

    private fun validateDigitalTimespecSetValid(face: ClockFace) {
        if (face.layers.any { it is DigitalHandLayer }) {
            val ids = buildSet {
                for (layer in face.layers) {
                    if (
                        layer is DigitalHandLayer && layer.timespec != DigitalTimespec.DATE_FORMAT
                    ) {
                        add(lookupDigitalLayerId(layer))
                    }
                }
            }
            check(
                validDigitalTimespecSets.contains(ids),
                "Digital clock lack some elements, current set is $ids",
            )
        }
    }

    private fun validateDigitalClockHandLayer(layer: DigitalHandLayer) {
        validateDigitalClockTimeFormat(layer)
        when (val style = layer.style) {
            is CustomFontTextStyle -> {
                check(layer.font != null, "Layer must specify font")
                check(
                    (style.fillColorLight != null) == (style.fillColorDark != null),
                    "Style should specify both a light and dark fill color, or neither",
                )
            }
            is LottieTextStyle -> {
                check(style.numbers.size == 10, "Style has must have exactly 10 number drawables")
                check(
                    (style.fillColorDarkMap != null) == (style.fillColorLightMap != null),
                    "Style should specify both a light and dark fill color, or neither",
                )
            }
            else -> errors.add("Unrecognized text style type: $style")
        }
    }

    private fun validateDigitalClockTimeFormat(layer: DigitalHandLayer) {
        try {
            SimpleDateFormat(layer.dateTimeFormat)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("DateTimeFormat for ${layer.timespec} is not valid")
        }
    }

    private fun validateComposedDigitalHandLayer(layer: ComposedDigitalHandLayer) {
        layer.digitalLayers.forEach { validateDigitalClockHandLayer(it) }
    }

    companion object {
        private val fourDigitsClockIds =
            setOf(
                ClockViewIds.HOUR_FIRST_DIGIT,
                ClockViewIds.HOUR_SECOND_DIGIT,
                ClockViewIds.MINUTE_FIRST_DIGIT,
                ClockViewIds.MINUTE_SECOND_DIGIT,
            )
        private val twoPairsClockIds =
            setOf(ClockViewIds.HOUR_DIGIT_PAIR, ClockViewIds.MINUTE_DIGIT_PAIR)
        private val fullFormat = setOf(ClockViewIds.TIME_FULL_FORMAT)
        private val validDigitalTimespecSets =
            setOf(fourDigitsClockIds, twoPairsClockIds, fullFormat)
    }
}
