package com.isa.cuidadocompartidomayor.data.model

import com.isa.cuidadocompartidomayor.R

enum class BloodPressureStatus(val displayName: String, val range: String, val colorRes: Int) {
    HYPOTENSION("Hipotensión", "< 80/60 mmHg", R.color.glucose_stroke),
    NORMAL("Normotensión", "80-120/60-80 mmHg", R.color.green_one),
    ELEVATED("Elevada", "80-120/60-80 mmHg", R.color.status_late),
    HYPERTENSION_L1("Hipertensión Nivel 1", "130-139/80-89 mmHg", R.color.elderly_emergency),
    HYPERTENSION_L2("Hipertensión Nivel 2", "> 140/", R.color.status_missed),
    HYPERTENSION_CRYSIS("Crisis de Hipertensión", "> 180/120", R.color.status_critical);

    companion object {
        fun fromBloodPressure(systolic: Int, diastolic: Int): BloodPressureStatus {
            return when {
                systolic < 80 || diastolic < 60 -> HYPOTENSION
                systolic in 80..120 && diastolic in 60 .. 80 -> NORMAL
                systolic in 120..129 && diastolic > 80 -> ELEVATED
                systolic in 130..139 || diastolic in 80 .. 89 -> HYPERTENSION_L1
                systolic >= 140 || diastolic >= 90 -> HYPERTENSION_L2
                else -> HYPERTENSION_CRYSIS
            }
        }
    }
}