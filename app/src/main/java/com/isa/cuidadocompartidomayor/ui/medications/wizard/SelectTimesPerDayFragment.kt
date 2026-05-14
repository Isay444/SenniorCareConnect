package com.isa.cuidadocompartidomayor.ui.medications.wizard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.databinding.FragmentSelectTimesPerDayBinding
import com.isa.cuidadocompartidomayor.ui.medications.viewmodel.AddMedicationViewModel

class SelectTimesPerDayFragment : Fragment() {

    private var _binding: FragmentSelectTimesPerDayBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMedicationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectTimesPerDayBinding.inflate(inflater, container, false)
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
        binding.npTimesPerDay.apply {
            minValue = 1
            maxValue = 10
            value = 3
            wrapSelectorWheel = true
        }

        // Actualizar el texto de resumen
        binding.npTimesPerDay.setOnValueChangedListener { _, _, newVal ->
            updateSummaryText(newVal)
        }

        updateSummaryText(binding.npTimesPerDay.value)
    }

    private fun updateSummaryText(times: Int) {
        val text = if (times == 1) "1 toma al día" else "$times tomas al día"
        binding.tvSummary.text = text
    }

    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            val times = binding.npTimesPerDay.value
            viewModel.setTimesPerDay(times)

            // Navegar a programar múltiples horarios
            findNavController().navigate(
                R.id.action_selectTimesPerDayFragment_to_scheduleMultipleDosesFragment
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}