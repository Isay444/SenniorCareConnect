package com.isa.cuidadocompartidomayor.ui.medications.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.isa.cuidadocompartidomayor.databinding.ItemDoseTimeBinding

class DoseTimeAdapter(
    private val onTimeClick: (position: Int, currentTime: String) -> Unit
) : RecyclerView.Adapter<DoseTimeAdapter.DoseTimeViewHolder>() {

    private var times = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoseTimeViewHolder {
        val binding = ItemDoseTimeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DoseTimeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DoseTimeViewHolder, position: Int) {
        holder.bind(position, times[position])
    }

    override fun getItemCount(): Int = times.size

    fun updateTimes(newTimes: List<String>) {
        times = newTimes.toMutableList()
        notifyDataSetChanged()
    }

    inner class DoseTimeViewHolder(
        private val binding: ItemDoseTimeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int, time: String) {
            binding.apply {
                tvDoseNumber.text = "${position + 1} hora consumo"
                tvTime.text = formatTime(time)

                root.setOnClickListener {
                    onTimeClick(position, time)
                }
            }
        }

        private fun formatTime(time: String): String {
            val parts = time.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()

            return String.format("%02d:%02d", hour, minute)
        }
    }
}