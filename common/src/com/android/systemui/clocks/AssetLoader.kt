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
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.TypedValue
import com.android.internal.graphics.ColorUtils
import com.android.internal.graphics.cam.Cam
import com.android.internal.graphics.cam.CamUtils
import com.android.systemui.log.core.Logger
import com.android.systemui.log.core.MessageBuffer
import com.android.systemui.monet.ColorScheme
import java.io.IOException

class AssetLoader
private constructor(
    private val pluginCtx: Context,
    val hostCtx: Context,
    private val baseDir: String,
    private val logger: Logger,
) {
    private val resources: List<Pair<Resources, String?>> =
        listOf(
            Pair(pluginCtx.resources, pluginCtx.packageName),
            Pair(hostCtx.resources, hostCtx.packageName),
        )

    constructor(
        pluginCtx: Context,
        hostCtx: Context,
        baseDir: String,
        messageBuffer: MessageBuffer,
    ) : this(pluginCtx, hostCtx, baseDir, Logger(messageBuffer, TAG))

    var seedColor: Int? = null
        private set

    var style: Int = 8
        private set

    var colorScheme: ColorScheme? = null
        private set

    private var overrideChroma: Float? = null

    fun copy(
        pluginCtx: Context? = null,
        hostCtx: Context? = null,
        messageBuffer: MessageBuffer? = null,
    ): AssetLoader {
        val newPluginCtx = pluginCtx ?: this.pluginCtx
        val newHostCtx = hostCtx ?: this.hostCtx
        val newBuffer = messageBuffer ?: this.logger.buffer
        val copied = AssetLoader(newPluginCtx, newHostCtx, baseDir, Logger(newBuffer, TAG))
        copied.seedColor = this.seedColor
        copied.style = this.style
        copied.colorScheme = this.colorScheme
        copied.overrideChroma = this.overrideChroma
        return copied
    }

    fun setSeedColor(seedColor: Int?, colorPalette: Int? = null) {
        this.seedColor = seedColor
        this.style = colorPalette ?: this.style
        refreshColorPalette()
    }

    private fun refreshColorPalette() {
        val seed =
            seedColor
                ?: try {
                    getThemeSeedColor(hostCtx)
                } catch (e: Exception) {
                    0
                }
        if (seed != 0) {
            val scheme = ColorScheme(seed, false, style)
            this.colorScheme = scheme
            val cam = Cam.fromInt(scheme.seed)
            this.overrideChroma = if (cam != null && cam.chroma < 15f) cam.chroma * 1.5f else null
        }
    }

    private fun getThemeSeedColor(context: Context): Int {
        val resId =
            context.resources.getIdentifier(
                "system_palette_key_color_primary_light",
                "color",
                "android",
            )
        return if (resId != 0) context.getColor(resId) else 0
    }

    private fun checkChroma(color: Int): Int {
        val chroma = overrideChroma ?: return color
        val cam = Cam.fromInt(color)
        return ColorUtils.CAMToColor(cam.hue, chroma, CamUtils.lstarFromInt(color))
    }

    private fun tryParseColorFromScheme(str: String): Int? {
        val scheme = colorScheme ?: return null
        val (pkg, type, name) =
            try {
                parseResourceId(str)
            } catch (e: IOException) {
                return null
            }
        if (pkg != "android" || type != "color") return null

        val parts = name.split('_')
        if (parts.size != 3) return null

        val paletteName = parts[1]
        val shadeStr = parts[2]

        val palette =
            when (paletteName) {
                "accent1" -> scheme.accent1
                "accent2" -> scheme.accent2
                "accent3" -> scheme.accent3
                "neutral1" -> scheme.neutral1
                "neutral2" -> scheme.neutral2
                else -> return null
            }

        val relIndex = shadeStr.indexOfFirst { it == '-' || it == '+' }
        if (relIndex >= 0) {
            val base =
                if (seedColor != null) {
                    scheme.seedTone.toFloat()
                } else {
                    shadeStr.substring(0, relIndex).toFloatOrNull()
                } ?: return null
            val rel = shadeStr.substring(relIndex).toFloatOrNull() ?: return null
            return palette.getAtTone(base + rel)
        } else {
            val shade = shadeStr.toIntOrNull() ?: return null
            val mapped = palette.allShadesMapped[shade] as? Int
            return mapped ?: palette.getAtTone(shade.toFloat())
        }
    }

    fun assetExists(str: String): Boolean {
        return try {
            if (str.startsWith("@")) {
                resolveResourceId(str) != null
            } else {
                pluginCtx.resources.assets.open(baseDir + str).use { true }
            }
        } catch (e: IOException) {
            false
        }
    }

    fun readColor(str: String): Int {
        if (str.startsWith("#")) {
            return Color.parseColor(str)
        }
        val schemeColor = tryParseColorFromScheme(str)
        if (schemeColor != null) {
            return checkChroma(schemeColor)
        }
        val (resources, id, tone) =
            resolveColorResourceId(str) ?: throw IOException("Failed to parse color: $str")
        val baseColor = resources.getColor(id, null)
        val color =
            if (tone == null || tone.toInt() in SHADE_KEYS) {
                baseColor
            } else {
                ColorStateList.valueOf(baseColor).withLStar((1000f - tone) / 10f).defaultColor
            }
        return checkChroma(color)
    }

    fun readDrawableAsset(str: String): Drawable {
        val drawable =
            if (str.startsWith("@")) {
                val pair =
                    resolveResourceId(str) ?: throw IOException("Failed to parse $str to an id")
                pair.first.getDrawable(pair.second, null)
            } else {
                pluginCtx.resources.assets.open(baseDir + str).use { stream ->
                    Drawable.createFromResourceStream(
                        pluginCtx.resources,
                        TypedValue(),
                        stream,
                        null,
                    )
                }
            }
        return drawable ?: throw IOException("Failed to load: $baseDir$str")
    }

    fun readString(str: String): String {
        if (!str.startsWith("@")) {
            logger.w("String resources should start with '@' to be localized. Using literal.")
            return str
        }
        val pair = resolveResourceId(str) ?: throw IOException("Failed to parse string: $str")
        return pair.first.getString(pair.second)
    }

    fun readTextAsset(str: String): String {
        pluginCtx.resources.assets.open(baseDir + str).use { stream ->
            return stream.readBytes().toString(Charsets.UTF_8)
        }
    }

    fun tryReadColor(str: String?): Int? = str?.let { tryOrNull { readColor(it) } }

    fun tryReadDrawableAsset(str: String?): Drawable? = str?.let {
        tryOrNull { readDrawableAsset(it) }
    }

    fun tryReadString(str: String?): String? = str?.let { tryOrNull { readString(it) } }

    fun getString(str: String?): String = str?.let { tryReadString(it) } ?: ""

    private inline fun <T> tryOrNull(block: () -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveResourceId(str: String): Pair<Resources, Int>? {
        val (pkg, type, name) = parseResourceId(str)
        return resolveResourceId(pkg, type, name)
    }

    private fun resolveResourceId(
        pkg: String?,
        type: String?,
        name: String,
    ): Pair<Resources, Int>? {
        for ((resources, defaultPkg) in resources) {
            val resolvedPkg = pkg ?: defaultPkg
            val id = resources.getIdentifier(name, type, resolvedPkg)
            if (id != 0) {
                return Pair(resources, id)
            }
        }
        return null
    }

    private fun resolveColorResourceId(str: String): Triple<Resources, Int, Float>? {
        val (pkg, type, name) = parseResourceId(str)
        if (pkg != null && pkg != "android") {
            val pair = resolveResourceId(pkg, type, name) ?: return null
            return Triple(pair.first, pair.second, null)
        }

        val lastUnderscore = name.lastIndexOf('_')
        val tone =
            if (lastUnderscore < 0) {
                null
            } else {
                val suffix = name.substring(lastUnderscore + 1)
                val relIndex = suffix.indexOfFirst { it == '-' || it == '+' }
                if (relIndex >= 0) {
                    val base = suffix.substring(0, relIndex).toFloatOrNull()
                    val rel = suffix.substring(relIndex).toFloatOrNull()
                    if (base == null || rel == null) {
                        logger.w("Failed to parse relative tone from $str")
                        null
                    } else {
                        base + rel
                    }
                } else {
                    suffix.toFloatOrNull()
                }
            }

        var resolvedName = name
        if (tone != null && tone.toInt() !in SHADE_KEYS) {
            val nearest = SHADE_KEYS.minByOrNull { Math.abs(it - tone) }
            resolvedName = name.substring(0, lastUnderscore + 1) + nearest
            logger.i("Converted $str to @android:color/$resolvedName")
        }

        val pair = resolveResourceId(pkg, type, resolvedName) ?: return null
        return Triple(pair.first, pair.second, tone)
    }

    private fun parseResourceId(str: String): Triple<String?, String?, String> {
        val body = str.removePrefix("@")
        val parts = body.split(':', '/')
        return when (parts.size) {
            3 -> Triple(parts[0], parts[1], parts[2])
            2 -> Triple(null, parts[0], parts[1])
            1 -> Triple(null, null, parts[0])
            else -> throw IOException("Failed to parse resource string: $str")
        }
    }

    private companion object {
        const val TAG = "AssetLoader"
        val SHADE_KEYS = listOf(10, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000)
    }
}
