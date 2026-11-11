package com.example.exigence

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
                panel.bind(labels.getValue(position), state.readings.getValue(position), state)
            }
        }

        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
