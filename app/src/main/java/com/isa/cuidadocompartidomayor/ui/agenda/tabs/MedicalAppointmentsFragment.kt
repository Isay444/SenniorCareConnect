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
import com.isa.cuidadocompartidomayor.databinding.FragmentMedicalAppointmentsBinding
import com.isa.cuidadocompartidomayor.ui.agenda.adapter.MedicalAppointmentAdapter
import com.isa.cuidadocompartidomayor.ui.agenda.dialog.AddEditEventBottomSheet
import com.isa.cuidadocompartidomayor.ui.agenda.dialog.EventDetailsBottomSheet
import com.isa.cuidadocompartidomayor.ui.agenda.viewmodel.AgendaViewModel

class MedicalAppointmentsFragment : Fragment() {

    private var _binding: FragmentMedicalAppointmentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AgendaViewModel by activityViewModels()
    private lateinit var adapter: MedicalAppointmentAdapter

    companion object {
        private const val TAG = "MedicalAppointmentsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicalAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        Log.d(TAG, "✅ MedicalAppointmentsFragment inicializado")
    }

    private fun setupRecyclerView() {
        adapter = MedicalAppointmentAdapter(
            onEditClick = { appointment ->
                handleEdit(appointment)
            },
            onDeleteClick = { appointment ->
                showDeleteConfirmationDialog(appointment)
            },
            onItemClick = { appointment ->
                handleItemClick(appointment)
            }
        )

        binding.rvMedicalAppointments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MedicalAppointmentsFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        // Observar citas médicas
        viewModel.medicalAppointments.observe(viewLifecycleOwner) { appointments ->
            adapter.submitList(appointments)
            updateEmptyState(appointments.isEmpty())
            Log.d(TAG, "✅ ${appointments.size} citas médicas mostradas")

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

    private fun handleEdit(appointment: AgendaEvent.MedicalAppointment) {
        // Verificar si la cita ya pasó
        if (appointment.isPast()) {
            Toast.makeText(
                requireContext(),
                "No puedes editar citas pasadas",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val editSheet = AddEditEventBottomSheet.newInstanceForEdit(appointment)
        editSheet.show(childFragmentManager, "EditEventBottomSheet")
    }

    private fun handleItemClick(appointment: AgendaEvent.MedicalAppointment) {
        val detailsSheet = EventDetailsBottomSheet.newInstance(appointment)
        detailsSheet.show(childFragmentManager, "EventDetailsBottomSheet")
    }
    private fun showDeleteConfirmationDialog(appointment: AgendaEvent.MedicalAppointment) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar cita médica")
            .setMessage("¿Estás seguro de que deseas eliminar esta cita?\n\n${appointment.title}")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteAppointment(appointment)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteAppointment(appointment: AgendaEvent.MedicalAppointment) {
        viewModel.deleteEvent(
            eventId = appointment.id,
            onSuccess = {
                Toast.makeText(
                    requireContext(),
                    "Cita eliminada correctamente",
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
            binding.rvMedicalAppointments.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.rvMedicalAppointments.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
