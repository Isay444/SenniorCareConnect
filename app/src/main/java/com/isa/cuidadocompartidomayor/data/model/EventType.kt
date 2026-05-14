package com.isa.cuidadocompartidomayor.data.model

import com.isa.cuidadocompartidomayor.R

enum class EventType {
    MEDICAL_APPOINTMENT,
    NORMAL_TASK;

    fun getDisplayName(): String {
        return when (this) {
            MEDICAL_APPOINTMENT -> "Cita Médica"
            NORMAL_TASK -> "Tarea"
        }
    }

    fun getIcon(): Int {
        return when (this) {
            MEDICAL_APPOINTMENT -> R.drawable.ic_medical_calendar // 🏥
            NORMAL_TASK -> R.drawable.ic_task_checkbox // ☑️
        }
    }

    fun getColorRes(): Int {
        return when (this) {
            MEDICAL_APPOINTMENT -> R.color.medical_appointment_color // Azul/Celeste
            NORMAL_TASK -> R.color.normal_task_color // Verde/Teal
        }
    }
}
