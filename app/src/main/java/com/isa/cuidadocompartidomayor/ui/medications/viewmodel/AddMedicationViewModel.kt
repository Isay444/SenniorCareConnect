package com.isa.cuidadocompartidomayor.ui.medications.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.isa.cuidadocompartidomayor.data.model.*
import com.isa.cuidadocompartidomayor.data.repository.MedicationRepository
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AddMedicationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MedicationRepository(application.applicationContext)
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "AddMedicationViewModel"
    }

    // ========================================
    // DATOS DEL MEDICAMENTO EN CONSTRUCCIÓN
    // ========================================

    // Pantalla 0 - Selección de paciente
    private val _selectedElderlyId = MutableLiveData<String?>()
    val selectedElderlyId: LiveData<String?> = _selectedElderlyId

    private val _selectedElderlyName = MutableLiveData<String?>()
    val selectedElderlyName: LiveData<String?> = _selectedElderlyName

    // Pantalla 1 - Nombre del medicamento
    private val _medicationName = MutableLiveData<String>()
    val medicationName: LiveData<String> = _medicationName

    // Pantalla 2 - Forma del medicamento
    private val _medicationType = MutableLiveData<MedicationType>()
    val medicationType: LiveData<MedicationType> = _medicationType

    // Pantalla 3 - Frecuencia
    private val _frequencyType = MutableLiveData<FrequencyType>()
    val frequencyType: LiveData<FrequencyType> = _frequencyType

    // Pantallas 4-10 - Configuración específica de frecuencia
    private val _timesPerDay = MutableLiveData<Int>(1)
    val timesPerDay: LiveData<Int> = _timesPerDay

    private val _scheduledTimes = MutableLiveData<MutableList<String>>(mutableListOf())
    val scheduledTimes: LiveData<MutableList<String>> = _scheduledTimes

    private val _selectedWeekDays = MutableLiveData<MutableList<Int>>(mutableListOf())
    val selectedWeekDays: LiveData<MutableList<Int>> = _selectedWeekDays

    private val _intervalDays = MutableLiveData<Int?>()
    val intervalDays: LiveData<Int?> = _intervalDays

    private val _intervalWeeks = MutableLiveData<Int?>()
    val intervalWeeks: LiveData<Int?> = _intervalWeeks

    private val _intervalMonths = MutableLiveData<Int?>()
    val intervalMonths: LiveData<Int?> = _intervalMonths

    // Pantalla 11 - Dosis
    private val _dosage = MutableLiveData<String>()
    val dosage: LiveData<String> = _dosage

    // Pantalla 12 - Instrucciones
    private val _instructions = MutableLiveData<String>()
    val instructions: LiveData<String> = _instructions

    // ESTADO DEL WIZARD

    private val _isSaving = MutableLiveData<Boolean>()
    val isSaving: LiveData<Boolean> = _isSaving

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _saveError = MutableLiveData<String?>()
    val saveError: LiveData<String?> = _saveError

    // MÉTODOS - SETTERS

    fun setSelectedElderly(elderlyId: String, elderlyName: String) {
        _selectedElderlyId.value = elderlyId
        _selectedElderlyName.value = elderlyName
        Log.d(TAG, "✅ Paciente seleccionado: $elderlyName ($elderlyId)")
    }

    fun setMedicationName(name: String) {
        _medicationName.value = name
        Log.d(TAG, "✅ Nombre: $name")
    }

    fun setMedicationType(type: MedicationType) {
        _medicationType.value = type
        Log.d(TAG, "✅ Tipo: ${type.displayName}")
    }

    fun setFrequencyType(type: FrequencyType) {
        _frequencyType.value = type
        Log.d(TAG, "✅ Frecuencia: ${type.displayName}")
    }

    fun setTimesPerDay(times: Int) {
        _timesPerDay.value = times

        // Inicializar lista de horarios con el tamaño correcto
        val currentTimes = _scheduledTimes.value ?: mutableListOf()
        if (currentTimes.size != times) {
            currentTimes.clear()
            repeat(times) { currentTimes.add("00:00") }
            _scheduledTimes.value = currentTimes
        }

        Log.d(TAG, "✅ Veces al día: $times")
    }

    fun updateScheduledTime(index: Int, time: String) {
        val times = _scheduledTimes.value ?: mutableListOf()
        if (index < times.size) {
            times[index] = time
            _scheduledTimes.value = times
            Log.d(TAG, "✅ Hora actualizada [$index]: $time")
        }
    }

    fun setScheduledTimes(times: List<String>) {
        _scheduledTimes.value = times.toMutableList()
        Log.d(TAG, "✅ Horarios establecidos: $times")
    }

    fun toggleWeekDay(dayIndex: Int) {
        val days = _selectedWeekDays.value ?: mutableListOf()
        if (days.contains(dayIndex)) {
            days.remove(dayIndex)
        } else {
            days.add(dayIndex)
        }
        days.sort()
        _selectedWeekDays.value = days
        Log.d(TAG, "✅ Días seleccionados: $days")
    }

    fun setIntervalDays(days: Int) {
        _intervalDays.value = days
        Log.d(TAG, "✅ Intervalo días: $days")
    }

    fun setIntervalWeeks(weeks: Int) {
        _intervalWeeks.value = weeks
        Log.d(TAG, "✅ Intervalo semanas: $weeks")
    }

    fun setIntervalMonths(months: Int) {
        _intervalMonths.value = months
        Log.d(TAG, "✅ Intervalo meses: $months")
    }

    fun setDosage(dosage: String) {
        _dosage.value = dosage
        Log.d(TAG, "✅ Dosis: $dosage")
    }

    fun setInstructions(instructions: String) {
        _instructions.value = instructions
        Log.d(TAG, "✅ Instrucciones: $instructions")
    }

    // MÉTODOS - VALIDACIÓN Y GUARDADO

    /** Valida que todos los campos obligatorios estén completos */
    fun validateData(): Boolean {
        if (_selectedElderlyId.value.isNullOrEmpty()) {
            _saveError.value = "Debe seleccionar un paciente"
            return false
        }

        if (_medicationName.value.isNullOrEmpty()) {
            _saveError.value = "Debe ingresar el nombre del medicamento"
            return false
        }

        if (_dosage.value.isNullOrEmpty()) {
            _saveError.value = "Debe ingresar la dosis"
            return false
        }

        if (_scheduledTimes.value.isNullOrEmpty()) {
            _saveError.value = "Debe configurar al menos un horario"
            return false
        }

        // Validaciones específicas por tipo de frecuencia
        when (_frequencyType.value) {
            FrequencyType.SPECIFIC_DAYS -> {
                if (_selectedWeekDays.value.isNullOrEmpty()) {
                    _saveError.value = "Debe seleccionar al menos un día de la semana"
                    return false
                }
            }
            FrequencyType.EVERY_X_DAYS -> {
                if (_intervalDays.value == null || _intervalDays.value!! <= 0) {
                    _saveError.value = "Debe especificar el intervalo de días"
                    return false
                }
            }
            FrequencyType.EVERY_X_WEEKS -> {
                if (_intervalWeeks.value == null || _intervalWeeks.value!! <= 0) {
                    _saveError.value = "Debe especificar el intervalo de semanas"
                    return false
                }
            }
            FrequencyType.EVERY_X_MONTHS -> {
                if (_intervalMonths.value == null || _intervalMonths.value!! <= 0) {
                    _saveError.value = "Debe especificar el intervalo de meses"
                    return false
                }
            }
            else -> { /* No se requiere validación adicional */ }
        }

        return true
    }

    /** Guarda el medicamento en Firestore */
    fun saveMedication() {
        if (!validateData()) {
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null

            val currentUser = auth.currentUser
            if (currentUser == null) {
                _saveError.value = "Usuario no autenticado"
                _isSaving.value = false
                return@launch
            }

            // : Obtener nombre del cuidador desde Firestore
            val caregiverName = try {
                val userDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                userDoc.getString("name") ?: currentUser.displayName ?: "Cuidador"
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo obtener nombre del cuidador", e)
                currentUser.displayName ?: "Cuidador"
            }

            val medication = Medication(
                elderlyId = _selectedElderlyId.value!!,
                elderlyName = _selectedElderlyName.value!!,
                caregiverId = currentUser.uid,
                caregiverName = caregiverName,
                name = _medicationName.value!!,
                medicationType = _medicationType.value ?: MedicationType.PILL,
                dosage = _dosage.value!!,
                instructions = _instructions.value ?: "",
                frequencyType = _frequencyType.value ?: FrequencyType.ONCE_DAILY,
                timesPerDay = _timesPerDay.value ?: 1,
                intervalDays = _intervalDays.value,
                intervalWeeks = _intervalWeeks.value,
                intervalMonths = _intervalMonths.value,
                weekDays = _selectedWeekDays.value ?: emptyList(),
                scheduledTimes = _scheduledTimes.value ?: emptyList()
            )

            val result = repository.createMedication(medication)

            result.onSuccess {
                Log.d(TAG, "✅ Medicamento guardado exitosamente")
                _saveSuccess.value = true
            }.onFailure { exception ->
                Log.e(TAG, "❌ Error guardando medicamento", exception)
                _saveError.value = exception.message ?: "Error al guardar"
            }

            _isSaving.value = false
        }
    }

    /** Reinicia todos los datos del wizard */
    fun resetWizard() {
        _selectedElderlyId.value = null
        _selectedElderlyName.value = null
        _medicationName.value = ""
        _medicationType.value = MedicationType.PILL
        _frequencyType.value = FrequencyType.ONCE_DAILY
        _timesPerDay.value = 1
        _scheduledTimes.value = mutableListOf()
        _selectedWeekDays.value = mutableListOf()
        _intervalDays.value = null
        _intervalWeeks.value = null
        _intervalMonths.value = null
        _dosage.value = ""
        _instructions.value = ""
        _saveSuccess.value = false
        _saveError.value = null

        Log.d(TAG, "🔄 Wizard reiniciado")
    }

    /** Limpia mensajes de error */
    fun clearError() {
        _saveError.value = null
    }
}