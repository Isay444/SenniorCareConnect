package com.isa.cuidadocompartidomayor.ui.medications.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.isa.cuidadocompartidomayor.data.model.MedicationLog
import com.isa.cuidadocompartidomayor.databinding.ItemMedicationLogBinding

class MedicationLogAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = listOf<LogItem>()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_LOG = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is LogItem.Header -> TYPE_HEADER
            is LogItem.Log -> TYPE_LOG
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(
                    android.R.layout.simple_list_item_1,
                    parent,
                    false
                )
                HeaderViewHolder(view)
            }
            else -> {
                val binding = ItemMedicationLogBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                LogViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is LogItem.Header -> (holder as HeaderViewHolder).bind(item.date)
            is LogItem.Log -> (holder as LogViewHolder).bind(item.log)
        }
    }

    override fun getItemCount(): Int = items.size

    /** Actualiza la lista con logs agrupados por fecha */
    fun updateLogs(logsGroupedByDate: Map<String, List<MedicationLog>>) {
        val newItems = mutableListOf<LogItem>()

        logsGroupedByDate.forEach { (date, logs) ->
            // Agregar header de fecha
            newItems.add(LogItem.Header(date))

            // Agregar logs de esa fecha
            logs.forEach { log ->
                newItems.add(LogItem.Log(log))
            }
        }

        val diffCallback = LogItemDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    /** ViewHolder para headers de fecha */
    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView = itemView.findViewById<android.widget.TextView>(android.R.id.text1)

        fun bind(date: String) {
            textView.text = date
            textView.textSize = 16f
            textView.setTypeface(null, android.graphics.Typeface.BOLD)
            textView.setPadding(32, 24, 16, 8)
        }
    }

    /** ViewHolder para logs de medicamentos */
    class LogViewHolder(
        private val binding: ItemMedicationLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(log: MedicationLog) {
            binding.apply {
                // Nombre del medicamento
                tvMedicationName.text = log.getMedicationStatusText()
                // ✅ Y AGREGAR ESTILO SI ESTÁ ELIMINADO:
                if (!log.medicationActive) {
                    tvMedicationName.setTextColor(Color.GRAY)
                    tvMedicationName.paintFlags = tvMedicationName.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    tvMedicationName.setTextColor(Color.BLACK)
                    tvMedicationName.paintFlags = tvMedicationName.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
                // Dosis y frecuencia
                tvDosageFrequency.text = "${log.dosage}     |     ${log.frequency}"
                //nombre paciente
                tvElderlyName.text = "Para: ${log.elderlyName}"
                // Próxima dosis (hora programada)
                tvScheduledTime.text = "Hora: ${log.getFormattedTime()}"

                // Badge de estado
                tvStatus.text = log.getStatusText()
                tvStatus.setBackgroundColor(log.getStatusColor())
                tvStatus.setTextColor(Color.WHITE)

                // Color del borde de la tarjeta según estado
                cardLog.strokeColor = log.getStatusColor()

                cardLog.strokeWidth = 4
            }
        }
    }

    /** Sealed class para items de la lista (Header o Log) */
    sealed class LogItem {
        data class Header(val date: String) : LogItem()
        data class Log(val log: MedicationLog) : LogItem()
    }


    /** DiffUtil Callback */
    private class LogItemDiffCallback(
        private val oldList: List<LogItem>,
        private val newList: List<LogItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            return when {
                oldItem is LogItem.Header && newItem is LogItem.Header ->
                    oldItem.date == newItem.date
                oldItem is LogItem.Log && newItem is LogItem.Log ->
                    oldItem.log.id == newItem.log.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}