package com.isa.cuidadocompartidomayor.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.isa.cuidadocompartidomayor.data.model.User
import com.isa.cuidadocompartidomayor.utils.Constants
import kotlinx.coroutines.tasks.await

class AuthRepository{
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /** Registrar nuevo usuario con email y contraseña **/
    suspend fun registerUser(user: User, password: String): Result<User>{
        return try {
            Log.d("AuthRepository", "Iniciando registro para: ${user.email}")

            // Crear usuario en Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(user.email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Error al crear usuario")

            Log.d("AuthRepository", "Usuario creado en Auth: ${firebaseUser.uid}")

            // Crear documento del usuario en Firestore
            val userWithUid = user.copy(uid = firebaseUser.uid)

            Log.d("AuthRepository", "Guardando en Firestore: $userWithUid")

            // ✅ AGREGAR TIMEOUT Y LOGGING ESPECÍFICO
            try {
                firestore.collection(Constants.COLLECTION_USERS)
                    .document(firebaseUser.uid)
                    .set(userWithUid)
                    .await()

                Log.d("AuthRepository", "Usuario guardado exitosamente en Firestore")

            } catch (firestoreException: Exception) {
                Log.e("AuthRepository", "Error específico de Firestore: ${firestoreException.message}", firestoreException)
                throw firestoreException
            }

            Log.d("AuthRepository", "Registro completado exitosamente")
            Result.success(userWithUid)

        } catch (e: Exception) {
            Log.e("AuthRepository", "Error en registro: ${e.message}", e)
            Log.e("AuthRepository", "Tipo de error: ${e.javaClass.simpleName}")
            Log.e("AuthRepository", "Mensaje completo: ${e.localizedMessage}")
            Result.failure(e)
        }
    }


    /** Inicia sesión con email y contraseña **/
    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Error al iniciar sesión")

            val userDoc = firestore.collection(Constants.COLLECTION_USERS)
                .document(firebaseUser.uid)
                .get()
                .await()

            val user = userDoc.toObject(User::class.java) ?: throw Exception("Usuario no encontrado")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    fun logoutUser() = auth.signOut()
    fun getCurrentUser() = auth.currentUser
    fun isUserLoggedIn() = auth.currentUser != null
}