package com.example.exigence

import kotlin.math.roundToInt
import kotlin.random.Random

internal class TireDemoSimulator(private val random: Random = Random.Default) {
    val updateIntervalMillis = 1_000L
    private val pressureDrift = 0.1f
    private val temperatureDrift = 10f
    private val pressureRange = GaugeRange(1.5f, 2.4f)
    private val temperatureRange = GaugeRange(0f, 110f)

    fun nextReading(previous: TireReading): TireReading {
        return TireReading(
            pressureBar = driftValue(previous.pressureBar, pressureRange, pressureDrift),
            temperatureCelsius = driftValue(previous.temperatureCelsius, temperatureRange, temperatureDrift)
        )
    }

    private fun driftValue(value: Float?, range: GaugeRange, drift: Float): Float {
        val stepsPerUnit = 1f / drift
        val minimumStep = (range.minimum * stepsPerUnit).roundToInt()
        val maximumStep = (range.maximum * stepsPerUnit).roundToInt()
        val currentValue = value?.takeIf { it.isFinite() } ?: ((range.minimum + range.maximum) / 2f)
        val currentStep = (currentValue.coerceIn(range.minimum, range.maximum) * stepsPerUnit)
            .roundToInt().coerceIn(minimumStep, maximumStep)
        val nextStep = when (currentStep) {
            minimumStep -> currentStep + 1
            maximumStep -> currentStep - 1
            else -> currentStep + if (random.nextBoolean()) 1 else -1
        }
        return nextStep / stepsPerUnit
    }
}
