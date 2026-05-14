package com.isa.cuidadocompartidomayor.ui.elderly.viewmodel

import com.isa.cuidadocompartidomayor.data.repository.EmergencyRepository
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import com.isa.cuidadocompartidomayor.data.model.EmergencyAlert
import kotlinx.coroutines.launch

class EmergencyViewModel : ViewModel() {
    private val repository = EmergencyRepository()

    private val _alertSent = MutableLiveData<Boolean>()
    val alertSent: LiveData<Boolean> = _alertSent

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _lastAlert = MutableLiveData<EmergencyAlert?>()
    val lastAlert: LiveData<EmergencyAlert?> = _lastAlert

    /**
     * Envía una alerta de emergencia
     */
    fun sendEmergencyAlert(latitude: Double? = null, longitude: Double? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = repository.createEmergencyAlert(latitude, longitude)

            result.onSuccess { alert ->
                _lastAlert.value = alert
                _alertSent.value = true
                _isLoading.value = false
            }.onFailure { exception ->
                _error.value = exception.message ?: "Error al enviar alerta"
                _alertSent.value = false
                _isLoading.value = false
            }
        }
    }

    /**
     * Resetea el estado de envío
     */
    fun resetAlertState() {
        _alertSent.value = false
        _error.value = null
    }
}