package com.isa.cuidadocompartidomayor.data.model

enum class MoodSymptom(val displayName: String, val emoji: String) {
    PAIN("Dolor", "🤕"),
    FATIGUE("Fatiga", "😫"),
    DIZZINESS("Mareo", "😵"),
    NAUSEA("Náuseas", "🤢"),
    SLEEPINESS("Sueño", "😴"),
    DISORIENTATION("Desorientación", "😕"),
    FRUSTRATION("Frustración", "😤"),
    WORRY("Preocupación", "😟");

    companion object {
        fun fromString(value: String): MoodSymptom? {
            return entries.find { it.name == value }
        }
    }
}