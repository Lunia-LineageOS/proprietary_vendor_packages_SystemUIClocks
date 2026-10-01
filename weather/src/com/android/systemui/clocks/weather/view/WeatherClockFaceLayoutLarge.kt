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

package com.android.systemui.clocks.weather.view

import android.content.Context
import android.content.res.Resources
import android.view.View
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.dimensionResource
import androidx.constraintlayout.widget.ConstraintSet
import com.android.compose.animation.scene.ElementContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.MovableElementContentScope
import com.android.compose.animation.scene.MovableElementKey
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.weather.R
import com.android.systemui.customization.clocks.utils.ContextUtils
import com.android.systemui.customization.clocks.view.DefaultClockFaceLayout
import com.android.systemui.customization.clocks.view.HorizontalAlignment
import com.android.systemui.customization.clocks.view.VerticalAlignment
import com.android.systemui.plugins.keyguard.ui.clocks.AodClockBurnInModel
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceLayout
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPreviewConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds
import com.android.systemui.plugins.keyguard.ui.composable.elements.BaseLockscreenElement
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElement
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElementKeys
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenScope
import com.android.systemui.plugins.keyguard.ui.composable.elements.MovableLockscreenElement

object WeatherClockElementKeys {
    val Time = MovableElementKey("WeatherClockTime", LockscreenElementKeys.ContentPicker)
    val Date = MovableElementKey("WeatherClockDate", LockscreenElementKeys.ContentPicker)
    val StateIcon = MovableElementKey("WeatherClockStateIcon", LockscreenElementKeys.ContentPicker)
    val Temperature =
        MovableElementKey("WeatherClockTemperature", LockscreenElementKeys.ContentPicker)
    val AlarmDND = MovableElementKey("WeatherClockAlarmDND", LockscreenElementKeys.ContentPicker)
}

