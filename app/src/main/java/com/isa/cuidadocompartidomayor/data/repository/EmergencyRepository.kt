package com.isa.cuidadocompartidomayor.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.isa.cuidadocompartidomayor.data.model.EmergencyAlert
import com.isa.cuidadocompartidomayor.data.model.User
import kotlinx.coroutines.tasks.await

class EmergencyRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val connectionRepository = ConnectionRepository()

    companion object {
        private const val TAG = "EmergencyRepository"
        private const val EMERGENCY_ALERTS_COLLECTION = "emergencyAlerts"
        private const val USERS_COLLECTION = "users"
        private const val RELATIONSHIPS_COLLECTION = "relationships"
    }

    /**
     * Crea una nueva alerta de emergencia
     */
    suspend fun createEmergencyAlert( latitude: Double? = null, longitude: Double? = null ): Result<EmergencyAlert> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            Log.d(TAG, "🚨 Creando alerta de emergencia para: ${currentUser.uid}")

            // Obtener información del usuario elderly
            val elderlyUser = getUserById(currentUser.uid)
                ?: return Result.failure(Exception("Usuario no encontrado"))

            // Crear la alerta
            val alert = EmergencyAlert(
                elderlyId = elderlyUser.uid,
                elderlyName = elderlyUser.name,
                elderlyEmail = elderlyUser.email,
                latitude = latitude,
                longitude = longitude,
                timestamp = System.currentTimeMillis(),
                status = "active"
            )

            // Guardar en Firestore
            val documentRef = firestore.collection(EMERGENCY_ALERTS_COLLECTION)
                .add(alert)
                .await()

            val savedAlert = alert.copy(id = documentRef.id)

            // Actualizar con el ID
            documentRef.update("id", documentRef.id).await()

            Log.d(TAG, "✅ Alerta de emergencia creada: ${documentRef.id}")

            // Notificar a los cuidadores
            notifyAllCaregivers(savedAlert)

            Result.success(savedAlert)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando alerta de emergencia", e)
            Result.failure(e)
        }
    }

    /**
     * Notifica a todos los cuidadores conectados
     */
    private suspend fun notifyAllCaregivers(alert: EmergencyAlert) {
        try {
            // Obtener todos los cuidadores conectados
            val caregiversIds = getCaregiverIdsForElderly(alert.elderlyId)

            Log.d(TAG, "🔔 Notificando a ${caregiversIds.size} cuidadores")

            caregiversIds.forEach { caregiverId ->
                sendNotificationToCaregiver(caregiverId, alert)
            }

            // Actualizar la lista de cuidadores notificados
            firestore.collection(EMERGENCY_ALERTS_COLLECTION)
                .document(alert.id)
                .update("notifiedCaregivers", caregiversIds)
                .await()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error notificando cuidadores", e)
        }
    }

    /**
     * Obtiene los IDs de todos los cuidadores conectados al elderly
     */
    private suspend fun getCaregiverIdsForElderly(elderlyId: String): List<String> {
        return try {
            val snapshot = firestore.collection(RELATIONSHIPS_COLLECTION)
                .whereEqualTo("elderlyId", elderlyId)
                .whereEqualTo("status", "active")
                .get()
                .await()

            snapshot.documents.mapNotNull { it.getString("caregiverId") }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo cuidadores", e)
            emptyList()
        }
    }

    /**
     * Envía notificación a un cuidador específico
     */
    private suspend fun sendNotificationToCaregiver(caregiverId: String, alert: EmergencyAlert) {
        try {
            val notification = mapOf(
                "caregiverId" to caregiverId,
                "elderlyId" to alert.elderlyId,
                "elderlyName" to alert.elderlyName,
                "type" to "EMERGENCY",
                "title" to "🚨 EMERGENCIA DE ${alert.elderlyName}",
                "body" to buildNotificationMessage(alert),
                "latitude" to alert.latitude,
                "longitude" to alert.longitude,
                "alertId" to alert.id,
                "timestamp" to System.currentTimeMillis(),
                "read" to false
            )

            firestore.collection("caregiverNotifications")
                .add(notification)
                .await()

            Log.d(TAG, "✅ Notificación enviada a caregiver: $caregiverId")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando notificación", e)
        }
    }

    /**
     * Construye el mensaje de la notificación
     */
    private fun buildNotificationMessage(alert: EmergencyAlert): String {
        return if (alert.hasLocation()) {
            "¡${alert.elderlyName} ha activado una alerta de emergencia!\n\n📍 Ubicación: ${alert.getFormattedLocation()}"
        } else {
            "¡${alert.elderlyName} ha activado una alerta de emergencia!\n\n⚠️ Ubicación no disponible"
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

            document.toObject(User::class.java)?.copy(uid = document.id)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo usuario", e)
            null
        }
    }

    /**
     * Obtiene las últimas alertas del elderly
     */
    suspend fun getElderlyEmergencyAlerts(elderlyId: String, limit: Int = 10): Result<List<EmergencyAlert>> {
        return try {
            val snapshot = firestore.collection(EMERGENCY_ALERTS_COLLECTION)
                .whereEqualTo("elderlyId", elderlyId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val alerts = snapshot.documents.mapNotNull {
                it.toObject(EmergencyAlert::class.java)
            }

            Result.success(alerts)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo alertas", e)
            Result.failure(e)
        }
    }
    /**
     * Obtiene las notificaciones de emergencia del cuidador
     */
    suspend fun getCaregiverEmergencyNotifications(caregiverId: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = firestore.collection("caregiverNotifications")
                .whereEqualTo("caregiverId", caregiverId)
                .whereEqualTo("type", "EMERGENCY")
                .whereEqualTo("read", false)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val notifications = snapshot.documents.map { doc ->
                doc.data ?: emptyMap()
            }

            Log.d(TAG, "✅ ${notifications.size} notificaciones de emergencia encontradas")

            Result.success(notifications)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo notificaciones", e)
            Result.failure(e)
        }
    }

    /**
     * Marca una notificación como leída
     */
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            // Buscar el documento por alertId
            val snapshot = firestore.collection("caregiverNotifications")
                .whereEqualTo("alertId", notificationId)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                doc.reference.update("read", true).await()
            }

            Log.d(TAG, "✅ Notificación marcada como leída")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marcando notificación", e)
            Result.failure(e)
        }
    }

}