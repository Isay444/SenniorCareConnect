package com.isa.cuidadocompartidomayor.ui.medications.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.Medication
import com.isa.cuidadocompartidomayor.databinding.ItemMedicationBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MedicationListAdapter(private val onEditClick: (Medication) -> Unit,
                            private val onDeleteClick: (Medication) -> Unit
) : RecyclerView.Adapter<MedicationListAdapter.MedicationViewHolder>() {
    private var medications = listOf<Medication>()
    private var onItemClickListener: ((Medication) -> Unit)? = null

    fun setOnItemClickListener(listener: (Medication) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MedicationListAdapter.MedicationViewHolder {
        val binding = ItemMedicationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MedicationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        holder.bind(medications[position])
    }

    override fun getItemCount(): Int = medications.size

    /** Actualiza la lista de medicamentos con DiffUtil */
    fun updateMedications(newMedications: List<Medication>) {
        val diffCallback = MedicationDiffCallback(medications, newMedications)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        medications = newMedications
        diffResult.dispatchUpdatesTo(this)
    }

    inner class MedicationViewHolder(
        private val binding: ItemMedicationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(medication: Medication) {
            binding.apply {
                // Icono del tipo de medicamento
                ivMedicationType.setImageResource(medication.medicationType.iconRes)
                ivMedicationType.setColorFilter(
                    ContextCompat.getColor(root.context, R.color.main_accent)
                )

                // Nombre y dosis
                tvMedicationName.text = medication.name
                tvDosage.text = medication.dosage

                // Frecuencia
                tvFrequency.text = medication.getFrequencyText()

                //Nombre adulto mayor
                tvElderlyName.text = "Para: ${medication.elderlyName}"

                // Información del cuidador
                tvCaregiverName.text = "Cuidador: ${medication.caregiverName}"

                // Próxima toma
                val nextDoseText = getNextDoseText(medication)
                tvNextDose.text = "Próxima dosis: $nextDoseText"
                tvNextDose.visibility = android.view.View.VISIBLE

                // Click en el item
                root.setOnClickListener {
                    onItemClickListener?.invoke(medication)
                }

                // Botón editar
                btnEdit.setOnClickListener {
                    onEditClick(medication)
                }

                // Long click para eliminar
                root.setOnLongClickListener {
                    onDeleteClick(medication)
                    true
                }
            }
        }

        /** Genera texto de próxima dosis con fecha si es necesario */
        private fun getNextDoseText(medication: Medication): String {
            val nextDateTime = medication.getNextScheduledDateTime()

            if (nextDateTime == null) {
                return "Sin próxima dosis programada"
            }

            val (timestamp, time) = nextDateTime
            val nextCal = Calendar.getInstance().apply { timeInMillis = timestamp }
            val now = Calendar.getInstance()

            // Comparar fechas (ignorando hora)
            val nextDate = Calendar.getInstance().apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            return when {
                // Hoy
                nextDate.timeInMillis == today.timeInMillis -> "Hoy $time"

                // Mañana
                nextDate.timeInMillis == tomorrow.timeInMillis -> "Mañana $time"

                // Otra fecha
                else -> {
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    /** DiffUtil Callback para optimizar actualizaciones */
    private class MedicationDiffCallback(
        private val oldList: List<Medication>,
        private val newList: List<Medication>
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