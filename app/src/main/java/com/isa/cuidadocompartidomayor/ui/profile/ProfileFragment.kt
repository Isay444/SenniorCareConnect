package com.isa.cuidadocompartidomayor.ui.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.isa.cuidadocompartidomayor.MainActivity
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.databinding.FragmentProfileBinding
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    companion object {
        private const val TAG = "ProfileFragment"
    }

    // Launcher para seleccionar imagen
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadAndSaveProfileImage(requireContext(), it)
        }
    }

    // Launcher para permisos
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(requireContext(), "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
        viewModel.loadUserProfile()

        Log.d(TAG, "✅ ProfileFragment inicializado")
    }

    /**
     * Configura los observadores del ViewModel
     */
    private fun setupObservers() {
        // Observar datos del perfil
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvUserName.text = name
        }

        viewModel.userEmail.observe(viewLifecycleOwner) { email ->
            binding.tvUserEmail.text = email
        }

        viewModel.userRole.observe(viewLifecycleOwner) { role ->
            val roleText = when (role) {
                "caregiver" -> "CUIDADOR"
                "elderly" -> "ADULTO MAYOR"
                else -> "USUARIO"
            }
            binding.tvUserRole.text = roleText
        }

        // Observar estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observar mensajes de éxito
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }

        viewModel.userPhone.observe(viewLifecycleOwner) { phone ->
            binding.tvPhone.text = if (phone.isBlank()) "No registrado" else phone
        }

        viewModel.emergencyContact.observe(viewLifecycleOwner) { contact ->
            binding.tvEmergencyContact.text = if (contact.isBlank()) "No registrado" else contact
        }

        viewModel.userAddress.observe(viewLifecycleOwner) { address ->
            binding.tvAddress.text = if (address.isBlank()) "No registrada" else address
        }

        viewModel.birthDate.observe(viewLifecycleOwner) { birthDate ->
            binding.tvBirthDate.text = if (birthDate.isBlank()) "No registrada" else birthDate
        }

        // Observar mensajes de error
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }

        // Observar estado de subida de imagen
        viewModel.isUploadingImage.observe(viewLifecycleOwner) { isUploading ->
            binding.pbAvatar.visibility = if (isUploading) View.VISIBLE else View.GONE
            binding.cardAvatar.isEnabled = !isUploading
            binding.cardAvatar.alpha = if (isUploading) 0.6f else 1.0f
        }

        // Observar error de subida
        viewModel.uploadError.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        // Observar foto de perfil
        viewModel.profileImageUrl.observe(viewLifecycleOwner) { imageUrl ->
            loadProfileImage(imageUrl)
        }
    }

    /**
     * Configura los listeners de los clicks
     */
    private fun setupClickListeners() {
        binding.apply {
            // Editar nombre
            cardEditName.setOnClickListener {
                showEditNameDialog()
            }

            cardEditPhone.setOnClickListener {
                showEditPhoneDialog()
            }

            // Editar dirección
            cardEditAddress.setOnClickListener {
                showEditAddressDialog()
            }

            // Editar fecha de nacimiento
            cardEditBirthDate.setOnClickListener {
                showEditBirthDateDialog()
            }

            // Editar contacto de emergencia
            cardEditEmergencyContact.setOnClickListener {
                showEditEmergencyContactDialog()
            }

            // Cambiar correo
            cardEditEmail.setOnClickListener {
                showEditEmailDialog()
            }

            // Cambiar contraseña
            cardChangePassword.setOnClickListener {
                showChangePasswordDialog()
            }
            // Foto de perfil
            cardAvatar.setOnClickListener {
                showImagePickerOptions()
            }

            // Cerrar sesión
            cardSignOut.setOnClickListener {
                showSignOutConfirmation()
            }

            // Eliminar cuenta
            cardDeleteAccount.setOnClickListener {
                showDeleteAccountConfirmation()
            }
        }
    }

    /**
     * Muestra diálogo para editar nombre
     */
    private fun showEditNameDialog() {
        val input = EditText(requireContext()).apply {
            setText(viewModel.userName.value)
            hint = "Nuevo nombre"
            setPadding(60, 40, 60, 40)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar nombre")
            .setMessage("Ingresa tu nuevo nombre")
            .setView(input)
            .setPositiveButton("Guardar") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.updateName(newName)
                } else {
                    Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Muestra diálogo para editar teléfono
     */
    private fun showEditPhoneDialog() {
        val input = EditText(requireContext()).apply {
            setText(viewModel.userPhone.value)
            hint = "Teléfono"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setPadding(60, 40, 60, 40)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar teléfono")
            .setMessage("Ingresa tu número de teléfono")
            .setView(input)
            .setPositiveButton("Guardar") { dialog, _ ->
                val newPhone = input.text.toString().trim()
                if (newPhone.isNotEmpty()) {
                    viewModel.updatePhone(newPhone)
                } else {
                    Toast.makeText(requireContext(), "El teléfono no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Muestra diálogo para editar dirección
     */
    private fun showEditAddressDialog() {
        val input = EditText(requireContext()).apply {
            setText(viewModel.userAddress.value)
            hint = "Dirección completa"
            inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 3
            setPadding(60, 40, 60, 40)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar dirección")
            .setMessage("Ingresa tu dirección completa")
            .setView(input)
            .setPositiveButton("Guardar") { dialog, _ ->
                val newAddress = input.text.toString().trim()
                if (newAddress.isNotEmpty()) {
                    viewModel.updateAddress(newAddress)
                } else {
                    Toast.makeText(requireContext(), "La dirección no puede estar vacía", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Muestra diálogo para editar fecha de nacimiento
     */
    private fun showEditBirthDateDialog() {
        val calendar = java.util.Calendar.getInstance()

        // Parsear fecha actual si existe
        viewModel.birthDate.value?.let { currentDate ->
            if (currentDate.isNotBlank() && currentDate != "No registrada") {
                try {
                    val parts = currentDate.split("/")
                    if (parts.size == 3) {
                        calendar.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
                    }
                } catch (e: Exception) {
                    // Usar fecha actual si hay error
                }
            }
        }

        val datePickerDialog = android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val formattedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                viewModel.updateBirthDate(formattedDate)
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )

        // Establecer fecha máxima (hoy)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        datePickerDialog.show()
    }

    /**
     * Muestra diálogo para editar contacto de emergencia
     */
    private fun showEditEmergencyContactDialog() {
        val input = EditText(requireContext()).apply {
            setText(viewModel.emergencyContact.value)
            hint = "Teléfono de contacto"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setPadding(60, 40, 60, 40)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Contacto de emergencia")
            .setMessage("Ingresa el número de teléfono del contacto de emergencia")
            .setView(input)
            .setPositiveButton("Guardar") { dialog, _ ->
                val newContact = input.text.toString().trim()
                if (newContact.isNotEmpty()) {
                    viewModel.updateEmergencyContact(newContact)
                } else {
                    Toast.makeText(requireContext(), "El contacto no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    /**
     * Muestra diálogo para cambiar correo electrónico
     */
    private fun showEditEmailDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_email, null)

        val inputEmail = dialogView.findViewById<TextInputEditText>(R.id.etNewEmail)
        val inputPassword = dialogView.findViewById<TextInputEditText>(R.id.etCurrentPassword)

        inputEmail.setText(viewModel.userEmail.value)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cambiar correo electrónico")
            .setMessage("Por seguridad, necesitamos tu contraseña actual")
            .setView(dialogView)
            .setPositiveButton("Actualizar") { dialog, _ ->
                val newEmail = inputEmail.text.toString().trim()
                val password = inputPassword.text.toString()

                if (newEmail.isNotEmpty() && password.isNotEmpty()) {
                    viewModel.updateEmail(newEmail, password)
                } else {
                    Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Muestra diálogo para cambiar contraseña
     */
    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_change_password, null)

        val inputCurrentPassword = dialogView.findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val inputNewPassword = dialogView.findViewById<TextInputEditText>(R.id.etNewPassword)
        val inputConfirmPassword = dialogView.findViewById<TextInputEditText>(R.id.etConfirmPassword)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cambiar contraseña")
            .setMessage("Ingresa tu contraseña actual y la nueva contraseña")
            .setView(dialogView)
            .setPositiveButton("Cambiar") { dialog, _ ->
                val currentPassword = inputCurrentPassword.text.toString()
                val newPassword = inputNewPassword.text.toString()
                val confirmPassword = inputConfirmPassword.text.toString()

                if (currentPassword.isNotEmpty() && newPassword.isNotEmpty() && confirmPassword.isNotEmpty()) {
                    viewModel.updatePassword(currentPassword, newPassword, confirmPassword)
                } else {
                    Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Muestra confirmación para cerrar sesión
     */
    private fun showSignOutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro que deseas cerrar sesión?")
            .setIcon(R.drawable.ic_logout)
            .setPositiveButton("Cerrar sesión") { dialog, _ ->
                viewModel.signOut()
                navigateToLogin()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Muestra confirmación para eliminar cuenta
     */
    private fun showDeleteAccountConfirmation() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_delete_account, null)

        val inputPassword = dialogView.findViewById<TextInputEditText>(R.id.etPasswordConfirm)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ Eliminar cuenta")
            .setMessage("Esta acción es PERMANENTE. Se eliminarán todos tus datos y no podrás recuperarlos.\n\nPara confirmar, ingresa tu contraseña:")
            .setView(dialogView)
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Eliminar definitivamente") { dialog, _ ->
                val password = inputPassword.text.toString()

                if (password.isNotEmpty()) {
                    viewModel.deleteAccount(password) {
                        navigateToLogin()
                    }
                } else {
                    Toast.makeText(requireContext(), "Debes ingresar tu contraseña", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Navega a la pantalla de login
     */
    private fun navigateToLogin() {
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    /**
     * Muestra opciones para cambiar foto de perfil
     */
    private fun showImagePickerOptions() {
        val options = arrayOf("Seleccionar de galería", "Eliminar foto", "Cancelar")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Foto de perfil")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkPermissionAndPickImage()
                    1 -> removeProfileImage()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    /**
     * Verifica permisos y abre selector de imágenes
     */
    private fun checkPermissionAndPickImage() {
        when {
            // Android 13+ (API 33+)
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    openImagePicker()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
            // Android 6-12 (API 23-32)
            else -> {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    openImagePicker()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    /**
     * Abre el selector de imágenes
     */
    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    /**
     * Elimina la foto de perfil
     */
    private fun removeProfileImage() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar foto de perfil")
            .setMessage("¿Estás seguro que deseas eliminar tu foto de perfil?")
            .setPositiveButton("Eliminar") { dialog, _ ->
                viewModel.removeProfileImage(currentUserId)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Carga la imagen de perfil con Glide
     */
    private fun loadProfileImage(imageUrl: String) {
        if (imageUrl.isBlank()) {
            // Mostrar icono por defecto
            binding.ivAvatar.setImageResource(R.drawable.ic_avatar)
            binding.ivAvatar.setPadding(20, 20, 20, 20)
            binding.ivAvatar.setBackgroundColor(
                ContextCompat.getColor(requireContext(), com.google.android.material.R.color.m3_ref_palette_grey_variant98)
            )
        } else {
            // Cargar desde URL o Base64 con Glide
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_avatar)
                .error(R.drawable.ic_avatar)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.ivAvatar)
            binding.ivAvatar.setPadding(0, 0, 0, 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
