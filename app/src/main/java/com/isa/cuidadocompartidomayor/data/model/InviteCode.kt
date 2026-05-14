package com.isa.cuidadocompartidomayor.data.model

import com.isa.cuidadocompartidomayor.utils.InviteCodeGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InviteCode(
    val id: String = "",
    val code: String = "",
    val elderlyId: String = "",
    val elderlyName: String = "",
    val elderlyEmail: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // 24 horas
    val active: Boolean = true,
    val usedAt: Long? = null,
    val usedBy: String? = null // ID del cuidador que usó el código
) {
    // Constructor sin argumentos para Firestore
    constructor() : this("", "", "", "", "", 0L, 0L, false, null, null)


    /** Verifica si el código ha expirado */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }

    /** Verifica si el código es válido para uso  */
    fun isValid(): Boolean {
        return active && !isExpired() && usedAt == null
    }

    /** Obtiene el tiempo restante en horas */
    fun getHoursUntilExpiration(): Long {
        val remainingMillis = expiresAt - System.currentTimeMillis()
        return if (remainingMillis > 0) remainingMillis / (1000 * 60 * 60) else 0
    }

    /** Obtiene la fecha de creación formateada */
    fun getFormattedCreationDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(createdAt))
    }

    /** Obtiene la fecha de expiración formateada */
    fun getFormattedExpirationDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(expiresAt))
    }

    /** Formatea el código para mostrar (XXX-XXX) */
    fun getFormattedCode(): String {
        return InviteCodeGenerator.formatCodeForDisplay(code)
    }
}