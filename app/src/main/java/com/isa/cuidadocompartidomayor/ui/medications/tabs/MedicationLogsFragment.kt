package com.isa.cuidadocompartidomayor.ui.medications.tabs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.isa.cuidadocompartidomayor.databinding.FragmentMedicationLogsBinding
import com.isa.cuidadocompartidomayor.ui.medications.adapter.MedicationLogAdapter
import com.isa.cuidadocompartidomayor.ui.medications.viewmodel.MedicationViewModel

class MedicationLogsFragment : Fragment() {

    private var _binding: FragmentMedicationLogsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MedicationViewModel by activityViewModels()
    private lateinit var adapter: MedicationLogAdapter
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val TAG = "MedicationLogsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicationLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        setupRecyclerView()
        observeViewModel()
        loadLogs()
    }

    private fun setupRecyclerView() {
        adapter = MedicationLogAdapter()

        binding.rvLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MedicationLogsFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        viewModel.medicationLogs.observe(viewLifecycleOwner) { logs ->
            val grouped = logs.groupBy { it.getFormattedDate() }
            adapter.updateLogs(grouped)
            updateEmptyState(logs.isEmpty())
            Log.d(TAG, "✅ ${logs.size} logs en historial")
        }

        viewModel.isLoadingLogs.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun loadLogs() {
        val caregiverId = auth.currentUser?.uid
        if (caregiverId != null) {
            viewModel.loadMedicationLogsByCaregiver(caregiverId)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvEmptyState.text = "No hay historial de medicamentos.\n\nCuando tomes medicamentos aparecerán aquí."
        } else {
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
