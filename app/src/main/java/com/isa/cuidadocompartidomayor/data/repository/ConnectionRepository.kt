package com.isa.cuidadocompartidomayor.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.isa.cuidadocompartidomayor.data.model.Relationship
import com.isa.cuidadocompartidomayor.data.model.User
import kotlinx.coroutines.tasks.await
class ConnectionRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val caregiverRequestRepository = CaregiverRequestRepository()

    companion object {
        private const val TAG = "ConnectionRepository"
        private const val USERS_COLLECTION = "users"
        private const val RELATIONSHIPS_COLLECTION = "relationships"
    }

    /**
     * Conecta un cuidador con un adulto mayor usando código de invitación
     */
    suspend fun connectToElderly(inviteCode: String): Result<Relationship> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            Log.d(TAG, "🔵 PASO 1: Iniciando conexión con código: $inviteCode")

            // 1. Buscar adulto mayor por código de invitación
            val inviteCodeRepo = InviteCodeRepository()
            val elderlyUser = findElderlyByInviteCode(inviteCode, inviteCodeRepo)
                ?: return Result.failure(Exception("Código de invitación inválido o expirado"))

            Log.d(TAG, "🔵 PASO 2: Elderly encontrado: ${elderlyUser.name} (${elderlyUser.uid})")

            // 2. Verificar que el usuario actual sea cuidador
            val caregiverUser = getUserById(currentUser.uid)
                ?: return Result.failure(Exception("Usuario cuidador no encontrado"))

            if (caregiverUser.userType != "caregiver") {
                return Result.failure(Exception("Solo los cuidadores pueden conectarse con adultos mayores"))
            }

            Log.d(TAG, "🔵 PASO 3: Caregiver verificado: ${caregiverUser.name} (${caregiverUser.uid})")

            // 3. Verificar que no exista ya una relación
            val existingRelation = checkExistingRelationship(caregiverUser.uid, elderlyUser.uid)
            if (existingRelation != null) {
                return Result.failure(Exception("Ya existe una conexión con este adulto mayor"))
            }

            // 4. Crear CaregiverRequest
            Log.d(TAG, "🔵 PASO 4: Creando CaregiverRequest...")
            val requestResult = caregiverRequestRepository.createCaregiverRequest(
                caregiverId = caregiverUser.uid,
                elderlyId = elderlyUser.uid,
                caregiverName = caregiverUser.name,
                caregiverEmail = caregiverUser.email,
                caregiverMessage = "Solicitud de conexión mediante código QR",
                inviteCodeUsed = inviteCode
            )

            if (requestResult.isFailure) {
                Log.e(TAG, "❌ Error creando CaregiverRequest: ${requestResult.exceptionOrNull()?.message}")
                return Result.failure(requestResult.exceptionOrNull() ?: Exception("Error al crear solicitud"))
            }

            Log.d(TAG, "✅ PASO 5: CaregiverRequest creado: ${requestResult.getOrNull()?.id}")

            // 5. Crear Relationship
            Log.d(TAG, "🔵 PASO 6: Creando Relationship...")
            val relationship = createRelationship(caregiverUser, elderlyUser)
            Log.d(TAG, "✅ PASO 7: Relationship creado: ${relationship.id}")

            // 6. Marcar código como usado
            Log.d(TAG, "🔵 PASO 8: Marcando código como usado...")
            val inviteCodeDoc = inviteCodeRepo.findCodeByValue(inviteCode).getOrNull()
            if (inviteCodeDoc != null) {
                val markResult = inviteCodeRepo.markCodeAsUsed(inviteCodeDoc.id, caregiverUser.uid)
                if (markResult.isSuccess) {
                    Log.d(TAG, "✅ PASO 9: Código marcado como usado: ${inviteCodeDoc.code}")
                } else {
                    Log.w(TAG, "⚠️ No se pudo marcar el código como usado (no crítico)")
                }
            } else {
                Log.w(TAG, "⚠️ No se encontró el código para marcar como usado")
            }

            Log.d(TAG, "✅ CONEXIÓN COMPLETA EXITOSA")
            Result.success(relationship)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al conectar: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Busca un adulto mayor usando el código de invitación
     */
    private suspend fun findElderlyByInviteCode(
        inviteCode: String,
        inviteCodeRepo: InviteCodeRepository
    ): User? {
        return try {
            // Buscar código en la colección de inviteCodes
            val codeResult = inviteCodeRepo.findCodeByValue(inviteCode)
            val code = codeResult.getOrNull()

            if (code == null || !code.isValid()) {
                Log.w(TAG, "❌ Código inválido o no encontrado")
                return null
            }

            // Obtener usuario elderly usando el elderlyId del código
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(code.elderlyId)
                .get()
                .await()

            val elderlyUser = userDoc.toObject(User::class.java)

            if (elderlyUser != null) {
                Log.d(TAG, "✅ Elderly encontrado: ${elderlyUser.name}")
            }

            elderlyUser

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error buscando elderly por código: ${e.message}", e)
            null
        }
    }

    /* Busca un adulto mayor por su código de invitación */
    private suspend fun findElderlyByInviteCode(inviteCode: String): User? {
        return try {
            // Crear instancia del InviteCodeRepository
            val inviteCodeRepository = InviteCodeRepository()

            // 1. Buscar el código en la colección inviteCodes
            val codeResult = inviteCodeRepository.findCodeByValue(inviteCode)
            val inviteCodeObj = codeResult.getOrNull()
                ?: return null  // Código no encontrado o inválido

            // 2. Verificar que el código esté activo y no expirado
            if (!inviteCodeObj.active || !inviteCodeObj.isValid()) {
                Log.w(TAG, "Código inválido o expirado: $inviteCode")
                return null
            }

            // 3. Obtener el usuario elderly por su ID
            val elderlyUser = getUserById(inviteCodeObj.elderlyId)

            // 4. Marcar el código como usado (opcional, si quieres marcarlo aquí)
            if (elderlyUser != null) {
                val currentUserId = auth.currentUser?.uid ?: ""
                inviteCodeRepository.markCodeAsUsed(inviteCodeObj.id, currentUserId)
            }

            Log.d(TAG, "✅ Elderly encontrado: ${elderlyUser?.name}")
            elderlyUser

        } catch (e: Exception) {
            Log.e(TAG, "Error buscando adulto mayor: ${e.message}", e)
            null
        }
    }


    /**
     * Obtiene un usuario por su ID
     */
    private suspend fun getUserById(userId: String): User? {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                document.toObject(User::class.java)?.copy(uid = document.id)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo usuario: ${e.message}", e)
            null
        }
    }

    /**
     * Verifica si ya existe una relación entre cuidador y adulto mayor
     */
    private suspend fun checkExistingRelationship(caregiverId: String, elderlyId: String): Relationship? {
        return try {
            val querySnapshot = firestore.collection(RELATIONSHIPS_COLLECTION)
                .whereEqualTo("caregiverId", caregiverId)
                .whereEqualTo("elderlyId", elderlyId)
                .limit(1)
                .get()
                .await()

            if (querySnapshot.documents.isNotEmpty()) {
                val document = querySnapshot.documents[0]
                document.toObject(Relationship::class.java)?.copy(id = document.id)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando relación existente: ${e.message}", e)
            null
        }
    }

    /**
     * Crea una nueva relación entre cuidador y adulto mayor
     */
    private suspend fun createRelationship(caregiver: User, elderly: User): Relationship {
        val relationship = Relationship(
            elderlyId = elderly.uid,
            elderlyName = elderly.name,
            elderlyEmail = elderly.email,
            caregiverId = caregiver.uid,
            caregiverName = caregiver.name,
            caregiverEmail = caregiver.email,
            status = "pending",
            role = "secondary",
            permissions = listOf("read"),
            createdAt = System.currentTimeMillis(),
            notes = "Conexión solicitada via código de invitación"
        )

        val documentRef = firestore.collection(RELATIONSHIPS_COLLECTION).add(relationship).await()

        // ✅ AGREGAR ESTA LÍNEA:
        documentRef.update("id", documentRef.id).await()

        Log.d(TAG, "✅ Relación creada con ID: ${documentRef.id}")

        return relationship.copy(id = documentRef.id)
    }

    /**
     * Obtiene todas las relaciones de un cuidador
     */
    suspend fun getCaregiverRelationships(caregiverId: String): Result<List<Relationship>> {
        return try {
            val querySnapshot = firestore.collection(RELATIONSHIPS_COLLECTION)
                .whereEqualTo("caregiverId", caregiverId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val relationships = querySnapshot.documents.mapNotNull { document ->
                document.toObject(Relationship::class.java)?.copy(id = document.id)
            }

            Log.d(TAG, "Relaciones del cuidador cargadas: ${relationships.size}")
            Result.success(relationships)

        } catch (e: Exception) {
            Log.e(TAG, "Error cargando relaciones: ${e.message}", e)
            Result.failure(e)
        }
    }
}
