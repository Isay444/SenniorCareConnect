package com.isa.cuidadocompartidomayor.ui.elderly

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.isa.cuidadocompartidomayor.data.model.InviteCode
import com.isa.cuidadocompartidomayor.databinding.FragmentGenerateInviteCodeBinding
import com.isa.cuidadocompartidomayor.ui.elderly.adapter.InviteCodeAdapter
import com.isa.cuidadocompartidomayor.ui.elderly.viewmodel.InviteCodeViewModel

class GenerateInviteCodeFragment : Fragment() {

    private var _binding: FragmentGenerateInviteCodeBinding? = null
    private val binding get() = _binding!!

    private val inviteCodeViewModel: InviteCodeViewModel by viewModels()
    private lateinit var inviteCodeAdapter: InviteCodeAdapter

    companion object {
        private const val TAG = "GenerateInviteCodeFragment"
        private const val QR_SIZE = 400
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGenerateInviteCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        setupObservers()

        Log.d(TAG, "✅ GenerateInviteCodeFragment inicializado")
    }

    /**
     * Configura el RecyclerView de códigos anteriores
     */
    private fun setupRecyclerView() {
        inviteCodeAdapter = InviteCodeAdapter { inviteCode ->
            shareInviteCode(inviteCode)
        }

        binding.rvPreviousCodes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = inviteCodeAdapter
        }

