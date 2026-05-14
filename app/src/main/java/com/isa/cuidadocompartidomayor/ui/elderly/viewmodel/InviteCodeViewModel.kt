package com.isa.cuidadocompartidomayor.ui.elderly.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.isa.cuidadocompartidomayor.data.model.InviteCode
import com.isa.cuidadocompartidomayor.data.repository.InviteCodeRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class InviteCodeViewModel : ViewModel() {

    private val repository = InviteCodeRepository()
    private val auth = FirebaseAuth.getInstance()

    // Estados para la UI
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _currentInviteCode = MutableLiveData<InviteCode?>()
    val currentInviteCode: LiveData<InviteCode?> = _currentInviteCode

    private val _allInviteCodes = MutableLiveData<List<InviteCode>>()
    val allInviteCodes: LiveData<List<InviteCode>> = _allInviteCodes

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    companion object {
        private const val TAG = "InviteCodeViewModel"
    }

    /**
     * Genera un nuevo código de invitación
     */
    fun generateNewInviteCode() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _errorMessage.value = "Usuario no autenticado"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {

                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val userDoc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    val userName = userDoc.getString("name") ?: "Usuario"

                    val result = repository.generateInviteCode(
                        elderlyId = currentUser.uid,
                        elderlyName = userName,
                        elderlyEmail = currentUser.email ?: ""
                    )

                    if (result.isSuccess) {
                        val newCode = result.getOrNull()
                        _currentInviteCode.value = newCode
                        _successMessage.value = "¡Código generado exitosamente!"

                        // Recargar lista completa
                        loadAllInviteCodes()

                        Log.d(TAG, "Código generado: ${newCode?.code}")
                    } else {
                        _errorMessage.value = "Error al generar código: ${result.exceptionOrNull()?.message}"
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error generando código", e)
                _errorMessage.value = "Error inesperado al generar código"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Carga todos los códigos de invitación del usuario */
    fun loadAllInviteCodes() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _errorMessage.value = "Usuario no autenticado"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = repository.getElderlyInviteCodes(currentUser.uid)

                if (result.isSuccess) {
                    val codes = result.getOrNull() ?: emptyList()
                    _allInviteCodes.value = codes

                    // ✅ CAMBIO AQUÍ: Solo actualizar si NO hay código actual
                    if (_currentInviteCode.value == null || _currentInviteCode.value?.active == false) {
                        // Encontrar el código más reciente y activo
                        val activeCode = codes.firstOrNull { it.active }
                        _currentInviteCode.value = activeCode
                    }

                    Log.d(TAG, "Códigos cargados: ${codes.size}")
                } else {
                    _errorMessage.value = "Error al cargar códigos: ${result.exceptionOrNull()?.message}"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error cargando códigos", e)
                _errorMessage.value = "Error inesperado al cargar códigos"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Obtiene el texto para compartir el código */
    fun getShareText(inviteCode: InviteCode): String {
        return """
            👥 Invitación de Cuidado Compartido
            
            Hola! Te invito a ser mi cuidador/a.
            
            Mi código de invitación es: ${inviteCode.getFormattedCode()}
            
            Este código expira en ${inviteCode.getHoursUntilExpiration()} horas.
            
            Descarga la app "Cuidado Compartido Mayor" y úsalo para conectarte conmigo.
            
            ¡Gracias por cuidarme! 💙
        """.trimIndent()
    }

    /** Limpia los mensajes de error y éxito */
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    /** Inicialización del ViewModel */
    init {
        loadAllInviteCodes()
    }
}
