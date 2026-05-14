package com.isa.cuidadocompartidomayor.ui.agenda.tabs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.isa.cuidadocompartidomayor.data.model.AgendaEvent
import com.isa.cuidadocompartidomayor.databinding.FragmentNormalTasksBinding
import com.isa.cuidadocompartidomayor.ui.agenda.adapter.NormalTaskAdapter
import com.isa.cuidadocompartidomayor.ui.agenda.dialog.AddEditEventBottomSheet
import com.isa.cuidadocompartidomayor.ui.agenda.dialog.EventDetailsBottomSheet
import com.isa.cuidadocompartidomayor.ui.agenda.viewmodel.AgendaViewModel

class NormalTasksFragment : Fragment() {

    private var _binding: FragmentNormalTasksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AgendaViewModel by activityViewModels()
    private lateinit var adapter: NormalTaskAdapter

    companion object {
        private const val TAG = "NormalTasksFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNormalTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        Log.d(TAG, "✅ NormalTasksFragment inicializado")
    }

    private fun setupRecyclerView() {
        adapter = NormalTaskAdapter(
            onCheckboxClick = { task, isChecked ->
                handleToggleComplete(task, isChecked)
            },
            onEditClick = { task ->
                handleEdit(task)
            },
            onDeleteClick = { task ->
                showDeleteConfirmationDialog(task)
            },
            onItemClick = { task ->
                handleItemClick(task)
            }
        )

        binding.rvNormalTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NormalTasksFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        // Observar tareas normales
        viewModel.normalTasks.observe(viewLifecycleOwner) { tasks ->
            adapter.submitList(tasks)
            updateEmptyState(tasks.isEmpty())
            Log.d(TAG, "✅ ${tasks.size} tareas mostradas")

            // ✅ AGREGAR: Ocultar progressBar cuando hay datos
            binding.progressBar.visibility = View.GONE
        }

        // Observar estado de carga
        /*
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
         */

        // Observar errores
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun handleToggleComplete(task: AgendaEvent.NormalTask, isChecked: Boolean) {
        viewModel.toggleTaskCompletion(
            taskId = task.id,
            isCompleted = isChecked,
            onSuccess = {
                val message = if (isChecked) "Tarea completada ✓" else "Tarea marcada como pendiente"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.loadAllEvents()
            },
            onError = { error ->
                Toast.makeText(
                    requireContext(),
                    "Error: $error",
                    Toast.LENGTH_SHORT
                ).show()
                // Revertir el cambio en el adapter si falló
                adapter.notifyDataSetChanged()
            }
        )
    }

    private fun handleEdit(task: AgendaEvent.NormalTask) {
        // Verificar si la tarea está completada
        if (task.isCompleted) {
            Toast.makeText(
                requireContext(),
                "No puedes editar tareas completadas",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Verificar si la tarea ya pasó
        if (task.isPast()) {
            Toast.makeText(
                requireContext(),
                "No puedes editar tareas pasadas",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val editSheet = AddEditEventBottomSheet.newInstanceForEdit(task)
        editSheet.show(childFragmentManager, "EditEventBottomSheet")
    }

    private fun handleItemClick(appointment: AgendaEvent.NormalTask) {
        val detailsSheet = EventDetailsBottomSheet.newInstance(appointment)
        detailsSheet.show(childFragmentManager, "EventDetailsBottomSheet")
    }
    private fun showDeleteConfirmationDialog(task: AgendaEvent.NormalTask) {
        val message = if (task.isCompleted) {
            "¿Estás seguro de que deseas eliminar esta tarea completada?\n\n${task.title}"
        } else {
            "¿Estás seguro de que deseas eliminar esta tarea?\n\n${task.title}"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar tarea")
            .setMessage(message)
            .setPositiveButton("Eliminar") { _, _ ->
                deleteTask(task)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteTask(task: AgendaEvent.NormalTask) {
        viewModel.deleteEvent(
            eventId = task.id,
            onSuccess = {
                Toast.makeText(
                    requireContext(),
                    "Tarea eliminada correctamente",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.loadAllEvents()
            },
            onError = { error ->
                Toast.makeText(
                    requireContext(),
                    "Error al eliminar: $error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.rvNormalTasks.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.rvNormalTasks.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
