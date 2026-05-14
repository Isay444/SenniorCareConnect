package com.isa.cuidadocompartidomayor.ui.caregiver.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.isa.cuidadocompartidomayor.databinding.ItemEmergencyAlertBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class EmergencyAlertAdapter(
    private val onDismissClick: (String) -> Unit
) : ListAdapter<Map<String, Any>, EmergencyAlertAdapter.EmergencyAlertViewHolder>(EmergencyAlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmergencyAlertViewHolder {
        val binding = ItemEmergencyAlertBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EmergencyAlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EmergencyAlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EmergencyAlertViewHolder(
        private val binding: ItemEmergencyAlertBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Map<String, Any>) {
            val elderlyName = notification["elderlyName"] as? String ?: "Paciente"
            val body = notification["body"] as? String ?: "Alerta de emergencia"
            val timestamp = notification["timestamp"] as? Long ?: System.currentTimeMillis()
            val latitude = notification["latitude"] as? Double
            val longitude = notification["longitude"] as? Double
            val alertId = notification["alertId"] as? String ?: ""

            binding.tvElderlyName.text = elderlyName
            binding.tvEmergencyMessage.text = body
            binding.tvTimeAgo.text = getTimeAgo(timestamp)

            // Botón para ver ubicación
            if (latitude != null && longitude != null) {
                binding.btnViewLocation.setOnClickListener {
                    openGoogleMaps(latitude, longitude)
                }
            } else {
                binding.btnViewLocation.isEnabled = false
                binding.btnViewLocation.text = "SIN UBICACIÓN"
            }

            // Botón para descartar alerta
            binding.btnDismissAlert.setOnClickListener {
                onDismissClick(alertId)
            }
        }

        private fun openGoogleMaps(latitude: Double, longitude: Double) {
            val uri = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            binding.root.context.startActivity(intent)
        }

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Ahora"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "Hace $minutes min"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "Hace $hours h"
                }
                else -> {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    dateFormat.format(Date(timestamp))
                }
            }
        }
    }

    class EmergencyAlertDiffCallback : DiffUtil.ItemCallback<Map<String, Any>>() {
        override fun areItemsTheSame(
            oldItem: Map<String, Any>,
            newItem: Map<String, Any>
        ): Boolean {
            return oldItem["alertId"] == newItem["alertId"]
        }

        override fun areContentsTheSame(
            oldItem: Map<String, Any>,
            newItem: Map<String, Any>
        ): Boolean {
            return oldItem == newItem
        }
    }
}
