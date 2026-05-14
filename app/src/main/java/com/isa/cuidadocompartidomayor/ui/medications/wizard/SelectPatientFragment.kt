package com.isa.cuidadocompartidomayor.ui.medications.wizard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.ElderlyItem
import com.isa.cuidadocompartidomayor.databinding.FragmentSelectPatientBinding
import com.isa.cuidadocompartidomayor.ui.caregiver.viewmodel.ConnectionViewModel
import com.isa.cuidadocompartidomayor.ui.medications.adapter.PatientSelectionAdapter
import com.isa.cuidadocompartidomayor.ui.medications.viewmodel.AddMedicationViewModel

class SelectPatientFragment : Fragment() {

    private var _binding: FragmentSelectPatientBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMedicationViewModel by activityViewModels()
    private val connectionViewModel: ConnectionViewModel by viewModels()

    private lateinit var patientAdapter: PatientSelectionAdapter
    private var selectedPatient: ElderlyItem? = null

    companion object {
        private const val TAG = "SelectPatientFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectPatientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupContinueButton()
        observeViewModel()
        
        // Cargar lista de pacientes
        connectionViewModel.loadCaregiverRelationships()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        patientAdapter = PatientSelectionAdapter { patient ->
            selectedPatient = patient
            binding.btnContinue.isEnabled = true
            Log.d(TAG, "Paciente seleccionado: ${patient.name}")
        }

        binding.rvPatients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = patientAdapter
        }
    }

    private fun observeViewModel() {
        // Observar lista de pacientes con fotos
        connectionViewModel.elderlyPatients.observe(viewLifecycleOwner) { patients ->
            if (patients.isEmpty()) {
                showEmptyState()
            } else {
                showPatients(patients)
            }
        }

        // Observar estado de carga
        connectionViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
        }

        // Observar errores
        connectionViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { showError(it) }
        }
    }

    /**
     * Muestra la lista de pacientes
     */
    private fun showPatients(patients: List<ElderlyItem>) {
        binding.apply {
            rvPatients.visibility = View.VISIBLE
            tvPatientInfo.visibility = View.GONE
            layoutEmptyState.visibility = View.GONE
        }

        patientAdapter.updatePatients(patients)
    }

    /**
     * Muestra estado vacío cuando no hay pacientes
     */
    private fun showEmptyState() {
        binding.apply {
            rvPatients.visibility = View.GONE
            tvPatientInfo.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        }
    }

    /**
     * Muestra/oculta el loading
     */
    private fun showLoading(isLoading: Boolean) {
        binding.apply {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnContinue.isEnabled = !isLoading && selectedPatient != null
        }
    }

    /**
     * Muestra un mensaje de error
     */
    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        binding.apply {
            tvPatientInfo.visibility = View.VISIBLE
            tvPatientInfo.text = message
            rvPatients.visibility = View.GONE
        }
    }

    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            val patient = patientAdapter.getSelectedPatient()

            if (patient == null) {
                Toast.makeText(
                    requireContext(),
                    "Debe seleccionar un paciente",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Guardar paciente seleccionado en el ViewModel
            viewModel.setSelectedElderly(patient.id, patient.name)

            // Navegar a la siguiente pantalla
            findNavController().navigate(
                R.id.action_selectPatientFragment_to_medicationNameFragment
            )

            Log.d(TAG, "Navegando con paciente: ${patient.name}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
