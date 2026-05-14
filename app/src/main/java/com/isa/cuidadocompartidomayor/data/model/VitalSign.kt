package com.isa.cuidadocompartidomayor.data.model
/* Modelo de datos para signos vitales
 * Colección en Firestore: vital_signs */
data class VitalSign(
    val id: String = "",
    val elderlyId: String = "",
    val elderlyName: String = "",
    val caregiverId: String = "",
    val caregiverName: String = "",

    // Signos vitales (todos opcionales)
    val heartRate: Int? = null,              // LPM
    val glucose: Double? = null,             // mg/dL
    val glucoseMoment: String? = null,       // GlucoseMoment enum
    val temperature: Double? = null,         // °C
    val systolicBP: Int? = null,            // mmHg
    val diastolicBP: Int? = null,           // mmHg
    val oxygenSaturation: Int? = null,      // %
    val weight: Double? = null,             // Kg

    // Interpretaciones automáticas (calculadas)
    val heartRateStatus: String? = null,     // HeartRateStatus enum
    val temperatureStatus: String? = null,   // TemperatureStatus enum
    val bpStatus: String? = null,           // BloodPressureStatus enum
    val oxygenStatus: String? = null,       // OxygenStatus enum

    // Notas adicionales
    val notes: String = "",

    // Metadata
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Información de edición
    val editedBy: String? = null,
    val editedByName: String? = null,
    val editedAt: Long? = null
) {
    /**
     * Verifica si al menos un signo vital está registrado
     */
    fun hasAnyVitalSign(): Boolean {
        return heartRate != null || glucose != null || temperature != null ||
                (systolicBP != null && diastolicBP != null) ||
                oxygenSaturation != null || weight != null
    }

    /**
     * Obtiene una lista de los signos vitales registrados
     */
    fun getRegisteredSigns(): List<VitalSignType> {
        val signs = mutableListOf<VitalSignType>()
        if (heartRate != null) signs.add(VitalSignType.HEART_RATE)
        if (glucose != null) signs.add(VitalSignType.GLUCOSE)
        if (temperature != null) signs.add(VitalSignType.TEMPERATURE)
        if (systolicBP != null && diastolicBP != null) signs.add(VitalSignType.BLOOD_PRESSURE)
        if (oxygenSaturation != null) signs.add(VitalSignType.OXYGEN_SATURATION)
        if (weight != null) signs.add(VitalSignType.WEIGHT)
        return signs
    }

    /**
     * Convierte a Map para Firestore
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "elderlyId" to elderlyId,
            "elderlyName" to elderlyName,
            "caregiverId" to caregiverId,
            "caregiverName" to caregiverName,
            "heartRate" to heartRate,
            "glucose" to glucose,
            "glucoseMoment" to glucoseMoment,
            "temperature" to temperature,
            "systolicBP" to systolicBP,
            "diastolicBP" to diastolicBP,
            "oxygenSaturation" to oxygenSaturation,
            "weight" to weight,
            "heartRateStatus" to heartRateStatus,
            "temperatureStatus" to temperatureStatus,
            "bpStatus" to bpStatus,
            "oxygenStatus" to oxygenStatus,
            "notes" to notes,
            "timestamp" to timestamp,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "editedBy" to editedBy,
            "editedByName" to editedByName,
            "editedAt" to editedAt
        )
    }

    companion object {
        /**
         * Crea una instancia de VitalSign calculando automáticamente los estados
         */
        fun create(
            id: String = "",
            elderlyId: String,
            elderlyName: String,
            caregiverId: String,
            caregiverName: String,
            heartRate: Int? = null,
            glucose: Double? = null,
            glucoseMoment: GlucoseMoment? = null,
            temperature: Double? = null,
            systolicBP: Int? = null,
            diastolicBP: Int? = null,
            oxygenSaturation: Int? = null,
            weight: Double? = null,
            notes: String = "",
            timestamp: Long = System.currentTimeMillis()
        ): VitalSign {
            // Calcular estados automáticamente
            val heartRateStatus = heartRate?.let {
                HeartRateStatus.fromHeartRate(it).name
            }

            val temperatureStatus = temperature?.let {
                TemperatureStatus.fromTemperature(it).name
            }

            val bpStatus = if (systolicBP != null && diastolicBP != null) {
                BloodPressureStatus.fromBloodPressure(systolicBP, diastolicBP).name
            } else null

            val oxygenStatus = oxygenSaturation?.let {
                OxygenStatus.fromOxygenSaturation(it).name
            }

            return VitalSign(
                id = id,
                elderlyId = elderlyId,
                elderlyName = elderlyName,
                caregiverId = caregiverId,
                caregiverName = caregiverName,
                heartRate = heartRate,
                glucose = glucose,
                glucoseMoment = glucoseMoment?.name,
                temperature = temperature,
                systolicBP = systolicBP,
                diastolicBP = diastolicBP,
                oxygenSaturation = oxygenSaturation,
                weight = weight,
                heartRateStatus = heartRateStatus,
                temperatureStatus = temperatureStatus,
                bpStatus = bpStatus,
                oxygenStatus = oxygenStatus,
                notes = notes,
                timestamp = timestamp,
                createdAt = timestamp,
                updatedAt = timestamp
            )
        }
    }
}