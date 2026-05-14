package com.isa.cuidadocompartidomayor.data.model

enum class GlucoseMoment (val displayName: String, val icon: String) {
    BEFORE_BREAKFAST("Antes del desayuno", "🌅"),
    AFTER_BREAKFAST("Después del desayuno", "☕"),
    BEFORE_LUNCH("Antes de la comida", "🌞"),
    AFTER_LUNCH("Después de la comida", "🍽️"),
    BEFORE_DINNER("Antes de la cena", "🌆"),
    AFTER_DINNER("Después de la cena", "🌙");

    companion object {
        fun fromString(value: String): GlucoseMoment? {
            return entries.find { it.name == value }
        }
    }
}