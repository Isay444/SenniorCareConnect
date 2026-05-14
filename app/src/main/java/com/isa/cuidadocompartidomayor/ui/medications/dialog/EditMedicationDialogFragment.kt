package com.isa.cuidadocompartidomayor.ui.medications.dialog

import android.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.isa.cuidadocompartidomayor.data.model.Medication
import com.isa.cuidadocompartidomayor.data.model.MedicationType
import com.isa.cuidadocompartidomayor.databinding.DialogTimePickerBinding
import com.isa.cuidadocompartidomayor.databinding.FragmentEditMedicationBinding
import com.isa.cuidadocompartidomayor.ui.medications.adapter.EditDoseTimeAdapter
import com.isa.cuidadocompartidomayor.ui.medications.viewmodel.MedicationViewModel

class EditMedicationDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentEditMedicationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MedicationViewModel by activityViewModels()
    private val args: EditMedicationDialogFragmentArgs by navArgs()

    private lateinit var currentMedication: Medication
    private val scheduledTimesList = mutableListOf<String>()
    private lateinit var adapter: EditDoseTimeAdapter

    companion object {
        private const val TAG = "EditMedicationDialog"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditMedicationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMedicationTypeDropdown()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        // Cargar medicamento
        viewModel.loadMedicationById(args.medicationId)
    }

    private fun setupMedicationTypeDropdown() {
        val types = MedicationType.values().map { it.displayName }
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_dropdown_item_1line, types)
        binding.actvMedicationType.setAdapter(adapter)
    }

    private fun setupRecyclerView() {
        // ✅ Solo el callback onTimeClick - sin delete
        adapter = EditDoseTimeAdapter(
            onTimeClick = { position, currentTime ->
                showCustomTimePickerDialog(position, currentTime)
            }
        )

        binding.rvScheduledTimes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvScheduledTimes.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveChanges()
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.currentMedication.observe(viewLifecycleOwner) { medication ->
            medication?.let {
                currentMedication = it
                populateFields(it)
            }
        }

        viewModel.operationSuccess.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearOperationMessages()
                dismiss()
            }
        }

        viewModel.operationError.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearOperationMessages()
            }
        }

        viewModel.isProcessing.observe(viewLifecycleOwner) { isProcessing ->
            binding.btnSave.isEnabled = !isProcessing
            binding.btnSave.text = if (isProcessing) "Guardando..." else "Guardar cambios"
        }
    }

    private fun populateFields(medication: Medication) {
        binding.etMedicationName.setText(medication.name)
        binding.actvMedicationType.setText(medication.medicationType.displayName, false)
        binding.etDosage.setText(medication.dosage)

        // Cargar horarios existentes
        scheduledTimesList.clear()
        scheduledTimesList.addAll(medication.scheduledTimes)
        adapter.updateTimes(scheduledTimesList)
    }

    /**
     * ✅ Muestra diálogo personalizado con NumberPickers
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

            // Actualizar tiempo en la lista
            scheduledTimesList[position] = newTime
            scheduledTimesList.sort()
            adapter.updateTimes(scheduledTimesList)

            dialog.dismiss()
        }

        // Cambiar el título según la cantidad de horarios
        dialogBinding.tvDialogTitle.text = if (scheduledTimesList.size == 1) {
            "Hora de consumo"
        } else {
            "${position + 1}ª hora de consumo"
        }

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

    private fun saveChanges() {
        val name = binding.etMedicationName.text.toString().trim()
        val typeString = binding.actvMedicationType.text.toString()
        val dosage = binding.etDosage.text.toString().trim()

        // Validaciones
        if (name.isEmpty()) {
            binding.tilMedicationName.error = "Ingresa el nombre"
            return
        }

        if (typeString.isEmpty()) {
            binding.tilMedicationType.error = "Selecciona el tipo"
            return
        }

        if (dosage.isEmpty()) {
            binding.tilDosage.error = "Ingresa la dosis"
            return
        }

        if (scheduledTimesList.isEmpty()) {
            Toast.makeText(requireContext(), "Debe tener al menos un horario", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear medicamento actualizado
        val updatedMedication = currentMedication.copy(
            name = name,
            medicationType = MedicationType.Companion.fromDisplayName(typeString),
            dosage = dosage,
            scheduledTimes = scheduledTimesList.toList()
        )

        // Guardar
        viewModel.updateMedication(updatedMedication)
        Log.d(TAG, "✅ Guardando cambios: $name")
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ Eliminar medicamento")
            .setMessage(
                """
                ¿Estás seguro de eliminar este medicamento?
                
                💊 ${currentMedication.name}
                📏 ${currentMedication.dosage}
                
                ⚠️ Esta acción no se puede deshacer.
                """.trimIndent()
            )
            .setPositiveButton("🗑️ Sí, eliminar") { _, _ ->
                deleteMedication()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteMedication() {
        viewModel.deleteMedication(currentMedication.id, currentMedication.elderlyId)
        Toast.makeText(requireContext(), "Medicamento eliminado", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "✅ Medicamento eliminado: ${currentMedication.id}")
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}