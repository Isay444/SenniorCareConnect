package com.isa.cuidadocompartidomayor.data.model
/** Modelo de datos para registros de estado de ánimo
 * Colección en Firestore: mood_entries
 */
data class MoodEntry (
    val id: String = "",
    val elderlyId: String = "",
    val elderlyName: String = "",
    val caregiverId: String = "",
    val caregiverName: String = "",

    // Emociones (lista de MoodEmotion enum names)
    val emotions: List<String> = emptyList(),

    // Síntomas (lista de MoodSymptom enum names)
    val symptoms: List<String> = emptyList(),

    // Niveles
    val energyLevel: String? = null,           // EnergyLevel enum
    val appetiteLevel: String? = null,         // AppetiteLevel enum
    val functionalCapacity: String? = null,    // FunctionalCapacity enum

    // Notas adicionales (texto libre)
    val notes: String = "",

    // Metadata
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Información de edición
    val editedBy: String? = null,
    val editedByName: String? = null,
    val editedAt: Long? = null
){
    /** Verifica si hay al menos un dato registrado */
    fun hasAnyData(): Boolean {
        return emotions.isNotEmpty() || symptoms.isNotEmpty() ||
                energyLevel != null || appetiteLevel != null ||
                functionalCapacity != null || notes.isNotEmpty()
    }

    /** Obtiene las emociones como objetos MoodEmotion */
    fun getEmotionsList(): List<MoodEmotion> {
        return emotions.mapNotNull { MoodEmotion.fromString(it) }
    }

    /** Obtiene los síntomas como objetos MoodSymptom */
    fun getSymptomsList(): List<MoodSymptom> {
        return symptoms.mapNotNull { MoodSymptom.fromString(it) }
    }

    /** Obtiene el nivel de energía como objeto EnergyLevel */
    fun getEnergyLevelEnum(): EnergyLevel? {
        return energyLevel?.let { EnergyLevel.fromString(it) }
    }

    /** Obtiene el nivel de apetito como objeto AppetiteLevel */
    fun getAppetiteLevelEnum(): AppetiteLevel? {
        return appetiteLevel?.let { AppetiteLevel.fromString(it) }
    }

    /** Obtiene la capacidad funcional como objeto FunctionalCapacity */
    fun getFunctionalCapacityEnum(): FunctionalCapacity? {
        return functionalCapacity?.let { FunctionalCapacity.fromString(it) }
    }

    /** Convierte a Map para Firestore */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "elderlyId" to elderlyId,
            "elderlyName" to elderlyName,
            "caregiverId" to caregiverId,
            "caregiverName" to caregiverName,
            "emotions" to emotions,
            "symptoms" to symptoms,
            "energyLevel" to energyLevel,
            "appetiteLevel" to appetiteLevel,
            "functionalCapacity" to functionalCapacity,
            "notes" to notes,
            "timestamp" to timestamp,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "editedBy" to editedBy,
            "editedByName" to editedByName,
            "editedAt" to editedAt
        )
    }
}