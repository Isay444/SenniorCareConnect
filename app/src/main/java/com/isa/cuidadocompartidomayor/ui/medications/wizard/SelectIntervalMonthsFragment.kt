package com.isa.cuidadocompartidomayor.ui.medications.wizard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.isa.cuidadocompartidomayor.R
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.isa.cuidadocompartidomayor.databinding.FragmentSelectIntervalMonthsBinding
import com.isa.cuidadocompartidomayor.ui.medications.viewmodel.AddMedicationViewModel

class SelectIntervalMonthsFragment : Fragment() {

    private var _binding: FragmentSelectIntervalMonthsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMedicationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectIntervalMonthsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupNumberPicker()
        setupContinueButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupNumberPicker() {
        binding.npIntervalMonths.apply {
            minValue = 1
            maxValue = 12
            value = 1
            wrapSelectorWheel = true
        }

        binding.npIntervalMonths.setOnValueChangedListener { _, _, newVal ->
            updateSummaryText(newVal)
        }

        updateSummaryText(binding.npIntervalMonths.value)
    }

    private fun updateSummaryText(months: Int) {
        val text = if (months == 1) "Cada mes" else "Cada $months meses"
        binding.tvSummary.text = text
    }

    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            val months = binding.npIntervalMonths.value
            viewModel.setIntervalMonths(months)

            // Navegar a seleccionar veces al día
            findNavController().navigate(
                R.id.action_selectIntervalMonthsFragment_to_selectTimesPerDayFragment
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}