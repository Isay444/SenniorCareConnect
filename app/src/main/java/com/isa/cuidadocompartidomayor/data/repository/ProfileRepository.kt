package com.isa.cuidadocompartidomayor.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.graphics.Bitmap


class ProfileRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "ProfileRepository"
        private const val USERS_COLLECTION = "users"
    }

    /**
     * Obtiene los datos del perfil del usuario actual
     */
    suspend fun getUserProfile(): Result<Map<String, Any>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val document = db.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .get()
                .await()

            if (document.exists()) {
                Result.success(document.data ?: emptyMap())
            } else {
                Result.failure(Exception("Perfil no encontrado"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo perfil: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza la URL de la foto de perfil (Cloudinary)
     */
    suspend fun updateProfileImageUrl(userId: String, imageUrl: String): Result<Unit> {
        return try {
            db.collection(USERS_COLLECTION)
                .document(userId)
                .update("profileImageUrl", imageUrl)
                .await()
            Log.d(TAG, "✅ URL de imagen actualizada en Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando URL de imagen: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Elimina la foto de perfil en Firestore
     */
    suspend fun removeProfileImageUrl(userId: String): Result<Unit> {
        return try {
            db.collection(USERS_COLLECTION)
                .document(userId)
                .update("profileImageUrl", "")
                .await()
            Log.d(TAG, "✅ Imagen eliminada de Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando imagen: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza el nombre del usuario
     */
    suspend fun updateUserName(newName: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            db.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .update("name", newName)
                .await()

            Log.d(TAG, "✅ Nombre actualizado correctamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando nombre: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza el teléfono del usuario
     */
    suspend fun updateUserPhone(newPhone: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            db.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .update("phone", newPhone)
                .await()

            Log.d(TAG, "✅ Teléfono actualizado correctamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando teléfono: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza el contacto de emergencia
     */
    suspend fun updateEmergencyContact(newContact: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            db.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .update("emergencyContact", newContact)
                .await()

            Log.d(TAG, "✅ Contacto de emergencia actualizado")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando contacto de emergencia: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza la dirección del usuario
     */
    suspend fun updateUserAddress(newAddress: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            db.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .update("address", newAddress)
                .await()

            Log.d(TAG, "✅ Dirección actualizada correctamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando dirección: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza la fecha de nacimiento
     */
    suspend fun updateBirthDate(newBirthDate: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            db.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .update("birthDate", newBirthDate)
                .await()

            Log.d(TAG, "✅ Fecha de nacimiento actualizada")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando fecha de nacimiento: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza el correo electrónico del usuario
     * Requiere reautenticación reciente
     */
    suspend fun updateUserEmail(newEmail: String, currentPassword: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val currentEmail = currentUser.email
                ?: return Result.failure(Exception("No se pudo obtener el correo actual"))

            // Reautenticar usuario antes de cambiar email
            val credential = EmailAuthProvider.getCredential(currentEmail, currentPassword)
            currentUser.reauthenticate(credential).await()

            // Actualizar email en Authentication
            currentUser.updateEmail(newEmail).await()

            // Actualizar email en Firestore
            db.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .update("email", newEmail)
                .await()

            Log.d(TAG, "✅ Correo actualizado correctamente")
            Result.success(Unit)
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Log.e(TAG, "❌ Se requiere reautenticación", e)
            Result.failure(Exception("Por seguridad, vuelve a iniciar sesión para cambiar tu correo"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando correo: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Cambia la contraseña del usuario
     * Requiere la contraseña actual para verificación
     */
    suspend fun updateUserPassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val email = currentUser.email
                ?: return Result.failure(Exception("No se pudo obtener el correo"))

            // Reautenticar usuario
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            currentUser.reauthenticate(credential).await()

            // Actualizar contraseña
            currentUser.updatePassword(newPassword).await()

            Log.d(TAG, "✅ Contraseña actualizada correctamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando contraseña: ${e.message}", e)
            Result.failure(Exception("Contraseña actual incorrecta o error al actualizar"))
        }
    }

    /**
     * Cierra la sesión del usuario
     */
    fun signOut() {
        auth.signOut()
        Log.d(TAG, "✅ Sesión cerrada")
    }

    /**
     * Elimina la cuenta del usuario
     * Requiere reautenticación
     */
    suspend fun deleteAccount(currentPassword: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val email = currentUser.email
                ?: return Result.failure(Exception("No se pudo obtener el correo"))

            // Reautenticar usuario
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            currentUser.reauthenticate(credential).await()

            // Eliminar documento de Firestore
            db.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .delete()
                .await()

            // Eliminar cuenta de Authentication
            currentUser.delete().await()

            Log.d(TAG, "✅ Cuenta eliminada correctamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando cuenta: ${e.message}", e)
            Result.failure(e)
        }
    }
}
