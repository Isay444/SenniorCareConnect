package com.isa.cuidadocompartidomayor.ui.medications.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.Medication
import com.isa.cuidadocompartidomayor.databinding.BottomSheetMedicationDetailBinding
import java.text.SimpleDateFormat
import java.util.*

class MedicationDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMedicationDetailBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_MEDICATION_JSON = "medication_json"
        private const val ARG_ELDERLY_PHOTO = "elderly_photo"
        private const val ARG_CAREGIVER_PHOTO = "caregiver_photo"

        fun newInstance(
            medication: Medication,
            elderlyPhotoUrl: String,
            caregiverPhotoUrl: String
        ): MedicationDetailBottomSheet {
            val fragment = MedicationDetailBottomSheet()
            val args = Bundle()
            args.putString(ARG_MEDICATION_JSON, Gson().toJson(medication))
            args.putString(ARG_ELDERLY_PHOTO, elderlyPhotoUrl)
            args.putString(ARG_CAREGIVER_PHOTO, caregiverPhotoUrl)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMedicationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val medicationJson = arguments?.getString(ARG_MEDICATION_JSON)
        val elderlyPhoto = arguments?.getString(ARG_ELDERLY_PHOTO) ?: ""
        val caregiverPhoto = arguments?.getString(ARG_CAREGIVER_PHOTO) ?: ""

        if (medicationJson != null) {
            val medication = Gson().fromJson(medicationJson, Medication::class.java)
            displayMedicationDetails(medication, elderlyPhoto, caregiverPhoto)
        }

        binding.btnClose.setOnClickListener { dismiss() }
    }

    private fun displayMedicationDetails(
        medication: Medication,
        elderlyPhoto: String,
        caregiverPhoto: String
    ) {
        binding.apply {
            // Header
            tvMedicationName.text = medication.name
            tvMedicationType.text = medication.medicationType.displayName
            ivMedicationType.setImageResource(medication.medicationType.iconRes)

            // Personas
            tvElderlyName.text = medication.elderlyName
            tvCaregiverName.text = medication.caregiverName

            Glide.with(this@MedicationDetailBottomSheet)
                .load(elderlyPhoto)
                .placeholder(R.drawable.ic_avatar)
                .error(R.drawable.ic_avatar)
                .circleCrop()
                .into(ivElderlyAvatar)

            Glide.with(this@MedicationDetailBottomSheet)
                .load(caregiverPhoto)
                .placeholder(R.drawable.ic_avatar)
                .error(R.drawable.ic_avatar)
                .circleCrop()
                .into(ivCaregiverAvatar)

            // Detalles
            tvDosage.text = "Dosis: ${medication.dosage}"
            tvFrequency.text = "Frecuencia: ${medication.getFrequencyText()}"
            tvScheduledTimes.text = "Horarios: ${medication.scheduledTimes.joinToString(", ")}"

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val startDateStr = sdf.format(Date(medication.startDate))
            val endDateStr = if (medication.endDate != null) sdf.format(Date(medication.endDate)) else "Indefinido"
            tvDuration.text = "Vigencia: $startDateStr - $endDateStr"

            // Instrucciones
            if (medication.instructions.isNotBlank() && medication.instructions != "Sin notas") {
                layoutInstructions.visibility = View.VISIBLE
                tvInstructions.text = medication.instructions
            } else {
                layoutInstructions.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
