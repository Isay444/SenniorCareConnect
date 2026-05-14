package com.isa.cuidadocompartidomayor.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.isa.cuidadocompartidomayor.data.model.InviteCode
import com.isa.cuidadocompartidomayor.utils.InviteCodeGenerator
import kotlinx.coroutines.tasks.await

class InviteCodeRepository {

    private val db = FirebaseFirestore.getInstance()
    private val inviteCodesCollection = db.collection("inviteCodes")

    companion object {
        private const val TAG = "InviteCodeRepository"
    }

    /**
     * Genera y guarda un nuevo código de invitación
     */
    suspend fun generateInviteCode(
        elderlyId: String,
        elderlyName: String,
        elderlyEmail: String
    ): Result<InviteCode> {
        return try {
            // Desactivar códigos anteriores activos
            deactivateExistingCodes(elderlyId)

            // Generar nuevo código
            val code = InviteCodeGenerator.generateInviteCode()
            val codeId = InviteCodeGenerator.generateCodeId()

            val inviteCode = InviteCode(
                id = codeId,
                code = code,
                elderlyId = elderlyId,
                elderlyName = elderlyName,
                elderlyEmail = elderlyEmail
            )

            // Guardar en Firestore
            inviteCodesCollection.document(codeId).set(inviteCode).await()

            Log.d(TAG, "✅ Código de invitación generado: $code para $elderlyName")
            Result.success(inviteCode)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generando código de invitación", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene todos los códigos de un adulto mayor
     */
    suspend fun getElderlyInviteCodes(elderlyId: String): Result<List<InviteCode>> {
        return try {
            val snapshot = inviteCodesCollection
                .whereEqualTo("elderlyId", elderlyId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val codes = snapshot.documents.mapNotNull { doc ->
                doc.toObject(InviteCode::class.java)
            }

            Log.d(TAG, "✅ Códigos obtenidos: ${codes.size} para elderly $elderlyId")
            Result.success(codes)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo códigos", e)
            Result.failure(e)
        }
    }

    /**
     * Busca un código por su valor
     */
    suspend fun findCodeByValue(code: String): Result<InviteCode?> {
        return try {
            val cleanCode = InviteCodeGenerator.cleanCode(code)

            val snapshot = inviteCodesCollection
                .whereEqualTo("code", cleanCode)
                .whereEqualTo("active", true)
                .get()
                .await()

            val inviteCode = snapshot.documents.firstOrNull()?.toObject(InviteCode::class.java)

            Log.d(
                TAG,
                "🔍 Código buscado: $cleanCode - ${if (inviteCode != null) "✅ Encontrado" else "❌ No encontrado"}"
            )
            Result.success(inviteCode)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error buscando código", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ CORREGIDO: Marca un código como usado
     */
    suspend fun markCodeAsUsed(codeId: String, caregiverId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "usedAt" to System.currentTimeMillis(),
                "usedBy" to caregiverId,
                "active" to false
            )

            inviteCodesCollection.document(codeId).update(updates).await()

            Log.d(TAG, "✅ Código marcado como usado: $codeId por $caregiverId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marcando código como usado", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ CORREGIDO: Desactiva códigos existentes del adulto mayor
     */
    private suspend fun deactivateExistingCodes(elderlyId: String) {
        try {
            val snapshot = inviteCodesCollection
                .whereEqualTo("elderlyId", elderlyId)
                .whereEqualTo("active", true)
                .get()
                .await()

            val batch = db.batch()

            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "active", false)  // ✅ CORREGIDO
            }

            batch.commit().await()
            Log.d(TAG, "✅ Códigos anteriores desactivados para elderly $elderlyId (${snapshot.size()})")

        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error desactivando códigos anteriores", e)
        }
    }

    /* Elimina códigos expirados (limpieza) */
    suspend fun cleanupExpiredCodes(): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()

            val snapshot = inviteCodesCollection
                .whereLessThan("expiresAt", now)
                .get()
                .await()

            val batch = db.batch()

            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            batch.commit().await()
            Log.d(TAG, "Códigos expirados eliminados: ${snapshot.size()}")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando códigos expirados", e)
            Result.failure(e)
        }
    }
}