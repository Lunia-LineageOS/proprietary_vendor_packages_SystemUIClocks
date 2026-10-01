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
    private val dozeAsset = asset.doze?.let { loader.tryReadDrawableAsset(it) }

    private var masterAlpha = 255
    private var lightFraction = 1f
    private var dozeFraction = 0f

    init {
        updateTints()
    }

    private fun tryDraw(canvas: Canvas, drawable: Drawable?, fraction: Float) {
        if (drawable == null || fraction <= 0f) return
        drawable.alpha = (masterAlpha * fraction).roundToInt()
        drawable.draw(canvas)
    }

    private fun trySetTint(drawable: Drawable?, tint: String?) {
        if (drawable == null) return
        val color = loader.tryReadColor(tint)
        if (color != null) {
            drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
        } else {
            drawable.colorFilter = null
        }
    }

    private fun updateChildBounds(rect: Rect) {
        lightAsset?.bounds = rect
        darkAsset?.bounds = rect
        dozeAsset?.bounds = rect
    }

    override fun draw(canvas: Canvas) {
        if (masterAlpha <= 0) return
        tryDraw(canvas, lightAsset, lightFraction * (1f - dozeFraction))
        tryDraw(canvas, darkAsset, (1f - lightFraction) * (1f - dozeFraction))
        tryDraw(canvas, dozeAsset, dozeFraction)
    }

    override fun getAlpha(): Int = masterAlpha

    override fun setAlpha(alpha: Int) {
        masterAlpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int {
        var width = lightAsset?.intrinsicWidth ?: 0
        width = max(width, darkAsset?.intrinsicWidth ?: 0)
        width = max(width, dozeAsset?.intrinsicWidth ?: 0)
        return width
    }

    override fun getIntrinsicHeight(): Int {
        var height = lightAsset?.intrinsicHeight ?: 0
        height = max(height, darkAsset?.intrinsicHeight ?: 0)
        height = max(height, dozeAsset?.intrinsicHeight ?: 0)
        return height
    }

    override fun onBoundsChange(bounds: Rect) {
        updateChildBounds(bounds)
    }

    override fun onLayoutDirectionChanged(layoutDirection: Int): Boolean {
        val changed =
            listOfNotNull(lightAsset, darkAsset, dozeAsset).fold(false) { acc, drawable ->
                drawable.setLayoutDirection(layoutDirection) || acc
            }
        updateChildBounds(bounds)
        return changed
    }

    override fun onLevelChange(level: Int): Boolean {
        updateChildBounds(bounds)
        return false
    }

    override fun onStateChange(state: IntArray): Boolean {
        updateChildBounds(bounds)
        return false
    }

    fun setDozeFraction(fraction: Float) {
        if (dozeFraction == fraction) return
        dozeFraction = fraction
        invalidateSelf()
    }

    fun setLightFraction(fraction: Float) {
        if (lightFraction == fraction) return
        lightFraction = fraction
        invalidateSelf()
    }

    fun updateTints() {
        trySetTint(lightAsset, asset.lightTint)
        trySetTint(darkAsset, asset.darkTint)
        trySetTint(dozeAsset, asset.dozeTint)
        invalidateSelf()
    }
}
