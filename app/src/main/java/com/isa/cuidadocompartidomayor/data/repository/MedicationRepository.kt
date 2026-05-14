package com.isa.cuidadocompartidomayor.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.isa.cuidadocompartidomayor.data.model.*
import kotlinx.coroutines.tasks.await
import java.util.*
import com.isa.cuidadocompartidomayor.utils.MedicationAlarmManager
import kotlin.math.abs

class MedicationRepository (private val context: Context){
    private val db = FirebaseFirestore.getInstance()
    private val medicationsCollection = db.collection("medications")
    private val logsCollection = db.collection("medicationLogs")

    private val auth = FirebaseAuth.getInstance() // Warning message: Property auth is never used

    companion object {
        private const val TAG = "MedicationRepository"
    }

    /** Crea un nuevo medicamento (2 usages en AddMedicationViewModel.kt en la funcion saveMedication() y MedicationViewModel.kt en la funcion createMedication() */
    suspend fun createMedication(medication: Medication): Result<Medication> {
        return try {
            val medicationId = medicationsCollection.document().id
            val newMedication = medication.copy(
                id = medicationId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            medicationsCollection.document(medicationId).set(newMedication).await()

            // Crear logs iniciales para las próximas tomas
            createInitialLogs(newMedication)

            // Programar notificaciones para este medicamento
            try {
                MedicationAlarmManager.scheduleMedicationAlarms(context, newMedication)
                Log.d(TAG, "🔔 Alarmas programadas para: ${newMedication.name}")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error programando alarmas (no crítico)", e)
                // No fallar la creación del medicamento si falla la alarma
            }

            Log.d(TAG, "✅ Medicamento creado: ${newMedication.name} (${newMedication.id})")
            Result.success(newMedication)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando medicamento", e)
            Result.failure(e)
        }
    }

    /** Actualiza un medicamento existente (1 usage en MedicationViewModel.kt en la funcion updateMedication())*/
    suspend fun updateMedication(medication: Medication): Result<Medication> {
        return try {
            val updatedMedication = medication.copy(
                updatedAt = System.currentTimeMillis()
            )
            medicationsCollection.document(medication.id).set(updatedMedication).await()

            // Actualizar logs
            updateAllMedicationLogs(updatedMedication)

            // Cancelar alarmas antiguas
            try {
                MedicationAlarmManager.cancelMedicationAlarms(context, medication)
                Log.d(TAG, "🔕 Alarmas antiguas canceladas para: ${medication.name}")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error cancelando alarmas antiguas", e)
            }

            // Reprogramar alarmas con nuevos datos
            try {
                MedicationAlarmManager.scheduleMedicationAlarms(context, updatedMedication)
                Log.d(TAG, "🔔 Alarmas reprogramadas para: ${updatedMedication.name}")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error reprogramando alarmas (no crítico)", e)
            }

            Log.d(TAG, "✅ Medicamento actualizado: ${medication.name}")
            Result.success(updatedMedication)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando medicamento", e)
            Result.failure(e)
        }
    }

    /** Elimina un medicamento para ponerlo como inactivo (1 usage en MedicationViewModel.kt en la funcion deleteMedication())
    suspend fun deleteMedication(medicationId: String): Result<Unit> {
        return try {
            // Obtener el medicamento antes de eliminarlo (para cancelar alarmas)
            val medicationSnapshot = medicationsCollection.document(medicationId).get().await()
            val medication = medicationSnapshot.toObject(Medication::class.java)
            //marcar como inactivo
            medicationsCollection.document(medicationId).update(
                mapOf(
                    "active" to false,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            //  Cancelar todas las alarmas programadas
            if (medication != null) {
                try {
                    MedicationAlarmManager.cancelMedicationAlarms(context, medication)
                    Log.d(TAG, "🔕 Alarmas canceladas para: ${medication.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Error cancelando alarmas", e)
                }
            }

            Log.d(TAG, "✅ Medicamento eliminado: $medicationId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando medicamento", e)
            Result.failure(e)
        }
    }*/

    /** Elimina COMPLETAMENTE un medicamento y sus logs asociados de Firestore Esta acción es PERMANENTE y no se puede deshacer */
    suspend fun deleteMedication(medicationId: String): Result<Unit> {
        return try {
            // 1. Obtener el medicamento antes de eliminarlo
            val medicationSnapshot = medicationsCollection.document(medicationId).get().await()
            val medication = medicationSnapshot.toObject(Medication::class.java)

            if (medication == null) {
                Log.w(TAG, "⚠️ Medicamento no encontrado: $medicationId")
                return Result.failure(Exception("Medicamento no encontrado"))
            }

            // 2. Cancelar todas las alarmas programadas
            try {
                MedicationAlarmManager.cancelMedicationAlarms(context, medication)
                Log.d(TAG, "🔕 Alarmas canceladas para: ${medication.name}")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error cancelando alarmas", e)
            }

            // 3. Eliminar todos los logs asociados al medicamento
            try {
                val logsSnapshot = logsCollection
                    .whereEqualTo("medicationId", medicationId)
                    .get()
                    .await()

                val batch = db.batch()
                logsSnapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()

                Log.d(TAG, "🗑️ ${logsSnapshot.size()} logs eliminados para: ${medication.name}")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error eliminando logs asociados", e)
            }

            // 4. Eliminar el documento del medicamento
            medicationsCollection.document(medicationId).delete().await()

            Log.d(TAG, "✅ Medicamento eliminado completamente: $medicationId (${medication.name})")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando medicamento", e)
            Result.failure(e)
        }
    }

    /** Obtiene un medicamento por ID (2 usages en MedicationViewModel.kt en las funciones deletMedication() y loadMedicationById())*/
    suspend fun getMedicationById(medicationId: String): Result<Medication?> {
        return try {
            val snapshot = medicationsCollection.document(medicationId).get().await()
            val medication = snapshot.toObject(Medication::class.java)

            Log.d(TAG, "✅ Medicamento obtenido: ${medication?.name ?: "null"}")
            Result.success(medication)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo medicamento", e)
            Result.failure(e)
        }
    }

    /** Obtiene todos los medicamentos activos de un adulto mayor (2 usages: MedicationAlarmReceiver.kt en la funcion handleBootCompleted() y MedicationLogViewModel.kt en la funcion loadActiveMedications())*/
    suspend fun getActiveMedicationsByElderly(elderlyId: String): Result<List<Medication>> {
        return try {
            val snapshot = medicationsCollection
                .whereEqualTo("elderlyId", elderlyId)
                .whereEqualTo("active", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val medications = snapshot.documents.mapNotNull {
                it.toObject(Medication::class.java)
            }

            Log.d(TAG, "✅ Medicamentos activos obtenidos para elderly $elderlyId: ${medications.size}")
            Result.success(medications)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo medicamentos activos", e)
            Result.failure(e)
        }
    }

    /** Obtiene medicamentos activos de un cuidador de todos sus pacientes. (1 usage en MedicationViewModel.kt en la funcion loadActiveMedicationsByCaregiver()) */
    suspend fun getActiveMedicationsByCaregiver(caregiverId: String): Result<List<Medication>> {
        return try {
            // 1. Obtener elderly IDs del caregiver
            val relationshipsSnapshot = db.collection("relationships")
                .whereEqualTo("caregiverId", caregiverId)
                .whereEqualTo("status", "active")
                .get()
                .await()

            val elderlyIds = relationshipsSnapshot.documents.mapNotNull { doc ->
                doc.getString("elderlyId")
            }

            if (elderlyIds.isEmpty()) {
                Log.d(TAG, "⚠️ No hay pacientes conectados")
                return Result.success(emptyList())
            }

            // Sin filtro de caregiverId - todos ven todos los meds del paciente
            val allMedications = mutableListOf<Medication>()
            for (elderlyId in elderlyIds) {
                val medicationsSnapshot = medicationsCollection
                    .whereEqualTo("elderlyId", elderlyId)
                    .whereEqualTo("active", true)
                    .get()
                    .await()

                val medications = medicationsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Medication::class.java)
                }

                allMedications.addAll(medications)
            }

            Log.d(TAG, "✅ Medicamentos del caregiver $caregiverId: ${allMedications.size}")
            Result.success(allMedications.toList())
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo medicamentos del caregiver", e)
            Result.failure(e)
        }
    }

