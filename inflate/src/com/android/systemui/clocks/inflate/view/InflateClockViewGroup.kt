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

package com.android.systemui.clocks.inflate.view

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import com.android.systemui.clocks.ComposedDigitalHandLayer
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.FontSpec
import com.android.systemui.clocks.view.CustomDigitalTextView
import com.android.systemui.clocks.view.DigitalFaceViewGroup
import com.android.systemui.customization.clocks.DigitTranslateAnimator
import com.android.systemui.customization.clocks.utils.ViewUtils.measuredSize
import com.android.systemui.customization.clocks.view.HorizontalAlignment
import com.android.systemui.customization.clocks.view.VerticalAlignment
import com.android.systemui.plugins.keyguard.VMeasurePoint
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.VPointF.Companion.minus
import com.android.systemui.plugins.keyguard.VRectF
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds

@SuppressLint("ViewConstructor")
class InflateChildView(clockCtx: CustomClockContext, font: FontSpec) :
    CustomDigitalTextView(clockCtx, font)

@SuppressLint("ViewConstructor")
open class InflateClockViewGroup(clockCtx: CustomClockContext) :
    DigitalFaceViewGroup<InflateChildView>(clockCtx) {
    protected var maxSingleDigitSize = VPointF(-1)
    private var lockscreenTranslate = VPointF.ZERO
    private var aodTranslate = VPointF.ZERO
    private var bounceTranslate = VPointF.ZERO

    protected lateinit var hourDigit1: InflateChildView
    protected lateinit var hourDigit2: InflateChildView
    protected lateinit var minuteDigit1: InflateChildView
    protected lateinit var minuteDigit2: InflateChildView

    init {
        setWillNotDraw(false)
        layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
    }

    override val children: Sequence<InflateChildView>
        get() = sequenceOf(hourDigit1, hourDigit2, minuteDigit1, minuteDigit2)

    override fun onViewAdded(child: View?) {
        if (child == null || child !is InflateChildView) {
            logger.e({ "Unrecognized child view added: $str1" }) { str1 = "$child" }
            return
        }

        logger.onViewAdded(child)
        super.onViewAdded(child)
        child.digitTranslateAnimator = DigitTranslateAnimator { invalidate() }
        when (child.id) {
            ClockViewIds.HOUR_FIRST_DIGIT -> hourDigit1 = child
            ClockViewIds.HOUR_SECOND_DIGIT -> hourDigit2 = child
            ClockViewIds.MINUTE_FIRST_DIGIT -> minuteDigit1 = child
            ClockViewIds.MINUTE_SECOND_DIGIT -> minuteDigit2 = child
        }
    }

    override fun calculateSize(measureSpec: VMeasurePoint): VPointF {
        maxSingleDigitSize = hourDigit1.maxSingleDigitSize
        aodTranslate = maxSingleDigitSize * OVERLAPPED_RATIO * AOD_TRANSLATE_RATIO
        bounceTranslate = maxSingleDigitSize * OVERLAPPED_RATIO * BOUNCE_TRANSLATE_RATIO
        return maxSingleDigitSize * (2 - OVERLAPPED_RATIO)
    }

    override fun getChildFrame(child: InflateChildView): VRectF {
        val offset = (1 - OVERLAPPED_RATIO) * maxSingleDigitSize
        val position =
            when (child) {
                hourDigit1 -> VPointF.ZERO
                hourDigit2 -> VPointF(offset.x, 0f)
                minuteDigit1 -> VPointF(0f, offset.y)
                minuteDigit2 -> offset
                else -> VPointF.ZERO
            }
        return VRectF.fromTopLeft(position, child.measuredSize)
    }

    override fun refreshTime() {
        super.refreshTime()
        children.forEach { it.refreshText() }
    }

    override fun animateDoze(isDozing: Boolean, isAnimated: Boolean) {
        dozeControlState.animateDoze = {
            super.animateDoze(isDozing, isAnimated)
            if (maxSingleDigitSize.x < 0f || maxSingleDigitSize.y < 0f) {
                measure(0, 0)
            }

            children.forEach { child ->
                child.digitTranslateAnimator?.let { animator ->
                    if (!isDozing) {
                        val phase3BounceAnim = Runnable {
                            animator.animatePosition(
                                animate = isAnimated && isAnimationEnabled,
                                duration = INFLATE_CLOCK_PHASE3_DURATION[id] ?: 0L,
                                interpolator = INFLATE_CLOCK_PHASE3_INTERPOLATOR,
                                targetTranslation =
                                    updateDirectionalTargetTranslate(id, lockscreenTranslate),
                            )
                        }
                        val phase2BounceAnim = Runnable {
                            animator.animatePosition(
                                animate = isAnimated && isAnimationEnabled,
                                duration = INFLATE_CLOCK_PHASE2_DURATION[id] ?: 0L,
                                interpolator = INFLATE_CLOCK_PHASE2_INTERPOLATOR,
                                targetTranslation =
                                    updateDirectionalTargetTranslate(id, bounceTranslate),
                                onAnimationEnd = phase3BounceAnim,
                            )
                        }
                        animator.animatePosition(
                            animate = isAnimated && isAnimationEnabled,
                            duration = INFLATE_CLOCK_PHASE1_DURATION[id] ?: 0L,
                            interpolator = INFLATE_CLOCK_PHASE1_INTERPOLATOR,
                            targetTranslation =
                                updateDirectionalTargetTranslate(id, lockscreenTranslate),
                            onAnimationEnd = phase2BounceAnim,
                        )
                    } else {
                        animator.animatePosition(
                            animate = isAnimated && isAnimationEnabled,
                            duration = 800L,
                            interpolator = INFLATE_CLOCK_DOZING_INTERPOLATOR,
                            targetTranslation = updateDirectionalTargetTranslate(id, aodTranslate),
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val OVERLAPPED_RATIO = VPointF(0.32f, 0.15f)
        private val INFLATE_CLOCK_PHASE1_INTERPOLATOR = PathInterpolator(0.33f, 0f, 0.6f, 1f)
        val INFLATE_CLOCK_PHASE2_INTERPOLATOR = PathInterpolator(0.22f, 0f, 0.54f, 1f)
        val INFLATE_CLOCK_PHASE3_INTERPOLATOR = PathInterpolator(0.29f, 0f, 0.58f, 1f)

        private val INFLATE_CLOCK_PHASE1_DURATION =
            mapOf(
                ClockViewIds.HOUR_FIRST_DIGIT to 400L,
                ClockViewIds.HOUR_SECOND_DIGIT to 350L,
                ClockViewIds.MINUTE_FIRST_DIGIT to 367L,
                ClockViewIds.MINUTE_SECOND_DIGIT to 450L,
            )
        val INFLATE_CLOCK_PHASE2_DURATION =
            mapOf(
                ClockViewIds.HOUR_FIRST_DIGIT to 783L,
                ClockViewIds.HOUR_SECOND_DIGIT to 733L,
                ClockViewIds.MINUTE_FIRST_DIGIT to 783L,
                ClockViewIds.MINUTE_SECOND_DIGIT to 667L,
            )
        val INFLATE_CLOCK_PHASE3_DURATION =
            mapOf(
                ClockViewIds.HOUR_FIRST_DIGIT to 817L,
                ClockViewIds.HOUR_SECOND_DIGIT to 1050L,
                ClockViewIds.MINUTE_FIRST_DIGIT to 917L,
                ClockViewIds.MINUTE_SECOND_DIGIT to 1083L,
            )

        private val INFLATE_CLOCK_DOZING_INTERPOLATOR = PathInterpolator(0.3f, 0f, 0.1f, 1f)
        private val AOD_TRANSLATE_RATIO = VPointF(0.15f, 0.35f)
        private val BOUNCE_TRANSLATE_RATIO = VPointF(0.1f)

        fun updateDirectionalTargetTranslate(id: Int, targetTranslation: VPointF): VPointF {
            return when (id) {
                ClockViewIds.HOUR_FIRST_DIGIT -> targetTranslation * VPointF(-1, -1)
                ClockViewIds.HOUR_SECOND_DIGIT -> targetTranslation * VPointF(1, -1)
                ClockViewIds.MINUTE_FIRST_DIGIT -> targetTranslation * VPointF(-1, 1)
                ClockViewIds.MINUTE_SECOND_DIGIT -> targetTranslation
                else -> targetTranslation
            }
        }
    }
}

@SuppressLint("ViewConstructor")
class InflateClockSmallViewGroup(clockCtx: CustomClockContext, layer: ComposedDigitalHandLayer) :
    InflateClockViewGroup(clockCtx) {
    private val colon =
        InflateChildView(clockCtx, layer.font!!)
            .apply {
                text = ":"
                horizontalAlignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
            }
            .also { addView(it) }

    override val children: Sequence<InflateChildView>
        get() = sequenceOf(hourDigit1, hourDigit2, colon, minuteDigit1, minuteDigit2)

    override fun onViewAdded(child: View?) {
        if (child == null) return
        logger.onViewAdded(child)
        super.onViewAdded(child)
        if (childCount < 5 || child !is CustomDigitalTextView) return
        colon.applyStyles(child.textStyle, child.aodStyle)
    }

    override fun getChildFrame(child: InflateChildView): VRectF {
        val leftChild =
            when (child) {
                hourDigit1 -> null
                hourDigit2 -> if (hasLeadingDigit) hourDigit1 else null
                colon -> hourDigit2
                minuteDigit1 -> colon
                minuteDigit2 -> minuteDigit1
                else -> null
            }
        return VRectF.fromTopLeft(
            VPointF(
                leftChild?.let { it.right - it.measuredWidth * 0.25f } ?: 0f,
                (measuredHeight - child.measuredHeight) / 2,
            ),
            child.measuredSize,
        )
    }

    override fun calculateSize(measureSpec: VMeasurePoint): VPointF {
        maxSingleDigitSize = hourDigit1.maxSingleDigitSize
        val height =
            if (measureSpec.height.size != 0) {
                measureSpec.height.size.toFloat()
            } else {
                maxSingleDigitSize.y
            }
        return VPointF(children.maxOf { it.right }, height)
    }
}
