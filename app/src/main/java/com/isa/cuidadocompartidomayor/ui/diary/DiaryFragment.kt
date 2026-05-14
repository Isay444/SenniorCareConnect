package com.isa.cuidadocompartidomayor.ui.diary

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.isa.cuidadocompartidomayor.R
import com.google.android.material.snackbar.Snackbar
import com.isa.cuidadocompartidomayor.data.model.ElderlyItem
import com.isa.cuidadocompartidomayor.databinding.FragmentDiaryBinding
import com.isa.cuidadocompartidomayor.ui.agenda.adapter.ElderlySpinnerAdapter
import com.isa.cuidadocompartidomayor.ui.diary.adapter.DiaryPagerAdapter
import com.isa.cuidadocompartidomayor.ui.diary.viewmodel.DiaryViewModel

class DiaryFragment : Fragment() {

    private var _binding: FragmentDiaryBinding? = null
    private val binding get() = _binding!!

    private val diaryViewModel: DiaryViewModel by activityViewModels()

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var pagerAdapter: DiaryPagerAdapter

    // Lista de adultos mayores
    private var elderlyRelationships = mutableListOf<ElderlyItem>()
    private var selectedElderlyId: String? = null

    companion object {
        private const val TAG = "DiaryFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupViewPager()
        setupSpinner()
        setupFAB()
        observeViewModel()

        Log.d(TAG, "✅ DiaryFragment inicializado")
    }

    // ========================================
    // Configuración de ViewPager2 y Tabs
    // ========================================

    private fun setupViewPager() {
        pagerAdapter = DiaryPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        // Conectar TabLayout con ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = pagerAdapter.getPageTitle(position)
        }.attach()

        Log.d(TAG, "✅ ViewPager y TabLayout configurados")
    }

    // ========================================
    // Configuración del Spinner
    // ========================================

    private fun setupSpinner() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.w(TAG, "Usuario no autenticado")
            return
        }

        // Cargar lista desde el ViewModel
        diaryViewModel.loadElderlyList(currentUserId)
    }

    private fun setupSpinnerAdapter() {
        val adapter = ElderlySpinnerAdapter(
            requireContext(),
            elderlyRelationships
        )
        binding.spinnerPatientFilter.adapter = adapter

        // Listener para cambios de selección
        binding.spinnerPatientFilter.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selected = elderlyRelationships[position]
                    selectedElderlyId = selected.id

                    Log.d(TAG, "Filtro seleccionado: ${selected.name}")
                    loadDataForFilter(selected)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // No hacer nada
                }
            }
    }

    private fun loadDataForFilter(elderly: ElderlyItem) {
        // Cargar datos según el filtro seleccionado
        diaryViewModel.loadVitalSigns(elderly.id)
        diaryViewModel.loadMoodEntries(elderly.id)

        Log.d(TAG, "📥 Cargando datos para: ${elderly.id}")
    }

    private fun showEmptyState() {
        Toast.makeText(
            requireContext(),
            "No tienes adultos mayores conectados",
            Toast.LENGTH_LONG
        ).show()
    }

    // ========================================
    // Configuración del FAB
    // ========================================

    private fun setupFAB() {
        binding.fabAddEntry.setOnClickListener {
            // Mostrar opciones según la pestaña actual
            val currentTab = binding.viewPager.currentItem

            when (currentTab) {
                0 -> navigateToAddVitalSigns()
                1 -> navigateToAddMoodEntry()
                else -> return@setOnClickListener
            }
        }
    }

    private fun navigateToAddVitalSigns() {
        try {
            findNavController().navigate(R.id.action_diary_to_vitalSignWizard)
            Log.d(TAG, "Navegando a wizard de signos vitales")
        } catch (e: Exception) {
            Log.e(TAG, "Error al navegar: ${e.message}")
            Toast.makeText(
                requireContext(),
                "Error al abrir wizard",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun navigateToAddMoodEntry() {
        try {
            findNavController().navigate(R.id.action_diary_to_moodWizard)
            Log.d(TAG, "Navegando a wizard de estados de ánimo")
        } catch (e: Exception) {
            Log.e(TAG, "Error al navegar: ${e.message}")
            Toast.makeText( requireContext(), "Error al abrir wizard", Toast.LENGTH_SHORT ).show()
        }
    }

    // ========================================
    // Observar ViewModel
    // ========================================

    private fun observeViewModel() {
        // Observar lista de adultos mayores
        diaryViewModel.elderlyList.observe(viewLifecycleOwner) { list ->
            if (list.isNotEmpty()) {
                elderlyRelationships.clear()
                elderlyRelationships.addAll(list)
                setupSpinnerAdapter()

                // Selección inicial
                if (selectedElderlyId == null) {
                    val first = list[0]
                    selectedElderlyId = first.id
                    loadDataForFilter(first)
                }
            } else {
                showEmptyState()
            }
        }

        diaryViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                diaryViewModel.clearError()
            }
        }
    }

    // ========================================
    // Lifecycle
    // ========================================

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "✅ DiaryFragment destruido")
    }
}
