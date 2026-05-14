package com.isa.cuidadocompartidomayor.ui.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.isa.cuidadocompartidomayor.data.repository.ProfileRepository
import com.isa.cuidadocompartidomayor.utils.ImageUploadHelper
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repository = ProfileRepository()

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    // LiveData para datos del perfil
    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> = _userEmail

    private val _userRole = MutableLiveData<String>()
    val userRole: LiveData<String> = _userRole

    private val _userPhone = MutableLiveData<String>()
    val userPhone: LiveData<String> = _userPhone

    private val _emergencyContact = MutableLiveData<String>()
    val emergencyContact: LiveData<String> = _emergencyContact

    private val _userAddress = MutableLiveData<String>()
    val userAddress: LiveData<String> = _userAddress

    private val _birthDate = MutableLiveData<String>()
    val birthDate: LiveData<String> = _birthDate

    private val _profileImageUrl = MutableLiveData<String>()
    val profileImageUrl: LiveData<String> = _profileImageUrl


    // LiveData para estados
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isUploadingImage = MutableLiveData<Boolean>()
    val isUploadingImage: LiveData<Boolean> = _isUploadingImage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _uploadError = MutableLiveData<String?>()
    val uploadError: LiveData<String?> = _uploadError

    /**
     * Carga los datos del perfil del usuario
     */
    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "📥 Cargando perfil del usuario...")

                val result = repository.getUserProfile()
                result.onSuccess { data ->
                    _userName.value = data["name"] as? String ?: "Usuario"
                    _userEmail.value = data["email"] as? String ?: "correo@ejemplo.com"
                    _userRole.value = data["userType"] as? String ?: "caregiver"
                    _userPhone.value = data["phone"] as? String ?: ""
                    _emergencyContact.value = data["emergencyContact"] as? String ?: ""
                    _userAddress.value = data["address"] as? String ?: ""
                    _birthDate.value = data["birthDate"] as? String ?: ""
                    _profileImageUrl.value = data["profileImageUrl"] as? String ?: ""
                    Log.d(TAG, "✅ Perfil cargado correctamente")
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                    Log.e(TAG, "❌ Error: ${exception.message}")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualiza el nombre del usuario
     */
    fun updateName(newName: String) {
        if (newName.isBlank()) {
            _errorMessage.value = "El nombre no puede estar vacío"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "📝 Actualizando nombre...")

                val result = repository.updateUserName(newName)
                result.onSuccess {
                    _userName.value = newName
                    _successMessage.value = "Nombre actualizado correctamente"
                    Log.d(TAG, "✅ Nombre actualizado")
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                    Log.e(TAG, "❌ Error: ${exception.message}")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualiza el teléfono
     */
    fun updatePhone(newPhone: String) {
        if (newPhone.isBlank()) {
            _errorMessage.value = "El teléfono no puede estar vacío"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.updateUserPhone(newPhone)
                result.onSuccess {
                    _userPhone.value = newPhone
                    _successMessage.value = "Teléfono actualizado"
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualiza el contacto de emergencia
     */
    fun updateEmergencyContact(newContact: String) {
        if (newContact.isBlank()) {
            _errorMessage.value = "El contacto de emergencia no puede estar vacío"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.updateEmergencyContact(newContact)
                result.onSuccess {
                    _emergencyContact.value = newContact
                    _successMessage.value = "Contacto de emergencia actualizado"
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualiza la dirección
     */
    fun updateAddress(newAddress: String) {
        if (newAddress.isBlank()) {
            _errorMessage.value = "La dirección no puede estar vacía"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.updateUserAddress(newAddress)
                result.onSuccess {
                    _userAddress.value = newAddress
                    _successMessage.value = "Dirección actualizada"
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualiza la fecha de nacimiento
     */
    fun updateBirthDate(newBirthDate: String) {
        if (newBirthDate.isBlank()) {
            _errorMessage.value = "La fecha de nacimiento no puede estar vacía"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.updateBirthDate(newBirthDate)
                result.onSuccess {
                    _birthDate.value = newBirthDate
                    _successMessage.value = "Fecha de nacimiento actualizada"
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualiza el correo electrónico
     */
    fun updateEmail(newEmail: String, currentPassword: String) {
        if (newEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            _errorMessage.value = "Correo electrónico inválido"
            return
        }

        if (currentPassword.isBlank()) {
            _errorMessage.value = "Debes ingresar tu contraseña actual"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "📧 Actualizando correo...")

                val result = repository.updateUserEmail(newEmail, currentPassword)
                result.onSuccess {
                    _userEmail.value = newEmail
                    _successMessage.value = "Correo actualizado correctamente"
                    Log.d(TAG, "✅ Correo actualizado")
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                    Log.e(TAG, "❌ Error: ${exception.message}")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cambia la contraseña
     */
    fun updatePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        if (currentPassword.isBlank()) {
            _errorMessage.value = "Debes ingresar tu contraseña actual"
            return
        }

        if (newPassword.length < 6) {
            _errorMessage.value = "La nueva contraseña debe tener al menos 6 caracteres"
            return
        }

        if (newPassword != confirmPassword) {
            _errorMessage.value = "Las contraseñas no coinciden"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "🔒 Actualizando contraseña...")

                val result = repository.updateUserPassword(currentPassword, newPassword)
                result.onSuccess {
                    _successMessage.value = "Contraseña actualizada correctamente"
                    Log.d(TAG, "✅ Contraseña actualizada")
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                    Log.e(TAG, "❌ Error: ${exception.message}")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Elimina la cuenta del usuario
     */
    fun deleteAccount(currentPassword: String, onSuccess: () -> Unit) {
        if (currentPassword.isBlank()) {
            _errorMessage.value = "Debes ingresar tu contraseña para eliminar la cuenta"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "🗑️ Eliminando cuenta...")

                val result = repository.deleteAccount(currentPassword)
                result.onSuccess {
                    _successMessage.value = "Cuenta eliminada correctamente"
                    Log.d(TAG, "✅ Cuenta eliminada")
                    onSuccess()
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                    Log.e(TAG, "❌ Error: ${exception.message}")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }


    /**
     * Sube una imagen a Cloudinary y guarda la URL en Firestore
     */
    fun uploadAndSaveProfileImage(context: Context, imageUri: Uri) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _isUploadingImage.value = true
                _uploadError.value = null
                Log.d(TAG, "📸 Iniciando subida a Cloudinary...")

                val uploadResult = ImageUploadHelper.uploadProfileImage(context, imageUri)
                
                uploadResult.onSuccess { imageUrl ->
                    Log.d(TAG, "✅ Imagen subida, guardando URL en Firestore...")
                    val saveResult = repository.updateProfileImageUrl(userId, imageUrl)
                    
                    saveResult.onSuccess {
                        _profileImageUrl.value = imageUrl
                        _successMessage.value = "Foto de perfil actualizada"
                    }.onFailure { exception ->
                        _uploadError.value = "Error al guardar en perfil: ${exception.message}"
                    }
                }.onFailure { exception ->
                    _uploadError.value = "Error al subir imagen: ${exception.message}"
                    Log.e(TAG, "❌ Error Cloudinary: ${exception.message}")
                }
            } finally {
                _isUploadingImage.value = false
            }
        }
    }

    /**
     * Elimina la foto de perfil del usuario
     */
    fun removeProfileImage(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.removeProfileImageUrl(userId)
                result.onSuccess {
                    _profileImageUrl.value = ""
                    _successMessage.value = "Foto de perfil eliminada"
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cierra la sesión
     */
    fun signOut() {
        repository.signOut()
        Log.d(TAG, "✅ Sesión cerrada")
    }

    /**
     * Limpia los mensajes
     */
    fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }
}