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

package com.android.systemui.clocks.numoverlap

import com.android.systemui.clocks.BaseClockProvider
import com.android.systemui.clocks.BaseTypeFactory
import com.android.systemui.clocks.ComposedDigitalHandLayer
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.FontSpec
import com.android.systemui.clocks.TypeFactory
import com.android.systemui.clocks.numoverlap.view.NumberOverlapChildView
import com.android.systemui.clocks.numoverlap.view.NumberOverlapClockViewGroup
import com.android.systemui.clocks.numoverlap.view.NumberOverlapSmallClockViewGroup
import com.android.systemui.clocks.view.DigitalFaceViewGroup
import com.android.systemui.customization.clocks.AnimationState
import com.android.systemui.plugins.annotations.Requires
import com.android.systemui.plugins.keyguard.ui.clocks.ClockProviderPlugin

@Requires(target = ClockProviderPlugin::class, version = ClockProviderPlugin.VERSION)
class NumberOverlapClockProvider : BaseClockProvider(listOf(numberOverlapDesign)) {
    override val factory: TypeFactory =
        object : BaseTypeFactory() {
            override fun createComposedView(
                clockCtx: CustomClockContext,
                layer: ComposedDigitalHandLayer,
                dozeState: AnimationState,
            ): DigitalFaceViewGroup {
                return when (layer.customizedView) {
                    "NumberOverlap" -> NumberOverlapClockViewGroup(clockCtx, dozeState)
                    "NumberOverlapSmall" ->
                        NumberOverlapSmallClockViewGroup(clockCtx, layer, dozeState)
                    else -> super.createComposedView(clockCtx, layer, dozeState)
                }
            }

            override fun createTextView(clockCtx: CustomClockContext, fontSpec: FontSpec) =
                NumberOverlapChildView(clockCtx, fontSpec)
        }
}
