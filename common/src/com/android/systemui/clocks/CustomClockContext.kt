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

package com.android.systemui.clocks

import android.content.Context
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.android.systemui.customization.clocks.AnimationState
import com.android.systemui.customization.clocks.ClockContext
import com.android.systemui.customization.clocks.ClockContextImpl
import com.android.systemui.customization.clocks.view.DefaultClockFaceLayout
import com.android.systemui.log.core.MessageBuffer
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceLayout

interface TypeFactory {
    fun createLayerController(
        clockCtx: CustomClockContext,
        layer: ClockLayer,
        isLargeClock: Boolean,
    ): ClockLayerController

    fun createLayout(
        clockCtx: CustomClockContext,
        clockFace: ClockFace,
        view: android.view.View,
    ): ClockFaceLayout

    fun createViewGroup(clockCtx: CustomClockContext, clockFace: ClockFace): ViewGroup

    fun createTextView(clockCtx: CustomClockContext, fontSpec: FontSpec): CustomDigitalTextView

    fun createComposedView(
        clockCtx: CustomClockContext,
        layer: ComposedDigitalHandLayer,
        dozeState: AnimationState,
    ): DigitalFaceViewGroup
}

class CustomClockContext(
    val factory: TypeFactory,
    val assets: AssetLoader,
    val typefaceCache: TypefaceCache<FontSpec>,
    private val innerCtx: ClockContextImpl,
) : ClockContext by innerCtx {
    override val context: Context
        get() = innerCtx.context

    fun copy(messageBuffer: MessageBuffer): CustomClockContext {
        return CustomClockContext(
            factory,
            assets.copy(messageBuffer = messageBuffer),
            typefaceCache,
            ClockContextImpl(
                context = innerCtx.context,
                resources = innerCtx.resources,
                settings = innerCtx.settings,
                messageBuffer = messageBuffer,
                vibrator = innerCtx.vibrator,
                timeKeeper = innerCtx.timeKeeper,
                isAnimationEnabled = innerCtx.isAnimationEnabled,
            ),
        )
    }
}

open class BaseTypeFactory : TypeFactory {
    override fun createLayerController(
        clockCtx: CustomClockContext,
        layer: ClockLayer,
        isLargeClock: Boolean,
    ): ClockLayerController {
        return when (layer) {
            is AssetLayer -> AssetLayerController(clockCtx, layer)
            is AnalogHandLayer -> AnalogHandLayerController(clockCtx, layer)
            is DigitalHandLayer ->
                DigitalHandLayerController(
                    clockCtx,
                    layer,
                    createTextView(
                        clockCtx,
                        requireNotNull(layer.font) { "Digital layer requires a font" },
                    ),
                )
            is ComposedDigitalHandLayer ->
                ComposedDigitalLayerController(clockCtx, layer, isLargeClock)
        }
    }

    override fun createLayout(
        clockCtx: CustomClockContext,
        clockFace: ClockFace,
        view: android.view.View,
    ): ClockFaceLayout {
        if (clockFace.layers.any { it is AnalogHandLayer }) {
            return AnalogClockFaceLayout(view)
        }
        return DefaultClockFaceLayout(view)
    }

    override fun createViewGroup(clockCtx: CustomClockContext, clockFace: ClockFace): ViewGroup {
        if (clockFace.layers.any { it is AnalogHandLayer }) {
            return RelativeLayout(clockCtx.context)
        }
        return CustomClockViewGroup(clockCtx, clockFace.faceLayout)
    }

    override fun createTextView(
        clockCtx: CustomClockContext,
        fontSpec: FontSpec,
    ): CustomDigitalTextView {
        return CustomDigitalTextView(clockCtx, fontSpec)
    }

    override fun createComposedView(
        clockCtx: CustomClockContext,
        layer: ComposedDigitalHandLayer,
        dozeState: AnimationState,
    ): DigitalFaceViewGroup {
        return CustomClockViewGroup(clockCtx, null)
    }
}
