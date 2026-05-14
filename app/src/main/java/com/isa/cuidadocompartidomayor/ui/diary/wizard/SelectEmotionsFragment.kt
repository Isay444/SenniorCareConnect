package com.isa.cuidadocompartidomayor.ui.diary.wizard

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.chip.Chip
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.MoodEmotion
import com.isa.cuidadocompartidomayor.databinding.FragmentSelectEmotionsBinding
import com.isa.cuidadocompartidomayor.ui.diary.wizard.viewmodel.AddMoodEntryViewModel

class SelectEmotionsFragment : Fragment() {

    private var _binding: FragmentSelectEmotionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddMoodEntryViewModel by activityViewModels()
    private val args: SelectEmotionsFragmentArgs by navArgs()

    companion object {
        private const val TAG = "SelectEmotions"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectEmotionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupPatientInfo()
        setupEmotionChips()
        setupButtons()
        observeViewModel()

        if (viewModel.isEditMode.value == true) {
            binding.toolbar.title = "Editar Emociones"
        }

        Log.d(TAG, "✅ Fragment inicializado")
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupPatientInfo() {
        binding.tvPatientName.text = "Para: ${args.elderlyName}"
        viewModel.setElderlyData(args.elderlyId, args.elderlyName)
    }

    private fun setupEmotionChips() {
        // Crear chip para cada emoción disponible
        MoodEmotion.entries.forEach { emotion ->
            val chip = createEmotionChip(emotion)
            binding.chipGroupEmotions.addView(chip)
        }
    }

    private fun createEmotionChip(emotion: MoodEmotion): Chip {
        return Chip(requireContext()).apply {
            text = "${emotion.emoji} ${emotion.displayName}"
            isCheckable = true
            isChecked = viewModel.isEmotionSelected(emotion)

            // Estilo del chip
            chipBackgroundColor = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf()
                ),
                intArrayOf(
                    ContextCompat.getColor(context, R.color.azul_principal),
                    ContextCompat.getColor(context, R.color.background_secondary)
                )
            )

            setTextColor(
                ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf()
                    ),
                    intArrayOf(
                        ContextCompat.getColor(context, android.R.color.white),
                        ContextCompat.getColor(context, R.color.text_primary)
                    )
                )
            )

            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.toggleEmotion(emotion)
                } else {
                    viewModel.toggleEmotion(emotion)
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.diaryFragment)
        }

        binding.btnContinue.setOnClickListener {
            navigateToSymptomsAndLevels()
        }
    }

    private fun observeViewModel() {
        viewModel.selectedEmotions.observe(viewLifecycleOwner) { selectedEmotions ->
            binding.btnContinue.isEnabled = selectedEmotions.isNotEmpty()
            Log.d(TAG, "${selectedEmotions.size} emociones seleccionadas")
        }
    }

    private fun navigateToSymptomsAndLevels() {
        val selectedEmotionsString = viewModel.selectedEmotions.value
            ?.joinToString(",") { it.name } ?: ""

        val action = SelectEmotionsFragmentDirections
            .actionSelectEmotionsToSelectSymptomsAndLevels(
                elderlyId = args.elderlyId,
                elderlyName = args.elderlyName,
                selectedEmotions = selectedEmotionsString,
                moodEntryId = args.moodEntryId
            )

        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
