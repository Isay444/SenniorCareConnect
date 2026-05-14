package com.isa.cuidadocompartidomayor.ui.diary.wizard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.isa.cuidadocompartidomayor.databinding.FragmentAddMoodNotesBinding
import com.isa.cuidadocompartidomayor.ui.diary.wizard.viewmodel.AddMoodEntryViewModel

class AddMoodNotesFragment : Fragment() {

    private var _binding: FragmentAddMoodNotesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMoodEntryViewModel by activityViewModels()
    private val args: AddMoodNotesFragmentArgs by navArgs()

    companion object {
        private const val TAG = "AddMoodNotes"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMoodNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupPatientInfo()
        displaySummary()
        setupNotesField()
        setupButtons()
        observeViewModel()

        // Actualizar título y botón en modo edición
        if (viewModel.isEditMode.value == true) {
            binding.toolbar.title = "Actualizar Registro"
            binding.btnSave.text = "✓ Actualizar"
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

    private fun displaySummary() {
        // Emociones
        val emotions = viewModel.selectedEmotions.value
        if (emotions != null && emotions.isNotEmpty()) {
            binding.layoutEmotionsSummary.visibility = View.VISIBLE
            binding.tvEmotionsSummary.text = emotions.joinToString(", ") {
                "${it.emoji} ${it.displayName}"
            }
        } else {
            binding.layoutEmotionsSummary.visibility = View.GONE
        }

        // Síntomas
        val symptoms = viewModel.selectedSymptoms.value
        if (symptoms != null && symptoms.isNotEmpty()) {
            binding.layoutSymptomsSummary.visibility = View.VISIBLE
            binding.tvSymptomsSummary.text = symptoms.joinToString(", ") {
                "${it.emoji} ${it.displayName}"
            }
        } else {
            binding.layoutSymptomsSummary.visibility = View.GONE
        }

        // Niveles
        val levelsSummary = buildString {
            viewModel.energyLevel.value?.let {
                append("${it.emoji} Energía: ${it.displayName}\n")
            }
            viewModel.appetiteLevel.value?.let {
                append("${it.emoji} Apetito: ${it.displayName}\n")
            }
            viewModel.functionalCapacity.value?.let {
                append("${it.emoji} Capacidad: ${it.displayName}")
            }
        }

        if (levelsSummary.isNotEmpty()) {
            binding.layoutLevelsSummary.visibility = View.VISIBLE
            binding.tvLevelsSummary.text = levelsSummary.trim()
        } else {
            binding.layoutLevelsSummary.visibility = View.GONE
        }

        Log.d(TAG, "Resumen mostrado")
    }

    private fun setupNotesField() {
        binding.etNotes.addTextChangedListener { text ->
            viewModel.setNotes(text?.toString() ?: "")
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSave.setOnClickListener {
            saveMoodEntry()
        }
    }

    private fun observeViewModel() {
        viewModel.isSaving.observe(viewLifecycleOwner) { isSaving ->
            if (isSaving) {
                showLoading()
            } else {
                hideLoading()
            }
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                //  NUEVO: Mensaje diferenciado
                val message = if (viewModel.isEditMode.value == true) {
                    "✅ Registro actualizado exitosamente"
                } else {
                    "✅ Estado de ánimo guardado exitosamente"
                }

                Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_SHORT
                ).show()

                viewModel.reset()
                navigateBackToDiary()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(
                    requireContext(),
                    "❌ Error: $it",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.clearError()
            }
        }
    }

    private fun saveMoodEntry() {
        binding.btnSave.isEnabled = false
        binding.btnBack.isEnabled = false
        viewModel.validateAndSave()
    }

    private fun showLoading() {
        binding.loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.loadingOverlay.visibility = View.GONE
        binding.btnSave.isEnabled = true
        binding.btnBack.isEnabled = true
    }

    private fun navigateBackToDiary() {
        findNavController().popBackStack(
            com.isa.cuidadocompartidomayor.R.id.diaryFragment,
            false
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
