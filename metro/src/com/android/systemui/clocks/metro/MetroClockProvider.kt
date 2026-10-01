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

package com.android.systemui.clocks.metro

import android.view.ViewGroup
import com.android.systemui.clocks.BaseClockProvider
import com.android.systemui.clocks.BaseTypeFactory
import com.android.systemui.clocks.ClockFace
import com.android.systemui.clocks.ClockLayer
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.DigitalHandLayer
import com.android.systemui.clocks.LottieTextStyle
import com.android.systemui.clocks.TypeFactory
import com.android.systemui.clocks.controller.ClockLayerController
import com.android.systemui.clocks.controller.DigitalHandLayerController
import com.android.systemui.clocks.metro.view.LottieDigitalTextView
import com.android.systemui.clocks.metro.view.MetroClockViewGroup
import com.android.systemui.plugins.annotations.Requires
import com.android.systemui.plugins.keyguard.ui.clocks.ClockProviderPlugin

@Requires(target = ClockProviderPlugin::class, version = ClockProviderPlugin.VERSION)
class MetroClockProvider : BaseClockProvider(listOf(metroDesign)) {
    override val factory: TypeFactory =
        object : BaseTypeFactory() {
            override fun createLayerController(
                clockCtx: CustomClockContext,
                layer: ClockLayer,
                isLargeClock: Boolean,
            ): ClockLayerController {
                if (layer is DigitalHandLayer && layer.style is LottieTextStyle) {
                    return DigitalHandLayerController(
                        clockCtx,
                        layer,
                        LottieDigitalTextView(clockCtx, isLargeClock),
                    )
                }
                return super.createLayerController(clockCtx, layer, isLargeClock)
            }

            override fun createViewGroup(
                clockCtx: CustomClockContext,
                clockFace: ClockFace,
            ): ViewGroup = MetroClockViewGroup(clockCtx, clockFace.faceLayout)
        }
}
