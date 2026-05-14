package com.isa.cuidadocompartidomayor.ui.medications.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.FrequencyType
import com.isa.cuidadocompartidomayor.databinding.FragmentSelectFrequencyBinding
import com.isa.cuidadocompartidomayor.ui.medications.adapter.FrequencyTypeAdapter
import com.isa.cuidadocompartidomayor.ui.medications.viewmodel.AddMedicationViewModel

class SelectFrequencyFragment : Fragment() {

    private var _binding: FragmentSelectFrequencyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMedicationViewModel by activityViewModels()
    private lateinit var adapter: FrequencyTypeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectFrequencyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupContinueButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = FrequencyTypeAdapter { selectedFrequency ->
            viewModel.setFrequencyType(selectedFrequency)
            binding.btnContinue.isEnabled = true
        }

        binding.rvFrequencyTypes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFrequencyTypes.adapter = adapter

        // Cargar todas las frecuencias
        adapter.updateTypes(FrequencyType.values().toList())
    }

    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            val frequency = viewModel.frequencyType.value ?: FrequencyType.ONCE_DAILY

            // Navegar según el tipo de frecuencia seleccionado
            val action = when (frequency) {
                FrequencyType.ONCE_DAILY -> R.id.action_selectFrequencyFragment_to_scheduleSingleDoseFragment
                FrequencyType.MULTIPLE_DAILY -> R.id.action_selectFrequencyFragment_to_selectTimesPerDayFragment
                FrequencyType.SPECIFIC_DAYS -> R.id.action_selectFrequencyFragment_to_selectWeekDaysFragment
                FrequencyType.EVERY_X_DAYS -> R.id.action_selectFrequencyFragment_to_selectIntervalDaysFragment
                FrequencyType.EVERY_X_WEEKS -> R.id.action_selectFrequencyFragment_to_selectIntervalWeeksFragment
                FrequencyType.EVERY_X_MONTHS -> R.id.action_selectFrequencyFragment_to_selectIntervalMonthsFragment
            }

            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}