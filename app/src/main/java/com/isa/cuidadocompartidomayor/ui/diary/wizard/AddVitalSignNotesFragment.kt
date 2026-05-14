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
import com.isa.cuidadocompartidomayor.databinding.FragmentAddVitalSignNotesBinding
import com.isa.cuidadocompartidomayor.ui.diary.wizard.viewmodel.AddVitalSignViewModel

class AddVitalSignNotesFragment : Fragment() {

    private var _binding: FragmentAddVitalSignNotesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddVitalSignViewModel by activityViewModels()
    private val args: AddVitalSignNotesFragmentArgs by navArgs()

    companion object {
        private const val TAG = "AddVitalSignNotes"
    }

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View {
        _binding = FragmentAddVitalSignNotesBinding.inflate(inflater, container, false)
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
        val summary = buildString {
            viewModel.heartRate.value?.let {
                append("❤️ Pulso: $it LPM\n")
            }

            viewModel.glucose.value?.let { glucose ->
                append("🩸 Glucosa: $glucose mg/dL")
                viewModel.glucoseMoment.value?.let { moment ->
                    append(" (${moment.displayName})")
                }
                append("\n")
            }

            viewModel.temperature.value?.let {
                append("🌡️ Temperatura: $it°C\n")
            }

            if (viewModel.systolicBP.value != null && viewModel.diastolicBP.value != null) {
                append("💉 Presión: ${viewModel.systolicBP.value}/${viewModel.diastolicBP.value} mmHg\n")
            }

            viewModel.oxygenSaturation.value?.let {
                append("💨 Saturación O₂: $it%\n")
            }

            viewModel.weight.value?.let {
                append("⚖️ Peso: $it Kg\n")
            }
        }

        binding.tvSummary.text = summary.trim()
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
            saveVitalSign()
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
                    "✅ Signos vitales guardados exitosamente"
                }
                Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_SHORT
                ).show()

                // Limpiar ViewModel y volver al DiaryFragment
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

    private fun saveVitalSign() {
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
        // Navegar de vuelta al DiaryFragment y limpiar el back stack
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
