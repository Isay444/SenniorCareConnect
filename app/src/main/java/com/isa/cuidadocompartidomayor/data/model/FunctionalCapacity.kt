package com.isa.cuidadocompartidomayor.data.model

enum class FunctionalCapacity(val displayName: String, val emoji: String) {
    SUFFICIENT("Suficiente", "✅"),
    REGULAR("Regular", "⚠️"),
    INSUFFICIENT("Insuficiente", "❌");

    companion object {
        fun fromString(value: String): FunctionalCapacity? {
            return entries.find { it.name == value }
        }
    }
}