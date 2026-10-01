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

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.text.TextPaint
import android.view.View
import com.android.app.animation.Interpolators
import com.android.systemui.animation.TextAnimator
import com.android.systemui.clocks.ComposedDigitalHandLayer
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.FontSpec
import com.android.systemui.clocks.view.CustomDigitalTextView
import com.android.systemui.clocks.view.DigitalFaceViewGroup
import com.android.systemui.customization.clocks.AnimationState
import com.android.systemui.customization.clocks.utils.CanvasUtils.withLayer
import com.android.systemui.customization.clocks.utils.ViewUtils.measuredSize
import com.android.systemui.customization.clocks.utils.ViewUtils.position
import com.android.systemui.customization.clocks.view.HorizontalAlignment
import com.android.systemui.customization.clocks.view.VerticalAlignment
import com.android.systemui.plugins.keyguard.VMeasurePoint
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.VRectF
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds

open class NumberOverlapChildView(clockCtx: CustomClockContext, fontSpec: FontSpec) :
    CustomDigitalTextView(clockCtx, fontSpec) {
    var aodTranslate = VPointF.ZERO
    var innerCutAnimator: TextAnimator? = null
    var outlineCutAnimator: TextAnimator? = null

    val interpolatedTextBounds: VRectF
        get() = getInterpolatedTextBounds()

    val aodBorderWidthCompat: Float
        get() = aodBorderWidth
}

