package com.isa.cuidadocompartidomayor.ui.caregiver.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.isa.cuidadocompartidomayor.data.model.ElderlyItem
import com.isa.cuidadocompartidomayor.data.model.Relationship
import com.isa.cuidadocompartidomayor.data.repository.ConnectionRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ConnectionViewModel: ViewModel() {

    private val connectionRepository = ConnectionRepository()

    companion object{
        private const val TAG = "ConnectionViewModel"
    }

    //Estados de carga
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: MutableLiveData<Boolean> = _isLoading

    //Resultado dela conexion
    private val _connectionResult = MutableLiveData<Result<Relationship>?>()
    val connectionResult: LiveData<Result<Relationship>?> = _connectionResult

    //Lista de relaciones del cuidador
    private val _caregiverRelationships = MutableLiveData<List<Relationship>>()
    val caregiverRelationships: LiveData<List<Relationship>> = _caregiverRelationships

    // Lista de pacientes simplificada con foto
    private val _elderlyPatients = MutableLiveData<List<ElderlyItem>>()
    val elderlyPatients: LiveData<List<ElderlyItem>> = _elderlyPatients

    //Mensaje de error
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /** Conectar al cuidador con un adulto mayor usando un código de invitacion */
    fun connectToElderly(inviteCode: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                Log.d(TAG, "Iniciando conexión con código: $inviteCode")

                val result = connectionRepository.connectToElderly(inviteCode)
                _connectionResult.value = result

                if(result.isSuccess){
                    Log.d(TAG, "Conexión exitosa: ${result.getOrNull()}")
                    //Recargar la lista de relaciones
                    loadCaregiverRelationships()
                }else{
                    val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                    Log.e(TAG, "Error al conectar: $error")
                    _errorMessage.value = error
                }
            }catch (e: Exception){
                Log.e(TAG, "Error inesperado: ${e.message}", e)
                _connectionResult.value = Result.failure(e)
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** cargar la relaciones del cuidador actual*/
    fun loadCaregiverRelationships() {
        val db = FirebaseFirestore.getInstance()
        viewModelScope.launch {
            try {
                Log.d(TAG, "Cargando relaciones del cuidador")

                val result = connectionRepository.getCaregiverRelationships(getCurrentUserId())

                if (result.isSuccess) {
                    val relationships = result.getOrNull() ?: emptyList()
                    
                    // Enriquecer con fotos de perfil
                    val elderlyIds = relationships.map { it.elderlyId }.filter { it.isNotEmpty() }
                    
                    if (elderlyIds.isNotEmpty()) {
                        val usersSnapshot = db.collection("users")
                            .whereIn(FieldPath.documentId(), elderlyIds)
                            .get()
                            .await()
                            
                        val photoMap = usersSnapshot.documents.associate { doc ->
                            doc.id to (doc.getString("profileImageUrl") ?: "")
                        }
                        
                        // Actualizar relaciones con la foto
                        val enrichedRelationships = relationships.map { rel ->
                            rel.copy(elderlyProfileImageUrl = photoMap[rel.elderlyId] ?: "")
                        }
                        
                        _caregiverRelationships.value = enrichedRelationships
                        
                        // Crear lista simplificada para elderlyPatients LiveData
                        val patientsList = enrichedRelationships.map { rel ->
                            ElderlyItem(
                                id = rel.elderlyId,
                                name = rel.elderlyName,
                                profileImageUrl = rel.elderlyProfileImageUrl
                            )
                        }
                        _elderlyPatients.value = patientsList
                        
                    } else {
                        _caregiverRelationships.value = relationships
                        _elderlyPatients.value = emptyList()
                    }
                    
                    Log.d(TAG, "Relaciones cargadas: ${relationships.size}")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Error cargando relaciones"
                    Log.e(TAG, "Error cargando relaciones: $error")
                    _errorMessage.value = error
                    _caregiverRelationships.value = emptyList()
                    _elderlyPatients.value = emptyList()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado cargando relaciones: ${e.message}", e)
                _errorMessage.value = e.message
                _caregiverRelationships.value = emptyList()
                _elderlyPatients.value = emptyList()
            }
        }
    }

    /**Limpia el resultado de la conexion*/
    fun clearConnectionResult(){
        _connectionResult.value = null
    }
    /**Limpia el mensaje de error*/
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    /** Obtiene el ID del usuario actual (simulado - en implementación real sería desde FirebaseAuth) */
    private fun getCurrentUserId(): String {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }

    /** Obtiene el conteo de relaciones activas */
    fun getActiveRelationshipsCount(): Int {
        return _caregiverRelationships.value?.count { it.status == "active" } ?: 0
    }

    /** Obtiene el conteo de relaciones pendientes */
    fun getPendingRelationshipsCount(): Int {
        return _caregiverRelationships.value?.count { it.status == "pending" } ?: 0
    }

    /** Verifica si hay relaciones cargadas */
    fun hasRelationships(): Boolean {
        return !_caregiverRelationships.value.isNullOrEmpty()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel limpiado")
    }
}