    /** Crea logs iniciales para las próximas 7 días de tomas (2 usages en MedicationRepository.kt en las funciones createMedication() y updatePendingLogs())*/
    private suspend fun createInitialLogs(medication: Medication) {
        try {
            val logs = generateLogsForNextDays(medication, 7)

            val batch = db.batch()
            logs.forEach { log ->
                val logRef = logsCollection.document()

                // Usar HashMap para control total de campos
                val logData = hashMapOf(
                    "id" to logRef.id,
                    "medicationId" to log.medicationId,
                    "medicationName" to log.medicationName,
                    "dosage" to log.dosage,
                    "frequency" to log.frequency,
                    "scheduledTime" to log.scheduledTime,
                    "takenAt" to log.takenAt,
                    "statusString" to log.statusString,
                    "elderlyId" to log.elderlyId,
                    "elderlyName" to log.elderlyName,
                    "caregiverId" to log.caregiverId,
                    "caregiverName" to log.caregiverName,
                    "confirmedBy" to log.confirmedBy,
                    "createdAt" to log.createdAt,
                    "medicationActive" to log.medicationActive
                )

                batch.set(logRef, logData)
            }
            batch.commit().await()

            Log.d(TAG, "✅ Logs iniciales creados: ${logs.size} logs")

            // Actualizar nextScheduledDateTime después de crear logs
            updateMedicationNextDose(medication.id)

        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error creando logs iniciales", e)
        }
    }

