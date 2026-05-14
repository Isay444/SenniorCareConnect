package com.isa.cuidadocompartidomayor.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.isa.cuidadocompartidomayor.data.model.AgendaEvent
import com.isa.cuidadocompartidomayor.data.model.EventType
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import java.util.UUID

class AgendaRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val EVENTS_COLLECTION = "events"
        private const val USERS_COLLECTION = "users"
        private const val TAG = "AgendaRepository"
    }

    // ==================== CREAR EVENTOS ====================

    /**
     * Crea una nueva cita médica
     */
    suspend fun createMedicalAppointment(
        title: String,
        date: Long,
        elderlyId: String,
        location: String,
        notes: String
    ): Result<AgendaEvent.MedicalAppointment> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Obtener información del cuidador
            val caregiverDoc = db.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .get()
                .await()

            val caregiverName = caregiverDoc.getString("name") ?: "Cuidador"

            // Obtener información del adulto mayor
            val elderlyDoc = db.collection(USERS_COLLECTION)
                .document(elderlyId)
                .get()
                .await()

            val elderlyName = elderlyDoc.getString("name") ?: "Adulto Mayor"

            // Crear el objeto MedicalAppointment
            val appointmentId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val appointment = AgendaEvent.MedicalAppointment(
                id = appointmentId,
                title = title,
                date = date,
                elderlyId = elderlyId,
                elderlyName = elderlyName,
                createdBy = currentUser.uid,
                createdByName = caregiverName,
                notes = notes,
                location = location,
                createdAt = timestamp,
                updatedAt = timestamp
            )

            // Guardar en Firestore
            db.collection(EVENTS_COLLECTION)
                .document(appointmentId)
                .set(appointment.toMap())
                .await()

            Log.d(TAG, "✅ Cita médica creada: $title - $elderlyName")
            Result.success(appointment)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando cita médica: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Crea una nueva tarea normal
     */
    suspend fun createNormalTask(
        title: String,
        date: Long,
        elderlyId: String,
        assignedTo: String?,
        notes: String
    ): Result<AgendaEvent.NormalTask> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Obtener información del cuidador
            val caregiverDoc = db.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .get()
                .await()

            val caregiverName = caregiverDoc.getString("name") ?: "Cuidador"

            // Obtener información del adulto mayor
            val elderlyDoc = db.collection(USERS_COLLECTION)
                .document(elderlyId)
                .get()
                .await()

            val elderlyName = elderlyDoc.getString("name") ?: "Adulto Mayor"

            // Obtener información del cuidador asignado (si existe)
            var assignedToName: String? = null
            if (assignedTo != null) {
                val assignedDoc = db.collection(USERS_COLLECTION)
                    .document(assignedTo)
                    .get()
                    .await()
                assignedToName = assignedDoc.getString("name")
            }

            // Crear el objeto NormalTask
            val taskId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val task = AgendaEvent.NormalTask(
                id = taskId,
                title = title,
                date = date,
                elderlyId = elderlyId,
                elderlyName = elderlyName,
                createdBy = currentUser.uid,
                createdByName = caregiverName,
                notes = notes,
                assignedTo = assignedTo,
                assignedToName = assignedToName,
                isCompleted = false,
                completedAt = null,
                completedBy = null,
                createdAt = timestamp,
                updatedAt = timestamp
            )

            // Guardar en Firestore
            db.collection(EVENTS_COLLECTION)
                .document(taskId)
                .set(task.toMap())
                .await()

            Log.d(TAG, "✅ Tarea creada: $title - $elderlyName")
            Result.success(task)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando tarea: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ==================== OBTENER EVENTOS ====================

    /**
     * Obtiene las citas médicas de un adulto mayor en un rango de fechas
     */
    suspend fun getMedicalAppointments(
        elderlyId: String,
        startDate: Long,
        endDate: Long
    ): Result<List<AgendaEvent.MedicalAppointment>> {
        return try {
            val snapshot = db.collection(EVENTS_COLLECTION)
                .whereEqualTo("elderlyId", elderlyId)
                .whereEqualTo("type", EventType.MEDICAL_APPOINTMENT.name)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()

            val appointments = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                AgendaEvent.MedicalAppointment.fromMap(doc.id, data)
            }

            Log.d(TAG, "✅ ${appointments.size} citas médicas obtenidas")
            Result.success(appointments)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo citas médicas: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene las tareas normales de un adulto mayor en un rango de fechas
     */
    suspend fun getNormalTasks(
        elderlyId: String,
        startDate: Long,
        endDate: Long
    ): Result<List<AgendaEvent.NormalTask>> {
        return try {
            val snapshot = db.collection(EVENTS_COLLECTION)
                .whereEqualTo("elderlyId", elderlyId)
                .whereEqualTo("type", EventType.NORMAL_TASK.name)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()

            val tasks = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                AgendaEvent.NormalTask.fromMap(doc.id, data)
            }

            Log.d(TAG, "✅ ${tasks.size} tareas obtenidas")
            Result.success(tasks)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo tareas: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene los próximos eventos para el dashboard del cuidador
     * - Todas las citas médicas de sus pacientes
     * - Tareas asignadas al cuidador actual
     */
    suspend fun getUpcomingEventsForDashboard(limit: Int = 4): Result<List<AgendaEvent>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // 1. Obtener todos los elderly IDs relacionados con el cuidador
            val relationshipsSnapshot = db.collection("relationships")
                .whereEqualTo("caregiverId", currentUser.uid)
                .whereEqualTo("status", "active")
                .get()
                .await()

            val elderlyIds = relationshipsSnapshot.documents.mapNotNull {
                it.getString("elderlyId")
            }

            if (elderlyIds.isEmpty()) {
                Log.d(TAG, "❌ No hay pacientes conectados")
                return Result.success(emptyList())
            }

            Log.d(TAG, "👥 Cargando próximos eventos de ${elderlyIds.size} pacientes")

            val now = System.currentTimeMillis()
            val allEvents = mutableListOf<AgendaEvent>()

            // 2. Para cada adulto mayor, obtener sus eventos
            for (elderlyId in elderlyIds) {
                // 2.1 Obtener TODAS las citas médicas futuras del adulto mayor
                val appointmentsSnapshot = db.collection(EVENTS_COLLECTION)
                    .whereEqualTo("elderlyId", elderlyId)
                    .whereEqualTo("type", "MEDICAL_APPOINTMENT")
                    .whereGreaterThanOrEqualTo("date", now)
                    .orderBy("date", Query.Direction.ASCENDING)
                    .get()
                    .await()

                // ✅ USAR fromMap() en lugar de toObject()
                val appointments = appointmentsSnapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    AgendaEvent.MedicalAppointment.fromMap(doc.id, data)
                }

                // 2.2 Obtener SOLO las tareas asignadas al cuidador actual
                val tasksSnapshot = db.collection(EVENTS_COLLECTION)
                    .whereEqualTo("elderlyId", elderlyId)
                    .whereEqualTo("type", "NORMAL_TASK")
                    .whereEqualTo("assignedTo", currentUser.uid)
                    .whereGreaterThanOrEqualTo("date", now)
                    .orderBy("date", Query.Direction.ASCENDING)
                    .get()
                    .await()

                // ✅ USAR fromMap() en lugar de toObject()
                val tasks = tasksSnapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    AgendaEvent.NormalTask.fromMap(doc.id, data)
                }

                allEvents.addAll(appointments)
                allEvents.addAll(tasks)
            }

            // 3. Ordenar por fecha y tomar los primeros N
            val sortedEvents = allEvents
                .sortedBy { it.date }
                .take(limit)

            Log.d(TAG, "✅ ${sortedEvents.size} próximos eventos obtenidos para dashboard")

            Result.success(sortedEvents)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo eventos para dashboard", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene TODOS los eventos de los adultos mayores del cuidador
     * - Todas las citas médicas
     * - Todas las tareas (sin importar a quién estén asignadas)
     */
    suspend fun getAllEventsForCaregiver(): Result<List<AgendaEvent>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // 1. Obtener todos los elderly IDs relacionados con el cuidador
            val relationshipsSnapshot = db.collection("relationships")
                .whereEqualTo("caregiverId", currentUser.uid)
                .whereEqualTo("status", "active")
                .get()
                .await()

            val elderlyIds = relationshipsSnapshot.documents.mapNotNull {
                it.getString("elderlyId")
            }

            if (elderlyIds.isEmpty()) {
                Log.d(TAG, "❌ No hay pacientes conectados")
                return Result.success(emptyList())
            }

            Log.d(TAG, "👥 Cargando TODOS los eventos de ${elderlyIds.size} pacientes")

            val allEvents = mutableListOf<AgendaEvent>()

            // 2. Para cada adulto mayor, obtener TODOS sus eventos
            for (elderlyId in elderlyIds) {
                val eventsSnapshot = db.collection(EVENTS_COLLECTION)
                    .whereEqualTo("elderlyId", elderlyId)
                    .orderBy("date", Query.Direction.DESCENDING)
                    .get()
                    .await()

                // ✅ USAR fromMap() en lugar de toObject()
                val events = eventsSnapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val type = data["type"] as? String ?: return@mapNotNull null

                    when (type) {
                        "MEDICAL_APPOINTMENT" -> {
                            AgendaEvent.MedicalAppointment.fromMap(doc.id, data)
                        }
                        "NORMAL_TASK" -> {
                            AgendaEvent.NormalTask.fromMap(doc.id, data)
                        }
                        else -> null
                    }
                }

                allEvents.addAll(events)
            }

            // 3. Ordenar por fecha (más recientes primero)
            val sortedEvents = allEvents.sortedByDescending { it.date }

            Log.d(TAG, "✅ ${sortedEvents.size} eventos totales obtenidos")

            Result.success(sortedEvents)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo todos los eventos", e)
            Result.failure(e)
        }
    }


    // ==================== ACTUALIZAR EVENTOS ====================

    /**
     * Actualiza una cita médica existente
     */
    suspend fun updateMedicalAppointment(
        eventId: String,
        title: String,
        date: Long,
        location: String,
        notes: String
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "title" to title,
                "date" to date,
                "location" to location,
                "notes" to notes,
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update(updates)
                .await()

            Log.d(TAG, "✅ Cita médica actualizada: $eventId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando cita médica: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza una tarea normal existente
     */
    suspend fun updateNormalTask(
        eventId: String,
        title: String,
        date: Long,
        assignedTo: String?,
        notes: String
    ): Result<Unit> {
        return try {
            // Obtener nombre del cuidador asignado
            var assignedToName: String? = null
            if (assignedTo != null) {
                val assignedDoc = db.collection(USERS_COLLECTION)
                    .document(assignedTo)
                    .get()
                    .await()
                assignedToName = assignedDoc.getString("name")
            }

            val updates = mapOf(
                "title" to title,
                "date" to date,
                "assignedTo" to assignedTo,
                "assignedToName" to assignedToName,
                "notes" to notes,
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update(updates)
                .await()

            Log.d(TAG, "✅ Tarea actualizada: $eventId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando tarea: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Marca una tarea como completada o no completada
     */
    suspend fun toggleTaskCompletion(
        taskId: String,
        isCompleted: Boolean
    ): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val updates = if (isCompleted) {
                mapOf(
                    "isCompleted" to true,
                    "completedAt" to System.currentTimeMillis(),
                    "completedBy" to currentUser.uid,
                    "updatedAt" to System.currentTimeMillis()
                )
            } else {
                mapOf(
                    "isCompleted" to false,
                    "completedAt" to null,
                    "completedBy" to null,
                    "updatedAt" to System.currentTimeMillis()
                )
            }

            db.collection(EVENTS_COLLECTION)
                .document(taskId)
                .update(updates)
                .await()

            Log.d(TAG, "✅ Tarea ${if (isCompleted) "completada" else "marcada como pendiente"}: $taskId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cambiando estado de tarea: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ==================== ELIMINAR EVENTOS ====================

    /**
     * Elimina un evento (cita o tarea)
     */
    suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .delete()
                .await()

            Log.d(TAG, "✅ Evento eliminado: $eventId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando evento: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ==================== OBTENER EVENTO POR ID ====================

    /**
     * Obtiene un evento específico por ID
     */
    suspend fun getEventById(eventId: String): Result<AgendaEvent?> {
        return try {
            val doc = db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .get()
                .await()

            val data = doc.data
            if (data == null) {
                return Result.success(null)
            }

            val type = data["type"] as? String
            val event = when (type) {
                EventType.MEDICAL_APPOINTMENT.name -> {
                    AgendaEvent.MedicalAppointment.fromMap(doc.id, data)
                }
                EventType.NORMAL_TASK.name -> {
                    AgendaEvent.NormalTask.fromMap(doc.id, data)
                }
                else -> null
            }

            Result.success(event)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo evento: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ==================== NUEVAS FUNCIONES PARA CALENDARIO ====================

    /**
     * ✅ CORREGIDO: Obtiene TODOS los eventos de los adultos mayores asignados al cuidador
     * en un rango de fechas (para cuando se selecciona "Todos los adultos mayores")
     */
    suspend fun getAllEventsByDateRange(
        startDate: Long,
        endDate: Long
    ): List<AgendaEvent> {
        return try {
            val currentUser = auth.currentUser
                ?: throw Exception("Usuario no autenticado")

            // ✅ PASO 1: Obtener los IDs de adultos mayores asignados al cuidador
            val relationshipsSnapshot = db.collection("relationships")
                .whereEqualTo("caregiverId", currentUser.uid)
                .get()
                .await()

            val elderlyIds = relationshipsSnapshot.documents.mapNotNull {
                it.getString("elderlyId")
            }

            if (elderlyIds.isEmpty()) {
                Log.w(TAG, "⚠️ No hay adultos mayores asignados")
                return emptyList()
            }

            Log.d(TAG, "✅ Adultos mayores encontrados: ${elderlyIds.size}")

            // ✅ PASO 2: Obtener eventos de TODOS esos adultos mayores
            // Firestore permite máximo 10 items en whereIn, así que dividimos si hay más
            val allEvents = mutableListOf<AgendaEvent>()

            elderlyIds.chunked(10).forEach { chunk ->
                val snapshot = db.collection(EVENTS_COLLECTION)
                    .whereIn("elderlyId", chunk)  // ✅ Filtrar por elderlyId, no createdBy
                    .whereGreaterThanOrEqualTo("date", startDate)
                    .whereLessThanOrEqualTo("date", endDate)
                    .get()
                    .await()

                snapshot.documents.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val type = data["type"] as? String

                    when (type) {
                        EventType.MEDICAL_APPOINTMENT.name -> {
                            AgendaEvent.MedicalAppointment.fromMap(doc.id, data)?.let {
                                allEvents.add(it)
                            }
                        }
                        EventType.NORMAL_TASK.name -> {
                            AgendaEvent.NormalTask.fromMap(doc.id, data)?.let {
                                allEvents.add(it)
                            }
                        }
                    }
                }
            }

            // Ordenar por fecha
            val sortedEvents = allEvents.sortedBy { event ->
                when (event) {
                    is AgendaEvent.MedicalAppointment -> event.date
                    is AgendaEvent.NormalTask -> event.date
                }
            }

            Log.d(TAG, "✅ ${sortedEvents.size} eventos obtenidos en rango de fechas")
            sortedEvents

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo eventos por rango: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * ✅ NUEVA: Obtiene eventos de un adulto mayor específico en un rango de fechas
     * Para cuando se selecciona un adulto mayor específico del spinner
     */
    suspend fun getEventsByElderlyAndDate(
        elderlyId: String,
        startDate: Long,
        endDate: Long
    ): List<AgendaEvent> {
        return try {
            val snapshot = db.collection(EVENTS_COLLECTION)
                .whereEqualTo("elderlyId", elderlyId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()

            val events = mutableListOf<AgendaEvent>()
            snapshot.documents.forEach { doc ->
                val data = doc.data ?: return@forEach
                val type = data["type"] as? String

                when (type) {
                    EventType.MEDICAL_APPOINTMENT.name -> {
                        AgendaEvent.MedicalAppointment.fromMap(doc.id, data)?.let {
                            events.add(it)
                        }
                    }
                    EventType.NORMAL_TASK.name -> {
                        AgendaEvent.NormalTask.fromMap(doc.id, data)?.let {
                            events.add(it)
                        }
                    }
                }
            }

            Log.d(TAG, "✅ ${events.size} eventos obtenidos para adulto mayor: $elderlyId")
            events

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo eventos por adulto mayor: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * ✅ NUEVA: Obtiene TODOS los eventos del cuidador (sin importar fecha)
     * Para la versión antigua que usa tabs
     */
    suspend fun getAllEvents(): List<AgendaEvent> {
        return try {
            val currentUser = auth.currentUser
                ?: throw Exception("Usuario no autenticado")

            val snapshot = db.collection(EVENTS_COLLECTION)
                .whereEqualTo("createdBy", currentUser.uid)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()

            val events = mutableListOf<AgendaEvent>()
            snapshot.documents.forEach { doc ->
                val data = doc.data ?: return@forEach
                val type = data["type"] as? String

                when (type) {
                    EventType.MEDICAL_APPOINTMENT.name -> {
                        AgendaEvent.MedicalAppointment.fromMap(doc.id, data)?.let {
                            events.add(it)
                        }
                    }
                    EventType.NORMAL_TASK.name -> {
                        AgendaEvent.NormalTask.fromMap(doc.id, data)?.let {
                            events.add(it)
                        }
                    }
                }
            }

            Log.d(TAG, "✅ ${events.size} eventos totales obtenidos")
            events

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo todos los eventos: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * ✅ NUEVA: Actualiza el estado de completado de una tarea
     * Versión simplificada para el checkbox del adapter
     */
    suspend fun updateTaskCompletionStatus(
        taskId: String,
        isCompleted: Boolean
    ) {
        try {
            val currentUser = auth.currentUser
                ?: throw Exception("Usuario no autenticado")

            val updates = if (isCompleted) {
                mapOf(
                    "isCompleted" to true,
                    "completedAt" to System.currentTimeMillis(),
                    "completedBy" to currentUser.uid,
                    "updatedAt" to System.currentTimeMillis()
                )
            } else {
                mapOf(
                    "isCompleted" to false,
                    "completedAt" to null,
                    "completedBy" to null,
                    "updatedAt" to System.currentTimeMillis()
                )
            }

            db.collection(EVENTS_COLLECTION)
                .document(taskId)
                .update(updates)
                .await()

            Log.d(TAG, "✅ Estado de tarea actualizado: $taskId -> $isCompleted")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando estado de tarea: ${e.message}", e)
            throw e
        }
    }

    /**
     * : Obtiene las citas médicas del adulto mayor para el día actual
     */
    suspend fun getTodayMedicalAppointmentsForElderly(elderlyId: String): List<AgendaEvent.MedicalAppointment> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = calendar.timeInMillis

            Log.d(TAG, "🔍 Buscando citas médicas para elderlyId: $elderlyId")
            Log.d(TAG, "📅 Rango: ${Date(startOfDay)} a ${Date(endOfDay)}")

            val snapshot = db.collection(EVENTS_COLLECTION)
                .whereEqualTo("elderlyId", elderlyId)
                .whereEqualTo("type", EventType.MEDICAL_APPOINTMENT.name)
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThanOrEqualTo("date", endOfDay)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()

            val appointments = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                AgendaEvent.MedicalAppointment.fromMap(doc.id, data)
            }

            Log.d(TAG, "✅ ${appointments.size} citas médicas encontradas para hoy")
            appointments

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo citas médicas del día: ${e.message}", e)
            emptyList()
        }
    }
}
