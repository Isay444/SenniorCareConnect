package com.isa.cuidadocompartidomayor.ui.elderly.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isa.cuidadocompartidomayor.data.model.MedicationLog
import com.isa.cuidadocompartidomayor.data.repository.MedicationRepository
import kotlinx.coroutines.launch

class ElderlyMedicationViewModel(application: Application) : AndroidViewModel(application){
    private val repository = MedicationRepository(application.applicationContext)
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "ElderlyMedicationVM"
    }
    // ========================================
    // LIVEDATA - PRÓXIMO MEDICAMENTO

    private val _nextMedication = MutableLiveData<MedicationLog?>()
    val nextMedication: LiveData<MedicationLog?> = _nextMedication
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _actionSuccess = MutableLiveData<String?>()
    val actionSuccess: LiveData<String?> = _actionSuccess
    private val _actionError = MutableLiveData<String?>()
    val actionError: LiveData<String?> = _actionError

    /** Carga el próximo medicamento PENDING del adulto mayor */
    fun loadNextMedication(){
        val elderlyId = auth.currentUser?.uid ?: return

        viewModelScope.launch{
            _isLoading.value = true
            val result = repository.getNextPendingMedication(elderlyId) //Solo devuelve medicamentos PENDING cuya hora programada sea DESPUÉS de la hora actual
            result.onSuccess {
                medicationLog -> _nextMedication.value = medicationLog
                Log.d(TAG, "✅ Próximo medicamento cargado: ${medicationLog?.medicationName ?: "Ninguno"}")
            }.onFailure { exception ->
                _actionError.value = "Error al cargar medicamento: ${exception.message}"
                Log.e(TAG, "❌ Error cargando próximo medicamento", exception )
            }
            _isLoading.value = false
        }
    }

    /** Confirma que el adulto mayor tomó el medicamento */
    fun confirmMedicationTaken(medicationLog: MedicationLog) {
        val elderlyId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.confirmMedicationTaken(
                medicationId = medicationLog.medicationId,
                medicationName = medicationLog.medicationName,
                dosage = medicationLog.dosage,
                frequency = medicationLog.frequency,
                elderlyId = elderlyId,
                elderlyName = medicationLog.elderlyName,
                caregiverId = medicationLog.caregiverId,
                caregiverName = medicationLog.caregiverName,
                confirmedBy = elderlyId // ✅ Confirmado por el adulto mayor
            )

            result.onSuccess {
                _actionSuccess.value = "¡Medicamento confirmado! 👏"
                Log.d(TAG, "✅ Medicamento confirmado por el adulto mayor")

                // ✅ Recargar automáticamente el siguiente medicamento
                loadNextMedication()

            }.onFailure { exception ->
                _actionError.value = "Error al confirmar medicamento: ${exception.message}"
                Log.e(TAG, "❌ Error confirmando medicamento", exception)
            }

            _isLoading.value = false
        }
    }

    fun clearMessages() {
        _actionSuccess.value = null
        _actionError.value = null
    }

}