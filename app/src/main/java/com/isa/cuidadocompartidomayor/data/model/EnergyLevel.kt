package com.isa.cuidadocompartidomayor.data.model

enum class EnergyLevel (val displayName: String, val emoji: String) {
    HIGH("Alta", "⚡"),
    MEDIUM("Media", "🔋"),
    LOW("Baja", "🪫");

    companion object {
        fun fromString(value: String): EnergyLevel? {
            return entries.find { it.name == value }
        }
    }
}