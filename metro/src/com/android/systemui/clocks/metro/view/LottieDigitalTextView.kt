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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.graphics.withSave
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
import com.android.systemui.customization.clocks.ClockLogger
import com.android.systemui.customization.clocks.TextStyle
import com.android.systemui.customization.clocks.utils.ViewUtils.measuredSize
import com.android.systemui.customization.clocks.view.HorizontalAlignment
import com.android.systemui.customization.clocks.view.VerticalAlignment
import com.android.systemui.plugins.keyguard.VMeasurePoint
import com.android.systemui.plugins.keyguard.VMeasureSpec
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.VRectF
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig
import java.io.FileNotFoundException
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

fun readLottieComposition(assets: AssetLoader, path: String): LottieComposition? {
    return try {
        LottieCompositionFactory.fromJsonStringSync(assets.readTextAsset(path), path).value
    } catch (e: FileNotFoundException) {
        assets.logger.w({ "Failed to find file at $str1" }) { str1 = path }
        null
    }
}

class LottieAnimator(val updateCallback: (Float) -> Unit, val startCallback: () -> Unit) {
    private val loopAnimator =
        ValueAnimator.ofFloat(1f).apply {
            addUpdateListener { updateCallback(it.animatedValue as Float) }
            addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
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

@SuppressLint("ViewConstructor")
class LottieDigitalTextView(
    private val clockCtx: CustomClockContext,
    private val isLargeClock: Boolean,
) : View(clockCtx.context), ICustomDigitalTextView {
    private val logger = ClockLogger(this, clockCtx.messageBuffer, TAG)

    private var numbers: List<Drawable> = emptyList()
    private var colon: LottieDrawable? = null
    private var lightColorMap: Map<String, String> = emptyMap()
    private var darkColorMap: Map<String, String> = emptyMap()
    private val parser = DimensionParser(clockCtx.context)
    lateinit var textStyle: LottieTextStyle

    override var verticalAlignment = VerticalAlignment.CENTER
    override var horizontalAlignment = HorizontalAlignment.CENTER

    private var letterSpacing = 0f
    private var paddingVertical = 0f
    private var paddingHorizontal = 0f
    private var lastUnconstrainedFontSizePx = Float.MAX_VALUE
    private var innerWidth = 0f
    private var innerHeight = 0f

    override var onViewBoundsChanged: ((VRectF) -> Unit)? = null
    override var onViewMaxSizeChanged: ((VPointF) -> Unit)? = null

    override var text: String = ""
        set(value) {
            field = value
            requestLayout()
        }

    override var dozeFraction: Float = 0f
        set(value) {
            field = value
            refreshAlphaByText(value)
            invalidate()
        }

    override var maxSize = VPointF(-1)
        private set

    private var animationState = 0
    private var lastProgress = 0f

    private val lottieAnimator =
        LottieAnimator(
            { fraction ->
                val end = if (startProgress > endProgress) endProgress + 1f else endProgress
                val progress = (end - startProgress) * fraction + startProgress
                syncLottieProgress(progress - floor(progress))
                invalidate()
            },
            {
                startProgress = lastProgress
                updateAnimationState()
                logger.d({ "onAnimationStart startProgress $double1" }) {
                    double1 = startProgress.toDouble()
                }
            },
        )

    init {
        setWillNotDraw(false)
        setLayerType(
            LAYER_TYPE_SOFTWARE,
            Paint().apply {
                isDither = true
                isAntiAlias = true
                isFilterBitmap = true
            },
        )
    }

    private var startProgress = -1f
        get() = if (field == -1f) animationState * 0.25f else field

    private val endProgress: Float
        get() = (animationState + 1) * 0.25f

    override fun refreshText() {
        refreshAlphaByText(dozeFraction)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measureSpec = VMeasurePoint.fromSpecs(widthMeasureSpec, heightMeasureSpec)
        logger.onMeasure(measureSpec)

        innerWidth = 0f
        innerHeight = 0f
        for (char in text) {
            val bounds = getDrawable(char, numbers, colon)?.bounds ?: continue
            if (innerWidth > 0f) {
                innerWidth += letterSpacing
            }
            innerHeight = max(innerHeight, bounds.height().toFloat())
            innerWidth += bounds.width()
        }
        innerWidth += paddingHorizontal * 2f
        innerHeight += paddingVertical * 2f

        val width =
            when (measureSpec.width.mode) {
                VMeasureSpec.Mode.EXACTLY -> measureSpec.width.size
                VMeasureSpec.Mode.AT_MOST -> min(measureSpec.width.size, innerWidth.toInt())
                else -> innerWidth.toInt()
            }
        val height =
            when (measureSpec.height.mode) {
                VMeasureSpec.Mode.EXACTLY -> measureSpec.height.size
                VMeasureSpec.Mode.AT_MOST -> min(measureSpec.height.size, innerHeight.toInt())
                else -> innerHeight.toInt()
            }
        setMeasuredDimension(width, height)
        maxSize = measuredSize
    }

    override fun updateTheme(theme: ThemeConfig) {
        val colorMap = if (theme.isDarkTheme) lightColorMap else darkColorMap
        val colonColor = clockCtx.assets.readColor("@android:color/system_accent1_100")
        colorMap.forEach { (keyPath, colorRes) ->
            val color = clockCtx.assets.readColor(colorRes)
            numbers.forEachIndexed { _, drawable ->
                (drawable as? LottieDrawable)?.addValueCallback(
                    KeyPath("**", keyPath),
                    LottieProperty.STROKE_COLOR,
                    SimpleLottieValueCallback { color },
                )
                (drawable as? LottieDrawable)?.addValueCallback(
                    KeyPath("**", "#FFFFFF"),
                    LottieProperty.STROKE_COLOR,
                    SimpleLottieValueCallback { colonColor },
                )
                (drawable as? LottieDrawable)?.addValueCallback(
                    KeyPath("**", "#FFFFFF"),
                    LottieProperty.COLOR,
                    SimpleLottieValueCallback { colonColor },
                )
            }
        }
        invalidate()
    }

    override fun applyStyles(textStyle: TextStyle, aodStyle: TextStyle?) {
        logger.d("applyTextStyle")
        val style = textStyle as LottieTextStyle
        this.textStyle = style

        val assets = if (isLargeClock) largeTransitClockAssets else smallTransitClockAssets
        if (numbers.isEmpty()) {
            numbers = assets.getNumbersList(clockCtx.assets, style.numbers)
        }
        colon = style.colon?.let { assets.getColon(clockCtx.assets, it) }
        style.fillColorLightMap?.let { lightColorMap = it }
        style.fillColorDarkMap?.let { darkColorMap = it }
    }

    override fun applyTextSize(targetFontSizePx: Float?, constrainedByHeight: Boolean) {
        val fontSizePx = adjustTextSize(targetFontSizePx, constrainedByHeight)
        logger.d("applyTextSize")

        val scale = fontSizePx * textStyle.fontSizeScale / numbers[0].intrinsicHeight
        letterSpacing = parser.convert(textStyle.spacing) * scale
        paddingVertical = parser.convert(textStyle.paddingVertical) * scale
        paddingHorizontal = parser.convert(textStyle.paddingHorizontal) * scale
        for (drawable in numbers + colon) {
            if (drawable == null) return
            drawable.setBounds(
                0,
                0,
                (drawable.intrinsicWidth * scale - paddingHorizontal * 2f).toInt(),
                (drawable.intrinsicHeight * scale - paddingVertical * 2f).toInt(),
            )
        }
        refreshText()
    }

    override fun animateDoze(isDozing: Boolean, isAnimated: Boolean) {
        if (isAnimated) {
            lottieAnimator.start()
        }
    }

    override fun animateCharge() {
        lottieAnimator.start()
    }

    override fun setVisibility(visibility: Int) {
        logger.setVisibility(visibility)
        super.setVisibility(visibility)
    }

    override fun setAlpha(alpha: Float) {
        logger.setAlpha(alpha)
        super.setAlpha(alpha)
    }

    override fun invalidate() {
        logger.invalidate()
        super.invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        logger.onDraw(text)
        drawNumbers(canvas) { getDrawable(it, numbers, colon) }
    }

    fun adjustTextSize(targetFontSizePx: Float?, constrainedByHeight: Boolean): Float {
        if (constrainedByHeight) {
            return min(targetFontSizePx ?: 0f, lastUnconstrainedFontSizePx)
        }
        val fontSizePx = targetFontSizePx ?: 0f
        lastUnconstrainedFontSizePx = fontSizePx
        return fontSizePx
    }

    private fun refreshAlphaByText(dozeFraction: Float) {
        for (char in text) {
            val drawable = getDrawable(char, numbers, colon)
            COLOR_STROKE_KEYPATH_LIST.forEach { keyPath ->
                drawable?.addValueCallback(
                    KeyPath("**", keyPath),
                    LottieProperty.OPACITY,
                    SimpleLottieValueCallback { ((1f - dozeFraction) * 100f).toInt() },
                )
            }
            drawable?.addValueCallback(
                KeyPath("**", "#FFFFFF"),
                LottieProperty.OPACITY,
                SimpleLottieValueCallback { (dozeFraction * 100f).toInt() },
            )
        }
    }

    private fun drawNumbers(canvas: Canvas, getDrawable: (Char) -> Drawable?) {
        canvas.withSave {
            translate(paddingLeft.toFloat(), paddingTop.toFloat())
            translate(localTranslationX, localTranslationY)

            var missing: StringBuilder? = null
            for (char in text) {
                val drawable = getDrawable(char)
                if (drawable != null) {
                    drawable.draw(this)
                    translate(drawable.bounds.width() + letterSpacing, 0f)
                } else {
                    if (missing == null) {
                        missing = StringBuilder()
                    }
                    missing.append(char)
                }
            }

            if (missing != null) {
                logger.e({ "Didn't find drawables for '$str1'" }) { str1 = missing.toString() }
            }
        }
    }

    private val localTranslationY: Float
        get() =
            when (verticalAlignment) {
                VerticalAlignment.CENTER -> (measuredHeight - innerHeight) / 2f
                VerticalAlignment.BOTTOM -> measuredHeight - innerHeight
                else -> 0f
            }

    private val localTranslationX: Float
        get() =
            when (horizontalAlignment) {
                HorizontalAlignment.CENTER -> (measuredWidth - innerWidth) / 2f
                HorizontalAlignment.RIGHT -> measuredWidth - innerWidth
                else -> 0f
            }

    private fun getDrawable(
        char: Char,
        numbers: List<Drawable>,
        colon: LottieDrawable?,
    ): LottieDrawable? {
        if (char == ':') {
            return colon
        }
        return char.digitToIntOrNull()?.let { numbers[it] as LottieDrawable }
    }

    private fun syncLottieProgress(progress: Float) {
        lastProgress = progress
        for (char in text.toSet()) {
            getDrawable(char, numbers, colon)?.progress = progress
        }
        logger.d({ "syncLottieProgress progress $double1" }) { double1 = progress.toDouble() }
    }

    private fun updateAnimationState() {
        startProgress -= floor(startProgress)
        animationState = round(startProgress / 0.25f).toInt() % 4
        logger.d({ "updateAnimationState $int1" }) { int1 = animationState }
    }

    companion object {
        private val TAG = LottieDigitalTextView::class.simpleName!!
        private val largeTransitClockAssets = TransitClockLottieAssets()
        private val smallTransitClockAssets = TransitClockLottieAssets()
        private val COLOR_STROKE_KEYPATH_LIST = listOf("Color 1", "Color 2", "Color 3", "Color 4")
    }
}
