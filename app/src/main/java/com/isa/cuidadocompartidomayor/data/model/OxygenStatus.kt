package com.isa.cuidadocompartidomayor.data.model

import com.isa.cuidadocompartidomayor.R

enum class OxygenStatus(val displayName: String, val range: String, val colorRes: Int) {
    NORMAL("Normosaturación", "> 95%", R.color.green_one),
    MILD_HYPOXIA("Hipoxia Leve", "93-95%", android.R.color.holo_orange_light),
    MODERATE_HYPOXIA("Hipoxia Moderada", "88-92%", android.R.color.holo_orange_dark),
    SEVERE_HYPOXIA("Hipoxia Severa", "< 88%", android.R.color.holo_red_dark);

    companion object {
        fun fromOxygenSaturation(saturation: Int): OxygenStatus {
            return when {
                saturation > 95 -> NORMAL
                saturation in 93..95 -> MILD_HYPOXIA
                saturation in 88..92 -> MODERATE_HYPOXIA
                else -> SEVERE_HYPOXIA
            }
        }
    }
}