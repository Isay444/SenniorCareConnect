package com.isa.cuidadocompartidomayor.ui.elderly

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.AgendaEvent
import com.isa.cuidadocompartidomayor.data.model.MedicationLog
import com.isa.cuidadocompartidomayor.databinding.FragmentElderlyHomeBinding
import com.isa.cuidadocompartidomayor.ui.agenda.dialog.EventDetailsBottomSheet
import com.isa.cuidadocompartidomayor.ui.elderly.adapter.ElderlyAppointmentAdapter
import com.isa.cuidadocompartidomayor.ui.elderly.viewmodel.ElderlyHomeViewModel
import com.isa.cuidadocompartidomayor.ui.elderly.viewmodel.ElderlyMedicationViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ElderlyHomeFragment : Fragment() {
    private var _binding: FragmentElderlyHomeBinding? = null
    private val binding get() = _binding!!

    // ViewModels
    private val medicationViewModel: ElderlyMedicationViewModel by viewModels()
    private val homeViewModel: ElderlyHomeViewModel by viewModels()

    // Adapter para citas
    private lateinit var appointmentsAdapter: ElderlyAppointmentAdapter

    // Firebase
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "ElderlyHomeFragment"
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentElderlyHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAppointmentsRecyclerView()
        setupObservers()
        setupClickListeners()
        loadUserData()

        Log.d(TAG, "✅ ElderlyHomeFragment inicializado correctamente")
    }

    /**
     * : Configura el RecyclerView de citas médicas
     */
    private fun setupAppointmentsRecyclerView() {
        appointmentsAdapter = ElderlyAppointmentAdapter { appointment ->
            openAppointmentDetails(appointment)
        }

        binding.recyclerViewAppointments.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = appointmentsAdapter
        }
    }

    /** Configura los observadores del ViewModel */
    private fun setupObservers() {
        // Observar próximo medicamento
        medicationViewModel.nextMedication.observe(viewLifecycleOwner) { medicationLog ->
            if (medicationLog != null) {
                updateMedicationCard(medicationLog)
                showMedicationCard()
            } else {
                showEmptyState()
            }
        }

        // Observar estado de carga de medicamentos
        medicationViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnTookMedication.isEnabled = !isLoading
        }

        // Observar mensajes de éxito
        medicationViewModel.actionSuccess.observe(viewLifecycleOwner) { message ->
            message?.let {
                showSuccessFeedback(it)
                medicationViewModel.clearMessages()
            }
        }

        // Observar mensajes de error
        medicationViewModel.actionError.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                medicationViewModel.clearMessages()
            }
        }

        // : Observar citas médicas del día
        homeViewModel.todayAppointments.observe(viewLifecycleOwner) { appointments ->
            binding.apply {
                if (appointments.isEmpty()) {
                    // Sin citas
                    layoutNoAppointments.visibility = View.VISIBLE
                    recyclerViewAppointments.visibility = View.GONE
                    tvAppointmentsCount.visibility = View.GONE
                } else {
                    // Hay citas
                    layoutNoAppointments.visibility = View.GONE
                    recyclerViewAppointments.visibility = View.VISIBLE
                    tvAppointmentsCount.visibility = View.VISIBLE
                    tvAppointmentsCount.text = appointments.size.toString()
                    appointmentsAdapter.submitList(appointments)
                }
            }
        }
    }

    /** Configura los eventos de click */
    private fun setupClickListeners() {
        // Botón "LO TOMÉ" gigante
        binding.btnTookMedication.setOnClickListener {
            confirmMedicationTaken()
        }

        /* Click en tarjeta de medicamento → Mostrar información completa*/
        binding.cardMedicine.setOnClickListener {
            medicationViewModel.nextMedication.value?.let { medication ->
                showMedicationDetailsDialog(medication)
            }
        }
    }

    /**
     * : Carga datos del usuario y sus citas médicas
     */
    private fun loadUserData() {
        val currentUser = auth.currentUser ?: run {
            Log.e(TAG, "❌ Usuario no autenticado")
            return
        }

        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userName = document.getString("name") ?: "Usuario"
                    binding.tvUserName.text = "Hola, $userName"

                    // Cargar próximos medicamentos
                    medicationViewModel.loadNextMedication()

                    // ✅ Cargar citas médicas del día
                    homeViewModel.loadTodayAppointments(currentUser.uid)

                    Log.d(TAG, "✅ Datos del usuario cargados: $userName")
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "❌ Error cargando datos del usuario", exception)
                Toast.makeText(
                    requireContext(),
                    "Error al cargar datos del usuario",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    /** Actualiza la tarjeta de medicamento con datos reales */
    private fun updateMedicationCard(medicationLog: MedicationLog) {
        binding.apply {
            tvMedicationName.text = medicationLog.medicationName
            tvDosage.text = medicationLog.dosage
            tvMedicationTime.text = timeFormat.format(medicationLog.scheduledTime)
            tvMedicationFrecuency.text = medicationLog.frequency
        }
        Log.d(TAG, "Tarjeta actualizada con: ${medicationLog.medicationName}")
    }

    /** Muestra la tarjeta de medicamento */
    private fun showMedicationCard() {
        binding.cardMedicine.visibility = View.VISIBLE
        binding.layoutEmptyState.visibility = View.GONE
    }

    /** ✅ Muestra el estado vacío cuando no hay medicamentos pendientes */
    private fun showEmptyState() {
        binding.cardMedicine.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.VISIBLE
        Log.d(TAG, "Estado vacío mostrado - No hay medicamentos pendientes")
    }

    /** Muestra un diálogo con la información completa del medicamento */
    private fun showMedicationDetailsDialog(medicationLog: MedicationLog) {
        val message = buildString {
            append("💊 Medicamento: ${medicationLog.medicationName}\n\n")
            append("📏 Dosis: ${medicationLog.dosage}\n\n")
            append("🕒 Hora programada: ${timeFormat.format(medicationLog.scheduledTime)}\n\n")
            append("📅 Frecuencia: ${medicationLog.frequency}\n\n")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Información del Medicamento")
            .setMessage(message)
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /** Confirma que se tomó el medicamento */
    private fun confirmMedicationTaken() {
        val medication = medicationViewModel.nextMedication.value

        if (medication != null) {
            medicationViewModel.confirmMedicationTaken(medication)
        } else {
            Toast.makeText(
                requireContext(),
                "No hay medicamento para confirmar",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /** Muestra feedback visual de confirmación exitosa */
    private fun showSuccessFeedback(message: String) {
        binding.btnTookMedication.apply {
            isEnabled = false
            text = "CONFIRMADO"
            setBackgroundColor(resources.getColor(R.color.green_one, null))
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_check_circle)
        }

        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

        // Restaurar botón después de 2 segundos
        view?.postDelayed({
            binding.btnTookMedication.apply {
                isEnabled = true
                text = "LO TOMÉ"
                backgroundTintList = ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.green_one
                )
            }
        }, 2000)
    }

    /**
     * : Abre el BottomSheet de detalles de la cita
     */
    private fun openAppointmentDetails(appointment: AgendaEvent.MedicalAppointment) {
        val detailsSheet = EventDetailsBottomSheet.newInstance(appointment)
        detailsSheet.show(childFragmentManager, "AppointmentDetails")
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos cuando la pantalla vuelve a ser visible
        loadUserData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "Vista destruida - recursos limpiados")
    }
}
