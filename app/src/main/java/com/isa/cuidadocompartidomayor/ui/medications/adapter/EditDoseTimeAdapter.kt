package com.isa.cuidadocompartidomayor.ui.medications.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.isa.cuidadocompartidomayor.databinding.ItemDoseTimeBinding

class EditDoseTimeAdapter(
    private val onTimeClick: (Int, String) -> Unit,
    private val onDeleteClick: ((Int) -> Unit)? = null, // Null si no se puede eliminar
    private val isEditable: Boolean = true
) : RecyclerView.Adapter<EditDoseTimeAdapter.TimeViewHolder>() {

    private val times = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeViewHolder {
        val binding = ItemDoseTimeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeViewHolder, position: Int) {
        holder.bind(times[position], position)
    }

    override fun getItemCount(): Int = times.size

    fun updateTimes(newTimes: List<String>) {
        times.clear()
        times.addAll(newTimes)
        notifyDataSetChanged()
    }

    inner class TimeViewHolder(
        private val binding: ItemDoseTimeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(time: String, position: Int) {
            // Formato de la hora (convertir de 24h a 12h con AM/PM)
            val formattedTime = formatTimeTo12Hour(time)

            // ✅ Usar los campos correctos de tu XML
            binding.tvDoseNumber.text = "${position + 1}ª dosis"
            binding.tvTime.text = formattedTime

            // Click para editar hora
            binding.root.setOnClickListener {
                onTimeClick(position, time)
            }
        }

        private fun formatTimeTo12Hour(time24: String): String {
            val parts = time24.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1]

            val period = if (hour >= 12) "PM" else "AM"
            val hour12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }

            return String.format("%02d:%s %s", hour12, minute, period)
        }
    }
}