class WeatherClockFaceLayoutLarge(
    private val clockCtx: CustomClockContext,
    private val view: WeatherClockLargeViewGroup,
) : ClockFaceLayout {
    private val resources: Resources = clockCtx.resources
    private val smartspaceHeight: Int =
        resources.getDimensionPixelSize(R.dimen.enhanced_smartspace_height)
    private val smartspaceBottomMargin: Int =
        resources.getDimensionPixelSize(R.dimen.weather_clock_smartspace_bottom_margin)

    init {
        val verticalAlignment = VerticalAlignment.TOP
        val horizontalAlignmentStart = HorizontalAlignment.START
        val horizontalAlignmentEnd = HorizontalAlignment.END

        view.timeView.verticalAlignment = verticalAlignment
        view.timeView.horizontalAlignment = horizontalAlignmentStart

        view.dateView.isVertical = true
        view.dateView.verticalAlignment = verticalAlignment
        view.dateView.horizontalAlignment = horizontalAlignmentStart

        view.temperatureView.isVertical = true
        view.temperatureView.verticalAlignment = verticalAlignment
        view.temperatureView.horizontalAlignment = horizontalAlignmentEnd

        view.weatherIconView.verticalAlignment = verticalAlignment
        view.weatherIconView.horizontalAlignment = horizontalAlignmentStart

        view.alarmDndIconView.verticalAlignment = verticalAlignment
        view.alarmDndIconView.horizontalAlignment = horizontalAlignmentEnd
    }

    override val views: List<View> by lazy {
        view.setWillNotDraw(true)
        view.children
            .map {
                view.removeView(it)
                it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                it.setWillNotDraw(false)
                it
            }
            .toList()
    }

    override val elements: List<BaseLockscreenElement> by lazy {
        view.setWillNotDraw(true)
        for (child in view.children) {
            view.removeView(child)
            child.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            child.setWillNotDraw(false)
        }
        view.alarmDndIconView.isSingleLine = true
        listOf(
            WeatherElement(WeatherClockElementKeys.Time, view.timeView) {
                Modifier.padding(start = dimensionResource(R.dimen.weather_time_start_padding))
            },
            WeatherElement(WeatherClockElementKeys.Date, view.dateView) {
                Modifier.padding(start = dimensionResource(R.dimen.weather_date_start_padding))
            },
            WeatherElement(WeatherClockElementKeys.StateIcon, view.weatherIconView) {
                Modifier.padding(start = dimensionResource(R.dimen.weather_date_icon_padding))
            },
            WeatherElement(WeatherClockElementKeys.AlarmDND, view.alarmDndIconView) {
                Modifier.padding(end = dimensionResource(R.dimen.weather_alarm_end_padding))
            },
            WeatherElement(WeatherClockElementKeys.Temperature, view.temperatureView) {
                Modifier.padding(end = dimensionResource(R.dimen.weather_temp_end_padding))
            },
            LargeWeatherRegionElement(),
        )
    }

    private inner class WeatherElement(
        override val key: MovableElementKey,
        private val targetView: View,
        private val modifier: @Composable MovableElementContentScope.() -> Modifier,
    ) : MovableLockscreenElement {
        override val source = BaseLockscreenElement.ElementSource.DYNAMIC
        override val context: Context = view.context

        @Composable
        override fun LockscreenScope<MovableElementContentScope>.LockscreenElement() {
            DefaultClockFaceLayout.ClockView(
                targetView,
                nonAuthUI(burnInAware(contentScope.modifier(), isClock = true)),
            )
        }
    }

    private inner class LargeWeatherRegionElement : LockscreenElement {
        override val key: ElementKey = LockscreenElementKeys.Region.Clock.Large
        override val source = BaseLockscreenElement.ElementSource.DYNAMIC
        override val context: Context = view.context

        @Composable
        override fun LockscreenScope<ElementContentScope>.LockscreenElement() {
            Layout(
                content = {
                    LockscreenElement(WeatherClockElementKeys.Time)
                    LockscreenElement(WeatherClockElementKeys.Date)
                    LockscreenElement(WeatherClockElementKeys.StateIcon)
                    LockscreenElement(WeatherClockElementKeys.AlarmDND)
                    LockscreenElement(WeatherClockElementKeys.Temperature)
                    LockscreenElement(
                        LockscreenElementKeys.Smartspace.Cards,
                        Modifier.heightIn(
                            max = dimensionResource(R.dimen.enhanced_smartspace_height)
                        ),
                    )
                }
            ) { measurables, constraints ->
                check(measurables.size == 6)
                val unconstrained = constraints.copy(minWidth = 0, minHeight = 0)
                val timePlaceable = measurables[0].measure(unconstrained)
                val datePlaceable = measurables[1].measure(unconstrained)
                val stateIconPlaceable = measurables[2].measure(unconstrained)
                val alarmDndPlaceable = measurables[3].measure(unconstrained)
                val tempPlaceable = measurables[4].measure(unconstrained)
                val smartspacePlaceable = measurables[5].measure(unconstrained)

                layout(constraints.maxWidth, constraints.maxHeight) {
                    timePlaceable.placeRelative(0, 0)
                    smartspacePlaceable.placeRelative(0, timePlaceable.height)
                    val dateRowY = timePlaceable.height + smartspacePlaceable.height
                    datePlaceable.placeRelative(0, dateRowY)
                    stateIconPlaceable.placeRelative(datePlaceable.width, dateRowY)
                    alarmDndPlaceable.placeRelative(
                        constraints.maxWidth - alarmDndPlaceable.width,
                        dateRowY,
                    )
                    tempPlaceable.placeRelative(
                        constraints.maxWidth - tempPlaceable.width,
                        dateRowY + datePlaceable.height - tempPlaceable.height,
                    )
                }
            }
        }
    }

    override fun applyConstraints(constraints: ConstraintSet): ConstraintSet {
        val clockPaddingStart = resources.getDimensionPixelSize(R.dimen.clock_padding_start)
        val constraint = constraints.getConstraint(ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE)
        val endToEnd = constraint?.layout?.endToEnd ?: -1
        val dateIconPadding = resources.getDimensionPixelSize(R.dimen.weather_date_icon_padding)

        constraints.constrainWidth(ClockViewIds.WEATHER_CLOCK_TIME, ConstraintSet.WRAP_CONTENT)
        constraints.constrainHeight(ClockViewIds.WEATHER_CLOCK_TIME, ConstraintSet.WRAP_CONTENT)
        constraints.connect(
            ClockViewIds.WEATHER_CLOCK_TIME,
            ConstraintSet.TOP,
            0,
            ConstraintSet.TOP,
            view.statusBarHeight,
        )
        constraints.connect(
            ClockViewIds.WEATHER_CLOCK_TIME,
            ConstraintSet.START,
            0,
            ConstraintSet.START,
            clockPaddingStart,
        )

        constraints.connect(
            clockCtx.assets.getResourcesId("bc_smartspace_view"),
            ConstraintSet.TOP,
            ClockViewIds.WEATHER_CLOCK_TIME,
            ConstraintSet.BOTTOM,
        )

        constraints.constrainWidth(ClockViewIds.WEATHER_CLOCK_DATE, ConstraintSet.WRAP_CONTENT)
        constraints.constrainHeight(ClockViewIds.WEATHER_CLOCK_DATE, ConstraintSet.WRAP_CONTENT)
        constraints.connect(
            ClockViewIds.WEATHER_CLOCK_DATE,
            ConstraintSet.TOP,
            ClockViewIds.WEATHER_CLOCK_DATE_BARRIER_BOTTOM,
            ConstraintSet.BOTTOM,
            smartspaceBottomMargin,
        )
        constraints.connect(
            ClockViewIds.WEATHER_CLOCK_DATE,
            ConstraintSet.START,
            0,
            ConstraintSet.START,
            clockPaddingStart,
        )

        constraints.constrainWidth(view.weatherIconView.id, ConstraintSet.WRAP_CONTENT)
        constraints.constrainHeight(view.weatherIconView.id, ConstraintSet.WRAP_CONTENT)
        constraints.connect(
            view.weatherIconView.id,
            ConstraintSet.START,
            ClockViewIds.WEATHER_CLOCK_DATE,
            ConstraintSet.END,
            dateIconPadding,
        )
        constraints.connect(
            view.weatherIconView.id,
            ConstraintSet.TOP,
            ClockViewIds.WEATHER_CLOCK_DATE,
            ConstraintSet.TOP,
        )

        constraints.constrainWidth(view.alarmDndIconView.id, ConstraintSet.WRAP_CONTENT)
        constraints.constrainHeight(view.alarmDndIconView.id, ConstraintSet.WRAP_CONTENT)
        constraints.connect(
            view.alarmDndIconView.id,
            ConstraintSet.END,
            view.temperatureView.id,
            ConstraintSet.END,
        )
        constraints.connect(
            view.alarmDndIconView.id,
            ConstraintSet.TOP,
            ClockViewIds.WEATHER_CLOCK_DATE,
            ConstraintSet.TOP,
        )
        constraints.setGoneMargin(
            view.alarmDndIconView.id,
            ConstraintSet.TOP,
            smartspaceHeight + smartspaceBottomMargin,
        )

        constraints.constrainWidth(view.temperatureView.id, ConstraintSet.WRAP_CONTENT)
        constraints.constrainHeight(view.temperatureView.id, ConstraintSet.WRAP_CONTENT)
        constraints.connect(
            view.temperatureView.id,
            ConstraintSet.BOTTOM,
            ClockViewIds.WEATHER_CLOCK_DATE,
            ConstraintSet.BOTTOM,
        )
        val endTarget = if (endToEnd == -1) 0 else endToEnd
        constraints.connect(
            view.temperatureView.id,
            ConstraintSet.END,
            endTarget,
            ConstraintSet.END,
            clockPaddingStart,
        )

        return constraints
    }

    override fun applyPreviewConstraints(
        clockPreviewConfig: ClockPreviewConfig,
        constraints: ConstraintSet,
    ): ConstraintSet {
        val clockPaddingStart = resources.getDimensionPixelSize(R.dimen.clock_padding_start)
        val dateIconPadding = resources.getDimensionPixelSize(R.dimen.weather_date_icon_padding)

        constraints.constrainWidth(ClockViewIds.WEATHER_CLOCK_TIME, ConstraintSet.WRAP_CONTENT)
        constraints.constrainHeight(ClockViewIds.WEATHER_CLOCK_TIME, ConstraintSet.WRAP_CONTENT)
        constraints.connect(
            ClockViewIds.WEATHER_CLOCK_TIME,
            ConstraintSet.TOP,
            0,
            ConstraintSet.TOP,
            ContextUtils.getSafeStatusBarHeight(clockCtx.context),
        )
        constraints.connect(
            ClockViewIds.WEATHER_CLOCK_TIME,
            ConstraintSet.START,
            0,
            ConstraintSet.START,
            clockPaddingStart,
        )

        constraints.constrainWidth(ClockViewIds.WEATHER_CLOCK_DATE, ConstraintSet.WRAP_CONTENT)
        constraints.constrainHeight(ClockViewIds.WEATHER_CLOCK_DATE, ConstraintSet.WRAP_CONTENT)
        constraints.connect(
            ClockViewIds.WEATHER_CLOCK_DATE,
            ConstraintSet.TOP,
            ClockViewIds.WEATHER_CLOCK_TIME,
            ConstraintSet.BOTTOM,
            resources.getDimensionPixelSize(R.dimen.weather_clock_smartspace_bottom_margin) +
                resources.getDimensionPixelSize(R.dimen.enhanced_smartspace_height),
        )
        constraints.connect(
            ClockViewIds.WEATHER_CLOCK_DATE,
            ConstraintSet.START,
            0,
            ConstraintSet.START,
            clockPaddingStart,
        )

        constraints.constrainWidth(view.weatherIconView.id, ConstraintSet.WRAP_CONTENT)
        constraints.constrainHeight(view.weatherIconView.id, ConstraintSet.WRAP_CONTENT)
        constraints.connect(
            view.weatherIconView.id,
            ConstraintSet.START,
            ClockViewIds.WEATHER_CLOCK_DATE,
            ConstraintSet.END,
            dateIconPadding,
        )
        constraints.connect(
            view.weatherIconView.id,
            ConstraintSet.TOP,
            ClockViewIds.WEATHER_CLOCK_DATE,
            ConstraintSet.TOP,
        )

        constraints.constrainWidth(view.temperatureView.id, ConstraintSet.WRAP_CONTENT)
        constraints.constrainHeight(view.temperatureView.id, ConstraintSet.WRAP_CONTENT)
        constraints.connect(
            view.temperatureView.id,
            ConstraintSet.BOTTOM,
            ClockViewIds.WEATHER_CLOCK_DATE,
            ConstraintSet.BOTTOM,
        )
        constraints.connect(
            view.temperatureView.id,
            ConstraintSet.END,
            0,
            ConstraintSet.END,
            clockPaddingStart,
        )

        return constraints
    }

    override fun applyExternalDisplayPresentationConstraints(
        constraints: ConstraintSet
    ): ConstraintSet {
        val clockPaddingStart = resources.getDimensionPixelSize(R.dimen.clock_padding_start)
        val dateIconPadding = resources.getDimensionPixelSize(R.dimen.weather_date_icon_padding)

        constraints.constrainWidth(ClockViewIds.WEATHER_CLOCK_TIME, ConstraintSet.WRAP_CONTENT)
        constraints.constrainHeight(ClockViewIds.WEATHER_CLOCK_TIME, ConstraintSet.WRAP_CONTENT)
        constraints.connect(
            ClockViewIds.WEATHER_CLOCK_TIME,
            ConstraintSet.TOP,
            0,
            ConstraintSet.TOP,
            clockPaddingStart,
        )
        constraints.connect(
            ClockViewIds.WEATHER_CLOCK_TIME,
            ConstraintSet.START,
            0,
            ConstraintSet.START,
            clockPaddingStart,
        )

        constraints.constrainWidth(ClockViewIds.WEATHER_CLOCK_DATE, ConstraintSet.WRAP_CONTENT)
        constraints.constrainHeight(ClockViewIds.WEATHER_CLOCK_DATE, ConstraintSet.WRAP_CONTENT)
        constraints.connect(
            ClockViewIds.WEATHER_CLOCK_DATE,
            ConstraintSet.TOP,
            ClockViewIds.WEATHER_CLOCK_TIME,
            ConstraintSet.BOTTOM,
            clockPaddingStart + resources.getDimensionPixelSize(R.dimen.enhanced_smartspace_height),
        )
        constraints.connect(
            ClockViewIds.WEATHER_CLOCK_DATE,
            ConstraintSet.START,
            0,
            ConstraintSet.START,
            clockPaddingStart,
        )

        constraints.constrainWidth(view.weatherIconView.id, ConstraintSet.WRAP_CONTENT)
        constraints.constrainHeight(view.weatherIconView.id, ConstraintSet.WRAP_CONTENT)
        constraints.connect(
            view.weatherIconView.id,
            ConstraintSet.START,
            ClockViewIds.WEATHER_CLOCK_DATE,
            ConstraintSet.END,
            dateIconPadding,
        )
        constraints.connect(
            view.weatherIconView.id,
            ConstraintSet.TOP,
            ClockViewIds.WEATHER_CLOCK_DATE,
            ConstraintSet.TOP,
        )

        constraints.constrainWidth(view.temperatureView.id, ConstraintSet.WRAP_CONTENT)
        constraints.constrainHeight(view.temperatureView.id, ConstraintSet.WRAP_CONTENT)
        constraints.connect(
            view.temperatureView.id,
            ConstraintSet.BOTTOM,
            ClockViewIds.WEATHER_CLOCK_DATE,
            ConstraintSet.BOTTOM,
        )
        constraints.connect(
            view.temperatureView.id,
            ConstraintSet.END,
            0,
            ConstraintSet.END,
            clockPaddingStart,
        )

        return constraints
    }

    override fun applyAodBurnIn(aodBurnInModel: AodClockBurnInModel) {
        view.timeView.translationX = aodBurnInModel.translationX
        view.timeView.translationY = aodBurnInModel.translationY
        view.dateView.translationX = aodBurnInModel.translationX
        view.dateView.translationY = aodBurnInModel.translationY
        view.weatherIconView.translationX = aodBurnInModel.translationX
        view.weatherIconView.translationY = aodBurnInModel.translationY
        view.temperatureView.translationX = -aodBurnInModel.translationX
        view.temperatureView.translationY = aodBurnInModel.translationY
        view.alarmDndIconView.translationX = -aodBurnInModel.translationX
        view.alarmDndIconView.translationY = aodBurnInModel.translationY
    }
}
