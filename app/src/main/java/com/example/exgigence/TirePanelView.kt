package com.example.exigence

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import java.util.Locale
import kotlin.math.min

class TirePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gaugePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val drawingBounds = RectF()
    private val panelColor = Color.rgb(17, 23, 31)
    private val trackColor = Color.rgb(42, 55, 69)
    private var lowPressureThresholdBar = 1.7f
    private var highPressureThresholdBar = 2.1f
    private var pressureColor = Color.WHITE
    private val temperatureColors = intArrayOf(
        Color.rgb(0, 0, 139), Color.GREEN, Color.GREEN, Color.RED
    )
    private var temperatureLowColorThresholdCelsius = 40f
    private var temperatureHighColorThresholdCelsius = 60f
    private var temperatureGradient = createTemperatureGradient(40f, 60f)
    private val temperatureGradientMatrix = Matrix()
    private var temperatureColor = Color.GREEN
    private val primaryTextColor = Color.rgb(245, 247, 250)
    private val secondaryTextColor = Color.rgb(152, 167, 181)
    private var tireLabel = ""
    private var reading = TireReading()
    private var pressureRange = GaugeRange(0f, 3f)
    private val temperatureScale = GaugeRange(0f, 110f)
    private var pressureText = ""
    private var pressureDisplayText = ""
    private var temperatureText = ""
    private var minimumPressureText = ""
    private var maximumPressureText = ""
    private var minimumTemperatureText = ""
    private var maximumTemperatureText = ""

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isFocusable = true
    }

    fun bind(
        position: TirePosition,
        label: String,
        tireReading: TireReading,
        state: TireDashboardState
    ) {
        tireLabel = label
        reading = tireReading
        pressureRange = state.pressureRange
        val isFrontTire = position == TirePosition.FRONT_LEFT || position == TirePosition.FRONT_RIGHT
        val temperatureColorRange = if (isFrontTire) {
            state.frontTemperatureColorRange
        } else {
            state.rearTemperatureColorRange
        }
        temperatureLowColorThresholdCelsius = temperatureColorRange.minimum
        temperatureHighColorThresholdCelsius = temperatureColorRange.maximum
        temperatureGradient = createTemperatureGradient(
            temperatureLowColorThresholdCelsius,
            temperatureHighColorThresholdCelsius
        )
        lowPressureThresholdBar = if (isFrontTire) {
            state.frontLowPressureThresholdBar
        } else {
            state.rearLowPressureThresholdBar
        }
        highPressureThresholdBar = if (isFrontTire) {
            state.frontHighPressureThresholdBar
        } else {
            state.rearHighPressureThresholdBar
        }
        pressureColor = colorForPressure(reading.pressureBar, reading.temperatureCelsius)
        temperatureColor = colorForTemperature(reading.temperatureCelsius)
        pressureText = formatReading(reading.pressureBar, 1)
        pressureDisplayText = context.getString(R.string.pressure_value, pressureText)
        temperatureText = context.getString(
            R.string.temperature_value,
            formatReading(reading.temperatureCelsius, 1)
        )
        minimumPressureText = formatScaleValue(pressureRange.minimum)
        maximumPressureText = formatScaleValue(pressureRange.maximum)
        minimumTemperatureText = context.getString(
            R.string.temperature_value,
            formatScaleValue(temperatureScale.minimum)
        )
        maximumTemperatureText = context.getString(
            R.string.temperature_value,
            formatScaleValue(temperatureScale.maximum)
        )
        contentDescription = context.getString(
            R.string.tire_panel_description,
            tireLabel,
            pressureText,
            minimumPressureText,
            maximumPressureText,
            temperatureText,
            minimumTemperatureText,
            maximumTemperatureText
        )
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val panelWidth = width.toFloat()
        val panelHeight = height.toFloat()
        val isWidePanel = panelWidth > panelHeight * 1.4f
        val unit = if (isWidePanel) {
            min(panelWidth / 320f, panelHeight / 180f)
        } else {
            min(panelWidth / 200f, panelHeight / 280f)
        }
        if (unit <= 0f) return

        backgroundPaint.color = panelColor
        drawingBounds.set(0f, 0f, panelWidth, panelHeight)
        canvas.drawRoundRect(drawingBounds, 14f * unit, 14f * unit, backgroundPaint)

        val contentLeft = 16f * unit
        val contentRight = panelWidth - contentLeft
        val contentWidth = contentRight - contentLeft
        drawText(
            canvas, tireLabel, contentLeft, 24f * unit, 16f * unit,
            primaryTextColor, Paint.Align.LEFT, contentWidth, true
        )

        val temperatureTop = panelHeight - 64f * unit
        val pressureTop = 42f * unit
        val pressureBottom = temperatureTop - 8f * unit
        drawPressureText(
            canvas, panelWidth / 2f, (pressureTop + pressureBottom) / 2f,
            contentWidth, pressureBottom - pressureTop
        )
        drawTemperatureGauge(canvas, contentLeft, contentRight, temperatureTop, unit)
    }

    private fun drawPressureText(
        canvas: Canvas,
        centerHorizontal: Float,
        centerVertical: Float,
        availableWidth: Float,
        availableHeight: Float
    ) {
        drawText(
            canvas, pressureDisplayText, centerHorizontal, centerVertical,
            min(availableWidth * 0.3f, availableHeight * 0.95f), pressureColor,
            maximumWidth = availableWidth, bold = true, maximumHeight = availableHeight
        )
    }

    private fun drawTemperatureGauge(
        canvas: Canvas,
        contentLeft: Float,
        contentRight: Float,
        temperatureTop: Float,
        unit: Float
    ) {
        gaugePaint.color = trackColor
        gaugePaint.strokeWidth = unit
        canvas.drawLine(contentLeft, temperatureTop, contentRight, temperatureTop, gaugePaint)

        val contentWidth = contentRight - contentLeft
        drawText(
            canvas, context.getString(R.string.temperature_label), contentLeft,
            temperatureTop + 16f * unit, 11f * unit, secondaryTextColor,
            Paint.Align.LEFT, contentWidth * 0.58f
        )
        drawText(
            canvas, temperatureText, contentRight, temperatureTop + 16f * unit,
            18f * unit, temperatureColor, Paint.Align.RIGHT, contentWidth * 0.4f, true
        )

        val trackStart = contentLeft + 3f * unit
        val trackEnd = contentRight - 3f * unit
        val trackVertical = temperatureTop + 34f * unit
        gaugePaint.strokeWidth = 5f * unit
        gaugePaint.color = trackColor
        canvas.drawLine(trackStart, trackVertical, trackEnd, trackVertical, gaugePaint)
        val temperatureFraction = temperatureScale.fraction(reading.temperatureCelsius)
        val temperatureCelsius = reading.temperatureCelsius
        if (temperatureCelsius != null && temperatureCelsius.isFinite()) {
            val pixelsPerDegree = (trackEnd - trackStart) /
                (temperatureScale.maximum - temperatureScale.minimum)
            temperatureGradientMatrix.setScale(pixelsPerDegree, 1f)
            temperatureGradientMatrix.postTranslate(
                trackStart - temperatureScale.minimum * pixelsPerDegree, 0f
            )
            temperatureGradient.setLocalMatrix(temperatureGradientMatrix)
            gaugePaint.shader = temperatureGradient
            if (temperatureFraction > 0f) {
                canvas.drawLine(
                    trackStart, trackVertical,
                    trackStart + (trackEnd - trackStart) * temperatureFraction,
                    trackVertical, gaugePaint
                )
            } else {
                canvas.drawPoint(trackStart, trackVertical, gaugePaint)
            }
            gaugePaint.shader = null
        }
        drawText(
            canvas, minimumTemperatureText, contentLeft, temperatureTop + 51f * unit,
            11f * unit, secondaryTextColor, Paint.Align.LEFT, contentWidth / 2f
        )
        drawText(
            canvas, maximumTemperatureText, contentRight, temperatureTop + 51f * unit,
            11f * unit, secondaryTextColor, Paint.Align.RIGHT, contentWidth / 2f
        )
    }

    private fun colorForPressure(pressureBar: Float?, temperatureCelsius: Float?): Int {
        return when {
            pressureBar == null || !pressureBar.isFinite() -> secondaryTextColor
            pressureBar < lowPressureThresholdBar -> Color.rgb(100, 181, 246)
            pressureBar > highPressureThresholdBar -> Color.RED
            temperatureCelsius != null &&
                temperatureCelsius.isFinite() &&
                temperatureCelsius in temperatureLowColorThresholdCelsius..temperatureHighColorThresholdCelsius ->
                Color.GREEN
            else -> Color.WHITE
        }
    }

    private fun createTemperatureGradient(lowThreshold: Float, highThreshold: Float): LinearGradient {
        val lowPosition = (lowThreshold / 110f).coerceIn(0.0001f, 0.9998f)
        val highPosition = (highThreshold / 110f).coerceIn(lowPosition + 0.0001f, 0.9999f)
        val positions = floatArrayOf(0f, lowPosition, highPosition, 1f)
        return LinearGradient(
            0f, 0f, 110f, 0f,
            temperatureColors,
            positions,
            Shader.TileMode.CLAMP
        )
    }

    private fun colorForTemperature(temperatureCelsius: Float?): Int {
        if (temperatureCelsius == null || !temperatureCelsius.isFinite()) return secondaryTextColor
        val temperature = temperatureCelsius.coerceIn(0f, 110f)
        return when {
            temperature < temperatureLowColorThresholdCelsius -> {
                val fraction = if (temperatureLowColorThresholdCelsius == 0f) {
                    1f
                } else {
                    temperature / temperatureLowColorThresholdCelsius
                }
                ColorUtils.blendARGB(temperatureColors.first(), Color.GREEN, fraction)
            }
            temperature <= temperatureHighColorThresholdCelsius -> Color.GREEN
            else -> {
                val fraction = if (temperatureHighColorThresholdCelsius == 110f) {
                    0f
                } else {
                    (temperature - temperatureHighColorThresholdCelsius) /
                        (110f - temperatureHighColorThresholdCelsius)
                }
                ColorUtils.blendARGB(Color.GREEN, temperatureColors.last(), fraction)
            }
        }
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        horizontal: Float,
        vertical: Float,
        size: Float,
        color: Int,
        alignment: Paint.Align = Paint.Align.CENTER,
        maximumWidth: Float = Float.MAX_VALUE,
        bold: Boolean = false,
        maximumHeight: Float = Float.MAX_VALUE
    ) {
        textPaint.color = color
        textPaint.textAlign = alignment
        textPaint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        textPaint.textSize = size * resources.configuration.fontScale
        val textWidth = textPaint.measureText(text)
        if (textWidth > maximumWidth && textWidth > 0f) {
            textPaint.textSize *= maximumWidth / textWidth
        }
        val textHeight = textPaint.descent() - textPaint.ascent()
        if (textHeight > maximumHeight && textHeight > 0f) {
            textPaint.textSize *= maximumHeight / textHeight
        }
        val baseline = vertical - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(text, horizontal, baseline, textPaint)
    }

    private fun formatScaleValue(value: Float): String {
        return formatReading(value, if (value % 1f == 0f) 0 else 1)
    }

    private fun formatReading(value: Float?, decimals: Int): String {
        return if (value != null && value.isFinite()) {
            String.format(Locale.getDefault(), "%.${decimals}f", value)
        } else {
            context.getString(R.string.reading_unavailable)
        }
    }
}
