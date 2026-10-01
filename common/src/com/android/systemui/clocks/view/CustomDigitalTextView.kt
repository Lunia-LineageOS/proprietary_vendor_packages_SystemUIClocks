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

package com.android.systemui.clocks.view

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.text.Layout
import android.text.TextPaint
import androidx.core.graphics.withSave
import com.android.systemui.animation.TextAnimator
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.CustomFontTextStyle
import com.android.systemui.clocks.DimensionParser
import com.android.systemui.clocks.FontSpec
import com.android.systemui.clocks.RenderType
import com.android.systemui.customization.clocks.TextStyle
import com.android.systemui.customization.clocks.utils.CanvasUtils.withLayer
import com.android.systemui.customization.clocks.utils.ViewUtils.size
import com.android.systemui.customization.clocks.view.DigitalClockTextView
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.VRectF
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAxisStyle
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig
import kotlin.math.ceil
import kotlin.math.max

@SuppressLint("ViewConstructor")
open class CustomDigitalTextView(private val clockCtx: CustomClockContext, font: FontSpec) :
    DigitalClockTextView(clockCtx), ICustomDigitalTextView {
    private val parser = DimensionParser(clockCtx.context)
    override val typefaceCache = clockCtx.typefaceCache.getVariantCache(font)
    override var fontVariations = font.variations

    var textStyle = CustomFontTextStyle()
        private set

    var aodStyle: CustomFontTextStyle? = null
        private set

    private var renderType = RenderType.STROKE_TEXT
    private var textBorderWidth = 0f

    var aodBorderWidth = 0f
        private set

    private var textOutlineColor = Color.TRANSPARENT
    private var aodOutlineColor = Color.WHITE

    lateinit var innerAnimator: TextAnimator
        private set

    lateinit var outlineAnimator: TextAnimator
        private set

    override fun updateFontVariations(lsAxes: ClockAxisStyle) = fontVariations

    override fun initializeAnimators(layout: Layout) {
        super.initializeAnimators(layout)
        innerAnimator = TextAnimator(layout, typefaceCache, null)
        outlineAnimator = TextAnimator(layout, typefaceCache, null)
    }

    override fun updateAnimators(layout: Layout) {
        super.updateAnimators(layout)
        innerAnimator.updateLayout(layout)
        outlineAnimator.updateLayout(layout)
    }

    override fun updateTheme(theme: ThemeConfig) {
        val lockscreenColorRes =
            if (theme.isDarkTheme) textStyle.fillColorLight else textStyle.fillColorDark
        val aodColorRes = aodStyle?.let {
            if (theme.isDarkTheme) it.fillColorLight else it.fillColorDark
        }

        val assets = clockCtx.assets
        val lockscreenColor =
            assets.tryReadColor(lockscreenColorRes) ?: theme.getDefaultColor(clockCtx.context)
        val aodColor =
            assets.tryReadColor(aodColorRes)
                ?: when (renderType) {
                    RenderType.CHANGE_WEIGHT -> theme.getAodColor(clockCtx.context)
                    RenderType.HOLLOW_TEXT,
                    RenderType.STROKE_TEXT,
                    RenderType.OUTER_OUTLINE_TEXT -> Color.TRANSPARENT
                }
        updateColor(lockscreenColor, aodColor)

        textOutlineColor = assets.tryReadColor(textStyle.outlineColor) ?: Color.TRANSPARENT
        aodOutlineColor = assets.tryReadColor(aodStyle?.outlineColor) ?: Color.WHITE
        outlineAnimator.setTextStyle(
            TextAnimator.Style(color = if (dozeFraction < 1f) textOutlineColor else aodOutlineColor)
        )
    }

    override fun drawAnimators(canvas: Canvas, drawTranslation: VPointF) {
        val strokeOffset = VPointF(lockscreenPaint.strokeWidth)
        val bounds =
            VRectF.fromTopLeft((drawTranslation + strokeOffset) * -1, size + strokeOffset * 2)

        canvas.withSave {
            when (renderType) {
                RenderType.CHANGE_WEIGHT -> {}
                RenderType.HOLLOW_TEXT ->
                    withLayer(bounds) {
                        outlineAnimator.draw(this)
                        withLayer(bounds, INNER_ANIMATOR_PAINT) { innerAnimator.draw(this) }
                    }
                RenderType.STROKE_TEXT,
                RenderType.OUTER_OUTLINE_TEXT -> withLayer(bounds) { outlineAnimator.draw(this) }
            }
        }
        super.drawAnimators(canvas, drawTranslation)
    }

    override fun onAnimateDoze(
        isDozing: Boolean,
        style: TextAnimator.Style,
        animation: TextAnimator.Animation,
    ) {
        super.onAnimateDoze(isDozing, style, animation)
        outlineAnimator.setTextStyle(
            style.copy(
                color = if (isDozing) aodOutlineColor else textOutlineColor,
                strokeWidth = if (isDozing) aodBorderWidth else textBorderWidth,
            ),
            animation,
        )
        innerAnimator.setTextStyle(style.copy(color = Color.WHITE), animation)
    }

    override fun onAnimateTransient(
        targetStyle: TextAnimator.Style,
        returnStyle: TextAnimator.Style,
        animation: TextAnimator.Animation,
    ) {
        super.onAnimateTransient(targetStyle, returnStyle, animation)
        outlineAnimator.setTransientTextStyle(targetStyle, returnStyle, animation)
        innerAnimator.setTransientTextStyle(targetStyle, returnStyle, animation)
    }

    override fun applyStyles(textStyle: TextStyle, aodStyle: TextStyle?) {
        if (textStyle is CustomFontTextStyle && aodStyle is CustomFontTextStyle?) {
            this.textStyle = textStyle
            this.aodStyle = aodStyle
            renderType = aodStyle?.renderType ?: textStyle.renderType
            textBorderWidth = parser.convert(textStyle.borderWidth ?: "2dp")
            aodBorderWidth = parser.convert(aodStyle?.borderWidth ?: "2dp")
            lockscreenPaint.strokeWidth = ceil(max(textBorderWidth, aodBorderWidth))
        }
        super.applyStyles(textStyle, aodStyle)
    }

    override fun setInterpolatorPaint(isDozing: Boolean, textStyle: TextAnimator.Style) {
        super.setInterpolatorPaint(isDozing, textStyle)

        outlineAnimator.textInterpolator.targetPaint.set(
            TextPaint(lockscreenPaint).apply {
                style =
                    when (renderType) {
                        RenderType.CHANGE_WEIGHT -> Paint.Style.STROKE
                        RenderType.HOLLOW_TEXT -> Paint.Style.FILL_AND_STROKE
                        RenderType.STROKE_TEXT,
                        RenderType.OUTER_OUTLINE_TEXT -> Paint.Style.STROKE
                    }
            }
        )
        outlineAnimator.textInterpolator.onTargetPaintModified()
        outlineAnimator.setTextStyle(
            textStyle.copy(
                color = if (isDozing) aodOutlineColor else textOutlineColor,
                strokeWidth = if (isDozing) aodBorderWidth else textBorderWidth,
            )
        )

        innerAnimator.textInterpolator.targetPaint.set(
            TextPaint(lockscreenPaint).apply { style = Paint.Style.FILL }
        )
        innerAnimator.textInterpolator.onTargetPaintModified()
        innerAnimator.setTextStyle(textStyle.copy(color = Color.WHITE))
    }

    companion object {
        private val INNER_ANIMATOR_PAINT =
            Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }
    }
}
