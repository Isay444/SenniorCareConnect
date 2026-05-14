package com.isa.cuidadocompartidomayor.ui.medications.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.databinding.FragmentScheduleSingleDoseBinding
import com.isa.cuidadocompartidomayor.ui.medications.viewmodel.AddMedicationViewModel

class ScheduleSingleDoseFragment : Fragment() {

    private var _binding: FragmentScheduleSingleDoseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMedicationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleSingleDoseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupNumberPickers()
        setupContinueButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupNumberPickers() {
        // NumberPicker de horas (01-12)
        binding.npHour.apply {
            minValue = 1
            maxValue = 12
            value = 8
            setFormatter { String.format("%02d", it) }
            wrapSelectorWheel = true
        }

        // NumberPicker de minutos (00-59)
        binding.npMinute.apply {
            minValue = 0
            maxValue = 59
            value = 0
            setFormatter { String.format("%02d", it) }
            wrapSelectorWheel = true
        }

        // NumberPicker de AM/PM
        binding.npAmPm.apply {
            minValue = 0
            maxValue = 1
            displayedValues = arrayOf("AM", "PM")
            value = 0
            wrapSelectorWheel = false
        }
    }

    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            val hour = binding.npHour.value
            val minute = binding.npMinute.value
            val isPM = binding.npAmPm.value == 1

            // Convertir a formato 24 horas
            var hour24 = if (isPM && hour != 12) hour + 12 else hour
            if (!isPM && hour == 12) hour24 = 0

            val timeString = String.format("%02d:%02d", hour24, minute)

            // Guardar la hora en el ViewModel
            viewModel.setScheduledTimes(listOf(timeString))

            // Navegar a la pantalla de dosis
            findNavController().navigate(
                R.id.action_scheduleSingleDoseFragment_to_enterDosageFragment
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}