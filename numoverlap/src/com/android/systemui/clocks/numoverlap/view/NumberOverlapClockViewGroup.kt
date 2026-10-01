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

package com.android.systemui.clocks.numoverlap.view

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.text.TextPaint
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.withSave
import com.android.app.animation.Interpolators
import com.android.systemui.animation.TextAnimator
import com.android.systemui.clocks.ComposedDigitalHandLayer
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.FontSpec
import com.android.systemui.clocks.view.CustomDigitalTextView
import com.android.systemui.clocks.view.DigitalFaceViewGroup
import com.android.systemui.customization.clocks.AnimationState
import com.android.systemui.customization.clocks.DigitTranslateAnimator
import com.android.systemui.customization.clocks.utils.CanvasUtils.translate
import com.android.systemui.customization.clocks.utils.CanvasUtils.withLayer
import com.android.systemui.customization.clocks.utils.ViewUtils.measuredSize
import com.android.systemui.customization.clocks.utils.ViewUtils.position
import com.android.systemui.customization.clocks.view.DigitalClockTextView
import com.android.systemui.customization.clocks.view.HorizontalAlignment
import com.android.systemui.customization.clocks.view.VerticalAlignment
import com.android.systemui.plugins.keyguard.VMeasurePoint
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.VPointF.Companion.minus
import com.android.systemui.plugins.keyguard.VRectF
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class NumberOverlapChildView(clockCtx: CustomClockContext, font: FontSpec) :
    CustomDigitalTextView(clockCtx, font) {
    var aodTranslate = VPointF.ZERO
    var outlineCutAnimator: TextAnimator? = null
    var innerCutAnimator: TextAnimator? = null
}

