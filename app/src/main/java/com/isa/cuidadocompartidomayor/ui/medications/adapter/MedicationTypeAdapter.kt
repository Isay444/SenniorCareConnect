package com.isa.cuidadocompartidomayor.ui.medications.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.MedicationType
import com.isa.cuidadocompartidomayor.databinding.ItemMedicationTypeBinding

class MedicationTypeAdapter(
    private val onTypeSelected: (MedicationType) -> Unit
) : RecyclerView.Adapter<MedicationTypeAdapter.TypeViewHolder>() {

    private var types = listOf<MedicationType>()
    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TypeViewHolder {
        val binding = ItemMedicationTypeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TypeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TypeViewHolder, position: Int) {
        holder.bind(types[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = types.size

    fun updateTypes(newTypes: List<MedicationType>) {
        types = newTypes
        notifyDataSetChanged()
    }

    inner class TypeViewHolder(
        private val binding: ItemMedicationTypeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(type: MedicationType, isSelected: Boolean) {
            binding.apply {
                ivIcon.setImageResource(type.iconRes)
                tvTypeName.text = type.displayName

                // Color según selección
                if (isSelected) {
                    cardType.strokeColor = ContextCompat.getColor(root.context, R.color.main_accent)
                    cardType.strokeWidth = 4
                } else {
                    cardType.strokeColor = ContextCompat.getColor(root.context, R.color.card_stroke)
                    cardType.strokeWidth = 1
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