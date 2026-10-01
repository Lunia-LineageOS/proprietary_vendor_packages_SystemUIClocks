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

import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.DigitalFaceLayout
import com.android.systemui.customization.clocks.utils.ViewUtils.measuredSize
import com.android.systemui.plugins.keyguard.VMeasurePoint
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.VRectF
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds

open class CustomClockViewGroup(
    clockCtx: CustomClockContext,
    private val faceLayout: DigitalFaceLayout?,
) : DigitalFaceViewGroup(clockCtx) {

    override val children: Sequence<CustomDigitalTextView>
        get() =
            (0 until childCount)
                .asSequence()
                .map { getChildAt(it) }
                .filterIsInstance<CustomDigitalTextView>()

    override fun calculateSize(measureSpec: VMeasurePoint): VPointF {
        if (faceLayout == DigitalFaceLayout.FOUR_DIGITS_HORIZONTAL) {
            var measuredWidth = 0f
            children.forEach { measuredWidth += it.measuredWidth }
            return VPointF(
                measuredWidth,
                if (measureSpec.height.size != 0) {
                    measureSpec.height.size.toFloat()
                } else {
                    maxChildSize.y
                },
            )
        }
        return maxChildSize * if (children.count() < 4) 1f else 2f
    }

    override fun getChildFrame(child: CustomDigitalTextView): VRectF {
        if (faceLayout == DigitalFaceLayout.FOUR_DIGITS_HORIZONTAL) {
            val previous =
                when (child.id) {
                    ClockViewIds.HOUR_SECOND_DIGIT ->
                        if (hasLeadingDigit) getChild(ClockViewIds.HOUR_FIRST_DIGIT) else null
                    ClockViewIds.MINUTE_FIRST_DIGIT -> getChild(ClockViewIds.HOUR_SECOND_DIGIT)
                    ClockViewIds.MINUTE_SECOND_DIGIT -> getChild(ClockViewIds.MINUTE_FIRST_DIGIT)
                    else -> null
                }
            return VRectF.fromTopLeft(
                VPointF(
                    previous?.right?.toFloat() ?: 0f,
                    (measuredHeight - child.measuredHeight) / 2f,
                ),
                child.measuredSize,
            )
        }
        val offset =
            when (child.id) {
                ClockViewIds.HOUR_DIGIT_PAIR,
                ClockViewIds.HOUR_FIRST_DIGIT -> VPointF(0f, 0f)
                ClockViewIds.HOUR_SECOND_DIGIT -> VPointF(maxChildSize.x, 0f)
                ClockViewIds.MINUTE_DIGIT_PAIR,
                ClockViewIds.MINUTE_FIRST_DIGIT -> VPointF(0f, maxChildSize.y)
                ClockViewIds.MINUTE_SECOND_DIGIT -> maxChildSize
                else -> VPointF(0f, 0f)
            }
        val count = children.count()
        val centerOffset =
            VPointF(
                (if (count < 4) measuredWidth / 2f else measuredWidth / 4f) -
                    child.measuredSize.x / 2f,
                0f,
            )
        return VRectF.fromTopLeft(offset + centerOffset, child.measuredSize)
    }

    override fun refreshTime() {
        children.forEach { it.refreshText() }
    }
}
