package com.isa.cuidadocompartidomayor.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.isa.cuidadocompartidomayor.data.model.Medication
import com.isa.cuidadocompartidomayor.data.repository.MedicationRepository
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

class MedicationLogGeneratorWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = FirebaseFirestore.getInstance()
    private val medicationRepository = MedicationRepository(context)
    private val TAG = "MedicationLogWorker"
    companion object {
        private var totalRuns = 0
    }

    override suspend fun doWork(): Result {
        return try {
            totalRuns++
            Log.d(TAG, "####### EJECUCIÓN NÚMERO: $totalRuns #######")
            Log.d(TAG, "Iniciando generación automática de logs de medicamentos...")

            // 1. Obtener todos los medicamentos activos de todos los usuarios usando collectionGroup
            val querySnapshot = db.collectionGroup("medications")
                .whereEqualTo("active", true)
                .get()
                .await()

            val medications = querySnapshot.documents.mapNotNull { it.toObject(Medication::class.java) }
            Log.d(TAG, "Se encontraron ${medications.size} medicamentos activos.")

            if (medications.isEmpty()) return Result.success()

            for (medication in medications) {
                generateMissingLogsForMedication(medication)
            }

            Log.d(TAG, "Generación de logs completada exitosamente.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error en MedicationLogGeneratorWorker: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun generateMissingLogsForMedication(medication: Medication) {
        try {
            // Reducir consultas a Firestore
            // Obtener el rango de tiempo de los logs potenciales (desde inicio de hoy hasta 10 días)
            val startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val tenDaysLater = startOfToday + (10 * 24 * 60 * 60 * 1000L) // +10 días para cubrir margen

            // Obtener logs existentes para este medicamento en ese rango
            val existingLogsSnapshot = db.collectionGroup("medicationLogs")
                .whereEqualTo("medicationId", medication.id)
                .whereGreaterThanOrEqualTo("scheduledTime", startOfToday)
                .whereLessThanOrEqualTo("scheduledTime", tenDaysLater)
                .get()
                .await()

            val existingScheduledTimes = existingLogsSnapshot.documents
                .mapNotNull { it.getLong("scheduledTime") }
                .toSet()

            // 1. Generar logs potenciales para los próximos 8 días (days=8)
            val potentialLogs = medicationRepository.generateLogsForNextDays(medication, 8)
            
            if (potentialLogs.isEmpty()) return

            val newLogsBatch = db.batch()
            var addedCount = 0

            for (log in potentialLogs) {
                // 2. Verificar contra el Set en memoria si ya existe el log
                if (!existingScheduledTimes.contains(log.scheduledTime)) {
                    // 3. Si no existe, agregarlo al batch
                    val logRef = db.collection("medicationLogs").document()
                    
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
                    
                    newLogsBatch.set(logRef, logData)
                    addedCount++
                }
            }

            // 4. Ejecutar el batch si hay nuevos logs
            if (addedCount > 0) {
                newLogsBatch.commit().await()
                Log.d(TAG, "Se crearon $addedCount nuevos logs para: ${medication.name}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error procesando medicamento ${medication.id}: ${e.message}")
        }
    }
}
