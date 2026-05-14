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
import com.isa.cuidadocompartidomayor.data.model.MoodEntry
import com.isa.cuidadocompartidomayor.data.model.VitalSign
import com.isa.cuidadocompartidomayor.databinding.FragmentMoodEntriesListBinding
import com.isa.cuidadocompartidomayor.ui.diary.adapter.MoodEntryAdapter
import com.isa.cuidadocompartidomayor.ui.diary.viewmodel.DiaryViewModel
import com.isa.cuidadocompartidomayor.ui.diary.wizard.viewmodel.AddMoodEntryViewModel
import com.isa.cuidadocompartidomayor.ui.diary.wizard.viewmodel.AddVitalSignViewModel

class MoodEntriesListFragment : Fragment() {

    private var _binding: FragmentMoodEntriesListBinding? = null
    private val binding get() = _binding!!

    private val diaryViewModel: DiaryViewModel by activityViewModels()
    private lateinit var moodEntryAdapter: MoodEntryAdapter

    companion object {
        private const val TAG = "MoodEntriesListFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoodEntriesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        Log.d(TAG, "✅ MoodEntriesListFragment inicializado")
    }

    private fun setupRecyclerView() {
        moodEntryAdapter = MoodEntryAdapter(
            onEditClick = { moodEntry -> editMoodEntry(moodEntry) },
            onDeleteClick = { moodEntry -> confirmDelete(moodEntry) }
        )

        binding.rvMoodEntries.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = moodEntryAdapter
        }
    }

    private fun observeViewModel() {
        diaryViewModel.moodEntries.observe(viewLifecycleOwner) { moodEntries ->
            if (moodEntries.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvMoodEntries.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvMoodEntries.visibility = View.VISIBLE
                moodEntryAdapter.submitList(moodEntries)
            }
            Log.d(TAG, "✅ ${moodEntries.size} estados de ánimo mostrados")
        }

        diaryViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun editMoodEntry(moodEntry: MoodEntry) {
        try {
            // Cargar datos en el ViewModel y navegar
            val viewModel: AddMoodEntryViewModel by activityViewModels()
            viewModel.loadMoodEntryForEdit(moodEntry.id)

            viewModel.setEditMode(true, moodEntry.id)

            findNavController().navigate(R.id.nav_graph_add_mood_entry)
            Log.d(TAG, "Navegando a edición: ${moodEntry.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error navegando a edición: ${e.message}")
            Toast.makeText( requireContext(), "Error abriendo editor", Toast.LENGTH_SHORT ).show()
        }
    }

    private fun confirmDelete(moodEntry: MoodEntry) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar registro")
            .setMessage("¿Estás seguro de eliminar este registro de estado de ánimo?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteMoodEntry(moodEntry)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteMoodEntry(moodEntry: MoodEntry) {
        diaryViewModel.deleteMoodEntry(
            moodEntryId = moodEntry.id,
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