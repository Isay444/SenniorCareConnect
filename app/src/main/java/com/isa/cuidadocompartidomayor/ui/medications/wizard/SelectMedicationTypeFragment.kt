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
import com.isa.cuidadocompartidomayor.data.model.MedicationType
import com.isa.cuidadocompartidomayor.databinding.FragmentSelectMedicationTypeBinding
import com.isa.cuidadocompartidomayor.ui.medications.adapter.MedicationTypeAdapter
import com.isa.cuidadocompartidomayor.ui.medications.viewmodel.AddMedicationViewModel

class SelectMedicationTypeFragment : Fragment() {

    private var _binding: FragmentSelectMedicationTypeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMedicationViewModel by activityViewModels()
    private lateinit var adapter: MedicationTypeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectMedicationTypeBinding.inflate(inflater, container, false)
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
        adapter = MedicationTypeAdapter { selectedType ->
            viewModel.setMedicationType(selectedType)
            binding.btnContinue.isEnabled = true
        }

        binding.rvMedicationTypes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMedicationTypes.adapter = adapter

        // Cargar todos los tipos
        adapter.updateTypes(MedicationType.entries)
    }

    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            findNavController().navigate(
                R.id.action_selectMedicationTypeFragment_to_selectFrequencyFragment
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}