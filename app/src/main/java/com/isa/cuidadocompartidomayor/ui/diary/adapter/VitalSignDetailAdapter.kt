package com.isa.cuidadocompartidomayor.ui.diary.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.isa.cuidadocompartidomayor.databinding.ItemVitalSignDetailBinding

class VitalSignDetailAdapter :
    ListAdapter<VitalSignAdapter.VitalSignDetailItem, VitalSignDetailAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVitalSignDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemVitalSignDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VitalSignAdapter.VitalSignDetailItem) {
            // Icono
            binding.ivIcon.setImageResource(item.icon)
            binding.ivIcon.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(binding.root.context, item.iconTint)
            )

            // Label y valor
            binding.tvLabel.text = item.label
            binding.tvValue.text = item.value



            // Chip de estado
            if (item.status != null && item.statusColor != null) {
                binding.chipStatus.visibility = View.VISIBLE
                binding.chipStatus.text = item.status
                binding.chipStatus.chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(binding.root.context, item.statusColor)
                )
                binding.chipStatus.setTextColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.white)
                )
            } else {
                binding.chipStatus.visibility = View.GONE
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<VitalSignAdapter.VitalSignDetailItem>() {
        override fun areItemsTheSame(
            oldItem: VitalSignAdapter.VitalSignDetailItem,
            newItem: VitalSignAdapter.VitalSignDetailItem
        ): Boolean {
            return oldItem.label == newItem.label
        }

        override fun areContentsTheSame(
            oldItem: VitalSignAdapter.VitalSignDetailItem,
            newItem: VitalSignAdapter.VitalSignDetailItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}