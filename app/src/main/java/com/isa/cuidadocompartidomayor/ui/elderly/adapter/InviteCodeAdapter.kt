package com.isa.cuidadocompartidomayor.ui.elderly.adapter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.InviteCode
import com.isa.cuidadocompartidomayor.databinding.ItemInviteCodeBinding
import java.text.SimpleDateFormat
import java.util.*
class InviteCodeAdapter(
    private val onShareClick: (InviteCode) -> Unit
) : RecyclerView.Adapter<InviteCodeAdapter.InviteCodeViewHolder>() {

    private var codes = listOf<InviteCode>()

    companion object {
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }

    inner class InviteCodeViewHolder(private val binding: ItemInviteCodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(inviteCode: InviteCode) {
            binding.apply {
                // Código formateado
                tvCode.text = inviteCode.getFormattedCode()

                // Fecha de creación
                val createdDate = Date(inviteCode.createdAt)
                tvCreatedDate.text = "Creado: ${dateFormat.format(createdDate)}"

                // Estado del código
                when {
                    inviteCode.usedAt != null -> {
                        tvStatus.text = "✅ USADO"
                        tvStatus.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.text_tertiary)
                        )
                        cardCode.alpha = 0.7f
                        btnShare.isEnabled = false
                        btnShare.text = "USADO"
                    }
                    inviteCode.isExpired() -> {
                        tvStatus.text = "⏰ EXPIRADO"
                        tvStatus.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.text_tertiary)
                        )
                        cardCode.alpha = 0.7f
                        btnShare.isEnabled = false
                        btnShare.text = "EXPIRADO"
                    }
                    else -> {
                        tvStatus.text = "🔴 INACTIVO"
                        tvStatus.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.text_tertiary)
                        )
                        cardCode.alpha = 0.7f
                        btnShare.isEnabled = true
                        btnShare.text = "COMPARTIR"
                    }
                }

                // Click listener para compartir
                btnShare.setOnClickListener {
                    if (btnShare.isEnabled) {
                        onShareClick(inviteCode)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteCodeViewHolder {
        val binding = ItemInviteCodeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InviteCodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InviteCodeViewHolder, position: Int) {
        holder.bind(codes[position])
    }

    override fun getItemCount(): Int = codes.size

    fun updateCodes(newCodes: List<InviteCode>) {
        val diffCallback = InviteCodeDiffCallback(codes, newCodes)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        codes = newCodes
        diffResult.dispatchUpdatesTo(this)
    }

    private class InviteCodeDiffCallback(
        private val oldList: List<InviteCode>,
        private val newList: List<InviteCode>
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