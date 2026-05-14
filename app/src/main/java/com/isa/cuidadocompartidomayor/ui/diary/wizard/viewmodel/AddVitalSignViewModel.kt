package com.isa.cuidadocompartidomayor.ui.diary.wizard.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.isa.cuidadocompartidomayor.data.model.*
import com.isa.cuidadocompartidomayor.data.repository.DiaryRepository
import com.isa.cuidadocompartidomayor.utils.VitalSignValidator
import kotlinx.coroutines.launch

class AddVitalSignViewModel : ViewModel() {

    private val repository = DiaryRepository()
    private val auth = FirebaseAuth.getInstance()

    // Datos del paciente seleccionado
    private val _elderlyId = MutableLiveData<String>()
    val elderlyId: LiveData<String> = _elderlyId

    private val _elderlyName = MutableLiveData<String>()
    val elderlyName: LiveData<String> = _elderlyName

    // Tipos de signos vitales seleccionados
    private val _selectedTypes = MutableLiveData<Set<VitalSignType>>(emptySet())
    val selectedTypes: LiveData<Set<VitalSignType>> = _selectedTypes

    // Valores ingresados
    private val _heartRate = MutableLiveData<Int?>()
    val heartRate: LiveData<Int?> = _heartRate

    private val _glucose = MutableLiveData<Double?>()
    val glucose: LiveData<Double?> = _glucose

    private val _glucoseMoment = MutableLiveData<GlucoseMoment?>()
    val glucoseMoment: LiveData<GlucoseMoment?> = _glucoseMoment

    private val _temperature = MutableLiveData<Double?>()
    val temperature: LiveData<Double?> = _temperature

    private val _systolicBP = MutableLiveData<Int?>()
    val systolicBP: LiveData<Int?> = _systolicBP

    private val _diastolicBP = MutableLiveData<Int?>()
    val diastolicBP: LiveData<Int?> = _diastolicBP

    private val _oxygenSaturation = MutableLiveData<Int?>()
    val oxygenSaturation: LiveData<Int?> = _oxygenSaturation

    private val _weight = MutableLiveData<Double?>()
    val weight: LiveData<Double?> = _weight

    private val _notes = MutableLiveData<String>("")
    val notes: LiveData<String> = _notes

