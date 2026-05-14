package com.isa.cuidadocompartidomayor.ui.caregiver

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.LogStatus
import com.isa.cuidadocompartidomayor.data.model.Medication
import com.isa.cuidadocompartidomayor.data.model.MedicationLog
import com.isa.cuidadocompartidomayor.data.repository.EmergencyRepository
import com.isa.cuidadocompartidomayor.databinding.FragmentCaregiverHomeBinding
import com.isa.cuidadocompartidomayor.ui.caregiver.adapter.EmergencyAlertAdapter
import com.isa.cuidadocompartidomayor.ui.medications.adapter.UpcomingMedicationAdapter
import com.isa.cuidadocompartidomayor.ui.medications.viewmodel.MedicationViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.content.res.ColorStateList
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.material.chip.Chip
import com.isa.cuidadocompartidomayor.data.model.*
import com.isa.cuidadocompartidomayor.data.repository.DiaryRepository
import com.isa.cuidadocompartidomayor.ui.agenda.adapter.DashboardEventAdapter
import com.isa.cuidadocompartidomayor.ui.agenda.dialog.ViewAllEventsBottomSheet
import com.isa.cuidadocompartidomayor.ui.agenda.viewmodel.AgendaViewModel

class CaregiverHomeFragment : Fragment() {

    private var _binding: FragmentCaregiverHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Repositories
    private val emergencyRepository = EmergencyRepository()
    private val diaryRepository = DiaryRepository() //  NUEVO

    //View Models
    private val medicationViewModel: MedicationViewModel by activityViewModels()

    //Adapters
    private lateinit var upcomingMedicationsAdapter: UpcomingMedicationAdapter
    private var emergencyAlertAdapter: EmergencyAlertAdapter? = null

    //Listeners
    private var relationshipsListener: ListenerRegistration? = null
    private var emergencyAlertRunnable: Runnable? = null
    private var emergencyAlertsListener: ListenerRegistration? = null
    private var elderlySpinnerListener: ListenerRegistration? = null

    // State
    private var hasLoadedInitialData = false

    // Lista de adultos mayores y selección actual
    private var elderlyRelationships = mutableListOf<ElderlyRelationshipItem>()
    private var selectedElderlyId: String? = null

    //Eventos tareas y citas medicas
    private lateinit var upcomingEventsAdapter: DashboardEventAdapter
    private val agendaViewModel: AgendaViewModel by activityViewModels()


    companion object {
        private const val TAG = "CaregiverHomeFragment"
    }

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View {
        _binding = FragmentCaregiverHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupUI()
        setupClickListeners()
        setupEmergencyAlerts()
        setupElderlySpinner()
        setupUpcomingMedications()
        setupUpcomingEvents()

        // Observar PRIMERO, cargar DESPUÉS
        observeViewModel()

        // ✅ Solo cargar una vez
        if (!hasLoadedInitialData) {
            loadCaregiverData()

            // ✅ Dar tiempo al RecyclerView para inicializar
            view.post {
                loadUpcomingMedications()
            }
            hasLoadedInitialData = true
        }

        Log.d(TAG, "✅ CaregiverHomeFragment inicializado correctamente")
    }

    private fun setupUpcomingEvents() {
        upcomingEventsAdapter = DashboardEventAdapter { event ->
            // ✅ Abrir detalles del evento
            val detailsSheet = com.isa.cuidadocompartidomayor.ui.agenda.dialog.EventDetailsBottomSheet.newInstance(event)
            detailsSheet.show(childFragmentManager, "EventDetailsBottomSheet")
        }

        binding.rvUpcomingEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = upcomingEventsAdapter
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
        }

        // Botón "Ver todas"
        binding.btnViewAllEvents.setOnClickListener {
            val bottomSheet = ViewAllEventsBottomSheet.newInstance()
            bottomSheet.show(childFragmentManager, "ViewAllEventsBottomSheet")
        }

        // Observar próximos eventos
        agendaViewModel.upcomingEvents.observe(viewLifecycleOwner) { events ->
            upcomingEventsAdapter.submitList(events)

            if (events.isEmpty()) {
                binding.emptyStateEvents.visibility = View.VISIBLE
                binding.rvUpcomingEvents.visibility = View.GONE
            } else {
                binding.emptyStateEvents.visibility = View.GONE
                binding.rvUpcomingEvents.visibility = View.VISIBLE
            }
        }

