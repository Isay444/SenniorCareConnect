package com.isa.cuidadocompartidomayor.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.isa.cuidadocompartidomayor.data.model.CaregiverRequest
import kotlinx.coroutines.tasks.await

class CaregiverRequestRepository {

    private val db = FirebaseFirestore.getInstance()
    private val caregiverRequestsCollection = db.collection("caregiverRequests")
    private val relationshipsCollection = db.collection("relationships")

    companion object {
        private const val TAG = "CaregiverRequestRepository"
    }

    /**
     * Crea una nueva solicitud de cuidador
     */
    suspend fun createCaregiverRequest(
        caregiverId: String,
        elderlyId: String,
        caregiverName: String,
        caregiverEmail: String,
        caregiverMessage: String = "",
        inviteCodeUsed: String? = null
    ): Result<CaregiverRequest> {
        return try {
            // Verificar si ya existe una solicitud activa
            val existingRequest = checkExistingRequest(caregiverId, elderlyId)
            if (existingRequest != null) {
                return Result.failure(Exception("Ya existe una solicitud activa entre estos usuarios"))
            }

            // Generar ID único
            val requestId = caregiverRequestsCollection.document().id
            val caregiverRequest = CaregiverRequest(
                id = requestId,
                caregiverId = caregiverId,
                elderlyId = elderlyId,
                caregiverName = caregiverName,
                caregiverEmail = caregiverEmail,
                caregiverMessage = caregiverMessage,
                inviteCodeUsed = inviteCodeUsed
            )

            // Guardar en Firestore
            caregiverRequestsCollection.document(requestId).set(caregiverRequest).await()
            Log.d(TAG, "Solicitud de cuidador creada: $requestId")
            Result.success(caregiverRequest)
        } catch (e: Exception) {
            Log.e(TAG, "Error creando solicitud de cuidador", e)
            Result.failure(e)
        }
    }

