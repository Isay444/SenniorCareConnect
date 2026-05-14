package com.isa.cuidadocompartidomayor.utils

import android.graphics.Color
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.toColorInt

/**
 * Clase utilitaria para configurar gráficas de MPAndroidChart
 */
object ChartHelper {

    // Colores para diferentes signos vitales
    object Colors {
        val HEART_RATE = Color.RED
        val GLUCOSE = "#FF9800".toColorInt() // Naranja
        val TEMPERATURE = "#2196F3".toColorInt() // Azul
        val BLOOD_PRESSURE_SYS = "#E91E63".toColorInt() // Rosa
        val BLOOD_PRESSURE_DIA = "#9C27B0".toColorInt() // Púrpura
        val OXYGEN = "#00BCD4".toColorInt() // Cian
        val WEIGHT = "#4CAF50".toColorInt() // Verde

        // Colores para estados de ánimo
        val MOOD_POSITIVE = "#4CAF50".toColorInt()
        val MOOD_NEUTRAL = "#FFC107".toColorInt()
        val MOOD_NEGATIVE = "#F44336".toColorInt()
    }

    /**
     * Configura el estilo común para LineCharts
     */
    fun setupLineChart(chart: LineChart) {
        chart.apply {
            // Configuración general
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            animateX(1000)

            // Eje X (inferior)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                granularity = 1f
                textSize = 10f
                labelRotationAngle = -45f
                setDrawAxisLine(true)
                setLabelCount(5, false)
            }

            // Eje Y izquierdo
            axisLeft.apply {
                setDrawGridLines(true)
                textSize = 10f
                setDrawAxisLine(true)
                granularity = 1f
            }

            // Eje Y derecho (deshabilitado)
            axisRight.isEnabled = false

            // Leyenda
            legend.apply {
                isEnabled = true
                textSize = 12f
                form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }
        }
    }

    /**
     * Configura el estilo común para BarCharts
     */
    fun setupBarChart(chart: BarChart) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = false
            setScaleEnabled(false)
            setDrawGridBackground(false)
            animateY(1000)

            // Eje X (inferior)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textSize = 11f
                labelRotationAngle = -45f
                setDrawAxisLine(true)
            }

            // Eje Y izquierdo
            axisLeft.apply {
                setDrawGridLines(true)
                textSize = 10f
                setDrawAxisLine(true)
                granularity = 1f
                axisMinimum = 0f
            }

            // Eje Y derecho (deshabilitado)
            axisRight.isEnabled = false

            // Leyenda
            legend.apply {
                isEnabled = true
                textSize = 12f
                form = com.github.mikephil.charting.components.Legend.LegendForm.SQUARE
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }
        }
    }

    /**
     * Formateador para convertir timestamp a fecha
     */
    class DateValueFormatter : ValueFormatter() {
        private val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            return dateFormat.format(Date(value.toLong()))
        }
    }

    /**
     * Formateador para convertir timestamp a fecha con hora
     */
    class DateTimeValueFormatter : ValueFormatter() {
        private val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            return dateFormat.format(Date(value.toLong()))
        }
    }

    /**
     * Formateador para valores de presión arterial
     */
    class BloodPressureValueFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return "${value.toInt()} mmHg"
        }
    }

    /**
     * Formateador para valores de frecuencia cardíaca
     */
    class HeartRateValueFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return "${value.toInt()} LPM"
        }
    }

    /**
     * Formateador para valores de glucosa
     */
    class GlucoseValueFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return "${value.toInt()} mg/dL"
        }
    }

    /**
     * Formateador para valores de temperatura
     */
    class TemperatureValueFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return String.format("%.1f°C", value)
        }
    }

    /**
     * Formateador para porcentajes
     */
    class PercentageValueFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return "${value.toInt()}%"
        }
    }
}
