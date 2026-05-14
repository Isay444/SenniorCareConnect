package com.isa.cuidadocompartidomayor.data.model

enum class MoodEmotion(val displayName: String, val emoji: String) {

    IRRITABILITY("Irritabilidad", "😠"),
    SADNESS("Tristeza", "😢"),
    ANXIETY("Ansiedad", "😰"),
    ANGER("Ira", "😡"),
    FEAR("Miedo", "😨"),
    REJECTION("Rechazo", "😒"),
    APATHY("Apatía", "😑"),
    JOY("Alegría", "😊"),
    SERENITY("Serenidad", "😌"),
    SURPRISE("Sorpresa", "😲"),
    SOCIABLE("Sociable", "🤗"),
    PRIDE("Orgullo", "😌"),
    PURPOSE("Sentido de Propósito", "💪"),
    GRATITUDE("Agradecido", "🙏"),
    NEUTRAL("Neutro", "😐"),
    BOREDOM("Sedentario", "😴");

    companion object {
        fun fromString(value: String): MoodEmotion? {
            return entries.find { it.name == value }
        }
    }
}