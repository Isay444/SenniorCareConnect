package com.isa.cuidadocompartidomayor.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.isa.cuidadocompartidomayor.data.model.Medication
import com.isa.cuidadocompartidomayor.notifications.MedicationAlarmReceiver
import java.util.Calendar

object MedicationAlarmManager {

    private const val TAG = "MedicationAlarmManager"

    /** Programa alarmas para un medicamento */
    fun scheduleMedicationAlarms(context: Context, medication: Medication) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        medication.scheduledTimes.forEach { time ->
            var alarmTime = getAlarmTimeForToday(time)

            if (alarmTime <= System.currentTimeMillis()) { // Si la hora ya pasó hoy, programar para mañana
                alarmTime += 24 * 60 * 60 * 1000 // +1 día
            }

            val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
                action = "MEDICATION_ALARM"
                putExtra("MEDICATION_ID", medication.id)
                putExtra("MEDICATION_NAME", medication.name)
                putExtra("MEDICATION_DOSAGE", medication.dosage)
                putExtra("MEDICATION_TIME", time)

                putExtra("ELDERLY_NAME", medication.elderlyName)
                putExtra("CAREGIVER_ID", medication.caregiverId)
                putExtra("ELDERLY_ID", medication.elderlyId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                "${medication.id}_$time".hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                // Usar alarma exacta
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alarmTime,
                            pendingIntent
                        )
                        Log.d(TAG, "✅ Alarma exacta programada para $time - ${medication.name}")
                    } else {
                        // Fallback a alarma inexacta
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alarmTime,
                            pendingIntent
                        )
                        Log.w(TAG, "⚠️ Alarma inexacta programada (sin permiso EXACT_ALARM)")
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        pendingIntent
                    )
                    Log.d(TAG, "✅ Alarma programada para $time - ${medication.name}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error programando alarma", e)
            }
        }
    }

    /** Cancela alarmas de un medicamento */
    fun cancelMedicationAlarms(context: Context, medication: Medication) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        medication.scheduledTimes.forEach { time ->
            val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
                action = "MEDICATION_ALARM"
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                "${medication.id}_$time".hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
                Log.d(TAG, "✅ Alarma cancelada para $time - ${medication.name}")
            }
        }
    }

    /**
     * Convierte un String de hora (HH:mm) a timestamp de hoy
     */
    private fun getAlarmTimeForToday(time: String): Long {
        val parts = time.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}