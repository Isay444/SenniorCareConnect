package com.isa.cuidadocompartidomayor.ui.medications

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.databinding.FragmentMedicationsBinding
import com.isa.cuidadocompartidomayor.ui.medications.adapter.MedicationsPagerAdapter

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.isa.cuidadocompartidomayor.utils.MedicationLogGeneratorWorker

class MedicationsFragment : Fragment() {

    private var _binding: FragmentMedicationsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "MedicationsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()
//        binding.fabAddMedication.setOnLongClickListener {
//            val testWorkRequest = OneTimeWorkRequestBuilder<MedicationLogGeneratorWorker>()
//                .addTag("manual_test")
//                .build()
//            WorkManager.getInstance(requireContext()).enqueue(testWorkRequest)
//            Toast.makeText(requireContext(), "Worker disparado!", Toast.LENGTH_SHORT).show()
//            true
//        }
        setupFab()
        Log.d(TAG, "✅ MedicationsFragment inicializado con ViewPager2")
    }

    private fun setupViewPager() {
        val adapter = MedicationsPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Medicamentos"
                1 -> "Historial"
                else -> ""
            }
        }.attach()
    }

    private fun setupFab() {
        binding.fabAddMedication.setOnClickListener {
            navigateToMedicationWizard()
        }
    }

    private fun navigateToMedicationWizard() {
        try {
            //  Navega al nested graph (nav_graph_medications)
            findNavController().navigate(
                R.id.action_medicationsFragment_to_medicationWizard
            )
            Log.d(TAG, "Navegando al wizard de medicamentos")
        } catch (e: Exception) {
            Log.e(TAG, "Error navegando al wizard: ${e.message}")
            Toast.makeText(
                requireContext(),
                "Error al abrir wizard de medicamentos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