    /** 2 usages en MedicationRepository.kt en las funciones createInitialLogs(), updateAllMedicationLogs() y 1 usage en MedicationLogGeneratorWorker.kt*/
    internal fun generateLogsForNextDays( medication: Medication, days: Int, startDay: Int = 0 ): List<MedicationLog> {// ✅ Valor por defecto = 0 (empieza desde hoy)
        val logs = mutableListOf<MedicationLog>()
        val calendar = Calendar.getInstance()

        // Avanzar al día de inicio
        if (startDay > 0) {
            calendar.add(Calendar.DAY_OF_YEAR, startDay)
        }

        for (day in 0 until days) {
            val currentDayCal = calendar.clone() as Calendar

            // Verificar si el medicamento aplica para este día
            if (shouldTakeMedicationOnDay(medication, currentDayCal)) {
                // Validar si el día está dentro del rango startDate / endDate
                val dayTime = currentDayCal.timeInMillis
                
                // Normalizar a medianoche para comparación de fechas
                val dayMidnight = currentDayCal.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val startMidnight = Calendar.getInstance().apply {
                    timeInMillis = medication.startDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                if (dayMidnight >= startMidnight && (medication.endDate == null || dayMidnight <= medication.endDate)) {
                    medication.scheduledTimes.forEach { time ->
                        val scheduledTimestamp = getTimestampForTime(currentDayCal, time)

                        logs.add(
                            MedicationLog(
                                medicationId = medication.id,
                                medicationName = medication.name,
                                dosage = medication.dosage,
                                frequency = medication.getFrequencyText(),
                                scheduledTime = scheduledTimestamp,
                                statusString = LogStatus.PENDING.name,
                                elderlyId = medication.elderlyId,
                                elderlyName = medication.elderlyName,
                                caregiverId = medication.caregiverId,
                                caregiverName = medication.caregiverName
                            )
                        )
                    }
                }
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return logs
    }

    /** Verifica si el medicamento debe tomarse en un día específico (1 usage en MedicationRepository.kt en la funcion generateLogsForNextDays())*/
    private fun shouldTakeMedicationOnDay(medication: Medication, calendar: Calendar): Boolean {
        return when (medication.frequencyType) {
            FrequencyType.ONCE_DAILY, FrequencyType.MULTIPLE_DAILY -> true

            FrequencyType.SPECIFIC_DAYS -> {
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val dayIndex = when (dayOfWeek) {
                    Calendar.MONDAY -> 1
                    Calendar.TUESDAY -> 2
                    Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4
                    Calendar.FRIDAY -> 5
                    Calendar.SATURDAY -> 6
                    Calendar.SUNDAY -> 7
                    else -> 0
                }
                medication.weekDays.contains(dayIndex)
            }

            FrequencyType.EVERY_X_DAYS -> {
                // Normalizar ambas fechas a medianoche para cálculo exacto de días
                val currentMidnight = (calendar.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val startMidnight = Calendar.getInstance().apply {
                    timeInMillis = medication.startDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val daysSinceStart = ((currentMidnight - startMidnight) / (1000 * 60 * 60 * 24)).toInt()
                daysSinceStart >= 0 && daysSinceStart % (medication.intervalDays ?: 1) == 0
            }

            FrequencyType.EVERY_X_WEEKS -> {
                val currentMidnight = (calendar.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val startMidnight = Calendar.getInstance().apply {
                    timeInMillis = medication.startDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val weeksSinceStart = ((currentMidnight - startMidnight) / (1000 * 60 * 60 * 24 * 7)).toInt()
                weeksSinceStart >= 0 && weeksSinceStart % (medication.intervalWeeks ?: 1) == 0
            }

            FrequencyType.EVERY_X_MONTHS -> {
                val startCal = Calendar.getInstance().apply { timeInMillis = medication.startDate }
                val monthsSinceStart = (calendar.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)) * 12 +
                        (calendar.get(Calendar.MONTH) - startCal.get(Calendar.MONTH))
                monthsSinceStart % (medication.intervalMonths ?: 1) == 0
            }
        }
    }

    /** Convierte hora "HH:mm" a timestamp del día especificado (1 un usage en MedicationRepository.kt en la funcion generateLogsForNextDays())*/
    private fun getTimestampForTime(calendar: Calendar, time: String): Long {
        val timeParts = time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return cal.timeInMillis
    }

    /** Actualiza TODOS los logs de un medicamento (confirmados, omitidos y pendientes) cuando se edita el medicamento (1 usage en MedicationRepository.kt en la funcion updateMedication())*/
    private suspend fun updateAllMedicationLogs(medication: Medication) {
        try {
            // Obtener el rango de hoy (00:00 - 23:59:59)
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val todayEnd = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            // 1️⃣ ACTUALIZAR logs del día actual (solo PENDING)
            // Eliminar logs de hoy antes de actualizar
            val todayLogsSnapshot = logsCollection
                .whereEqualTo("medicationId", medication.id)
                .whereEqualTo("elderlyId", medication.elderlyId)
                .whereEqualTo("statusString", LogStatus.PENDING.name)
                .whereGreaterThanOrEqualTo("scheduledTime", todayStart)
                .whereLessThanOrEqualTo("scheduledTime", todayEnd)
                .get()
                .await()

            Log.d(TAG, "🔄 Eliminando ${todayLogsSnapshot.size()} logs PENDING del día actual antes de recrearlos")

            val deleteBatch = db.batch()
            todayLogsSnapshot.documents.forEach { doc ->
                deleteBatch.delete(doc.reference)
            }
            if (todayLogsSnapshot.size() > 0) {
                deleteBatch.commit().await()
            }

            // 1.5️⃣ ACTUALIZAR logs confirmados de HOY (que no fueron borrados en Paso 1)
            val todayConfirmedSnapshot = logsCollection
                .whereEqualTo("medicationId", medication.id)
                .whereEqualTo("elderlyId", medication.elderlyId)
                .whereGreaterThanOrEqualTo("scheduledTime", todayStart)
                .whereLessThanOrEqualTo("scheduledTime", todayEnd)
                .get()
                .await()

            val todayUpdateBatch = db.batch()
            var todayUpdatedCount = 0
            for (doc in todayConfirmedSnapshot.documents) {
                if (doc.getString("statusString") != LogStatus.PENDING.name) {
                    val updates = hashMapOf<String, Any>(
                        "medicationName" to medication.name,
                        "dosage" to medication.dosage,
                        "frequency" to medication.getFrequencyText()
                    )
                    todayUpdateBatch.update(doc.reference, updates)
                    todayUpdatedCount++
                }
            }
            if (todayUpdatedCount > 0) {
                todayUpdateBatch.commit().await()
                Log.d(TAG, "🔄 Actualizados $todayUpdatedCount logs confirmados de hoy")
            }

            // 2️⃣ ACTUALIZAR logs pasados (confirmados/omitidos de días anteriores)
            val pastLogsSnapshot = logsCollection
                .whereEqualTo("medicationId", medication.id)
                .whereEqualTo("elderlyId", medication.elderlyId)
                .whereLessThan("scheduledTime", todayStart)
                .get()
                .await()

            Log.d(TAG, "🔄 Actualizando ${pastLogsSnapshot.size()} logs pasados")

            val updateBatch = db.batch()
            for (doc in pastLogsSnapshot.documents) {
                val updates = hashMapOf<String, Any>(
                    "medicationName" to medication.name,
                    "dosage" to medication.dosage,
                    "frequency" to medication.getFrequencyText()
                )
                updateBatch.update(logsCollection.document(doc.id), updates)
            }
            if (pastLogsSnapshot.size() > 0) {
                updateBatch.commit().await()
            }

            // 3️⃣ ELIMINAR logs pendientes futuros (después de hoy)
            val futurePendingLogsSnapshot = logsCollection
                .whereEqualTo("medicationId", medication.id)
                .whereEqualTo("elderlyId", medication.elderlyId)
                .whereEqualTo("statusString", LogStatus.PENDING.name)
                .whereGreaterThan("scheduledTime", todayEnd)
                .get()
                .await()

            Log.d(TAG, "🗑️ Eliminando ${futurePendingLogsSnapshot.size()} logs pendientes futuros")

            val futureBatch = db.batch()
            futurePendingLogsSnapshot.documents.forEach { doc ->
                futureBatch.delete(doc.reference)
            }
            if (futurePendingLogsSnapshot.size() > 0) {
                futureBatch.commit().await()
            }

            // 4️⃣ CREAR nuevos logs futuros (hoy + próximos 6 días = 7 días total)
            // Generar logs desde hoy (no desde mañana)
            val newLogs = generateLogsForNextDays(
                medication = medication,
                days = 7  // Hoy + 6 días futuros
            )

            // Obtener logs confirmados de hoy para evitar duplicados (CAMBIO 2)
            val confirmedLogsSnapshot = logsCollection
                .whereEqualTo("medicationId", medication.id)
                .whereEqualTo("elderlyId", medication.elderlyId)
                .whereGreaterThanOrEqualTo("scheduledTime", todayStart)
                .whereLessThanOrEqualTo("scheduledTime", todayEnd)
                .get()
                .await()

            val hasConfirmedLogToday = confirmedLogsSnapshot.documents
                .any { it.getString("statusString") != LogStatus.PENDING.name }

            // Guardar los nuevos logs
            val createBatch = db.batch()
            newLogs.forEach { log ->
                // Omitir si ya existe un log confirmado para esta hora hoy (CAMBIO 2)
                val isForToday = log.scheduledTime in todayStart..todayEnd
                if (isForToday && hasConfirmedLogToday) {
                    Log.d(TAG, "⏭️ Omitiendo recreación de logs para hoy (ya existe una toma confirmada)")
                    return@forEach
                }
                val logRef = logsCollection.document()
                val logData = hashMapOf(
                    "id" to logRef.id,
                    "medicationId" to log.medicationId,
                    "medicationName" to log.medicationName,
                    "dosage" to log.dosage,
                    "frequency" to log.frequency,
                    "scheduledTime" to log.scheduledTime,
                    "takenAt" to log.takenAt,
                    "statusString" to log.statusString,
                    "elderlyId" to log.elderlyId,
                    "elderlyName" to log.elderlyName,
                    "caregiverId" to log.caregiverId,
                    "caregiverName" to log.caregiverName,
                    "confirmedBy" to log.confirmedBy,
                    "createdAt" to log.createdAt,
                    "medicationActive" to log.medicationActive
                )
                createBatch.set(logRef, logData)
            }
            if (newLogs.isNotEmpty()) {
                createBatch.commit().await()
            }

            Log.d(TAG, "✅ Logs futuros creados: ${newLogs.size} logs")
            Log.d(TAG, "✅ Todos los logs actualizados correctamente")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando logs", e)
            throw e
        }
    }

    /** Obtiene solo logs PASADOS y del DÍA ACTUAL con info de medicamento activo/inactivo (1 usage en MedicationViewModel.kt en la funcion loadMedicationLogsByCaregiver())*/
    suspend fun getMedicationLogsByCaregiver(caregiverId: String): Result<List<MedicationLog>> {
        return try {
            // 1. Obtener todos los elderly IDs relacionados
            val relationshipsSnapshot = db.collection("relationships")
                .whereEqualTo("caregiverId", caregiverId)
                .whereEqualTo("status", "active")
                .get()
                .await()

            val elderlyIds = relationshipsSnapshot.documents.mapNotNull { doc ->
                doc.getString("elderlyId")
            }

            if (elderlyIds.isEmpty()) {
                return Result.success(emptyList())
            }

            // Actualizar logs vencidos a MISSED para cada paciente
            elderlyIds.forEach { elderlyId ->
                updateMissedLogs(elderlyId)
            }

            // 2. Obtener logs de TODOS los elderly
            val allLogs = mutableListOf<MedicationLog>()

            // Fin del día de hoy
            val endOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            for (elderlyId in elderlyIds) {
                val logsSnapshot = logsCollection
                    .whereEqualTo("elderlyId", elderlyId)
                    .whereLessThanOrEqualTo("scheduledTime", endOfToday) // Solo hasta hoy
                    .orderBy("scheduledTime", Query.Direction.DESCENDING)
                    .limit(50) // Últimos 50 por paciente
                    .get()
                    .await()

                val logs = logsSnapshot.documents.mapNotNull { doc ->
                    val log = doc.toObject(MedicationLog::class.java)
                    // Verificar si el medicamento sigue activo
                    if (log != null) {
                        val medicationDoc = medicationsCollection
                            .document(log.medicationId)
                            .get()
                            .await()

                        val isActive = medicationDoc.getBoolean("active") ?: true
                        log.copy(medicationActive = isActive)  // Incluye estado del medicamento
                    } else {
                        null
                    }
                }

                allLogs.addAll(logs)
            }

            // 3. Ordenar por fecha descendente
            val sortedLogs = allLogs.sortedByDescending { it.scheduledTime }

            Log.d(TAG, "✅ Logs del caregiver (${sortedLogs.size} de ${elderlyIds.size} pacientes)")
            Result.success(sortedLogs)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo logs del caregiver", e)
            Result.failure(e)
        }
    }

    /** Obtiene el timestamp de la hora programada más cercana (2 usages en MedicationRepository.kt en las funciones confirmMedicationTake() y revertLogToPending() )*/
    private suspend fun getScheduledTimeForNow(medicationId: String): Long {
        return try {
            val medication = medicationsCollection.document(medicationId).get().await()
                .toObject(Medication::class.java)

            if (medication == null) {
                return System.currentTimeMillis()
            }

            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY) // Warning message: Property currentHour is never used
            val currentMinute = now.get(Calendar.MINUTE)  // Warning message: Property currentMinute is never used

            // Buscar la hora programada más cercana
            val scheduledTimes = medication.scheduledTimes
            if (scheduledTimes.isEmpty()) {
                return now.timeInMillis
            }

            var closestTime: Calendar? = null
            var minDifference = Long.MAX_VALUE

            for (timeStr in scheduledTimes) {
                val parts = timeStr.split(":")
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()

                val scheduledCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val difference = abs(now.timeInMillis - scheduledCal.timeInMillis)
                if (difference < minDifference) {
                    minDifference = difference
                    closestTime = scheduledCal
                }
            }

            closestTime?.timeInMillis ?: now.timeInMillis
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    /** Calcula el status del log basado en la diferencia de tiempo (1 Usage en MedicationRepository.kt en la funcion confirmMedicationTaken() )*/
    private fun calculateLogStatus(scheduledTime: Long, takenAt: Long): LogStatus {
        val difference = takenAt - scheduledTime
        val minutesDifference = difference / (1000 * 60)

        return when {
            minutesDifference <= 15 -> LogStatus.ON_TIME  // Dentro de 15 minutos
            minutesDifference <= 60 -> LogStatus.LATE     // Hasta 1 hora de retraso
            else -> LogStatus.LATE                         // Más de 1 hora
        }
    }

    /**
     * Busca logs pendientes y pasados, y los actualiza a MISSED
     * PROTECCIÓN HASTA FIN DEL DÍA: Solo marca como MISSED si fue modificado en un día anterior
     */
    private suspend fun updateMissedLogs(elderlyId: String) {
        try {
            val now = System.currentTimeMillis()
            val tolerance = 45 * 60 * 1000  // 45 minutos de gracia

            val pendingLogsSnapshot = logsCollection
                .whereEqualTo("elderlyId", elderlyId)
                .whereEqualTo("statusString", LogStatus.PENDING.name)
                .whereLessThan("scheduledTime", now - tolerance)
                .get()
                .await()

            if (pendingLogsSnapshot.isEmpty) return

            // Calcular inicio del día actual (00:00:00)
            val startOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val batch = db.batch()
            var updatedCount = 0

            pendingLogsSnapshot.documents.forEach { doc ->
                val log = doc.toObject(MedicationLog::class.java)

                if (log != null) {
                    // Verificar si el log fue modificado HOY
                    val wasModifiedToday = log.lastModifiedBy.isNotEmpty() &&
                            log.lastModifiedAt >= startOfToday

                    // Solo marcar como MISSED si:
                    // 1. NO fue confirmado antes (confirmedBy vacío)
                    // 2. NO fue modificado manualmente HOY (protección hasta fin del día)
                    if (log.confirmedBy.isNullOrEmpty() && !wasModifiedToday) {
                        batch.update(doc.reference, "statusString", LogStatus.MISSED.name)
                        updatedCount++
                    } else if (wasModifiedToday) {
                        Log.d(TAG, "⏳ Log ${doc.id} modificado HOY - protegido hasta medianoche")
                    }
                }
            }

            if (updatedCount > 0) {
                batch.commit().await()
                Log.d(TAG, "✅ $updatedCount logs actualizados a MISSED")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando logs a MISSED", e)
        }
    }

    /** Confirma que un medicamento fue tomado y crea/actualiza el log (1 usage en ElderlyMedicationViewModel.confirmMedicationTaken())*/
    suspend fun confirmMedicationTaken( medicationId: String, medicationName: String, dosage: String, frequency: String, elderlyId: String, elderlyName: String, caregiverId: String, caregiverName: String, confirmedBy: String ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()

            // Calcular el timestamp de la hora programada más cercana
            val scheduledTime = getScheduledTimeForNow(medicationId)

            // Determinar el status basado en la diferencia de tiempo
            val status = calculateLogStatus(scheduledTime, now)

            // Buscar si ya existe un log para esta toma
            val existingLog = logsCollection
                .whereEqualTo("medicationId", medicationId)
                .whereEqualTo("scheduledTime", scheduledTime)
                .get()
                .await()

            if (existingLog.isEmpty) {
                // Crear nuevo log
                val logId = logsCollection.document().id
                val medicationLog = MedicationLog(
                    id = logId,
                    medicationId = medicationId,
                    medicationName = medicationName,
                    dosage = dosage,
                    frequency = frequency,
                    scheduledTime = scheduledTime,
                    takenAt = now,
                    statusString = status.name,
                    elderlyId = elderlyId,
                    elderlyName = elderlyName,
                    caregiverId = caregiverId,
                    caregiverName = caregiverName,
                    confirmedBy = confirmedBy,
                    createdAt = now
                )

                logsCollection.document(logId).set(medicationLog).await()
                Log.d(TAG, "✅ Log de medicamento creado: $logId")
            } else {
                // Actualizar log existente
                val logDoc = existingLog.documents.first()
                logDoc.reference.update(
                    mapOf(
                        "takenAt" to now,
                        "statusString" to status.name,
                        "confirmedBy" to confirmedBy
                    )
                ).await()
                Log.d(TAG, "✅ Log de medicamento actualizado: ${logDoc.id}")
            }

            // Actualizar nextScheduledDateTime del medicamento
            updateMedicationNextDose(medicationId)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error confirmando medicamento", e)
            Result.failure(e)
        }
    }

    /** Obtiene la próxima dosis pendiente consultando los logs de Firebase (1 usage en MedicationRepository.kt en la funcion updateMedicationNextDose())*/
    private suspend fun getNextPendingDose(medicationId: String): Pair<Long, String>? {
        return try {
            val now = System.currentTimeMillis()

            // Buscar el primer log PENDING en el futuro
            val nextLogSnapshot = logsCollection
                .whereEqualTo("medicationId", medicationId)
                .whereEqualTo("statusString", LogStatus.PENDING.name)
                .whereGreaterThanOrEqualTo("scheduledTime", now)
                .orderBy("scheduledTime", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .await()

            if (nextLogSnapshot.documents.isNotEmpty()) {
                val log = nextLogSnapshot.documents[0].toObject(MedicationLog::class.java)
                if (log != null) {
                    val time = getTimeFromTimestamp(log.scheduledTime)
                    return Pair(log.scheduledTime, time)
                }
            }

            // Si no hay logs PENDING futuros, retornar null
            null

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo próxima dosis", e)
            null
        }
    }

    /** Actualiza el campo nextScheduledDateTime del medicamento en Firebase (6 usages en MedicationRepository.kt en las funciones confirmMedicationTaken(), confirmMedicationTakenByLogId(), createInitialLogs(), markMedicationAsTaken(), revertLogToPendingByLogId() y skipMedicationByLogId()*/
    private suspend fun updateMedicationNextDose(medicationId: String) {
        try {
            val nextDose = getNextPendingDose(medicationId)

            val updateData = if (nextDose != null) {
                hashMapOf<String, Any>(
                    "nextScheduledDateTime" to hashMapOf(
                        "first" to nextDose.first,
                        "second" to nextDose.second
                    )
                )
            } else {
                hashMapOf<String, Any>(
                    "nextScheduledDateTime" to FieldValue.delete()
                )
            }

            medicationsCollection.document(medicationId)
                .update(updateData)
                .await()

            Log.d(TAG, "✅ nextScheduledDateTime actualizado para medication: $medicationId")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando próxima dosis", e)
        }
    }

    /** Convierte un timestamp a formato "HH:mm" (2 usages en MedicationRepository.kt en las funciones getNextMedicationPendingDose() y getNextPendingMedication())*/
    private fun getTimeFromTimestamp(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return String.format("%02d:%02d", hour, minute)
    }

    /** Obtiene el próximo medicamento PENDING del adulto mayor. Solo devuelve medicamentos PENDING cuya hora programada sea DESPUÉS de la hora actual (1 usage en ElderlyMedicationViewModel.k en la funcion loadNextMedication())*/
    suspend fun getNextPendingMedication(elderlyId: String): Result<MedicationLog?> {
        return try {
            val now = System.currentTimeMillis()
            val tolerance = 45 * 60 * 1000 // 45 minutos de tolerancia

            // Obtener inicio y fin del día actual
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val todayEnd = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            // Buscar logs PENDING de HOY que estén DESPUÉS de la hora actual (con tolerancia)
            val snapshot = logsCollection
                .whereEqualTo("elderlyId", elderlyId)
                .whereEqualTo("statusString", LogStatus.PENDING.name)
                .whereGreaterThanOrEqualTo("scheduledTime", todayStart)
                .whereLessThanOrEqualTo("scheduledTime", todayEnd)
                .orderBy("scheduledTime", Query.Direction.ASCENDING)
                .get()
                .await()

            // Filtrar solo logs de medicamentos ACTIVOS y después de (hora_actual - tolerancia)
            val allLogs = snapshot.documents.mapNotNull { it.toObject(MedicationLog::class.java) }
            var nextLog: MedicationLog? = null
            for (log in allLogs) {
                // Verificar que la hora sea válida (después de hora_actual - tolerancia)
                if (log.scheduledTime >= (now - tolerance)) {

                    // Verificar que el medicamento esté ACTIVO
                    val medicationDoc = medicationsCollection
                        .document(log.medicationId)
                        .get()
                        .await()

                    val isActive = medicationDoc.getBoolean("active") ?: true

                    if (isActive) {
                        nextLog = log
                        break  // Encontramos el primer medicamento activo
                    }
                }
            }

            if (nextLog != null) {
                Log.d(TAG, "✅ Próximo medicamento pendiente: ${nextLog.medicationName} a las ${getTimeFromTimestamp(nextLog.scheduledTime)}")
            } else {
                Log.d(TAG, "ℹ️ No hay medicamentos pendientes para mostrar")
            }

            Result.success(nextLog)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo próximo medicamento pendiente", e)
            Result.failure(e)
        }
    }

    /** Obtiene TODOS los logs del día actual (no solo PENDING). Esto permite al cuidador gestionar medicamentos en cualquier estado (1 usage en MedicationViewModel.kt en la funcion loadUpcomingLogsForCaregiver()) */
    // limit = 30 Aumentado porque incluimos todos los estados
    suspend fun getUpcomingLogsByCaregiver( caregiverId: String, limit: Int = 30): Result<List<MedicationLog>> {
        return try {
            // 1. Obtener todos los elderly IDs relacionados
            val relationshipsSnapshot = db.collection("relationships")
                .whereEqualTo("caregiverId", caregiverId)
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

            // Actualizar logs atrasados a MISSED antes de cargar
            elderlyIds.forEach { elderlyId ->
                updateMissedLogs(elderlyId)
            }

            Log.d(TAG, "👥 Cargando TODOS los logs del día de ${elderlyIds.size} pacientes")

            // 2. Obtener rango del día actual
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val todayEnd = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            // 3. Obtener TODOS los logs de HOY (sin filtrar por status)
            val allLogs = mutableListOf<MedicationLog>()

            for (elderlyId in elderlyIds) {
                val logsSnapshot = logsCollection
                    .whereEqualTo("elderlyId", elderlyId)
                    .whereGreaterThanOrEqualTo("scheduledTime", todayStart)
                    .whereLessThanOrEqualTo("scheduledTime", todayEnd)
                    .orderBy("scheduledTime", Query.Direction.ASCENDING)
                    .get()
                    .await()

                val logs = logsSnapshot.documents.mapNotNull { doc ->
                    val log = doc.toObject(MedicationLog::class.java)

                    // Verificar que el medicamento esté ACTIVO
                    if (log != null) {
                        val medicationDoc = medicationsCollection
                            .document(log.medicationId)
                            .get()
                            .await()

                        val isActive = medicationDoc.getBoolean("active") ?: true

                        if (isActive) log else null
                    } else {
                        null
                    }
                }

                allLogs.addAll(logs)
            }

            // 4. Ordenar por hora y tomar los primeros N
            val sortedLogs = allLogs
                .sortedBy { it.scheduledTime }
                .take(limit)

            Log.d(TAG, "✅ Logs del día cargados: ${sortedLogs.size} (PENDING=${sortedLogs.count { it.statusString == "PENDING" }}, " +
                    "CONFIRMED=${sortedLogs.count { it.statusString == "ON_TIME" || it.statusString == "LATE" }}, " +
                    "SKIPPED=${sortedLogs.count { it.statusString == "MISSED" }})")

            Result.success(sortedLogs)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo logs del día", e)
            Result.failure(e)
        }
    }

    // ===== FUNCIONES POR LOG ID ===== //

    /** Confirma un medicamento usando el logId directamente (1 usage en MedicationViewModel.kt en la funcion confirmMedicationTaken()) */
    suspend fun confirmMedicationTakenByLogId( logId: String, confirmedBy: String ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()

            // Obtener el log
            val logDoc = logsCollection.document(logId).get().await()
            val log = logDoc.toObject(MedicationLog::class.java)
                ?: return Result.failure(Exception("Log no encontrado"))

            // Calcular status basado en la diferencia de tiempo
            val timeDifference = now - log.scheduledTime
            val status = if (timeDifference <= 45 * 60 * 1000) {  // 45 minutos
                LogStatus.ON_TIME
            } else {
                LogStatus.LATE
            }

            // Actualizar el log
            logsCollection.document(logId).update(
                mapOf(
                    "takenAt" to now,
                    "statusString" to status.name,
                    "confirmedBy" to confirmedBy
                )
            ).await()

            Log.d(TAG, "✅ Log confirmado: $logId - ${status.name}")

            // Actualizar nextScheduledDateTime
            updateMedicationNextDose(log.medicationId)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error confirmando log", e)
            Result.failure(e)
        }
    }

    /** Omite un medicamento usando el logId directamente (1 usage en MedicationViewModel.kt en la funcion skipMedication()) */
    suspend fun skipMedicationByLogId( logId: String, skippedBy: String ): Result<Unit> {
        return try {
            // Actualizar el log a MISSED
            logsCollection.document(logId).update(
                mapOf(
                    "statusString" to LogStatus.MISSED.name,
                    "confirmedBy" to skippedBy,
                    "takenAt" to null
                )
            ).await()

            Log.d(TAG, "✅ Log omitido: $logId")

            // Obtener medicationId para actualizar nextScheduledDateTime
            val logDoc = logsCollection.document(logId).get().await()
            val log = logDoc.toObject(MedicationLog::class.java)

            if (log != null) {
                updateMedicationNextDose(log.medicationId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error omitiendo log", e)
            Result.failure(e)
        }
    }

    /**  Revierte un log a PENDING usando el logId directamente.
     *    Registra quién hizo el cambio manual para evitar auto-MISSED
     *  (1 usage en MedicationViewModel.kt en la funcion setPendingMedication()) */
    suspend fun revertLogToPendingByLogId( logId: String, modifiedBy: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()

            // Revertir el log a PENDING y marcar como modificado manualmente
            logsCollection.document(logId).update(
                mapOf(
                    "statusString" to LogStatus.PENDING.name,
                    "takenAt" to null,
                    "confirmedBy" to null,
                    "lastModifiedBy" to modifiedBy,
                    "lastModifiedAt" to now
                )
            ).await()

            Log.d(TAG, "✅ Log revertido a PENDING manualmente: $logId por $modifiedBy")

            // Obtener medicationId para actualizar nextScheduledDateTime
            val logDoc = logsCollection.document(logId).get().await()
            val log = logDoc.toObject(MedicationLog::class.java)

            if (log != null) {
                updateMedicationNextDose(log.medicationId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error revirtiendo log a PENDING", e)
            Result.failure(e)
        }
    }
}