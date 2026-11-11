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
    val temperatureRange: GaugeRange = GaugeRange(minimum = 0f, maximum = 110f)
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

    fun updateRanges(pressureRange: GaugeRange, temperatureRange: GaugeRange) {
        mutableState.value = checkNotNull(mutableState.value).copy(
            pressureRange = pressureRange,
            temperatureRange = temperatureRange
        )
    }
}
