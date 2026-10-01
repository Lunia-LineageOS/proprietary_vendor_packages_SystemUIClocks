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

package com.android.systemui.clocks.metro.view

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.SimpleLottieValueCallback
import com.android.app.animation.Interpolators
import com.android.systemui.clocks.AssetLoader
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.DimensionParser
import com.android.systemui.clocks.LottieTextStyle
import com.android.systemui.clocks.view.ICustomDigitalTextView
import com.android.systemui.customization.clocks.TextStyle
import com.android.systemui.customization.clocks.utils.ViewUtils.measuredSize
import com.android.systemui.customization.clocks.view.HorizontalAlignment
import com.android.systemui.customization.clocks.view.VerticalAlignment
import com.android.systemui.plugins.keyguard.VMeasurePoint
import com.android.systemui.plugins.keyguard.VMeasureSpec
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.VRectF
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAxisStyle
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max

fun readLottieComposition(clockCtx: CustomClockContext, asset: String): LottieComposition? =
    readLottieComposition(clockCtx.assets, asset)

fun readLottieComposition(assetLoader: AssetLoader, asset: String): LottieComposition? {
    return try {
        LottieCompositionFactory.fromJsonStringSync(assetLoader.readTextAsset(asset), asset).value
    } catch (e: Exception) {
        null
    }
}

