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
import android.text.format.LocalePreferences
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.android.app.animation.Interpolators
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.CustomFontTextStyle
import com.android.systemui.clocks.view.CustomDigitalTextView
import com.android.systemui.clocks.view.DigitalFaceViewGroup
import com.android.systemui.clocks.weather.weatherIconFont
import com.android.systemui.clocks.weather.weatherTextFont
import com.android.systemui.customization.clocks.utils.ViewUtils.measuredSize
import com.android.systemui.customization.clocks.utils.ViewUtils.position
import com.android.systemui.customization.clocks.view.HorizontalAlignment
import com.android.systemui.customization.clocks.view.VerticalAlignment
import com.android.systemui.plugins.keyguard.VMeasurePoint
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.VRectF
import com.android.systemui.plugins.keyguard.data.model.AlarmData
import com.android.systemui.plugins.keyguard.data.model.WeatherData
import com.android.systemui.plugins.keyguard.data.model.ZenData
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds
import java.util.Locale
import kotlin.math.roundToInt

class WeatherChildView(
    clockCtx: CustomClockContext,
    fontSpec: com.android.systemui.clocks.FontSpec,
) : CustomDigitalTextView(clockCtx, fontSpec)

abstract class WeatherClockBaseViewGroup(clockCtx: CustomClockContext) :
    DigitalFaceViewGroup(clockCtx) {
    protected val clockContext = clockCtx
    val weatherIconView =
        WeatherChildView(clockCtx, weatherIconFont).apply { id = ClockViewIds.WEATHER_CLOCK_ICON }
    val alarmDndIconView =
        WeatherChildView(clockCtx, weatherIconFont).apply {
            id = ClockViewIds.WEATHER_CLOCK_ALARM_DND
        }

    lateinit var timeView: WeatherChildView
    private var alarmData: AlarmData? = null
    private var zenData: ZenData? = null
    protected var lastWeatherData: WeatherData? = null
    protected var onClickAction: ((View) -> Unit)? = null

    init {
        addView(weatherIconView)
        addView(alarmDndIconView)
        setWillNotDraw(false)
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }

    override val hasCustomWeatherDataDisplay: Boolean
        get() = true

    protected abstract fun refreshTemperature(data: WeatherData)

    protected open fun applyChildStyles() {
        if (!::timeView.isInitialized) return
        val timeStyle = timeView.textStyle as? CustomFontTextStyle ?: return
        val timeAodStyle = timeView.aodStyle as? CustomFontTextStyle
        val iconStyle =
            timeStyle.copy(borderWidth = "0dp", fontSizeScale = timeStyle.fontSizeScale * 1.25f)
        val iconAodStyle =
            timeAodStyle?.copy(
                borderWidth = "0dp",
                fontSizeScale = timeAodStyle.fontSizeScale * 1.25f,
            )
        listOf(weatherIconView, alarmDndIconView).forEach {
            it.applyStyles(iconStyle, iconAodStyle)
        }
    }

    override fun onViewAdded(child: View?) {
        val weatherChild = child as? WeatherChildView ?: return
        super.onViewAdded(child)
        when (weatherChild.id) {
            ClockViewIds.TIME_FULL_FORMAT -> {
                weatherChild.id = ClockViewIds.WEATHER_CLOCK_TIME
                timeView = weatherChild
                applyChildStyles()
            }
            ClockViewIds.DATE_FORMAT -> {
                weatherChild.id = ClockViewIds.WEATHER_CLOCK_DATE
            }
        }
    }

    override fun onWeatherDataChanged(data: WeatherData) {
        lastWeatherData = data
        onClickAction = data.touchAction
        refreshIconViews()
        refreshTemperature(data)
        contentDescription = generateContentDescription()
    }

    override fun onAlarmDataChanged(data: AlarmData) {
        alarmData = data
        refreshIconViews()
        contentDescription = generateContentDescription()
    }

    override fun onZenDataChanged(data: ZenData) {
        zenData = data
        refreshIconViews()
        contentDescription = generateContentDescription()
    }

    override fun refreshTime() {
        children.forEach { it.refreshText() }
        invalidate()
        contentDescription = generateContentDescription()
    }

    private fun refreshIconViews() {
        weatherIconView.text = lastWeatherData?.state?.icon ?: ""
        weatherIconView.contentDescription = lastWeatherData?.description
        weatherIconView.refreshText()

        val stateText = StringBuilder()
        val stateDescription = StringBuilder()
        alarmData?.nextAlarmMillis?.let {
            stateText.append("p")
            stateDescription.append(clockContext.assets.getString(alarmData?.descriptionId))
        }
        if ((zenData?.zenMode ?: ZenData.ZenMode.OFF) != ZenData.ZenMode.OFF) {
            stateText.append("o")
            if (stateDescription.isNotEmpty()) {
                stateDescription.append(", ")
            }
            stateDescription.append(clockContext.assets.getString(zenData?.descriptionId))
        }
        alarmDndIconView.text = stateText.toString()
        alarmDndIconView.contentDescription = stateDescription.toString()
        alarmDndIconView.refreshText()
        invalidate()
    }

    private fun generateContentDescription(): String {
        return children
            .mapNotNull { child -> (child as? WeatherChildView)?.contentDescription?.toString() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }

    protected companion object {
        const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}

class WeatherClockLargeViewGroup(clockCtx: CustomClockContext) :
    WeatherClockBaseViewGroup(clockCtx) {
    val temperatureView =
        WeatherChildView(clockCtx, weatherTextFont).apply { id = ClockViewIds.WEATHER_CLOCK_TEMP }
    lateinit var dateView: WeatherChildView

    val statusBarHeight = clockContext.context.getSafeStatusBarHeight()
    private var prevWidth = -1
    private var interpolatedMeasuredWidth = -1f
    private var locale: Locale = Locale.getDefault()
    private var tempUnit: String? = null

    init {
        addView(temperatureView)
        setOnTouchListener { view, motionEvent -> handleTouch(view, motionEvent) }
    }

    override val hasCustomPositionUpdatedAnimation = true
    override val useCustomClockScene = true
    override val isAlignedWithScreen = true

    override val children: Sequence<WeatherChildView>
        get() =
            listOfNotNull(
                    if (::timeView.isInitialized) timeView else null,
                    if (::dateView.isInitialized) dateView else null,
                    weatherIconView,
                    alarmDndIconView,
                    temperatureView,
                )
                .asSequence()

    override fun onViewAdded(child: View?) {
        val weatherChild = child as? WeatherChildView ?: return
        super.onViewAdded(child)
        val id = weatherChild.id
        if (id == ClockViewIds.WEATHER_CLOCK_DATE) {
            dateView = weatherChild
            weatherChild.setSingleLine()
            weatherChild.layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        } else if (id == ClockViewIds.WEATHER_CLOCK_TIME) {
            weatherChild.setSingleLine()
            weatherChild.layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            temperatureView.verticalAlignment = VerticalAlignment.BOTTOM
            temperatureView.layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            weatherIconView.verticalAlignment = VerticalAlignment.TOP
            alarmDndIconView.verticalAlignment = VerticalAlignment.TOP
        }
    }

    private fun handleTouch(view: View, motionEvent: MotionEvent): Boolean {
        val action = onClickAction ?: return false
        if (motionEvent.action != MotionEvent.ACTION_DOWN) return false
        val pt = VPointF(motionEvent.x, motionEvent.y)
        val iconBounds = VRectF.fromTopLeft(weatherIconView.position, weatherIconView.measuredSize)
        val temperatureBounds =
            VRectF.fromTopLeft(temperatureView.position, temperatureView.measuredSize)
        if (iconBounds.contains(pt) || temperatureBounds.contains(pt)) {
            action(view)
            return true
        }
        return false
    }

    override fun applyChildStyles() {
        super.applyChildStyles()
        if (::dateView.isInitialized) {
            temperatureView.applyStyles(dateView.textStyle, dateView.aodStyle)
            temperatureView.setSingleLine()
        }
    }

    override fun calculateSize(measureSpec: VMeasurePoint): VPointF {
        return VPointF(measureSpec.width.size.toFloat(), measureSpec.height.size.toFloat())
    }

    override fun getChildFrame(child: WeatherChildView): VRectF {
        val fMax = maxOf(0f, clockContext.context.resources.displayMetrics.density / 2.625f - 1f)
        val timeHeight = if (::timeView.isInitialized) timeView.textBounds.height else 0f
        val topLeft =
            if (::dateView.isInitialized && child == dateView) {
                VPointF(
                    -((fMax + 5f) * timeHeight) - dateView.textBounds.width,
                    interpolatedMeasuredWidth * 0.04f,
                )
            } else if (child == weatherIconView) {
                val dateViewY = if (::dateView.isInitialized) dateView.position.y else 0f
                val dateViewHeight =
                    if (::dateView.isInitialized) dateView.textBounds.height else 0f
                VPointF(
                    dateViewY + dateViewHeight + weatherIconView.textBounds.width * 0.3f,
                    (fMax + 5f) * timeHeight,
                )
            } else if (child == alarmDndIconView) {
                VPointF(
                    interpolatedMeasuredWidth -
                        alarmDndIconView.measuredWidth -
                        interpolatedMeasuredWidth * 0.04f,
                    (fMax + 5f) * timeHeight,
                )
            } else if (::timeView.isInitialized && child == timeView) {
                VPointF(interpolatedMeasuredWidth * 0.04f, statusBarHeight.toFloat())
            } else if (child == temperatureView) {
                VPointF(
                    -measuredHeight + measuredHeight * 0.33f,
                    interpolatedMeasuredWidth -
                        temperatureView.measuredHeight -
                        interpolatedMeasuredWidth * 0.04f,
                )
            } else {
                VPointF.ZERO
            }
        return VRectF.fromTopLeft(topLeft, child.measuredSize)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        prevWidth = if (prevWidth == -1) measuredWidth else interpolatedMeasuredWidth.roundToInt()
        interpolatedMeasuredWidth = measuredWidth.toFloat()
        updateChildFrames(isLayout = true)
    }

    override fun onPositionAnimated(
        args: com.android.systemui.plugins.keyguard.ui.clocks.ClockPositionAnimationArgs
    ) {
        val interpolation = Interpolators.EMPHASIZED.getInterpolation(args.fraction)
        interpolatedMeasuredWidth = prevWidth * (1f - interpolation) + measuredWidth * interpolation
        updateChildFrames(isLayout = false)
        invalidate()
    }

    override fun refreshTime() {
        super.refreshTime()
        updateChildFrames(isLayout = false)
    }

    override fun onLocaleChanged(locale: Locale) {
        if (locale == this.locale) return
        this.locale = locale
        tempUnit = null
        lastWeatherData?.let { onWeatherDataChanged(it) }
    }

    override fun refreshTemperature(data: WeatherData) {
        val temperature = "${getPreferredTemperature(data)}°"
        temperatureView.text = temperature
        temperatureView.contentDescription = temperature
        temperatureView.refreshText()
    }

    private fun getPreferredTemperature(data: WeatherData): Int {
        val unit =
            tempUnit ?: LocalePreferences.getTemperatureUnit(locale, false).also { tempUnit = it }
        val celsius =
            if (data.useCelsius) data.temperature else convertFahrenheitToCelsius(data.temperature)
        return when (unit) {
            "fahrenhe" -> convertCelsiusToFahrenheit(celsius)
            "kelvin" -> convertCelsiusToKelvin(celsius)
            else -> celsius
        }
    }

    private fun convertCelsiusToFahrenheit(celsius: Int): Int =
        ((celsius * 9f / 5f) + 32f).roundToInt()

    private fun convertCelsiusToKelvin(celsius: Int): Int = (celsius - 273.15f).roundToInt()

    private fun convertFahrenheitToCelsius(fahrenheit: Int): Int =
        ((fahrenheit - 32f) * 5f / 9f).roundToInt()
}

private fun Context.getSafeStatusBarHeight(): Int {
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
}

class WeatherClockSmallViewGroup(clockCtx: CustomClockContext) :
    WeatherClockBaseViewGroup(clockCtx) {
    init {
        alarmDndIconView.visibility = View.GONE
        setOnClickListener { view -> onClickAction?.invoke(view) }
    }

    override val children: Sequence<WeatherChildView>
        get() = listOfNotNull(timeView, weatherIconView).asSequence()

    override fun applyChildStyles() {
        super.applyChildStyles()
        timeView?.apply {
            horizontalAlignment = HorizontalAlignment.LEFT
            clockVerticalAlignment = VerticalAlignment.CENTER
        }
        weatherIconView.layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, MATCH_PARENT)
        weatherIconView.clockHorizontalAlignment = HorizontalAlignment.LEFT
        weatherIconView.clockVerticalAlignment = VerticalAlignment.CENTER
    }

    override fun calculateSize(measureSpec: VMeasurePoint): VPointF {
        val iconGap =
            if (weatherIconView.measuredWidth > 0) weatherIconView.textBounds.width * 0.3f else 0f
        return VPointF(
            (timeView?.textBounds?.width ?: 0f) + iconGap + weatherIconView.measuredWidth,
            if (measureSpec.height.size != 0) {
                measureSpec.height.size.toFloat()
            } else {
                measuredHeight.toFloat()
            },
        )
    }

    override fun getChildFrame(child: WeatherChildView): VRectF {
        val iconGap = weatherIconView.measuredWidth * 0.3f
        var x = 0f
        if (child == timeView && isLayoutRtl) {
            x = weatherIconView.right + iconGap
        } else if (child == weatherIconView && !isLayoutRtl) {
            x = (timeView?.right ?: 0) + iconGap
        }
        return VRectF.fromTopLeft(
            VPointF(x, (measuredHeight - child.measuredHeight) / 2f),
            child.measuredSize,
        )
    }

    override fun refreshTemperature(data: WeatherData) = Unit
}
