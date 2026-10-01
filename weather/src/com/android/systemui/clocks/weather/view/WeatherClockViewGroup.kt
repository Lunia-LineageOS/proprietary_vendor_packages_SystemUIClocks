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

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.graphics.withSave
import androidx.core.text.util.LocalePreferences
import com.android.app.animation.Interpolators
import com.android.systemui.animation.GSFAxes
import com.android.systemui.clocks.CustomClockContext
import com.android.systemui.clocks.FontSource
import com.android.systemui.clocks.FontSpec
import com.android.systemui.clocks.view.CustomDigitalTextView
import com.android.systemui.clocks.view.DigitalFaceViewGroup
import com.android.systemui.customization.clocks.utils.CanvasUtils.translate
import com.android.systemui.customization.clocks.utils.ContextUtils.getSafeStatusBarHeight
import com.android.systemui.customization.clocks.utils.ViewUtils.measuredSize
import com.android.systemui.customization.clocks.utils.ViewUtils.position
import com.android.systemui.customization.clocks.utils.ViewUtils.size
import com.android.systemui.customization.clocks.view.DigitalClockTextView.FontVariations
import com.android.systemui.customization.clocks.view.HorizontalAlignment
import com.android.systemui.customization.clocks.view.VerticalAlignment
import com.android.systemui.plugins.keyguard.VMeasurePoint
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.VRectF
import com.android.systemui.plugins.keyguard.data.model.AlarmData
import com.android.systemui.plugins.keyguard.data.model.WeatherData
import com.android.systemui.plugins.keyguard.data.model.ZenData
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPositionAnimationArgs
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class WeatherChildView(clockCtx: CustomClockContext, font: FontSpec) :
    CustomDigitalTextView(clockCtx, font)

