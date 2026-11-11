package com.example.exigence

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class TirePosition {
    FRONT_LEFT,
    FRONT_RIGHT,
    REAR_LEFT,
    REAR_RIGHT
}

data class TireReading(
    val pressureBar: Float? = 2.0f,
    val temperatureCelsius: Float? = 40f
)

data class GaugeRange(val minimum: Float, val maximum: Float) {
    init {
        require(minimum.isFinite() && maximum.isFinite() && maximum > minimum)
    }

    fun fraction(value: Float?): Float {
        if (value == null || !value.isFinite()) return 0f
        return ((value - minimum) / (maximum - minimum)).coerceIn(0f, 1f)
    }
}

data class TireDashboardState(
    val readings: Map<TirePosition, TireReading> = TirePosition.entries.associateWith { TireReading() },
    val pressureRange: GaugeRange = GaugeRange(minimum = 0f, maximum = 3f),
    val frontTemperatureColorRange: GaugeRange = GaugeRange(minimum = 0f, maximum = 110f),
    val rearTemperatureColorRange: GaugeRange = GaugeRange(minimum = 0f, maximum = 110f),
    val frontLowPressureThresholdBar: Float = 1.75f,
    val frontHighPressureThresholdBar: Float = 2.25f,
    val rearLowPressureThresholdBar: Float = 1.75f,
    val rearHighPressureThresholdBar: Float = 2.25f
)

class TireDashboardViewModel : ViewModel() {
    private val mutableState = MutableLiveData(TireDashboardState())
    val state: LiveData<TireDashboardState> = mutableState
    private val demoSimulator = TireDemoSimulator()

    suspend fun collectTires() {
        while (currentCoroutineContext().isActive) {
            delay(demoSimulator.updateIntervalMillis)
            val currentState = checkNotNull(mutableState.value)
            mutableState.value = currentState.copy(
                readings = currentState.readings.mapValues { (_, reading) ->
                    demoSimulator.nextReading(reading)
                }
            )
        }
    }

    fun updateTire(position: TirePosition, pressureBar: Float?, temperatureCelsius: Float?) {
        val currentState = checkNotNull(mutableState.value)
        mutableState.value = currentState.copy(
            readings = currentState.readings + (position to TireReading(pressureBar, temperatureCelsius))
        )
    }

    fun updatePressureRange(pressureRange: GaugeRange) {
        mutableState.value = checkNotNull(mutableState.value).copy(pressureRange = pressureRange)
    }

    fun updateTemperatureColorRanges(frontMin: Float, frontMax: Float, rearMin: Float, rearMax: Float) {
        require(frontMin.isFinite() && frontMax.isFinite() && frontMax > frontMin)
        require(rearMin.isFinite() && rearMax.isFinite() && rearMax > rearMin)
        mutableState.value = checkNotNull(mutableState.value).copy(
            frontTemperatureColorRange = GaugeRange(frontMin, frontMax),
            rearTemperatureColorRange = GaugeRange(rearMin, rearMax)
        )
    }

    fun updatePressureThresholds(
        frontLow: Float,
        frontHigh: Float,
        rearLow: Float,
        rearHigh: Float
    ) {
        require(frontLow.isFinite() && frontHigh.isFinite() && frontLow < frontHigh)
        require(rearLow.isFinite() && rearHigh.isFinite() && rearLow < rearHigh)
        mutableState.value = checkNotNull(mutableState.value).copy(
            frontLowPressureThresholdBar = frontLow,
            frontHighPressureThresholdBar = frontHigh,
            rearLowPressureThresholdBar = rearLow,
            rearHighPressureThresholdBar = rearHigh
        )
    }
}
