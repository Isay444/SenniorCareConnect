package com.isa.cuidadocompartidomayor.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.MedicationType.*
import java.text.SimpleDateFormat
import java.util.*

data class MedicationLog(
    val id: String = "",
    val medicationId: String = "",
    val medicationName: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val scheduledTime: Long = 0,
    val takenAt: Long? = null,
    @get:PropertyName("statusString")
    @set:PropertyName("statusString")
    var statusString: String = LogStatus.PENDING.name,
    val elderlyId: String = "",
    val elderlyName: String = "",
    val caregiverId: String = "",
    val caregiverName: String = "",
    val confirmedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val medicationActive: Boolean = true,
    val lastModifiedBy: String = "",      // Usuario que modificó manualmente
    val lastModifiedAt: Long = 0L         // Timestamp de última modificación manual
) {
    // Constructor sin argumentos para Firestore
    constructor() : this("", "", "", "", "", 0L, null, LogStatus.PENDING.name, "", "", "", "", null, 0L, true)

    // PROPIEDAD COMPUTADA
    @get:Exclude
    val status: LogStatus
        get() = try {
            LogStatus.valueOf(statusString)
        } catch (e: Exception) {
            LogStatus.PENDING
        }

    // MÉTODOS COMPUTADOS
    @Exclude
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(scheduledTime))
    }

    @Exclude
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(scheduledTime))
    }

    @Exclude
    fun getStatusText(): String {
        return when (status) {
            LogStatus.ON_TIME -> "A tiempo"
            LogStatus.LATE -> "Atrasado"
            LogStatus.MISSED -> "Omitido"
            LogStatus.PENDING -> "Pendiente"
        }
    }

    @Exclude
    fun getStatusColor(): Int {
        return when (status) {
            LogStatus.ON_TIME -> 0xFF85CB33.toInt()     // Verde /confirmado
            LogStatus.LATE ->    0xFFFF9800.toInt()     // Naranja / atrasado
            LogStatus.MISSED ->  0xFFF44336.toInt()     // Rojo /omitido
            LogStatus.PENDING -> 0xFF9E9E9E.toInt()     // Gris / pendiente
        }
    }

    /**
     * Obtiene texto descriptivo si el medicamento fue eliminado
     */
    fun getMedicationStatusText(): String {
        return if (medicationActive) {
            medicationName
        } else {
            "$medicationName (Eliminado)"
        }
    }
}


enum class LogStatus {
    ON_TIME,     // 🟢 Tomado a tiempo
    LATE,        // 🟠 Tomado con retraso
    MISSED,      // 🔴 Omitido (no tomado)
    PENDING      // ⏳ Pendiente (aún no es la hora)
}
