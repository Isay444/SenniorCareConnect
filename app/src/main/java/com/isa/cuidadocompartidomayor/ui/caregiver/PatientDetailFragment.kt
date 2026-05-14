package com.isa.cuidadocompartidomayor.ui.caregiver

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.Relationship
import com.isa.cuidadocompartidomayor.databinding.FragmentPatientDetailBinding
import com.isa.cuidadocompartidomayor.ui.caregiver.viewmodel.ConnectionViewModel
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class PatientDetailFragment : Fragment() {

    private var _binding: FragmentPatientDetailBinding? = null
    private val binding get() = _binding!!
    private val connectionViewModel: ConnectionViewModel by viewModels()
    private var currentRelationship: Relationship? = null

    // Obtener argumentos SIN SafeArgs
    private val relationshipId: String by lazy {
        arguments?.getString("relationshipId") ?: ""
    }

    companion object {
        private const val TAG = "PatientDetailFragment"
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        private val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupObservers()

        // Cargar detalles del paciente
        loadPatientDetails()

        Log.d(TAG, "✅ PatientDetailFragment inicializado para relación: $relationshipId")
    }

    /**
     * Configura los eventos de click
     */
    private fun setupClickListeners() {
        // Botón volver
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        Log.d(TAG, "Click listeners configurados")
    }

    /**
     * Configura los observadores del ViewModel
     */
    private fun setupObservers() {
        // Lista de relaciones
        connectionViewModel.caregiverRelationships.observe(viewLifecycleOwner) { relationships ->
            val relationship = relationships.find { it.id == relationshipId }
            if (relationship != null) {
                currentRelationship = relationship
                displayPatientDetails(relationship)
            } else {
                showPatientNotFound()
            }
        }

        // Estado de carga
        connectionViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.scrollContent.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        // Mensajes de error
        connectionViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), "Error: $it", Toast.LENGTH_LONG).show()
                connectionViewModel.clearErrorMessage()
            }
        }

        Log.d(TAG, "Observadores configurados")
    }

    /**
     * Carga los detalles del paciente
     */
    private fun loadPatientDetails() {
        connectionViewModel.loadCaregiverRelationships()
    }

    /**
     * Muestra los detalles del paciente en la interfaz
     */
    private fun displayPatientDetails(relationship: Relationship) {
        with(binding) {
            // Foto de perfil
            Glide.with(requireContext())
                .load(relationship.elderlyProfileImageUrl)
                .placeholder(R.drawable.ic_avatar)
                .error(R.drawable.ic_avatar)
                .circleCrop()
                .into(ivPatientAvatar)

            // Información básica
            tvPatientName.text = relationship.elderlyName
            tvPatientEmail.text = relationship.elderlyEmail

            // Estado de conexión
            updateConnectionStatus(relationship.status)

            // Fechas
            val createdDate = Date(relationship.createdAt)
            tvConnectionDate.text = dateFormat.format(createdDate)
            tvCreatedDate.text = dateOnlyFormat.format(createdDate)

            // Rol
            tvRole.text = when (relationship.role) {
                "primary" -> "Cuidador Primario"
                "secondary" -> "Cuidador Primario"
                else -> "Cuidador"
            }

            // Notas
            if (relationship.notes.isNotEmpty()) {
                tvNotes.text = relationship.notes
                layoutNotes.visibility = View.VISIBLE
            } else {
                layoutNotes.visibility = View.GONE
            }
        }

        Log.d(TAG, "Detalles mostrados para: ${relationship.elderlyName}")
    }

    /**
     * Actualiza el estado visual de la conexión
     */
    private fun updateConnectionStatus(status: String) {
        val context = requireContext()

        when (status) {
            "active" -> {
                binding.tvStatus.text = "Conexión Activa"
                binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.white_bone))
                binding.ivStatusIcon.setImageResource(android.R.drawable.ic_menu_mylocation)
                binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                binding.cardStatus.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_light))
            }
            "pending" -> {
                binding.tvStatus.text = "Conexión Pendiente"
                binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.white_bone))
                binding.ivStatusIcon.setImageResource(android.R.drawable.ic_menu_recent_history)
                binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
                binding.cardStatus.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_orange_light))
            }
            "rejected" -> {
                binding.tvStatus.text = "Conexión Rechazada"
                binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.white_bone))
                binding.ivStatusIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                binding.cardStatus.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_light))
            }
            else -> {
                binding.tvStatus.text = "Estado Desconocido"
                binding.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                binding.ivStatusIcon.setImageResource(android.R.drawable.ic_menu_help)
                binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray))
                binding.cardStatus.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.background_light))
            }
        }
    }
    /**
     * Muestra mensaje cuando no se encuentra el paciente
     */
    private fun showPatientNotFound() {
        binding.scrollContent.visibility = View.GONE
        Toast.makeText(requireContext(), "Persona no encontrada", Toast.LENGTH_LONG).show()
        findNavController().navigateUp()
        Log.w(TAG, "Persona Mayor no encontrada para ID: $relationshipId")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "Vista destruida - recursos limpiados")
    }
}
