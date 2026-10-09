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
import android.view.View
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.dimensionResource
import androidx.constraintlayout.widget.ConstraintSet
import com.android.compose.animation.scene.ElementContentScope
import com.android.compose.animation.scene.MovableElementContentScope
import com.android.compose.animation.scene.MovableElementKey
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.weather.R
import com.android.systemui.customization.clocks.utils.ContextUtils.getSafeStatusBarHeight
import com.android.systemui.customization.clocks.view.DefaultClockFaceLayout
import com.android.systemui.customization.clocks.view.HorizontalAlignment
import com.android.systemui.customization.clocks.view.VerticalAlignment
import com.android.systemui.plugins.keyguard.ui.clocks.AodClockBurnInModel
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceLayout
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPreviewConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds
import com.android.systemui.plugins.keyguard.ui.composable.elements.BaseLockscreenElement
import com.android.systemui.plugins.keyguard.ui.composable.elements.BaseLockscreenElement.ElementSource
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElement
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElementKeys
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenScope
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenScope.Companion.LockscreenElement
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
    private val resources = clockCtx.resources
    private val smartspaceHeight =
        resources.getDimensionPixelSize(R.dimen.enhanced_smartspace_height)
    private val smartspaceBottomMargin =
        resources.getDimensionPixelSize(R.dimen.weather_clock_smartspace_bottom_margin)

    init {
        view.timeView.apply {
            verticalAlignment = VerticalAlignment.TOP
            horizontalAlignment = HorizontalAlignment.START
        }
        view.dateView.apply {
            isVertical = true
            verticalAlignment = VerticalAlignment.TOP
            horizontalAlignment = HorizontalAlignment.START
        }
        view.temperatureView.apply {
            isVertical = true
            verticalAlignment = VerticalAlignment.TOP
            horizontalAlignment = HorizontalAlignment.END
        }
        view.weatherIconView.apply {
            verticalAlignment = VerticalAlignment.TOP
            horizontalAlignment = HorizontalAlignment.START
        }
        view.alarmDndIconView.apply {
            verticalAlignment = VerticalAlignment.TOP
            horizontalAlignment = HorizontalAlignment.END
        }
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
        view.alarmDndIconView.setSingleLine(true)

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
        override val context: Context = view.context
        override val source = ElementSource.DYNAMIC

        @Composable
        override fun LockscreenScope<MovableElementContentScope>.LockscreenElement() {
            DefaultClockFaceLayout.ClockView(
                targetView,
                contentScope.modifier().burnInAware(isClock = true).nonAuthUI(),
            )
        }
    }

    private inner class LargeWeatherRegionElement : LockscreenElement {
        override val key = LockscreenElementKeys.Region.Clock.Large
        override val context: Context = view.context
        override val source = ElementSource.DYNAMIC

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
                            min = dimensionResource(R.dimen.enhanced_smartspace_height)
                        ),
                    )
                }
            ) { measurables, constraints ->
                check(measurables.size == 6)
                val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
                val time = measurables[0].measure(childConstraints)
                val date = measurables[1].measure(childConstraints)
                val stateIcon = measurables[2].measure(childConstraints)
                val alarmDnd = measurables[3].measure(childConstraints)
                val temperature = measurables[4].measure(childConstraints)
                val smartspace = measurables[5].measure(childConstraints)

                layout(constraints.maxWidth, constraints.maxHeight) {
                    time.placeRelative(0, 0)
                    smartspace.placeRelative(0, time.measuredHeight)
                    val dateTop = time.measuredHeight + smartspace.measuredHeight
                    date.placeRelative(0, dateTop)
                    stateIcon.placeRelative(date.measuredWidth, dateTop)
                    alarmDnd.placeRelative(constraints.maxWidth - alarmDnd.measuredWidth, dateTop)
                    temperature.placeRelative(
                        constraints.maxWidth - temperature.measuredWidth,
                        dateTop + date.measuredHeight - temperature.measuredHeight,
                    )
                }
            }
        }
    }

    override fun applyConstraints(constraints: ConstraintSet): ConstraintSet {
        val clockPaddingStart = resources.getDimensionPixelSize(R.dimen.clock_padding_start)
        val largeClockEnd =
            constraints.getConstraint(ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE)?.layout?.endToEnd
                ?: -1
        val dateIconPadding = resources.getDimensionPixelSize(R.dimen.weather_date_icon_padding)
        val weatherIconId = view.weatherIconView.id
        val alarmDndId = view.alarmDndIconView.id
        val temperatureId = view.temperatureView.id

        return constraints.apply {
            constrainWidth(ClockViewIds.WEATHER_CLOCK_TIME, ConstraintSet.WRAP_CONTENT)
            constrainHeight(ClockViewIds.WEATHER_CLOCK_TIME, ConstraintSet.WRAP_CONTENT)
            connect(
                ClockViewIds.WEATHER_CLOCK_TIME,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
                view.statusBarHeight,
            )
            connect(
                ClockViewIds.WEATHER_CLOCK_TIME,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                clockPaddingStart,
            )
            connect(
                clockCtx.assets.getResourcesId("bc_smartspace_view"),
                ConstraintSet.TOP,
                ClockViewIds.WEATHER_CLOCK_TIME,
                ConstraintSet.BOTTOM,
            )

            constrainWidth(ClockViewIds.WEATHER_CLOCK_DATE, ConstraintSet.WRAP_CONTENT)
            constrainHeight(ClockViewIds.WEATHER_CLOCK_DATE, ConstraintSet.WRAP_CONTENT)
            connect(
                ClockViewIds.WEATHER_CLOCK_DATE,
                ConstraintSet.TOP,
                ClockViewIds.WEATHER_CLOCK_DATE_BARRIER_BOTTOM,
                ConstraintSet.BOTTOM,
                smartspaceBottomMargin,
            )
            connect(
                ClockViewIds.WEATHER_CLOCK_DATE,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                clockPaddingStart,
            )

            constrainWidth(weatherIconId, ConstraintSet.WRAP_CONTENT)
            constrainHeight(weatherIconId, ConstraintSet.WRAP_CONTENT)
            connect(
                weatherIconId,
                ConstraintSet.START,
                ClockViewIds.WEATHER_CLOCK_DATE,
                ConstraintSet.END,
                dateIconPadding,
            )
            connect(
                weatherIconId,
                ConstraintSet.TOP,
                ClockViewIds.WEATHER_CLOCK_DATE,
                ConstraintSet.TOP,
            )

            constrainWidth(alarmDndId, ConstraintSet.WRAP_CONTENT)
            constrainHeight(alarmDndId, ConstraintSet.WRAP_CONTENT)
            connect(alarmDndId, ConstraintSet.END, temperatureId, ConstraintSet.END)
            connect(
                alarmDndId,
                ConstraintSet.TOP,
                ClockViewIds.WEATHER_CLOCK_DATE,
                ConstraintSet.TOP,
            )
            setGoneMargin(alarmDndId, ConstraintSet.TOP, smartspaceHeight + smartspaceBottomMargin)

            constrainWidth(temperatureId, ConstraintSet.WRAP_CONTENT)
            constrainHeight(temperatureId, ConstraintSet.WRAP_CONTENT)
            connect(
                temperatureId,
                ConstraintSet.BOTTOM,
                ClockViewIds.WEATHER_CLOCK_DATE,
                ConstraintSet.BOTTOM,
            )
            connect(
                temperatureId,
                ConstraintSet.END,
                if (largeClockEnd == -1) ConstraintSet.PARENT_ID else largeClockEnd,
                ConstraintSet.END,
                clockPaddingStart,
            )
        }
    }

    override fun applyPreviewConstraints(
        clockPreviewConfig: ClockPreviewConfig,
        constraints: ConstraintSet,
    ): ConstraintSet {
        val clockPaddingStart = resources.getDimensionPixelSize(R.dimen.clock_padding_start)
        val dateIconPadding = resources.getDimensionPixelSize(R.dimen.weather_date_icon_padding)
        val weatherIconId = view.weatherIconView.id
        val alarmDndId = view.alarmDndIconView.id
        val temperatureId = view.temperatureView.id

        return constraints.apply {
            constrainWidth(ClockViewIds.WEATHER_CLOCK_TIME, ConstraintSet.WRAP_CONTENT)
            constrainHeight(ClockViewIds.WEATHER_CLOCK_TIME, ConstraintSet.WRAP_CONTENT)
            connect(
                ClockViewIds.WEATHER_CLOCK_TIME,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
                clockCtx.context.getSafeStatusBarHeight(),
            )
            connect(
                ClockViewIds.WEATHER_CLOCK_TIME,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                clockPaddingStart,
            )

            constrainWidth(ClockViewIds.WEATHER_CLOCK_DATE, ConstraintSet.WRAP_CONTENT)
            constrainHeight(ClockViewIds.WEATHER_CLOCK_DATE, ConstraintSet.WRAP_CONTENT)
            connect(
                ClockViewIds.WEATHER_CLOCK_DATE,
                ConstraintSet.TOP,
                ClockViewIds.WEATHER_CLOCK_TIME,
                ConstraintSet.BOTTOM,
                resources.getDimensionPixelSize(R.dimen.weather_clock_smartspace_bottom_margin) +
                    resources.getDimensionPixelSize(R.dimen.enhanced_smartspace_height),
            )
            connect(
                ClockViewIds.WEATHER_CLOCK_DATE,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                clockPaddingStart,
            )

            constrainWidth(weatherIconId, ConstraintSet.WRAP_CONTENT)
            constrainHeight(weatherIconId, ConstraintSet.WRAP_CONTENT)
            connect(
                weatherIconId,
                ConstraintSet.START,
                ClockViewIds.WEATHER_CLOCK_DATE,
                ConstraintSet.END,
                dateIconPadding,
            )
            connect(
                weatherIconId,
                ConstraintSet.TOP,
                ClockViewIds.WEATHER_CLOCK_DATE,
                ConstraintSet.TOP,
            )

            constrainWidth(alarmDndId, ConstraintSet.WRAP_CONTENT)
            constrainHeight(alarmDndId, ConstraintSet.WRAP_CONTENT)
            connect(alarmDndId, ConstraintSet.END, temperatureId, ConstraintSet.END)
            connect(
                alarmDndId,
                ConstraintSet.TOP,
                ClockViewIds.WEATHER_CLOCK_DATE,
                ConstraintSet.TOP,
            )

            constrainWidth(temperatureId, ConstraintSet.WRAP_CONTENT)
            constrainHeight(temperatureId, ConstraintSet.WRAP_CONTENT)
            connect(
                temperatureId,
                ConstraintSet.BOTTOM,
                ClockViewIds.WEATHER_CLOCK_DATE,
                ConstraintSet.BOTTOM,
            )
            connect(
                temperatureId,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
                clockPaddingStart,
            )
        }
    }

    override fun applyExternalDisplayPresentationConstraints(
        constraints: ConstraintSet
    ): ConstraintSet {
        val clockPaddingStart = resources.getDimensionPixelSize(R.dimen.clock_padding_start)
        val dateIconPadding = resources.getDimensionPixelSize(R.dimen.weather_date_icon_padding)
        val weatherIconId = view.weatherIconView.id
        val temperatureId = view.temperatureView.id

        return constraints.apply {
            constrainWidth(ClockViewIds.WEATHER_CLOCK_TIME, ConstraintSet.WRAP_CONTENT)
            constrainHeight(ClockViewIds.WEATHER_CLOCK_TIME, ConstraintSet.WRAP_CONTENT)
            connect(
                ClockViewIds.WEATHER_CLOCK_TIME,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
                clockPaddingStart,
            )
            connect(
                ClockViewIds.WEATHER_CLOCK_TIME,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                clockPaddingStart,
            )

            constrainWidth(ClockViewIds.WEATHER_CLOCK_DATE, ConstraintSet.WRAP_CONTENT)
            constrainHeight(ClockViewIds.WEATHER_CLOCK_DATE, ConstraintSet.WRAP_CONTENT)
            connect(
                ClockViewIds.WEATHER_CLOCK_DATE,
                ConstraintSet.TOP,
                ClockViewIds.WEATHER_CLOCK_TIME,
                ConstraintSet.BOTTOM,
                clockPaddingStart +
                    resources.getDimensionPixelSize(R.dimen.enhanced_smartspace_height),
            )
            connect(
                ClockViewIds.WEATHER_CLOCK_DATE,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                clockPaddingStart,
            )

            constrainWidth(weatherIconId, ConstraintSet.WRAP_CONTENT)
            constrainHeight(weatherIconId, ConstraintSet.WRAP_CONTENT)
            connect(
                weatherIconId,
                ConstraintSet.START,
                ClockViewIds.WEATHER_CLOCK_DATE,
                ConstraintSet.END,
                dateIconPadding,
            )
            connect(
                weatherIconId,
                ConstraintSet.TOP,
                ClockViewIds.WEATHER_CLOCK_DATE,
                ConstraintSet.TOP,
            )

            constrainWidth(temperatureId, ConstraintSet.WRAP_CONTENT)
            constrainHeight(temperatureId, ConstraintSet.WRAP_CONTENT)
            connect(
                temperatureId,
                ConstraintSet.BOTTOM,
                ClockViewIds.WEATHER_CLOCK_DATE,
                ConstraintSet.BOTTOM,
            )
            connect(
                temperatureId,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
                clockPaddingStart,
            )
        }
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
