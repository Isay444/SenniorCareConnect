package com.isa.cuidadocompartidomayor.ui.medications.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.FrequencyType
import com.isa.cuidadocompartidomayor.databinding.ItemFrequencyTypeBinding

class FrequencyTypeAdapter(
    private val onTypeSelected: (FrequencyType) -> Unit
) : RecyclerView.Adapter<FrequencyTypeAdapter.FrequencyViewHolder>() {

    private var types = listOf<FrequencyType>()
    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrequencyViewHolder {
        val binding = ItemFrequencyTypeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FrequencyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FrequencyViewHolder, position: Int) {
        holder.bind(types[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = types.size

    fun updateTypes(newTypes: List<FrequencyType>) {
        types = newTypes
        notifyDataSetChanged()
    }

    inner class FrequencyViewHolder(
        private val binding: ItemFrequencyTypeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(type: FrequencyType, isSelected: Boolean) {
            binding.apply {
                tvFrequencyName.text = type.displayName

                // Color según selección
                if (isSelected) {
                    cardFrequency.strokeColor = ContextCompat.getColor(root.context, R.color.main_accent)
                    cardFrequency.strokeWidth = 4
                } else {
                    cardFrequency.strokeColor = ContextCompat.getColor(root.context, R.color.card_stroke)
                    cardFrequency.strokeWidth = 1
                }

                root.setOnClickListener {
                    val oldPosition = selectedPosition
                    selectedPosition = bindingAdapterPosition
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)
                    onTypeSelected(type)
                }
            }
        }
    }
}