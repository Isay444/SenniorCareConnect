package com.isa.cuidadocompartidomayor.ui.diary.wizard.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.isa.cuidadocompartidomayor.data.model.*
import com.isa.cuidadocompartidomayor.data.repository.DiaryRepository
import kotlinx.coroutines.launch

class AddMoodEntryViewModel : ViewModel() {

    private val repository = DiaryRepository()
    private val auth = FirebaseAuth.getInstance()

    // Datos del paciente seleccionado
    private val _elderlyId = MutableLiveData<String>()
    val elderlyId: LiveData<String> = _elderlyId

    private val _elderlyName = MutableLiveData<String>()
    val elderlyName: LiveData<String> = _elderlyName

    // Emociones seleccionadas
    private val _selectedEmotions = MutableLiveData<Set<MoodEmotion>>(emptySet())
    val selectedEmotions: LiveData<Set<MoodEmotion>> = _selectedEmotions

    // Síntomas seleccionados
    private val _selectedSymptoms = MutableLiveData<Set<MoodSymptom>>(emptySet())
    val selectedSymptoms: LiveData<Set<MoodSymptom>> = _selectedSymptoms

    // Niveles
    private val _energyLevel = MutableLiveData<EnergyLevel?>()
    val energyLevel: LiveData<EnergyLevel?> = _energyLevel

    private val _appetiteLevel = MutableLiveData<AppetiteLevel?>()
    val appetiteLevel: LiveData<AppetiteLevel?> = _appetiteLevel

    private val _functionalCapacity = MutableLiveData<FunctionalCapacity?>()
    val functionalCapacity: LiveData<FunctionalCapacity?> = _functionalCapacity

    // Notas
    private val _notes = MutableLiveData<String>("")
    val notes: LiveData<String> = _notes

