package com.isa.cuidadocompartidomayor.ui.diary.wizard

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.GlucoseMoment
import com.isa.cuidadocompartidomayor.data.model.VitalSignType
import com.isa.cuidadocompartidomayor.databinding.FragmentEnterVitalSignValuesBinding
import com.isa.cuidadocompartidomayor.ui.diary.wizard.viewmodel.AddVitalSignViewModel
import com.isa.cuidadocompartidomayor.utils.VitalSignValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EnterVitalSignValuesFragment : Fragment() {

    private var _binding: FragmentEnterVitalSignValuesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddVitalSignViewModel by activityViewModels()
    private val args: EnterVitalSignValuesFragmentArgs by navArgs()

    private val inputFields = mutableMapOf<VitalSignType, View>()

    companion object {
        private const val TAG = "EnterVitalSignValues"
    }

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View {
        _binding = FragmentEnterVitalSignValuesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupPatientInfo()
        createDynamicFields()
        setupButtons()

        //  DIAGNÓSTICO: Logs para verificar
        Log.d(TAG, "🔍 args.vitalSignId = ${args.vitalSignId}")
        Log.d(TAG, "🔍 viewModel.isEditMode = ${viewModel.isEditMode.value}")
        Log.d(TAG, "🔍 viewModel.editingVitalSignId = ${viewModel.editingVitalSignId.value}")

        if (viewModel.isEditMode.value == true) {
            binding.toolbar.title = "Editar Valores"
            Log.d(TAG, "🔄 Modo edición detectado, iniciando pre-carga...")
            preloadValues()
        }

        Log.d(TAG, "✅ Fragment inicializado")
    }
    private fun preloadValues() {
        Log.d(TAG, "🔄 Configurando pre-carga de valores en modo edición...")

        //  CLAVE: Usar postDelayed para dar tiempo a que se renderizen los campos
        binding.root.postDelayed({
            Log.d(TAG, "🔄 Iniciando pre-carga de valores...")

            // ========================================
            // PARTE 1: Cargar valores que YA existen
            // ========================================

            // Pulso
            viewModel.heartRate.value?.let { value ->
                inputFields[VitalSignType.HEART_RATE]?.let { layout ->
                    val editText = (layout as? TextInputLayout)?.editText
                    editText?.setText(value.toString())
                    Log.d(TAG, "✅ Pulso pre-cargado: $value")
                }
            }

            // Glucosa
            viewModel.glucose.value?.let { value ->
                inputFields[VitalSignType.GLUCOSE]?.let { layout ->
                    val editText = (layout as? TextInputLayout)?.editText
                    editText?.setText(value.toString())
                    Log.d(TAG, "✅ Glucosa pre-cargada: $value")
                }
            }

            // Temperatura
            viewModel.temperature.value?.let { value ->
                inputFields[VitalSignType.TEMPERATURE]?.let { layout ->
                    val editText = (layout as? TextInputLayout)?.editText
                    editText?.setText(value.toString())
                    Log.d(TAG, "✅ Temperatura pre-cargada: $value")
                }
            }

            // Presión arterial - Sistólica
            viewModel.systolicBP.value?.let { value ->
                binding.layoutForm.children.forEach { view ->
                    if (view is TextInputLayout && view.hint?.contains("Sistólica") == true) {
                        view.editText?.setText(value.toString())
                        Log.d(TAG, "✅ Presión sistólica pre-cargada: $value")
                    }
                }
            }

            // Presión arterial - Diastólica
            viewModel.diastolicBP.value?.let { value ->
                binding.layoutForm.children.forEach { view ->
                    if (view is TextInputLayout && view.hint?.contains("Diastólica") == true) {
                        view.editText?.setText(value.toString())
                        Log.d(TAG, "✅ Presión diastólica pre-cargada: $value")
                    }
                }
            }

            // Saturación de oxígeno
            viewModel.oxygenSaturation.value?.let { value ->
                inputFields[VitalSignType.OXYGEN_SATURATION]?.let { layout ->
                    val editText = (layout as? TextInputLayout)?.editText
                    editText?.setText(value.toString())
                    Log.d(TAG, "✅ Saturación pre-cargada: $value")
                }
            }

            // Peso
            viewModel.weight.value?.let { value ->
                inputFields[VitalSignType.WEIGHT]?.let { layout ->
                    val editText = (layout as? TextInputLayout)?.editText
                    editText?.setText(value.toString())
                    Log.d(TAG, "✅ Peso pre-cargado: $value")
                }
            }

            // Momento de glucosa (si existe el dropdown)
            viewModel.glucoseMoment.value?.let { moment ->
                binding.layoutForm.children.forEach { view ->
                    if (view is TextInputLayout && view.hint?.contains("Momento") == true) {
                        val dropdown = view.editText as? MaterialAutoCompleteTextView
                        dropdown?.setText(moment.displayName, false)
                        Log.d(TAG, "✅ Momento de glucosa pre-cargado: ${moment.displayName}")
                    }
                }
            }

            Log.d(TAG, "✅ Pre-carga de valores completada")

            // ========================================
            // PARTE 2: Registrar observers para cambios futuros
            // ========================================
            registerObservers()

        }, 300) // Dar 300ms para que se renderizen los campos
    }

    //  NUEVO: Metodo separado para observers
    private fun registerObservers() {
        Log.d(TAG, "🔄 Registrando observers para cambios futuros...")

        viewModel.heartRate.observe(viewLifecycleOwner) { value ->
            value?.let {
                inputFields[VitalSignType.HEART_RATE]?.let { layout ->
                    val editText = (layout as? TextInputLayout)?.editText
                    // Solo actualizar si está vacío (para evitar loops)
                    if (editText?.text.isNullOrBlank()) {
                        editText?.setText(it.toString())
                        Log.d(TAG, "🔄 Pulso actualizado: $it")
                    }
                }
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupPatientInfo() {
        binding.tvPatientName.text = "Para: ${args.elderlyName}"
    }

    private fun createDynamicFields() {
        val selectedTypes = args.selectedTypes.split(",")
            .mapNotNull { typeName ->
                try {
                    VitalSignType.valueOf(typeName)
                } catch (e: Exception) {
                    null
                }
            }

        selectedTypes.forEach { type ->
            when (type) {
                VitalSignType.HEART_RATE -> addHeartRateField()
                VitalSignType.GLUCOSE -> addGlucoseField()
                VitalSignType.TEMPERATURE -> addTemperatureField()
                VitalSignType.BLOOD_PRESSURE -> addBloodPressureField()
                VitalSignType.OXYGEN_SATURATION -> addOxygenSaturationField()
                VitalSignType.WEIGHT -> addWeightField()
            }
        }

        Log.d(TAG, "${selectedTypes.size} campos creados")
    }

    private fun addHeartRateField() {
        val textInputLayout = createTextInputLayout(
            hint = "❤️ Pulso (LPM)",
            helperText = "Rango normal: 60- 100 LPM"
        )

        val editText = createEditText(
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        editText.addTextChangedListener { text ->
            val value = text?.toString()?.toIntOrNull()
            val validationResult = VitalSignValidator.validateHeartRate(value)

            if (!validationResult.isValid()) {
                textInputLayout.error = validationResult.getErrorMessage()
            } else {
                textInputLayout.error = null
            }

            viewModel.setHeartRate(value)
        }

        textInputLayout.addView(editText)
        binding.layoutForm.addView(textInputLayout)
        inputFields[VitalSignType.HEART_RATE] = textInputLayout
    }

    private fun addGlucoseField() {
        val textInputLayout = createTextInputLayout(
            hint = "🩸 Glucosa (mg/dL)",
            helperText = "Rango normal en ayunas: 70-100 mg/dL"
        )

        val editText = createEditText(
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        )

        editText.addTextChangedListener { text ->
            val value = text?.toString()?.toDoubleOrNull()

            val validationResult = VitalSignValidator.validateGlucose(value)
            if (!validationResult.isValid()) {
                textInputLayout.error = validationResult.getErrorMessage()
            } else {
                textInputLayout.error = null
            }
            viewModel.setGlucose(value, viewModel.glucoseMoment.value)
        }

        textInputLayout.addView(editText)
        binding.layoutForm.addView(textInputLayout)

        // Dropdown para momento de medición
        val momentLayout = createTextInputLayout(
            hint = "Momento de medición",
            helperText = "Selecciona cuándo se tomó la medición",
            style = TextInputLayout.END_ICON_DROPDOWN_MENU
        )

        val momentDropdown = MaterialAutoCompleteTextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val moments = GlucoseMoment.entries.map { it.displayName }
            setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, moments))

            setOnItemClickListener { _, _, position, _ ->
                val selectedMoment = GlucoseMoment.entries[position]
                viewModel.setGlucose(viewModel.glucose.value, selectedMoment)
            }
        }

        momentLayout.addView(momentDropdown)
        binding.layoutForm.addView(momentLayout)

        inputFields[VitalSignType.GLUCOSE] = textInputLayout
    }

    private fun addTemperatureField() {
        val textInputLayout = createTextInputLayout(
            hint = "🌡️ Temperatura (°C)",
            helperText = "Rango normal: 36.5-37.5°C"
        )

        val editText = createEditText(
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        )

        editText.addTextChangedListener { text ->
            val value = text?.toString()?.toDoubleOrNull()
            //  Validar en tiempo real
            val validationResult = VitalSignValidator.validateTemperature(value)
            if (!validationResult.isValid()) {
                textInputLayout.error = validationResult.getErrorMessage()
            } else {
                textInputLayout.error = null
            }
            viewModel.setTemperature(value)
        }

        textInputLayout.addView(editText)
        binding.layoutForm.addView(textInputLayout)
        inputFields[VitalSignType.TEMPERATURE] = textInputLayout
    }

    private fun addBloodPressureField() {
        // Presión Sistólica
        val systolicLayout = createTextInputLayout(
            hint = "💉 Presión Sistólica (mmHg)",
            helperText = "Número superior (normal: 90-120)"
        )

        val systolicEditText = createEditText(
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        // Presión Diastólica
        val diastolicLayout = createTextInputLayout(
            hint = "💉 Presión Diastólica (mmHg)",
            helperText = "Número inferior (normal: 60-80)"
        )

        val diastolicEditText = createEditText(
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        //  Validar en tiempo real AMBAS presiones juntas
        val validateBothPressures = {
            val systolic = systolicEditText.text?.toString()?.toIntOrNull()
            val diastolic = diastolicEditText.text?.toString()?.toIntOrNull()

            val validationResult = VitalSignValidator.validateBloodPressure(systolic, diastolic)

            if (!validationResult.isValid()) {
                val errorMsg = validationResult.getErrorMessage()
                // Mostrar error en el campo correspondiente
                if (errorMsg?.contains("sistólica") == true) {
                    systolicLayout.error = errorMsg
                    diastolicLayout.error = null
                } else if (errorMsg?.contains("diastólica") == true) {
                    diastolicLayout.error = errorMsg
                    systolicLayout.error = null
                } else {
                    // Error general (ej: sistólica debe ser mayor)
                    systolicLayout.error = errorMsg
                    diastolicLayout.error = errorMsg
                }
            } else {
                systolicLayout.error = null
                diastolicLayout.error = null
            }

            viewModel.setBloodPressure(systolic, diastolic)
        }

        systolicEditText.addTextChangedListener {
            validateBothPressures()
        }

        diastolicEditText.addTextChangedListener {
            validateBothPressures()
        }

        systolicLayout.addView(systolicEditText)
        binding.layoutForm.addView(systolicLayout)

        diastolicLayout.addView(diastolicEditText)
        binding.layoutForm.addView(diastolicLayout)

        inputFields[VitalSignType.BLOOD_PRESSURE] = systolicLayout
    }

    private fun addOxygenSaturationField() {
        val textInputLayout = createTextInputLayout(
            hint = "💨 Saturación de Oxígeno (%)",
            helperText = "Rango normal: 95-100%"
        )

        val editText = createEditText(
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        editText.addTextChangedListener { text ->
            val value = text?.toString()?.toIntOrNull()
            //  Validar en tiempo real
            val validationResult = VitalSignValidator.validateOxygenSaturation(value)
            if (!validationResult.isValid()) {
                textInputLayout.error = validationResult.getErrorMessage()
            } else {
                textInputLayout.error = null
            }
            viewModel.setOxygenSaturation(value)
        }

        textInputLayout.addView(editText)
        binding.layoutForm.addView(textInputLayout)
        inputFields[VitalSignType.OXYGEN_SATURATION] = textInputLayout
    }

    private fun addWeightField() {
        val textInputLayout = createTextInputLayout(
            hint = "⚖️ Peso (Kg)",
            helperText = "Peso corporal"
        )

        val editText = createEditText(
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        )

        editText.addTextChangedListener { text ->
            val value = text?.toString()?.toDoubleOrNull()
            //  Validar en tiempo real
            val validationResult = VitalSignValidator.validateWeight(value)
            if (!validationResult.isValid()) {
                textInputLayout.error = validationResult.getErrorMessage()
            } else {
                textInputLayout.error = null
            }
            viewModel.setWeight(value)
        }

        textInputLayout.addView(editText)
        binding.layoutForm.addView(textInputLayout)
        inputFields[VitalSignType.WEIGHT] = textInputLayout
    }

    // ✅ MÉTODOS CORREGIDOS
    private fun createTextInputLayout(
        hint: String,
        helperText: String,
        style: Int = TextInputLayout.END_ICON_NONE
    ): TextInputLayout {
        return TextInputLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 48
            }
            this.hint = hint
            this.helperText = helperText
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            endIconMode = style
        }
    }

    private fun createEditText(inputType: Int): TextInputEditText {
        return TextInputEditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            this.inputType = inputType
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnContinue.setOnClickListener {
            navigateToNotes()
        }
    }

    // ========================================
    //  NUEVO: Validación antes de continuar
    // ========================================

    private fun validateAllFields(): Boolean {
        var isValid = true

        // Validar cada campo
        val heartRateResult = VitalSignValidator.validateHeartRate(viewModel.heartRate.value)
        if (!heartRateResult.isValid()) {
            isValid = false
            // El error ya se muestra en el campo
        }

        val glucoseResult = VitalSignValidator.validateGlucose(viewModel.glucose.value)
        if (!glucoseResult.isValid()) {
            isValid = false
        }

        val temperatureResult = VitalSignValidator.validateTemperature(viewModel.temperature.value)
        if (!temperatureResult.isValid()) {
            isValid = false
        }

        val bloodPressureResult = VitalSignValidator.validateBloodPressure(
            viewModel.systolicBP.value,
            viewModel.diastolicBP.value
        )
        if (!bloodPressureResult.isValid()) {
            isValid = false
        }

        val oxygenResult = VitalSignValidator.validateOxygenSaturation(viewModel.oxygenSaturation.value)
        if (!oxygenResult.isValid()) {
            isValid = false
        }

        val weightResult = VitalSignValidator.validateWeight(viewModel.weight.value)
        if (!weightResult.isValid()) {
            isValid = false
        }

        //  Validar que al menos un signo esté presente
        val atLeastOneResult = VitalSignValidator.validateAtLeastOneSign(
            viewModel.heartRate.value,
            viewModel.glucose.value,
            viewModel.temperature.value,
            viewModel.systolicBP.value,
            viewModel.diastolicBP.value,
            viewModel.oxygenSaturation.value,
            viewModel.weight.value
        )

        if (!atLeastOneResult.isValid()) {
            android.widget.Toast.makeText(
                requireContext(),
                atLeastOneResult.getErrorMessage(),
                android.widget.Toast.LENGTH_LONG
            ).show()
            return false
        }

        if (!isValid) {
            android.widget.Toast.makeText(
                requireContext(),
                "Por favor corrige los errores en los campos",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }

        return isValid
    }

    private fun navigateToNotes() {
        val vitalSignData = buildString {
            viewModel.heartRate.value?.let { append("heartRate:$it;") }
            viewModel.glucose.value?.let { append("glucose:$it;") }
            viewModel.glucoseMoment.value?.let { append("glucoseMoment:${it.name};") }
            viewModel.temperature.value?.let { append("temperature:$it;") }
            viewModel.systolicBP.value?.let { append("systolicBP:$it;") }
            viewModel.diastolicBP.value?.let { append("diastolicBP:$it;") }
            viewModel.oxygenSaturation.value?.let { append("oxygenSaturation:$it;") }
            viewModel.weight.value?.let { append("weight:$it;") }
        }

        val vitalSignId = args.vitalSignId

        Log.d(TAG, "🔄 Navegando con vitalSignId = $vitalSignId")

        val action = EnterVitalSignValuesFragmentDirections
            .actionEnterValuesToAddNotes(
                elderlyId = args.elderlyId,
                elderlyName = args.elderlyName,
                vitalSignData = vitalSignData,
                vitalSignId = vitalSignId
            )

        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}