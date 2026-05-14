package com.isa.cuidadocompartidomayor.ui.diary.tabs

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.chip.Chip
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.MoodEmotion
import com.isa.cuidadocompartidomayor.data.model.VitalSignType
import com.isa.cuidadocompartidomayor.data.repository.DiaryRepository
import com.isa.cuidadocompartidomayor.databinding.FragmentTrendsStatsBinding
import com.isa.cuidadocompartidomayor.ui.diary.viewmodel.DiaryViewModel
import com.isa.cuidadocompartidomayor.utils.ChartHelper
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.*

class TrendsStatsFragment : Fragment() {

    private var _binding: FragmentTrendsStatsBinding? = null
    private val binding get() = _binding!!

    private val diaryViewModel: DiaryViewModel by activityViewModels()
    private val diaryRepository = DiaryRepository()

    private var selectedPeriodDays = 7
    private var selectedVitalSignType: VitalSignType = VitalSignType.HEART_RATE
    
    private var currentElderlyId: String? = null

    companion object {
        private const val TAG = "TrendsStatsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrendsStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPeriodSpinner()
        setupVitalSignTypeChips()
        setupCharts()
        observeViewModel()

        Log.d(TAG, "✅ TrendsStatsFragment inicializado")
    }

    private fun setupPeriodSpinner() {
        val periods = arrayOf("Última semana", "Último mes", "Últimos 3 meses", "Todo")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerDateRange.adapter = adapter
        binding.spinnerDateRange.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPeriodDays = when (position) {
                    0 -> 7
                    1 -> 30
                    2 -> 90
                    else -> Int.MAX_VALUE
                }
                currentElderlyId?.let { loadStatistics(it) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupVitalSignTypeChips() {
        // Crear chips para cada tipo de signo vital
        val vitalSignTypes = listOf(
            VitalSignType.HEART_RATE to "❤️ Pulso",
            VitalSignType.GLUCOSE to "🩸 Glucosa",
            VitalSignType.TEMPERATURE to "🌡️ Temperatura",
            VitalSignType.BLOOD_PRESSURE to "💉 Presión",
            VitalSignType.OXYGEN_SATURATION to "💨 Oxígeno",
            VitalSignType.WEIGHT to "⚖️ Peso"
        )

        vitalSignTypes.forEach { (type, label) ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                isChecked = (type == selectedVitalSignType)

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

                setOnClickListener {
                    selectedVitalSignType = type
                    // Pasar el elderlyId actual
                    currentElderlyId?.let { loadLineChartData(it) }
                }
            }
            binding.chipGroupVitalSignType.addView(chip)
        }
    }

    private fun setupCharts() {
        // Configurar LineChart
        ChartHelper.setupLineChart(binding.lineChart)

        // Configurar BarChart
        ChartHelper.setupBarChart(binding.barChart)
    }

    private fun observeViewModel() {
        diaryViewModel.selectedElderlyId.observe(viewLifecycleOwner) { elderlyId ->
            elderlyId?.let {
                // GUARDAR el elderlyId actual
                currentElderlyId = it
                Log.d(TAG, "📊 Cargando estadísticas para: $it")
                loadStatistics(it)
            }
        }
    }
    private fun loadStatistics(elderlyId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "📊 Cargando estadísticas para: $elderlyId")

                // Calcular fecha de inicio
                val calendar = Calendar.getInstance()
                if (selectedPeriodDays != Int.MAX_VALUE) {
                    calendar.add(Calendar.DAY_OF_YEAR, -selectedPeriodDays)
                }
                val startDate = if (selectedPeriodDays == Int.MAX_VALUE) 0L else calendar.timeInMillis
                val endDate = System.currentTimeMillis()

                val statsResult = diaryRepository.getVitalSignsStatistics(elderlyId, startDate, endDate)
                val stats = statsResult.getOrNull()

                // Obtener mood entries
                val moodEntries = diaryRepository.getMoodEntriesByElderlyId(elderlyId).getOrNull() ?: emptyList()
                val filteredMoodEntries = moodEntries.filter { it.timestamp >= startDate }

                Log.d(TAG, "📊 Estadísticas obtenidas: $stats, Periodo: $startDate - $endDate")
                Log.d(TAG, "😊 Estados de ánimo: ${filteredMoodEntries.size}")

                // Actualizar UI
                if (stats == null || (stats.totalRecords == 0 && filteredMoodEntries.isEmpty())) {
                    //hideAllCards()
                    //showEmptyState("No hay datos para el período seleccionado.\n\nAgrega registros para ver estadísticas.")
                } else {
                    binding.tvEmptyCharts.visibility = View.GONE

                    if (stats.totalRecords > 0 || filteredMoodEntries.isNotEmpty()) {
                        binding.cardStats.visibility = View.VISIBLE
                        updateGeneralStatsWithRepository(stats, filteredMoodEntries.size)
                    }

                    if (stats.totalRecords > 0) {
                        binding.cardLineChart.visibility = View.VISIBLE
                        loadLineChartData(elderlyId)
                    } else {
                        binding.cardLineChart.visibility = View.GONE
                    }

                    if (filteredMoodEntries.isNotEmpty()) {
                        binding.cardBarChart.visibility = View.VISIBLE
                        loadBarChartData(filteredMoodEntries)
                    } else {
                        binding.cardBarChart.visibility = View.GONE
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando estadísticas: ${e.message}", e)
                //showEmptyState("Error: ${e.message}")
            }
        }
    }
    //  NUEVA FUNCIÓN: Actualizar stats usando el resultado del repository
    private fun updateGeneralStatsWithRepository(
        stats: DiaryRepository.VitalSignsStatistics,
        moodEntriesCount: Int
    ) {
        val df = DecimalFormat("#.#")

        // Total de registros
        binding.tvTotalRecords.text = " Total de registros: ${stats.totalRecords}" //+ moodEntriesCount
        Log.d(TAG, "Total registros de singos vitales ${stats.totalRecords}")

        // Promedio de pulso
        binding.tvAvgHeartRate.text = stats.averageHeartRate?.let {
            "${df.format(it)} LPM"
        } ?: "Sin datos"

        // Promedio de glucosa
        binding.tvAvgGlucose.text = stats.averageGlucose?.let {
            "${df.format(it)} mg/dL"
        } ?: "Sin datos"

        // Promedio de temperatura
        binding.tvAvgTemperature.text = stats.averageTemperature?.let {
            "${df.format(it)}°C"
        } ?: "Sin datos"

        // Promedio de Oxigenacion
        binding.tvAvgOxygenSaturation.text = stats.averageOxygenSaturation?.let {
            "${df.format(it)}°%"
        } ?: "Sin datos"

        // Promedio de Oxigenacion
        binding.tvAvgWeight.text = stats.averageWeight?.let {
            "${df.format(it)}Kg"
        } ?: "Sin datos"

        // Promedio de presión arterial
        binding.tvAvgBloodPressure.text = if (stats.averageSystolicBP != null && stats.averageDiastolicBP != null) {
            "${stats.averageSystolicBP}/${stats.averageDiastolicBP} mmHg"
        } else {
            "Sin datos"
        }
    }

