package com.isa.cuidadocompartidomayor.ui.elderly.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isa.cuidadocompartidomayor.data.model.AgendaEvent
import com.isa.cuidadocompartidomayor.data.repository.AgendaRepository
import kotlinx.coroutines.launch

class ElderlyHomeViewModel : ViewModel() {

    private val agendaRepository = AgendaRepository()

    companion object {
        private const val TAG = "ElderlyHomeViewModel"
    }

    // LiveData para citas médicas del día
    private val _todayAppointments = MutableLiveData<List<AgendaEvent.MedicalAppointment>>()
    val todayAppointments: LiveData<List<AgendaEvent.MedicalAppointment>> = _todayAppointments

    // LiveData para estado de carga
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Carga las citas médicas del día actual para el adulto mayor
     */
    fun loadTodayAppointments(elderlyId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "📅 Cargando citas del día para: $elderlyId")

                val appointments = agendaRepository.getTodayMedicalAppointmentsForElderly(elderlyId)
                _todayAppointments.value = appointments

                Log.d(TAG, "✅ ${appointments.size} citas cargadas")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando citas: ${e.message}", e)
                _todayAppointments.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
