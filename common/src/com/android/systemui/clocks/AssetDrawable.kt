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

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.Drawable
import kotlin.math.max
import kotlin.math.roundToInt

class AssetDrawable(private val loader: AssetLoader, private val asset: AssetReference) :
    Drawable() {
    private val lightAsset = loader.tryReadDrawableAsset(asset.light)
    private val darkAsset = loader.tryReadDrawableAsset(asset.dark)
    private val dozeAsset = loader.tryReadDrawableAsset(asset.doze)
    private var masterAlpha = 255

    init {
        updateTints()
    }

    var lightFraction = 1f
        set(value) {
            if (field == value) return
            field = value
            invalidateSelf()
        }

    var dozeFraction = 0f
        set(value) {
            if (field == value) return
            field = value
            invalidateSelf()
        }

    fun updateTints() {
        trySetTint(lightAsset, asset.lightTint)
        trySetTint(darkAsset, asset.darkTint)
        trySetTint(dozeAsset, asset.dozeTint)
        invalidateSelf()
    }

    private fun trySetTint(drawable: Drawable?, tint: String?) {
        if (drawable == null || tint == null) return
        val color = loader.tryReadColor(tint)
        if (color != null) {
            drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
        } else {
            drawable.colorFilter = null
        }
    }

    override fun getIntrinsicHeight(): Int {
        return max(
            max(lightAsset?.intrinsicHeight ?: 0, darkAsset?.intrinsicHeight ?: 0),
            dozeAsset?.intrinsicHeight ?: 0,
        )
    }

    override fun getIntrinsicWidth(): Int {
        return max(
            max(lightAsset?.intrinsicWidth ?: 0, darkAsset?.intrinsicWidth ?: 0),
            dozeAsset?.intrinsicWidth ?: 0,
        )
    }

    override fun draw(canvas: Canvas) {
        if (masterAlpha <= 0) return
        tryDraw(canvas, lightAsset, lightFraction * (1f - dozeFraction))
        tryDraw(canvas, darkAsset, (1f - lightFraction) * (1f - dozeFraction))
        tryDraw(canvas, dozeAsset, dozeFraction)
    }

    private fun tryDraw(canvas: Canvas, drawable: Drawable?, fraction: Float) {
        if (drawable == null || fraction <= 0f) return
        drawable.alpha = (masterAlpha * fraction).roundToInt()
        drawable.draw(canvas)
    }

    override fun getAlpha(): Int = masterAlpha

    override fun setAlpha(alpha: Int) {
        masterAlpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun onStateChange(state: IntArray): Boolean {
        updateChildBounds(bounds)
        return false
    }

    override fun onLevelChange(level: Int): Boolean {
        updateChildBounds(bounds)
        return false
    }

    override fun onBoundsChange(bounds: Rect) {
        updateChildBounds(bounds)
    }

    override fun onLayoutDirectionChanged(layoutDirection: Int): Boolean {
        var result = lightAsset?.setLayoutDirection(layoutDirection) ?: false
        result = (darkAsset?.setLayoutDirection(layoutDirection) ?: false) || result
        result = (dozeAsset?.setLayoutDirection(layoutDirection) ?: false) || result
        updateChildBounds(bounds)
        return result
    }

    private fun updateChildBounds(bounds: Rect) {
        lightAsset?.bounds = bounds
        darkAsset?.bounds = bounds
        dozeAsset?.bounds = bounds
    }
}
