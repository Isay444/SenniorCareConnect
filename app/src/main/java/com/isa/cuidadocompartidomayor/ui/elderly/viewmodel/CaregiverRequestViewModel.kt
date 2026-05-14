package com.isa.cuidadocompartidomayor.ui.elderly.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.isa.cuidadocompartidomayor.data.model.CaregiverRequest
import com.isa.cuidadocompartidomayor.data.repository.CaregiverRequestRepository
import kotlinx.coroutines.launch

class CaregiverRequestViewModel : ViewModel() {

    private val repository = CaregiverRequestRepository()
    private val auth = FirebaseAuth.getInstance()

    // Estados para la UI
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _pendingRequests = MutableLiveData<List<CaregiverRequest>>()
    val pendingRequests: LiveData<List<CaregiverRequest>> = _pendingRequests

    private val _approvedCaregivers = MutableLiveData<List<CaregiverRequest>>()
    val approvedCaregivers: LiveData<List<CaregiverRequest>> = _approvedCaregivers

    private val _pendingCount = MutableLiveData<Int>()
    val pendingCount: LiveData<Int> = _pendingCount

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    // Estados para operaciones específicas
    private val _operationInProgress = MutableLiveData<String?>() // ID de la solicitud siendo procesada
    val operationInProgress: LiveData<String?> = _operationInProgress

    private val _requestApproved = MutableLiveData<CaregiverRequest?>()
    val requestApproved: LiveData<CaregiverRequest?> = _requestApproved

    private val _requestRejected = MutableLiveData<CaregiverRequest?>()
    val requestRejected: LiveData<CaregiverRequest?> = _requestRejected

    companion object {
        private const val TAG = "CaregiverRequestViewModel"
    }

    /**
     * Inicialización del ViewModel
     */
    init {
        loadAllData()
    }

    /**
     * Carga todos los datos (solicitudes pendientes y cuidadores aprobados)
     */
    fun loadAllData() {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                _errorMessage.value = "Usuario no autenticado"
                return@launch
            }

            _isLoading.value = true

