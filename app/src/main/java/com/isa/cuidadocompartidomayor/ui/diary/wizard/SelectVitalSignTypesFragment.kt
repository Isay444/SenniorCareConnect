package com.isa.cuidadocompartidomayor.ui.diary.wizard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.VitalSignType
import com.isa.cuidadocompartidomayor.databinding.FragmentSelectVitalSignTypesBinding
import com.isa.cuidadocompartidomayor.ui.diary.wizard.viewmodel.AddVitalSignViewModel

class SelectVitalSignTypesFragment : Fragment() {

    private var _binding: FragmentSelectVitalSignTypesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddVitalSignViewModel by activityViewModels()
    private val args: SelectVitalSignTypesFragmentArgs by navArgs()

    companion object {
        private const val TAG = "SelectVitalSignTypes"
    }

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View {
        _binding = FragmentSelectVitalSignTypesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupPatientInfo()
        setupCards()
        setupButtons()
        observeViewModel()

        //  DIAGNÓSTICO: Logs para verificar
        Log.d(TAG, "🔍 args.vitalSignId = ${args.vitalSignId}")
        Log.d(TAG, "🔍 viewModel.isEditMode = ${viewModel.isEditMode.value}")
        Log.d(TAG, "🔍 viewModel.editingVitalSignId = ${viewModel.editingVitalSignId.value}")

        //  NUEVO: Actualizar título en modo edición
        if (viewModel.isEditMode.value == true) {
            binding.toolbar.title = "Editar Signos Vitales"
        }

        Log.d(TAG, "✅ Fragment inicializado")
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupPatientInfo() {
        binding.tvPatientName.text = "Para: ${args.elderlyName}"
        viewModel.setElderlyData(args.elderlyId, args.elderlyName)
    }

    private fun setupCards() {
        // Pulso
        binding.cardHeartRate.setOnClickListener {
            toggleCard(VitalSignType.HEART_RATE, binding.cardHeartRate)
        }

        // Glucosa
        binding.cardGlucose.setOnClickListener {
            toggleCard(VitalSignType.GLUCOSE, binding.cardGlucose)
        }

        // Temperatura
        binding.cardTemperature.setOnClickListener {
            toggleCard(VitalSignType.TEMPERATURE, binding.cardTemperature)
        }

        // Presión Arterial
        binding.cardBloodPressure.setOnClickListener {
            toggleCard(VitalSignType.BLOOD_PRESSURE, binding.cardBloodPressure)
        }

        // Saturación O₂
        binding.cardOxygenSaturation.setOnClickListener {
            toggleCard(VitalSignType.OXYGEN_SATURATION, binding.cardOxygenSaturation)
        }

        // Peso
        binding.cardWeight.setOnClickListener {
            toggleCard(VitalSignType.WEIGHT, binding.cardWeight)
        }
    }

    private fun toggleCard(type: VitalSignType, cardView: com.google.android.material.card.MaterialCardView) {
        viewModel.toggleVitalSignType(type)
        updateCardAppearance(type, cardView)
    }

    private fun updateCardAppearance(
        type: VitalSignType,
        cardView: com.google.android.material.card.MaterialCardView
    ) {
        val isSelected = viewModel.isTypeSelected(type)

        if (isSelected) {
            // Seleccionado
            cardView.strokeWidth = 4
            cardView.strokeColor = ContextCompat.getColor(requireContext(), R.color.azul_principal)
            cardView.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.background_secondary)
            )
        } else {
            // No seleccionado
            cardView.strokeWidth = 2
            cardView.strokeColor = ContextCompat.getColor(requireContext(), R.color.card_stroke)
            cardView.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), android.R.color.white)
            )
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.diaryFragment)
        }

        binding.btnContinue.setOnClickListener {
            navigateToEnterValues()
        }
    }

    private fun observeViewModel() {
        viewModel.selectedTypes.observe(viewLifecycleOwner) { selectedTypes ->
            binding.btnContinue.isEnabled = selectedTypes.isNotEmpty()

            // Actualizar apariencia de todas las cards
            updateCardAppearance(VitalSignType.HEART_RATE, binding.cardHeartRate)
            updateCardAppearance(VitalSignType.GLUCOSE, binding.cardGlucose)
            updateCardAppearance(VitalSignType.TEMPERATURE, binding.cardTemperature)
            updateCardAppearance(VitalSignType.BLOOD_PRESSURE, binding.cardBloodPressure)
            updateCardAppearance(VitalSignType.OXYGEN_SATURATION, binding.cardOxygenSaturation)
            updateCardAppearance(VitalSignType.WEIGHT, binding.cardWeight)

            Log.d(TAG, "${selectedTypes.size} tipos seleccionados")
        }
    }

    private fun navigateToEnterValues() {
        val selectedTypesString = viewModel.selectedTypes.value
            ?.joinToString(",") { it.name } ?: ""

        // ✅ Ahora args.vitalSignId tiene valor
        val vitalSignId = args.vitalSignId

        Log.d(TAG, "🔄 Navegando a EnterValues con vitalSignId = $vitalSignId")

        val action = SelectVitalSignTypesFragmentDirections
            .actionSelectTypesToEnterValues(
                elderlyId = args.elderlyId,
                elderlyName = args.elderlyName,
                selectedTypes = selectedTypesString,
                vitalSignId = vitalSignId //  Pasar el ID
            )

        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
