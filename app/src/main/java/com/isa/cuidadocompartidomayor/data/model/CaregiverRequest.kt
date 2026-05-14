package com.isa.cuidadocompartidomayor.data.model
import java.util.*
data class CaregiverRequest(
    val id: String = "",
    val caregiverId: String = "",
    val elderlyId: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val caregiverName: String = "",
    val caregiverEmail: String = "",
    val caregiverMessage: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val respondedAt: Long? = null,
    val inviteCodeUsed: String? = null,
    val caregiverProfileImageUrl: String = ""
) {
    constructor() : this("", "", "", RequestStatus.PENDING, "", "", "", 0L, 0L, null, null, "")

    /** Estados posibles de una solicitud */
    enum class RequestStatus {
        PENDING,    // Pendiente de respuesta
        APPROVED,   // Aprobada por el adulto mayor
        REJECTED    // Rechazada por el adulto mayor
    }

    /** Verifica si la solicitud está pendiente. 0 usages */
    fun isPending(): Boolean = status == RequestStatus.PENDING

    /** Verifica si la solicitud fue aprobada. 0 usages */
    fun isApproved(): Boolean = status == RequestStatus.APPROVED

    /** Verifica si la solicitud fue rechazada. 0 usages */
    fun isRejected(): Boolean = status == RequestStatus.REJECTED

    /** Obtiene el tiempo transcurrido desde la creación. 0 usages */
    fun getTimeAgo(): String {
        val now = System.currentTimeMillis()
        val diffInHours = (now - createdAt) / (1000 * 60 * 60)

        return when {
            diffInHours < 1 -> "Hace menos de 1 hora"
            diffInHours == 1L -> "Hace 1 hora"
            diffInHours < 24 -> "Hace $diffInHours horas"
            diffInHours < 48 -> "Hace 1 día"
            else -> {
                val days = diffInHours / 24
                "Hace $days días"
            }
        }
    }

    /** Obtiene el texto del estado para mostrar en UI. 0 usages */
    fun getStatusText(): String {
        return when (status) {
            RequestStatus.PENDING -> "Pendiente"
            RequestStatus.APPROVED -> "Aprobado"
            RequestStatus.REJECTED -> "Rechazado"
        }
    }

    /** Obtiene el emoji del estado. 0 usages */
    fun getStatusEmoji(): String {
        return when (status) {
            RequestStatus.PENDING -> "⏳"
            RequestStatus.APPROVED -> "✅"
            RequestStatus.REJECTED -> "❌"
        }
    }

    /** Obtiene la fecha de creación formateada */
    fun getFormattedCreatedDate(): String {
        return java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(createdAt))
    }

    /**
     * Verifica si la solicitud es reciente (menos de 24 horas). 0 usages
     */
    fun isRecent(): Boolean {
        val now = System.currentTimeMillis()
        val dayInMillis = 24 * 60 * 60 * 1000
        return (now - createdAt) < dayInMillis
    }

    /** Crea una copia con estado actualizado */
    fun withStatus(newStatus: RequestStatus): CaregiverRequest {
        return copy(
            status = newStatus,
            updatedAt = System.currentTimeMillis(),
            respondedAt = if (newStatus != RequestStatus.PENDING) System.currentTimeMillis() else null
        )
    }
}