            try {
                // Cargar en paralelo ambas listas
                loadPendingRequests(currentUser.uid)
                loadApprovedCaregivers(currentUser.uid)
                updatePendingCount(currentUser.uid)

            } catch (e: Exception) {
                Log.e(TAG, "Error cargando datos", e)
                _errorMessage.value = "Error cargando datos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Carga solo las solicitudes pendientes
     */
    fun loadPendingRequests(elderlyId: String? = null) {
        viewModelScope.launch {
            val userId = elderlyId ?: auth.currentUser?.uid
            if (userId == null) {
                _errorMessage.value = "Usuario no autenticado"
                return@launch
            }

            try {
                val result = repository.getPendingRequests(userId)

                if (result.isSuccess) {
                    val requests = result.getOrNull() ?: emptyList()
                    _pendingRequests.value = requests

                    Log.d(TAG, "Solicitudes pendientes cargadas: ${requests.size}")
                } else {
                    _errorMessage.value = "Error cargando solicitudes: ${result.exceptionOrNull()?.message}"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error cargando solicitudes pendientes", e)
                _errorMessage.value = "Error inesperado cargando solicitudes"
            }
        }
    }

    /**
     * Carga los cuidadores ya aprobados
     */
    fun loadApprovedCaregivers(elderlyId: String? = null) {
        viewModelScope.launch {
            val userId = elderlyId ?: auth.currentUser?.uid
            if (userId == null) {
                _errorMessage.value = "Usuario no autenticado"
                return@launch
            }

            try {
                val result = repository.getApprovedCaregivers(userId)

                if (result.isSuccess) {
                    val caregivers = result.getOrNull() ?: emptyList()
                    _approvedCaregivers.value = caregivers

                    Log.d(TAG, "Cuidadores aprobados cargados: ${caregivers.size}")
                } else {
                    _errorMessage.value = "Error cargando cuidadores: ${result.exceptionOrNull()?.message}"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error cargando cuidadores aprobados", e)
                _errorMessage.value = "Error inesperado cargando cuidadores"
            }
        }
    }

    /**
     * Aprueba una solicitud de cuidador
     */
    fun approveRequest(request: CaregiverRequest) {
        if (_operationInProgress.value != null) {
            _errorMessage.value = "Ya hay una operación en progreso"
            return
        }

        _operationInProgress.value = request.id

        viewModelScope.launch {
            try {
                val result = repository.approveRequest(request.id)

                if (result.isSuccess) {
                    val approvedRequest = result.getOrNull()!!
                    _requestApproved.value = approvedRequest
                    _successMessage.value = "✅ ${request.caregiverName} fue aprobado como cuidador"

                    // Recargar datos
                    loadAllData()

                    Log.d(TAG, "Solicitud aprobada: ${request.id}")
                } else {
                    _errorMessage.value = "Error aprobando solicitud: ${result.exceptionOrNull()?.message}"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error aprobando solicitud", e)
                _errorMessage.value = "Error inesperado aprobando solicitud"
            } finally {
                _operationInProgress.value = null
            }
        }
    }

    /**
     * Rechaza una solicitud de cuidador
     */
    fun rejectRequest(request: CaregiverRequest) {
        if (_operationInProgress.value != null) {
            _errorMessage.value = "Ya hay una operación en progreso"
            return
        }

        _operationInProgress.value = request.id

        viewModelScope.launch {
            try {
                val result = repository.rejectRequest(request.id)

                if (result.isSuccess) {
                    val rejectedRequest = result.getOrNull()!!
                    _requestRejected.value = rejectedRequest
                    _successMessage.value = "❌ Solicitud de ${request.caregiverName} rechazada"

                    // Recargar datos
                    loadAllData()

                    Log.d(TAG, "Solicitud rechazada: ${request.id}")
                } else {
                    _errorMessage.value = "Error rechazando solicitud: ${result.exceptionOrNull()?.message}"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error rechazando solicitud", e)
                _errorMessage.value = "Error inesperado rechazando solicitud"
            } finally {
                _operationInProgress.value = null
            }
        }
    }

    /**
     * Remueve un cuidador ya aprobado
     */
    fun removeCaregiver(caregiver: CaregiverRequest) {
        if (_operationInProgress.value != null) {
            _errorMessage.value = "Ya hay una operación en progreso"
            return
        }

        _operationInProgress.value = caregiver.id

        viewModelScope.launch {
            try {
                val result = repository.removeCaregiver(caregiver.caregiverId, caregiver.elderlyId)

                if (result.isSuccess) {
                    _successMessage.value = "🗑️ ${caregiver.caregiverName} fue removido como cuidador"

                    // Recargar datos
                    loadAllData()

                    Log.d(TAG, "Cuidador removido: ${caregiver.id}")
                } else {
                    _errorMessage.value = "Error removiendo cuidador: ${result.exceptionOrNull()?.message}"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error removiendo cuidador", e)
                _errorMessage.value = "Error inesperado removiendo cuidador"
            } finally {
                _operationInProgress.value = null
            }
        }
    }

    /**
     * Actualiza el contador de solicitudes pendientes
     */
    private fun updatePendingCount(elderlyId: String) {
        viewModelScope.launch {
            try {
                val result = repository.countPendingRequests(elderlyId)

                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    _pendingCount.value = count

                    Log.d(TAG, "Contador actualizado: $count solicitudes pendientes")
                }

            } catch (e: Exception) {
                Log.w(TAG, "Error actualizando contador", e)
                // No mostrar error al usuario para esto
            }
        }
    }

    /**
     * Refresca todos los datos (pull-to-refresh)
     */
    fun refreshData() {
        _isRefreshing.value = true

        viewModelScope.launch {
            try {
                loadAllData()
                _successMessage.value = "Datos actualizados"
            } catch (e: Exception) {
                _errorMessage.value = "Error refrescando datos"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Obtiene el texto de confirmación para aprobar una solicitud
     */
    fun getApprovalConfirmationText(request: CaregiverRequest): String {
        return """
            ¿Aprobar a ${request.caregiverName} como tu cuidador?
            
            ${request.caregiverName} podrá:
            • Ver tus medicamentos
            • Gestionar tus citas médicas
            • Recibir alertas de emergencia
            
            Puedes removerlo más tarde si es necesario.
        """.trimIndent()
    }

    /**
     * Obtiene el texto de confirmación para rechazar una solicitud
     */
    fun getRejectionConfirmationText(request: CaregiverRequest): String {
        return """
            ¿Rechazar la solicitud de ${request.caregiverName}?
            
            Esta acción no se puede deshacer.
            
            ${request.caregiverName} tendrá que solicitar acceso nuevamente si cambias de opinión.
        """.trimIndent()
    }

    /**
     * Obtiene el texto de confirmación para remover un cuidador
     */
    fun getRemovalConfirmationText(caregiver: CaregiverRequest): String {
        return """
            ¿Remover a ${caregiver.caregiverName} como tu cuidador?
            
            ${caregiver.caregiverName} ya no podrá:
            • Ver tus medicamentos
            • Gestionar tus citas médicas
            • Recibir alertas de emergencia
            
            Tendrá que solicitar acceso nuevamente.
        """.trimIndent()
    }

    /**
     * Verifica si hay solicitudes pendientes
     */
    fun hasPendingRequests(): Boolean {
        val requests = _pendingRequests.value
        return requests != null && requests.isNotEmpty()
    }

    /**
     * Verifica si hay cuidadores aprobados
     */
    fun hasApprovedCaregivers(): Boolean {
        val caregivers = _approvedCaregivers.value
        return caregivers != null && caregivers.isNotEmpty()
    }

    /**
     * Obtiene el número total de solicitudes (para badges)
     */
    fun getTotalRequestsCount(): Int {
        val pending = _pendingRequests.value?.size ?: 0
        val approved = _approvedCaregivers.value?.size ?: 0
        return pending + approved
    }

    /**
     * Verifica si una operación específica está en progreso
     */
    fun isOperationInProgress(requestId: String): Boolean {
        return _operationInProgress.value == requestId
    }

    /**
     * Limpia los mensajes de error y éxito
     */
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
        _requestApproved.value = null
        _requestRejected.value = null
    }

    /**
     * Manejo específico de errores para UI
     */
    fun handleError(error: Throwable) {
        when {
            error.message?.contains("network", ignoreCase = true) == true -> {
                _errorMessage.value = "Sin conexión a internet. Verifica tu conexión."
            }
            error.message?.contains("permission", ignoreCase = true) == true -> {
                _errorMessage.value = "No tienes permisos para realizar esta acción."
            }
            error.message?.contains("not found", ignoreCase = true) == true -> {
                _errorMessage.value = "La solicitud ya no existe."
            }
            else -> {
                _errorMessage.value = "Error inesperado: ${error.message}"
            }
        }
    }

    /**
     * Filtros para la UI (futuras implementaciones)
     */
    fun filterRequestsByStatus(status: CaregiverRequest.RequestStatus): List<CaregiverRequest> {
        return when (status) {
            CaregiverRequest.RequestStatus.PENDING -> _pendingRequests.value ?: emptyList()
            CaregiverRequest.RequestStatus.APPROVED -> _approvedCaregivers.value ?: emptyList()
            CaregiverRequest.RequestStatus.REJECTED -> emptyList() // No mostramos rechazados
        }
    }

    /**
     * Limpieza al destruir el ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "CaregiverRequestViewModel destruido - limpiando recursos")
    }
}
