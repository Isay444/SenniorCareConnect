package com.isa.cuidadocompartidomayor.ui.agenda.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.AgendaEvent
import com.isa.cuidadocompartidomayor.data.model.EventType
import com.isa.cuidadocompartidomayor.databinding.ItemDashboardEventBinding

class DashboardEventAdapter(
    private val onInfoClick: (AgendaEvent) -> Unit
) : ListAdapter<AgendaEvent, DashboardEventAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDashboardEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemDashboardEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: AgendaEvent) {
            binding.apply {
                // Fecha (día y mes)
                tvDayNumber.text = event.getDayOfMonth()
                tvMonthAbbr.text = event.getMonthAbbreviation()

                // Color del bloque de fecha según el tipo
                val dateCardColor = when (event.type) {
                    EventType.MEDICAL_APPOINTMENT -> {
                        ContextCompat.getColor(root.context, R.color.medical_appointment_stroke)
                    }
                    EventType.NORMAL_TASK -> {
                        ContextCompat.getColor(root.context, R.color.normal_task_stroke)
                    }
                }

                dateCard.setCardBackgroundColor(dateCardColor)

                // Badge "HOY"
                if (event.isToday()){
                    chipToday.visibility = View.VISIBLE
                    layoutCard.strokeColor = ContextCompat.getColor(root.context, R.color.azul_principal)
                    Log.d("DashboardEventAdapter", "Badge 'HOY' visible")

                } else {
                    ContextCompat.getColor(root.context, R.color.card_stroke)
                    chipToday.visibility= View.GONE
                }

                // Título del evento
                tvEventType.text = event.title

                // Adulto mayor
                tvElderly.text = "Para: ${event.elderlyName}"
                //Cuidador que añadio el evento
                tvCreatedBy.text = "Añadido por: ${event.createdByName}"

                // Hora
                tvTime.text = event.getFormattedTime()

                // Información específica según el tipo
                when (event) {
                    is AgendaEvent.MedicalAppointment -> {
                        tvLocationOrAssigned.visibility = View.VISIBLE
                        tvLocationOrAssigned.text = event.location
                        tvLocationOrAssigned.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.ic_location_pin, 0, 0, 0
                        )

                        // ✅ Mostrar tipo en badge
                        tvTaskType.visibility = View.VISIBLE
                        tvTaskType.text = "Cita Médica"
                        tvTaskType.setTextColor(ContextCompat.getColor(root.context, R.color.medical_appointment_stroke))
                        tvTaskType.setBackgroundResource(R.drawable.bg_cite_medic)
                    }
                    is AgendaEvent.NormalTask -> {
                        if (event.assignedToName != null) {
                            tvLocationOrAssigned.visibility = View.VISIBLE
                            tvLocationOrAssigned.text = "Asignado a: ${event.assignedToName}"
                            tvLocationOrAssigned.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                R.drawable.ic_avatar, 0, 0, 0
                            )
                        } else {
                            tvLocationOrAssigned.visibility = View.GONE
                        }

                        // ✅ Mostrar tipo en badge
                        tvTaskType.text = "Tarea"
                        tvTaskType.setTextColor(ContextCompat.getColor(root.context, R.color.normal_task_stroke))
                        tvTaskType.setBackgroundResource(R.drawable.bg_task)

                        tvTaskType.visibility = View.VISIBLE

                        if (event.isCompleted) {
                            tvTaskStatus.visibility = View.VISIBLE
                            tvTaskStatus.setBackgroundColor(
                                ContextCompat.getColor(root.context, R.color.event_completed)
                            )
                        } else if (event.isPast()) {
                            tvTaskStatus.visibility = View.VISIBLE
                            tvTaskStatus.text = "Vencida"
                            tvTaskStatus.setBackgroundColor(
                                ContextCompat.getColor(root.context, R.color.event_overdue)
                            )
                        } else {
                            tvTaskStatus.visibility = View.VISIBLE
                            tvTaskStatus.text = "Pendiente"
                            tvTaskStatus.setBackgroundColor(
                                ContextCompat.getColor(root.context, R.color.event_pending)
                            )
                        }
                    }
                }

                // click en la card
                root.setOnClickListener {
                    onInfoClick(event)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AgendaEvent>() {
        override fun areItemsTheSame(oldItem: AgendaEvent, newItem: AgendaEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AgendaEvent, newItem: AgendaEvent): Boolean {
            return oldItem == newItem
        }
    }
}