@SuppressLint("ViewConstructor")
abstract class WeatherClockBaseViewGroup(private val clockCtx: CustomClockContext) :
    DigitalFaceViewGroup<WeatherChildView>(clockCtx) {
    val weatherIconView =
        WeatherChildView(clockCtx, WEATHER_ICON_FONT).apply { id = ClockViewIds.WEATHER_CLOCK_ICON }
    val alarmDndIconView =
        WeatherChildView(clockCtx, WEATHER_ICON_FONT).apply {
            id = ClockViewIds.WEATHER_CLOCK_ALARM_DND
        }
    lateinit var timeView: WeatherChildView

    protected var onClickAction: ((View) -> Unit)? = null
        private set

    private var weatherData: WeatherData? = null
    private var alarmData: AlarmData? = null
    private var zenData: ZenData? = null

    init {
        setWillNotDraw(false)
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        addView(weatherIconView)
        addView(alarmDndIconView)
    }

    private fun updateIconData(
        weatherData: WeatherData? = null,
        alarmData: AlarmData? = null,
        zenData: ZenData? = null,
    ) {
        this.weatherData = weatherData ?: this.weatherData
        this.alarmData = alarmData ?: this.alarmData
        this.zenData = zenData ?: this.zenData
        refreshIconViews()
    }

    private fun refreshIconViews() {
        weatherIconView.text = weatherData?.state?.icon ?: ""
        weatherIconView.contentDescription = weatherData?.description
        weatherIconView.refreshText()

        val icons = StringBuilder()
        val descriptions = StringBuilder()
        alarmData?.let { alarmData ->
            if (alarmData.nextAlarmMillis != null) {
                icons.append("p")
                alarmData.descriptionId?.let { descriptions.append(clockCtx.assets.getString(it)) }
            }
        }
        zenData?.let { zenData ->
            if (zenData.zenMode != ZenData.ZenMode.OFF) {
                icons.append("o")
                zenData.descriptionId?.let {
                    if (descriptions.isNotEmpty()) {
                        descriptions.append(", ")
                    }
                    descriptions.append(clockCtx.assets.getString(it))
                }
            }
        }

        alarmDndIconView.text = icons.toString()
        alarmDndIconView.contentDescription = descriptions.toString()
        alarmDndIconView.refreshText()
        invalidate()
    }

    override fun onViewAdded(child: View?) {
        if (child == null || child !is WeatherChildView) {
            logger.e({ "Unrecognized child view added: $str1" }) { str1 = "$child" }
            return
        }

        when (child.id) {
            ClockViewIds.TIME_FULL_FORMAT -> child.id = ClockViewIds.WEATHER_CLOCK_TIME
            ClockViewIds.DATE_FORMAT -> child.id = ClockViewIds.WEATHER_CLOCK_DATE
        }
        logger.onViewAdded(child)
        super.onViewAdded(child)
        if (child.id == ClockViewIds.WEATHER_CLOCK_TIME) {
            timeView = child
        }
    }

    protected open fun applyChildStyles() {
        for (iconView in sequenceOf(weatherIconView, alarmDndIconView)) {
            iconView.applyStyles(
                timeView.textStyle.copy(
                    borderWidth = "0dp",
                    fontSizeScale = timeView.textStyle.fontSizeScale * 1.25f,
                ),
                timeView.aodStyle?.copy(
                    borderWidth = "0dp",
                    fontSizeScale = (timeView.aodStyle?.fontSizeScale ?: 1f) * 1.25f,
                    transitionDuration = 700L,
                    transitionInterpolator = Interpolators.EMPHASIZED,
                ),
            )
        }
    }

    protected open fun refreshTemperature(data: WeatherData) {}

    override fun refreshTime() {
        super.refreshTime()
        invalidate()
        contentDescription = generateContentDescription()
    }

    private fun generateContentDescription(): String {
        return children.mapNotNull { it.contentDescription }.joinToString(" ")
    }

    override fun onWeatherDataChanged(data: WeatherData) {
        logger.i({ "onWeatherDataChanged -> $str1" }) { str1 = "$data" }
        onClickAction = data.touchAction
        updateIconData(weatherData = data)
        refreshTemperature(data)
        contentDescription = generateContentDescription()
    }

    override fun onAlarmDataChanged(data: AlarmData) {
        logger.i({ "onAlarmDataChanged -> $str1" }) { str1 = "$data" }
        updateIconData(alarmData = data)
        contentDescription = generateContentDescription()
    }

    override fun onZenDataChanged(data: ZenData) {
        logger.i({ "onZenDataChanged -> $str1" }) { str1 = "$data" }
        updateIconData(zenData = data)
        contentDescription = generateContentDescription()
    }

    companion object {
        private val WEATHER_ICON_FONT =
            FontSpec(
                FontSource.AssetFontSource("FrameWeatherVF.ttf"),
                FontVariations("'${GSFAxes.WEIGHT.tag}' 1000", "'${GSFAxes.WEIGHT.tag}' 0"),
            )
        val WEATHER_TEXT_FONT =
            FontSpec(
                FontSource.AssetFontSource("GoogleSansFlex-Custom-wght94-420.ttf"),
                FontVariations("'${GSFAxes.WEIGHT.tag}' 420", "'${GSFAxes.WEIGHT.tag}' 94"),
            )
    }
}

