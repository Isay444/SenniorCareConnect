package com.isa.cuidadocompartidomayor.data.model

enum class VitalSignType (val displayName: String, val unit: String){
    HEART_RATE("Frecuencia Cardíaca", "LPM"),
    GLUCOSE("Glucosa", "mg/dL"),
    TEMPERATURE("Temperatura", "°C"),
    BLOOD_PRESSURE("Presión Arterial", "mmHg"),
    OXYGEN_SATURATION("Saturación O₂", "%"),
    WEIGHT("Peso", "Kg");

    companion object {
        fun fromString(value: String): VitalSignType? {
            return entries.find { it.name == value }
        }
    }
}