@SuppressLint("ViewConstructor")
open class NumberOverlapClockViewGroup(
    clockCtx: CustomClockContext,
    val dozeState: AnimationState,
) : DigitalFaceViewGroup<NumberOverlapChildView>(clockCtx) {
    enum class PaintState {
        AOD_INNER,
        AOD_OUTER,
        LOCKSCREEN,
    }

    protected open val overlappedRatio = VPointF(0.2f, 0.15f)

    protected var maxSingleDigitSize = VPointF(-1)
        private set

    private var innerCutStrokeWidth = 0f
    private var outlineCutStrokeWidth = 0f
    private val paintForInner =
        Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }

    protected lateinit var hourDigit1: NumberOverlapChildView
    protected lateinit var hourDigit2: NumberOverlapChildView
    protected lateinit var minuteDigit1: NumberOverlapChildView
    protected lateinit var minuteDigit2: NumberOverlapChildView

    init {
        setWillNotDraw(false)
        layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
    }

    override val children: Sequence<NumberOverlapChildView>
        get() = sequenceOf(hourDigit1, hourDigit2, minuteDigit1, minuteDigit2)

    val text: String
        get() = buildString { children.forEach { append(it.text) } }

    override fun onViewAdded(child: View?) {
        if (child == null || child !is NumberOverlapChildView) {
            logger.e({ "Unrecognized child view added: $str1" }) { str1 = "$child" }
            return
        }

        logger.onViewAdded(child)
        super.onViewAdded(child)
        child.digitTranslateAnimator = DigitTranslateAnimator {
            updateChildFrames(isLayout = false)
            updateMeasuredSize()
            invalidate()
        }
        when (child.id) {
            ClockViewIds.HOUR_FIRST_DIGIT -> hourDigit1 = child
            ClockViewIds.HOUR_SECOND_DIGIT -> hourDigit2 = child
            ClockViewIds.MINUTE_FIRST_DIGIT -> minuteDigit1 = child
            ClockViewIds.MINUTE_SECOND_DIGIT -> minuteDigit2 = child
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setClippingAnimator()
    }

    protected fun updateMaxSingleDigitSize() {
        val maxSize =
            children.fold(VPointF.ZERO) { maxSize, child ->
                VPointF.max(maxSize, child.maxSingleDigitSize)
            }
        if (maxSize == maxSingleDigitSize) return
        maxSingleDigitSize = maxSize
        updateCuttingAnimatorStrokeWidth()
    }

    override fun calculateSize(measureSpec: VMeasurePoint): VPointF {
        for (child in children) {
            if (child.layout != null) {
                child.outlineCutAnimator?.updateLayout(child.layout)
                child.innerCutAnimator?.updateLayout(child.layout)
            }
        }
        updateMaxSingleDigitSize()
        return maxSingleDigitSize * (2 - overlappedRatio) +
            AOD_TRANSLATE_RATIO * maxSingleDigitSize * 2
    }

    override fun onFontSettingChanged(fontSizePx: Float) {
        super.onFontSettingChanged(fontSizePx)
        val style = TextAnimator.Style(textSize = fontSizePx)
        for (child in children) {
            child.outlineCutAnimator?.setTextStyle(style)
            child.innerCutAnimator?.setTextStyle(style)
        }
    }

    private fun setClippingAnimator() {
        logger.i({ "setClippingAnimator(innerCut=${int1 / 1000f}, outlineCut=${int2 / 1000f})" }) {
            int1 = (innerCutStrokeWidth * 1000f).roundToInt()
            int2 = (outlineCutStrokeWidth * 1000f).roundToInt()
        }
        updateCuttingAnimator(outlineCutStrokeWidth) { child, createAnimator ->
            child.outlineCutAnimator ?: createAnimator().also { child.outlineCutAnimator = it }
        }
        updateCuttingAnimator(innerCutStrokeWidth) { child, createAnimator ->
            child.innerCutAnimator ?: createAnimator().also { child.innerCutAnimator = it }
        }
    }

    private fun updateCuttingAnimator(
        strokeWidth: Float,
        getAnimator: (NumberOverlapChildView, () -> TextAnimator) -> TextAnimator,
    ) {
        for (child in children) {
            if (child.layout == null) continue

            val animator =
                getAnimator(child) {
                    TextAnimator(child.layout, child.typefaceCache).apply {
                        textInterpolator.targetPaint.set(
                            TextPaint(child.lockscreenPaint).apply {
                                style = Paint.Style.FILL_AND_STROKE
                                strokeJoin = Paint.Join.ROUND
                                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                            }
                        )
                        textInterpolator.onTargetPaintModified()
                    }
                }
            animator.updateLayout(child.layout)
            animator.setTextStyle(
                TextAnimator.Style(
                    fVar = child.fontVariations.getStandard(dozeState.isActive),
                    strokeWidth = strokeWidth,
                )
            )
        }
    }

    private fun updateCuttingAnimatorStrokeWidth() {
        outlineCutStrokeWidth = maxSingleDigitSize.x * 0.1f
        val aodBorderWidth = children.maxOfOrNull { it.aodBorderWidth } ?: 0f
        innerCutStrokeWidth = outlineCutStrokeWidth + aodBorderWidth
        for (child in children) {
            child.outlineCutAnimator?.setTextStyle(
                TextAnimator.Style(strokeWidth = outlineCutStrokeWidth)
            )
            child.innerCutAnimator?.setTextStyle(
                TextAnimator.Style(strokeWidth = innerCutStrokeWidth)
            )
        }
        logger.i({
            "updateCuttingAnimatorStrokeWidth($int1, ${int2 / 1000f}) -> " +
                "(inner=${long1 / 1000f}, outline=${long2 / 1000f})"
        }) {
            int1 = maxSingleDigitSize.x.roundToInt()
            int2 = (aodBorderWidth * 1000f).roundToInt()
            long1 = (innerCutStrokeWidth * 1000f).roundToInt().toLong()
            long2 = (outlineCutStrokeWidth * 1000f).roundToInt().toLong()
        }
    }

    override fun refreshTime() {
        super.refreshTime()
        invalidate()
    }

    override fun getChildFrame(child: NumberOverlapChildView): VRectF {
        val aodTranslate = AOD_TRANSLATE_RATIO * maxSingleDigitSize
        val position =
            when (child) {
                hourDigit1 -> VPointF.ZERO
                hourDigit2 -> VPointF(maxSingleDigitSize.x * (1f - overlappedRatio.x), 0)
                minuteDigit1 -> VPointF(0, maxSingleDigitSize.y * (1f - overlappedRatio.y))
                minuteDigit2 -> maxSingleDigitSize * (1 - overlappedRatio)
                else -> VPointF.ZERO
            }
        child.aodTranslate =
            when (child) {
                hourDigit1 -> VPointF(0, -aodTranslate.y)
                hourDigit2 -> VPointF(aodTranslate.x, 0)
                minuteDigit1 -> VPointF(-aodTranslate.x, 0)
                minuteDigit2 -> VPointF(0, aodTranslate.y)
                else -> VPointF.ZERO
            }
        return VRectF.fromTopLeft(
            position +
                aodTranslate +
                (child.digitTranslateAnimator?.currentTranslation ?: VPointF.ZERO),
            child.measuredSize,
        )
    }

    override fun animateDoze(isDozing: Boolean, isAnimated: Boolean) {
        for (child in children) {
            child.animateDoze(isDozing, isAnimated)
            val style = TextAnimator.Style(fVar = child.fontVariations.getStandard(isDozing))
            val animation = TextAnimator.Animation(animate = isAnimated && isAnimationEnabled)
            child.outlineCutAnimator?.setTextStyle(style, animation)
            child.innerCutAnimator?.setTextStyle(style, animation)
        }

        dozeControlState.animateDoze = {
            for (child in children) {
                child.digitTranslateAnimator?.animatePosition(
                    animate = isAnimated && isAnimationEnabled,
                    duration = 750L,
                    interpolator = Interpolators.EMPHASIZED,
                    targetTranslation = if (isDozing) child.aodTranslate else VPointF.ZERO,
                )
            }
        }
    }

    override fun animateCharge() {
        children.forEach { child ->
            child.animateCharge()
            val chargeStyle =
                TextAnimator.Style(fVar = child.fontVariations.getCharge(dozeFraction < 0.5f))
            val returnStyle =
                TextAnimator.Style(fVar = child.fontVariations.getCharge(dozeFraction > 0.5f))
            val animation = TextAnimator.Animation(animate = isAnimationEnabled)
            child.outlineCutAnimator?.setTextStyle(
                chargeStyle,
                TextAnimator.Animation(
                    animate = isAnimationEnabled,
                    onAnimationEnd = {
                        child.outlineCutAnimator?.setTextStyle(returnStyle, animation)
                        child.innerCutAnimator?.setTextStyle(returnStyle, animation)
                    },
                ),
            )
            child.innerCutAnimator?.setTextStyle(chargeStyle, animation)
        }
    }

    private fun drawLockScreenClock(canvas: Canvas, bounds: VRectF) {
        canvas.withLayer(bounds) { drawOverlappedDigits(this, PaintState.LOCKSCREEN) }
    }

    private fun drawAodClock(canvas: Canvas, bounds: VRectF) {
        canvas.withLayer(bounds) {
            drawOverlappedDigits(this, PaintState.AOD_OUTER)
            withLayer(bounds, paintForInner) { drawOverlappedDigits(this, PaintState.AOD_INNER) }
        }
    }

    private fun drawSingleDigit(
        canvas: Canvas,
        child: NumberOverlapChildView,
        paintState: PaintState,
    ) {
        fun getChild(id: Int): NumberOverlapChildView? {
            return when (id) {
                ClockViewIds.HOUR_FIRST_DIGIT -> hourDigit1
                ClockViewIds.HOUR_SECOND_DIGIT -> hourDigit2
                ClockViewIds.MINUTE_FIRST_DIGIT -> minuteDigit1
                ClockViewIds.MINUTE_SECOND_DIGIT -> minuteDigit2
                else -> null
            }
        }

        canvas.withSave {
            translate(getAddedTranslateForDrawing(child))
            when (paintState) {
                PaintState.AOD_INNER -> child.innerAnimator.draw(this)
                PaintState.AOD_OUTER -> child.outlineAnimator.draw(this)
                PaintState.LOCKSCREEN -> child.textAnimator.draw(this)
            }
        }

        OVERLAPPED_MAP[child.id]?.mapNotNull { getChild(it) }
            ?.forEach { overlapped ->
                canvas.withSave {
                    translate(getAddedTranslateForDrawing(overlapped))
                    when (paintState) {
                        PaintState.AOD_INNER -> overlapped.innerCutAnimator?.draw(this)
                        PaintState.AOD_OUTER,
                        PaintState.LOCKSCREEN -> overlapped.outlineCutAnimator?.draw(this)
                    }
                }
            }
    }

    private fun getAddedTranslateForDrawing(child: NumberOverlapChildView): VPointF {
        return child.position + child.getDrawTranslation(child.getInterpolatedTextBounds())
    }

    private fun drawOverlappedDigits(canvas: Canvas, paintState: PaintState) {
        children.forEach { drawSingleDigit(canvas, it, paintState) }
    }

    override fun onDraw(canvas: Canvas) {
        logger.i({
            "onDraw($str1, inner=${int1 / 1000f}, outline=${int2 / 1000f}, doze=${long1 / 1000f})"
        }) {
            str1 = text
            int1 = (innerCutStrokeWidth * 1000f).roundToInt()
            int2 = (outlineCutStrokeWidth * 1000f).roundToInt()
            long1 = (dozeState.fraction * 1000f).roundToInt().toLong()
        }

        val bounds = VRectF.fromTopLeft(VPointF.ZERO, measuredSize)
        canvas.withSave {
            if (dozeState.fraction > 0.0) {
                drawAodClock(canvas, bounds)
            }
            if (dozeState.fraction < 1.0) {
                drawLockScreenClock(canvas, bounds)
            }
        }
    }

    companion object {
        private val OVERLAPPED_MAP =
            mapOf(
                ClockViewIds.HOUR_FIRST_DIGIT to
                    listOf(
                        ClockViewIds.HOUR_SECOND_DIGIT,
                        ClockViewIds.MINUTE_FIRST_DIGIT,
                        ClockViewIds.MINUTE_FIRST_DIGIT,
                    ),
                ClockViewIds.HOUR_SECOND_DIGIT to
                    listOf(ClockViewIds.MINUTE_SECOND_DIGIT, ClockViewIds.MINUTE_FIRST_DIGIT),
                ClockViewIds.MINUTE_FIRST_DIGIT to listOf(ClockViewIds.MINUTE_SECOND_DIGIT),
            )
        private val AOD_TRANSLATE_RATIO = VPointF(0.1f, 0.25f)
    }
}

