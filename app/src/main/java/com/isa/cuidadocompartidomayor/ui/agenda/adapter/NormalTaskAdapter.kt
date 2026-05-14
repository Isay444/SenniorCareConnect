package com.isa.cuidadocompartidomayor.ui.agenda.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.AgendaEvent
import com.isa.cuidadocompartidomayor.databinding.ItemNormalTaskBinding

class NormalTaskAdapter(
    private val onCheckboxClick: (AgendaEvent.NormalTask, Boolean) -> Unit,
    private val onEditClick: (AgendaEvent.NormalTask) -> Unit,
    private val onDeleteClick: (AgendaEvent.NormalTask) -> Unit,
    private val onItemClick: (AgendaEvent.NormalTask) -> Unit
) : ListAdapter<AgendaEvent.NormalTask, NormalTaskAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNormalTaskBinding.inflate(
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
        private val binding: ItemNormalTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: AgendaEvent.NormalTask) {
            binding.apply {
                // Checkbox
                checkboxComplete.isChecked = task.isCompleted

                // Prevenir bucle infinito
                checkboxComplete.setOnCheckedChangeListener(null)
                checkboxComplete.isChecked = task.isCompleted
                checkboxComplete.setOnCheckedChangeListener { _, isChecked ->
                    onCheckboxClick(task, isChecked)
                }

                // Título (con tachado si está completada)
                tvTitle.text = task.title
                if (task.isCompleted) {
                    tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    tvTitle.alpha = 0.6f
                } else {
                    tvTitle.paintFlags = tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    tvTitle.alpha = 1f
                }

                // Hora
                tvTime.text = task.getFormattedTime()
                if (task.isCompleted) {
                    tvTime.alpha = 0.6f
                } else {
                    tvTime.alpha = 1f
                }

                // Asignado a
                if (task.assignedToName != null) {
                    tvAssignedTo.visibility = View.VISIBLE
                    tvAssignedTo.text = "Asignado a: ${task.assignedToName}"
                    if (task.isCompleted) {
                        tvAssignedTo.alpha = 0.6f
                    } else {
                        tvAssignedTo.alpha = 1f
                    }
                } else {
                    tvAssignedTo.visibility = View.GONE
                }
                // Añadido por
                if (task.createdByName.isNotBlank()) {
                    tvCreatedBy.visibility = View.VISIBLE
                    tvCreatedBy.text = "Añadido por: ${task.createdByName}"
                    if (task.isCompleted) {
                        tvCreatedBy.alpha = 0.6f
                    }else{
                        tvCreatedBy.alpha = 1f
                    }
                } else {
                    tvCreatedBy.visibility = View.GONE
                }


                // Notas/Instrucciones
                if (task.notes.isNotBlank()) {
                    tvNotes.visibility = View.VISIBLE
                    tvNotes.text = "Instrucciones: ${task.notes}"
                    if (task.isCompleted) {
                        tvNotes.alpha = 0.6f
                    } else {
                        tvNotes.alpha = 1f
                    }
                } else {
                    tvNotes.visibility = View.GONE
                }

                // Menú de opciones
                btnMenu.setOnClickListener { view ->
                    showPopupMenu(view, task)
                }

                // Click en el item completo
                root.setOnClickListener {
                    onItemClick(task)
                }
            }
        }

        private fun showPopupMenu(view: View, task: AgendaEvent.NormalTask) {
            val popupMenu = PopupMenu(view.context, view)
            popupMenu.inflate(R.menu.menu_task_options)

            // Deshabilitar edición si está completada o ya pasó
            if (task.isCompleted || task.isPast()) {
                popupMenu.menu.findItem(R.id.action_edit)?.isEnabled = false
            }

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        onEditClick(task)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteClick(task)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AgendaEvent.NormalTask>() {
        override fun areItemsTheSame(
            oldItem: AgendaEvent.NormalTask,
            newItem: AgendaEvent.NormalTask
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: AgendaEvent.NormalTask,
            newItem: AgendaEvent.NormalTask
        ): Boolean = oldItem == newItem
    }
}
