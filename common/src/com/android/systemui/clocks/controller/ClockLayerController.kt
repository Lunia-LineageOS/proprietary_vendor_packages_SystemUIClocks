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
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAnimations
import com.android.systemui.plugins.keyguard.ui.clocks.ClockEvents
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceEvents

interface ClockLayerController {
    val config: ClockFaceConfig
    val events: ClockEvents
    val faceEvents: ClockFaceEvents
    val animations: ClockAnimations
    val view: View

    fun setOnViewBoundsChanged(callback: ((android.graphics.RectF) -> Unit)?) = Unit

    fun setOnViewMaxSizeChanged(callback: ((VPointF) -> Unit)?) = Unit
}
