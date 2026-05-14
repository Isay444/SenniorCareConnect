package com.isa.cuidadocompartidomayor.ui.medications.wizard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.isa.cuidadocompartidomayor.R
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.isa.cuidadocompartidomayor.databinding.FragmentEnterDosageBinding
import com.isa.cuidadocompartidomayor.ui.medications.viewmodel.AddMedicationViewModel

class EnterDosageFragment : Fragment() {

    private var _binding: FragmentEnterDosageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMedicationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnterDosageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupInput()
        setupContinueButton()
        restoreData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupInput() {
        binding.etDosage.addTextChangedListener { text ->
            binding.btnContinue.isEnabled = !text.isNullOrBlank()
        }
    }

    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            val dosage = binding.etDosage.text.toString().trim()

            if (dosage.isBlank()) {
                Toast.makeText(requireContext(), "Ingrese la dosis", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.setDosage(dosage)

            // Navegar a instrucciones
            findNavController().navigate(
                R.id.action_enterDosageFragment_to_enterInstructionsFragment
            )
        }
    }

    private fun restoreData() {
        viewModel.dosage.value?.let { dosage ->
            binding.etDosage.setText(dosage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}