class LottieDigitalTextView(
    private val clockCtx: CustomClockContext,
    private val isLargeClock: Boolean,
) : View(clockCtx.context), ICustomDigitalTextView {
    private val parser = DimensionParser(clockCtx.context)
    private var textStyle: LottieTextStyle = LottieTextStyle()
    private var numbers: List<LottieDrawable> = emptyList()
    private var colon: LottieDrawable? = null
    private var lightColorMap: Map<String, String> = emptyMap()
    private var darkColorMap: Map<String, String> = emptyMap()
    private var letterSpacing = 0f
    private var paddingHorizontal = 0f
    private var paddingVertical = 0f
    private var innerWidth = 0f
    private var innerHeight = 0f
    private var lastUnconstrainedFontSizePx = Float.MAX_VALUE

    override var clockVerticalAlignment = VerticalAlignment.CENTER
    override var clockHorizontalAlignment = HorizontalAlignment.CENTER

    override val textStyle: TextStyle
        get() = this.textStyle

    override val aodStyle: TextStyle?
        get() = null

    override var text: String = ""
        set(value) {
            field = value
            requestLayout()
        }

    override var dozeFraction: Float = 0f
        set(value) {
            field = value
            refreshAlphaByFraction(value)
            invalidate()
        }

    override var maxSize = VPointF(-1f)
        private set

    override var onViewBoundsChanged: ((VRectF) -> Unit)? = null
    override var onViewMaxSizeChanged: ((VPointF) -> Unit)? = null

    private var animationState = 0
    private var startProgress = -1f
    private var lastProgress = 0f

    private val lottieAnimator =
        LottieAnimator(
            updateCallback = { fraction ->
                var start = getStartProgress()
                var endProgress = getEndProgress()
                if (start > endProgress) {
                    endProgress += 1f
                }
                val progress = (endProgress - start) * fraction + start
                syncLottieProgress(progress - floor(progress))
                invalidate()
            },
            startCallback = {
                startProgress = lastProgress
                updateAnimationState()
            },
        )

    init {
        setWillNotDraw(false)
        val paint =
            Paint().apply {
                isDither = true
                isAntiAlias = true
                isFilterBitmap = true
            }
        setLayerType(View.LAYER_TYPE_HARDWARE, paint)
    }

    private fun getStartProgress(): Float {
        val progress = startProgress
        return if (progress == -1f) animationState * 0.25f else progress
    }

    private fun getEndProgress(): Float = (animationState + 1) * 0.25f

    private fun updateAnimationState() {
        val progress = getStartProgress()
        startProgress = progress - floor(progress)
        animationState = Math.rint(startProgress / 0.25f).toInt() % 4
    }

    private fun syncLottieProgress(progress: Float) {
        lastProgress = progress
        text.toSet().forEach { c -> getDrawable(c)?.setProgress(progress) }
    }

    private fun refreshAlphaByFraction(fraction: Float) {
        text.forEach { c ->
            val drawable = getDrawable(c) ?: return@forEach
            COLOR_STROKE_KEYPATH_LIST.forEach { name ->
                drawable.addValueCallback(
                    KeyPath("**", name),
                    LottieProperty.OPACITY,
                    SimpleLottieValueCallback<Integer> {
                        Integer.valueOf(((1f - fraction) * 100f).toInt())
                    },
                )
            }
            drawable.addValueCallback(
                KeyPath("**", "#FFFFFF"),
                LottieProperty.OPACITY,
                SimpleLottieValueCallback<Integer> { Integer.valueOf((fraction * 100f).toInt()) },
            )
        }
    }

    private fun getDrawable(c: Char): LottieDrawable? {
        if (c == ':') return colon
        val digit = Character.getNumericValue(c)
        return if (digit in 0..9) numbers[digit] else null
    }

    private fun getLocalTranslationX(): Float {
        return when (clockHorizontalAlignment) {
            HorizontalAlignment.CENTER -> (measuredWidth - innerWidth) / 2f
            HorizontalAlignment.RIGHT,
            HorizontalAlignment.END -> measuredWidth - innerWidth
            else -> 0f
        }
    }

    private fun getLocalTranslationY(): Float {
        return when (clockVerticalAlignment) {
            VerticalAlignment.CENTER -> (measuredHeight - innerHeight) / 2f
            VerticalAlignment.BOTTOM -> measuredHeight - innerHeight
            else -> 0f
        }
    }

    private fun drawNumbers(canvas: Canvas) {
        canvas.save()
        try {
            canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
            canvas.translate(getLocalTranslationX(), getLocalTranslationY())
            text.forEach { c ->
                val drawable = getDrawable(c)
                if (drawable != null) {
                    drawable.draw(canvas)
                    canvas.translate(drawable.bounds.width() + letterSpacing, 0f)
                }
            }
        } finally {
            canvas.restore()
        }
    }

    override fun onDraw(canvas: Canvas) {
        drawNumbers(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val specs = VMeasurePoint.fromSpecs(widthMeasureSpec, heightMeasureSpec)
        innerWidth = 0f
        innerHeight = 0f
        text.forEach { c ->
            val bounds = getDrawable(c)?.bounds
            if (bounds != null) {
                if (innerWidth > 0f) {
                    innerWidth += letterSpacing
                }
                innerHeight = max(innerHeight, bounds.height().toFloat())
                innerWidth += bounds.width()
            }
        }
        innerWidth += paddingHorizontal * 2f
        innerHeight += paddingVertical * 2f

        val width =
            when (specs.width.mode) {
                VMeasureSpec.Mode.EXACTLY -> specs.width.size
                VMeasureSpec.Mode.AT_MOST -> minOf(specs.width.size, innerWidth.toInt())
                else -> innerWidth.toInt()
            }
        val height =
            when (specs.height.mode) {
                VMeasureSpec.Mode.EXACTLY -> specs.height.size
                VMeasureSpec.Mode.AT_MOST -> minOf(specs.height.size, innerHeight.toInt())
                else -> innerHeight.toInt()
            }
        setMeasuredDimension(width, height)
        maxSize = measuredSize
        onViewMaxSizeChanged?.let { it(maxSize) }
    }

    override fun refreshText() {
        refreshAlphaByFraction(dozeFraction)
        invalidate()
    }

    override fun animateCharge() {
        lottieAnimator.start()
    }

    override fun animateDoze(isDozing: Boolean, isAnimated: Boolean) {
        if (isAnimated) {
            lottieAnimator.start()
        }
    }

    override fun applyStyles(textStyle: TextStyle, aodStyle: TextStyle?) {
        (textStyle as? LottieTextStyle)?.let { style ->
            this.textStyle = style
            val assets = if (isLargeClock) largeTransitClockAssets else smallTransitClockAssets
            if (numbers.isEmpty()) {
                numbers = assets.getNumbersList(clockCtx.assets, style.numbers)
            }
            colon = style.colon?.let { assets.getColon(clockCtx.assets, it) }
            style.fillColorLightMap?.let { lightColorMap = it }
            style.fillColorDarkMap?.let { darkColorMap = it }
        }
    }

    override fun applyCustomTextSize(fontSizePx: Float?, constrainedByHeight: Boolean) {
        val unconstrained =
            if (constrainedByHeight) {
                minOf(fontSizePx ?: 0f, lastUnconstrainedFontSizePx)
            } else {
                (fontSizePx ?: 0f).also { lastUnconstrainedFontSizePx = it }
            }
        val scale =
            (unconstrained * textStyle.fontSizeScale) /
                (numbers.firstOrNull()?.intrinsicHeight?.toFloat() ?: 1f)
        letterSpacing = parser.convert(textStyle.spacing) * scale
        paddingVertical = parser.convert(textStyle.paddingVertical) * scale
        paddingHorizontal = parser.convert(textStyle.paddingHorizontal) * scale
        (numbers + listOfNotNull(colon)).forEach { drawable ->
            drawable.setBounds(
                0,
                0,
                (drawable.intrinsicWidth * scale - paddingHorizontal * 2f).toInt(),
                (drawable.intrinsicHeight * scale - paddingVertical * 2f).toInt(),
            )
        }
        refreshText()
    }

    override fun updateTheme(theme: ThemeConfig) {
        val colorMap = if (theme.isDarkTheme) lightColorMap else darkColorMap
        val baseColor = clockCtx.assets.readColor("@android:color/system_accent1_100")
        numbers.forEach { drawable ->
            colorMap.forEach { (keyPathName, colorRef) ->
                val color = clockCtx.assets.readColor(colorRef)
                drawable.addValueCallback(
                    KeyPath("**", keyPathName),
                    LottieProperty.STROKE_COLOR,
                    SimpleLottieValueCallback<Integer> { Integer.valueOf(color) },
                )
            }
            drawable.addValueCallback(
                KeyPath("**", "#FFFFFF"),
                LottieProperty.STROKE_COLOR,
                SimpleLottieValueCallback<Integer> { Integer.valueOf(baseColor) },
            )
            drawable.addValueCallback(
                KeyPath("**", "#FFFFFF"),
                LottieProperty.COLOR,
                SimpleLottieValueCallback<Integer> { Integer.valueOf(baseColor) },
            )
        }
        invalidate()
    }

    override fun notifyAxesChanged(axes: ClockAxisStyle, isAnimated: Boolean) = Unit

    override fun onLocaleChanged(locale: Locale) = Unit

    companion object {
        private val largeTransitClockAssets = TransitClockLottieAssets()
        private val smallTransitClockAssets = TransitClockLottieAssets()
        val COLOR_STROKE_KEYPATH_LIST = listOf("Color 1", "Color 2", "Color 3", "Color 4")
    }
}

private class LottieAnimator(
    private val updateCallback: (Float) -> Unit,
    private val startCallback: () -> Unit,
) {
    private val loopAnimator =
        ValueAnimator.ofFloat(1f).apply {
            addUpdateListener { animator -> updateCallback(animator.animatedValue as Float) }
            addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animator: android.animation.Animator) {
                        startCallback()
                    }
                }
            )
        }

    fun start() {
        if (loopAnimator.isRunning) {
            loopAnimator.cancel()
        }
        loopAnimator.duration = 800L
        loopAnimator.interpolator = Interpolators.EMPHASIZED
        loopAnimator.start()
    }
}
