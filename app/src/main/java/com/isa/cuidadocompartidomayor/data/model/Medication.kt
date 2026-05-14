package com.isa.cuidadocompartidomayor.data.model

import com.google.firebase.firestore.PropertyName
import java.text.SimpleDateFormat
import java.util.*

data class Medication(
    val id: String = "",
    val elderlyId: String = "",
    val elderlyName: String = "",
    val caregiverId: String = "",
    val caregiverName: String = "",

    // Información básica
    val name: String = "",
    val medicationType: MedicationType = MedicationType.PILL, // Por default es PILL
    val dosage: String = "ml",
    val instructions: String = "Sin notas",

    // Frecuencia
    val frequencyType: FrequencyType = FrequencyType.ONCE_DAILY,
    val timesPerDay: Int = 1,                      // Para MULTIPLE_DAILY
    val intervalDays: Int? = null,                 // Para EVERY_X_DAYS
    val intervalWeeks: Int? = null,                // Para EVERY_X_WEEKS
    val intervalMonths: Int? = null,               // Para EVERY_X_MONTHS
    val weekDays: List<Int> = emptyList(),         // Para SPECIFIC_DAYS [1=Lun, 7=Dom]

    // Horarios programados
    val scheduledTimes: List<String> = emptyList(), // ["08:00", "14:00", "20:00"]

    // Fechas
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,                     // null = indefinido

    // Estado
    @get:PropertyName("active")
    @set:PropertyName("active")
    var active: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
){
    // Constructor sin argumentos para Firestore
    constructor() : this(
        "", "", "", "", "", "",
        MedicationType.PILL, "", "",
        FrequencyType.ONCE_DAILY, 1, null, null, null,
        emptyList(), emptyList(), 0L, null,
        true, 0L, 0L
    )

    /** Obtiene el texto de frecuencia legible */
    fun getFrequencyText(): String {
        return when (frequencyType) {
            FrequencyType.ONCE_DAILY -> "Una vez al día"
            FrequencyType.MULTIPLE_DAILY -> "$timesPerDay veces al día"
            FrequencyType.SPECIFIC_DAYS -> getWeekDaysText()
            FrequencyType.EVERY_X_DAYS -> "Cada $intervalDays días"
            FrequencyType.EVERY_X_WEEKS -> "Cada $intervalWeeks semanas"
            FrequencyType.EVERY_X_MONTHS -> "Cada $intervalMonths meses"
        }
    }

    /** Obtiene el texto de días de la semana seleccionados */
    private fun getWeekDaysText(): String {
        val dayNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        return weekDays.sorted().joinToString(", ") { dayNames[it - 1] }
    }

    /** Obtiene la próxima fecha y hora de toma considerando la frecuencia del medicamento
     * @return Pair(fecha_timestamp, hora_string) o null si no hay próxima toma */
    fun getNextScheduledDateTime(): Pair<Long, String>? {
        if (scheduledTimes.isEmpty()) return null

        val now = Calendar.getInstance()
        val currentTime = now.timeInMillis

        // Buscar la próxima toma válida en los próximos 60 días
        for (dayOffset in 0..60) {
            val checkDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }

            // Verificar si el medicamento aplica para este día
            if (shouldTakeMedicationOnDay(checkDate)) {
                // Buscar la primera hora disponible para este día
                for (time in scheduledTimes.sorted()) {
                    val scheduledDateTime = getTimestampForTime(checkDate, time)

                    // Si es una hora futura, retornarla
                    if (scheduledDateTime > currentTime) {
                        return Pair(scheduledDateTime, time)
                    }
                }
            }
        }

        return null // No hay próxima toma en los próximos 60 días
    }

    /** Verifica si el medicamento debe tomarse en una fecha específica */
    private fun shouldTakeMedicationOnDay(calendar: Calendar): Boolean {
        return when (frequencyType) {
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
                weekDays.contains(dayIndex)
            }

            FrequencyType.EVERY_X_DAYS -> {
                // ✅ Normalizar ambas fechas a medianoche
                val startCal = Calendar.getInstance().apply {
                    timeInMillis = startDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val checkCal = Calendar.getInstance().apply {
                    timeInMillis = calendar.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val daysSinceStart = ((checkCal.timeInMillis - startCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                daysSinceStart >= 0 && daysSinceStart % (intervalDays ?: 1) == 0
            }

            FrequencyType.EVERY_X_WEEKS -> {
                // ✅ Normalizar ambas fechas a medianoche
                val startCal = Calendar.getInstance().apply {
                    timeInMillis = startDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val checkCal = Calendar.getInstance().apply {
                    timeInMillis = calendar.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val daysSinceStart = ((checkCal.timeInMillis - startCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                val weeksSinceStart = daysSinceStart / 7
                weeksSinceStart >= 0 && weeksSinceStart % (intervalWeeks ?: 1) == 0 && daysSinceStart % 7 == 0
            }

            FrequencyType.EVERY_X_MONTHS -> {
                // ✅ Comparar año, mes y día
                val startCal = Calendar.getInstance().apply {
                    timeInMillis = startDate
                }

                val checkCal = Calendar.getInstance().apply {
                    timeInMillis = calendar.timeInMillis
                }

                // Verificar que sea el mismo día del mes
                if (startCal.get(Calendar.DAY_OF_MONTH) != checkCal.get(Calendar.DAY_OF_MONTH)) {
                    return false
                }

                val monthsSinceStart = (checkCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)) * 12 +
                        (checkCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH))

                monthsSinceStart >= 0 && monthsSinceStart % (intervalMonths ?: 1) == 0
            }
        }
    }

    /** Convierte una fecha Calendar y hora String a timestamp */
    private fun getTimestampForTime(calendar: Calendar, time: String): Long {
        val parts = time.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        return Calendar.getInstance().apply {
            timeInMillis = calendar.timeInMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /** Convierte "HH:mm" a minutos del día */
    private fun parseTime(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    /** Obtiene la fecha formateada */
    fun getFormattedStartDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(startDate))
    }

    /** Verifica si el medicamento está activo hoy */
    fun isScheduledForToday(): Boolean {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val todayIndex = when (today) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 0
        }

        return when (frequencyType) {
            FrequencyType.SPECIFIC_DAYS -> weekDays.contains(todayIndex)
            else -> true // Otros tipos siempre están activos (se calcula diferente)
        }
    }
}