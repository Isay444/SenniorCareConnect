package com.isa.cuidadocompartidomayor.ui.agenda.dialog

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.AgendaEvent
import com.isa.cuidadocompartidomayor.data.model.EventType
import com.isa.cuidadocompartidomayor.data.repository.AgendaRepository
import com.isa.cuidadocompartidomayor.databinding.BottomSheetAddEditEventBinding
import com.isa.cuidadocompartidomayor.data.model.ElderlyItem // Cambio: Antes import com.isa.cuidadocompartidomayor.ui.Agenda.Elderly
import com.isa.cuidadocompartidomayor.ui.agenda.viewmodel.AgendaViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class AddEditEventBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddEditEventBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AgendaViewModel by activityViewModels()
    private val repository = AgendaRepository()
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val elderlyList = mutableListOf<ElderlyItem>() // Cambio: Antes mutableListOf<Elderly>()
    private val caregiverList = mutableListOf<Caregiver>()

    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedElderlyId: String? = null
    private var selectedCaregiverId: String? = null
    private var selectedEventType: EventType = EventType.MEDICAL_APPOINTMENT

    // Variables para modo EDICIÓN
    private var isEditMode: Boolean = false
    private var eventToEdit: AgendaEvent? = null

    companion object {
        private const val TAG = "AddEditEventBottomSheet"
        private const val ARG_EVENT = "arg_event" // Warning: Property "ARG_EVENT" is never used

        // Factory method para CREAR
        fun newInstance(): AddEditEventBottomSheet { // Warning: Function "newInstance" is never used
            return AddEditEventBottomSheet()
        }

        // Factory method para EDITAR
        fun newInstanceForEdit(event: AgendaEvent): AddEditEventBottomSheet {
            return AddEditEventBottomSheet().apply {
                arguments = Bundle().apply {
                    // Convertir el evento a un formato serializable
                    putString("eventId", event.id)
                    putString("eventType", event.type.name)
                    putString("title", event.title)
                    putLong("date", event.date)
                    putString("elderlyId", event.elderlyId)
                    putString("elderlyName", event.elderlyName)
                    putString("notes", event.notes)

                    when (event) {
                        is AgendaEvent.MedicalAppointment -> {
                            putString("location", event.location)
                        }
                        is AgendaEvent.NormalTask -> {
                            putString("assignedTo", event.assignedTo)
                            putString("assignedToName", event.assignedToName)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddEditEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Detectar modo edición
        checkEditMode()

        setupEventTypeToggle()
        setupDateTimePickers()
        setupButtons()

        viewLifecycleOwner.lifecycleScope.launch {
            loadElderlyList()
            loadCaregiverList()

            // Pre-llenar datos en modo edición
            if (isEditMode) {
                prefillEventData()
            }
        }

        Log.d(TAG, "✅ AddEditEventBottomSheet inicializado (Modo: ${if (isEditMode) "EDITAR" else "CREAR"})")
    }

    // Detectar si estamos en modo edición
    private fun checkEditMode() {
        arguments?.let { args ->
            val eventId = args.getString("eventId")
            if (eventId != null) {
                isEditMode = true

                // Reconstruir el evento desde los arguments
                val eventTypeStr = args.getString("eventType") ?: return
                val eventType = EventType.valueOf(eventTypeStr)

                when (eventType) {
                    EventType.MEDICAL_APPOINTMENT -> {
                        eventToEdit = AgendaEvent.MedicalAppointment(
                            id = eventId,
                            title = args.getString("title") ?: "",
                            date = args.getLong("date"),
                            elderlyId = args.getString("elderlyId") ?: "",
                            elderlyName = args.getString("elderlyName") ?: "",
                            createdBy = "",
                            createdByName = "",
                            notes = args.getString("notes") ?: "",
                            location = args.getString("location") ?: "",
                            createdAt = 0L,
                            updatedAt = 0L
                        )
                    }
                    EventType.NORMAL_TASK -> {
                        eventToEdit = AgendaEvent.NormalTask(
                            id = eventId,
                            title = args.getString("title") ?: "",
                            date = args.getLong("date"),
                            elderlyId = args.getString("elderlyId") ?: "",
                            elderlyName = args.getString("elderlyName") ?: "",
                            createdBy = "",
                            createdByName = "",
                            notes = args.getString("notes") ?: "",
                            assignedTo = args.getString("assignedTo"),
                            assignedToName = args.getString("assignedToName"),
                            createdAt = 0L,
                            updatedAt = 0L
                        )
                    }
                }

                // Actualizar título del BottomSheet
                binding.tvTitle.text = "Editar Evento"
                binding.btnSave.text = "Actualizar"
            }
        }
    }

    // Pre-llenar datos del evento
    private fun prefillEventData() {
        eventToEdit?.let { event ->
            // Fecha
            selectedDate.timeInMillis = event.date
            updateDateDisplay()
            updateTimeDisplay()

            // Título
            binding.etTitle.setText(event.title)

            // Notas
            binding.etNotes.setText(event.notes)

            // Adulto mayor (se seleccionará cuando se cargue la lista)
            selectedElderlyId = event.elderlyId

            // Tipo de evento
            when (event) {
                is AgendaEvent.MedicalAppointment -> {
                    binding.radioCitaMedica.isChecked = true
                    selectedEventType = EventType.MEDICAL_APPOINTMENT
                    binding.etLocation.setText(event.location)
                }
                is AgendaEvent.NormalTask -> {
                    binding.radioTarea.isChecked = true
                    selectedEventType = EventType.NORMAL_TASK
                    selectedCaregiverId = event.assignedTo
                }
            }

            // ✅ IMPORTANTE: Deshabilitar cambio de tipo en modo edición
            binding.radioGroupEventType.isEnabled = false
            binding.radioCitaMedica.isEnabled = false
            binding.radioTarea.isEnabled = false

            // ✅ IMPORTANTE: Deshabilitar cambio de adulto mayor en modo edición
            binding.spinnerElderly.isEnabled = false
        }
    }

    private fun setupEventTypeToggle() {
        binding.radioGroupEventType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioCitaMedica -> {
                    selectedEventType = EventType.MEDICAL_APPOINTMENT
                    binding.layoutCitaMedica.visibility = View.VISIBLE
                    binding.layoutTarea.visibility = View.GONE
                }
                R.id.radioTarea -> {
                    selectedEventType = EventType.NORMAL_TASK
                    binding.layoutCitaMedica.visibility = View.GONE
                    binding.layoutTarea.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupDateTimePickers() {
        // Date Picker
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // Time Picker
        binding.etTime.setOnClickListener {
            showTimePicker()
        }

        // Set fecha y hora actual por defecto (solo si NO es modo edición)
        if (!isEditMode) {
            updateDateDisplay()
            updateTimeDisplay()
        }
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateDisplay()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )

        // ✅ Solo establecer fecha mínima si NO es modo edición
        if (!isEditMode) {
            datePicker.datePicker.minDate = System.currentTimeMillis()
        }

        datePicker.show()
    }

    private fun showTimePicker() {
        val timePicker = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDate.set(Calendar.MINUTE, minute)
                updateTimeDisplay()
            },
            selectedDate.get(Calendar.HOUR_OF_DAY),
            selectedDate.get(Calendar.MINUTE),
            false
        )
        timePicker.show()
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.etDate.setText(dateFormat.format(selectedDate.time))
    }

    private fun updateTimeDisplay() {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        binding.etTime.setText(timeFormat.format(selectedDate.time))
    }

    private suspend fun loadElderlyList() {
        try {
            val currentUser = auth.currentUser ?: return

            val snapshot = db.collection("relationships")
                .whereEqualTo("caregiverId", currentUser.uid)
                .whereEqualTo("status", "active")
                .get()
                .await()

            elderlyList.clear()

            for (doc in snapshot.documents) {
                val elderlyId = doc.getString("elderlyId") ?: continue
                val elderlyName = doc.getString("elderlyName") ?: "Adulto Mayor"

                elderlyList.add(ElderlyItem(elderlyId, elderlyName)) // Cambio: antes elderlyList.add(Elderly(elderlyId, elderlyName))
            }

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                elderlyList.map { it.name }
            )

            binding.spinnerElderly.setAdapter(adapter)

            // ✅ Seleccionar el adulto mayor correcto
            if (isEditMode && selectedElderlyId != null) {
                // En modo edición, seleccionar el adulto mayor del evento
                val elderlyIndex = elderlyList.indexOfFirst { it.id == selectedElderlyId }
                if (elderlyIndex != -1) {
                    binding.spinnerElderly.setText(elderlyList[elderlyIndex].name, false)
                }
            } else if (elderlyList.isNotEmpty()) {
                // En modo creación, seleccionar el primero
                binding.spinnerElderly.setText(elderlyList[0].name, false)
                selectedElderlyId = elderlyList[0].id
            }

            Log.d(TAG, "✅ ${elderlyList.size} adultos mayores cargados")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cargando adultos mayores: ${e.message}")
        }
    }

    private suspend fun loadCaregiverList() {
        try {
            val elderlyId = selectedElderlyId ?: return

            // ✅ Obtener TODOS los cuidadores del adulto mayor seleccionado
            val snapshot = db.collection("relationships")
                .whereEqualTo("elderlyId", elderlyId)
                .whereEqualTo("status", "active")
                .get()
                .await()

            caregiverList.clear()

            for (doc in snapshot.documents) {
                val caregiverId = doc.getString("caregiverId") ?: continue
                val caregiverName = doc.getString("caregiverName") ?: "Cuidador"

                caregiverList.add(Caregiver(caregiverId, caregiverName))
            }

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                caregiverList.map { it.name }
            )

            binding.spinnerCaregiver.setAdapter(adapter)

            // ✅ Seleccionar el cuidador correcto
            if (isEditMode && selectedCaregiverId != null) {
                val caregiverIndex = caregiverList.indexOfFirst { it.id == selectedCaregiverId }
                if (caregiverIndex != -1) {
                    binding.spinnerCaregiver.setText(caregiverList[caregiverIndex].name, false)
                }
            } else if (caregiverList.isNotEmpty()) {
                // Por defecto, seleccionar el cuidador actual
                val currentUserId = auth.currentUser?.uid
                val currentIndex = caregiverList.indexOfFirst { it.id == currentUserId }

                if (currentIndex != -1) {
                    binding.spinnerCaregiver.setText(caregiverList[currentIndex].name, false)
                    selectedCaregiverId = caregiverList[currentIndex].id
                } else {
                    // Si no está en la lista, seleccionar el primero
                    binding.spinnerCaregiver.setText(caregiverList[0].name, false)
                    selectedCaregiverId = caregiverList[0].id
                }
            }

            Log.d(TAG, "✅ ${caregiverList.size} cuidadores cargados")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cargando cuidadores: ${e.message}")
        }
    }

    private fun setupButtons() {
        binding.spinnerElderly.setOnItemClickListener { _, _, position, _ ->
            selectedElderlyId = elderlyList[position].id
            // ✅ IMPORTANTE: Recargar cuidadores cuando cambia el adulto mayor
            viewLifecycleOwner.lifecycleScope.launch {
                loadCaregiverList()
            }
        }

        binding.spinnerCaregiver.setOnItemClickListener { _, _, position, _ ->
            selectedCaregiverId = caregiverList[position].id
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            // ✅ Llamar a función de actualización o creación según el modo
            if (isEditMode) {
                updateEvent()
            } else {
                saveEvent()
            }
        }
    }

    // Función para ACTUALIZAR evento existente
    private fun updateEvent() {
        // Validaciones
        val title = binding.etTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa un título", Toast.LENGTH_SHORT).show()
            return
        }

        val notes = binding.etNotes.text.toString().trim()
        val eventId = eventToEdit?.id ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                when (selectedEventType) {
                    EventType.MEDICAL_APPOINTMENT -> {
                        val location = binding.etLocation.text.toString().trim()
                        if (location.isEmpty()) {
                            Toast.makeText(requireContext(), "Ingresa la ubicación", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val result = repository.updateMedicalAppointment(
                            eventId = eventId,
                            title = title,
                            date = selectedDate.timeInMillis,
                            location = location,
                            notes = notes
                        )

                        result.onSuccess {
                            Toast.makeText(requireContext(), "Cita médica actualizada", Toast.LENGTH_SHORT).show()
                            viewModel.loadAllEvents()
                            viewModel.loadUpcomingEventsForDashboard()
                            viewModel.notifyEventChanged()
                            dismiss()
                        }.onFailure { error ->
                            Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    EventType.NORMAL_TASK -> {
                        val result = repository.updateNormalTask(
                            eventId = eventId,
                            title = title,
                            date = selectedDate.timeInMillis,
                            assignedTo = selectedCaregiverId,
                            notes = notes
                        )

                        result.onSuccess {
                            Toast.makeText(requireContext(), "Tarea actualizada", Toast.LENGTH_SHORT).show()
                            viewModel.loadAllEvents()
                            viewModel.loadUpcomingEventsForDashboard()
                            viewModel.notifyEventChanged()
                            dismiss()
                        }.onFailure { error ->
                            Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error inesperado: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función existente para CREAR (sin cambios)
    private fun saveEvent() {
        // Validaciones
        val title = binding.etTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa un título", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedElderlyId == null) {
            Toast.makeText(requireContext(), "Selecciona un adulto mayor", Toast.LENGTH_SHORT).show()
            return
        }

        val notes = binding.etNotes.text.toString().trim()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                when (selectedEventType) {
                    EventType.MEDICAL_APPOINTMENT -> {
                        val location = binding.etLocation.text.toString().trim()
                        if (location.isEmpty()) {
                            Toast.makeText(requireContext(), "Ingresa la ubicación", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val result = repository.createMedicalAppointment(
                            title = title,
                            date = selectedDate.timeInMillis,
                            elderlyId = selectedElderlyId!!,
                            location = location,
                            notes = notes
                        )

                        result.onSuccess {
                            Toast.makeText(requireContext(), "Cita médica creada", Toast.LENGTH_SHORT).show()
                            //Recargar TODOS los eventos
                            viewModel.loadAllEvents()
                            viewModel.loadUpcomingEventsForDashboard()
                            viewModel.notifyEventChanged()
                            dismiss()
                        }.onFailure { error ->
                            Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    EventType.NORMAL_TASK -> {
                        val result = repository.createNormalTask(
                            title = title,
                            date = selectedDate.timeInMillis,
                            elderlyId = selectedElderlyId!!,
                            assignedTo = selectedCaregiverId,
                            notes = notes
                        )

                        result.onSuccess {
                            Toast.makeText(requireContext(), "Tarea creada", Toast.LENGTH_SHORT).show()

                            viewModel.loadAllEvents()
                            viewModel.loadUpcomingEventsForDashboard()
                            viewModel.notifyEventChanged()
                            dismiss()
                        }.onFailure { error ->
                            Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error inesperado: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Data class para representar un cuidador
data class Caregiver(
    val id: String,
    val name: String
)