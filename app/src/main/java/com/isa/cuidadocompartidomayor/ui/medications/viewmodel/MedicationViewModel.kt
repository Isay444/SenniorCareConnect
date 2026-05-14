package com.isa.cuidadocompartidomayor.ui.medications.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.isa.cuidadocompartidomayor.data.model.Medication
import com.isa.cuidadocompartidomayor.data.model.MedicationLog
import com.isa.cuidadocompartidomayor.data.repository.MedicationRepository
import com.isa.cuidadocompartidomayor.utils.MedicationAlarmManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
class MedicationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MedicationRepository(application.applicationContext)
    private val auth = FirebaseAuth.getInstance()
    companion object {
        private const val TAG = "MedicationViewModel"
    }
    private val _activeMedications = MutableLiveData<List<Medication>>()
    val activeMedications: LiveData<List<Medication>> = _activeMedications
    private val _isLoadingActive = MutableLiveData<Boolean>()
    val isLoadingActive: LiveData<Boolean> = _isLoadingActive
    private val _activeError = MutableLiveData<String?>()
    val activeError: LiveData<String?> = _activeError
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _upcomingLogs = MutableLiveData<List<MedicationLog>>()
    val upcomingLogs: LiveData<List<MedicationLog>> = _upcomingLogs
    private val _upcomingMedications = MutableLiveData<List<Medication>>()
    val upcomingMedications: LiveData<List<Medication>> = _upcomingMedications
    private val _isLoadingUpcoming = MutableLiveData<Boolean>()
    val isLoadingUpcoming: LiveData<Boolean> = _isLoadingUpcoming
    private val _upcomingError = MutableLiveData<String?>()
    val upcomingError: LiveData<String?> = _upcomingError
    private val _medicationLogs = MutableLiveData<List<MedicationLog>>()
    val medicationLogs: LiveData<List<MedicationLog>> = _medicationLogs
    private val _isLoadingLogs = MutableLiveData<Boolean>()
    val isLoadingLogs: LiveData<Boolean> = _isLoadingLogs
    private val _logsError = MutableLiveData<String?>()
    val logsError: LiveData<String?> = _logsError
    private val _operationSuccess = MutableLiveData<String?>()
    val operationSuccess: LiveData<String?> = _operationSuccess
    private val _operationError = MutableLiveData<String?>()
    val operationError: LiveData<String?> = _operationError
    val _isProcessing = MutableLiveData<Boolean>()
    val isProcessing: LiveData<Boolean> = _isProcessing
    private val _currentMedication = MutableLiveData<Medication?>()
    val currentMedication: LiveData<Medication?> = _currentMedication

    // Mapa de fotos de perfil (userId -> imageUrl)
    private val _medicationPhotoMap = MutableLiveData<Map<String, String>>(emptyMap())
    val medicationPhotoMap: LiveData<Map<String, String>> = _medicationPhotoMap

    //CONFIRMAR MEDICAMENTO
    private val _medicationActionError = MutableLiveData<String?>()
    val medicationActionError: LiveData<String?> = _medicationActionError

    /** Carga medicamentos activos de un cuidador (todos sus pacientes) (2 usages en MedicationsFragment.kt en las funciones loadMedicationByCaregiver() y showListTab(), 2 usages en MedicationViewModel.kt en las funciones deleteMedication() y updateMedication())*/
    fun loadActiveMedicationsByCaregiver(caregiverId: String) {
        viewModelScope.launch {
            _isLoadingActive.value = true
            _activeError.value = null

            val result = repository.getActiveMedicationsByCaregiver(caregiverId)

            result.onSuccess { medications ->
                // ✅ toList() crea una nueva lista, forzando la actualización del LiveData
                _activeMedications.value = medications.toList()
                Log.d(TAG, "✅ Medicamentos del caregiver cargados: ${medications.size}")

                // Cargar fotos de perfil de todos los involucrados
                enrichWithUserPhotos(medications)
            }.onFailure { exception ->
                _activeError.value = exception.message ?: "Error al cargar medicamentos"
                Log.e(TAG, "❌ Error cargando medicamentos del caregiver", exception)
            }

            _isLoadingActive.value = false
        }
    }

    /** Carga todos los logs de medicamentos del caregiver (3 usages en MedicationViewModel.kt en las funciones confirmMedicationTaken(), setPendingMedication() y skipMedication() y 1 usage en MedicationsFragment.kt en la funcion showHistoryTab())*/
    fun loadMedicationLogsByCaregiver(caregiverId: String) {
        viewModelScope.launch {
            _isLoadingLogs.value = true
            _logsError.value = null

            val result = repository.getMedicationLogsByCaregiver(caregiverId)

            result.onSuccess { logs ->
                _medicationLogs.value = logs
                Log.d(TAG, "✅ Logs del caregiver cargados: ${logs.size}")
            }.onFailure { exception ->
                _logsError.value = exception.message ?: "Error al cargar historial"
                Log.e(TAG, "❌ Error cargando logs del caregiver", exception)
            }

            _isLoadingLogs.value = false
        }
    }

    /** Actualiza un medicamento existente (1 usage en EditMedicationDialogFragment.kt en la funcion saveChanges())*/
    fun updateMedication(medication: Medication) {
        viewModelScope.launch {
            _isProcessing.value = true
            _operationError.value = null
            _operationSuccess.value = null

            val result = repository.updateMedication(medication)

            result.onSuccess { updatedMedication ->
                _operationSuccess.value = "Medicamento actualizado exitosamente"
                Log.d(TAG, "✅ Medicamento actualizado: ${updatedMedication.name}")

                // ✅ CORRECCIÓN: Usar el caregiverId del usuario actual, NO del medicamento
                val currentUserId = auth.currentUser?.uid
                if (currentUserId != null) {
                    loadActiveMedicationsByCaregiver(currentUserId)
                }

            }.onFailure { exception ->
                _operationError.value = exception.message ?: "Error al actualizar medicamento"
                Log.e(TAG, "❌ Error actualizando medicamento", exception)
            }

            _isProcessing.value = false
        }
    }
    /** Elimina un medicamento (marca como inactivo) y CANCELA ALARMAS (1 usage en EditMedicationDialogFragment.kt en la funcion deleteMedication y 1 usage en MedicationsFragment.kt en la funcion showDeleteConfirmationDialog())*/
    fun deleteMedication(medicationId: String, elderlyId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _operationError.value = null
            _operationSuccess.value = null

            // 1️⃣ Obtener el medicamento antes de eliminarlo
            val getMedicationResult = repository.getMedicationById(medicationId)

            getMedicationResult.onSuccess { medication ->
                if (medication != null) {
                    // 2️⃣ Cancelar alarmas programadas
                    try {
                        MedicationAlarmManager.cancelMedicationAlarms(
                            context = getApplication<Application>().applicationContext,
                            medication = medication
                        )
                        Log.d(TAG, "✅ Alarmas canceladas para: ${medication.name}")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error cancelando alarmas", e)
                    }

                    // 3️⃣ Marcar como inactivo en Firestore
                    val deleteResult = repository.deleteMedication(medicationId)

                    deleteResult.onSuccess {
                        _operationSuccess.value = "Medicamento eliminado exitosamente"
                        Log.d(TAG, "✅ Medicamento eliminado: $medicationId")

                        // 4️⃣ Recargar con el ID del usuario actual
                        val currentUserId = auth.currentUser?.uid
                        if (currentUserId != null) {
                            loadActiveMedicationsByCaregiver(currentUserId)
                        }
                    }.onFailure { exception ->
                        _operationError.value = exception.message ?: "Error al eliminar medicamento"
                        Log.e(TAG, "❌ Error eliminando medicamento", exception)
                    }
                }
            }.onFailure { exception ->
                _operationError.value = exception.message ?: "Error al obtener medicamento"
                Log.e(TAG, "❌ Error obteniendo medicamento", exception)
            }

            _isProcessing.value = false
        }
    }

    /** Carga un medicamento específico por ID (para edición) (1 usage en EditMedicationDialogFragment.kt en onViewCreated())*/
    fun loadMedicationById(medicationId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _operationError.value = null

            val result = repository.getMedicationById(medicationId)

            result.onSuccess { medication ->
                _currentMedication.value = medication
                Log.d(TAG, "✅ Medicamento cargado: ${medication?.name}")
            }.onFailure { exception ->
                _operationError.value = exception.message ?: "Error al cargar medicamento"
                Log.e(TAG, "❌ Error cargando medicamento", exception)
            }

            _isProcessing.value = false
        }
    }

    /** Omite un medicamento (1 usage en CaregiverHomeFragment.kt en la funcion skipMedication())*/
    fun skipMedication(medication: Medication, log: MedicationLog, skippedBy: String) {
        viewModelScope.launch {
            _isLoadingActive.value = true
            _medicationActionError.value = null

            val result = repository.skipMedicationByLogId(  // ✅ Nueva función
                logId = log.id,  // ✅ Pasar logId
                skippedBy = skippedBy
            )

            result.onSuccess {
                Log.d(TAG, "✅ Medicamento omitido exitosamente: ${log.id}")
                loadUpcomingLogsForCaregiver(skippedBy)
                loadMedicationLogsByCaregiver(medication.caregiverId)
            }.onFailure { error ->
                _medicationActionError.value = error.message ?: "Error al omitir medicamento"
                Log.e(TAG, "❌ Error omitiendo medicamento", error)
            }

            _isLoadingActive.value = false
        }
    }

    /** Confirma que un medicamento fue tomado (1 usage en CaregiverHomeFragment.kt en la funcion confirmMedicationTaken())*/
    fun confirmMedicationTaken(medication: Medication, log: MedicationLog, confirmedBy: String) {
        viewModelScope.launch {
            _isLoadingActive.value = true
            _medicationActionError.value = null

            val result = repository.confirmMedicationTakenByLogId(  // ✅ Nueva función
                logId = log.id,  // ✅ Pasar logId
                confirmedBy = confirmedBy
            )

            result.onSuccess {
                Log.d(TAG, "✅ Medicamento confirmado exitosamente")
                loadUpcomingLogsForCaregiver(confirmedBy)
                loadMedicationLogsByCaregiver(medication.caregiverId)

            }.onFailure { exception ->
                _medicationActionError.value = exception.message ?: "Error al confirmar medicamento"
                Log.e(TAG, "❌ Error confirmando medicamento", exception)
            }
            _isLoadingActive.value = false
        }
    }
    
    /**(1 usage en CaregiverHomeFragment.kt en la funcion setPendingMedication())*/
    fun setPendingMedication(medication: Medication, log: MedicationLog, changedBy: String) {
        viewModelScope.launch {
            _isLoadingActive.value = true
            _medicationActionError.value = null

            val result = repository.revertLogToPendingByLogId(
                logId = log.id,
                modifiedBy = changedBy  // : Pasar el userId
            )

            result.onSuccess {
                Log.d(TAG, "✅ Medicamento revertido a pendiente: ${log.id}")
                loadUpcomingLogsForCaregiver(changedBy)
                loadMedicationLogsByCaregiver(medication.caregiverId)
            }.onFailure { exception ->
                _medicationActionError.value = exception.message ?: "Error al revertir medicamento"
                Log.e(TAG, "❌ Error revirtiendo medicamento", exception)
            }

            _isLoadingActive.value = false
        }
    }

    /**  Carga logs PENDING en lugar de medicamentos (1 usage en CragevierHomeFragment.kt en la funcion loadUpcomingMedications() y 3 usages en MedicationViewModel.kt en las funciones confirmMedicationTaken(), setPendingMedication() y skipMedication())*/
    fun loadUpcomingLogsForCaregiver(caregiverId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _activeError.value = null

            val result = repository.getUpcomingLogsByCaregiver(caregiverId, limit = 10)

            result.onSuccess { logs ->
                _upcomingLogs.value = logs  // ✅ Necesitas crear este LiveData
                Log.d(TAG, "✅ Logs PENDING cargados: ${logs.size}")
            }.onFailure { error ->
                Log.e(TAG, "❌ Error cargando logs PENDING", error)
                _upcomingLogs.value = emptyList()
            }

            _isLoading.value = false
        }
    }

    /**
     * Obtiene las fotos de perfil de todos los adultos mayores y cuidadores en la lista
     */
    private fun enrichWithUserPhotos(medications: List<Medication>) {
        val db = FirebaseFirestore.getInstance()
        val userIds = (medications.map { it.elderlyId } + medications.map { it.caregiverId })
            .filter { it.isNotEmpty() }
            .distinct()

        if (userIds.isEmpty()) return

        viewModelScope.launch {
            try {
                val usersSnapshot = db.collection("users")
                    .whereIn(FieldPath.documentId(), userIds)
                    .get()
                    .await()

                val photoMap = usersSnapshot.documents.associate { doc ->
                    doc.id to (doc.getString("profileImageUrl") ?: "")
                }
                _medicationPhotoMap.value = photoMap
                Log.d(TAG, "✅ Mapa de fotos actualizado con ${photoMap.size} usuarios")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error obteniendo fotos para medicamentos", e)
            }
        }
    }

    /** Limpia mensajes de operaciones (2 usages en EditMedicationDialogFragment.kt en la funcione observeViewModel() y 2 usages en MedicationsFragment.kt en la funcion observeViewModel())*/
    fun clearOperationMessages() {
        _operationSuccess.value = null
        _operationError.value = null
    }

    /** Limpia errores de carga de medicamentos activos (1 usage en MedicationsFragment.kt() en observeViewModel())*/
    fun clearActiveError() {
        _activeError.value = null
    }
}