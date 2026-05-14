package com.isa.cuidadocompartidomayor.ui.elderly.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.CaregiverRequest
import com.isa.cuidadocompartidomayor.databinding.ItemCaregiverRequestBinding

class CaregiverRequestAdapter(
    private val onApproveClick: ((CaregiverRequest) -> Unit)? = null,
    private val onRejectClick: ((CaregiverRequest) -> Unit)? = null,
    private val onRemoveClick: ((CaregiverRequest) -> Unit)? = null,
    private val isApprovedList: Boolean = false
) : RecyclerView.Adapter<CaregiverRequestAdapter.CaregiverRequestViewHolder>() {

    private var requests = listOf<CaregiverRequest>()
    private var loadingRequestIds = mutableSetOf<String>()

    inner class CaregiverRequestViewHolder(private val binding: ItemCaregiverRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(request: CaregiverRequest) {
            binding.apply {
                // Cargar imagen con Glide
                Glide.with(root.context)
                    .load(request.caregiverProfileImageUrl)
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .circleCrop()
                    .into(ivCaregiverAvatar)

                // Información básica del cuidador
                tvCaregiverName.text = request.caregiverName
                tvCaregiverEmail.text = request.caregiverEmail
                tvRequestTime.text = request.getTimeAgo()

                // Mensaje del cuidador (si lo hay)
                if (request.caregiverMessage.isNotBlank()) {
                    tvCaregiverMessage.text = "\"${request.caregiverMessage}\""
                    tvCaregiverMessage.visibility = View.VISIBLE
                    labelMessage.visibility = View.GONE ///Cambiar porque por ahora no hay posibilidad de editar el mensaje
                } else {
                    tvCaregiverMessage.visibility = View.GONE
                    labelMessage.visibility = View.GONE
                }

                // Código de invitación usado (si está disponible)
                if (!request.inviteCodeUsed.isNullOrBlank()) {
                    tvInviteCodeUsed.text = "Usó código: ${request.inviteCodeUsed}"
                    tvInviteCodeUsed.visibility = View.VISIBLE
                } else {
                    tvInviteCodeUsed.visibility = View.GONE
                }

                // Configurar vista según el tipo de lista
                if (isApprovedList) {
                    setupApprovedView(request)
                } else {
                    setupPendingView(request)
                }

                // Estado de loading
                val isLoading = loadingRequestIds.contains(request.id)
                updateLoadingState(isLoading)
            }
        }

        private fun setupPendingView(request: CaregiverRequest) {
            binding.apply {
                // Mostrar botones de acción para solicitudes pendientes
                layoutActionButtons.visibility = View.VISIBLE
                layoutApprovedInfo.visibility = View.GONE

                // Configurar botón APROBAR
                btnApprove.apply {
                    text = "APROBAR"
                    backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.status_on_time)
                    setOnClickListener {
                        onApproveClick?.invoke(request)
                    }
                }

                // Configurar botón RECHAZAR
                btnReject.apply {
                    text = "RECHAZAR"
                    backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.elderly_emergency)
                    setOnClickListener {
                        onRejectClick?.invoke(request)
                    }
                }

                // Estilo de tarjeta pendiente
                cardRequest.strokeColor = ContextCompat.getColor(itemView.context, R.color.warning)
                cardRequest.strokeWidth = 4
            }
        }

        private fun setupApprovedView(request: CaregiverRequest) {
            binding.apply {
                // Ocultar botones de acción para cuidadores aprobados
                layoutActionButtons.visibility = View.GONE
                layoutApprovedInfo.visibility = View.VISIBLE

                // Información de aprobación
                tvApprovedDate.text = "Conectado: ${request.getFormattedCreatedDate()}"

                // Configurar botón REMOVER
                btnRemoveCaregiver.apply {
                    text = "REMOVER"
                    backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.elderly_emergency)
                    setOnClickListener {
                        onRemoveClick?.invoke(request)
                    }
                }

                // Estilo de tarjeta aprobada
                cardRequest.strokeColor = ContextCompat.getColor(itemView.context, R.color.status_on_time)
                cardRequest.strokeWidth = 4
            }
        }

        private fun updateLoadingState(isLoading: Boolean) {
            binding.apply {
                if (isLoading) {
                    // Mostrar loading y deshabilitar botones
                    progressBarItem.visibility = View.VISIBLE

                    if (!isApprovedList) {
                        btnApprove.isEnabled = false
                        btnReject.isEnabled = false
                        btnApprove.text = "⏳ PROCESANDO..."
                        btnReject.text = "⏳ PROCESANDO..."
                    } else {
                        btnRemoveCaregiver.isEnabled = false
                        btnRemoveCaregiver.text = "⏳ PROCESANDO..."
                    }
                } else {
                    // Ocultar loading y restaurar botones
                    progressBarItem.visibility = View.GONE

                    if (!isApprovedList) {
                        btnApprove.isEnabled = true
                        btnReject.isEnabled = true
                        btnApprove.text = "APROBAR"
                        btnReject.text = "RECHAZAR"
                    } else {
                        btnRemoveCaregiver.isEnabled = true
                        btnRemoveCaregiver.text = "REMOVER"
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CaregiverRequestViewHolder {
        val binding = ItemCaregiverRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CaregiverRequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CaregiverRequestViewHolder, position: Int) {
        holder.bind(requests[position])
    }

    override fun getItemCount(): Int = requests.size

    /**
     * Actualiza la lista de solicitudes con DiffUtil
     */
    fun updateRequests(newRequests: List<CaregiverRequest>) {
        val diffCallback = CaregiverRequestDiffCallback(requests, newRequests)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        requests = newRequests
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * Marca una solicitud como en proceso de carga
     */
    fun setLoading(requestId: String, isLoading: Boolean) {
        if (isLoading) {
            loadingRequestIds.add(requestId)
        } else {
            loadingRequestIds.remove(requestId)
        }

        // Encontrar y actualizar el item específico
        val position = requests.indexOfFirst { it.id == requestId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    /**
     * Obtiene una solicitud por posición
     */
    fun getRequestAt(position: Int): CaregiverRequest? {
        return if (position in requests.indices) requests[position] else null
    }

    /**
     * DiffCallback para actualizaciones eficientes
     */
    private class CaregiverRequestDiffCallback(
        private val oldList: List<CaregiverRequest>,
        private val newList: List<CaregiverRequest>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            return oldItem.status == newItem.status &&
                    oldItem.caregiverName == newItem.caregiverName &&
                    oldItem.caregiverEmail == newItem.caregiverEmail &&
                    oldItem.caregiverMessage == newItem.caregiverMessage &&
                    oldItem.updatedAt == newItem.updatedAt
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            if (oldItem.status != newItem.status) {
                return "status_changed"
            }

            return super.getChangePayload(oldItemPosition, newItemPosition)
        }
    }
}