        // Cargar eventos
        agendaViewModel.loadUpcomingEventsForDashboard(4)
    }

    private fun setupEmergencyAlerts() {
        Log.d(TAG, "🚨 Configurando sistema de alertas de emergencia")

        emergencyAlertAdapter = EmergencyAlertAdapter { alertId ->
            dismissEmergencyAlert(alertId)
        }

        binding.rvEmergencyAlerts.apply {
            adapter = emergencyAlertAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        Log.d(TAG, "✅ RecyclerView de alertas configurado")

        /* Cargar alertas cada 30 s*/
        loadEmergencyAlerts()

        // Guardar referencia del Runnable
        emergencyAlertRunnable = object : Runnable {
            override fun run() {
                // ✅ Verificar que la vista aún existe
                if (_binding != null && isAdded) {
                    Log.d(TAG, "⏰ Actualización periódica de alertas")
                    loadEmergencyAlerts()
                    view?.postDelayed(this, 30000) // Cada 30 segundos
                } else {
                    Log.d(TAG, "⚠️ Vista destruida - cancelando actualizaciones")
                }
            }
        }

        // Programar primera actualización en 30 segundos
        view?.postDelayed(emergencyAlertRunnable!!, 30000)

    }

    private fun loadEmergencyAlerts() {

        /*Verificar que la vista existe ANTES de usar viewLifecycleOwner*/
        if (_binding == null || !isAdded) {
            Log.w(TAG, "⚠️ Vista no disponible - cancelando carga de alertas")
            return
        }

        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            Log.w(TAG, "⚠️ Usuario no autenticado - no se pueden cargar alertas")
            return
        }

        Log.d(TAG, "🔍 Buscando alertas de emergencia para: $currentUserId")

        viewLifecycleOwner.lifecycleScope.launch {
            val result = emergencyRepository.getCaregiverEmergencyNotifications(currentUserId)

            result.onSuccess { notifications ->
                // ✅ Verificar de nuevo que binding existe
                if (_binding == null) {
                    Log.w(TAG, "⚠️ Binding se volvió null durante la carga")
                    return@onSuccess
                }

                Log.d(TAG, "✅ Alertas recibidas: ${notifications.size}")

                notifications.forEach { notification ->
                    Log.d(TAG, "🚨 Alerta: ${notification["elderlyName"]} - ${notification["body"]}")
                }

                emergencyAlertAdapter?.submitList(notifications)
                if (notifications.isNotEmpty()) {
                    val vibrator = ContextCompat.getSystemService(requireContext(), Vibrator::class.java)
                    vibrator?.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
                }

                // Mostrar/ocultar vista según haya alertas
                binding.rvEmergencyAlerts.visibility = if (notifications.isEmpty()) {
                    Log.d(TAG, "❌ Sin alertas - ocultando RecyclerView")
                    View.GONE
                } else {
                    Log.d(TAG, "✅ ${notifications.size} alertas - mostrando RecyclerView")
                    View.VISIBLE
                }
            }.onFailure { error ->
                Log.e(TAG, "❌ Error cargando alertas: ${error.message}", error)
            }
        }
    }

    private fun dismissEmergencyAlert(alertId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            emergencyRepository.markNotificationAsRead(alertId)
            loadEmergencyAlerts() // Recargar lista

            Toast.makeText(
                requireContext(),
                "Alerta marcada como leída",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupUI() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userName = currentUser.displayName?.takeIf { it.isNotEmpty() }
                ?: currentUser.email?.substringBefore("@")
                ?: "Cuidador"

            binding.tvWelcome.text = "¡Hola, $userName!"
            Log.d(TAG, "UI configurada para usuario: $userName")
        } else {
            binding.tvWelcome.text = "¡Hola, Cuidador!"
            Log.w(TAG, "Usuario no autenticado")
        }
    }

    private fun setupUpcomingMedications() {
        upcomingMedicationsAdapter = UpcomingMedicationAdapter(
            onConfirmClick = { medication, log ->
                confirmMedicationTaken(medication, log)
            },
            onSkipClick = { medication, log ->
                skipMedication(medication, log)
            },
            onPendingClick = { medication, log ->
                setPendingMedication(medication, log)
            },
            onInfoClick = { medication, log ->
                showMedicationDetails(medication, log)
            }
        )

        binding.rvUpcomingMedications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = upcomingMedicationsAdapter
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
        }

        Log.d(TAG, "✅ RecyclerView de medicamentos configurado")
    }

    private fun setupClickListeners() {
        binding.cardConnectElderly.setOnClickListener {
            onConnectElderlyClicked()
        }

        binding.cardMyPatients.setOnClickListener {
            onViewPatientsClicked()
        }

        Log.d(TAG, "Click listeners configurados")
    }

    private fun observeViewModel() {
        medicationViewModel.upcomingLogs.observe(viewLifecycleOwner) { logs ->
            if (_binding == null){
                Log.w(TAG,"Binding es null al observar upcomingMedications")
                return@observe
            }
            Log.d(TAG,"Observer medicamentos recibidos count=${logs.size}")

            if (logs.isEmpty()) {
                binding.rvUpcomingMedications.visibility = View.GONE
                binding.tvEmptyMedications.visibility = View.VISIBLE
                binding.progressMedications.visibility = View.GONE
            } else {
                binding.rvUpcomingMedications.visibility = View.VISIBLE
                binding.tvEmptyMedications.visibility = View.GONE
                binding.progressMedications.visibility = View.GONE

                Log.d(TAG, "Cargando estados de ${logs.size} medicamentos")
                loadLogsAsMedications(logs)
                Log.d(TAG, "✅ Próximos medicamentos actualizados: ${logs.size} items")
            }
        }

        medicationViewModel.isLoadingUpcoming.observe(viewLifecycleOwner) { isLoading ->
            if (_binding == null) return@observe

            if (isLoading) {
                binding.progressMedications.visibility = View.VISIBLE
                binding.rvUpcomingMedications.visibility = View.GONE
                binding.tvEmptyMedications.visibility = View.GONE
            }
        }

        medicationViewModel.upcomingError.observe(viewLifecycleOwner) { error ->
            if (_binding == null) return@observe

            error?.let {
                Toast.makeText(requireContext(), "Error: $it", Toast.LENGTH_SHORT).show()
                binding.progressMedications.visibility = View.GONE
                binding.tvEmptyMedications.visibility = View.VISIBLE
                Log.e(TAG, "❌ Error: $it")
            }
        }

        medicationViewModel.medicationActionError.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(requireContext(), "❌ $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadUpcomingMedications() {
        val caregiverId = auth.currentUser?.uid

        if (caregiverId != null) {
            medicationViewModel.loadUpcomingLogsForCaregiver(caregiverId)
            Log.d(TAG, "📥 Cargando medicamentos para caregiver: $caregiverId")
        } else {
            Log.w(TAG, "⚠️ No hay caregiver autenticado")
        }
    }

    private fun confirmMedicationTaken(medication: Medication, log: MedicationLog) {
        val caregiverId = auth.currentUser?.uid
        if (caregiverId == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Formatear la hora del log específico
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val scheduledTimeString = timeFormat.format(java.util.Date(log.scheduledTime))

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar toma de medicamento")
            .setMessage(
                """
                ¿Confirmas que ${medication.elderlyName} tomó el medicamento?
                
                💊 ${medication.name}
                📏 ${medication.dosage}
                🕐 ${scheduledTimeString}
                """.trimIndent()
            )
            .setPositiveButton("✓ Sí, confirmar") { _, _ ->
                medicationViewModel.confirmMedicationTaken(
                    medication = medication,
                    log = log,
                    confirmedBy = caregiverId
                )

                Toast.makeText(
                    requireContext(),
                    "✅ Medicamento confirmado exitosamente",
                    Toast.LENGTH_SHORT
                ).show()

                Log.d(TAG, "✅ Medicamento confirmado: ${medication.name} para ${medication.elderlyName}")

            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun skipMedication(medication: Medication, log: MedicationLog) {  // ✅ Recibir log
        val caregiverId = auth.currentUser?.uid
        if (caregiverId == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Formatear la hora del log específico
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val scheduledTimeString = timeFormat.format(java.util.Date(log.scheduledTime))

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Omitir medicamento")
            .setMessage(
                """
            ¿Confirmas que ${medication.elderlyName} omitió este medicamento?
            💊 ${medication.name}
            📏 ${medication.dosage}
            🕐 $scheduledTimeString
            ⚠️ Esta acción quedará registrada en el historial.
            """.trimIndent()
            )
            .setPositiveButton("⏭️ Sí, omitir") { _, _ ->
                medicationViewModel.skipMedication(
                    medication = medication,
                    log = log,  // ✅ Pasar log
                    skippedBy = caregiverId
                )
                Toast.makeText(
                    requireContext(),
                    "⏭️ Medicamento marcado como omitido",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showMedicationDetails(medication: Medication, log: MedicationLog) {
        lifecycleScope.launch {
            val status = LogStatus.valueOf(log.statusString)  // ✅ Usar el status del log
            val statusText = when (status) {
                LogStatus.ON_TIME -> "✅ A tiempo"
                LogStatus.LATE -> "⏰ Atrasado"
                LogStatus.MISSED -> "⏭️ Omitido"
                LogStatus.PENDING -> "⏳ Pendiente"
            }

            // ✅ Formatear la hora del log específico
            val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val scheduledTimeString = timeFormat.format(java.util.Date(log.scheduledTime))

            val details = """
                📋 Medicamento: ${medication.name}
                
                💊 Tipo: ${medication.medicationType.displayName}
                📏 Dosis: ${medication.dosage}
                🕐 Frecuencia: ${medication.getFrequencyText()}
                ⏰ Hora Programada: $scheduledTimeString    
                            
                📊 Estado actual: $statusText        
                        
                👤 Paciente: ${medication.elderlyName}
                👨‍⚕️ Registrado por: ${medication.caregiverName}
                📝 Instrucciones:  ${medication.instructions.ifEmpty { "Sin instrucciones adicionales" }} """.trimIndent()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Detalles del Medicamento")
                .setMessage(details)
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    private fun setPendingMedication(medication: Medication, log: MedicationLog) {  // ✅ Recibir log
        val caregiverId = auth.currentUser?.uid
        if (caregiverId == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Formatear la hora del log específico
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val scheduledTimeString = timeFormat.format(java.util.Date(log.scheduledTime))

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Revertir a pendiente")
            .setMessage(
                """
            ¿Deseas marcar este medicamento como pendiente nuevamente?
            💊 ${medication.name}
            📏 ${medication.dosage}
            🕐 $scheduledTimeString
            """.trimIndent()
            )
            .setPositiveButton("⏳ Sí, marcar pendiente") { _, _ ->
                medicationViewModel.setPendingMedication(
                    medication = medication,
                    log = log,  // ✅ Pasar log
                    changedBy = caregiverId
                )
                Toast.makeText(
                    requireContext(),
                    "⏳ Medicamento marcado como pendiente",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupElderlySpinner() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.w(TAG, "Usuario no autenticado")
            return
        }

        // ✅ Remover listener anterior si existe
        elderlySpinnerListener?.remove()

        // Listener en tiempo real para cargar adultos mayores
        firestore.collection("relationships")
            .whereEqualTo("caregiverId", currentUserId)
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                // ✅ CRÍTICO: Verificar que el Fragment aún existe
                if (_binding == null || !isAdded) {
                    Log.d(TAG, "⚠️ Fragment no disponible - cancelando actualización del spinner")
                    return@addSnapshotListener
                }
                if (error != null) {
                    Log.e(TAG, "Error cargando relaciones: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    Log.d(TAG, "No hay adultos mayores conectados")
                    showEmptyElderlyState()
                    return@addSnapshotListener
                }

                lifecycleScope.launch {
                    // ✅ Verificar OTRA VEZ antes de actualizar UI
                    if (_binding == null || !isAdded) {
                        Log.d(TAG, "⚠️ Fragment destruido durante la carga")
                        return@launch
                    }
                    // Cargar datos completos de cada adulto mayor
                    elderlyRelationships.clear()

                    snapshot.documents.forEach { doc ->
                        val elderlyId = doc.getString("elderlyId") ?: return@forEach
                        val elderlyName = doc.getString("elderlyName") ?: "Adulto Mayor"

                        elderlyRelationships.add(
                            ElderlyRelationshipItem(
                                id = elderlyId,
                                name = elderlyName
                            )
                        )
                    }

                    // ✅ Verificar ANTES de llamar setupSpinnerAdapter
                    if (elderlyRelationships.isNotEmpty() && _binding != null && isAdded) {
                        setupSpinnerAdapter()

                        // Seleccionar el primero por defecto si no hay selección
                        if (selectedElderlyId == null) {
                            selectedElderlyId = elderlyRelationships[0].id
                            loadDiaryDataForElderly(selectedElderlyId!!)
                        }
                    }

                    Log.d(TAG, "✅ ${elderlyRelationships.size} adultos mayores cargados")
                }
            }
    }

    private fun setupSpinnerAdapter() {
        // ✅ Verificación defensiva
        if (_binding == null || !isAdded) {
            Log.w(TAG, "⚠️ Fragment no disponible - no se puede configurar spinner")
            return
        }
        val elderlyNames = elderlyRelationships.map { it.name }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            elderlyNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerElderlySelection.adapter = adapter

        // Listener para cambios de selección
        binding.spinnerElderlySelection.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // ✅ Verificar bounds
                    if (position < 0 || position >= elderlyRelationships.size) {
                        Log.w(TAG, "⚠️ Posición inválida: $position")
                        return
                    }
                    val selectedElderly = elderlyRelationships[position]
                    selectedElderlyId = selectedElderly.id

                    Log.d(TAG, "Adulto mayor seleccionado: ${selectedElderly.name}")

                    // Cargar datos del diario para este adulto mayor
                    loadDiaryDataForElderly(selectedElderly.id)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // No hacer nada
                }
            }
    }

    private fun showEmptyElderlyState() {
        binding.tvVitalSignsTitle.text = "Últimas lecturas médicas"
        binding.tvBehaviorsTitle.text = "Últimos comportamientos registrados"

        // Limpiar todas las tarjetas
        clearVitalSignsCards()
        clearBehaviorChips()
    }

    private fun loadDiaryDataForElderly(elderlyId: String) {
        // ✅ Verificación defensiva
        if (_binding == null || !isAdded) {
            Log.w(TAG, "⚠️ Fragment no disponible - no se pueden cargar datos del diario")
            return
        }
        val elderlyName = elderlyRelationships.find { it.id == elderlyId }?.name
            ?: "Adulto Mayor"

        // Actualizar títulos con el nombre del adulto mayor
        binding.tvVitalSignsTitle.text = "Últimas lecturas médicas de $elderlyName"
        binding.tvBehaviorsTitle.text = "Últimos comportamientos de $elderlyName"

        viewLifecycleOwner.lifecycleScope.launch {
            // ✅ Verificar OTRA VEZ antes de llamadas asíncronas
            if (_binding == null || !isAdded) {
                Log.w(TAG, "⚠️ Fragment destruido durante la carga")
                return@launch
            }
            // Cargar último signo vital
            val vitalSignResult = diaryRepository.getLatestVitalSign(elderlyId)
            vitalSignResult.onSuccess { vitalSign ->
                if (_binding != null && isAdded) { // ✅ Verificar antes de actualizar UI
                    updateVitalSignsCards(vitalSign)
                }
            }.onFailure {
                Log.e(TAG, "Error cargando signos vitales: ${it.message}")
                clearVitalSignsCards()
            }

            // Cargar último estado de ánimo
            val moodEntryResult = diaryRepository.getLatestMoodEntry(elderlyId)
            moodEntryResult.onSuccess { moodEntry ->
                if (_binding != null && isAdded) { // ✅ Verificar antes de actualizar UI
                    updateBehaviorChips(moodEntry)
                }
            }.onFailure {
                Log.e(TAG, "Error cargando estado de ánimo: ${it.message}")
                clearBehaviorChips()
            }
        }
    }

    private fun updateVitalSignsCards(vitalSign: VitalSign?) {
        if (_binding == null) return

        if (vitalSign == null) {
            clearVitalSignsCards()
            return
        }

        // Actualizar valores
        binding.tvPulse.text = vitalSign.heartRate?.toString() ?: "--"
        binding.tvGlucose.text = vitalSign.glucose?.toString() ?: "--"
        binding.tvTemperature.text = vitalSign.temperature?.toString() ?: "--"
        binding.tvBloodPressure.text = if (vitalSign.systolicBP != null && vitalSign.diastolicBP != null) {
            "${vitalSign.systolicBP}/${vitalSign.diastolicBP}"
        } else {
            "--/--"
        }
        binding.tvOxygenSaturation.text = vitalSign.oxygenSaturation?.toString() ?: "--"
        binding.tvWeight.text = vitalSign.weight?.toString() ?: "--"

        // Actualizar colores según estado
        //updateCardColors(vitalSign)

        Log.d(TAG, "✅ Signos vitales actualizados en dashboard")
    }

    private fun clearVitalSignsCards() {
        if (_binding == null) return

        binding.tvPulse.text = "--"
        binding.tvGlucose.text = "--"
        binding.tvTemperature.text = "--"
        binding.tvBloodPressure.text = "--/--"
        binding.tvOxygenSaturation.text = "--"
        binding.tvWeight.text = "--"

        // Resetear colores a los por defecto
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.card_stroke)
        binding.cardPulse.strokeColor = defaultColor
        binding.cardGlucose.strokeColor = defaultColor
        binding.cardTemperature.strokeColor = defaultColor
        binding.cardBloodPressure.strokeColor = defaultColor
        binding.cardOxygenSaturation.strokeColor = defaultColor
        binding.cardWeight.strokeColor = defaultColor
    }

    // ========================================
    // Actualizar chips de comportamientos
    private fun updateBehaviorChips(moodEntry: MoodEntry?) {
        if (_binding == null) return

        binding.chipGroupBehaviors.removeAllViews()

        if (moodEntry == null || !moodEntry.hasAnyData()) {
            binding.tvEmptyBehaviors.visibility = View.VISIBLE
            binding.chipGroupBehaviors.visibility = View.GONE
            return
        }

        binding.tvEmptyBehaviors.visibility = View.GONE
        binding.chipGroupBehaviors.visibility = View.VISIBLE

        // Agregar chips de emociones (máximo 3)
        val emotions = moodEntry.getEmotionsList()
        emotions.take(3).forEach { emotion ->
            val chip = createChip("${emotion.emoji} ${emotion.displayName}")
            binding.chipGroupBehaviors.addView(chip)
        }

        // Agregar chip de nivel de energía si existe
        moodEntry.getEnergyLevelEnum()?.let { energyLevel ->
            val chip = createChip("${energyLevel.emoji} Energía: ${energyLevel.displayName}")
            binding.chipGroupBehaviors.addView(chip)
        }

        // Agregar chip de apetito si existe
        moodEntry.getAppetiteLevelEnum()?.let { appetiteLevel ->
            val chip = createChip("${appetiteLevel.emoji} Apetito: ${appetiteLevel.displayName}")
            binding.chipGroupBehaviors.addView(chip)
        }

        moodEntry.getFunctionalCapacityEnum()?.let { capacityLevel ->
            val chip = createChip("${capacityLevel.emoji} Capacidad Funcional: ${capacityLevel.displayName}")
            binding.chipGroupBehaviors.addView(chip)
        }

        Log.d(TAG, "✅ Comportamientos actualizados: ${emotions.size} emociones")
    }

    private fun createChip(text: String): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.white)
            )
            setTextColor(ContextCompat.getColor(context, R.color.black))
            isClickable = false
            isCheckable = false
        }
    }

    private fun clearBehaviorChips() {
        if (_binding == null) return

        binding.chipGroupBehaviors.removeAllViews()
        binding.tvEmptyBehaviors.visibility = View.VISIBLE
        binding.chipGroupBehaviors.visibility = View.GONE
    }

    private fun loadCaregiverData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "Usuario no autenticado - no se pueden cargar datos")
            return
        }

        relationshipsListener?.remove()

        relationshipsListener = firestore.collection("relationships")
            .whereEqualTo("caregiverId", currentUser.uid)
            .whereEqualTo("status", "active")
            .addSnapshotListener { documents, error ->
                if (_binding == null) {
                    Log.d(TAG, "⚠️ Binding es null, ignorando actualización")
                    return@addSnapshotListener
                }

                if (error != null) {
                    Log.e(TAG, "Error al cargar relaciones: ${error.message}")
                    return@addSnapshotListener
                }

                val activeRelationships = documents?.size() ?: 0
                Log.d(TAG, "✅ Relaciones activas cargadas: $activeRelationships")
            }
    }

    private fun onConnectElderlyClicked() {
        try {
            findNavController().navigate(R.id.connectElderlyFragment)
            Log.d(TAG, "Navegando a ConnectElderlyFragment")
        } catch (e: Exception) {
            Log.e(TAG, "Error al navegar a ConnectElderly", e)
            Toast.makeText(requireContext(), "Error al abrir conexión", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onViewPatientsClicked() {
        try {
            findNavController().navigate(R.id.patientListFragment)
            Log.d(TAG, "Navegando a PatientListFragment")
        } catch (e: Exception) {
            Log.e(TAG, "Error al navegar a PatientList", e)
            Toast.makeText(requireContext(), "Error al abrir lista de pacientes", Toast.LENGTH_SHORT).show()
        }
    }

    /** Convierte logs a medications con status para el adapter */
    private fun loadLogsAsMedications(logs: List<MedicationLog>) {
        if (logs.isEmpty()) {
            upcomingMedicationsAdapter.submitList(emptyList())
            Log.d(TAG, "Lista vacía, no hay logs para mostrar")
            return
        }

        lifecycleScope.launch {
            try {
                val medicationsWithStatus = logs.map { log ->
                    async {
                        // Obtener el medicamento completo desde Firebase
                        val medication = getMedicationFromLog(log)

                        if (medication != null) {
                            // ✅ Pasar el LOG completo al adapter
                            val status = LogStatus.valueOf(log.statusString)
                            UpcomingMedicationAdapter.MedicationWithStatus(
                                medication = medication,
                                log = log,
                                currentStatus = status
                            )
                        } else {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()

                Log.d(TAG, "Enviando ${medicationsWithStatus.size} meds con status al adaptador")
                upcomingMedicationsAdapter.submitList(medicationsWithStatus)

                Log.d(TAG, "Lista enviada al adaptador")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando logs: ${e.message}")
            }
        }
    }

    /** Obtiene el medicamento completo desde un log */
    private suspend fun getMedicationFromLog(log: MedicationLog): Medication? {
        return try {
            val medicationDoc = firestore.collection("medications")
                .document(log.medicationId)
                .get()
                .await()

            medicationDoc.toObject(Medication::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo medicamento: ${e.message}")
            null
        }
    }

    override fun onResume() {
        super.onResume()
        // Solo recargar si ya se inicializó
        if (hasLoadedInitialData) {
            loadUpcomingMedications()
            //  Recargar datos del diario si hay adulto mayor seleccionado
            selectedElderlyId?.let { elderlyId ->
                loadDiaryDataForElderly(elderlyId)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancelar el polling de alertas
        emergencyAlertRunnable?.let {
            view?.removeCallbacks(it)
        }
        emergencyAlertRunnable = null


        //NUEVO (Spinner para filtrar por adutlo mayor): Remover listener del spinner
        elderlySpinnerListener?.remove()
        elderlySpinnerListener = null

        relationshipsListener?.remove()
        relationshipsListener = null

        // : Remover listener de alertas
        emergencyAlertsListener?.remove()
        emergencyAlertsListener = null

        // ✅ Resetear flag al destruir
        hasLoadedInitialData = false

        _binding = null
        Log.d(TAG, "✅ CaregiverHomeFragment destruido, listeners removidos")
    }

        // Data class helper
        data class ElderlyRelationshipItem(
            val id: String,
            val name: String
        )
}
