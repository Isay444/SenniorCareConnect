package com.isa.cuidadocompartidomayor.data.model

data class CaregiverNotification(
    val id: String = "",
    val caregiverId: String = "",
    val elderlyId: String = "",
    val elderlyName: String = "",
    val medicationId: String = "",
    val medicationName: String = "",
    val dosage: String = "",
    val time: String = "",
    val type: String = "REMINDER", // REMINDER, EMERGENCY
    val title: String = "",
    val body: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)
