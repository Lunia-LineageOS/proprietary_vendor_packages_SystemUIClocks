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
import android.content.theming.ThemeStyle
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.android.systemui.clocks.controller.AnalogClockFaceLayout
import com.android.systemui.clocks.controller.AnalogHandLayerController
import com.android.systemui.clocks.controller.AssetLayerController
import com.android.systemui.clocks.controller.ClockLayerController
import com.android.systemui.clocks.controller.ComposedDigitalLayerController
import com.android.systemui.clocks.controller.CustomClockController
import com.android.systemui.clocks.controller.DigitalHandLayerController
import com.android.systemui.clocks.view.CustomClockViewGroup
import com.android.systemui.clocks.view.CustomDigitalTextView
import com.android.systemui.clocks.view.DigitalFaceViewGroup
import com.android.systemui.customization.clocks.AnimationState
import com.android.systemui.customization.clocks.ClockContextImpl
import com.android.systemui.customization.clocks.ClockLogger
import com.android.systemui.customization.clocks.TimeKeeper
import com.android.systemui.customization.clocks.TimeKeeperImpl
import com.android.systemui.customization.clocks.TypefaceCache
import com.android.systemui.customization.clocks.view.DefaultClockFaceLayout
import com.android.systemui.log.core.Logger
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceLayout
import com.android.systemui.plugins.keyguard.ui.clocks.ClockMessageBuffers
import com.android.systemui.plugins.keyguard.ui.clocks.ClockMetadata
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPickerConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockProviderPlugin
import com.android.systemui.plugins.keyguard.ui.clocks.ClockSettings

abstract class BaseClockProvider(designs: List<ClockDesign>) : ClockProviderPlugin {
    private lateinit var hostCtx: Context
    private lateinit var pluginCtx: Context
    private var messageBuffers: ClockMessageBuffers? = null
    private lateinit var logger: Logger
    private lateinit var typefaceCache: TypefaceCache<FontSpec>
    private var timeKeeper: TimeKeeper = TimeKeeperImpl()
    private val designs = designs.associateBy { it.id }.toMutableMap()

    protected lateinit var assets: AssetLoader
    protected lateinit var fontDiskCache: FontDiskCache

    open val factory: TypeFactory = BaseTypeFactory()

    open val isAnimationEnabled: Boolean
        get() = true

    override fun onCreate(hostCtx: Context, pluginCtx: Context) {
        this.hostCtx = hostCtx
        this.pluginCtx = pluginCtx
    }

    override fun initialize(buffers: ClockMessageBuffers?) {
        messageBuffers = buffers
        val messageBuffer = buffers?.infraMessageBuffer ?: ClockLogger.DEFAULT_MESSAGE_BUFFER
        logger = Logger(messageBuffer, TAG)

        if (!::assets.isInitialized) {
            assets = AssetLoader(pluginCtx, hostCtx, "clocks/", messageBuffer)
        }

        if (!::fontDiskCache.isInitialized) {
            fontDiskCache = FontDiskCache(hostCtx)
            fontDiskCache.freeAll()
        }

        if (!::typefaceCache.isInitialized) {
            typefaceCache =
                TypefaceCache(messageBuffer, 30) { fontSpec ->
                    when (val source = fontSpec.source) {
                        is FontSource.AssetFontSource ->
                            Typeface.createFromAsset(pluginCtx.assets, "fonts/" + source.file)
                    }
                }
        }

        designs.values.removeAll { !validateDesign(it) }
    }

    fun validateDesign(design: ClockDesign): Boolean {
        val validator = ClockDesignValidator(assets)
        validator.validate(design)
        if (!validator.isValid) {
            logger.e({ "Clock '$str1' failed validation:\n$str2" }) {
                str1 = design.id
                str2 = buildString { validator.errors.forEach { appendLine("    $it") } }
            }
        }
        return validator.isValid
    }

    open class BaseTypeFactory : TypeFactory {
        override fun createViewGroup(clockCtx: CustomClockContext, face: ClockFace): ViewGroup {
            if (face.layers.any { it is AnalogHandLayer }) {
                return RelativeLayout(clockCtx.context)
            }
            return CustomClockViewGroup(clockCtx, face.faceLayout)
        }

        override fun createComposedView(
            clockCtx: CustomClockContext,
            layer: ComposedDigitalHandLayer,
            dozeState: AnimationState,
        ): DigitalFaceViewGroup<*> {
            error("'${layer.customizedView}' is invalid")
        }

        open fun createTextView(
            clockCtx: CustomClockContext,
            font: FontSpec,
        ): CustomDigitalTextView {
            return CustomDigitalTextView(clockCtx, font)
        }

        override fun createLayerController(
            clockCtx: CustomClockContext,
            layer: ClockLayer,
            isLargeClock: Boolean,
        ): ClockLayerController {
            return when {
                layer is AssetLayer -> AssetLayerController(clockCtx, layer)
                layer is AnalogHandLayer -> AnalogHandLayerController(clockCtx, layer)
                layer is ComposedDigitalHandLayer ->
                    ComposedDigitalLayerController(clockCtx, layer, isLargeClock)
                layer is DigitalHandLayer && layer.font != null ->
                    DigitalHandLayerController(
                        clockCtx,
                        layer,
                        createTextView(clockCtx, layer.font),
                    )
                else -> throw IllegalArgumentException("Failed to find a controller for $layer")
            }
        }

        override fun createLayout(
            clockCtx: CustomClockContext,
            face: ClockFace,
            view: View,
        ): ClockFaceLayout {
            if (face.layers.any { it is AnalogHandLayer }) {
                return AnalogClockFaceLayout(view)
            }
            return DefaultClockFaceLayout(view)
        }
    }

    override fun getClocks(): List<ClockMetadata> {
        return designs.keys.map { ClockMetadata(it) }
    }

    private fun getDesign(settings: ClockSettings): ClockDesign {
        val clockId = requireNotNull(settings.clockId) { "ClockId not specified in $settings" }
        return requireNotNull(designs[clockId]) { "$clockId is not valid for $TAG" }
    }

    override fun createClock(ctx: Context, settings: ClockSettings): CustomClockController {
        val design = getDesign(settings)
        val factory = factory
        val assets = assets.copy(hostCtx = ctx)
        assets.setSeedColor(settings.seedColor, design.colorPalette)
        return CustomClockController(
            CustomClockContext(
                factory,
                assets,
                typefaceCache,
                ClockContextImpl(
                    context = pluginCtx,
                    resources = pluginCtx.resources,
                    settings = settings,
                    messageBuffer =
                        messageBuffers?.infraMessageBuffer ?: ClockLogger.DEFAULT_MESSAGE_BUFFER,
                    vibrator = null,
                    timeKeeper = timeKeeper,
                    isAnimationEnabled = isAnimationEnabled,
                ),
            ),
            design,
            messageBuffers,
        )
    }

    override fun getClockPickerConfig(settings: ClockSettings): ClockPickerConfig {
        val design = getDesign(settings)
        return ClockPickerConfig(
            id = design.id,
            name = design.name?.let { assets.tryReadString(it) ?: it } ?: "",
            description = design.description?.let { assets.tryReadString(it) ?: it } ?: "",
            thumbnail =
                design.thumbnail?.let { assets.tryReadDrawableAsset(it) }
                    ?: pluginCtx.resources.getDrawable(R.drawable.placeholder_thumbnail, null),
            isReactiveToTone =
                design.colorPalette == null || design.colorPalette == ThemeStyle.CLOCK,
        )
    }

    companion object {
        private val TAG = this::class.simpleName!!
    }
}
