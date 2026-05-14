package com.isa.cuidadocompartidomayor.ui.medications.wizard

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.isa.cuidadocompartidomayor.R
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.isa.cuidadocompartidomayor.data.model.Medication
import com.isa.cuidadocompartidomayor.databinding.FragmentEnterInstructionsBinding
import com.isa.cuidadocompartidomayor.ui.medications.viewmodel.AddMedicationViewModel

class EnterInstructionsFragment : Fragment() {

    private var _binding: FragmentEnterInstructionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMedicationViewModel by activityViewModels()

    companion object {
        private const val TAG = "EnterInstructionsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnterInstructionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupFinishButton()
        observeViewModel()
        restoreData()
        showMedicationSummary()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupFinishButton() {
        binding.btnFinish.setOnClickListener {
            val instructions = binding.etInstructions.text.toString().trim()
            viewModel.setInstructions(instructions)

            // Mostrar diálogo de confirmación
            showConfirmationDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.isSaving.observe(viewLifecycleOwner) { isSaving ->
            binding.progressBar.visibility = if (isSaving) View.VISIBLE else View.GONE
            binding.btnFinish.isEnabled = !isSaving
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(
                    requireContext(),
                    "Medicamento guardado exitosamente",
                    Toast.LENGTH_LONG
                ).show()

                // Reiniciar wizard
                viewModel.resetWizard()

                // ✅ Navegar de regreso a medicationsFragment
                try {
                    findNavController().popBackStack(R.id.nav_graph_medications, true)
                    Log.d(TAG, "✅ Wizard cerrado exitosamente")
                } catch (e: Exception) {
                    // Si falla, usar popBackStack como fallback
                    Log.w(TAG, "Usando popBackStack como fallback")
                    findNavController().popBackStack(R.id.medicationsFragment, false)
                }
            }
        }


        viewModel.saveError.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), "Error: $it", Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun restoreData() {
        viewModel.instructions.value?.let { instructions ->
            binding.etInstructions.setText(instructions)
        }
    }

    private fun showMedicationSummary() {
        val summary = buildString {
            append("📋 Resumen del medicamento:\n\n")
            append("Nombre: ${viewModel.medicationName.value}\n")
            append("Tipo: ${viewModel.medicationType.value?.displayName}\n")
            append("Dosis: ${viewModel.dosage.value}\n")
            append("Frecuencia: ${viewModel.frequencyType.value?.displayName}\n")
            append("Horarios: ${viewModel.scheduledTimes.value?.joinToString(", ")}\n")
            append("Adulto Mayor: ${viewModel.selectedElderlyName.value}\n")
        }
        binding.tvSummary.text = summary
    }

    private fun showConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar medicamento")
            .setMessage("¿Deseas guardar este medicamento?")
            .setPositiveButton("Guardar") { _, _ ->
                viewModel.saveMedication()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}