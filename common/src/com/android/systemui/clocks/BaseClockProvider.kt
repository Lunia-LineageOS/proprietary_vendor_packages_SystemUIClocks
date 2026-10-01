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
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import com.android.systemui.clocks.controller.CustomClockController
import com.android.systemui.customization.clocks.ClockContextImpl
import com.android.systemui.customization.clocks.TimeKeeperImpl
import com.android.systemui.customization.clocks.TypefaceCache
import com.android.systemui.log.LogcatOnlyMessageBuffer
import com.android.systemui.log.core.LogLevel
import com.android.systemui.log.core.Logger
import com.android.systemui.plugins.annotations.Requires
import com.android.systemui.plugins.keyguard.ui.clocks.ClockController
import com.android.systemui.plugins.keyguard.ui.clocks.ClockMessageBuffers
import com.android.systemui.plugins.keyguard.ui.clocks.ClockMetadata
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPickerConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockProviderPlugin
import com.android.systemui.plugins.keyguard.ui.clocks.ClockSettings

@Requires(target = ClockProviderPlugin::class, version = ClockProviderPlugin.VERSION)
abstract class BaseClockProvider(private val designs: List<ClockDesign>) : ClockProviderPlugin {
    private lateinit var hostCtx: Context
    private lateinit var pluginCtx: Context
    private lateinit var messageBuffers: ClockMessageBuffers
    private lateinit var assets: AssetLoader
    private lateinit var typefaceCache: TypefaceCache<FontSpec>
    private lateinit var logger: Logger

    private val designMap = designs.associateBy { it.id }.toMutableMap()

    override fun onCreate(hostCtx: Context, pluginCtx: Context) {
        this.hostCtx = hostCtx
        this.pluginCtx = pluginCtx
    }

    override fun initialize(buffers: ClockMessageBuffers?) {
        messageBuffers = buffers ?: ClockMessageBuffers(LogcatOnlyMessageBuffer(LogLevel.DEBUG))
        val infraBuffer = messageBuffers.infraMessageBuffer
        logger = Logger(infraBuffer, TAG)

        assets = AssetLoader(pluginCtx, hostCtx, ASSET_BASE_DIR, infraBuffer)
        typefaceCache =
            TypefaceCache(infraBuffer, ANIMATION_FRAME_COUNT) { fontSpec -> loadTypeface(fontSpec) }

        designMap.keys.toList().forEach { id ->
            val design = designMap[id]!!
            val errors = validateDesign(design)
            if (errors.isNotEmpty()) {
                logger.e({ "Clock '$str1' failed validation:\n$str2" }) {
                    str1 = design.id
                    str2 = errors.joinToString("\n") { "    $it" }
                }
                designMap.remove(id)
            }
        }
    }

    private fun loadTypeface(fontSpec: FontSpec): Typeface {
        return when (fontSpec.source) {
            is FontSource.AssetFontSource ->
                Typeface.createFromAsset(pluginCtx.assets, FONT_ASSET_DIR + fontSpec.source.file)
        }
    }

    private fun getDesign(settings: ClockSettings): ClockDesign {
        val clockId = requireNotNull(settings.clockId) { "ClockId not specified in $settings" }
        return requireNotNull(designMap[clockId]) { "$clockId is not valid for $TAG" }
    }

    override fun getClocks(): List<ClockMetadata> {
        return designMap.keys.map { ClockMetadata(it) }
    }

    override fun getClockPickerConfig(settings: ClockSettings): ClockPickerConfig {
        val design = getDesign(settings)
        val name = assets.tryReadString(design.name) ?: design.name ?: ""
        val description = assets.tryReadString(design.description) ?: design.description ?: ""
        val thumbnail: Drawable =
            design.thumbnail?.let { assets.tryReadDrawableAsset(it) }
                ?: pluginCtx.getDrawable(R.drawable.placeholder_thumbnail)!!
        return ClockPickerConfig(
            design.id,
            name,
            description,
            thumbnail,
            isReactiveToTone = design.colorPalette == null || design.colorPalette == 8,
        )
    }

    override fun createClock(ctx: Context, settings: ClockSettings): ClockController {
        val design = getDesign(settings)
        val clockAssets = assets.copy(hostCtx = ctx)
        clockAssets.setSeedColor(settings.seedColor, design.colorPalette)
        val clockCtx =
            CustomClockContext(
                factory,
                clockAssets,
                typefaceCache,
                ClockContextImpl(
                    context = pluginCtx,
                    resources = pluginCtx.resources,
                    settings = settings,
                    messageBuffer = messageBuffers.infraMessageBuffer,
                    vibrator = null,
                    timeKeeper = TimeKeeperImpl(),
                    isAnimationEnabled = isAnimationEnabled,
                ),
            )
        return CustomClockController(clockCtx, design, messageBuffers)
    }

    protected open val isAnimationEnabled: Boolean
        get() = true

    protected open val factory: TypeFactory = BaseTypeFactory()

    private fun validateDesign(design: ClockDesign): List<String> {
        val errors = mutableListOf<String>()
        design.large.layers.forEach { layer -> validateLayer(layer, errors) }
        design.small?.layers?.forEach { layer -> validateLayer(layer, errors) }
        return errors
    }

    private fun validateLayer(layer: ClockLayer, errors: MutableList<String>) {
        when (layer) {
            is AssetLayer -> validateAsset(layer.asset, errors)
            is AnalogHandLayer -> validateAsset(layer.asset, errors)
            is DigitalHandLayer -> layer.font?.let { font -> validateFont(font, errors) }
            is ComposedDigitalHandLayer -> {
                layer.font?.let { font -> validateFont(font, errors) }
                layer.digitalLayers.forEach { child ->
                    child.font?.let { font -> validateFont(font, errors) }
                }
            }
        }
    }

    private fun validateAsset(asset: AssetReference, errors: MutableList<String>) {
        listOfNotNull(asset.light, asset.dark, asset.doze).forEach { reference ->
            if (!assets.assetExists(reference)) {
                errors.add("Failed to resolve asset $reference")
            }
        }
    }

    private fun validateFont(font: FontSpec, errors: MutableList<String>) {
        when (font.source) {
            is FontSource.AssetFontSource ->
                if (!assets.assetExists(FONT_ASSET_DIR + font.source.file)) {
                    errors.add("Failed to resolve font ${font.source.file}")
                }
        }
    }

    private companion object {
        const val TAG = "BaseClockProvider"
        const val ASSET_BASE_DIR = "clocks/"
        const val FONT_ASSET_DIR = "fonts/"
        const val ANIMATION_FRAME_COUNT = 30
    }
}
