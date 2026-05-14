package com.isa.cuidadocompartidomayor.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Dispositivo reiniciado. Programando MedicationLogGeneratorWorker...")
            MedicationLogScheduler.schedule(context)
        }
    }
}
