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

import android.annotation.SuppressLint
import androidx.core.view.children as childViews
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.DigitalFaceLayout
import com.android.systemui.clocks.view.DigitalFaceViewGroup
import com.android.systemui.customization.clocks.utils.ViewUtils.measuredSize
import com.android.systemui.plugins.keyguard.VMeasurePoint
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.VRectF
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds

@SuppressLint("ViewConstructor")
class MetroClockViewGroup(
    clockCtx: CustomClockContext,
    private val faceLayout: DigitalFaceLayout?,
) : DigitalFaceViewGroup<LottieDigitalTextView>(clockCtx) {
    override val children: Sequence<LottieDigitalTextView>
        get() = childViews.filterIsInstance<LottieDigitalTextView>()

    override fun calculateSize(measureSpec: VMeasurePoint): VPointF {
        if (faceLayout == DigitalFaceLayout.FOUR_DIGITS_HORIZONTAL) {
            var width = 0
            for (child in children) {
                width += child.measuredWidth
            }
            val height =
                if (measureSpec.height.size != 0) {
                    measureSpec.height.size.toFloat()
                } else {
                    maxChildSize.y
                }
            return VPointF(width, height)
        }
        return maxChildSize * VPointF(if (children.count() < 4) 1f else 2f, 2f)
    }

    override fun getChildFrame(child: LottieDigitalTextView): VRectF {
        if (faceLayout == DigitalFaceLayout.FOUR_DIGITS_HORIZONTAL) {
            val leftChild =
                when (child.id) {
                    ClockViewIds.HOUR_FIRST_DIGIT -> null
                    ClockViewIds.HOUR_SECOND_DIGIT ->
                        if (hasLeadingDigit) getChild(ClockViewIds.HOUR_FIRST_DIGIT) else null
                    ClockViewIds.MINUTE_FIRST_DIGIT -> getChild(ClockViewIds.HOUR_SECOND_DIGIT)
                    ClockViewIds.MINUTE_SECOND_DIGIT -> getChild(ClockViewIds.MINUTE_FIRST_DIGIT)
                    else -> null
                }
            return VRectF.fromTopLeft(
                VPointF(leftChild?.right ?: 0, (measuredHeight - child.measuredHeight) / 2),
                child.measuredSize,
            )
        }

        val maxChildSize = maxChildSize
        val offset =
            when (child.id) {
                ClockViewIds.HOUR_DIGIT_PAIR,
                ClockViewIds.HOUR_FIRST_DIGIT -> VPointF.ZERO
                ClockViewIds.HOUR_SECOND_DIGIT -> VPointF(maxChildSize.x, 0f)
                ClockViewIds.MINUTE_DIGIT_PAIR,
                ClockViewIds.MINUTE_FIRST_DIGIT -> VPointF(0f, maxChildSize.y)
                ClockViewIds.MINUTE_SECOND_DIGIT -> VPointF(maxChildSize.x, maxChildSize.y)
                else -> VPointF.ZERO
            }
        val size = child.measuredSize
        val xOffset = if (children.count() < 4) measuredWidth / 2f else measuredWidth / 4f
        return VRectF.fromTopLeft(offset + VPointF(xOffset - size.x / 2f, 0f), size)
    }
}
