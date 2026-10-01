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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.text.Layout
import android.text.TextPaint
import com.android.systemui.animation.TextAnimator
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.CustomFontTextStyle
import com.android.systemui.clocks.DimensionParser
import com.android.systemui.clocks.FontSpec
import com.android.systemui.clocks.RenderType
import com.android.systemui.customization.clocks.TextStyle
import com.android.systemui.customization.clocks.utils.CanvasUtils.withLayer
import com.android.systemui.customization.clocks.utils.ViewUtils.measuredSize
import com.android.systemui.customization.clocks.view.DigitalClockTextView
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.VRectF
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAxisStyle
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig
import java.util.Locale
import kotlin.math.ceil

open class CustomDigitalTextView(clockCtx: CustomClockContext, fontSpec: FontSpec) :
    DigitalClockTextView(clockCtx), ICustomDigitalTextView {
    private val clockContext = clockCtx
    private val parser = DimensionParser(clockCtx.context)

    override val typefaceCache = clockCtx.typefaceCache.getVariantCache(fontSpec)
    final override var fontVariations = fontSpec.variations
        private set

    private var currentStyle: CustomFontTextStyle = CustomFontTextStyle()
    private var currentAodStyle: CustomFontTextStyle = currentStyle
    private var renderType = RenderType.STROKE_TEXT
    private var textBorderWidth = 0f
    var aodBorderWidth = 0f
        private set

    private var textOutlineColor = 0
    private var aodOutlineColor = -1
    private var innerAnimator: TextAnimator? = null
    private var outlineAnimator: TextAnimator? = null

    override val textStyle: TextStyle
        get() = currentStyle

    override val aodStyle: TextStyle?
        get() = currentAodStyle

    override fun updateFontVariations(lsAxes: ClockAxisStyle): FontVariations {
        return fontSpec.variations
    }

    override fun applyStyles(textStyle: TextStyle, aodStyle: TextStyle?) {
        if (textStyle is CustomFontTextStyle) {
            currentStyle = textStyle
            currentAodStyle = (aodStyle as? CustomFontTextStyle) ?: textStyle
            renderType = currentAodStyle.renderType
            textBorderWidth = parser.convert(currentStyle.borderWidth ?: DEFAULT_BORDER_WIDTH)
            aodBorderWidth = parser.convert(currentAodStyle.borderWidth ?: DEFAULT_BORDER_WIDTH)
            lockscreenPaint.strokeWidth =
                ceil(maxOf(textBorderWidth, aodBorderWidth).toDouble()).toFloat()
        }
        super.applyStyles(textStyle, aodStyle)
    }

    override fun initializeAnimators(layout: Layout) {
        super.initializeAnimators(layout)
        innerAnimator = TextAnimator(layout, typefaceCache)
        outlineAnimator = TextAnimator(layout, typefaceCache)
    }

    override fun updateAnimators(layout: Layout) {
        super.updateAnimators(layout)
        innerAnimator?.updateLayout(layout)
        outlineAnimator?.updateLayout(layout)
    }

    override fun drawAnimators(canvas: Canvas, drawTranslation: VPointF) {
        val strokeWidth = lockscreenPaint.strokeWidth
        val expanded =
            VRectF.fromTopLeft(
                VPointF(drawTranslation.x - strokeWidth, drawTranslation.y - strokeWidth),
                measuredSize.let { VPointF(it.x + strokeWidth * 2f, it.y + strokeWidth * 2f) },
            )
        canvas.save()
        when (renderType) {
            RenderType.CHANGE_WEIGHT -> Unit
            RenderType.HOLLOW_TEXT -> {
                canvas.withLayer(expanded) {
                    outlineAnimator?.draw(this)
                    withLayer(expanded, INNER_ANIMATOR_PAINT) { innerAnimator?.draw(this) }
                }
            }
            RenderType.STROKE_TEXT,
            RenderType.OUTER_OUTLINE_TEXT -> {
                canvas.withLayer(expanded) { outlineAnimator?.draw(this) }
            }
        }
        canvas.restore()
        super.drawAnimators(canvas, drawTranslation)
    }

    override fun onAnimateDoze(
        isDozing: Boolean,
        style: TextAnimator.Style,
        animation: TextAnimator.Animation,
    ) {
        super.onAnimateDoze(isDozing, style, animation)
        outlineAnimator?.setTextStyle(
            TextAnimator.Style(
                color = if (isDozing) aodOutlineColor else textOutlineColor,
                strokeWidth = if (isDozing) aodBorderWidth else textBorderWidth,
            ),
            animation,
        )
        innerAnimator?.setTextStyle(TextAnimator.Style(color = Color.WHITE), animation)
    }

    override fun onAnimateTransient(
        targetStyle: TextAnimator.Style,
        returnStyle: TextAnimator.Style,
        animation: TextAnimator.Animation,
    ) {
        super.onAnimateTransient(targetStyle, returnStyle, animation)
        outlineAnimator?.setTextStyle(
            targetStyle,
            animation.copy(
                onAnimationEnd = { outlineAnimator?.setTextStyle(returnStyle, animation) }
            ),
        )
        innerAnimator?.setTextStyle(
            targetStyle,
            animation.copy(onAnimationEnd = { innerAnimator?.setTextStyle(returnStyle, animation) }),
        )
    }

    override fun setInterpolatorPaint(isDozing: Boolean, textStyle: TextAnimator.Style) {
        super.setInterpolatorPaint(isDozing, textStyle)
        outlineAnimator?.let { outline ->
            val outlinePaint = TextPaint(lockscreenPaint)
            outlinePaint.style =
                when (renderType) {
                    RenderType.HOLLOW_TEXT -> Paint.Style.FILL_AND_STROKE
                    else -> Paint.Style.STROKE
                }
            outline.textInterpolator.targetPaint.set(outlinePaint)
            outline.textInterpolator.onTargetPaintModified()
            outline.setTextStyle(
                TextAnimator.Style(
                    color = if (isDozing) aodOutlineColor else textOutlineColor,
                    strokeWidth = if (isDozing) aodBorderWidth else textBorderWidth,
                )
            )
        }
        innerAnimator?.let { inner ->
            val innerPaint = TextPaint(lockscreenPaint)
            innerPaint.style = Paint.Style.FILL
            inner.textInterpolator.targetPaint.set(innerPaint)
            inner.textInterpolator.onTargetPaintModified()
            inner.setTextStyle(TextAnimator.Style(color = Color.WHITE))
        }
    }

    override fun updateTheme(theme: ThemeConfig) {
        val lsColorToken =
            if (theme.isDarkTheme) currentStyle.fillColorLight else currentStyle.fillColorDark
        val aodColorToken = currentAodStyle.let {
            if (theme.isDarkTheme) it.fillColorLight else it.fillColorDark
        }
        val assets = clockContext.assets
        val lsFillColor =
            assets.tryReadColor(lsColorToken) ?: theme.getDefaultColor(clockContext.context)
        val aodFillColor =
            assets.tryReadColor(aodColorToken)
                ?: when (renderType) {
                    RenderType.CHANGE_WEIGHT -> theme.getAodColor(clockContext.context)
                    RenderType.HOLLOW_TEXT,
                    RenderType.STROKE_TEXT,
                    RenderType.OUTER_OUTLINE_TEXT -> 0
                }
        updateColor(lsFillColor, aodFillColor)
        textOutlineColor = assets.tryReadColor(currentStyle.outlineColor) ?: 0
        aodOutlineColor = assets.tryReadColor(currentAodStyle.outlineColor) ?: -1
        outlineAnimator?.setTextStyle(
            TextAnimator.Style(color = if (dozeFraction < 1f) textOutlineColor else aodOutlineColor)
        )
    }

    override fun applyCustomTextSize(fontSizePx: Float?, constrainedByHeight: Boolean) {
        applyTextSize(fontSizePx, constrainedByHeight)
    }

    override fun notifyAxesChanged(axes: ClockAxisStyle, isAnimated: Boolean) {
        // Not reactive to font axes
    }

    override fun onLocaleChanged(locale: Locale) {
        refreshText()
    }

    override var clockVerticalAlignment: VerticalAlignment
        get() = verticalAlignment
        set(value) {
            verticalAlignment = value
        }

    override var clockHorizontalAlignment: HorizontalAlignment
        get() = horizontalAlignment
        set(value) {
            horizontalAlignment = value
        }

    companion object {
        private const val DEFAULT_BORDER_WIDTH = "2dp"
        private val INNER_ANIMATOR_PAINT =
            Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }
    }
}
