package com.example.exigence

import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TireDemoSimulatorTest {
    @Test
    fun repeatedUpdatesStayWithinLimitsAndUseIndependentDriftSizes() {
        val simulator = TireDemoSimulator(Random(42))
        var reading = TireReading()
        val observedTemperatures = mutableSetOf(checkNotNull(reading.temperatureCelsius))

        repeat(10_000) {
            val nextReading = simulator.nextReading(reading)
            val pressure = checkNotNull(nextReading.pressureBar)
            val temperature = checkNotNull(nextReading.temperatureCelsius)
            assertTrue(pressure in 1.7f..2.2f)
            assertTrue(temperature in 0f..110f)
            assertEquals(0.1f, abs(pressure - checkNotNull(reading.pressureBar)), 0.0001f)
            assertEquals(10f, abs(temperature - checkNotNull(reading.temperatureCelsius)), 0.0001f)
            observedTemperatures.add(temperature)
            reading = nextReading
        }
        assertTrue(observedTemperatures.containsAll(setOf(0f, 110f)))
    }

    @Test
    fun readingsAtLimitsDriftBackInsideTheRange() {
        val simulator = TireDemoSimulator(Random(42))
        val minimumReading = simulator.nextReading(TireReading(1.7f, 0f))
        val maximumReading = simulator.nextReading(TireReading(2.2f, 110f))

        assertEquals(1.8f, checkNotNull(minimumReading.pressureBar), 0.0001f)
        assertEquals(10f, checkNotNull(minimumReading.temperatureCelsius), 0.0001f)
        assertEquals(2.1f, checkNotNull(maximumReading.pressureBar), 0.0001f)
        assertEquals(100f, checkNotNull(maximumReading.temperatureCelsius), 0.0001f)
    }

    @Test
    fun unavailableAndOutOfRangeReadingsRecoverToValidDemoValues() {
        val simulator = TireDemoSimulator(Random(42))
        val invalidReadings = listOf(
            TireReading(null, null),
            TireReading(Float.NaN, Float.POSITIVE_INFINITY),
            TireReading(0f, 200f)
        )

        invalidReadings.forEach { reading ->
            val nextReading = simulator.nextReading(reading)
            assertTrue(checkNotNull(nextReading.pressureBar) in 1.7f..2.2f)
            assertTrue(checkNotNull(nextReading.temperatureCelsius) in 0f..110f)
        }
    }
}
