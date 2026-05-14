package com.isa.cuidadocompartidomayor.ui.caregiver

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.databinding.FragmentConnectElderlyBinding
import com.isa.cuidadocompartidomayor.ui.caregiver.viewmodel.ConnectionViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
class ConnectElderlyFragment : Fragment() {

    private var _binding: FragmentConnectElderlyBinding? = null
    private val binding get() = _binding!!

    private val connectionViewModel: ConnectionViewModel by viewModels()

    companion object {
        private const val TAG = "ConnectElderlyFragment"
        private const val INVITE_CODE_LENGTH = 6
    }

    // Launcher para el scanner QR
    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            // Código escaneado exitosamente
            val scannedCode = result.contents.uppercase()
            binding.etInviteCode.setText(scannedCode)

            // Validar y conectar automáticamente
            if (validateInviteCode(scannedCode)) {
                connectionViewModel.connectToElderly(scannedCode)
            }

            Log.d(TAG, "✅ Código QR escaneado: $scannedCode")
        } else {
            Toast.makeText(requireContext(), "❌ Escaneo cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectElderlyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupClickListeners()
        observeViewModel()

        Log.d(TAG, "✅ ConnectElderlyFragment inicializado")
    }

    /**
     * Configura la interfaz de usuario
     */
    private fun setupUI() {
        // Configurar el campo de código de invitación
        binding.etInviteCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val code = s.toString().uppercase()
                if (s.toString() != code) {
                    binding.etInviteCode.setText(code)
                    binding.etInviteCode.setSelection(code.length)
                }

                // Habilitar/deshabilitar botón conectar
                binding.btnConnect.isEnabled = code.length == INVITE_CODE_LENGTH

                // Limpiar error si existe
                if (code.isNotEmpty()) {
                    binding.tilInviteCode.error = null
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Estado inicial
        binding.btnConnect.isEnabled = false

        Log.d(TAG, "UI configurada")
    }

    /**
     * Configura los eventos de click
     */
    private fun setupClickListeners() {
        // Botón Conectar
        binding.btnConnect.setOnClickListener {
            val inviteCode = binding.etInviteCode.text.toString().trim()

            if (validateInviteCode(inviteCode)) {
                connectionViewModel.connectToElderly(inviteCode)
            }
        }

        // Botón Cancelar/Volver
        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
        // Botón Escanear QR (AGREGAR ANTES del botón Conectar)
        binding.btnScanQR.setOnClickListener {
            launchQRScanner()
        }


        Log.d(TAG, "Click listeners configurados")
    }

    /**
     * Observa los cambios en el ViewModel
     */
    private fun observeViewModel() {
        // Estado de carga
        connectionViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnConnect.isEnabled = !isLoading && binding.etInviteCode.text?.length == INVITE_CODE_LENGTH
        }

        // Resultado de conexión
        connectionViewModel.connectionResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    val relationship = it.getOrNull()
                    Toast.makeText(
                        requireContext(),
                        "✅ Conexión exitosa con ${relationship?.elderlyName}",
                        Toast.LENGTH_LONG
                    ).show()

                    // Volver al dashboard
                    findNavController().navigateUp()

                    Log.d(TAG, "Conexión exitosa: ${relationship?.elderlyName}")
                } else {
                    val error = it.exceptionOrNull()?.message ?: "Error desconocido"
                    binding.tilInviteCode.error = error

                    Log.e(TAG, "Error en conexión: $error")
                }

                connectionViewModel.clearConnectionResult()
            }
        }

        Log.d(TAG, "Observadores del ViewModel configurados")
    }

    /**
     * Valida el código de invitación
     */
    private fun validateInviteCode(code: String): Boolean {
        return when {
            code.isEmpty() -> {
                binding.tilInviteCode.error = "Por favor ingresa el código de invitación"
                false
            }
            code.length != INVITE_CODE_LENGTH -> {
                binding.tilInviteCode.error = "El código debe tener $INVITE_CODE_LENGTH caracteres"
                false
            }
            !code.matches(Regex("[A-Z0-9]+")) -> {
                binding.tilInviteCode.error = "El código solo puede contener letras y números"
                false
            }
            else -> {
                binding.tilInviteCode.error = null
                true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "Vista destruida - recursos limpiados")
    }

    /**
     * Lanza el scanner de código QR
     */
    private fun launchQRScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("📷 Escanea el código QR del adulto mayor")
            setBeepEnabled(true)
            setOrientationLocked(false)
            setBarcodeImageEnabled(false)
        }

        qrScannerLauncher.launch(options)
        Log.d(TAG, "Scanner QR iniciado")
    }

}