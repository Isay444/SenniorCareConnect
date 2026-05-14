package com.isa.cuidadocompartidomayor.ui.diary.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.*
import com.isa.cuidadocompartidomayor.databinding.ItemMoodEntryBinding
import java.text.SimpleDateFormat
import java.util.*

class MoodEntryAdapter(
    private val onEditClick: (MoodEntry) -> Unit,
    private val onDeleteClick: (MoodEntry) -> Unit
) : ListAdapter<MoodEntry, MoodEntryAdapter.MoodEntryViewHolder>(MoodEntryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodEntryViewHolder {
        val binding = ItemMoodEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MoodEntryViewHolder(binding, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: MoodEntryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MoodEntryViewHolder(
        private val binding: ItemMoodEntryBinding,
        private val onEditClick: (MoodEntry) -> Unit,
        private val onDeleteClick: (MoodEntry) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(moodEntry: MoodEntry) {
            // Fecha
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(Date(moodEntry.timestamp))

            // Nombre del adulto mayor
            binding.tvElderlyName.text = "👤 ${moodEntry.elderlyName}"

            // Registrado por
            binding.tvCaregiverName.text = "Registrado por: ${moodEntry.caregiverName}"

            //  NUEVO: Mostrar info de edición si existe
            if (moodEntry.editedBy != null && moodEntry.editedAt != null) {
                binding.tvEditInfo.visibility = View.VISIBLE
                val editDateFormat = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault())
                binding.tvEditInfo.text =
                    "✏️ Editado por: ${moodEntry.editedByName ?: "Usuario"} - ${editDateFormat.format(Date(moodEntry.editedAt))}"
            } else {
                binding.tvEditInfo.visibility = View.GONE
            }

            // Emociones
            val emotions = moodEntry.getEmotionsList()
            if (emotions.isNotEmpty()) {
                binding.layoutEmotions.visibility = View.VISIBLE
                binding.chipGroupEmotions.removeAllViews()

                emotions.forEach { emotion ->
                    val chip = createChip(
                        "${emotion.emoji} ${emotion.displayName}",
                        R.color.background_secondary
                    )
                    binding.chipGroupEmotions.addView(chip)
                }
            } else {
                binding.layoutEmotions.visibility = View.GONE
            }

            // Síntomas
            val symptoms = moodEntry.getSymptomsList()
            if (symptoms.isNotEmpty()) {
                binding.layoutSymptoms.visibility = View.VISIBLE
                binding.chipGroupSymptoms.removeAllViews()

                symptoms.forEach { symptom ->
                    val chip = createChip(
                        "${symptom.emoji} ${symptom.displayName}",
                        R.color.status_warning
                    )
                    binding.chipGroupSymptoms.addView(chip)
                }
            } else {
                binding.layoutSymptoms.visibility = View.GONE
            }

            // Niveles
            var hasLevels = false

            moodEntry.getEnergyLevelEnum()?.let { energy ->
                binding.tvEnergyLevel.visibility = View.VISIBLE
                binding.tvEnergyLevel.text = "${energy.emoji} Energía: ${energy.displayName}"
                hasLevels = true
            } ?: run {
                binding.tvEnergyLevel.visibility = View.GONE
            }

            moodEntry.getAppetiteLevelEnum()?.let { appetite ->
                binding.tvAppetiteLevel.visibility = View.VISIBLE
                binding.tvAppetiteLevel.text = "${appetite.emoji} Apetito: ${appetite.displayName}"
                hasLevels = true
            } ?: run {
                binding.tvAppetiteLevel.visibility = View.GONE
            }

            moodEntry.getFunctionalCapacityEnum()?.let { capacity ->
                binding.tvFunctionalCapacity.visibility = View.VISIBLE
                binding.tvFunctionalCapacity.text = "${capacity.emoji} Cap. Funcional: ${capacity.displayName}"
                hasLevels = true
            } ?: run {
                binding.tvFunctionalCapacity.visibility = View.GONE
            }

            binding.layoutLevels.visibility = if (hasLevels) View.VISIBLE else View.GONE

            // Notas
            if (moodEntry.notes.isNotEmpty()) {
                binding.layoutNotes.visibility = View.VISIBLE
                binding.tvNotes.text = moodEntry.notes
            } else {
                binding.layoutNotes.visibility = View.GONE
            }

            // Click listeners
            binding.btnEdit.setOnClickListener { onEditClick(moodEntry) }
            binding.btnDelete.setOnClickListener { onDeleteClick(moodEntry) }
        }

        private fun createChip(text: String, backgroundColorRes: Int): Chip {
            return Chip(binding.root.context).apply {
                this.text = text
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, backgroundColorRes)
                )
                setTextColor(
                    ContextCompat.getColor(context, R.color.text_primary)
                )
                isClickable = false
                isCheckable = false
            }
        }
    }

    private class MoodEntryDiffCallback : DiffUtil.ItemCallback<MoodEntry>() {
        override fun areItemsTheSame(oldItem: MoodEntry, newItem: MoodEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MoodEntry, newItem: MoodEntry): Boolean {
            return oldItem == newItem
        }
    }
}