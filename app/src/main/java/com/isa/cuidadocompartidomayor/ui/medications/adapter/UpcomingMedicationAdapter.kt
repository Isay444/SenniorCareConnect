package com.isa.cuidadocompartidomayor.ui.medications.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.LogStatus
import com.isa.cuidadocompartidomayor.data.model.Medication
import com.isa.cuidadocompartidomayor.data.model.MedicationLog
import com.isa.cuidadocompartidomayor.databinding.ItemUpcomingMedicationBinding

class UpcomingMedicationAdapter(
    private val onConfirmClick: (Medication, MedicationLog) -> Unit,
    private val onSkipClick: (Medication, MedicationLog) -> Unit,
    private val onPendingClick: (Medication, MedicationLog) -> Unit,
    private val onInfoClick: (Medication, MedicationLog) -> Unit
) : RecyclerView.Adapter<UpcomingMedicationAdapter.ViewHolder>() {

    private var medications = listOf<MedicationWithStatus>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUpcomingMedicationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(medications[position])
    }

    override fun getItemCount() = medications.size

    fun submitList(newList: List<MedicationWithStatus>) {
        medications = newList
        notifyDataSetChanged()
    }

    inner class ViewHolder( private val binding: ItemUpcomingMedicationBinding ) : RecyclerView.ViewHolder(binding.root)
    {
        private var isExpanded = false

        fun bind(item: MedicationWithStatus) {
            val medication = item.medication
            val log = item.log  // 
            val status = item.currentStatus

            binding.apply {
                // Icono del tipo de medicamento
                ivMedicationType.setImageResource(medication.medicationType.iconRes)
                ivMedicationType.setColorFilter(
                    ContextCompat.getColor(root.context, R.color.main_accent)
                )

                // Nombre del medicamento
                tvMedicationName.text = medication.name

                // Hora y frecuencia
                // ✅ CAMBIO CRÍTICO: Usar la hora del LOG, no del medication
                val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val timeString = timeFormat.format(java.util.Date(log.scheduledTime))

                tvTimeAndFrequency.text = "$timeString | ${medication.getFrequencyText()}"

                // Nombre del paciente
                tvElderlyName.text = "Para: ${medication.elderlyName}"

                // ✅ CONFIGURAR ESTADO VISUAL
                updateStatusUI(status)

                // ✅ Botón de información (siempre visible)
                btnInfo.setOnClickListener {
                    onInfoClick(medication, log)
                }

                // Expandir/Contraer acciones Siempre habilitado
                btnExpand.setOnClickListener {
                    isExpanded = !isExpanded
                    layoutActions.visibility = if (isExpanded) View.VISIBLE else View.GONE
                    btnExpand.rotation = if (isExpanded) 180f else 0f

                    // ✅ Mostrar botones según estado actual
                    updateActionButtons(status)
                }

                // Botón Confirmar
                btnConfirm.setOnClickListener {
                    onConfirmClick(medication, log)
                    layoutActions.visibility = View.GONE
                    isExpanded = false
                    btnExpand.rotation = 0f
                }

                // Botón Omitir
                btnSkip.setOnClickListener {
                    onSkipClick(medication, log)
                    layoutActions.visibility = View.GONE
                    isExpanded = false
                    btnExpand.rotation = 0f
                }

                // Botón Pendiente
                btnPending.setOnClickListener {
                    onPendingClick(medication, log)
                    layoutActions.visibility = View.GONE
                    isExpanded = false
                    btnExpand.rotation = 0f
                }

                // Resetear estado de expansión al reciclar
                layoutActions.visibility = View.GONE
                isExpanded = false
                btnExpand.rotation = 0f
            }
        }

        private fun updateStatusUI(status: LogStatus) {
            binding.apply {
                when (status) {
                    LogStatus.PENDING -> {
                        // ⏳ Pendiente: Borde gris, sin icono de estado
                        cardMedication.strokeColor = ContextCompat.getColor(root.context, R.color.status_pending)
                        ivStatusIndicator.visibility = View.GONE
                    }

                    LogStatus.ON_TIME -> {
                        // ✅ Confirmado: Borde verde, icono de check
                        cardMedication.strokeColor = ContextCompat.getColor(root.context, R.color.status_on_time)
                        ivStatusIndicator.visibility = View.VISIBLE
                        ivStatusIndicator.setImageResource(R.drawable.ic_check_circle)
                        ivStatusIndicator.setColorFilter(ContextCompat.getColor(root.context, R.color.status_on_time))
                    }

                    LogStatus.MISSED -> {
                        // ⏭️ Omitido: Borde naranja, icono de skip
                        cardMedication.strokeColor = ContextCompat.getColor(root.context, R.color.status_missed)
                        ivStatusIndicator.visibility = View.VISIBLE
                        ivStatusIndicator.setImageResource(R.drawable.ic_skip_next)
                        ivStatusIndicator.setColorFilter(ContextCompat.getColor(root.context, R.color.status_missed))
                    }

                    LogStatus.LATE -> {
                        // ⏰ Atrasado: Borde rojo, sin icono
                        cardMedication.strokeColor = ContextCompat.getColor(root.context, R.color.status_on_time)
                        ivStatusIndicator.visibility = View.VISIBLE
                        ivStatusIndicator.setImageResource(R.drawable.ic_check_circle)
                        ivStatusIndicator.setColorFilter(ContextCompat.getColor(root.context, R.color.status_on_time))

                    }
                }
                btnExpand.isEnabled = true
            }
        }
        private fun updateActionButtons(currentStatus: LogStatus) {
            binding.apply {
                when (currentStatus) {
                    LogStatus.PENDING -> {
                        // Si está pendiente, mostrar solo Confirmar y Omitir
                        btnConfirm.visibility = View.VISIBLE
                        btnSkip.visibility = View.VISIBLE
                        btnPending.visibility = View.GONE
                    }
                    LogStatus.ON_TIME -> {
                        // Si está confirmado, permitir cambiar a Omitir o Pendiente
                        btnConfirm.visibility = View.GONE
                        btnSkip.visibility = View.VISIBLE
                        btnPending.visibility = View.VISIBLE
                    }
                    LogStatus.MISSED -> {
                        // Si está omitido, permitir cambiar a Confirmar o Pendiente
                        btnConfirm.visibility = View.VISIBLE
                        btnSkip.visibility = View.GONE
                        btnPending.visibility = View.VISIBLE
                    }
                    LogStatus.LATE -> {
                        // Si está atrasado, mostrar todas las opciones
                        btnConfirm.visibility = View.GONE
                        btnSkip.visibility = View.VISIBLE
                        btnPending.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    // Data class para combinar medicamento con su estado actual
    data class MedicationWithStatus(
        val medication: Medication,
        val log: MedicationLog,  // : Necesitamos el log completo
        val currentStatus: LogStatus = LogStatus.PENDING
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MedicationWithStatus) return false
            return medication.id == other.medication.id &&
                    log.id == other.log.id &&  // ✅ Comparar por log ID
                    currentStatus == other.currentStatus
        }

        override fun hashCode(): Int {
            var result = medication.id.hashCode()
            result = 31 * result + log.id.hashCode()  // ✅ Usar log ID
            result = 31 * result + currentStatus.hashCode()
            return result
        }
    }

}