    // Estado de guardado
    private val _isSaving = MutableLiveData<Boolean>(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isEditMode = MutableLiveData<Boolean>(false)
    val isEditMode: LiveData<Boolean> = _isEditMode
    private val _editingMoodEntryId = MutableLiveData<String?>()
    val editingMoodEntryId: LiveData<String?> = _editingMoodEntryId


    companion object {
        private const val TAG = "AddMoodEntryViewModel"
    }

    fun setEditMode(isEdit: Boolean, moodEntryId: String?) {
        _isEditMode.value = isEdit
        _editingMoodEntryId.value = moodEntryId
        Log.d(TAG, "🔄 Modo edición establecido: $isEdit, ID: $moodEntryId")
    }

    // ========================================
    // Setters para el paso 1
    // ========================================

    fun setElderlyData(elderlyId: String, elderlyName: String) {
        _elderlyId.value = elderlyId
        _elderlyName.value = elderlyName
        Log.d(TAG, "Paciente seleccionado: $elderlyName ($elderlyId)")
    }

    // ========================================
    // Paso 2: Emociones
    // ========================================

    fun toggleEmotion(emotion: MoodEmotion) {
        val current = _selectedEmotions.value?.toMutableSet() ?: mutableSetOf()
        if (current.contains(emotion)) {
            current.remove(emotion)
        } else {
            current.add(emotion)
        }
        _selectedEmotions.value = current
        Log.d(TAG, "Emociones seleccionadas: ${current.size}")
    }

    fun isEmotionSelected(emotion: MoodEmotion): Boolean {
        return _selectedEmotions.value?.contains(emotion) == true
    }

    // ========================================
    // Paso 3: Síntomas y Niveles
    // ========================================

    fun toggleSymptom(symptom: MoodSymptom) {
        val current = _selectedSymptoms.value?.toMutableSet() ?: mutableSetOf()
        if (current.contains(symptom)) {
            current.remove(symptom)
        } else {
            current.add(symptom)
        }
        _selectedSymptoms.value = current
        Log.d(TAG, "Síntomas seleccionados: ${current.size}")
    }

    fun isSymptomSelected(symptom: MoodSymptom): Boolean {
        return _selectedSymptoms.value?.contains(symptom) == true
    }

    fun setEnergyLevel(level: EnergyLevel?) {
        _energyLevel.value = level
    }

    fun setAppetiteLevel(level: AppetiteLevel?) {
        _appetiteLevel.value = level
    }

    fun setFunctionalCapacity(capacity: FunctionalCapacity?) {
        _functionalCapacity.value = capacity
    }

    // ========================================
    // Paso 4: Notas
    // ========================================

    fun setNotes(text: String) {
        _notes.value = text
    }

    // : Cargar datos existentes para edición
    fun loadMoodEntryForEdit(moodEntryId: String) {
        _editingMoodEntryId.value = moodEntryId
        _isEditMode.value = true

        viewModelScope.launch {
            try {
                val result = repository.getMoodEntryById(moodEntryId)

                result.onSuccess { moodEntry ->
                    // Pre-cargar datos
                    _elderlyId.value = moodEntry.elderlyId
                    _elderlyName.value = moodEntry.elderlyName

                    // Convertir listas de Strings a enums
                    val emotions = moodEntry.emotions.mapNotNull { emotionString ->
                        try {
                            MoodEmotion.valueOf(emotionString)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parseando emoción: $emotionString")
                            null
                        }
                    }.toSet()
                    _selectedEmotions.value = emotions

                    val symptoms = moodEntry.symptoms.mapNotNull { symptomString ->
                        try {
                            MoodSymptom.valueOf(symptomString)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parseando síntoma: $symptomString")
                            null
                        }
                    }.toSet()
                    _selectedSymptoms.value = symptoms

                    // Convertir niveles de String a enum
                    _energyLevel.value = moodEntry.energyLevel?.let { levelString ->
                        try {
                            EnergyLevel.valueOf(levelString)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parseando energyLevel: $levelString")
                            null
                        }
                    }

                    _appetiteLevel.value = moodEntry.appetiteLevel?.let { levelString ->
                        try {
                            AppetiteLevel.valueOf(levelString)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parseando appetiteLevel: $levelString")
                            null
                        }
                    }

                    _functionalCapacity.value = moodEntry.functionalCapacity?.let { capacityString ->
                        try {
                            FunctionalCapacity.valueOf(capacityString)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parseando functionalCapacity: $capacityString")
                            null
                        }
                    }

                    _notes.value = moodEntry.notes ?: ""

                    Log.d(TAG, "✅ Datos de mood entry cargados para edición")
                }.onFailure { error ->
                    _errorMessage.value = "Error cargando datos: ${error.message}"
                    Log.e(TAG, "❌ Error: ${error.message}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
                Log.e(TAG, "❌ Excepción: ${e.message}")
            }
        }
    }

    // ========================================
    // Validación y Guardado
    // ========================================

    fun validateAndSave() {
        val currentElderlyId = _elderlyId.value
        val currentElderlyName = _elderlyName.value

        if (currentElderlyId == null || currentElderlyName == null) {
            _errorMessage.value = "Error: Datos del paciente no encontrados"
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            _errorMessage.value = "Error: Usuario no autenticado"
            return
        }

        // Validar que al menos una emoción esté seleccionada
        val emotions = _selectedEmotions.value ?: emptySet()
        if (emotions.isEmpty()) {
            _errorMessage.value = "Debes seleccionar al menos una emoción"
            return
        }

        _isSaving.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                //  DECISIÓN: Actualizar o Crear
                val result = if (_isEditMode.value == true && _editingMoodEntryId.value != null) {
                    // MODO EDICIÓN
                    repository.updateMoodEntry(
                        moodEntryId = _editingMoodEntryId.value!!,
                        emotions = emotions.toList(),
                        symptoms = _selectedSymptoms.value?.toList() ?: emptyList(),
                        energyLevel = _energyLevel.value,
                        appetiteLevel = _appetiteLevel.value,
                        functionalCapacity = _functionalCapacity.value,
                        notes = _notes.value ?: ""
                    )
                } else {
                    // MODO CREACIÓN
                    repository.createMoodEntry(
                        elderlyId = currentElderlyId,
                        emotions = emotions.toList(),
                        symptoms = _selectedSymptoms.value?.toList() ?: emptyList(),
                        energyLevel = _energyLevel.value,
                        appetiteLevel = _appetiteLevel.value,
                        functionalCapacity = _functionalCapacity.value,
                        notes = _notes.value ?: ""
                    )
                }

                result.onSuccess { moodEntry ->
                    _saveSuccess.value = true
                    val action = if (_isEditMode.value == true) "actualizado" else "guardado"
                    Log.d(TAG, "✅ Estado de ánimo $action: ${moodEntry.id}")
                }.onFailure { error ->
                    _errorMessage.value = error.message ?: "Error desconocido"
                    Log.e(TAG, "❌ Error: ${error.message}")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error inesperado"
                Log.e(TAG, "❌ Excepción: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    // ========================================
    // Reset
    // ========================================

    fun reset() {
        _isEditMode.value = false
        _editingMoodEntryId.value = null
        _selectedEmotions.value = emptySet()
        _selectedSymptoms.value = emptySet()
        _energyLevel.value = null
        _appetiteLevel.value = null
        _functionalCapacity.value = null
        _notes.value = ""
        _saveSuccess.value = false
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