@SuppressLint("ViewConstructor")
class WeatherClockLargeViewGroup(clockCtx: CustomClockContext) :
    WeatherClockBaseViewGroup(clockCtx) {
    val temperatureView =
        WeatherChildView(clockCtx, WEATHER_TEXT_FONT).apply { id = ClockViewIds.WEATHER_CLOCK_TEMP }
    lateinit var dateView: WeatherChildView
    val statusBarHeight = context.getSafeStatusBarHeight()
    private var prevWidth = -1
    private var locale: Locale = Locale.getDefault()
    private var interpolatedMeasuredWidth = -1f
    private var _tempUnit: String? = null

    override val hasCustomWeatherDataDisplay: Boolean
        get() = true

    override val isAlignedWithScreen: Boolean
        get() = true

    override var hasCustomPositionUpdatedAnimation = true
        private set

    override val useCustomClockScene = true

    private val onTouchListener = View.OnTouchListener { view, event ->
        val onClickAction = onClickAction
        if (onClickAction != null && event.action == MotionEvent.ACTION_DOWN) {
            val touchPoint = VPointF(event.x, event.y)
            val temperatureBounds = temperatureView.let {
                VRectF(
                    it.position.y,
                    -it.position.x - it.width,
                    it.position.y + it.height,
                    -it.position.x,
                )
            }
            val weatherIconBounds = weatherIconView.let { VRectF.fromTopLeft(it.position, it.size) }
            if (temperatureBounds.contains(touchPoint) || weatherIconBounds.contains(touchPoint)) {
                onClickAction(view)
                return@OnTouchListener true
            }
        }
        false
    }

    init {
        addView(temperatureView)
        setOnTouchListener(onTouchListener)
    }

    override val children: Sequence<WeatherChildView>
        get() = sequenceOf(timeView, dateView, weatherIconView, alarmDndIconView, temperatureView)

    override fun onLocaleChanged(locale: Locale) {
        if (this.locale == locale) return
        this.locale = locale
        _tempUnit = null
    }

    private val temperatureUnit: String
        get() =
            _tempUnit ?: LocalePreferences.getTemperatureUnit(locale, false).also { _tempUnit = it }

    override fun onViewAdded(child: View?) {
        if (child == null || child !is WeatherChildView) return

        logger.onViewAdded(child)
        super.onViewAdded(child)
        when (child.id) {
            ClockViewIds.WEATHER_CLOCK_DATE -> {
                dateView = child
                child.setSingleLine()
                child.layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            }
            ClockViewIds.WEATHER_CLOCK_TIME -> {
                child.setSingleLine()
                child.layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                temperatureView.verticalAlignment = VerticalAlignment.BOTTOM
                temperatureView.layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                weatherIconView.verticalAlignment = VerticalAlignment.TOP
                weatherIconView.layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                alarmDndIconView.verticalAlignment = VerticalAlignment.TOP
                alarmDndIconView.horizontalAlignment = HorizontalAlignment.END
                alarmDndIconView.layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            }
        }
        child.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        if (childCount >= 5) {
            applyChildStyles()
        }
    }

    override fun applyChildStyles() {
        super.applyChildStyles()
        temperatureView.applyStyles(dateView.textStyle, dateView.aodStyle)
        temperatureView.setSingleLine()
    }

    override fun refreshTemperature(data: WeatherData) {
        temperatureView.text = "${getPreferredTemperature(data)}°"
        temperatureView.contentDescription = temperatureView.text
        temperatureView.refreshText()
    }

    override fun calculateSize(measureSpec: VMeasurePoint): VPointF = measureSpec.size.toVPointF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        prevWidth = if (prevWidth == -1) measuredWidth else interpolatedMeasuredWidth.roundToInt()
        interpolatedMeasuredWidth = measuredWidth.toFloat()
        updateChildFrames(isLayout = true)
    }

    override fun refreshTime() {
        super.refreshTime()
        updateChildFrames(isLayout = false)
    }

    override fun getChildFrame(child: WeatherChildView): VRectF {
        val densityOffset = max(0f, context.resources.displayMetrics.density / 2.625f - 1f)
        val position =
            when (child) {
                dateView ->
                    VPointF(
                        -(densityOffset + 5f) * timeView.textBounds.height -
                            dateView.textBounds.width,
                        interpolatedMeasuredWidth * 0.04f,
                    )
                weatherIconView ->
                    VPointF(
                        dateView.position.y +
                            dateView.textBounds.height +
                            weatherIconView.textBounds.width * 0.3f,
                        (densityOffset + 5f) * timeView.textBounds.height,
                    )
                alarmDndIconView ->
                    VPointF(
                        interpolatedMeasuredWidth -
                            alarmDndIconView.measuredWidth -
                            interpolatedMeasuredWidth * 0.04f,
                        (densityOffset + 5f) * timeView.textBounds.height,
                    )
                timeView -> VPointF(interpolatedMeasuredWidth * 0.04f, statusBarHeight)
                temperatureView ->
                    VPointF(
                        -measuredHeight + measuredHeight * 0.33f,
                        interpolatedMeasuredWidth -
                            temperatureView.measuredHeight -
                            interpolatedMeasuredWidth * 0.04f,
                    )
                else -> VPointF.ZERO
            }
        return VRectF.fromTopLeft(position, child.measuredSize)
    }

    override fun onPositionAnimated(anim: ClockPositionAnimationArgs) {
        val fraction = Interpolators.EMPHASIZED.getInterpolation(anim.fraction)
        interpolatedMeasuredWidth = prevWidth * (1f - fraction) + measuredWidth * fraction
        updateChildFrames(isLayout = false)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (child in children) {
            canvas.withSave {
                if (child.isVertical) {
                    rotate(-90f)
                }
                translate(child.position)
                child.draw(this)
            }
        }
    }

    private fun getPreferredTemperature(data: WeatherData): Int {
        val unit = temperatureUnit
        val temperature = getPreferredTemperature(data, unit)
        logger.i({ "$str1 got $int1 from $str2" }) {
            str1 = unit
            int1 = temperature
            str2 = data.toString()
        }
        return temperature
    }

    companion object {
        fun getPreferredTemperature(data: WeatherData, unit: String): Int {
            return when (unit) {
                LocalePreferences.TemperatureUnit.CELSIUS ->
                    convertTempUnit(data.temperature, data.useCelsius, true)
                LocalePreferences.TemperatureUnit.FAHRENHEIT ->
                    convertTempUnit(data.temperature, data.useCelsius, false)
                LocalePreferences.TemperatureUnit.KELVIN ->
                    convertCelsiusToKelvin(convertTempUnit(data.temperature, data.useCelsius, true))
                LocalePreferences.TemperatureUnit.DEFAULT -> data.temperature
                else -> data.temperature
            }
        }

        private fun convertTempUnit(temperature: Int, isCelsius: Boolean, toCelsius: Boolean): Int {
            if (isCelsius == toCelsius) return temperature
            return if (isCelsius) {
                convertCelsiusToFahrenheit(temperature)
            } else {
                convertFahrenheitToCelsius(temperature)
            }
        }

        private fun convertFahrenheitToCelsius(temperature: Int): Int {
            return ((temperature - 32f) * 5f / 9f).roundToInt()
        }

        private fun convertCelsiusToFahrenheit(temperature: Int): Int {
            return (temperature * 9f / 5f + 32f).roundToInt()
        }

        private fun convertCelsiusToKelvin(temperature: Int): Int {
            return (temperature - 273.15).roundToInt()
        }
    }
}

