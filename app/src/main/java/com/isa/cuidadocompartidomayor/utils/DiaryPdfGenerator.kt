package com.isa.cuidadocompartidomayor.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.isa.cuidadocompartidomayor.data.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Clase utilitaria para generar reportes PDF del diario
 */
object DiaryPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 50f
    private const val LINE_HEIGHT = 20f

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    /**
     * Genera un reporte PDF del diario

    fun generateReport(
        context: Context,
        elderlyName: String,
        startDate: Long,
        endDate: Long,
        vitalSigns: List<VitalSign>,
        moodEntries: List<MoodEntry>,
        stats: Map<String, DiaryReportViewModel.VitalSignStats>,
        notes: String
    ): String? {
        try {
            val pdfDocument = PdfDocument()
            var pageNumber = 1
            var yPosition = MARGIN

            // Página 1: Portada y Resumen
            val page1 = pdfDocument.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber++).create()
            )
            val canvas1 = page1.canvas

            yPosition = drawHeader(canvas1, elderlyName, startDate, endDate, yPosition)
            yPosition = drawSummary(canvas1, vitalSigns.size, moodEntries.size, yPosition)
            yPosition = drawStatistics(canvas1, stats, yPosition)

            pdfDocument.finishPage(page1)

            // Página 2+: Detalles de Signos Vitales
            if (vitalSigns.isNotEmpty()) {
                yPosition = MARGIN
                val page2 = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber++).create()
                )
                val canvas2 = page2.canvas

                yPosition = drawVitalSignsDetails(canvas2, vitalSigns, yPosition)

                pdfDocument.finishPage(page2)
            }

            // Página 3+: Detalles de Estados de Ánimo
            if (moodEntries.isNotEmpty()) {
                yPosition = MARGIN
                val page3 = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber++).create()
                )
                val canvas3 = page3.canvas

                yPosition = drawMoodEntriesDetails(canvas3, moodEntries, yPosition)

                pdfDocument.finishPage(page3)
            }

            // Página final: Notas
            if (notes.isNotBlank()) {
                yPosition = MARGIN
                val pageFinal = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber++).create()
                )
                val canvasFinal = pageFinal.canvas

                drawNotes(canvasFinal, notes, yPosition)

                pdfDocument.finishPage(pageFinal)
            }

            // Guardar el PDF
            val fileName = "Reporte_Diario_${System.currentTimeMillis()}.pdf"
            val directory = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "Reportes"
            )
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val file = File(directory, fileName)
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }

            pdfDocument.close()
            return file.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun drawHeader(
        canvas: Canvas,
        elderlyName: String,
        startDate: Long,
        endDate: Long,
        startY: Float
    ): Float {
        var y = startY
        val paint = Paint().apply {
            textSize = 24f
            isFakeBoldText = true
        }

        canvas.drawText("Reporte de Diario de Salud", MARGIN, y, paint)
        y += LINE_HEIGHT * 2

        paint.textSize = 16f
        paint.isFakeBoldText = false
        canvas.drawText("Paciente: $elderlyName", MARGIN, y, paint)
        y += LINE_HEIGHT * 1.5f

        canvas.drawText(
            "Período: ${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}",
            MARGIN,
            y,
            paint
        )
        y += LINE_HEIGHT * 1.5f

        canvas.drawText(
            "Fecha de generación: ${dateFormat.format(Date())}",
            MARGIN,
            y,
            paint
        )
        y += LINE_HEIGHT * 2

        return y
    }

    private fun drawSummary(
        canvas: Canvas,
        vitalSignsCount: Int,
        moodEntriesCount: Int,
        startY: Float
    ): Float {
        var y = startY
        val paint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }

        canvas.drawText("Resumen", MARGIN, y, paint)
        y += LINE_HEIGHT * 1.5f

        paint.textSize = 14f
        paint.isFakeBoldText = false
        canvas.drawText("Total de registros de signos vitales: $vitalSignsCount", MARGIN + 20, y, paint)
        y += LINE_HEIGHT

        canvas.drawText("Total de registros de estados de ánimo: $moodEntriesCount", MARGIN + 20, y, paint)
        y += LINE_HEIGHT * 2

        return y
    }

    private fun drawStatistics(
        canvas: Canvas,
        stats: Map<String, DiaryReportViewModel.VitalSignStats>,
        startY: Float
    ): Float {
        var y = startY
        val paint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }

        canvas.drawText("Estadísticas de Signos Vitales", MARGIN, y, paint)
        y += LINE_HEIGHT * 1.5f

        paint.textSize = 12f
        paint.isFakeBoldText = false

        stats.forEach { (key, stat) ->
            val label = when (key) {
                "heartRate" -> "Frecuencia Cardíaca (LPM)"
                "glucose" -> "Glucosa (mg/dL)"
                "temperature" -> "Temperatura (°C)"
                "systolicBP" -> "Presión Sistólica (mmHg)"
                "diastolicBP" -> "Presión Diastólica (mmHg)"
                "oxygenSaturation" -> "Saturación O₂ (%)"
                "weight" -> "Peso (Kg)"
                else -> key
            }

            paint.isFakeBoldText = true
            canvas.drawText(label, MARGIN + 20, y, paint)
            y += LINE_HEIGHT

            paint.isFakeBoldText = false
            canvas.drawText(
                "  Promedio: ${"%.2f".format(stat.average)} | Mín: ${"%.2f".format(stat.min)} | Máx: ${"%.2f".format(stat.max)} | Registros: ${stat.count}",
                MARGIN + 20,
                y,
                paint
            )
            y += LINE_HEIGHT * 1.5f
        }

        return y
    }

    private fun drawVitalSignsDetails(
        canvas: Canvas,
        vitalSigns: List<VitalSign>,
        startY: Float
    ): Float {
        var y = startY
        val paint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }

        canvas.drawText("Detalle de Signos Vitales", MARGIN, y, paint)
        y += LINE_HEIGHT * 1.5f

        paint.textSize = 10f
        paint.isFakeBoldText = false

        vitalSigns.take(10).forEach { sign ->
            val date = dateFormat.format(Date(sign.timestamp))
            val time = timeFormat.format(Date(sign.timestamp))

            paint.isFakeBoldText = true
            canvas.drawText("$date - $time", MARGIN + 10, y, paint)
            y += LINE_HEIGHT * 0.8f

            paint.isFakeBoldText = false
            sign.heartRate?.let {
                canvas.drawText("  FC: $it LPM", MARGIN + 20, y, paint)
                y += LINE_HEIGHT * 0.7f
            }
            sign.glucose?.let {
                canvas.drawText("  Glucosa: $it mg/dL", MARGIN + 20, y, paint)
                y += LINE_HEIGHT * 0.7f
            }
            sign.temperature?.let {
                canvas.drawText("  Temperatura: $it °C", MARGIN + 20, y, paint)
                y += LINE_HEIGHT * 0.7f
            }
            if (sign.systolicBP != null && sign.diastolicBP != null) {
                canvas.drawText("  PA: ${sign.systolicBP}/${sign.diastolicBP} mmHg", MARGIN + 20, y, paint)
                y += LINE_HEIGHT * 0.7f
            }
            sign.oxygenSaturation?.let {
                canvas.drawText("  SpO₂: $it%", MARGIN + 20, y, paint)
                y += LINE_HEIGHT * 0.7f
            }
            sign.weight?.let {
                canvas.drawText("  Peso: $it Kg", MARGIN + 20, y, paint)
                y += LINE_HEIGHT * 0.7f
            }

            y += LINE_HEIGHT * 0.5f
        }

        return y
    }

    private fun drawMoodEntriesDetails(
        canvas: Canvas,
        moodEntries: List<MoodEntry>,
        startY: Float
    ): Float {
        var y = startY
        val paint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }

        canvas.drawText("Detalle de Estados de Ánimo", MARGIN, y, paint)
        y += LINE_HEIGHT * 1.5f

        paint.textSize = 10f
        paint.isFakeBoldText = false

        moodEntries.take(15).forEach { entry ->
            val date = dateFormat.format(Date(entry.timestamp))
            val time = timeFormat.format(Date(entry.timestamp))

            paint.isFakeBoldText = true
            canvas.drawText("$date - $time", MARGIN + 10, y, paint)
            y += LINE_HEIGHT * 0.8f

            paint.isFakeBoldText = false
            if (entry.emotions.isNotEmpty()) {
                val emotions = entry.getEmotionsList().joinToString(", ") { it.displayName }
                canvas.drawText("  Emociones: $emotions", MARGIN + 20, y, paint)
                y += LINE_HEIGHT * 0.7f
            }
            if (entry.symptoms.isNotEmpty()) {
                val symptoms = entry.getSymptomsList().joinToString(", ") { it.displayName }
                canvas.drawText("  Síntomas: $symptoms", MARGIN + 20, y, paint)
                y += LINE_HEIGHT * 0.7f
            }

            y += LINE_HEIGHT * 0.5f
        }

        return y
    }

    private fun drawNotes(canvas: Canvas, notes: String, startY: Float): Float {
        var y = startY
        val paint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }

        canvas.drawText("Observaciones del Cuidador", MARGIN, y, paint)
        y += LINE_HEIGHT * 1.5f

        paint.textSize = 12f
        paint.isFakeBoldText = false

        // Dividir notas en líneas
        val maxWidth = PAGE_WIDTH - (MARGIN * 2)
        val words = notes.split(" ")
        var currentLine = ""

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val textWidth = paint.measureText(testLine)

            if (textWidth > maxWidth) {
                canvas.drawText(currentLine, MARGIN + 20, y, paint)
                y += LINE_HEIGHT
                currentLine = word
            } else {
                currentLine = testLine
            }
        }

        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, MARGIN + 20, y, paint)
            y += LINE_HEIGHT
        }

        return y
    }
    */
}
