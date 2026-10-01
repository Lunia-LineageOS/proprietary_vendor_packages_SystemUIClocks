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

package com.android.systemui.clocks.controller

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.Modifier
import androidx.constraintlayout.widget.ConstraintSet
import com.android.systemui.customization.clocks.R as clocksR
import com.android.systemui.customization.clocks.view.DefaultClockFaceLayout
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPreviewConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds
import kotlin.math.min

class AnalogClockFaceLayout(view: View) : DefaultClockFaceLayout(view) {
    private var maxWidth = -1
    private var maxHeight = -1

    init {
        (view as? ViewGroup)?.let { viewGroup ->
            for (i in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(i)
                maxHeight = maxOf(maxHeight, child.layoutParams.height)
                maxWidth = maxOf(maxWidth, child.layoutParams.width)
            }
        }
        smallClockModifier = { Modifier.aspectRatio(1f) }
    }

    override fun applyConstraints(constraints: ConstraintSet): ConstraintSet {
        constraints.constrainHeight(
            ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE,
            view.context.resources.getDimensionPixelSize(clocksR.dimen.large_clock_text_size) * 2,
        )
        var scale =
            if (constraints.getHeight(view.id) > 0) {
                constraints.getHeight(view.id).toFloat() / maxHeight
            } else {
                -1f
            }
        val widthScale =
            if (constraints.getWidth(view.id) > 0) {
                constraints.getWidth(view.id).toFloat() / maxWidth
            } else {
                -1f
            }
        scale =
            when {
                scale >= 0f && widthScale >= 0f -> min(scale, widthScale)
                scale < 0f -> if (widthScale >= 0f) widthScale else 1f
                else -> scale
            }
        constraints.constrainWidth(view.id, (scale * maxWidth).toInt())
        constraints.constrainHeight(view.id, (scale * maxHeight).toInt())
        return constraints
    }

    override fun applyExternalDisplayPresentationConstraints(
        constraints: ConstraintSet
    ): ConstraintSet {
        super.applyExternalDisplayPresentationConstraints(constraints)
        applyConstraints(constraints)
        constraints.connect(
            ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
        )
        constraints.connect(
            ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
        )
        constraints.connect(
            ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
        )
        constraints.connect(
            ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
        )
        return constraints
    }

    override fun applyPreviewConstraints(
        clockPreviewConfig: ClockPreviewConfig,
        constraints: ConstraintSet,
    ): ConstraintSet {
        super.applyPreviewConstraints(clockPreviewConfig, constraints)
        applyConstraints(constraints)
        return constraints
    }
}
