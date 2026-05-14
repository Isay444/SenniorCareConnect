package com.isa.cuidadocompartidomayor.ui.caregiver.adapter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.Relationship
import com.isa.cuidadocompartidomayor.databinding.ItemPatientBinding
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class PatientAdapter (
    private val onPatientClick: (Relationship) -> Unit
    ) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {

        private var patients = listOf<Relationship>()

        companion object {
            private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
            val binding = ItemPatientBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return PatientViewHolder(binding)
        }

        override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
            holder.bind(patients[position])
        }

        override fun getItemCount(): Int = patients.size

        /**
         * Actualiza la lista de pacientes con DiffUtil para animaciones suaves
         */
        fun updatePatients(newPatients: List<Relationship>) {
            val diffCallback = PatientDiffCallback(patients, newPatients)
            val diffResult = DiffUtil.calculateDiff(diffCallback)

            patients = newPatients
            diffResult.dispatchUpdatesTo(this)
        }

        inner class PatientViewHolder(
            private val binding: ItemPatientBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(relationship: Relationship) {
                with(binding) {
                    // Foto del paciente
                    Glide.with(root.context)
                        .load(relationship.elderlyProfileImageUrl)
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.ic_avatar)
                        .circleCrop()
                        .into(ivPatientAvatar)

                    // Información básica
                    tvPatientName.text = relationship.elderlyName
                    tvPatientEmail.text = relationship.elderlyEmail

                    // Estado de la conexión
                    updateConnectionStatus(relationship.status)

                    // Fecha de conexión
                    val connectionDate = Date(relationship.createdAt)
                    tvConnectionDate.text = "Conectado: ${dateFormat.format(connectionDate)}"

                    // Rol y permisos
                    tvRole.text = when (relationship.role) {
                        "primary" -> "Cuidador Primario"
                        "secondary" -> "Cuidador Secundario"
                        else -> "Cuidador"
                    }

                    // Notas (si existen)
                    if (relationship.notes.isNotEmpty()) {
                        tvNotes.text = relationship.notes
                        tvNotes.visibility = android.view.View.VISIBLE
                    } else {
                        tvNotes.visibility = android.view.View.GONE
                    }

                    // Click listener
                    root.setOnClickListener {
                        onPatientClick(relationship)
                    }
                }
            }

            /**
             * Actualiza el estado visual de la conexión
             */
            private fun updateConnectionStatus(status: String) {
                val context = binding.root.context

                when (status) {
                    "active" -> {
                        binding.tvStatus.text = "Activo"
                        binding.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                        binding.cardPatient.strokeColor = ContextCompat.getColor(context, android.R.color.holo_green_light)
                        binding.ivStatusIcon.setImageResource(R.drawable.ic_person_select)
                        binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                    }
                    "pending" -> {
                        binding.tvStatus.text = "Pendiente"
                        binding.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
                        binding.cardPatient.strokeColor = ContextCompat.getColor(context, android.R.color.holo_orange_light)
                        binding.ivStatusIcon.setImageResource(R.drawable.ic_warning)
                        binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
                    }
                    "rejected" -> {
                        binding.tvStatus.text = "Rechazado"
                        binding.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                        binding.cardPatient.strokeColor = ContextCompat.getColor(context, android.R.color.holo_red_light)
                        binding.ivStatusIcon.setImageResource(R.drawable.ic_close)
                        binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                    }
                    else -> {
                        binding.tvStatus.text = "Desconocido"
                        binding.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                        binding.cardPatient.strokeColor = ContextCompat.getColor(context, android.R.color.darker_gray)
                        binding.ivStatusIcon.setImageResource(android.R.drawable.ic_menu_help)
                        binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray))
                    }
                }
            }
        }

        /**
         * DiffUtil callback para optimizar las actualizaciones del RecyclerView
         */
        private class PatientDiffCallback(
            private val oldList: List<Relationship>,
            private val newList: List<Relationship>
        ) : DiffUtil.Callback() {

            override fun getOldListSize(): Int = oldList.size

            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition].id == newList[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = oldList[oldItemPosition]
                val newItem = newList[newItemPosition]

                return oldItem.elderlyName == newItem.elderlyName &&
                        oldItem.status == newItem.status &&
                        oldItem.elderlyEmail == newItem.elderlyEmail &&
                        oldItem.role == newItem.role
            }
        }
}