        Log.d(TAG, "RecyclerView configurado")
    }

    /**
     * Configura los eventos de click
     */
    private fun setupClickListeners() {
        // Botón principal GENERAR CÓDIGO
        binding.btnGenerateCode.setOnClickListener {
            generateNewCode()
        }

        // Botón compartir código actual
        binding.btnShareCode.setOnClickListener {
            val currentCode = inviteCodeViewModel.currentInviteCode.value
            if (currentCode != null) {
                shareInviteCode(currentCode)
            } else {
                Toast.makeText(requireContext(), "No hay código para compartir", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón copiar código
        binding.btnCopyCode.setOnClickListener {
            val currentCode = inviteCodeViewModel.currentInviteCode.value
            if (currentCode != null) {
                copyCodeToClipboard(currentCode)
            } else {
                Toast.makeText(requireContext(), "No hay código para copiar", Toast.LENGTH_SHORT).show()
            }
        }

        Log.d(TAG, "Click listeners configurados")
    }

    /**
     * Configura los observadores del ViewModel
     */
    private fun setupObservers() {
        // Código actual
        inviteCodeViewModel.currentInviteCode.observe(viewLifecycleOwner) { inviteCode ->
            displayCurrentCode(inviteCode)
        }

        // Lista de todos los códigos
        inviteCodeViewModel.allInviteCodes.observe(viewLifecycleOwner) { codes ->
            updateCodesList(codes)
        }

        // Estado de carga
        inviteCodeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            updateLoadingState(isLoading)
        }

        // Mensajes de éxito
        inviteCodeViewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                showSuccessMessage(it)
                inviteCodeViewModel.clearMessages()
            }
        }

        // Mensajes de error
        inviteCodeViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                showErrorMessage(it)
                inviteCodeViewModel.clearMessages()
            }
        }

        Log.d(TAG, "Observadores configurados")
    }

    /**
     * Genera un nuevo código de invitación
     */
    private fun generateNewCode() {
        inviteCodeViewModel.generateNewInviteCode()
        Log.d(TAG, "Solicitando generación de nuevo código")
    }

    /**
     * Muestra el código actual en la UI
     */
    private fun displayCurrentCode(inviteCode: InviteCode?) {
        //  VALIDACIÓN MEJORADA: Verificar que el código sea válido Y no haya sido usado
        if (inviteCode != null && inviteCode.isValid() && inviteCode.usedAt == null) {
            // Mostrar sección de código
            binding.layoutCurrentCode.visibility = View.VISIBLE
            binding.layoutNoCode.visibility = View.GONE

            // Mostrar código formateado
            binding.tvCurrentCode.text = inviteCode.getFormattedCode()

            // Mostrar información del código
            binding.tvCodeExpiration.text = "Expira en ${inviteCode.getHoursUntilExpiration()} horas"

            // Generar y mostrar QR Code
            generateQRCode(inviteCode.code)

            Log.d(TAG, "✅ Código válido mostrado: ${inviteCode.code}")
        } else {
            // Mostrar mensaje de no código
            binding.layoutCurrentCode.visibility = View.GONE
            binding.layoutNoCode.visibility = View.VISIBLE

            if (inviteCode?.usedAt != null) {
                Log.d(TAG, "⚠️ Código usado detectado - mostrando mensaje de no código")
            } else if (inviteCode?.isExpired() == true) {
                Log.d(TAG, "⚠️ Código expirado detectado - mostrando mensaje de no código")
            } else {
                Log.d(TAG, "No hay código válido para mostrar")
            }

            // OPCIONAL: Generar automáticamente un nuevo código si el actual está usado/expirado
            if (inviteCode != null && (inviteCode.usedAt != null || inviteCode.isExpired())) {
                Log.d(TAG, "🔄 Generando nuevo código automáticamente...")
                // Descomentar la siguiente línea si quieres auto-generación
                // generateNewCode()
            }
        }
    }


    /**
     * Genera el código QR
     */
    private fun generateQRCode(code: String) {
        try {
            val writer = MultiFormatWriter()
            val bitMatrix: BitMatrix = writer.encode(code, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE)

            val bitmap = Bitmap.createBitmap(QR_SIZE, QR_SIZE, Bitmap.Config.RGB_565)
            for (x in 0 until QR_SIZE) {
                for (y in 0 until QR_SIZE) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }

            binding.ivQrCode.setImageBitmap(bitmap)
            binding.layoutQrCode.visibility = View.VISIBLE

            Log.d(TAG, "QR Code generado para código: $code")

        } catch (e: Exception) {
            Log.e(TAG, "Error generando QR Code", e)
            binding.layoutQrCode.visibility = View.GONE
        }
    }

    /**
     * Actualiza la lista de códigos anteriores
     */
    private fun updateCodesList(codes: List<InviteCode>) {
        val previousCodes = codes.filter { !it.isValid() }

        if (previousCodes.isNotEmpty()) {
            binding.layoutPreviousCodes.visibility = View.VISIBLE
            inviteCodeAdapter.updateCodes(previousCodes)
        } else {
            binding.layoutPreviousCodes.visibility = View.GONE
        }

        Log.d(TAG, "Lista de códigos actualizada: ${previousCodes.size} códigos anteriores")
    }

    /**
     * Actualiza el estado de carga
     */
    private fun updateLoadingState(isLoading: Boolean) {
        binding.btnGenerateCode.apply {
            isEnabled = !isLoading
            text = if (isLoading) "GENERANDO..." else "GENERAR CÓDIGO"
        }

        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    /**
     * Comparte el código de invitación
     */
    private fun shareInviteCode(inviteCode: InviteCode) {
        val shareText = inviteCodeViewModel.getShareText(inviteCode)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Invitación de Cuidado Compartido")
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Compartir código de invitación")
        startActivity(chooserIntent)

        Log.d(TAG, "Compartiendo código: ${inviteCode.code}")
    }

    /**
     * Copia el código al portapapeles
     */
    private fun copyCodeToClipboard(inviteCode: InviteCode) {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Código de Invitación", inviteCode.getFormattedCode())
        clipboard.setPrimaryClip(clip)

        Toast.makeText(requireContext(), "✅ Código copiado: ${inviteCode.getFormattedCode()}", Toast.LENGTH_SHORT).show()

        Log.d(TAG, "Código copiado al portapapeles: ${inviteCode.code}")
    }

    /**
     * Muestra mensaje de éxito
     */
    private fun showSuccessMessage(message: String) {
        Toast.makeText(
            requireContext(),
            "✅ $message",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Muestra mensaje de error
     */
    private fun showErrorMessage(error: String) {
        Toast.makeText(
            requireContext(),
            "❌ $error",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onResume() {
        super.onResume()
        // Recargar códigos cuando vuelve a estar visible
        inviteCodeViewModel.loadAllInviteCodes()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "Vista destruida - recursos limpiados")
    }
}
