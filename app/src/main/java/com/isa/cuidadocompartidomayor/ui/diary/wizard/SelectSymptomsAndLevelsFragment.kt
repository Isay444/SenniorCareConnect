package com.isa.cuidadocompartidomayor.ui.diary.wizard

import android.content.res.ColorStateList
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
import com.google.android.material.chip.Chip
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.*
import com.isa.cuidadocompartidomayor.databinding.FragmentSelectSymptomsAndLevelsBinding
import com.isa.cuidadocompartidomayor.ui.diary.wizard.viewmodel.AddMoodEntryViewModel

class SelectSymptomsAndLevelsFragment : Fragment() {

    private var _binding: FragmentSelectSymptomsAndLevelsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMoodEntryViewModel by activityViewModels()
    private val args: SelectSymptomsAndLevelsFragmentArgs by navArgs()

    companion object {
        private const val TAG = "SelectSymptomsLevels"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectSymptomsAndLevelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupPatientInfo()
        setupSymptomChips()
        setupLevelRadioButtons()
        setupButtons()

        //  NUEVO: Actualizar título en modo edición
        if (viewModel.isEditMode.value == true) {
            binding.toolbar.title = "Editar Síntomas y Niveles"
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
    }

    private fun setupSymptomChips() {
        // Crear chip para cada síntoma disponible
        MoodSymptom.entries.forEach { symptom ->
            val chip = createSymptomChip(symptom)
            binding.chipGroupSymptoms.addView(chip)
        }
    }

    private fun createSymptomChip(symptom: MoodSymptom): Chip {
        return Chip(requireContext()).apply {
            text = "${symptom.emoji} ${symptom.displayName}"
            isCheckable = true
            isChecked = viewModel.isSymptomSelected(symptom)

            // Estilo del chip
            chipBackgroundColor = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf()
                ),
                intArrayOf(
                    ContextCompat.getColor(context, R.color.status_warning),
                    ContextCompat.getColor(context, R.color.background_secondary)
                )
            )

            setTextColor(
                ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf()
                    ),
                    intArrayOf(
                        ContextCompat.getColor(context, android.R.color.white),
                        ContextCompat.getColor(context, R.color.text_primary)
                    )
                )
            )

            setOnCheckedChangeListener { _, _ ->
                viewModel.toggleSymptom(symptom)
            }
        }
    }

    private fun setupLevelRadioButtons() {
        // Nivel de Energía
        binding.radioGroupEnergy.setOnCheckedChangeListener { _, checkedId ->
            val level = when (checkedId) {
                R.id.rbEnergyHigh -> EnergyLevel.HIGH
                R.id.rbEnergyMedium -> EnergyLevel.MEDIUM
                R.id.rbEnergyLow -> EnergyLevel.LOW
                else -> null
            }
            viewModel.setEnergyLevel(level)
        }

        // Nivel de Apetito
        binding.radioGroupAppetite.setOnCheckedChangeListener { _, checkedId ->
            val level = when (checkedId) {
                R.id.rbAppetiteGood -> AppetiteLevel.GOOD
                R.id.rbAppetiteRegular -> AppetiteLevel.REGULAR
                R.id.rbAppetitePoor -> AppetiteLevel.POOR
                else -> null
            }
            viewModel.setAppetiteLevel(level)
        }

        // Capacidad Funcional
        binding.radioGroupFunctional.setOnCheckedChangeListener { _, checkedId ->
            val level = when (checkedId) {
                R.id.rbFunctionalSufficient -> FunctionalCapacity.SUFFICIENT
                R.id.rbFunctionalRegular -> FunctionalCapacity.REGULAR
                R.id.rbFunctionalInsufficient -> FunctionalCapacity.INSUFFICIENT
                else -> null
            }
            viewModel.setFunctionalCapacity(level)
        }

        // Restaurar selecciones previas si existen
        viewModel.energyLevel.value?.let { energy ->
            when (energy) {
                EnergyLevel.HIGH -> binding.rbEnergyHigh.isChecked = true
                EnergyLevel.MEDIUM -> binding.rbEnergyMedium.isChecked = true
                EnergyLevel.LOW -> binding.rbEnergyLow.isChecked = true
            }
        }

        viewModel.appetiteLevel.value?.let { appetite ->
            when (appetite) {
                AppetiteLevel.GOOD -> binding.rbAppetiteGood.isChecked = true
                AppetiteLevel.REGULAR -> binding.rbAppetiteRegular.isChecked = true
                AppetiteLevel.POOR -> binding.rbAppetitePoor.isChecked = true
            }
        }

        viewModel.functionalCapacity.value?.let { capacity ->
            when (capacity) {
                FunctionalCapacity.SUFFICIENT -> binding.rbFunctionalSufficient.isChecked = true
                FunctionalCapacity.REGULAR -> binding.rbFunctionalRegular.isChecked = true
                FunctionalCapacity.INSUFFICIENT -> binding.rbFunctionalInsufficient.isChecked = true
            }
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

    private fun navigateToNotes() {
        // Serializar datos
        val moodData = buildString {
            viewModel.selectedEmotions.value?.let { emotions ->
                append("emotions:${emotions.joinToString("|") { it.name }};")
            }
            viewModel.selectedSymptoms.value?.let { symptoms ->
                if (symptoms.isNotEmpty()) {
                    append("symptoms:${symptoms.joinToString("|") { it.name }};")
                }
            }
            viewModel.energyLevel.value?.let { append("energyLevel:${it.name};") }
            viewModel.appetiteLevel.value?.let { append("appetiteLevel:${it.name};") }
            viewModel.functionalCapacity.value?.let { append("functionalCapacity:${it.name};") }
        }

        // ✅ Ahora args.vitalSignId tiene valor
        val moodEntryId = args.moodEntryId
        Log.d(TAG, "🔄 Navegando a Notas con moodEntryId = $moodEntryId")

        val action = SelectSymptomsAndLevelsFragmentDirections
            .actionSelectSymptomsLevelsToAddNotes(
                elderlyId = args.elderlyId,
                elderlyName = args.elderlyName,
                moodData = moodData,
                moodEntryId = moodEntryId //  Pasar el ID
            )

        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
