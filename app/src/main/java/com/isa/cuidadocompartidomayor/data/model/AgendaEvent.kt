package com.isa.cuidadocompartidomayor.data.model

import java.text.SimpleDateFormat
import java.util.*

sealed class AgendaEvent {
    abstract val id: String
    abstract val type: EventType
    abstract val title: String
    abstract val date: Long
    abstract val elderlyId: String
    abstract val elderlyName: String
    abstract val createdBy: String
    abstract val createdByName: String
    abstract val notes: String
    abstract val createdAt: Long
    abstract val updatedAt: Long

    /**
     * Formatea la fecha del evento
     */
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(date))
    }

    /**
     * Formatea la hora del evento
     */
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(date))
    }

    /**
     * Obtiene el día del mes (para la UI de dashboard)
     */
    fun getDayOfMonth(): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        return String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
    }

    /**
     * Obtiene el mes abreviado (NOV, DIC, etc.)
     */
    fun getMonthAbbreviation(): String {
        val sdf = SimpleDateFormat("MMM", Locale.getDefault())
        return sdf.format(Date(date)).uppercase()
    }

    /**
     * Verifica si el evento es hoy
     */
    fun isToday(): Boolean {
        val eventCalendar = Calendar.getInstance().apply { timeInMillis = date }
        val todayCalendar = Calendar.getInstance()

        return eventCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                eventCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Verifica si el evento ya pasó
     */
    fun isPast(): Boolean {
        return date < System.currentTimeMillis()
    }

    data class MedicalAppointment(
        override val id: String,
        override val type: EventType = EventType.MEDICAL_APPOINTMENT,
        override val title: String,
        override val date: Long,
        override val elderlyId: String,
        override val elderlyName: String,
        override val createdBy: String,
        override val createdByName: String,
        override val notes: String,
        override val createdAt: Long,
        override val updatedAt: Long,
        val location: String
    ) : AgendaEvent() {

        fun toMap(): Map<String, Any?> = mapOf(
            "id" to id,
            "type" to type.name,
            "title" to title,
            "date" to date,
            "elderlyId" to elderlyId,
            "elderlyName" to elderlyName,
            "createdBy" to createdBy,
            "createdByName" to createdByName,
            "notes" to notes,
            "location" to location,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )

        companion object {
            fun fromMap(id: String, data: Map<String, Any?>): MedicalAppointment? {
                return try {
                    MedicalAppointment(
                        id = id,
                        title = data["title"] as? String ?: "",
                        date = data["date"] as? Long ?: 0L,
                        elderlyId = data["elderlyId"] as? String ?: "",
                        elderlyName = data["elderlyName"] as? String ?: "",
                        createdBy = data["createdBy"] as? String ?: "",
                        createdByName = data["createdByName"] as? String ?: "",
                        notes = data["notes"] as? String ?: "",
                        location = data["location"] as? String ?: "",
                        createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                        updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    data class NormalTask(
        override val id: String,
        override val type: EventType = EventType.NORMAL_TASK,
        override val title: String,
        override val date: Long,
        override val elderlyId: String,
        override val elderlyName: String,
        override val createdBy: String,
        override val createdByName: String,
        override val notes: String,
        override val createdAt: Long,
        override val updatedAt: Long,
        val assignedTo: String?,
        val assignedToName: String?,
        val isCompleted: Boolean = false,
        val completedAt: Long? = null,
        val completedBy: String? = null
    ) : AgendaEvent() {

        fun toMap(): Map<String, Any?> = mapOf(
            "id" to id,
            "type" to type.name,
            "title" to title,
            "date" to date,
            "elderlyId" to elderlyId,
            "elderlyName" to elderlyName,
            "createdBy" to createdBy,
            "createdByName" to createdByName,
            "notes" to notes,
            "assignedTo" to assignedTo,
            "assignedToName" to assignedToName,
            "isCompleted" to isCompleted,
            "completedAt" to completedAt,
            "completedBy" to completedBy,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )

        companion object {
            fun fromMap(id: String, data: Map<String, Any?>): NormalTask? {
                return try {
                    NormalTask(
                        id = id,
                        title = data["title"] as? String ?: "",
                        date = data["date"] as? Long ?: 0L,
                        elderlyId = data["elderlyId"] as? String ?: "",
                        elderlyName = data["elderlyName"] as? String ?: "",
                        createdBy = data["createdBy"] as? String ?: "",
                        createdByName = data["createdByName"] as? String ?: "",
                        notes = data["notes"] as? String ?: "",
                        assignedTo = data["assignedTo"] as? String,
                        assignedToName = data["assignedToName"] as? String,
                        isCompleted = data["isCompleted"] as? Boolean ?: false,
                        completedAt = data["completedAt"] as? Long,
                        completedBy = data["completedBy"] as? String,
                        createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                        updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}
