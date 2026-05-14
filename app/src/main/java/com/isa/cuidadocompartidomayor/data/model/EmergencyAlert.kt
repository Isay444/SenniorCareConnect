package com.isa.cuidadocompartidomayor.data.model

data class EmergencyAlert(
    val id: String = "",
    val elderlyId: String = "",
    val elderlyName: String = "",
    val elderlyEmail: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "active", // active, resolved, cancelled
    val message: String = "¡EMERGENCIA! Necesito ayuda",
    val notifiedCaregivers: List<String> = emptyList(),
    val resolvedBy: String = "",
    val resolvedAt: Long = 0
) {
    // Verifica si la alerta tiene ubicación
    fun hasLocation(): Boolean = latitude != null && longitude != null

    // Obtiene la URL de Google Maps
    fun getGoogleMapsUrl(): String? {
        return if (hasLocation()) {
            "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
        } else null
    }

    // Formatea la ubicación para mostrar
    fun getFormattedLocation(): String {
        return if (hasLocation()) {
            "Lat: %.6f, Long: %.6f".format(latitude, longitude)
        } else {
            "Ubicación no disponible"
        }
    }

}