package com.isa.cuidadocompartidomayor.data.model

enum class AppetiteLevel(val displayName: String, val emoji: String) {
    GOOD("Alto", "🍽️"),
    REGULAR("Regular", "🥄"),
    POOR("Bajo", "🚫");

    companion object {
        fun fromString(value: String): AppetiteLevel? {
            return entries.find { it.name == value }
        }
    }
}