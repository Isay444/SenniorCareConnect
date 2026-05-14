package com.isa.cuidadocompartidomayor.ui.diary.tabs

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
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.VitalSign
import com.isa.cuidadocompartidomayor.databinding.FragmentVitalSignsListBinding
import com.isa.cuidadocompartidomayor.ui.diary.adapter.VitalSignAdapter
import com.isa.cuidadocompartidomayor.ui.diary.viewmodel.DiaryViewModel
import com.isa.cuidadocompartidomayor.ui.diary.wizard.viewmodel.AddVitalSignViewModel

class VitalSignsListFragment : Fragment() {

    private var _binding: FragmentVitalSignsListBinding? = null
    private val binding get() = _binding!!

    private val diaryViewModel: DiaryViewModel by activityViewModels()
    private lateinit var vitalSignAdapter: VitalSignAdapter

    companion object {
        private const val TAG = "VitalSignsListFragment"
    }

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View {
        _binding = FragmentVitalSignsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        Log.d(TAG, "✅ VitalSignsListFragment inicializado")
    }

    private fun setupRecyclerView() {
        vitalSignAdapter = VitalSignAdapter(
            onEditClick = { vitalSign -> editVitalSign(vitalSign) },
            onDeleteClick = { vitalSign -> confirmDelete(vitalSign) }
        )

        binding.rvVitalSigns.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = vitalSignAdapter
        }
    }

    private fun observeViewModel() {
        diaryViewModel.vitalSigns.observe(viewLifecycleOwner) { vitalSigns ->
            if (vitalSigns.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvVitalSigns.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvVitalSigns.visibility = View.VISIBLE
                vitalSignAdapter.submitList(vitalSigns)
            }
            Log.d(TAG, "✅ ${vitalSigns.size} signos vitales mostrados")
        }

        diaryViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun editVitalSign(vitalSign: VitalSign) {
        try {
            // Cargar datos ANTES de navegar
            val viewModel: AddVitalSignViewModel by activityViewModels()
            viewModel.loadVitalSignForEdit(vitalSign.id)

            // Marcar explícitamente que estamos en modo edición
            viewModel.setEditMode(true, vitalSign.id)

            // Navegar
            findNavController().navigate(R.id.nav_graph_add_vital_sign)

            Log.d(TAG, "✅ Navegando a edición: ${vitalSign.id}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            Toast.makeText(requireContext(), "Error abriendo editor", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(vitalSign: VitalSign) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar registro")
            .setMessage("¿Estás seguro de eliminar este registro de signos vitales?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteVitalSign(vitalSign)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteVitalSign(vitalSign: VitalSign) {
        diaryViewModel.deleteVitalSign(
            vitalSignId = vitalSign.id,
            onSuccess = {
                Toast.makeText(
                    requireContext(),
                    "✅ Registro eliminado",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onError = { error ->
                Toast.makeText(
                    requireContext(),
                    "❌ Error: $error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}