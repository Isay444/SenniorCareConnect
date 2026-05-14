package com.isa.cuidadocompartidomayor.ui.agenda.dialog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.bumptech.glide.Glide
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.AgendaEvent
import com.isa.cuidadocompartidomayor.data.model.EventType
import com.isa.cuidadocompartidomayor.databinding.BottomSheetEventDetailsBinding
import com.isa.cuidadocompartidomayor.ui.agenda.viewmodel.AgendaViewModel
import java.text.SimpleDateFormat
import java.util.*

class EventDetailsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEventDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AgendaViewModel by activityViewModels()
    private var event: AgendaEvent? = null
    private var profileImageUrl: String? = null

    companion object {
        private const val TAG = "EventDetailsBottomSheet"

        fun newInstance(event: AgendaEvent, profileImageUrl: String? = null): EventDetailsBottomSheet {
            return EventDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("profileImageUrl", profileImageUrl)
                    // Convertir el evento a un formato serializable
                    putString("eventId", event.id)
                    putString("eventType", event.type.name)
                    putString("title", event.title)
                    putLong("date", event.date)
                    putString("elderlyId", event.elderlyId)
                    putString("elderlyName", event.elderlyName)
                    putString("createdByName", event.createdByName)
                    putString("notes", event.notes)
                    putLong("createdAt", event.createdAt)

                    when (event) {
                        is AgendaEvent.MedicalAppointment -> {
                            putString("location", event.location)
                        }
                        is AgendaEvent.NormalTask -> {
                            putString("assignedTo", event.assignedTo)
                            putString("assignedToName", event.assignedToName)
                            putBoolean("isCompleted", event.isCompleted)
                            event.completedAt?.let { putLong("completedAt", it) }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEventDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadEventFromArguments()
        displayEventDetails()
        setupButtons()

        Log.d(TAG, "✅ EventDetailsBottomSheet inicializado")
    }

    private fun loadEventFromArguments() {
        arguments?.let { args ->
            profileImageUrl = args.getString("profileImageUrl")
            val eventId = args.getString("eventId") ?: return
            val eventTypeStr = args.getString("eventType") ?: return
            val eventType = EventType.valueOf(eventTypeStr)

            when (eventType) {
                EventType.MEDICAL_APPOINTMENT -> {
                    event = AgendaEvent.MedicalAppointment(
                        id = eventId,
                        title = args.getString("title") ?: "",
                        date = args.getLong("date"),
                        elderlyId = args.getString("elderlyId") ?: "",
                        elderlyName = args.getString("elderlyName") ?: "",
                        createdBy = "",
                        createdByName = args.getString("createdByName") ?: "",
                        notes = args.getString("notes") ?: "",
                        location = args.getString("location") ?: "",
                        createdAt = args.getLong("createdAt"),
                        updatedAt = 0L
                    )
                }
                EventType.NORMAL_TASK -> {
                    event = AgendaEvent.NormalTask(
                        id = eventId,
                        title = args.getString("title") ?: "",
                        date = args.getLong("date"),
                        elderlyId = args.getString("elderlyId") ?: "",
                        elderlyName = args.getString("elderlyName") ?: "",
                        createdBy = "",
                        createdByName = args.getString("createdByName") ?: "",
                        notes = args.getString("notes") ?: "",
                        assignedTo = args.getString("assignedTo"),
                        assignedToName = args.getString("assignedToName"),
                        isCompleted = args.getBoolean("isCompleted", false),
                        completedAt = if (args.containsKey("completedAt")) args.getLong("completedAt") else null,
                        createdAt = args.getLong("createdAt"),
                        updatedAt = 0L
                    )
                }
            }
        }
    }

    private fun displayEventDetails() {
        val currentEvent = event ?: return

        binding.apply {
            // Título
            tvTitle.text = currentEvent.title

            // Chip de tipo de evento
            when (currentEvent.type) {
                EventType.MEDICAL_APPOINTMENT -> {
                    chipEventType.text = "CITA MÉDICA"
                    chipEventType.setChipBackgroundColorResource(R.color.medical_appointment_stroke)
                    chipEventType.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                }
                EventType.NORMAL_TASK -> {
                    chipEventType.text = "TAREA"
                    chipEventType.setChipBackgroundColorResource(R.color.normal_task_stroke)
                    chipEventType.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                }
            }

            // Fecha y hora
            tvDate.text = currentEvent.getFormattedDate()
            tvTime.text = currentEvent.getFormattedTime()

            // Adulto mayor
            tvElderlyName.text = currentEvent.elderlyName

            // Foto del adulto mayor
            Glide.with(requireContext())
                .load(profileImageUrl)
                .placeholder(R.drawable.ic_avatar)
                .error(R.drawable.ic_avatar)
                .circleCrop()
                .into(ivElderlyAvatar)

            // Registrado por
            val createdDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(Date(currentEvent.createdAt))
            tvCreatedBy.text = "${currentEvent.createdByName} • $createdDate"

            // Notas
            if (currentEvent.notes.isNotBlank()) {
                layoutNotes.visibility = View.VISIBLE
                tvNotes.text = currentEvent.notes
            } else {
                layoutNotes.visibility = View.GONE
            }

            // Información específica según el tipo
            when (currentEvent) {
                is AgendaEvent.MedicalAppointment -> {
                    // Mostrar ubicación
                    layoutLocation.visibility = View.VISIBLE
                    tvLocation.text = currentEvent.location

                    // Ocultar campos de tarea
                    layoutAssignedTo.visibility = View.GONE
                    layoutStatus.visibility = View.GONE
                }
                is AgendaEvent.NormalTask -> {
                    // Ocultar ubicación
                    layoutLocation.visibility = View.GONE

                    // Mostrar asignado a
                    if (currentEvent.assignedToName != null) {
                        layoutAssignedTo.visibility = View.VISIBLE
                        tvAssignedTo.text = currentEvent.assignedToName
                    } else {
                        layoutAssignedTo.visibility = View.GONE
                    }

                    // Mostrar estado
                    layoutStatus.visibility = View.VISIBLE
                    if (currentEvent.isCompleted) {
                        chipStatus.text = "COMPLETADA"
                        chipStatus.setChipBackgroundColorResource(R.color.event_completed)
                        chipStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    } else if (currentEvent.isPast()) {
                        chipStatus.text = "VENCIDA"
                        chipStatus.setChipBackgroundColorResource(R.color.event_overdue)
                        chipStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    } else {
                        chipStatus.text = "PENDIENTE"
                        chipStatus.setChipBackgroundColorResource(R.color.event_pending)
                        chipStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    }
                }
            }

            // Deshabilitar edición según el estado
            btnEdit.isEnabled = canEditEvent(currentEvent)
        }
    }

    private fun canEditEvent(event: AgendaEvent): Boolean {
        return when (event) {
            is AgendaEvent.MedicalAppointment -> !event.isPast()
            is AgendaEvent.NormalTask -> !event.isCompleted && !event.isPast()
        }
    }

    private fun setupButtons() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnEdit.setOnClickListener {
            event?.let { currentEvent ->
                // Abrir BottomSheet de edición
                val editSheet = AddEditEventBottomSheet.newInstanceForEdit(currentEvent)
                editSheet.show(parentFragmentManager, "EditEventBottomSheet")
                dismiss()
            }
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar evento")
            .setMessage("¿Estás seguro de que deseas eliminar este evento?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteEvent()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteEvent() {
        val currentEvent = event ?: return

        viewModel.deleteEvent(
            eventId = currentEvent.id,
            onSuccess = {
                Toast.makeText(requireContext(), "Evento eliminado", Toast.LENGTH_SHORT).show()
                viewModel.notifyEventChanged()
                dismiss()
            },
            onError = { errorMessage ->
                Toast.makeText(requireContext(), "Error: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}