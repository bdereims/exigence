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
    private val lowPressureThresholdBar = 1.7f
    private val highPressureThresholdBar = 2.1f
    private var pressureColor = Color.WHITE
    private val temperatureStopsCelsius = floatArrayOf(0f, 40f, 60f, 110f)
    private val temperatureColors = intArrayOf(
        Color.rgb(0, 0, 139), Color.GREEN, Color.GREEN, Color.RED
    )
    private val temperatureGradient = LinearGradient(
        temperatureStopsCelsius.first(), 0f, temperatureStopsCelsius.last(), 0f,
        temperatureColors,
        FloatArray(temperatureStopsCelsius.size) { stopIndex ->
            (temperatureStopsCelsius[stopIndex] - temperatureStopsCelsius.first()) /
                (temperatureStopsCelsius.last() - temperatureStopsCelsius.first())
        },
        Shader.TileMode.CLAMP
    )
    private val temperatureGradientMatrix = Matrix()
    private var temperatureColor = Color.GREEN
    private val primaryTextColor = Color.rgb(245, 247, 250)
    private val secondaryTextColor = Color.rgb(152, 167, 181)
    private var tireLabel = ""
    private var reading = TireReading()
    private var pressureRange = GaugeRange(0f, 3f)
    private var temperatureRange = GaugeRange(0f, 110f)
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

    fun bind(label: String, tireReading: TireReading, state: TireDashboardState) {
        tireLabel = label
        reading = tireReading
        pressureRange = state.pressureRange
        temperatureRange = state.temperatureRange
        pressureColor = colorForPressure(reading.pressureBar)
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
            formatScaleValue(temperatureRange.minimum)
        )
        maximumTemperatureText = context.getString(
            R.string.temperature_value,
            formatScaleValue(temperatureRange.maximum)
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
        val temperatureFraction = temperatureRange.fraction(reading.temperatureCelsius)
        val temperatureCelsius = reading.temperatureCelsius
        if (temperatureCelsius != null && temperatureCelsius.isFinite()) {
            val pixelsPerDegree = (trackEnd - trackStart) /
                (temperatureRange.maximum - temperatureRange.minimum)
            temperatureGradientMatrix.setScale(pixelsPerDegree, 1f)
            temperatureGradientMatrix.postTranslate(
                trackStart - temperatureRange.minimum * pixelsPerDegree, 0f
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

    private fun colorForPressure(pressureBar: Float?): Int {
        return when {
            pressureBar == null || !pressureBar.isFinite() -> secondaryTextColor
            pressureBar < lowPressureThresholdBar -> Color.rgb(100, 181, 246)
            pressureBar > highPressureThresholdBar -> Color.RED
            else -> Color.WHITE
        }
    }

    private fun colorForTemperature(temperatureCelsius: Float?): Int {
        if (temperatureCelsius == null || !temperatureCelsius.isFinite()) return secondaryTextColor
        if (temperatureCelsius <= temperatureStopsCelsius.first()) return temperatureColors.first()
        for (stopIndex in 1 until temperatureStopsCelsius.size) {
            if (temperatureCelsius <= temperatureStopsCelsius[stopIndex]) {
                val fraction = (temperatureCelsius - temperatureStopsCelsius[stopIndex - 1]) /
                    (temperatureStopsCelsius[stopIndex] - temperatureStopsCelsius[stopIndex - 1])
                return ColorUtils.blendARGB(
                    temperatureColors[stopIndex - 1], temperatureColors[stopIndex], fraction
                )
            }
        }
        return temperatureColors.last()
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
