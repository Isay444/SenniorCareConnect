package com.isa.cuidadocompartidomayor.ui.diary.wizard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.ElderlyItem
import com.isa.cuidadocompartidomayor.databinding.FragmentSelectPatientBinding
import com.isa.cuidadocompartidomayor.ui.diary.wizard.viewmodel.AddVitalSignViewModel
import com.isa.cuidadocompartidomayor.ui.medications.adapter.PatientSelectionAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SelectPatientForVitalSignsFragment : Fragment() {

    private var _binding: FragmentSelectPatientBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddVitalSignViewModel by activityViewModels()

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var patientAdapter: PatientSelectionAdapter

    companion object {
        private const val TAG = "SelectPatientVitalSign"
    }

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View {
        _binding = FragmentSelectPatientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        //  Verificar modo edición desde el ViewModel
        if (viewModel.isEditMode.value == true) {
            Log.d(TAG, "🔄 Modo edición detectado: ${viewModel.editingVitalSignId.value}")

            // Verificar si los datos YA están cargados
            val currentElderlyName = viewModel.elderlyName.value
            val currentElderlyId = viewModel.elderlyId.value

            if (currentElderlyName != null && !currentElderlyName.isBlank() && currentElderlyId != null) {
                // Los datos ya están, navegar inmediatamente
                Log.d(TAG, "✅ Datos ya disponibles, navegando inmediatamente")
                navigateToSelectVitalSignTypes(currentElderlyId, currentElderlyName)
                return
            }

            // Los datos aún no están, observar
            Log.d(TAG, "⏳ Esperando carga de datos...")
            viewModel.elderlyName.observe(viewLifecycleOwner) { elderlyName ->
                if (!elderlyName.isNullOrBlank()) {
                    val elderlyId = viewModel.elderlyId.value
                    if (elderlyId != null && !elderlyId.isBlank()) {
                        Log.d(TAG, "✅ Datos cargados, navegando al paso 2")
                        // IMPORTANTE: Remover observer para evitar navegación múltiple
                        viewModel.elderlyName.removeObservers(viewLifecycleOwner)
                        navigateToSelectVitalSignTypes(elderlyId, elderlyName)
                    }
                }
            }
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupContinueButton()
        loadPatients()

        Log.d(TAG, "✅ Fragment inicializado(modo creación)")
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        // Usar el mismo adapter que el wizard de medicamentos
        patientAdapter = PatientSelectionAdapter { patient ->
            // Habilitar botón continuar cuando se selecciona un paciente
            binding.btnContinue.isEnabled = true
            Log.d(TAG, "Paciente seleccionado: ${patient.name}")
        }

        binding.rvPatients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = patientAdapter
        }
    }

    private fun setupContinueButton() {
        binding.btnContinue.isEnabled = false
        binding.btnContinue.setOnClickListener {
            val selectedPatient = patientAdapter.getSelectedPatient()

            if (selectedPatient != null) {
                navigateToSelectVitalSignTypes(
                    elderlyId = selectedPatient.id,
                    elderlyName = selectedPatient.name
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    "Selecciona un paciente",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadPatients() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.layoutEmptyState.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Paso 1: Obtener relaciones activas
                val snapshot = firestore.collection("relationships")
                    .whereEqualTo("caregiverId", currentUserId)
                    .whereEqualTo("status", "active")
                    .get()
                    .await()

                if (snapshot.isEmpty) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.rvPatients.visibility = View.GONE
                    return@launch
                }

                // Paso 2: Obtener elderlyIds y hacer batch fetch a la colección "users" para las fotos
                val elderlyIds = snapshot.documents.mapNotNull { it.getString("elderlyId") }
                
                val photoMap = if (elderlyIds.isNotEmpty()) {
                    val usersSnapshot = firestore.collection("users")
                        .whereIn(FieldPath.documentId(), elderlyIds)
                        .get()
                        .await()
                    
                    usersSnapshot.documents.associate { doc ->
                        doc.id to (doc.getString("profileImageUrl") ?: "")
                    }
                } else emptyMap()

                // Paso 3: Mapear directamente a ElderlyItem usando la foto del photoMap
                val elderlyItems = snapshot.documents.mapNotNull { doc ->
                    val elderlyId = doc.getString("elderlyId") ?: return@mapNotNull null
                    ElderlyItem(
                        id = elderlyId,
                        name = doc.getString("elderlyName") ?: "Adulto Mayor",
                        profileImageUrl = photoMap[elderlyId] ?: ""
                    )
                }

                binding.layoutEmptyState.visibility = View.GONE
                binding.rvPatients.visibility = View.VISIBLE
                patientAdapter.updatePatients(elderlyItems)

                Log.d(TAG, "✅ ${elderlyItems.size} pacientes cargados con fotos")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando pacientes: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Error cargando pacientes",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun navigateToSelectVitalSignTypes(elderlyId: String, elderlyName: String) {
        val vitalSignId = viewModel.editingVitalSignId.value

        Log.d(TAG, "🔄 Navegando a SelectVitalSignTypes con vitalSignId = $vitalSignId")

        val action = SelectPatientForVitalSignsFragmentDirections
            .actionSelectPatientToSelectVitalSignTypes(
                elderlyId = elderlyId,
                elderlyName = elderlyName,
                vitalSignId = vitalSignId //  Pasar el ID
            )

        findNavController().navigate(action)
        Log.d(TAG, "Navegando a SelectVitalSignTypesFragment" )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
