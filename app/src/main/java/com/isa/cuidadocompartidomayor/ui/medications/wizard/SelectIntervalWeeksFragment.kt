package com.isa.cuidadocompartidomayor.ui.medications.wizard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.isa.cuidadocompartidomayor.R
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.isa.cuidadocompartidomayor.databinding.FragmentSelectIntervalWeeksBinding
import com.isa.cuidadocompartidomayor.ui.medications.viewmodel.AddMedicationViewModel

class SelectIntervalWeeksFragment : Fragment() {

    private var _binding: FragmentSelectIntervalWeeksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMedicationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectIntervalWeeksBinding.inflate(inflater, container, false)
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
        binding.npIntervalWeeks.apply {
            minValue = 1
            maxValue = 12
            value = 2
            wrapSelectorWheel = true
        }

        binding.npIntervalWeeks.setOnValueChangedListener { _, _, newVal ->
            updateSummaryText(newVal)
        }

        updateSummaryText(binding.npIntervalWeeks.value)
    }

    private fun updateSummaryText(weeks: Int) {
        val text = if (weeks == 1) "Cada semana" else "Cada $weeks semanas"
        binding.tvSummary.text = text
    }

    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            val weeks = binding.npIntervalWeeks.value
            viewModel.setIntervalWeeks(weeks)

            // Navegar a seleccionar veces al día
            findNavController().navigate(
                R.id.action_selectIntervalWeeksFragment_to_selectTimesPerDayFragment
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}