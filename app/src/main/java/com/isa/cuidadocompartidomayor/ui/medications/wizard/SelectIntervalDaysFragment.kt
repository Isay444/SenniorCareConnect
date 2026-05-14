package com.isa.cuidadocompartidomayor.ui.medications.wizard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.isa.cuidadocompartidomayor.R
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.isa.cuidadocompartidomayor.databinding.FragmentSelectIntervalDaysBinding
import com.isa.cuidadocompartidomayor.ui.medications.viewmodel.AddMedicationViewModel

class SelectIntervalDaysFragment : Fragment() {

    private var _binding: FragmentSelectIntervalDaysBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMedicationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectIntervalDaysBinding.inflate(inflater, container, false)
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
        binding.npIntervalDays.apply {
            minValue = 1
            maxValue = 30
            value = 2
            wrapSelectorWheel = true
        }

        binding.npIntervalDays.setOnValueChangedListener { _, _, newVal ->
            updateSummaryText(newVal)
        }

        updateSummaryText(binding.npIntervalDays.value)
    }

    private fun updateSummaryText(days: Int) {
        val text = if (days == 1) "Cada día" else "Cada $days días"
        binding.tvSummary.text = text
    }

    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            val days = binding.npIntervalDays.value
            viewModel.setIntervalDays(days)

            // Navegar a seleccionar veces al día
            findNavController().navigate(
                R.id.action_selectIntervalDaysFragment_to_selectTimesPerDayFragment
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}