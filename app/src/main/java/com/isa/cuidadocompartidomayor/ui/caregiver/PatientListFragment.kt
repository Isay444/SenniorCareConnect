package com.isa.cuidadocompartidomayor.ui.caregiver

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.databinding.FragmentPatientListBinding
import com.isa.cuidadocompartidomayor.ui.caregiver.adapter.PatientAdapter
import com.isa.cuidadocompartidomayor.ui.caregiver.viewmodel.ConnectionViewModel

class PatientListFragment : Fragment() {

    private var _binding: FragmentPatientListBinding? = null
    private val binding get() = _binding!!

    private val connectionViewModel: ConnectionViewModel by viewModels()
    private lateinit var patientAdapter: PatientAdapter

    companion object {
        private const val TAG = "PatientListFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        // Cargar lista de pacientes
        connectionViewModel.loadCaregiverRelationships()

        Log.d(TAG, "✅ PatientListFragment inicializado")
    }

    /**
     * Configura el RecyclerView de pacientes
     */
    private fun setupRecyclerView() {
        patientAdapter = PatientAdapter { relationship ->
            // Navegar a detalles del paciente SIN SafeArgs
            val bundle = bundleOf("relationshipId" to relationship.id)
            findNavController().navigate(R.id.action_patientList_to_patientDetail, bundle)
        }

        binding.rvPatients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = patientAdapter
        }

        Log.d(TAG, "RecyclerView configurado")
    }

    /**
     * Configura los observadores del ViewModel
     */
    private fun setupObservers() {
        // Lista de relaciones
        connectionViewModel.caregiverRelationships.observe(viewLifecycleOwner) { relationships ->
            updatePatientList(relationships)
        }

        // Estado de carga
        connectionViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Mensajes de error
        connectionViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                updateEmptyState(it)
                connectionViewModel.clearErrorMessage()
            }
        }

        Log.d(TAG, "Observadores configurados")
    }

    /**
     * Configura los eventos de clic
     */
    private fun setupClickListeners() {
        // Botón de actualizar
        binding.swipeRefreshLayout.setOnRefreshListener {
            connectionViewModel.loadCaregiverRelationships()
        }

        // Floating Action Button para agregar paciente
        binding.fabAddPatient.setOnClickListener {
            findNavController().navigate(R.id.action_patientList_to_connectElderly)
        }

        binding.btnEmptyAction.setOnClickListener {
            findNavController().navigate(R.id.action_patientList_to_connectElderly)
        }

        Log.d(TAG, "Click listeners configurados")
    }

    /**
     * Actualiza la lista de pacientes
     */
    private fun updatePatientList(relationships: List<com.isa.cuidadocompartidomayor.data.model.Relationship>) {
        binding.swipeRefreshLayout.isRefreshing = false

        if (relationships.isEmpty()) {
            showEmptyState()
        } else {
            showPatientList()
            patientAdapter.updatePatients(relationships)

            // Actualizar estadísticas
            updateStatistics(relationships)
        }

        Log.d(TAG, "Lista actualizada: ${relationships.size} pacientes")
    }

    /**
     * Muestra el estado vacío
     */
    private fun showEmptyState() {
        binding.rvPatients.visibility = View.GONE
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.tvEmptyTitle.text = "No tienes Adulto Mayores conectados"
        binding.tvEmptyMessage.text = "Agrega tu primer adulto mayor usando un código de invitación"
    }

    /**
     * Muestra la lista de pacientes
     */
    private fun showPatientList() {
        binding.rvPatients.visibility = View.VISIBLE
        binding.layoutEmpty.visibility = View.GONE
    }

    /**
     * Actualiza el mensaje de estado vacío con error
     */
    private fun updateEmptyState(error: String) {
        binding.rvPatients.visibility = View.GONE
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.tvEmptyTitle.text = "Error cargando personas"
        binding.tvEmptyMessage.text = error
    }

    /**
     * Actualiza las estadísticas mostradas
     */
    private fun updateStatistics(relationships: List<com.isa.cuidadocompartidomayor.data.model.Relationship>) {
        val activeCount = connectionViewModel.getActiveRelationshipsCount()
        val pendingCount = connectionViewModel.getPendingRelationshipsCount()
        val totalCount = relationships.size

        binding.tvStatistics.text = "Total: $totalCount | Activos: $activeCount | Pendientes: $pendingCount"

        Log.d(TAG, "Estadísticas: Total=$totalCount, Activos=$activeCount, Pendientes=$pendingCount")
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos cuando vuelve a ser visible
        connectionViewModel.loadCaregiverRelationships()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "Vista destruida - recursos limpiados")
    }
}