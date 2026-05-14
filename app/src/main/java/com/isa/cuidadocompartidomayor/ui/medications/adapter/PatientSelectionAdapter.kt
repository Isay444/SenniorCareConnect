package com.isa.cuidadocompartidomayor.ui.medications.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.ElderlyItem
import com.isa.cuidadocompartidomayor.databinding.ItemPatientSelectionBinding

class PatientSelectionAdapter(
    private val onPatientSelected: (ElderlyItem) -> Unit
) : RecyclerView.Adapter<PatientSelectionAdapter.PatientViewHolder>() {

    private var patients = listOf<ElderlyItem>()
    private var selectedPatientId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = ItemPatientSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PatientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(patients[position], patients[position].id == selectedPatientId)
    }

    override fun getItemCount(): Int = patients.size

    fun updatePatients(newPatients: List<ElderlyItem>) {
        val diffCallback = PatientDiffCallback(patients, newPatients)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        patients = newPatients
        diffResult.dispatchUpdatesTo(this)
    }

    fun getSelectedPatient(): ElderlyItem? {
        return patients.find { it.id == selectedPatientId }
    }

    inner class PatientViewHolder(
        private val binding: ItemPatientSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(patient: ElderlyItem, isSelected: Boolean) {
            binding.apply {
                tvPatientName.text = patient.name
                tvPatientInfo.text = "Toca para seleccionar"

                // Cargar imagen con Glide
                Glide.with(root.context)
                    .load(patient.profileImageUrl)
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .circleCrop()
                    .into(ivPatientAvatar)

                // Cambiar estilo según selección
                if (isSelected) {
                    cardPatient.strokeColor = ContextCompat.getColor(root.context, R.color.main_accent)
                    cardPatient.strokeWidth = 6
                    ivCheckmark.visibility = android.view.View.VISIBLE
                } else {
                    cardPatient.strokeColor = ContextCompat.getColor(root.context, R.color.card_stroke)
                    cardPatient.strokeWidth = 2
                    ivCheckmark.visibility = android.view.View.GONE
                }

                root.setOnClickListener {
                    val oldSelectedId = selectedPatientId
                    selectedPatientId = patient.id

                    // Actualizar ambas tarjetas
                    notifyItemChanged(patients.indexOfFirst { it.id == oldSelectedId })
                    notifyItemChanged(bindingAdapterPosition)

                    onPatientSelected(patient)
                }
            }
        }
    }

    private class PatientDiffCallback(
        private val oldList: List<ElderlyItem>,
        private val newList: List<ElderlyItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
