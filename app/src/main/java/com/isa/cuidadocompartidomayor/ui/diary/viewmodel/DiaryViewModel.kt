package com.isa.cuidadocompartidomayor.ui.diary.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.isa.cuidadocompartidomayor.data.model.ElderlyItem
import com.isa.cuidadocompartidomayor.data.model.MoodEntry
import com.isa.cuidadocompartidomayor.data.model.VitalSign
import com.isa.cuidadocompartidomayor.data.repository.DiaryRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DiaryViewModel : ViewModel() {

    private val repository = DiaryRepository()

    // ========================================
    // Estado de carga
    // ========================================

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // ========================================
    // Lista de Adultos Mayores
    // ========================================

    private val _elderlyList = MutableLiveData<List<ElderlyItem>>()
    val elderlyList: LiveData<List<ElderlyItem>> = _elderlyList

    // ========================================
    // Signos Vitales
    // ========================================

    private val _vitalSigns = MutableLiveData<List<VitalSign>>()
    val vitalSigns: LiveData<List<VitalSign>> = _vitalSigns

    private val _latestVitalSign = MutableLiveData<VitalSign?>()
    val latestVitalSign: LiveData<VitalSign?> = _latestVitalSign

    // ========================================
    // Estados de Ánimo
    // ========================================

    private val _moodEntries = MutableLiveData<List<MoodEntry>>()
    val moodEntries: LiveData<List<MoodEntry>> = _moodEntries

    private val _latestMoodEntry = MutableLiveData<MoodEntry?>()
    val latestMoodEntry: LiveData<MoodEntry?> = _latestMoodEntry

    // ========================================
    // Filtro actual
    // ========================================

    private val _selectedElderlyId = MutableLiveData<String?>()
    val selectedElderlyId: LiveData<String?> = _selectedElderlyId

    companion object {
        private const val TAG = "DiaryViewModel"
        const val ALL_PATIENTS = "ALL_PATIENTS"
    }

    // ========================================
    // Cargar Signos Vitales
    // ========================================

    /**
     * Carga signos vitales según el elderlyId específico → Solo ese adulto mayor
     */
    fun loadVitalSigns(elderlyId: String?, limit: Int = 50) { // Mantener firma para compatibilidad
        if (elderlyId == null) {
            Log.w(TAG, "⚠️ elderlyId es null, no se puede cargar")
            return
        }

        _selectedElderlyId.value = elderlyId
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "📥 Cargando signos vitales de: $elderlyId")
                val result = repository.getVitalSigns(elderlyId, limit)
                /*
                val result = if (elderlyId == "all") {
                    repository.getAllVitalSignsForCaregiver(limit)
                } else {
                    repository.getVitalSigns(elderlyId, limit)
                }
                 */

                result.onSuccess { signs ->
                    _vitalSigns.value = signs
                    _isLoading.value = false
                    Log.d(TAG, "✅ ${signs.size} signos vitales cargados")
                }.onFailure { error ->
                    _errorMessage.value = error.message
                    _isLoading.value = false
                    Log.e(TAG, "❌ Error: ${error.message}")
                }

            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
                Log.e(TAG, "❌ Excepción: ${e.message}")
            }
        }
    }

    /**
     * Carga el último signo vital de un adulto mayor específico
     */
    fun loadLatestVitalSign(elderlyId: String) {
        viewModelScope.launch {
            try {
                val result = repository.getLatestVitalSign(elderlyId)
                result.onSuccess { vitalSign ->
                    _latestVitalSign.value = vitalSign
                    Log.d(TAG, "✅ Último signo vital cargado")
                }.onFailure { error ->
                    Log.e(TAG, "❌ Error cargando último signo: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción: ${e.message}")
            }
        }
    }

    // ========================================
    // Cargar Estados de Ánimo
    // ========================================

    /**
     * Carga estados de ánimo según el filtro
     */
    fun loadMoodEntries(elderlyId: String?, limit: Int = 50) { // Similar al anterior
        if (elderlyId == null) {
            Log.w(TAG, "⚠️ elderlyId es null, no se puede cargar")
            return
        }

        _selectedElderlyId.value = elderlyId
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "📥 Cargando estados de ánimo de: $elderlyId")
                val result = repository.getMoodEntries(elderlyId, limit)
                /*
                val result = if (elderlyId == "all") {
                    repository.getAllMoodEntriesForCaregiver(limit)
                } else {
                    repository.getMoodEntries(elderlyId, limit)
                }
                 */

                result.onSuccess { entries ->
                    _moodEntries.value = entries
                    _isLoading.value = false
                    Log.d(TAG, "✅ ${entries.size} estados de ánimo cargados")
                }.onFailure { error ->
                    _errorMessage.value = error.message
                    _isLoading.value = false
                    Log.e(TAG, "❌ Error: ${error.message}")
                }

            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
                Log.e(TAG, "❌ Excepción: ${e.message}")
            }
        }
    }

    /**
     * Carga el último estado de ánimo de un adulto mayor específico
     */
    fun loadLatestMoodEntry(elderlyId: String) {
        viewModelScope.launch {
            try {
                val result = repository.getLatestMoodEntry(elderlyId)
                result.onSuccess { moodEntry ->
                    _latestMoodEntry.value = moodEntry
                    Log.d(TAG, "✅ Último estado de ánimo cargado")
                }.onFailure { error ->
                    Log.e(TAG, "❌ Error cargando último estado: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción: ${e.message}")
            }
        }
    }

    // ========================================
    // Eliminar Registros
    // ========================================

    /**
     * Elimina un signo vital
     */
    fun deleteVitalSign(vitalSignId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.deleteVitalSign(vitalSignId)
                result.onSuccess {
                    Log.d(TAG, "✅ Signo vital eliminado: $vitalSignId")
                    onSuccess()
                    // Recargar lista
                    _selectedElderlyId.value?.let { elderlyId ->
                        loadVitalSigns(elderlyId)
                    }
                }.onFailure { error ->
                    Log.e(TAG, "❌ Error eliminando: ${error.message}")
                    onError(error.message ?: "Error desconocido")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción: ${e.message}")
                onError(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Elimina un estado de ánimo
     */
    fun deleteMoodEntry(moodEntryId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.deleteMoodEntry(moodEntryId)
                result.onSuccess {
                    Log.d(TAG, "✅ Estado de ánimo eliminado: $moodEntryId")
                    onSuccess()
                    // Recargar lista
                    _selectedElderlyId.value?.let { elderlyId ->
                        loadMoodEntries(elderlyId)
                    }
                }.onFailure { error ->
                    Log.e(TAG, "❌ Error eliminando: ${error.message}")
                    onError(error.message ?: "Error desconocido")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción: ${e.message}")
                onError(e.message ?: "Error desconocido")
            }
        }
    }

    // ========================================
    // Limpiar errores
    // ========================================

    fun clearError() {
        _errorMessage.value = null
    }

    // ========================================
    // Refrescar datos
    // ========================================

    fun refresh() {
        _selectedElderlyId.value?.let { elderlyId ->
            loadVitalSigns(elderlyId)
            loadMoodEntries(elderlyId)
        }
    }

    /**
     * Carga la lista de adultos mayores conectados al cuidador y sus fotos
     */
    fun loadElderlyList(caregiverId: String) {
        val db = FirebaseFirestore.getInstance()
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Paso 1: Obtener relaciones desde Firestore
                val relationshipsSnapshot = db.collection("relationships")
                    .whereEqualTo("caregiverId", caregiverId)
                    .whereEqualTo("status", "active")
                    .get()
                    .await()

                val tempRelationships = relationshipsSnapshot.documents.mapNotNull { doc ->
                    val elderlyId = doc.getString("elderlyId") ?: return@mapNotNull null
                    val elderlyName = doc.getString("elderlyName") ?: "Adulto Mayor"
                    ElderlyItem(id = elderlyId, name = elderlyName)
                }

                if (tempRelationships.isEmpty()) {
                    _elderlyList.value = emptyList()
                    return@launch
                }

                // Paso 2: Batch fetch a la colección "users" para obtener las fotos
                val elderlyIds = tempRelationships.map { it.id }
                
                val usersSnapshot = db.collection("users")
                    .whereIn(FieldPath.documentId(), elderlyIds)
                    .get()
                    .await()

                val photoMap = usersSnapshot.documents.associate { doc ->
                    doc.id to (doc.getString("profileImageUrl") ?: "")
                }

                // Paso 3: Enriquecer cada item con su foto
                val enrichedList = tempRelationships.map { item ->
                    item.copy(profileImageUrl = photoMap[item.id] ?: "")
                }
                /*
                val enrichedList = mutableListOf<ElderlyItem>()
                enrichedList.add(ElderlyItem("all", "Todos los adultos mayores", ""))

                tempRelationships.forEach { item ->
                    enrichedList.add(item.copy(profileImageUrl = photoMap[item.id] ?: ""))
                }
                 */

                _elderlyList.value = enrichedList

            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar pacientes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