    // Estado de guardado
    private val _isSaving = MutableLiveData<Boolean>(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    //  NUEVO: Modo edición
    private val _isEditMode = MutableLiveData<Boolean>(false)
    val isEditMode: LiveData<Boolean> = _isEditMode
    private val _editingVitalSignId = MutableLiveData<String?>()
    val editingVitalSignId: LiveData<String?> = _editingVitalSignId

    companion object {
        private const val TAG = "AddVitalSignViewModel"
    }

    //  NUEVO: Cargar datos existentes para edición
    fun loadVitalSignForEdit(vitalSignId: String) {
        _editingVitalSignId.value = vitalSignId
        _isEditMode.value = true

        viewModelScope.launch {
            try {
                val result = repository.getVitalSignById(vitalSignId)

                result.onSuccess { vitalSign ->
                    // Pre-cargar datos
                    _elderlyId.value = vitalSign.elderlyId
                    _elderlyName.value = vitalSign.elderlyName
                    _heartRate.value = vitalSign.heartRate
                    _glucose.value = vitalSign.glucose
                    // Convertir String a enum GlucoseMoment
                    _glucoseMoment.value = vitalSign.glucoseMoment?.let { momentString ->
                        try {
                            GlucoseMoment.valueOf(momentString)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parseando GlucoseMoment: $momentString")
                            null
                        }
                    }
                    _temperature.value = vitalSign.temperature
                    _systolicBP.value = vitalSign.systolicBP
                    _diastolicBP.value = vitalSign.diastolicBP
                    _oxygenSaturation.value = vitalSign.oxygenSaturation
                    _weight.value = vitalSign.weight
                    _notes.value = vitalSign.notes ?: ""

                    // Pre-seleccionar tipos
                    val types = mutableSetOf<VitalSignType>()
                    if (vitalSign.heartRate != null) types.add(VitalSignType.HEART_RATE)
                    if (vitalSign.glucose != null) types.add(VitalSignType.GLUCOSE)
                    if (vitalSign.temperature != null) types.add(VitalSignType.TEMPERATURE)
                    if (vitalSign.systolicBP != null) types.add(VitalSignType.BLOOD_PRESSURE)
                    if (vitalSign.oxygenSaturation != null) types.add(VitalSignType.OXYGEN_SATURATION)
                    if (vitalSign.weight != null) types.add(VitalSignType.WEIGHT)
                    _selectedTypes.value = types

                    Log.d(TAG, "✅ Datos cargados para edición")
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

    //  NUEVO: Metodo para establecer modo edición explícitamente
    fun setEditMode(isEdit: Boolean, vitalSignId: String?) {
        _isEditMode.value = isEdit
        _editingVitalSignId.value = vitalSignId
        Log.d(TAG, "🔄 Modo edición establecido: $isEdit, ID: $vitalSignId")
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
    // Paso 2: Selección de tipos
    // ========================================

    fun toggleVitalSignType(type: VitalSignType) {
        val current = _selectedTypes.value?.toMutableSet() ?: mutableSetOf()
        if (current.contains(type)) {
            current.remove(type)
        } else {
            current.add(type)
        }
        _selectedTypes.value = current
        Log.d(TAG, "Tipos seleccionados: ${current.size}")
    }

    fun isTypeSelected(type: VitalSignType): Boolean {
        return _selectedTypes.value?.contains(type) == true
    }

    // ========================================
    // Paso 3: Valores
    // ========================================

    fun setHeartRate(value: Int?) {
        _heartRate.value = value
    }

    fun setGlucose(value: Double?, moment: GlucoseMoment?) {
        _glucose.value = value
        _glucoseMoment.value = moment
    }

    fun setTemperature(value: Double?) {
        _temperature.value = value
    }

    fun setBloodPressure(systolic: Int?, diastolic: Int?) {
        _systolicBP.value = systolic
        _diastolicBP.value = diastolic
    }

    fun setOxygenSaturation(value: Int?) {
        _oxygenSaturation.value = value
    }

    fun setWeight(value: Double?) {
        _weight.value = value
    }

    // ========================================
    // Paso 4: Notas
    // ========================================

    fun setNotes(text: String) {
        _notes.value = text
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

        //  Validar usando VitalSignValidator
        val atLeastOneResult = VitalSignValidator.validateAtLeastOneSign(
            _heartRate.value,
            _glucose.value,
            _temperature.value,
            _systolicBP.value,
            _diastolicBP.value,
            _oxygenSaturation.value,
            _weight.value
        )

        if (!atLeastOneResult.isValid()) {
            _errorMessage.value = atLeastOneResult.getErrorMessage()
            return
        }

        //  Validar cada campo individualmente
        val validations = listOf(
            VitalSignValidator.validateHeartRate(_heartRate.value),
            VitalSignValidator.validateGlucose(_glucose.value),
            VitalSignValidator.validateTemperature(_temperature.value),
            VitalSignValidator.validateBloodPressure(_systolicBP.value, _diastolicBP.value),
            VitalSignValidator.validateOxygenSaturation(_oxygenSaturation.value),
            VitalSignValidator.validateWeight(_weight.value)
        )

        val firstError = validations.firstOrNull { !it.isValid() }
        if (firstError != null) {
            _errorMessage.value = firstError.getErrorMessage()
            return
        }

        _isSaving.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                //  DECISIÓN: Actualizar o Crear
                val result = if (_isEditMode.value == true && _editingVitalSignId.value != null) {
                    // MODO EDICIÓN
                    repository.updateVitalSign(
                        vitalSignId = _editingVitalSignId.value!!,
                        heartRate = _heartRate.value,
                        glucose = _glucose.value,
                        glucoseMoment = _glucoseMoment.value,
                        temperature = _temperature.value,
                        systolicBP = _systolicBP.value,
                        diastolicBP = _diastolicBP.value,
                        oxygenSaturation = _oxygenSaturation.value,
                        weight = _weight.value,
                        notes = _notes.value ?: ""
                    )
                } else {
                    // MODO CREACIÓN
                    repository.createVitalSign(
                        elderlyId = currentElderlyId,
                        heartRate = _heartRate.value,
                        glucose = _glucose.value,
                        glucoseMoment = _glucoseMoment.value,
                        temperature = _temperature.value,
                        systolicBP = _systolicBP.value,
                        diastolicBP = _diastolicBP.value,
                        oxygenSaturation = _oxygenSaturation.value,
                        weight = _weight.value,
                        notes = _notes.value ?: ""
                    )
                }

                result.onSuccess { vitalSign ->
                    _saveSuccess.value = true
                    val action = if (_isEditMode.value == true) "actualizado" else "guardado"
                    Log.d(TAG, "✅ Signo vital $action: ${vitalSign.id}")
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
        _editingVitalSignId.value = null
        _selectedTypes.value = emptySet()
        _heartRate.value = null
        _glucose.value = null
        _glucoseMoment.value = null
        _temperature.value = null
        _systolicBP.value = null
        _diastolicBP.value = null
        _oxygenSaturation.value = null
        _weight.value = null
        _notes.value = ""
        _saveSuccess.value = false
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
