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

package com.android.systemui.clocks.view

import com.android.systemui.customization.clocks.TextStyle
import com.android.systemui.customization.clocks.view.HorizontalAlignment
import com.android.systemui.customization.clocks.view.IDigitalClockTextView
import com.android.systemui.customization.clocks.view.VerticalAlignment
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig

interface ICustomDigitalTextView : IDigitalClockTextView {
    var verticalAlignment: VerticalAlignment
    var horizontalAlignment: HorizontalAlignment

    fun applyStyles(textStyle: TextStyle, aodStyle: TextStyle?)

    fun applyTextSize(targetFontSizePx: Float?, constrainedByHeight: Boolean)

    fun animateDoze(isDozing: Boolean, isAnimated: Boolean)

    fun updateTheme(theme: ThemeConfig)
}