@SuppressLint("ViewConstructor")
class NumberOverlapSmallClockViewGroup(
    clockCtx: CustomClockContext,
    layer: ComposedDigitalHandLayer,
    dozeState: AnimationState,
) : NumberOverlapClockViewGroup(clockCtx, dozeState) {
    override val overlappedRatio = VPointF(0.25f, 0f)

    private val colon =
        NumberOverlapChildView(clockCtx, layer.font!!)
            .apply {
                text = ":"
                horizontalAlignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
            }
            .also { addView(it) }

    override val children: Sequence<NumberOverlapChildView>
        get() = sequenceOf(hourDigit1, hourDigit2, colon, minuteDigit1, minuteDigit2)

    override fun onViewAdded(child: View?) {
        if (child == null) return
        logger.onViewAdded(child)
        super.onViewAdded(child)
        if (child is DigitalClockTextView) {
            child.digitTranslateAnimator = null
        }
        if (childCount < 5 || child !is CustomDigitalTextView) return
        colon.applyStyles(hourDigit2.textStyle, hourDigit2.aodStyle)
    }

    override fun calculateSize(measureSpec: VMeasurePoint): VPointF {
        updateMaxSingleDigitSize()
        val overlapScale = 1f - overlappedRatio.x
        val width =
            hourDigit1.measuredWidth * overlapScale +
                hourDigit2.measuredWidth +
                minuteDigit1.measuredWidth * overlapScale +
                minuteDigit2.measuredWidth +
                colon.measuredWidth
        val height =
            if (measureSpec.height.size != 0) {
                measureSpec.height.size.toFloat()
            } else {
                maxSingleDigitSize.y
            }
        return VPointF(width, height)
    }

    override fun getChildFrame(child: NumberOverlapChildView): VRectF {
        val leftChild =
            when (child) {
                hourDigit1 -> null
                hourDigit2 -> if (hasLeadingDigit) hourDigit1 else null
                colon -> hourDigit2
                minuteDigit1 -> colon
                minuteDigit2 -> minuteDigit1
                else -> null
            }
        val left =
            leftChild?.let {
                val ratio = if (child == colon || it == colon) VPointF.ZERO else overlappedRatio
                it.right - ratio.x * it.measuredWidth
            } ?: 0f
        val position = VPointF(left, (measuredHeight - child.measuredSize.y) / 2f)
        return VRectF.fromTopLeft(
            position + (child.digitTranslateAnimator?.currentTranslation ?: VPointF.ZERO),
            child.measuredSize,
        )
    }
}
