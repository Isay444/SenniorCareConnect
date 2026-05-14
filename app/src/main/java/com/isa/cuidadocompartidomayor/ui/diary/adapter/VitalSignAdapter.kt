package com.isa.cuidadocompartidomayor.ui.diary.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.*
import com.isa.cuidadocompartidomayor.databinding.ItemVitalSignBinding
import java.text.SimpleDateFormat
import java.util.*

class VitalSignAdapter(
    private val onEditClick: (VitalSign) -> Unit,
    private val onDeleteClick: (VitalSign) -> Unit
) : ListAdapter<VitalSign, VitalSignAdapter.VitalSignViewHolder>(VitalSignDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VitalSignViewHolder {
        val binding = ItemVitalSignBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VitalSignViewHolder(binding, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: VitalSignViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class VitalSignViewHolder(
        private val binding: ItemVitalSignBinding,
        private val onEditClick: (VitalSign) -> Unit,
        private val onDeleteClick: (VitalSign) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(vitalSign: VitalSign) {
            // Fecha
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(Date(vitalSign.timestamp))

            // Nombre del adulto mayor
            binding.tvElderlyName.text = "👤 ${vitalSign.elderlyName}"

            // Registrado por
            binding.tvCaregiverName.text = "Registrado por: ${vitalSign.caregiverName}"

            // Notas
            if (vitalSign.notes.isNotEmpty()) {
                binding.layoutNotes.visibility = View.VISIBLE
                binding.tvNotes.text = vitalSign.notes
            } else {
                binding.layoutNotes.visibility = View.GONE
            }

            //  NUEVO: Mostrar info de edición si existe
            if (vitalSign.editedBy != null && vitalSign.editedAt != null) {
                binding.tvEditInfo.visibility = View.VISIBLE
                val editDateFormat = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault())
                binding.tvEditInfo.text =
                    "✏️ Editado por: ${vitalSign.editedByName ?: "Usuario"} - ${editDateFormat.format(Date(vitalSign.editedAt))}"
            } else {
                binding.tvEditInfo.visibility = View.GONE
            }

            // Configurar RecyclerView interno con los signos vitales registrados
            setupVitalSignsRecyclerView(vitalSign)

            // Click listeners
            binding.btnEdit.setOnClickListener { onEditClick(vitalSign) }
            binding.btnDelete.setOnClickListener { onDeleteClick(vitalSign) }
        }

        private fun setupVitalSignsRecyclerView(vitalSign: VitalSign) {
            val items = mutableListOf<VitalSignDetailItem>()

            // Pulso
            vitalSign.heartRate?.let { heartRate ->
                val status = vitalSign.heartRateStatus?.let {
                    HeartRateStatus.valueOf(it)
                }
                items.add(
                    VitalSignDetailItem(
                        icon = R.drawable.ic_heart,
                        iconTint = R.color.pulse_stroke,
                        label = "Pulso",
                        value = "$heartRate LPM",
                        status = status?.displayName,
                        statusColor = status?.colorRes
                    )
                )
            }

            // Glucosa
            vitalSign.glucose?.let { glucose ->
                val momentText = vitalSign.glucoseMoment?.let {
                    GlucoseMoment.valueOf(it).displayName
                } ?: ""
                items.add(
                    VitalSignDetailItem(
                        icon = R.drawable.ic_glucose,
                        iconTint = R.color.glucose_stroke,
                        label = "Glucosa",
                        value = "$glucose mg/dL${if (momentText.isNotEmpty()) "\n($momentText)" else ""}",
                        status = null,
                        statusColor = null
                    )
                )
            }

            // Temperatura
            vitalSign.temperature?.let { temp ->
                val status = vitalSign.temperatureStatus?.let {
                    TemperatureStatus.valueOf(it)
                }
                items.add(
                    VitalSignDetailItem(
                        icon = R.drawable.ic_temperature,
                        iconTint = R.color.temperature_stroke,
                        label = "Temperatura",
                        value = "$temp °C",
                        status = status?.displayName,
                        statusColor = status?.colorRes
                    )
                )
            }

            // Presión Arterial
            if (vitalSign.systolicBP != null && vitalSign.diastolicBP != null) {
                val status = vitalSign.bpStatus?.let {
                    BloodPressureStatus.valueOf(it)
                }
                items.add(
                    VitalSignDetailItem(
                        icon = R.drawable.ic_blood_pressure,
                        iconTint = R.color.blood_pressure_stroke,
                        label = "Presión Arterial",
                        value = "${vitalSign.systolicBP}/${vitalSign.diastolicBP} mmHg",
                        status = status?.displayName,
                        statusColor = status?.colorRes
                    )
                )
            }

            // Saturación O2
            vitalSign.oxygenSaturation?.let { oxygen ->
                val status = vitalSign.oxygenStatus?.let {
                    OxygenStatus.valueOf(it)
                }
                items.add(
                    VitalSignDetailItem(
                        icon = R.drawable.ic_oxygen,
                        iconTint = R.color.oxygen_stroke,
                        label = "Saturación O₂",
                        value = "$oxygen %",
                        status = status?.displayName,
                        statusColor = status?.colorRes
                    )
                )
            }

            // Peso
            vitalSign.weight?.let { weight ->
                items.add(
                    VitalSignDetailItem(
                        icon = R.drawable.ic_weight,
                        iconTint = R.color.weight_stroke,
                        label = "Peso",
                        value = "$weight Kg",
                        status = null,
                        statusColor = null
                    )
                )
            }

            // Configurar adapter interno
            val detailAdapter = VitalSignDetailAdapter()
            binding.rvVitalSignItems.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = detailAdapter
            }
            detailAdapter.submitList(items)
        }
    }

    private class VitalSignDiffCallback : DiffUtil.ItemCallback<VitalSign>() {
        override fun areItemsTheSame(oldItem: VitalSign, newItem: VitalSign): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: VitalSign, newItem: VitalSign): Boolean {
            return oldItem == newItem
        }
    }

    // Data class para items del RecyclerView interno
    data class VitalSignDetailItem(
        val icon: Int,
        val iconTint: Int,
        val label: String,
        val value: String,
        val status: String?,
        val statusColor: Int?
    )
}
