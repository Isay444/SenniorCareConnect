package com.isa.cuidadocompartidomayor

import android.app.Application
import com.isa.cuidadocompartidomayor.utils.MedicationLogScheduler
import com.isa.cuidadocompartidomayor.utils.NotificationHelper

class MedicationApp: Application() {
    override fun onCreate() {
        super.onCreate()
        // ✅ Crear canal de notificaciones al inicio de la app
        NotificationHelper.createNotificationChannel(this)

        // ✅ Programar generación automática de logs de medicamentos
        MedicationLogScheduler.schedule(this)
    }
}