    private fun loadLineChartData(elderlyId: String) {
        val elderlyId = diaryViewModel.selectedElderlyId.value ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val vitalSigns = diaryRepository.getVitalSignsByElderlyId(elderlyId).getOrNull() ?: return@launch

                val calendar = Calendar.getInstance()
                if (selectedPeriodDays != Int.MAX_VALUE) {
                    calendar.add(Calendar.DAY_OF_YEAR, -selectedPeriodDays)
                }
                val startDate = if (selectedPeriodDays == Int.MAX_VALUE) 0L else calendar.timeInMillis
                val filteredSigns = vitalSigns.filter { it.timestamp >= startDate }
                val sortedSigns = filteredSigns.sortedBy { it.timestamp }

                //  CAMBIO: Si es presión arterial, crear 2 datasets
                if (selectedVitalSignType == VitalSignType.BLOOD_PRESSURE) {
                    loadBloodPressureChart(sortedSigns)
                } else {
                    loadSingleLineChart(sortedSigns)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando gráfica: ${e.message}", e)
            }
        }
    }
    /**
     * Carga una gráfica con una sola línea (para todos los signos excepto presión arterial)
     */
    private fun loadSingleLineChart(sortedSigns: List<com.isa.cuidadocompartidomayor.data.model.VitalSign>) {
        val entries = mutableListOf<Entry>()

        sortedSigns.forEach { sign ->
            val value = when (selectedVitalSignType) {
                VitalSignType.HEART_RATE -> sign.heartRate?.toFloat()
                VitalSignType.GLUCOSE -> sign.glucose?.toFloat()
                VitalSignType.TEMPERATURE -> sign.temperature?.toFloat()
                VitalSignType.OXYGEN_SATURATION -> sign.oxygenSaturation?.toFloat()
                VitalSignType.WEIGHT -> sign.weight?.toFloat()
                else -> null
            }

            value?.let {
                entries.add(Entry(sign.timestamp.toFloat(), it))
            }
        }

        if (entries.isEmpty()) {
            binding.lineChart.clear()
            binding.lineChart.setNoDataText("No hay datos para este signo vital")
            return
        }

        val dataSet = LineDataSet(entries, getVitalSignLabel(selectedVitalSignType)).apply {
            color = getVitalSignColor(selectedVitalSignType)
            setCircleColor(getVitalSignColor(selectedVitalSignType))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 9f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.lineChart.xAxis.valueFormatter = ChartHelper.DateValueFormatter()
        val lineData = LineData(dataSet)
        binding.lineChart.data = lineData
        binding.lineChart.invalidate()

        Log.d(TAG, "✅ Gráfica actualizada con ${entries.size} puntos")
    }

    /**
     * Carga una gráfica con DOS líneas para presión arterial (sistólica y diastólica)
     */
    private fun loadBloodPressureChart(sortedSigns: List<com.isa.cuidadocompartidomayor.data.model.VitalSign>) {
        val systolicEntries = mutableListOf<Entry>()
        val diastolicEntries = mutableListOf<Entry>()

        sortedSigns.forEach { sign ->
            val systolic = sign.systolicBP?.toFloat()
            val diastolic = sign.diastolicBP?.toFloat()

            systolic?.let {
                systolicEntries.add(Entry(sign.timestamp.toFloat(), it))
            }

            diastolic?.let {
                diastolicEntries.add(Entry(sign.timestamp.toFloat(), it))
            }
        }

        if (systolicEntries.isEmpty() && diastolicEntries.isEmpty()) {
            binding.lineChart.clear()
            binding.lineChart.setNoDataText("No hay datos de presión arterial")
            return
        }

        val dataSets = mutableListOf<ILineDataSet>()

        //  Dataset para SISTÓLICA (línea rosa)
        if (systolicEntries.isNotEmpty()) {
            val systolicDataSet = LineDataSet(systolicEntries, "💉 Sistólica (mmHg)").apply {
                color = ChartHelper.Colors.BLOOD_PRESSURE_SYS
                setCircleColor(ChartHelper.Colors.BLOOD_PRESSURE_SYS)
                lineWidth = 2.5f
                circleRadius = 4f
                setDrawCircleHole(false)
                valueTextSize = 9f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            dataSets.add(systolicDataSet)
        }

        //  Dataset para DIASTÓLICA (línea púrpura)
        if (diastolicEntries.isNotEmpty()) {
            val diastolicDataSet = LineDataSet(diastolicEntries, "💉 Diastólica (mmHg)").apply {
                color = ChartHelper.Colors.BLOOD_PRESSURE_DIA
                setCircleColor(ChartHelper.Colors.BLOOD_PRESSURE_DIA)
                lineWidth = 2.5f
                circleRadius = 4f
                setDrawCircleHole(false)
                valueTextSize = 9f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            dataSets.add(diastolicDataSet)
        }

        binding.lineChart.xAxis.valueFormatter = ChartHelper.DateValueFormatter()
        val lineData = LineData(dataSets)
        binding.lineChart.data = lineData
        binding.lineChart.invalidate()

        Log.d(TAG, "✅ Gráfica de presión arterial actualizada: ${systolicEntries.size} sistólica, ${diastolicEntries.size} diastólica")
    }

    private fun loadBarChartData(moodEntries: List<com.isa.cuidadocompartidomayor.data.model.MoodEntry>) {
        // Contar frecuencia de cada emoción
        val emotionCounts = mutableMapOf<String, Int>() //  CAMBIO: String en lugar de MoodEmotion

        moodEntries.forEach { entry ->
            entry.emotions.forEach { emotionName -> //  Ya es String
                emotionCounts[emotionName] = (emotionCounts[emotionName] ?: 0) + 1
            }
        }

        // Ordenar por frecuencia y tomar top 5
        val topEmotions = emotionCounts.entries
            .sortedByDescending { it.value }
            .take(5)

        if (topEmotions.isEmpty()) {
            binding.barChart.clear()
            binding.barChart.setNoDataText("No hay datos de emociones")
            return
        }

        // Crear entradas para el BarChart
        val entries = topEmotions.mapIndexed { index, (_, count) ->
            BarEntry(index.toFloat(), count.toFloat())
        }

        // Crear dataset
        val dataSet = BarDataSet(entries, "Frecuencia de Emociones\n Top 5").apply {
            colors = topEmotions.map { getEmotionColorByName(it.key) } //  CAMBIO: función auxiliar
            valueTextSize = 12f
            valueTextColor = Color.BLACK
        }

        // Configurar eje X con nombres de emociones
        binding.barChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < topEmotions.size) {
                    val emotionName = topEmotions[index].key
                    // Convertir String a MoodEmotion para obtener emoji y display name
                    try {
                        val emotion = MoodEmotion.valueOf(emotionName)
                        "${emotion.emoji} ${emotion.displayName}"
                    } catch (e: Exception) {
                        emotionName // Si falla, mostrar el nombre raw
                    }
                } else {
                    ""
                }
            }
        }

        // Configurar datos
        val barData = BarData(dataSet)
        barData.barWidth = 0.5f
        binding.barChart.data = barData
        binding.barChart.invalidate()

        Log.d(TAG, "✅ Gráfica de emociones actualizada")
    }

    private fun getVitalSignLabel(type: VitalSignType): String {
        return when (type) {
            VitalSignType.HEART_RATE -> "❤️ Pulso (LPM)"
            VitalSignType.GLUCOSE -> "🩸 Glucosa (mg/dL)"
            VitalSignType.TEMPERATURE -> "🌡️ Temperatura (°C)"
            VitalSignType.BLOOD_PRESSURE -> "💉 Presión Arterial (mmHg)"
            VitalSignType.OXYGEN_SATURATION -> "💨 Saturación O₂ (%)"
            VitalSignType.WEIGHT -> "⚖️ Peso (Kg)"
        }
    }

    private fun getVitalSignColor(type: VitalSignType): Int {
        return when (type) {
            VitalSignType.HEART_RATE -> ChartHelper.Colors.HEART_RATE
            VitalSignType.GLUCOSE -> ChartHelper.Colors.GLUCOSE
            VitalSignType.TEMPERATURE -> ChartHelper.Colors.TEMPERATURE
            VitalSignType.BLOOD_PRESSURE -> ChartHelper.Colors.BLOOD_PRESSURE_SYS
            VitalSignType.OXYGEN_SATURATION -> ChartHelper.Colors.OXYGEN
            VitalSignType.WEIGHT -> ChartHelper.Colors.WEIGHT
        }
    }

    private fun getEmotionColorByName(emotionName: String): Int {
        return try {
            val emotion = MoodEmotion.valueOf(emotionName)
            when (emotion) {
                MoodEmotion.JOY, MoodEmotion.SERENITY, MoodEmotion.GRATITUDE, MoodEmotion.SURPRISE, MoodEmotion.PURPOSE ->
                    ChartHelper.Colors.MOOD_POSITIVE
                MoodEmotion.NEUTRAL, MoodEmotion.BOREDOM, MoodEmotion.SOCIABLE, MoodEmotion.PRIDE->
                    ChartHelper.Colors.MOOD_NEUTRAL
                else ->
                    ChartHelper.Colors.MOOD_NEGATIVE
            }
        } catch (e: Exception) {
            ChartHelper.Colors.MOOD_NEUTRAL // Default si no se puede convertir
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}