    /* Obtiene todas las solicitudes de un adulto mayor */
    suspend fun getElderlyRequests(elderlyId: String): Result<List<CaregiverRequest>> {
        return try {
            val snapshot = caregiverRequestsCollection
                .whereEqualTo("elderlyId", elderlyId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val requests = snapshot.documents.mapNotNull { doc ->
                doc.toObject(CaregiverRequest::class.java)
            }

            Log.d(TAG, "Solicitudes obtenidas para elderly $elderlyId: ${requests.size}")
            Result.success(requests)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo solicitudes del adulto mayor", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene solo las solicitudes pendientes de un adulto mayor
     */
    suspend fun getPendingRequests(elderlyId: String): Result<List<CaregiverRequest>> {
        return try {
            Log.d(TAG, "🔍 Buscando solicitudes pendientes para elderlyId: $elderlyId")

            val snapshot = caregiverRequestsCollection
                .whereEqualTo("elderlyId", elderlyId)
                .whereEqualTo("status", "PENDING")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val requests = snapshot.documents.mapNotNull { doc ->
                doc.toObject(CaregiverRequest::class.java)
            }

            // Enriquecer con fotos de perfil
            val enrichedRequests = enrichWithCaregiverPhotos(requests)

            Log.d(TAG, "✅ Solicitudes pendientes encontradas: ${enrichedRequests.size}")
            Result.success(enrichedRequests)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo solicitudes pendientes", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene los cuidadores ya aprobados de un adulto mayor
     */
    suspend fun getApprovedCaregivers(elderlyId: String): Result<List<CaregiverRequest>> {
        return try {
            val snapshot = caregiverRequestsCollection
                .whereEqualTo("elderlyId", elderlyId)
                .whereEqualTo("status", "APPROVED")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val requests = snapshot.documents.mapNotNull { doc ->
                doc.toObject(CaregiverRequest::class.java)
            }

            // Enriquecer con fotos de perfil
            val enrichedRequests = enrichWithCaregiverPhotos(requests)

            Log.d(TAG, "✅ Cuidadores aprobados: ${enrichedRequests.size}")
            Result.success(enrichedRequests)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo cuidadores aprobados", e)
            Result.failure(e)
        }
    }

    /**
     * Auxiliar para obtener fotos de perfil de los cuidadores
     */
    private suspend fun enrichWithCaregiverPhotos(requests: List<CaregiverRequest>): List<CaregiverRequest> {
        val caregiverIds = requests.map { it.caregiverId }.distinct().filter { it.isNotEmpty() }
        
        if (caregiverIds.isEmpty()) return requests

        return try {
            val usersSnapshot = db.collection("users")
                .whereIn(FieldPath.documentId(), caregiverIds)
                .get()
                .await()

            val photoMap = usersSnapshot.documents.associate { doc ->
                doc.id to (doc.getString("profileImageUrl") ?: "")
            }

            requests.map { request ->
                request.copy(caregiverProfileImageUrl = photoMap[request.caregiverId] ?: "")
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ No se pudieron obtener las fotos de los cuidadores", e)
            requests
        }
    }

    /**
     *  CORREGIDO: Aprueba una solicitud actualizando el Relationship existente
     */
    suspend fun approveRequest(requestId: String): Result<CaregiverRequest> {
        return try {
            val requestDoc = caregiverRequestsCollection.document(requestId)
            val snapshot = requestDoc.get().await()
            val request = snapshot.toObject(CaregiverRequest::class.java)
                ?: return Result.failure(Exception("Solicitud no encontrada"))

            if (request.status != CaregiverRequest.RequestStatus.PENDING) {
                return Result.failure(Exception("La solicitud ya fue procesada"))
            }

            // 1. Actualizar estado de la solicitud
            val updatedRequest = request.withStatus(CaregiverRequest.RequestStatus.APPROVED)
            requestDoc.set(updatedRequest).await()

            // 2.  ACTUALIZAR el Relationship existente (NO crear uno nuevo)
            updateExistingRelationship(request.caregiverId, request.elderlyId)

            Log.d(TAG, "✅ Solicitud aprobada y relationship actualizado: $requestId")
            Result.success(updatedRequest)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error aprobando solicitud", e)
            Result.failure(e)
        }
    }

    /**
     *  CORREGIDO: Rechaza una solicitud y ELIMINA el Relationship pendiente
     */
    suspend fun rejectRequest(requestId: String): Result<CaregiverRequest> {
        return try {
            val requestDoc = caregiverRequestsCollection.document(requestId)
            val snapshot = requestDoc.get().await()
            val request = snapshot.toObject(CaregiverRequest::class.java)
                ?: return Result.failure(Exception("Solicitud no encontrada"))

            if (request.status != CaregiverRequest.RequestStatus.PENDING) {
                return Result.failure(Exception("La solicitud ya fue procesada"))
            }

            // 1. Actualizar estado de la solicitud a REJECTED
            val updatedRequest = request.withStatus(CaregiverRequest.RequestStatus.REJECTED)
            requestDoc.set(updatedRequest).await()

            // 2.  ELIMINAR el Relationship pendiente
            deleteRelationship(request.caregiverId, request.elderlyId)

            Log.d(TAG, "✅ Solicitud rechazada y relationship eliminado: $requestId")
            Result.success(updatedRequest)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error rechazando solicitud", e)
            Result.failure(e)
        }
    }

    /**
     * Elimina una relación entre cuidador y adulto mayor
     */
    suspend fun removeCaregiver(caregiverId: String, elderlyId: String): Result<Unit> {
        return try {
            // Buscar y eliminar la relación
            val relationshipSnapshot = relationshipsCollection
                .whereEqualTo("caregiverId", caregiverId)
                .whereEqualTo("elderlyId", elderlyId)
                .get()
                .await()

            val batch = db.batch()
            relationshipSnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            // También actualizar la solicitud correspondiente si existe
            val requestSnapshot = caregiverRequestsCollection
                .whereEqualTo("caregiverId", caregiverId)
                .whereEqualTo("elderlyId", elderlyId)
                .whereEqualTo("status", "APPROVED")
                .get()
                .await()

            requestSnapshot.documents.forEach { doc ->
                val updatedRequest = doc.toObject(CaregiverRequest::class.java)?.copy(
                    status = CaregiverRequest.RequestStatus.REJECTED,
                    updatedAt = System.currentTimeMillis()
                )

                if (updatedRequest != null) {
                    batch.set(doc.reference, updatedRequest)
                }
            }

            batch.commit().await()
            Log.d(TAG, "Cuidador removido: $caregiverId de elderly $elderlyId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removiendo cuidador", e)
            Result.failure(e)
        }
    }

    /**
     *  NUEVO: Actualiza el Relationship existente en lugar de crear uno nuevo
     */
    private suspend fun updateExistingRelationship(caregiverId: String, elderlyId: String) {
        try {
            // Buscar el relationship existente con status "pending"
            val relationshipSnapshot = relationshipsCollection
                .whereEqualTo("caregiverId", caregiverId)
                .whereEqualTo("elderlyId", elderlyId)
                .whereEqualTo("status", "pending")
                .get()
                .await()

            if (relationshipSnapshot.documents.isEmpty()) {
                Log.w(TAG, "⚠️ No se encontró relationship pendiente para actualizar")
                return
            }

            // Actualizar el primer (y debería ser único) documento encontrado
            val relationshipDoc = relationshipSnapshot.documents.first()
            relationshipDoc.reference.update(
                mapOf(
                    "status" to "active",
                    "approvedAt" to System.currentTimeMillis()
                )
            ).await()

            Log.d(TAG, "✅ Relationship actualizado a 'active': ${relationshipDoc.id}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando relationship", e)
            throw e
        }
    }

    /**
     *  NUEVO: Elimina el Relationship cuando se rechaza una solicitud
     */
    private suspend fun deleteRelationship(caregiverId: String, elderlyId: String) {
        try {
            val relationshipSnapshot = relationshipsCollection
                .whereEqualTo("caregiverId", caregiverId)
                .whereEqualTo("elderlyId", elderlyId)
                .get()
                .await()

            val batch = db.batch()
            relationshipSnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
                Log.d(TAG, "🗑️ Relationship eliminado: ${doc.id}")
            }
            batch.commit().await()

            Log.d(TAG, "✅ Relationships eliminados: ${relationshipSnapshot.size()}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando relationship", e)
            throw e
        }
    }

    /**
     * Verifica si ya existe una solicitud entre cuidador y adulto mayor
     */
    private suspend fun checkExistingRequest(
        caregiverId: String,
        elderlyId: String
    ): CaregiverRequest? {
        return try {
            val snapshot = caregiverRequestsCollection
                .whereEqualTo("caregiverId", caregiverId)
                .whereEqualTo("elderlyId", elderlyId)
                .whereIn("status", listOf(
                    CaregiverRequest.RequestStatus.PENDING.name,
                    CaregiverRequest.RequestStatus.APPROVED.name
                ))
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(CaregiverRequest::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Error verificando solicitud existente", e)
            null
        }
    }

    /**
     * Cuenta las solicitudes pendientes de un adulto mayor
     */
    suspend fun countPendingRequests(elderlyId: String): Result<Int> {
        return try {
            val snapshot = caregiverRequestsCollection
                .whereEqualTo("elderlyId", elderlyId)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()

            val count = snapshot.size()
            Log.d(TAG, "Solicitudes pendientes para elderly $elderlyId: $count")
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error contando solicitudes pendientes", e)
            Result.failure(e)
        }
    }

    /* Obtiene una solicitud específica por ID */
    suspend fun getRequestById(requestId: String): Result<CaregiverRequest?> {
        return try {
            val snapshot = caregiverRequestsCollection.document(requestId).get().await()
            val request = snapshot.toObject(CaregiverRequest::class.java)
            Log.d(TAG, "Solicitud obtenida por ID: $requestId")
            Result.success(request)
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo solicitud por ID", e)
            Result.failure(e)
        }
    }
}
