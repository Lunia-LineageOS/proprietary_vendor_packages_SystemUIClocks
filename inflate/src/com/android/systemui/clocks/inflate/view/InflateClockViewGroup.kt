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
import com.android.systemui.plugins.keyguard.VRectF
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds

class InflateChildView(clockCtx: CustomClockContext, fontSpec: FontSpec) :
    CustomDigitalTextView(clockCtx, fontSpec)

open class InflateClockViewGroup(clockCtx: CustomClockContext) : DigitalFaceViewGroup(clockCtx) {
    protected var maxSingleDigitSize = VPointF(-1f)
    protected var hourDigit1: InflateChildView? = null
    protected var hourDigit2: InflateChildView? = null
    protected var minuteDigit1: InflateChildView? = null
    protected var minuteDigit2: InflateChildView? = null

    @Suppress("UNCHECKED_CAST")
    override val children: Sequence<InflateChildView>
        get() =
            sequenceOf(hourDigit1, hourDigit2, minuteDigit1, minuteDigit2)
                as Sequence<InflateChildView>

    private var lockscreenTranslate = VPointF.ZERO
    private var aodTranslate = VPointF.ZERO
    private var bounceTranslate = VPointF.ZERO

    init {
        setWillNotDraw(false)
        layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
    }

    override fun onViewAdded(child: View?) {
        if (child !is InflateChildView) {
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
        maxSingleDigitSize = hourDigit1!!.maxSingleDigitSize
        aodTranslate = maxSingleDigitSize * OVERLAPPED_RATIO * AOD_TRANSLATE_RATIO
        bounceTranslate = maxSingleDigitSize * OVERLAPPED_RATIO * BOUNCE_TRANSLATE_RATIO
        return maxSingleDigitSize * (2 - OVERLAPPED_RATIO)
    }

    override fun getChildFrame(child: InflateChildView): VRectF {
        var point = (1 - OVERLAPPED_RATIO) * maxSingleDigitSize
        point =
            if (child == hourDigit1) {
                VPointF.ZERO
            } else if (child == hourDigit2) {
                VPointF(point.x, 0f)
            } else if (child == minuteDigit1) {
                VPointF(0f, point.y)
            } else if (child == minuteDigit2) {
                point
            } else {
                VPointF.ZERO
            }
        return VRectF.fromTopLeft(point, child.measuredSize)
    }

    override fun refreshTime() {
        super.refreshTime()
        children.forEach { it.refreshText() }
    }

    override fun animateDoze(isDozing: Boolean, isAnimated: Boolean) {
        dozeControlState.setAnimateDoze {
            super.animateDoze(isDozing, isAnimated)
            if (maxSingleDigitSize.x < 0f || maxSingleDigitSize.y < 0f) {
                measure(0, 0)
            }
            children.forEach { child ->
                val animator = child.digitTranslateAnimator
                if (animator != null) {
                    if (!isDozing) {
                        val phase3BounceAnim = Runnable {
                            animator.animatePosition(
                                animate = isAnimated && isAnimationEnabled,
                                interpolator = Companion.INFLATE_CLOCK_PHASE3_INTERPOLATOR,
                                duration = Companion.INFLATE_CLOCK_PHASE3_DURATION[id] ?: 0L,
                                targetTranslation =
                                    Companion.updateDirectionalTargetTranslate(
                                        id,
                                        lockscreenTranslate,
                                    ),
                            )
                        }
                        val phase2BounceAnim = Runnable {
                            animator.animatePosition(
                                animate = isAnimated && isAnimationEnabled,
                                interpolator = Companion.INFLATE_CLOCK_PHASE2_INTERPOLATOR,
                                duration = Companion.INFLATE_CLOCK_PHASE2_DURATION[id] ?: 0L,
                                targetTranslation =
                                    Companion.updateDirectionalTargetTranslate(id, bounceTranslate),
                                onAnimationEnd = phase3BounceAnim,
                            )
                        }
                        animator.animatePosition(
                            animate = isAnimated && isAnimationEnabled,
                            interpolator = INFLATE_CLOCK_PHASE1_INTERPOLATOR,
                            duration = INFLATE_CLOCK_PHASE1_DURATION[id] ?: 0L,
                            targetTranslation =
                                Companion.updateDirectionalTargetTranslate(id, lockscreenTranslate),
                            onAnimationEnd = phase2BounceAnim,
                        )
                    } else {
                        animator.animatePosition(
                            animate = isAnimated && isAnimationEnabled,
                            duration = 800L,
                            interpolator = INFLATE_CLOCK_DOZING_INTERPOLATOR,
                            targetTranslation =
                                Companion.updateDirectionalTargetTranslate(id, aodTranslate),
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val OVERLAPPED_RATIO = VPointF(0.32f, 0.15f)
        val INFLATE_CLOCK_PHASE2_INTERPOLATOR = PathInterpolator(0.22f, 0f, 0.54f, 1f)
        val INFLATE_CLOCK_PHASE3_INTERPOLATOR = PathInterpolator(0.29f, 0f, 0.58f, 1f)
        private val INFLATE_CLOCK_PHASE1_INTERPOLATOR = PathInterpolator(0.33f, 0f, 0.6f, 1f)
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

        fun updateDirectionalTargetTranslate(viewId: Int, translate: VPointF): VPointF {
            return when (viewId) {
                ClockViewIds.HOUR_FIRST_DIGIT -> translate * VPointF(-1f, -1f)
                ClockViewIds.HOUR_SECOND_DIGIT -> translate * VPointF(1f, -1f)
                ClockViewIds.MINUTE_FIRST_DIGIT -> translate * VPointF(-1f, 1f)
                ClockViewIds.MINUTE_SECOND_DIGIT -> translate
                else -> translate
            }
        }
    }
}

class InflateClockSmallViewGroup(clockCtx: CustomClockContext, layer: ComposedDigitalHandLayer) :
    InflateClockViewGroup(clockCtx) {
    private val colon: InflateChildView =
        InflateChildView(clockCtx, requireNotNull(layer.font)).apply {
            text = ":"
            horizontalAlignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
        }

    init {
        addView(colon)
    }

    @Suppress("UNCHECKED_CAST")
    override val children: Sequence<InflateChildView>
        get() =
            sequenceOf(hourDigit1, hourDigit2, colon, minuteDigit1, minuteDigit2)
                as Sequence<InflateChildView>

    override fun onViewAdded(child: View?) {
        if (child == null) {
            return
        }
        logger.onViewAdded(child)
        super.onViewAdded(child)
        if (childCount >= 5 && child is CustomDigitalTextView) {
            colon.applyStyles(child.textStyle, child.aodStyle)
        }
    }

    override fun getChildFrame(child: InflateChildView): VRectF {
        var previous: InflateChildView? = null
        if (child != hourDigit1) {
            if (child == hourDigit2) {
                if (hasLeadingDigit) {
                    previous = hourDigit1
                }
            } else if (child == colon) {
                previous = hourDigit2
            } else if (child == minuteDigit1) {
                previous = colon
            } else if (child == minuteDigit2) {
                previous = minuteDigit1
            }
        }
        val x = if (previous != null) previous.right - (previous.measuredWidth * 0.25f) else 0f
        return VRectF.fromTopLeft(
            VPointF(x, (measuredHeight - child.measuredHeight) / 2),
            child.measuredSize,
        )
    }

    override fun calculateSize(measureSpec: VMeasurePoint): VPointF {
        maxSingleDigitSize = hourDigit1!!.maxSingleDigitSize
        val height =
            if (measureSpec.height.size != 0) {
                measureSpec.height.size.toFloat()
            } else {
                maxSingleDigitSize.y
            }
        return VPointF(children.maxOf { it.right }.toFloat(), height)
    }
}
