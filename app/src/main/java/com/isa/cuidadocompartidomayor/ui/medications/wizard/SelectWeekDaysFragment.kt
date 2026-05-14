package com.isa.cuidadocompartidomayor.ui.medications.wizard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.isa.cuidadocompartidomayor.R
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.isa.cuidadocompartidomayor.databinding.FragmentSelectWeekDaysBinding
import com.isa.cuidadocompartidomayor.ui.medications.viewmodel.AddMedicationViewModel

class SelectWeekDaysFragment : Fragment() {

    private var _binding: FragmentSelectWeekDaysBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMedicationViewModel by activityViewModels()

    private val dayViews by lazy {
        listOf(
            binding.btnMonday,
            binding.btnTuesday,
            binding.btnWednesday,
            binding.btnThursday,
            binding.btnFriday,
            binding.btnSaturday,
            binding.btnSunday
        )
    }

    private val dayNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectWeekDaysBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupDayButtons()
        setupContinueButton()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupDayButtons() {
        dayViews.forEachIndexed { index, button ->
            button.setOnClickListener {
                viewModel.toggleWeekDay(index + 1) // Los días empiezan en 1
            }
        }
    }

    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            if (viewModel.selectedWeekDays.value.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Seleccione al menos un día", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Navegar a seleccionar veces al día
            findNavController().navigate(
                R.id.action_selectWeekDaysFragment_to_selectTimesPerDayFragment
            )
        }
    }

    private fun observeViewModel() {
        viewModel.selectedWeekDays.observe(viewLifecycleOwner) { selectedDays ->
            updateDayButtonStates(selectedDays)
            updateSummaryText(selectedDays)
            binding.btnContinue.isEnabled = selectedDays.isNotEmpty()
        }
    }

    private fun updateDayButtonStates(selectedDays: List<Int>) {
        dayViews.forEachIndexed { index, button ->
            val dayIndex = index + 1
            val isSelected = selectedDays.contains(dayIndex)

            if (isSelected) {
                button.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.main_secondary)
                button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            } else {
                button.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.day_unselected)
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
        }
    }

    private fun updateSummaryText(selectedDays: List<Int>) {
        if (selectedDays.isEmpty()) {
            binding.tvSelectedDays.text = "Selecciona uno o más días"
        } else {
            val selectedNames = selectedDays.map { dayNames[it - 1] }
            binding.tvSelectedDays.text = "Cada: ${selectedNames.joinToString(", ")}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}