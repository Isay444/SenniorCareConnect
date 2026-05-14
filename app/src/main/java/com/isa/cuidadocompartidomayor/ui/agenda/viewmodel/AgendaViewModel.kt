package com.isa.cuidadocompartidomayor.ui.agenda.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isa.cuidadocompartidomayor.data.model.AgendaEvent
import com.isa.cuidadocompartidomayor.data.model.TimeFilter
import com.isa.cuidadocompartidomayor.data.repository.AgendaRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class AgendaViewModel : ViewModel() {

    private val repository = AgendaRepository()

    companion object {
        private const val TAG = "AgendaViewModel"
    }

    // ==================== ESTADO DE CARGA ====================

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // ==================== CITAS MÉDICAS ====================

    private val _medicalAppointments = MutableLiveData<List<AgendaEvent.MedicalAppointment>>()
    val medicalAppointments: LiveData<List<AgendaEvent.MedicalAppointment>> = _medicalAppointments

    // ==================== TAREAS NORMALES ====================

    private val _normalTasks = MutableLiveData<List<AgendaEvent.NormalTask>>()
    val normalTasks: LiveData<List<AgendaEvent.NormalTask>> = _normalTasks

    // ==================== EVENTOS PARA DASHBOARD ====================

    private val _upcomingEvents = MutableLiveData<List<AgendaEvent>>()
    val upcomingEvents: LiveData<List<AgendaEvent>> = _upcomingEvents

    // ==================== TODOS LOS EVENTOS (PARA "VER TODAS") ====================

    private val _allEvents = MutableLiveData<List<AgendaEvent>>()
    val allEvents: LiveData<List<AgendaEvent>> = _allEvents

    // ==================== FILTRO SELECCIONADO ====================

    private val _selectedTimeFilter = MutableLiveData<TimeFilter>(TimeFilter.DAY)
    val selectedTimeFilter: LiveData<TimeFilter> = _selectedTimeFilter

    private val _selectedElderlyId = MutableLiveData<String?>()
    val selectedElderlyId: LiveData<String?> = _selectedElderlyId

    // : LiveData para eventos de una fecha específica (todos mezclados)
    private val _eventsForSelectedDate = MutableLiveData<List<AgendaEvent>>()
    val eventsForSelectedDate: LiveData<List<AgendaEvent>> = _eventsForSelectedDate

    // : LiveData para notificar cuando se debe refrescar
    private val _refreshTrigger = MutableLiveData<Long>()
    val refreshTrigger: LiveData<Long> = _refreshTrigger

    // ==================== FUNCIONES PARA CARGAR EVENTOS ====================

    /**
     * ✅ NUEVA FUNCIÓN: Cargar eventos de una fecha específica
     * @param elderlyId ID del adulto mayor ("all" para todos)
     * @param dateInMillis Fecha seleccionada en milisegundos
     */
    fun loadEventsByDate(elderlyId: String, dateInMillis: Long) {
        viewModelScope.launch {
            try {
                Log.d("AgendaViewModel", "Cargando eventos para fecha: $dateInMillis, elderlyId: $elderlyId")

                // Obtener inicio y fin del día
                val (startOfDay, endOfDay) = getDayRange(dateInMillis)

                // Cargar eventos del repositorio
                val allEvents = if (elderlyId == "all") {
                    repository.getAllEventsByDateRange(startOfDay, endOfDay)
                } else {
                    repository.getEventsByElderlyAndDate(elderlyId, startOfDay, endOfDay)
                }

                // Ordenar por fecha/hora
                val sortedEvents = allEvents.sortedBy { event ->
                    when (event) {
                        is AgendaEvent.MedicalAppointment -> event.date
                        is AgendaEvent.NormalTask -> event.date
                    }
                }

                _eventsForSelectedDate.value = sortedEvents
                Log.d("AgendaViewModel", "Eventos cargados: ${sortedEvents.size}")

            } catch (e: Exception) {
                Log.e("AgendaViewModel", "Error al cargar eventos por fecha: ${e.message}", e)
                _eventsForSelectedDate.value = emptyList()
            }
        }
    }

    /**
     * ✅ NUEVA FUNCIÓN HELPER: Obtener inicio y fin del día
     */
    private fun getDayRange(dateInMillis: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateInMillis

        // Inicio del día (00:00:00.000)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        // Fin del día (23:59:59.999)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        return Pair(startOfDay, endOfDay)
    }

    /**
     * Carga las citas médicas de un adulto mayor según el filtro de tiempo
     */
    fun loadMedicalAppointments(elderlyId: String, timeFilter: TimeFilter = TimeFilter.DAY) {
        _selectedElderlyId.value = elderlyId
        _selectedTimeFilter.value = timeFilter
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val (startDate, endDate) = timeFilter.getDateRange()

                Log.d(TAG, "📅 Cargando citas médicas: $elderlyId, filtro: ${timeFilter.getDisplayName()}")

                val result = repository.getMedicalAppointments(elderlyId, startDate, endDate)

                result.onSuccess { appointments ->
                    _medicalAppointments.value = appointments
                    _isLoading.value = false
                    Log.d(TAG, "✅ ${appointments.size} citas médicas cargadas")
                }.onFailure { error ->
                    _errorMessage.value = error.message
                    _isLoading.value = false
                    Log.e(TAG, "❌ Error cargando citas: ${error.message}")
                }

            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
                Log.e(TAG, "❌ Excepción: ${e.message}")
            }
        }
    }

    /**
     * Carga las tareas normales de un adulto mayor según el filtro de tiempo
     */
    fun loadNormalTasks(elderlyId: String, timeFilter: TimeFilter = TimeFilter.DAY) {
        _selectedElderlyId.value = elderlyId
        _selectedTimeFilter.value = timeFilter
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val (startDate, endDate) = timeFilter.getDateRange()

                Log.d(TAG, "📋 Cargando tareas: $elderlyId, filtro: ${timeFilter.getDisplayName()}")

                val result = repository.getNormalTasks(elderlyId, startDate, endDate)

                result.onSuccess { tasks ->
                    _normalTasks.value = tasks
                    _isLoading.value = false
                    Log.d(TAG, "✅ ${tasks.size} tareas cargadas")
                }.onFailure { error ->
                    _errorMessage.value = error.message
                    _isLoading.value = false
                    Log.e(TAG, "❌ Error cargando tareas: ${error.message}")
                }

            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
                Log.e(TAG, "❌ Excepción: ${e.message}")
            }
        }
    }

    /**
     * Carga los próximos eventos para el dashboard (límite 4)
     */
    fun loadUpcomingEventsForDashboard(limit: Int = 4) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "🏠 Cargando próximos $limit eventos para dashboard")

                val result = repository.getUpcomingEventsForDashboard(limit) //Cambio aqui antes era getUpcomingEventsForCaregiver()

                result.onSuccess { events ->
                    _upcomingEvents.value = events
                    _isLoading.value = false
                    Log.d(TAG, "✅ ${events.size} eventos próximos cargados")
                }.onFailure { error ->
                    _errorMessage.value = error.message
                    _isLoading.value = false
                    Log.e(TAG, "❌ Error cargando eventos próximos: ${error.message}")
                }

            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
                Log.e(TAG, "❌ Excepción: ${e.message}")
            }
        }
    }

    /**
     * Carga TODOS los eventos del cuidador
     */
    fun loadAllEvents() {
        _isLoading.value = true
        _errorMessage.value = null
        Log.d(TAG, "📋 Cargando todos los eventos")

        viewModelScope.launch {
            try {
                val result = repository.getAllEventsForCaregiver()

                result.onSuccess { events ->
                    _allEvents.value = events

                    // ✅ Separar eventos en citas y tareas
                    val appointments = events.filterIsInstance<AgendaEvent.MedicalAppointment>()
                    val tasks = events.filterIsInstance<AgendaEvent.NormalTask>()

                    _medicalAppointments.value = appointments
                    _normalTasks.value = tasks

                    Log.d(TAG, "✅ ${events.size} eventos totales cargados (${appointments.size} citas, ${tasks.size} tareas)")

                }.onFailure { error ->
                    _errorMessage.value = error.message
                    Log.e(TAG, "❌ Error cargando todos los eventos: ${error.message}")
                }

            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e(TAG, "❌ Excepción: ${e.message}")
            }

            // ✅ MOVER AQUÍ (fuera del onSuccess/onFailure)
            _isLoading.value = false
        }
    }


    // ==================== MARCAR TAREA COMO COMPLETADA ====================

    /**
     * Cambia el estado de completado de una tarea
     */
    fun toggleTaskCompletion(
        taskId: String,
        isCompleted: Boolean,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val result = repository.toggleTaskCompletion(taskId, isCompleted)

                result.onSuccess {
                    // Recargar las tareas
                    _selectedElderlyId.value?.let { elderlyId ->
                        _selectedTimeFilter.value?.let { filter ->
                            loadNormalTasks(elderlyId, filter)
                        }
                    }
                    // También recargar eventos del dashboard
                    loadUpcomingEventsForDashboard()
                    onSuccess()
                }.onFailure { error ->
                    onError(error.message ?: "Error desconocido")
                }

            } catch (e: Exception) {
                onError(e.message ?: "Error inesperado")
            }
        }
    }

    // ==================== ELIMINAR EVENTO ====================

    /**
     * Elimina un evento (cita o tarea)
     */
    fun deleteEvent(
        eventId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val result = repository.deleteEvent(eventId)

                result.onSuccess {
                    // Recargar datos
                    _selectedElderlyId.value?.let { elderlyId ->
                        _selectedTimeFilter.value?.let { filter ->
                            loadMedicalAppointments(elderlyId, filter)
                            loadNormalTasks(elderlyId, filter)
                        }
                    }
                    loadUpcomingEventsForDashboard()
                    onSuccess()
                }.onFailure { error ->
                    onError(error.message ?: "Error desconocido")
                }

            } catch (e: Exception) {
                onError(e.message ?: "Error inesperado")
            }
        }
    }

    // ==================== UTILIDADES ====================

    /**
     * Actualiza el filtro de tiempo seleccionado
     */
    fun setTimeFilter(timeFilter: TimeFilter) {
        _selectedTimeFilter.value = timeFilter

        // Recargar datos con el nuevo filtro
        _selectedElderlyId.value?.let { elderlyId ->
            loadMedicalAppointments(elderlyId, timeFilter)
            loadNormalTasks(elderlyId, timeFilter)
        }
    }

    fun notifyEventChanged() {
        _refreshTrigger.value = System.currentTimeMillis()
        Log.d("AgendaViewModel", "🔄 Notificación de cambio enviada")
    }

    /**
     * Limpia el mensaje de error
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
