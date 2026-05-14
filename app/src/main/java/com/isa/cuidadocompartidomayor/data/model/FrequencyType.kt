package com.isa.cuidadocompartidomayor.data.model

enum class FrequencyType(val displayName: String) {
    ONCE_DAILY("1 vez al día"),
    MULTIPLE_DAILY("Más de 1 vez al día"),
    SPECIFIC_DAYS("Días específicos de la semana"),
    EVERY_X_DAYS("Cada x días"),
    EVERY_X_WEEKS("Cada x semanas"),
    EVERY_X_MONTHS("Cada x meses");

    companion object {
        fun fromDisplayName(name: String): FrequencyType {
            return entries.firstOrNull { it.displayName == name } ?: ONCE_DAILY
        }
    }
}