package com.isa.cuidadocompartidomayor.utils

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.ui.elderly.ElderlyDashboardActivity
import android.app.AlarmManager
import android.provider.Settings
import android.util.Log
import com.isa.cuidadocompartidomayor.ui.caregiver.CaregiverDashboardActivity
import androidx.core.net.toUri


object NotificationHelper {
    private const val CHANNEL_ID = "medication_reminders"
    private const val CHANNEL_NAME = "Recordatorios de Medicamentos"
    private const val CHANNEL_DESCRIPTION = "Notificaciones para recordar tomar medicamentos"

    /**
     * Crea el canal de notificaciones
     */
    fun createNotificationChannel(context: Context) {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
            enableLights(true)
            setShowBadge(true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Muestra una notificación de medicamento
     */
    fun showMedicationNotification( context: Context, medicationId: String, medicationName: String, dosage: String, time: String, elderlyName: String ) {
        // Intent para abrir la app al hacer tap en la notificación
        val intent = Intent(context, ElderlyDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("MEDICATION_ID", medicationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            medicationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construir la notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medication)
            .setContentTitle("💊 Hola $elderlyName, es hora de tu medicamento")
            .setContentText("$medicationName - $dosage a las $time")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Es hora de tomar $medicationName ($dosage) programado para las $time")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        // Verificar permisos y mostrar notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(context).notify(medicationId.hashCode(), notification)
            }
        } else {
            NotificationManagerCompat.from(context).notify(medicationId.hashCode(), notification)
        }
    }

    /**
     * Muestra notificación al cuidador (notificación local)
     */
    fun showCaregiverNotification( context: Context, title: String, body: String, medicationId: String, elderlyId: String ) {
        val notificationId = System.currentTimeMillis().toInt()

        val intent = Intent(context, CaregiverDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("MEDICATION_ID", medicationId)
            putExtra("ELDERLY_ID", elderlyId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medication)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)

        Log.d("NotificationHelper", "✅ Notificación de cuidador mostrada")
    }

    /**
     * Verifica si la app tiene permiso de notificaciones
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No se requiere permiso en versiones anteriores
        }
    }

    /**
     * Verifica si la app puede programar alarmas exactas (Android 12+)
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // No se requiere en versiones anteriores
        }
    }

    /**
     * Abre la configuración para permitir alarmas exactas
     */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:${context.packageName}".toUri()
            }
            context.startActivity(intent)
        }
    }

    /**
     * Verifica si todos los permisos necesarios están otorgados
     */
    fun hasAllPermissions(context: Context): Boolean {
        return hasNotificationPermission(context) && canScheduleExactAlarms(context)
    }
}