package com.isa.cuidadocompartidomayor.ui.agenda

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.AgendaEvent
import com.isa.cuidadocompartidomayor.data.model.ElderlyItem
import com.isa.cuidadocompartidomayor.databinding.FragmentAgendaBinding
import com.isa.cuidadocompartidomayor.ui.agenda.adapter.AgendaUnifiedAdapter
import com.isa.cuidadocompartidomayor.ui.agenda.adapter.ElderlySpinnerAdapter
import com.isa.cuidadocompartidomayor.ui.agenda.dialog.AddEditEventBottomSheet
import com.isa.cuidadocompartidomayor.ui.agenda.dialog.EventDetailsBottomSheet
import com.isa.cuidadocompartidomayor.ui.agenda.viewmodel.AgendaViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AgendaFragment : Fragment() {

    private var _binding: FragmentAgendaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AgendaViewModel by activityViewModels()

    private lateinit var adapter: AgendaUnifiedAdapter
    private val elderlyList = mutableListOf<ElderlyItem>()
    private var selectedElderlyId: String? = null
    private var selectedDate: Long = System.currentTimeMillis()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgendaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCalendar()
        setupElderlySpinner()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = AgendaUnifiedAdapter(
            onEventClick = { event ->
                // Abrir detalles del evento (opcional)
                openDetailsDialog(event)
            },
            onEditClick = { event ->
                openEditDialog(event)
            },
            onDeleteClick = { event ->
                deleteEvent(event)
            },
            onTaskCheckChanged = { task, isChecked ->
                handleToggleComplete(task, isChecked)
            }
        )

        binding.recyclerViewEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AgendaFragment.adapter
        }
    }

    private fun setupCalendar() {
        // Configurar fecha inicial (hoy)
        binding.calendarView.date = selectedDate

        // Listener para cambios de fecha
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            selectedDate = calendar.timeInMillis

            // Actualizar título de la sección
            updateSelectedDateTitle(selectedDate)

            // Recargar eventos de la nueva fecha
            loadEventsForSelectedDate()
        }

        // Actualizar título inicial
        updateSelectedDateTitle(selectedDate)
    }

    private fun setupElderlySpinner() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Obtener relaciones del cuidador
                val relationshipsSnapshot = db.collection("relationships")
                    .whereEqualTo("caregiverId", currentUserId)
                    .get()
                    .await()

                val elderlyIds = relationshipsSnapshot.documents.mapNotNull {
                    it.getString("elderlyId")
                }

                if (elderlyIds.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "No tienes adultos mayores asignados",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Obtener datos de adultos mayores
                val elderlySnapshot = db.collection("users")
                    .whereIn("uid", elderlyIds)
                    .get()
                    .await()

                elderlyList.clear()
                elderlyList.add(ElderlyItem("all", "Todos los adultos mayores", ""))

                elderlySnapshot.documents.forEach { doc ->
                    val id = doc.id
                    val fullName = doc.getString("name") ?: ""
                    val profileImageUrl = doc.getString("profileImageUrl") ?: ""
                    elderlyList.add(ElderlyItem(id, fullName, profileImageUrl))
                }

                // Configurar adapter del spinner
                val spinnerAdapter = ElderlySpinnerAdapter(
                    requireContext(),
                    elderlyList
                )
                binding.spinnerElderly.adapter = spinnerAdapter

                // Listener de selección
                binding.spinnerElderly.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedElderlyId = elderlyList[position].id
                        loadEventsForSelectedDate()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al cargar adultos mayores: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupFab() {
        binding.fabAddEvent.setOnClickListener {
            val dialog = AddEditEventBottomSheet()
            dialog.show(childFragmentManager, "AddEditEventBottomSheet")
        }
    }

    private fun observeViewModel() {
        // Observar eventos de la fecha seleccionada
        viewModel.eventsForSelectedDate.observe(viewLifecycleOwner) { events ->
            binding.progressBar.visibility = View.GONE

            if (events.isEmpty()) {
                binding.recyclerViewEvents.visibility = View.GONE
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.tvEventCount.text = "0 eventos"
            } else {
                binding.recyclerViewEvents.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE
                binding.tvEventCount.text = resources.getQuantityString(
                    R.plurals.event_count,
                    events.size,
                    events.size
                )
                adapter.submitList(events)
            }
        }
        // Observer para refrescar cuando se edita un evento
        viewModel.refreshTrigger.observe(viewLifecycleOwner) { timestamp ->
            Log.d("AgendaFragment", "🔄 Recibida notificación de cambio: $timestamp")
            if (selectedElderlyId != null && selectedDate != 0L) {
                loadEventsForSelectedDate()
            }
        }
    }

    private fun loadEventsForSelectedDate() {
        if (selectedElderlyId == null) return

        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewEvents.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.GONE

        viewModel.loadEventsByDate(selectedElderlyId!!, selectedDate)
    }

    private fun updateSelectedDateTitle(dateInMillis: Long) {
        val date = Date(dateInMillis)
        val dateFormat = SimpleDateFormat("d 'de' MMMM", Locale.getDefault())
        val formattedDate = dateFormat.format(date)
        binding.tvSelectedDateTitle.text = "Eventos del $formattedDate"
    }

    private fun openEditDialog(event: AgendaEvent) {
        val dialog = AddEditEventBottomSheet.newInstanceForEdit(event)
        dialog.show(childFragmentManager, "AddEditEventBottomSheet")
    }

    private fun openDetailsDialog(event: AgendaEvent) {
        val elderly = elderlyList.find { it.id == event.elderlyId }
        val dialog = EventDetailsBottomSheet.newInstance(event, elderly?.profileImageUrl)
        dialog.show(childFragmentManager, "EventDetailsBottomSheet")
    }

    private fun deleteEvent(event: AgendaEvent) {
        viewModel.deleteEvent(
            eventId = event.id,
            onSuccess = {
                Toast.makeText(
                    requireContext(),
                    "Evento eliminado correctamente",
                    Toast.LENGTH_SHORT
                ).show()
                loadEventsForSelectedDate()
            },
            onError = { error ->
                Toast.makeText(
                    requireContext(),
                    "Error al eliminar: $error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun handleToggleComplete(task: AgendaEvent.NormalTask, isChecked: Boolean) {
        viewModel.toggleTaskCompletion(
            taskId = task.id,
            isCompleted = isChecked,
            onSuccess = {
                val message = if (isChecked) "Tarea completada ✓" else "Tarea marcada como pendiente"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                loadEventsForSelectedDate()
            },
            onError = { error ->
                Toast.makeText(
                    requireContext(),
                    "Error: $error",
                    Toast.LENGTH_SHORT
                ).show()
                adapter.notifyDataSetChanged()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
