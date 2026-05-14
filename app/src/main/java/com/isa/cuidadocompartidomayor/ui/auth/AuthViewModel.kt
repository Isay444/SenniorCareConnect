package com.isa.cuidadocompartidomayor.ui.auth

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isa.cuidadocompartidomayor.data.model.User
import com.isa.cuidadocompartidomayor.data.repository.AuthRepository
import com.isa.cuidadocompartidomayor.utils.Constants
import kotlinx.coroutines.launch

class AuthViewModel: ViewModel() {

    private val authRepository = AuthRepository()

    // Estados para la UI
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _authResult = MutableLiveData<Result<User>?>()
    val authResult: LiveData<Result<User>?> = _authResult

    private val _validationError = MutableLiveData<String?>()
    val validationError: LiveData<String?> = _validationError

    /**
     * Registra un nuevo usuario
     */
    fun registerUser(name: String, email: String, password: String, userType: String) {
        if (!validateRegistrationInput(name, email, password)) return

        _loading.value = true
        viewModelScope.launch {
            try {
                val user = User(
                    name = name.trim(),
                    email = email.trim().lowercase(),
                    userType = userType
                )

                Log.d("AuthViewModel", "Intentando registrar usuario: ${user.email}")

                val result = authRepository.registerUser(user, password)

                // ✅ LOGGING PARA DEBUG
                Log.d("AuthViewModel", "Resultado de registro: isSuccess=${result.isSuccess}")
                if (result.isFailure) {
                    Log.e("AuthViewModel", "Error específico: ${result.exceptionOrNull()?.message}")
                    Log.e("AuthViewModel", "Stack trace: ", result.exceptionOrNull())
                }

                // ✅ ASIGNAR RESULTADO ANTES DE PROCESAR
                _authResult.value = result

                // ✅ PROCESAR RESULTADO (SIN DUPLICAR)
                if (result.isSuccess) {
                    Log.d("AuthViewModel", "Registro exitoso para: ${user.email}")
                    // El RegisterFragment manejará la navegación
                } else {
                    Log.e("AuthViewModel", "Error en registro: ${result.exceptionOrNull()?.message}")
                    _validationError.value = getFirebaseErrorMessage(result.exceptionOrNull())
                }

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Excepción en registerUser: ${e.message}", e)
                _authResult.value = Result.failure(e)
                _validationError.value = "Error inesperado: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Inicia sesión
     */
    fun loginUser(email: String, password: String) {
        if (!validateLoginInput(email, password)) return

        _loading.value = true
        viewModelScope.launch {
            try {
                val result = authRepository.loginUser(email.trim().lowercase(), password)
                _authResult.value = result

                if (result.isFailure) {
                    _validationError.value = getFirebaseErrorMessage(result.exceptionOrNull())
                }
            } catch (e: Exception) {
                _authResult.value = Result.failure(e)
                _validationError.value = "Error inesperado: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    private fun validateRegistrationInput(name: String, email: String, password: String): Boolean {
        when {
            name.trim().isEmpty() -> {
                _validationError.value = "El nombre es requerido"
                return false
            }
            name.trim().length < 2 -> {
                _validationError.value = "El nombre debe tener al menos 2 caracteres"
                return false
            }
            email.trim().isEmpty() -> {
                _validationError.value = "El email es requerido"
                return false
            }
            !isValidEmail(email.trim()) -> {
                _validationError.value = "Ingresa un email válido"
                return false
            }
            password.isEmpty() -> {
                _validationError.value = "La contraseña es requerida"
                return false
            }
            password.length < Constants.MIN_PASSWORD_LENGTH -> {
                _validationError.value = "La contraseña debe tener al menos ${Constants.MIN_PASSWORD_LENGTH} caracteres"
                return false
            }
        }
        return true
    }

    private fun validateLoginInput(email: String, password: String): Boolean {
        when {
            email.trim().isEmpty() -> {
                _validationError.value = "El email es requerido"
                return false
            }
            !isValidEmail(email.trim()) -> {
                _validationError.value = "Ingresa un email válido"
                return false
            }
            password.isEmpty() -> {
                _validationError.value = "La contraseña es requerida"
                return false
            }
        }
        return true
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun getFirebaseErrorMessage(exception: Throwable?): String {
        return when (exception?.message) {
            "The email address is already in use by another account." ->
                "Este email ya está registrado"
            "The password is invalid or the user does not have a password." ->
                "Contraseña incorrecta"
            "There is no user record corresponding to this identifier. The user may have been deleted." ->
                "No existe una cuenta con este email"
            else -> exception?.message ?: "Error desconocido"
        }
    }

    fun clearErrors() {
        _validationError.value = null
        _authResult.value = null
    }

    fun checkAuthState() = authRepository.isUserLoggedIn()
}
