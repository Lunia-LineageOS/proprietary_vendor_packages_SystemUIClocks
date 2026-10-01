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
import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.theming.ThemeStyle
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.TypedValue
import com.android.internal.graphics.ColorUtils
import com.android.internal.graphics.cam.Cam
import com.android.internal.graphics.cam.CamUtils
import com.android.systemui.log.core.Logger
import com.android.systemui.log.core.MessageBuffer
import com.android.systemui.monet.ColorScheme
import com.android.systemui.monet.TonalPalette
import java.io.IOException
import kotlin.math.abs

class AssetLoader
private constructor(
    private val pluginCtx: Context,
    private val hostCtx: Context,
    private val baseDir: String,
    private var colorScheme: ColorScheme?,
    seedColor: Int?,
    private var overrideChroma: Float?,
    private val getThemeSeedColor: (Context) -> Int,
    messageBuffer: MessageBuffer,
) {
    constructor(
        pluginCtx: Context,
        hostCtx: Context,
        baseDir: String,
        messageBuffer: MessageBuffer,
        getThemeSeedColor: ((Context) -> Int)? = null,
    ) : this(
        pluginCtx,
        hostCtx,
        baseDir,
        null,
        null,
        null,
        getThemeSeedColor ?: Companion::getThemeSeedColor,
        messageBuffer,
    )

    val logger = Logger(messageBuffer, TAG)

    var seedColor: Int? = seedColor
        private set

    var style: Int = ThemeStyle.CLOCK
        private set

    private val resources =
        listOf(
            Pair(pluginCtx.resources, pluginCtx.packageName),
            Pair(hostCtx.resources, hostCtx.packageName),
        )

    fun tryReadString(path: String?): String? = tryRead(path, ::readString)

    fun readString(path: String): String {
        if (!path.startsWith("@")) {
            logger.e("String resources should start with '@' to be localized. Using literal.")
            return path
        }

        val (res, id) =
            resolveResourceId(path) ?: throw IOException("Failed to parse string: $path")
        return res.getString(id)
    }

    fun tryReadColor(path: String?): Int? = path?.let { tryRead(it, ::readColor) }

    fun readColor(path: String): Int {
        if (path.startsWith("#")) {
            return Color.parseColor(path)
        }

        tryParseColorFromScheme(path)?.let { color ->
            logColor("ColorScheme: $path", color)
            return checkChroma(color)
        }

        resolveColorResourceId(path)?.let { (res, id, tone) ->
            val color = res.getColor(id)
            if (tone == null || TonalPalette.SHADE_KEYS.contains(tone.toInt())) {
                logColor("Resources: $path", color)
                return checkChroma(color)
            }

            val interpolatedColor =
                ColorStateList.valueOf(color).withLStar((1000f - tone) / 10f).defaultColor
            logColor("Resources (interpolated tone): $path", interpolatedColor)
            return checkChroma(interpolatedColor)
        }

        throw IOException("Failed to parse color: $path")
    }

    private fun checkChroma(color: Int): Int {
        val chroma = overrideChroma ?: return color
        val cam = Cam.fromInt(color)
        val result = ColorUtils.CAMToColor(cam.hue, chroma, CamUtils.lstarFromInt(color))
        logColor("Chroma override", result)
        return result
    }

    private fun tryParseColorFromScheme(path: String): Int? {
        val colorScheme = this.colorScheme
        if (colorScheme == null) {
            logger.w("No color scheme available")
            return null
        }

        val (pkg, type, name) = parseResourceId(path)
        if (pkg != "android" || type != "color") {
            logger.w("Failed to parse package from $path")
            return null
        }

        val parts = name.split('_')
        if (parts.size != 3) {
            logger.w("Failed to find palette and shade from $name")
            return null
        }

        val shade = parts[2]
        val palette =
            when (parts[1]) {
                "accent1" -> colorScheme.accent1
                "accent2" -> colorScheme.accent2
                "accent3" -> colorScheme.accent3
                "neutral1" -> colorScheme.neutral1
                "neutral2" -> colorScheme.neutral2
                else -> return null
            }

        if (!shade.contains("+") && !shade.contains("-")) {
            val tone = shade.toIntOrNull()
            if (tone == null) {
                logger.w("Failed to parse tone from $shade")
                return null
            }
            return palette.allShadesMapped[tone] ?: palette.getAtTone(tone.toFloat())
        }

        val relIndex = shade.indexOfLast { it == '-' || it == '+' }
        val baseTone =
            if (seedColor != null) {
                colorScheme.seedTone.toFloat()
            } else {
                shade.substring(0, relIndex).toFloatOrNull()
            }
        val relTone = shade.substring(relIndex).toFloatOrNull()
        if (baseTone == null) {
            logger.w("Failed to parse base tone from $shade")
            return null
        }
        if (relTone == null) {
            logger.w("Failed to parse relative tone from $shade")
            return null
        }
        return palette.getAtTone(baseTone + relTone)
    }

    fun readTextAsset(path: String): String {
        return pluginCtx.resources.assets.open(baseDir + path).use {
            val bytes = ByteArray(it.available())
            it.read(bytes)
            String(bytes, Charsets.UTF_8)
        }
    }

    fun tryReadDrawableAsset(path: String?): Drawable? = tryRead(path, ::readDrawableAsset)

    fun readDrawableAsset(path: String): Drawable {
        val drawable =
            if (path.startsWith("@")) {
                val (res, id) =
                    resolveResourceId(path) ?: throw IOException("Failed to parse $path to an id")
                res.getDrawable(id)
            } else {
                if (path.endsWith("xml")) {
                    throw IOException("Cannot load xml files from assets")
                }
                pluginCtx.resources.assets.open(baseDir + path).use {
                    Drawable.createFromResourceStream(pluginCtx.resources, TypedValue(), it, null)
                }
            }
        return drawable ?: throw IOException("Failed to load: $baseDir$path")
    }

    private fun parseResourceId(path: String): Triple<String?, String, String> {
        if (!path.startsWith("@")) {
            throw IOException("Invalid resource id: $path; Must start with '@'")
        }

        val parts = path.drop(1).split('/', ':')
        return when (parts.size) {
            2 -> Triple(null, parts[0], parts[1])
            3 -> Triple(parts[0], parts[1], parts[2])
            else -> throw IOException("Failed to parse resource string: $path")
        }
    }

    private fun resolveColorResourceId(path: String): Triple<Resources, Int, Float?>? {
        var (pkg, type, name) = parseResourceId(path)
        val separatorIndex = name.indexOfLast { it == '_' }
        val isRelative = name.contains("-") || name.contains("+")
        val tone: Float? =
            if (pkg != "android") {
                null
            } else if (isRelative) {
                val relIndex = name.indexOfLast { it == '-' || it == '+' }
                val baseTone = name.substring(separatorIndex + 1, relIndex).toFloatOrNull()
                val relTone = name.substring(relIndex).toFloatOrNull()
                if (baseTone == null || relTone == null) {
                    logger.w("Failed to parse relative tone from $name")
                    return null
                }
                baseTone + relTone
            } else {
                val absTone = name.substring(separatorIndex + 1).toFloatOrNull()
                if (absTone == null) {
                    logger.w("Failed to parse absolute tone from $name")
                    return null
                }
                absTone
            }

        if (tone != null && (isRelative || !TonalPalette.SHADE_KEYS.contains(tone.toInt()))) {
            val shade = TonalPalette.SHADE_KEYS.minBy { abs(it - tone) }
            val shadeName = name.substring(0, separatorIndex + 1) + shade
            logger.i("Converted $name to $shadeName")
            name = shadeName
        }

        val (res, id) = resolveResourceId(pkg, type, name) ?: return null
        return Triple(res, id, tone)
    }

    fun resolveResourceId(path: String): Pair<Resources, Int>? {
        val (pkg, type, name) = parseResourceId(path)
        return resolveResourceId(pkg, type, name)
    }

    fun resolveResourceId(pkg: String?, type: String, name: String): Pair<Resources, Int>? {
        for ((res, resPkg) in resources) {
            val id = res.getIdentifier(name, type, pkg ?: resPkg)
            if (id != 0) {
                return Pair(res, id)
            }
        }
        return null
    }

    private fun <TIn : Any, TOut> tryRead(input: TIn?, read: (TIn) -> TOut): TOut? {
        if (input == null) return null
        return try {
            read(input)
        } catch (e: IOException) {
            logger.w("Failed to read $input", e)
            null
        }
    }

    fun assetExists(path: String): Boolean {
        return try {
            if (path.startsWith("@")) {
                resolveResourceId(path) != null || resolveColorResourceId(path) != null
            } else {
                val stream = pluginCtx.resources.assets.open(baseDir + path) ?: return false
                stream.close()
                true
            }
        } catch (e: IOException) {
            false
        }
    }

    fun copy(
        pluginCtx: Context? = null,
        hostCtx: Context? = null,
        messageBuffer: MessageBuffer? = null,
    ): AssetLoader {
        return AssetLoader(
            pluginCtx ?: this.pluginCtx,
            hostCtx ?: this.hostCtx,
            baseDir,
            colorScheme,
            seedColor,
            overrideChroma,
            getThemeSeedColor,
            messageBuffer ?: logger.buffer,
        )
    }

    fun setSeedColor(seedColor: Int?, style: Int?) {
        this.seedColor = seedColor
        refreshColorPalette(style)
    }

    private fun refreshColorPalette(style: Int?) {
        this.style = style ?: ThemeStyle.CLOCK
        val seedColor =
            this.seedColor ?: getThemeSeedColor(hostCtx).also { logColor("Theme Seed Color", it) }

        val colorScheme = ColorScheme(seedColor, false, this.style)
        this.colorScheme = colorScheme

        val cam = Cam.fromInt(colorScheme.seed)
        overrideChroma = if (cam == null || cam.chroma >= 15f) null else cam.chroma * 1.5f
    }

    fun getResourcesId(name: String): Int = getResource("id", name) { _, id -> id }

    fun getString(name: String): String = getResource("string", name, Resources::getString)

    private fun <T> getResource(type: String, name: String, getter: (Resources, Int) -> T): T {
        val (res, id) =
            resolveResourceId(null, type, name)
                ?: throw Exception("Cannot find id of $name from $TAG")
        if (id == -1) {
            throw Exception("Cannot find id of $id from $TAG")
        }
        return getter(res, id)
    }

    private fun logColor(label: String, color: Int) {
        val cam = Cam.fromInt(color)
        val tone = CamUtils.lstarFromInt(color)
        logger.i("$label -> (hue: ${cam.hue}, chroma: ${cam.chroma}, tone: $tone)")
    }

    companion object {
        private val TAG = AssetLoader::class.simpleName!!

        private fun getThemeSeedColor(context: Context): Int {
            return context.resources.getColor(
                android.R.color.system_palette_key_color_primary_light
            )
        }
    }
}
