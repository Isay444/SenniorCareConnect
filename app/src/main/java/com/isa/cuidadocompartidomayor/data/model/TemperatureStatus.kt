package com.isa.cuidadocompartidomayor.data.model

import com.isa.cuidadocompartidomayor.R

enum class TemperatureStatus(val displayName: String, val range: String, val colorRes: Int) {
    HYPOTHERMIA("Hipotermia", "< 35°C", android.R.color.holo_blue_dark),
    NORMAL("Normal", "36-37°C", R.color.green_one),
    FEBRICULA("Febrícula", "37-37.9°C", R.color.glucose_stroke),
    FEVER("Fiebre", "> 38°C", android.R.color.holo_red_dark);

    companion object {
        fun fromTemperature(temperature: Double): TemperatureStatus {
            return when {
                temperature < 35.0 -> HYPOTHERMIA
                temperature in 35.1..37.0 -> NORMAL
                temperature in 37.1..37.9 -> FEBRICULA
                else -> FEVER
            }
        }
    }
}