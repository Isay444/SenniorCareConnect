package com.isa.cuidadocompartidomayor.utils

import android.util.Log
import kotlin.random.Random

/** Generador de códigos de invitación únicos para adultos mayores */
object InviteCodeGenerator {
    private const val TAG = "InviteCodeGenerator"
    private const val CODE_LENGTH = 6

    // Caracteres permitidos (Sin vocales para evitar palabras ofensivas)
    private val ALLOWED_CHARS = "BCDFGHJKLMNPQRSTVWXYZ0123456789"

    /** Generar un código de invitación único. Formato: ABC123 (6 caracteres alfanuméricos) */
    fun generateInviteCode(): String {
        val code = StringBuilder()
        repeat(CODE_LENGTH) {
            val randomIndex = Random.nextInt(ALLOWED_CHARS.length)
            code.append(ALLOWED_CHARS[randomIndex])
        }

        val generatedCode = code.toString()
        Log.d(TAG, "Código de invitación generado: $generatedCode")

        return generatedCode
    }

    /** Valida el formato de un código de invitación */
    fun isValidInviteCode(code: String): Boolean {
        return when {
            code.length != CODE_LENGTH -> {
                Log.w(TAG, "Código inválido - longitud incorrecta: ${code.length}")
                false
            }
            !code.matches(Regex("[A-Z0-9]+")) -> {
                Log.w(TAG, "Código inválido - caracteres no permitidos: $code")
                false
            }
            else -> {
                Log.d(TAG, "Código válido: $code")
                true
            }
        }
    }

    /** Formatea un código para mostrar  */
    fun formatCodeForDisplay(code: String): String {
        return if (code.length == CODE_LENGTH) {
            "${code.substring(0, 3)}${code.substring(3)}"
        } else {
            code
        }
    }

    /** Remueve formato de un código (sin espacios ni guiones) */
    fun cleanCode(code: String): String {
        return code.replace(" ", "").replace("-", "").uppercase()
    }

    // ====== MÉTODOS ADICIONALES PARA COMPATIBILIDAD ======

    /** Genera un ID único para el código (para base de datos) */
    fun generateCodeId(): String {
        val timestamp = System.currentTimeMillis()
        val randomSuffix = (1..4)
            .map { ALLOWED_CHARS[Random.nextInt(ALLOWED_CHARS.length)] }
            .joinToString("")
        return "INV_${timestamp}_$randomSuffix"
    }


    /**  para compatibilidad con Repository */
    fun isValidCodeFormat(code: String): Boolean = isValidInviteCode(code)
}