open class NumberOverlapClockViewGroup(
    clockCtx: CustomClockContext,
    val dozeState: AnimationState,
) : DigitalFaceViewGroup(clockCtx) {
    protected var hourDigit1: NumberOverlapChildView? = null
    protected var hourDigit2: NumberOverlapChildView? = null
    protected var minuteDigit1: NumberOverlapChildView? = null
    protected var minuteDigit2: NumberOverlapChildView? = null

    protected var maxSingleDigitSize = VPointF(-1f)
    private var innerCutStrokeWidth = 0f
    private var outlineCutStrokeWidth = 0f
    private val paintForInner =
        Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }

    protected open val overlappedRatio: VPointF = VPointF(0.2f, 0.15f)

    override val children: Sequence<NumberOverlapChildView>
        get() = listOfNotNull(hourDigit1, hourDigit2, minuteDigit1, minuteDigit2).asSequence()

    override fun calculateSize(measureSpec: VMeasurePoint): VPointF {
        children.forEach { child ->
            child.layout?.let { layout ->
                child.outlineCutAnimator?.updateLayout(layout)
                child.innerCutAnimator?.updateLayout(layout)
            }
        }
        updateMaxSingleDigitSize()
        return maxSingleDigitSize * (2f - overlappedRatio) +
            AOD_TRANSLATE_RATIO * maxSingleDigitSize * 2f
    }

    override fun getChildFrame(child: NumberOverlapChildView): VRectF {
        val aodOffset = AOD_TRANSLATE_RATIO * maxSingleDigitSize
        val topLeft =
            when (child) {
                hourDigit1 -> VPointF(0f, -aodOffset.y)
                hourDigit2 ->
                    VPointF(maxSingleDigitSize.x * (1f - overlappedRatio.x) + aodOffset.x, 0f)
                minuteDigit1 ->
                    VPointF(-aodOffset.x, maxSingleDigitSize.y * (1f - overlappedRatio.y))
                minuteDigit2 ->
                    maxSingleDigitSize * (1f - overlappedRatio) + VPointF(0f, aodOffset.y)
                else -> VPointF.ZERO
            }
        child.aodTranslate =
            when (child) {
                hourDigit1 -> VPointF(0f, -aodOffset.y)
                hourDigit2 -> VPointF(aodOffset.x, 0f)
                minuteDigit1 -> VPointF(-aodOffset.x, 0f)
                minuteDigit2 -> VPointF(0f, aodOffset.y)
                else -> VPointF.ZERO
            }
        val currentTranslation = child.digitTranslateAnimator?.currentTranslation ?: VPointF.ZERO
        return VRectF.fromTopLeft(topLeft + aodOffset + currentTranslation, child.measuredSize)
    }

    override fun refreshTime() {
        children.forEach { it.refreshText() }
        invalidate()
    }

    override fun onViewAdded(child: View?) {
        val digitChild = child as? NumberOverlapChildView
        if (digitChild == null) {
            if (child != null) {
                logger.e({ "Unrecognized child view added: $str1" }) { str1 = "$child" }
            }
            return
        }
        super.onViewAdded(child)
        when (digitChild.id) {
            ClockViewIds.HOUR_FIRST_DIGIT -> hourDigit1 = digitChild
            ClockViewIds.HOUR_SECOND_DIGIT -> hourDigit2 = digitChild
            ClockViewIds.MINUTE_FIRST_DIGIT -> minuteDigit1 = digitChild
            ClockViewIds.MINUTE_SECOND_DIGIT -> minuteDigit2 = digitChild
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setClippingAnimators()
    }

    override fun onDraw(canvas: Canvas) {
        val bounds = VRectF.fromTopLeft(VPointF.ZERO, measuredSize)
        canvas.save()
        try {
            if (dozeState.fraction > 0f) {
                drawAodClock(canvas, bounds)
            }
            if (dozeState.fraction < 1f) {
                drawLockScreenClock(canvas, bounds)
            }
        } finally {
            canvas.restore()
        }
    }

    private fun drawAodClock(canvas: Canvas, bounds: VRectF) {
        canvas.withLayer(bounds) { drawOverlappedDigits(this, PaintState.AOD_OUTER) }
        canvas.withLayer(bounds, paintForInner) { drawOverlappedDigits(this, PaintState.AOD_INNER) }
    }

    private fun drawLockScreenClock(canvas: Canvas, bounds: VRectF) {
        canvas.withLayer(bounds) { drawOverlappedDigits(this, PaintState.LOCKSCREEN) }
    }

    private fun drawOverlappedDigits(canvas: Canvas, paintState: PaintState) {
        children.forEach { drawSingleDigit(canvas, it, paintState) }
    }

    private fun drawSingleDigit(
        canvas: Canvas,
        digit: NumberOverlapChildView,
        paintState: PaintState,
    ) {
        canvas.save()
        try {
            canvas.translate(
                digit.position + digit.getDrawTranslation(digit.interpolatedTextBounds)
            )
            when (paintState) {
                PaintState.AOD_INNER -> digit.innerAnimator?.draw(canvas)
                PaintState.AOD_OUTER -> digit.outlineAnimator?.draw(canvas)
                PaintState.LOCKSCREEN -> digit.textAnimator.draw(canvas)
            }
        } finally {
            canvas.restore()
        }
        overlappedIds(digit.id).forEach { otherId ->
            val other = children.firstOrNull { it.id == otherId } ?: return@forEach
            canvas.save()
            try {
                canvas.translate(
                    other.position + other.getDrawTranslation(other.interpolatedTextBounds)
                )
                val cutAnimator =
                    when (paintState) {
                        PaintState.AOD_INNER -> other.innerCutAnimator
                        else -> other.outlineCutAnimator
                    }
                cutAnimator?.draw(canvas)
            } finally {
                canvas.restore()
            }
        }
    }

    override fun animateDoze(isDozing: Boolean, isAnimated: Boolean) {
        children.forEach { child ->
            child.animateDoze(isDozing, isAnimated)
            val style = TextAnimator.Style(fVar = child.fontVariations.getStandard(isDozing))
            val animation =
                TextAnimator.Animation(animate = isAnimated && isAnimationEnabled, duration = 0L)
            child.outlineCutAnimator?.setTextStyle(style, animation)
            child.innerCutAnimator?.setTextStyle(style, animation)
        }
        dozeControlState.setAnimateDoze {
            children.forEach { child ->
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
                TextAnimator.Style(fVar = child.fontVariations.getCharge(dozeState.fraction < 0.5f))
            val returnStyle =
                TextAnimator.Style(fVar = child.fontVariations.getCharge(dozeState.fraction > 0.5f))
            val animation = TextAnimator.Animation(animate = isAnimationEnabled, duration = 0L)
            child.outlineCutAnimator?.setTextStyle(
                chargeStyle,
                animation.copy(
                    onAnimationEnd = {
                        child.outlineCutAnimator?.setTextStyle(returnStyle, animation)
                        child.innerCutAnimator?.setTextStyle(returnStyle, animation)
                    }
                ),
            )
            child.innerCutAnimator?.setTextStyle(chargeStyle, animation)
        }
    }

    override fun onFontSettingChanged(fontSizePx: Float) {
        super.onFontSettingChanged(fontSizePx)
        val style = TextAnimator.Style(textSize = fontSizePx)
        children.forEach { child ->
            child.outlineCutAnimator?.setTextStyle(style)
            child.innerCutAnimator?.setTextStyle(style)
        }
    }

    private fun updateMaxSingleDigitSize() {
        var size = VPointF.ZERO
        children.forEach { size = VPointF.max(size, it.maxSingleDigitSize) }
        if (size == maxSingleDigitSize) return
        maxSingleDigitSize = size
        updateCuttingAnimatorStrokeWidth()
    }

    protected fun updateCuttingAnimatorStrokeWidth() {
        outlineCutStrokeWidth = maxSingleDigitSize.x * 0.1f
        val maxAodBorder = children.maxOfOrNull { it.aodBorderWidthCompat } ?: 0f
        innerCutStrokeWidth = outlineCutStrokeWidth + maxAodBorder
        children.forEach { child ->
            child.outlineCutAnimator?.setTextStyle(
                TextAnimator.Style(strokeWidth = outlineCutStrokeWidth)
            )
            child.innerCutAnimator?.setTextStyle(
                TextAnimator.Style(strokeWidth = innerCutStrokeWidth)
            )
        }
    }

    private fun setClippingAnimators() {
        updateCuttingAnimator(outlineCutStrokeWidth) { child, create ->
            child.outlineCutAnimator ?: create().also { child.outlineCutAnimator = it }
        }
        updateCuttingAnimator(innerCutStrokeWidth) { child, create ->
            child.innerCutAnimator ?: create().also { child.innerCutAnimator = it }
        }
    }

    private fun updateCuttingAnimator(
        strokeWidth: Float,
        getAnimator: (NumberOverlapChildView, () -> TextAnimator) -> TextAnimator?,
    ) {
        children.forEach { child ->
            child.layout?.let { layout ->
                val animator =
                    getAnimator(child) {
                        val textAnimator = TextAnimator(layout, child.typefaceCache)
                        val targetPaint = textAnimator.textInterpolator.targetPaint
                        val cutPaint = TextPaint(child.lockscreenPaint)
                        cutPaint.style = Paint.Style.FILL_AND_STROKE
                        cutPaint.strokeJoin = Paint.Join.ROUND
                        cutPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                        targetPaint.set(cutPaint)
                        textAnimator.textInterpolator.onTargetPaintModified()
                        textAnimator
                    } ?: return@forEach
                animator.updateLayout(layout)
                animator.setTextStyle(
                    TextAnimator.Style(
                        fVar = child.fontVariations.getStandard(dozeState.isActive),
                        strokeWidth = strokeWidth,
                    )
                )
            }
        }
    }

    private fun overlappedIds(viewId: Int): List<Int> {
        return when (viewId) {
            ClockViewIds.HOUR_FIRST_DIGIT ->
                listOf(ClockViewIds.HOUR_SECOND_DIGIT, ClockViewIds.MINUTE_FIRST_DIGIT)
            ClockViewIds.HOUR_SECOND_DIGIT ->
                listOf(ClockViewIds.MINUTE_SECOND_DIGIT, ClockViewIds.MINUTE_FIRST_DIGIT)
            ClockViewIds.MINUTE_FIRST_DIGIT -> listOf(ClockViewIds.MINUTE_SECOND_DIGIT)
            else -> emptyList()
        }
    }

    protected enum class PaintState {
        AOD_INNER,
        AOD_OUTER,
        LOCKSCREEN,
    }

    protected companion object {
        val AOD_TRANSLATE_RATIO = VPointF(0.1f, 0.25f)
    }
}

open class NumberOverlapSmallClockViewGroup(
    clockCtx: CustomClockContext,
    layer: ComposedDigitalHandLayer,
    dozeState: AnimationState,
) : NumberOverlapClockViewGroup(clockCtx, dozeState) {
    private val colon: NumberOverlapChildView =
        NumberOverlapChildView(clockCtx, requireNotNull(layer.font)).apply {
            text = ":"
            clockVerticalAlignment = VerticalAlignment.CENTER
            clockHorizontalAlignment = HorizontalAlignment.CENTER
        }

    init {
        addView(colon)
    }

    override val children: Sequence<NumberOverlapChildView>
        get() = super.children + sequenceOf(colon)

    override val overlappedRatio: VPointF = VPointF(0.25f, 0f)

    override fun calculateSize(measureSpec: VMeasurePoint): VPointF {
        var size = VPointF.ZERO
        children.forEach { size = VPointF.max(size, it.maxSingleDigitSize) }
        if (size != maxSingleDigitSize) {
            maxSingleDigitSize = size
            updateCuttingAnimatorStrokeWidth()
        }
        val overlapFactor = 1f - overlappedRatio.x
        val width =
            (hourDigit1?.measuredWidth ?: 0) * overlapFactor +
                (hourDigit2?.measuredWidth ?: 0) +
                (minuteDigit1?.measuredWidth ?: 0) * overlapFactor +
                (minuteDigit2?.measuredWidth ?: 0) +
                colon.measuredWidth
        val height = if (measureSpec.height.size != 0) measureSpec.height.size.toFloat() else size.y
        return VPointF(width, height)
    }

    override fun getChildFrame(child: NumberOverlapChildView): VRectF {
        val previous =
            when (child) {
                hourDigit2 -> if (hasLeadingDigit) hourDigit1 else null
                colon -> hourDigit2
                minuteDigit1 -> colon
                minuteDigit2 -> minuteDigit1
                else -> null
            }
        val isColon = child == colon || previous == colon
        val overlap = if (isColon) 0f else overlappedRatio.x
        val x =
            previous?.right?.toFloat()?.let { right -> right - overlap * previous.measuredWidth }
                ?: 0f
        val currentTranslation = child.digitTranslateAnimator?.currentTranslation ?: VPointF.ZERO
        return VRectF.fromTopLeft(
            VPointF(x, (measuredHeight - child.measuredHeight) / 2f) + currentTranslation,
            child.measuredSize,
        )
    }

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        if (childCount >= 5 && child is CustomDigitalTextView) {
            colon.applyStyles(child.textStyle, child.aodStyle)
        }
    }
}
