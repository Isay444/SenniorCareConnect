package com.isa.cuidadocompartidomayor.data.model

import java.util.Calendar

enum class TimeFilter {
    DAY,
    WEEK,
    MONTH;

    fun getDisplayName(): String {
        return when (this) {
            DAY -> "Día"
            WEEK -> "Semana"
            MONTH -> "Mes"
        }
    }

    /**
     * Calcula el rango de fechas según el filtro seleccionado
     */
    fun getDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        calendar.timeInMillis = System.currentTimeMillis()

        return when (this) {
            DAY -> {
                // Hoy desde las 00:00 hasta las 23:59
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis

                Pair(startDate, endDate)
            }
            WEEK -> {
                // Próximos 7 días
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis

                calendar.add(Calendar.DAY_OF_YEAR, 7)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val weekEndDate = calendar.timeInMillis

                Pair(startDate, weekEndDate)
            }
            MONTH -> {
                // Mes actual
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val monthEndDate = calendar.timeInMillis

                Pair(startDate, monthEndDate)
            }
        }
    }
}