@SuppressLint("ViewConstructor")
class WeatherClockSmallViewGroup(clockCtx: CustomClockContext) :
    WeatherClockBaseViewGroup(clockCtx) {
    init {
        setOnClickListener { view -> onClickAction?.invoke(view) }
        alarmDndIconView.visibility = GONE
    }

    override val hasCustomWeatherDataDisplay = true

    override val children: Sequence<WeatherChildView>
        get() = sequenceOf(timeView, weatherIconView)

    override fun calculateSize(measureSpec: VMeasurePoint): VPointF {
        val iconGap =
            if (weatherIconView.measuredWidth > 0) weatherIconView.textBounds.width * 0.3f else 0f
        return VPointF(
            timeView.textBounds.width + iconGap + weatherIconView.measuredWidth,
            measureSpec.height.size,
        )
    }

    override fun getChildFrame(child: WeatherChildView): VRectF {
        val iconGap = weatherIconView.measuredWidth * 0.3f
        val x =
            when {
                child == timeView -> if (isLayoutRtl) weatherIconView.right + iconGap else 0f
                child == weatherIconView && !isLayoutRtl -> timeView.right + iconGap
                else -> 0f
            }
        return VRectF.fromTopLeft(
            VPointF(x, (measuredHeight - child.measuredHeight) / 2f),
            child.measuredSize,
        )
    }

    override fun onViewAdded(child: View?) {
        if (child == null) return
        logger.onViewAdded(child)
        super.onViewAdded(child)
        if (child.id == ClockViewIds.WEATHER_CLOCK_TIME) {
            child.layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, MATCH_PARENT)
        }
        if (childCount >= 3) {
            applyChildStyles()
        }
    }

    override fun applyChildStyles() {
        super.applyChildStyles()
        timeView.horizontalAlignment = HorizontalAlignment.START
        timeView.verticalAlignment = VerticalAlignment.CENTER
        weatherIconView.layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, MATCH_PARENT)
        weatherIconView.horizontalAlignment = HorizontalAlignment.START
        weatherIconView.verticalAlignment = VerticalAlignment.CENTER
    }
}
