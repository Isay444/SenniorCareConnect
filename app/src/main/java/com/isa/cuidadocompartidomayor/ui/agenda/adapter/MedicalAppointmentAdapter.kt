package com.isa.cuidadocompartidomayor.ui.agenda.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.AgendaEvent
import com.isa.cuidadocompartidomayor.databinding.ItemMedicalAppointmentBinding

class MedicalAppointmentAdapter(
    private val onEditClick: (AgendaEvent.MedicalAppointment) -> Unit,
    private val onDeleteClick: (AgendaEvent.MedicalAppointment) -> Unit,
    private val onItemClick: (AgendaEvent.MedicalAppointment) -> Unit
) : ListAdapter<AgendaEvent.MedicalAppointment, MedicalAppointmentAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMedicalAppointmentBinding.inflate(
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
        private val binding: ItemMedicalAppointmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(appointment: AgendaEvent.MedicalAppointment) {
            binding.apply {
                // Título
                tvTitle.text = appointment.title

                // Hora
                tvTime.text = appointment.getFormattedTime()

                // Ubicación
                tvLocation.text = appointment.location

                // Añadido por
                tvAddedBy.text = "Añadido por: ${appointment.createdByName}"

                // Notas
                if (appointment.notes.isNotBlank()) {
                    tvNotes.visibility = View.VISIBLE
                    tvNotes.text = "Notas: ${appointment.notes}"
                } else {
                    tvNotes.visibility = View.GONE
                }

                // Menú de opciones
                btnMenu.setOnClickListener { view ->
                    showPopupMenu(view, appointment)
                }

                // Click en el item completo
                root.setOnClickListener {
                    onItemClick(appointment)
                }
            }
        }

        private fun showPopupMenu(view: View, appointment: AgendaEvent.MedicalAppointment) {
            val popupMenu = PopupMenu(view.context, view)
            popupMenu.inflate(R.menu.menu_event_options)

            // Deshabilitar edición si la cita ya pasó
            if (appointment.isPast()) {
                popupMenu.menu.findItem(R.id.action_edit)?.isEnabled = false
            }

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        // ✅ CORREGIDO: Llamar al callback, el Fragment lo maneja
                        onEditClick(appointment)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteClick(appointment)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AgendaEvent.MedicalAppointment>() {
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