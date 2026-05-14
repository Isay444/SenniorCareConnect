package com.isa.cuidadocompartidomayor.ui.medications.wizard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.isa.cuidadocompartidomayor.R
import android.app.TimePickerDialog
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.isa.cuidadocompartidomayor.databinding.DialogTimePickerBinding
import com.isa.cuidadocompartidomayor.databinding.FragmentScheduleMultipleDosesBinding
import com.isa.cuidadocompartidomayor.ui.medications.adapter.DoseTimeAdapter
import com.isa.cuidadocompartidomayor.ui.medications.viewmodel.AddMedicationViewModel

class ScheduleMultipleDosesFragment : Fragment() {

    private var _binding: FragmentScheduleMultipleDosesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMedicationViewModel by activityViewModels()
    private lateinit var adapter: DoseTimeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleMultipleDosesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupContinueButton()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = DoseTimeAdapter { position, currentTime ->
            showCustomTimePickerDialog(position, currentTime)
        }

        binding.rvDoseTimes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDoseTimes.adapter = adapter
    }

    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            // Navegar a la pantalla de dosis
            findNavController().navigate(
                R.id.action_scheduleMultipleDosesFragment_to_enterDosageFragment
            )
        }
    }

    private fun observeViewModel() {
        viewModel.timesPerDay.observe(viewLifecycleOwner) { times ->
            binding.tvFrequencyInfo.text = "$times veces al día"
        }

        viewModel.scheduledTimes.observe(viewLifecycleOwner) { times ->
            adapter.updateTimes(times)
        }
    }

    /**
     * Muestra diálogo personalizado con NumberPickers
     */
    private fun showCustomTimePickerDialog(position: Int, currentTime: String) {
        // Parsear hora actual
        val timeParts = currentTime.split(":")
        val hour24 = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        // Convertir a formato 12 horas
        val hour12 = if (hour24 == 0) 12 else if (hour24 > 12) hour24 - 12 else hour24
        val isPM = hour24 >= 12

        // Inflar el layout del diálogo
        val dialogBinding = DialogTimePickerBinding.inflate(layoutInflater)

        // Configurar NumberPickers
        setupDialogNumberPickers(dialogBinding, hour12, minute, isPM)

        // Crear y mostrar el diálogo
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // Botón Cancelar
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Botón Aceptar
        dialogBinding.btnAccept.setOnClickListener {
            val selectedHour = dialogBinding.npHour.value
            val selectedMinute = dialogBinding.npMinute.value
            val selectedIsPM = dialogBinding.npAmPm.value == 1

            // Convertir a formato 24 horas
            var hour24Result = if (selectedIsPM && selectedHour != 12) {
                selectedHour + 12
            } else {
                selectedHour
            }

            if (!selectedIsPM && selectedHour == 12) {
                hour24Result = 0
            }

            val newTime = String.format("%02d:%02d", hour24Result, selectedMinute)
            viewModel.updateScheduledTime(position, newTime)

            dialog.dismiss()
        }

        // Cambiar el título según la posición
        dialogBinding.tvDialogTitle.text = "${position + 1}ª hora de consumo"

        dialog.show()
    }

    /**
     * Configura los NumberPickers del diálogo
     */
    private fun setupDialogNumberPickers(
        dialogBinding: DialogTimePickerBinding,
        hour: Int,
        minute: Int,
        isPM: Boolean
    ) {
        // NumberPicker de horas (01-12)
        dialogBinding.npHour.apply {
            minValue = 1
            maxValue = 12
            value = hour
            setFormatter { String.format("%02d", it) }
            wrapSelectorWheel = true
        }

        // NumberPicker de minutos (00-59)
        dialogBinding.npMinute.apply {
            minValue = 0
            maxValue = 59
            value = minute
            setFormatter { String.format("%02d", it) }
            wrapSelectorWheel = true
        }

        // NumberPicker de AM/PM
        dialogBinding.npAmPm.apply {
            minValue = 0
            maxValue = 1
            displayedValues = arrayOf("AM", "PM")
            value = if (isPM) 1 else 0
            wrapSelectorWheel = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}