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

package com.android.systemui.clocks.metro.view

import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.RenderMode
import com.android.systemui.clocks.AssetLoader

class TransitClockLottieAssets {
    private var colon: LottieDrawable? = null
    private var numbers: List<LottieDrawable> = emptyList()

    fun getColon(assetLoader: AssetLoader, asset: String): LottieDrawable? {
        if (colon == null) {
            val composition = readLottieComposition(assetLoader, asset) ?: return null
            colon = LottieDrawable().apply { setComposition(composition) }
        }
        return colon
    }

    fun getNumbersList(assetLoader: AssetLoader, assets: List<String>): List<LottieDrawable> {
        if (numbers.isEmpty()) {
            numbers = assets.map { asset ->
                LottieDrawable().apply {
                    readLottieComposition(assetLoader, asset)?.let { setComposition(it) }
                    setRenderMode(RenderMode.HARDWARE)
                }
            }
        }
        return numbers
    }
}
