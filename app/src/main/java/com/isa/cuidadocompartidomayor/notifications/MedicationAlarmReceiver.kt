package com.isa.cuidadocompartidomayor.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.isa.cuidadocompartidomayor.data.model.CaregiverNotification
import com.isa.cuidadocompartidomayor.data.repository.MedicationRepository
import com.isa.cuidadocompartidomayor.utils.MedicationAlarmManager
import com.isa.cuidadocompartidomayor.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MedicationAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📢 Alarma recibida: ${intent.action}")

        when (intent.action) {
            "MEDICATION_ALARM" -> {
                handleMedicationAlarm(context, intent)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                handleBootCompleted(context)
            }
        }
    }

    /**
     * Muestra notificación local al adulto mayor Y notifica al cuidador vía Firestore
     */
    private fun handleMedicationAlarm(context: Context, intent: Intent) {
        val medicationId = intent.getStringExtra("MEDICATION_ID") ?: return
        val medicationName = intent.getStringExtra("MEDICATION_NAME") ?: "Medicamento"
        val dosage = intent.getStringExtra("MEDICATION_DOSAGE") ?: ""
        val time = intent.getStringExtra("MEDICATION_TIME") ?: ""
        val elderlyName = intent.getStringExtra("ELDERLY_NAME") ?: "Adulto Mayor"
        val caregiverId = intent.getStringExtra("CAREGIVER_ID") ?: ""
        val elderlyId = intent.getStringExtra("ELDERLY_ID") ?: ""

        // 1. Mostrar notificación LOCAL al adulto mayor
        NotificationHelper.showMedicationNotification(
            context = context,
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = dosage,
            time = time,
            elderlyName = elderlyName
        )

        Log.d(TAG, "✅ Notificación local mostrada para $medicationName a las $time")

        // 2. : Notificar al cuidador vía Firestore (GRATIS)
        if (caregiverId.isNotEmpty()) {
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    sendNotificationToCaregiver(
                        caregiverId = caregiverId,
                        elderlyId = elderlyId,
                        elderlyName = elderlyName,
                        medicationId = medicationId,
                        medicationName = medicationName,
                        dosage = dosage,
                        time = time,
                        type = "REMINDER"
                    )

                    Log.d(TAG, "✅ Notificación registrada en Firestore para cuidador")

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error registrando notificación en Firestore", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    /**
     * Crea una notificación en Firestore para el cuidador
     */
    private suspend fun sendNotificationToCaregiver(
        caregiverId: String,
        elderlyId: String,
        elderlyName: String,
        medicationId: String,
        medicationName: String,
        dosage: String,
        time: String,
        type: String
    ) {
        val db = FirebaseFirestore.getInstance()

        val title = when (type) {
            "REMINDER" -> "💊 Recordatorio de medicamento para $elderlyName"
            "MISSED" -> "⚠️ Medicamento no tomado: $elderlyName"
            "TAKEN" -> "✅ Medicamento tomado: $elderlyName"
            else -> "Notificación: $elderlyName"
        }

        val body = when (type) {
            "REMINDER" -> "Es hora de que $elderlyName tome $medicationName ($dosage), programado a las $time"
            "MISSED" -> "$elderlyName NO ha tomado $medicationName programado a las $time"
            "TAKEN" -> "$elderlyName ha tomado $medicationName a las $time"
            else -> "Actualización sobre medicamento $medicationName"
        }

        val notification = CaregiverNotification(
            id = "${caregiverId}_${System.currentTimeMillis()}",
            caregiverId = caregiverId,
            elderlyId = elderlyId,
            elderlyName = elderlyName,
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = dosage,
            time = time,
            type = type,
            title = title,
            body = body,
            timestamp = System.currentTimeMillis(),
            read = false
        )

        db.collection("caregiverNotifications")
            .document(notification.id)
            .set(notification)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Notificación guardada en Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error guardando notificación", e)
            }
    }

    /**
     * Reprograma alarmas cuando el dispositivo se reinicia
     */
    private fun handleBootCompleted(context: Context) {
        Log.d(TAG, "🔄 Dispositivo reiniciado - Iniciando reprogramación de alarmas")

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(TAG, "⚠️ No hay usuario autenticado")
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = MedicationRepository(context.applicationContext)
                val result = repository.getActiveMedicationsByElderly(currentUser.uid)

                result.onSuccess { medications ->
                    Log.d(TAG, "📋 Medicamentos activos: ${medications.size}")

                    medications.forEach { medication ->
                        try {
                            MedicationAlarmManager.scheduleMedicationAlarms(context, medication)
                            Log.d(TAG, "✅ Alarmas reprogramadas: ${medication.name}")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error reprogramando: ${medication.name}", e)
                        }
                    }

                    Log.d(TAG, "🎉 Reprogramación completada")
                }.onFailure { error ->
                    Log.e(TAG, "❌ Error cargando medicamentos", error)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error crítico", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
