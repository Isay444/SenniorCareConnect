package com.isa.cuidadocompartidomayor.ui.medications.tabs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.Medication
import com.isa.cuidadocompartidomayor.databinding.FragmentMedicationListBinding
import com.isa.cuidadocompartidomayor.ui.medications.adapter.MedicationListAdapter
import com.isa.cuidadocompartidomayor.ui.medications.dialog.MedicationDetailBottomSheet
import com.isa.cuidadocompartidomayor.ui.medications.viewmodel.MedicationViewModel

class MedicationListFragment : Fragment() {

    private var _binding: FragmentMedicationListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MedicationViewModel by activityViewModels()
    private lateinit var adapter: MedicationListAdapter
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val TAG = "MedicationListFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicationListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        setupRecyclerView()
        observeViewModel()
        loadMedications()
    }

    private fun setupRecyclerView() {
        adapter = MedicationListAdapter(
            onEditClick = { medication ->
                navigateToEditMedication(medication)
            },
            onDeleteClick = { medication ->
                showDeleteConfirmationDialog(medication)
            }
        )

        adapter.setOnItemClickListener { medication ->
            val photoMap = viewModel.medicationPhotoMap.value ?: emptyMap()
            val elderlyPhotoUrl = photoMap[medication.elderlyId] ?: ""
            val caregiverPhotoUrl = photoMap[medication.caregiverId] ?: ""

            MedicationDetailBottomSheet.newInstance(
                medication,
                elderlyPhotoUrl,
                caregiverPhotoUrl
            ).show(childFragmentManager, "MedicationDetail")
        }

        binding.rvMedications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MedicationListFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        viewModel.activeMedications.observe(viewLifecycleOwner) { medications ->
            adapter.updateMedications(medications)
            updateEmptyState(medications.isEmpty())
            Log.d(TAG, "✅ ${medications.size} medicamentos activos")
        }

        viewModel.isLoadingActive.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.activeError.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearActiveError()
            }
        }
    }

    private fun loadMedications() {
        val caregiverId = auth.currentUser?.uid
        if (caregiverId != null) {
            viewModel.loadActiveMedicationsByCaregiver(caregiverId)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvEmptyState.text = "No hay medicamentos registrados.\nPresiona + para agregar uno."
        } else {
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    private fun navigateToEditMedication(medication: Medication) {
        try {
            //  Navega desde el parent fragment (MedicationsFragment)
            val action = com.isa.cuidadocompartidomayor.ui.medications.MedicationsFragmentDirections
                .actionMedicationsFragmentToEditMedicationDialog(medication.id)

            // Usar el NavController del parent fragment
            parentFragment?.findNavController()?.navigate(action)

            Log.d(TAG, "Navegando a editar medicamento: ${medication.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error navegando a edición: ${e.message}")
            Toast.makeText(requireContext(), "Error al abrir edición", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(medication: Medication) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar medicamento")
            .setMessage("¿Estás seguro de que deseas eliminar ${medication.name}?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteMedication(medication.id, medication.elderlyId)
                Toast.makeText(requireContext(), "Medicamento eliminado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
