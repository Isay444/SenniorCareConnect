package com.isa.cuidadocompartidomayor.data.model

import com.isa.cuidadocompartidomayor.R

enum class HeartRateStatus(val displayName: String, val range: String, val colorRes: Int) {
    BRADYCARDIA("Bradicardia", "< 60 LPM", R.color.status_critical),
    NORMAL("Normocardiaco", "60-100 LPM", R.color.green_one),
    TACHYCARDIA("Taquicardia", "> 100 LPM", android.R.color.holo_orange_dark);

    companion object {
        fun fromHeartRate(heartRate: Int): HeartRateStatus {
            return when {
                heartRate < 60 -> BRADYCARDIA
                heartRate in 60..100 -> NORMAL
                else -> TACHYCARDIA
            }
        }
    }
}