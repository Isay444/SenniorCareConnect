package com.isa.cuidadocompartidomayor.utils

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.concurrent.TimeUnit

object MedicationLogScheduler {

    private const val WORK_TAG = "medication_log_generator"

    //schedule se usa en MedicationApp para la generacion automatica
    fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Calcular el retraso inicial hasta la próxima medianoche
        val initialDelay = calculateDelayUntilMidnight()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<MedicationLogGeneratorWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        Log.d("WORKER_EJECUTADO", "Worker ejecutado en: ${System.currentTimeMillis()}")
    }

    private fun calculateDelayUntilMidnight(): Long {
        val now = LocalDateTime.now()
        val midnight = now.plusDays(1).truncatedTo(ChronoUnit.DAYS)
        return ChronoUnit.MILLIS.between(now, midnight)
    }
}
