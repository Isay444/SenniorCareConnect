package com.isa.cuidadocompartidomayor.ui.medications.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.databinding.FragmentMedicationNameBinding
import com.isa.cuidadocompartidomayor.ui.medications.viewmodel.AddMedicationViewModel

class MedicationNameFragment : Fragment() {

    private var _binding: FragmentMedicationNameBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMedicationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicationNameBinding.inflate(inflater, container, false)
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
        binding.etMedicationName.addTextChangedListener { text ->
            binding.btnContinue.isEnabled = !text.isNullOrBlank()
        }
    }

    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            val name = binding.etMedicationName.text.toString().trim()

            if (name.isBlank()) {
                Toast.makeText(requireContext(), "Ingrese el nombre del medicamento", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.setMedicationName(name)

            findNavController().navigate(
                R.id.action_medicationNameFragment_to_selectMedicationTypeFragment
            )
        }
    }

    private fun restoreData() {
        viewModel.medicationName.value?.let { name ->
            binding.etMedicationName.setText(name)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}