package com.isa.cuidadocompartidomayor.ui.elderly.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.AgendaEvent
import com.isa.cuidadocompartidomayor.databinding.ItemElderlyAppointmentBinding
import java.text.SimpleDateFormat
import java.util.*

class ElderlyAppointmentAdapter(
    private val onAppointmentClick: (AgendaEvent.MedicalAppointment) -> Unit
) : ListAdapter<AgendaEvent.MedicalAppointment, ElderlyAppointmentAdapter.AppointmentViewHolder>(AppointmentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val binding = ItemElderlyAppointmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppointmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppointmentViewHolder(
        private val binding: ItemElderlyAppointmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(appointment: AgendaEvent.MedicalAppointment) {
            binding.apply {
                // Título de la cita
                tvAppointmentTitle.text = appointment.title

                // Hora
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                tvAppointmentTime.text = timeFormat.format(Date(appointment.date))

                // Ubicación
                tvAppointmentLocation.text = appointment.location

                // Click para ver detalles
                root.setOnClickListener {
                    onAppointmentClick(appointment)
                }
            }
        }
    }

    private class AppointmentDiffCallback : DiffUtil.ItemCallback<AgendaEvent.MedicalAppointment>() {
        override fun areItemsTheSame(
            oldItem: AgendaEvent.MedicalAppointment,
            newItem: AgendaEvent.MedicalAppointment
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: AgendaEvent.MedicalAppointment,
            newItem: AgendaEvent.MedicalAppointment
        ): Boolean = oldItem == newItem
    }
}
