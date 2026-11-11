package com.example.exigence

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.slider.RangeSlider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private val dashboard by lazy { ViewModelProvider(this)[TireDashboardViewModel::class.java] }
    private var tireCollection: Job? = null

    override fun onStart() {
        super.onStart()
        tireCollection = lifecycleScope.launch { dashboard.collectTires() }
    }

    override fun onStop() {
        tireCollection?.cancel()
        tireCollection = null
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        val root = findViewById<FrameLayout>(R.id.root_container)
        val mainContent = findViewById<View>(R.id.main_content)
        val settingsScreen = findViewById<View>(R.id.settings_screen)
        val preferences = getSharedPreferences("tire_pressure_settings", MODE_PRIVATE)
        val frontRangeSlider = findViewById<RangeSlider>(R.id.front_threshold_slider)
        val rearRangeSlider = findViewById<RangeSlider>(R.id.rear_threshold_slider)
        val frontTemperatureSlider = findViewById<RangeSlider>(R.id.front_temperature_slider)
        val rearTemperatureSlider = findViewById<RangeSlider>(R.id.rear_temperature_slider)

        fun quantizePressure(value: Float): Float =
            1f + ((value - 1f) / 0.05f).roundToInt().coerceIn(0, 40) * 0.05f

        fun loadThresholdPair(
            lowKey: String,
            highKey: String,
            oldLowKey: String? = null,
            oldHighKey: String? = null
        ): Pair<Float, Float> {
            val low = preferences.getFloat(
                lowKey,
                oldLowKey?.let { preferences.getFloat(it, 1.75f) } ?: 1.75f
            ).coerceIn(1f, 3f)
            val high = preferences.getFloat(
                highKey,
                oldHighKey?.let { preferences.getFloat(it, 2.25f) } ?: 2.25f
            ).coerceIn(1f, 3f)
            val quantizedLow = quantizePressure(low)
            val quantizedHigh = quantizePressure(high)
            return if (quantizedLow < quantizedHigh) {
                quantizedLow to quantizedHigh
            } else {
                1.75f to 2.25f
            }
        }

        val (initialFrontLow, initialFrontHigh) = loadThresholdPair(
            "front_low_threshold_bar",
            "front_high_threshold_bar",
            oldLowKey = "low_threshold_bar",
            oldHighKey = "high_threshold_bar"
        )
        val (initialRearLow, initialRearHigh) = loadThresholdPair(
            "rear_low_threshold_bar",
            "rear_high_threshold_bar"
        )
        var frontLow = initialFrontLow
        var frontHigh = initialFrontHigh
        var rearLow = initialRearLow
        var rearHigh = initialRearHigh
        var frontTemperatureMin = preferences.getFloat("front_temperature_min_c", 0f).roundToInt().coerceIn(0, 109).toFloat()
        var frontTemperatureMax = preferences.getFloat("front_temperature_max_c", 110f).roundToInt().coerceIn(1, 110).toFloat()
        var rearTemperatureMin = preferences.getFloat("rear_temperature_min_c", 0f).roundToInt().coerceIn(0, 109).toFloat()
        var rearTemperatureMax = preferences.getFloat("rear_temperature_max_c", 110f).roundToInt().coerceIn(1, 110).toFloat()
        if (frontTemperatureMin >= frontTemperatureMax) {
            frontTemperatureMin = 0f
            frontTemperatureMax = 110f
        }
        if (rearTemperatureMin >= rearTemperatureMax) {
            rearTemperatureMin = 0f
            rearTemperatureMax = 110f
        }
        frontRangeSlider.values = listOf(frontLow, frontHigh)
        rearRangeSlider.values = listOf(rearLow, rearHigh)
        frontTemperatureSlider.values = listOf(frontTemperatureMin, frontTemperatureMax)
        rearTemperatureSlider.values = listOf(rearTemperatureMin, rearTemperatureMax)

        val frontRangeLabel = findViewById<android.widget.TextView>(R.id.front_threshold_label)
        val rearRangeLabel = findViewById<android.widget.TextView>(R.id.rear_threshold_label)
        val frontTemperatureLabel =
            findViewById<android.widget.TextView>(R.id.front_temperature_range_label)
        val rearTemperatureLabel =
            findViewById<android.widget.TextView>(R.id.rear_temperature_range_label)

        fun updateThresholdSettings() {
            frontRangeLabel.text = getString(R.string.front_pressure_range, frontLow, frontHigh)
            rearRangeLabel.text = getString(R.string.rear_pressure_range, rearLow, rearHigh)
            frontRangeSlider.contentDescription = frontRangeLabel.text
            rearRangeSlider.contentDescription = rearRangeLabel.text
            frontTemperatureLabel.text = getString(
                R.string.front_temperature_range, frontTemperatureMin, frontTemperatureMax
            )
            rearTemperatureLabel.text = getString(
                R.string.rear_temperature_range, rearTemperatureMin, rearTemperatureMax
            )
            frontTemperatureSlider.contentDescription = frontTemperatureLabel.text
            rearTemperatureSlider.contentDescription = rearTemperatureLabel.text
            preferences.edit()
                .putFloat("front_low_threshold_bar", frontLow)
                .putFloat("front_high_threshold_bar", frontHigh)
                .putFloat("rear_low_threshold_bar", rearLow)
                .putFloat("rear_high_threshold_bar", rearHigh)
                .putFloat("front_temperature_min_c", frontTemperatureMin)
                .putFloat("front_temperature_max_c", frontTemperatureMax)
                .putFloat("rear_temperature_min_c", rearTemperatureMin)
                .putFloat("rear_temperature_max_c", rearTemperatureMax)
                .apply()
            dashboard.updatePressureThresholds(frontLow, frontHigh, rearLow, rearHigh)
            dashboard.updateTemperatureColorRanges(
                frontTemperatureMin, frontTemperatureMax, rearTemperatureMin, rearTemperatureMax
            )
        }

        updateThresholdSettings()
        frontRangeSlider.addOnChangeListener { slider, value, _ ->
            val values = slider.values
            frontLow = values[0]
            frontHigh = values[1]
            if (frontHigh - frontLow < 0.049f) {
                if (value <= frontLow) {
                    frontLow = (frontHigh - 0.05f).coerceAtLeast(1f)
                } else {
                    frontHigh = (frontLow + 0.05f).coerceAtMost(3f)
                }
                slider.values = listOf(frontLow, frontHigh)
            }
            updateThresholdSettings()
        }
        rearRangeSlider.addOnChangeListener { slider, value, _ ->
            val values = slider.values
            rearLow = values[0]
            rearHigh = values[1]
            if (rearHigh - rearLow < 0.049f) {
                if (value <= rearLow) {
                    rearLow = (rearHigh - 0.05f).coerceAtLeast(1f)
                } else {
                    rearHigh = (rearLow + 0.05f).coerceAtMost(3f)
                }
                slider.values = listOf(rearLow, rearHigh)
            }
            updateThresholdSettings()
        }
        frontTemperatureSlider.addOnChangeListener { slider, value, _ ->
            val values = slider.values
            frontTemperatureMin = values[0]
            frontTemperatureMax = values[1]
            if (frontTemperatureMax - frontTemperatureMin < 0.99f) {
                if (value <= frontTemperatureMin) {
                    frontTemperatureMin = (frontTemperatureMax - 1f).coerceAtLeast(0f)
                } else {
                    frontTemperatureMax = (frontTemperatureMin + 1f).coerceAtMost(110f)
                }
                slider.values = listOf(frontTemperatureMin, frontTemperatureMax)
            }
            updateThresholdSettings()
        }
        rearTemperatureSlider.addOnChangeListener { slider, value, _ ->
            val values = slider.values
            rearTemperatureMin = values[0]
            rearTemperatureMax = values[1]
            if (rearTemperatureMax - rearTemperatureMin < 0.99f) {
                if (value <= rearTemperatureMin) {
                    rearTemperatureMin = (rearTemperatureMax - 1f).coerceAtLeast(0f)
                } else {
                    rearTemperatureMax = (rearTemperatureMin + 1f).coerceAtMost(110f)
                }
                slider.values = listOf(rearTemperatureMin, rearTemperatureMax)
            }
            updateThresholdSettings()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                settingsScreen.visibility = View.GONE
                mainContent.visibility = View.VISIBLE
                isEnabled = false
            }
        }.also { callback ->
            findViewById<View>(R.id.settings_button).setOnClickListener {
                mainContent.visibility = View.GONE
                settingsScreen.visibility = View.VISIBLE
                callback.isEnabled = true
            }
            findViewById<View>(R.id.settings_back_button).setOnClickListener {
                settingsScreen.visibility = View.GONE
                mainContent.visibility = View.VISIBLE
                callback.isEnabled = false
            }
        })
        val splashImage = ImageView(this).apply {
            setImageResource(R.drawable.splash)
            scaleType = ImageView.ScaleType.FIT_XY
            contentDescription = null
        }
        root.addView(
            splashImage,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        lifecycleScope.launch {
            delay(2_000)
            root.removeView(splashImage)
        }

        val dashboardView = findViewById<View>(R.id.tire_dashboard)
        val basePadding = dashboardView.paddingLeft
        ViewCompat.setOnApplyWindowInsetsListener(dashboardView) { view, insets ->
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            view.setPadding(
                basePadding + cutout.left, basePadding + cutout.top,
                basePadding + cutout.right, basePadding + cutout.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(dashboardView)

        val panels = mapOf(
            TirePosition.FRONT_LEFT to findViewById<TirePanelView>(R.id.front_left_tire),
            TirePosition.FRONT_RIGHT to findViewById<TirePanelView>(R.id.front_right_tire),
            TirePosition.REAR_LEFT to findViewById<TirePanelView>(R.id.rear_left_tire),
            TirePosition.REAR_RIGHT to findViewById<TirePanelView>(R.id.rear_right_tire)
        )
        val labels = mapOf(
            TirePosition.FRONT_LEFT to getString(R.string.tire_front_left),
            TirePosition.FRONT_RIGHT to getString(R.string.tire_front_right),
            TirePosition.REAR_LEFT to getString(R.string.tire_rear_left),
            TirePosition.REAR_RIGHT to getString(R.string.tire_rear_right)
        )
        dashboard.state.observe(this) { state ->
            panels.forEach { (position, panel) ->
                panel.bind(position, labels.getValue(position), state.readings.getValue(position), state)
            }
        }

        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
