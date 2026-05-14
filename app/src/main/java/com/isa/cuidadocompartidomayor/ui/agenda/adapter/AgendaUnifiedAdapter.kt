package com.isa.cuidadocompartidomayor.ui.agenda.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.AgendaEvent
import com.isa.cuidadocompartidomayor.databinding.ItemAgendaEventUnifiedBinding

class AgendaUnifiedAdapter(
    private val onEventClick: (AgendaEvent) -> Unit,
    private val onEditClick: (AgendaEvent) -> Unit,
    private val onDeleteClick: (AgendaEvent) -> Unit,
    private val onTaskCheckChanged: (AgendaEvent.NormalTask, Boolean) -> Unit
) : ListAdapter<AgendaEvent, AgendaUnifiedAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemAgendaEventUnifiedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(
        private val binding: ItemAgendaEventUnifiedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: AgendaEvent) {
            when (event) {
                is AgendaEvent.MedicalAppointment -> bindMedicalAppointment(event)
                is AgendaEvent.NormalTask -> bindNormalTask(event)
            }

            // Click en toda la card
            binding.cardEvent.setOnClickListener {
                onEventClick(event)
            }

            // Botones de acción
            binding.btnEdit.setOnClickListener {
                onEditClick(event)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(event)
            }
        }

        private fun bindMedicalAppointment(appointment: AgendaEvent.MedicalAppointment) {
            binding.apply {
                // Badge de tipo
                chipEventType.apply {
                    text = context.getString(R.string.medical_appointment)
                    setChipIconResource(R.drawable.ic_medical)
                    setChipBackgroundColorResource(R.color.chip_medical_bg)
                    setTextColor(ContextCompat.getColor(context, R.color.chip_medical_text))
                }

                // Título
                tvTitle.text = appointment.title

                // Hora
                tvTime.text = appointment.getFormattedTime()

                // Ubicación
                ivDetailsIcon.setImageResource(R.drawable.ic_location_pin)
                tvDetails.text = appointment.location

                // Adulto mayor
                tvElderlyName.text = appointment.elderlyName

                // Notas
                if (appointment.notes.isNotBlank()) {
                    tvNotes.visibility = View.VISIBLE
                    tvNotes.text = appointment.notes
                } else {
                    tvNotes.visibility = View.GONE
                }

                // Ocultar checkbox (solo para tareas)
                checkboxComplete.visibility = View.GONE
            }
        }

        private fun bindNormalTask(task: AgendaEvent.NormalTask) {
            binding.apply {
                // Badge de tipo
                chipEventType.apply {
                    text = context.getString(R.string.normal_task)
                    setChipIconResource(R.drawable.ic_task)
                    setChipBackgroundColorResource(R.color.chip_task_bg)
                    setTextColor(ContextCompat.getColor(context, R.color.chip_task_text))
                }

                // Título
                tvTitle.text = task.title

                // Hora
                tvTime.text = task.getFormattedTime()

                // Asignado a
                ivDetailsIcon.setImageResource(R.drawable.ic_avatar)
                tvDetails.text = "Asignado a ${task.assignedToName}"

                // Adulto mayor
                tvElderlyName.text = "Adulto Mayor: ${task.elderlyName}"

                // Notas
                if (task.notes.isNotBlank()) {
                    tvNotes.visibility = View.VISIBLE
                    tvNotes.text = task.notes
                } else {
                    tvNotes.visibility = View.GONE
                }

                // Checkbox para completar
                checkboxComplete.apply {
                    visibility = View.VISIBLE
                    isChecked = task.isCompleted

                    // Listener para cambios
                    setOnCheckedChangeListener { _, isChecked ->
                        onTaskCheckChanged(task, isChecked)
                    }
                }
            }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<AgendaEvent>() {
        override fun areItemsTheSame(oldItem: AgendaEvent, newItem: AgendaEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AgendaEvent, newItem: AgendaEvent): Boolean {
            return oldItem == newItem
        }
    }
}
