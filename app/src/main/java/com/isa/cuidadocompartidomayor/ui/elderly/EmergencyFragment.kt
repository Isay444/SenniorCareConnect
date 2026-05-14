package com.isa.cuidadocompartidomayor.ui.elderly

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.isa.cuidadocompartidomayor.databinding.FragmentEmergencyBinding
import com.isa.cuidadocompartidomayor.ui.elderly.viewmodel.EmergencyViewModel

class EmergencyFragment : Fragment() {

    private var _binding: FragmentEmergencyBinding? = null
    private val binding get() = _binding!!

    private val emergencyViewModel: EmergencyViewModel by viewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var vibrator: Vibrator? = null
    private var isEmergencyActivated = false

    companion object {
        private const val TAG = "EmergencyFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmergencyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEmergencyButton()
        setupLocationServices()
        setupClickListeners()
        setupObservers()
        Log.d(TAG, "✅ EmergencyFragment inicializado correctamente")
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocationAndSendAlert()
        } else {
            sendAlertWithoutLocation()
        }
    }

    private fun setupEmergencyButton() {
        binding.btnEmergency.apply {
            contentDescription = "Botón de emergencia SOS. Presiona para enviar alerta de emergencia."
            isHapticFeedbackEnabled = true
        }

        vibrator = ContextCompat.getSystemService(requireContext(), Vibrator::class.java)
        Log.d(TAG, "Botón de emergencia configurado")
    }

    private fun setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        Log.d(TAG, "Servicios de ubicación configurados")
    }

    private fun setupClickListeners() {
        binding.btnEmergency.setOnClickListener {
            activateEmergencyMode()
        }

        binding.btnConfirmEmergency.setOnClickListener {
            sendEmergencyAlert()
        }

        binding.btnCancelEmergency.setOnClickListener {
            cancelEmergency()
        }

        Log.d(TAG, "Click listeners configurados")
    }

    /**
     * : Observa cambios del ViewModel
     */
    private fun setupObservers() {
        emergencyViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                showLoadingState()
            }
        }

        emergencyViewModel.alertSent.observe(viewLifecycleOwner) { sent ->
            if (sent) {
                showSuccessState()
            }
        }

        emergencyViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showErrorState(it)
            }
        }
    }

    private fun activateEmergencyMode() {
        vibratePhone()
        showConfirmationScreen()
        isEmergencyActivated = true
        Log.d(TAG, "Modo de emergencia activado - mostrando confirmación")
    }

    private fun showConfirmationScreen() {
        binding.layoutEmergencyButton.visibility = View.GONE
        binding.layoutConfirmation.visibility = View.VISIBLE
        binding.root.setBackgroundColor(
            ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
        )

        Log.d(TAG, "Pantalla de confirmación mostrada")
    }

    private fun sendEmergencyAlert() {
        getCurrentLocationAndSendAlert()
    }

    private fun getCurrentLocationAndSendAlert() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                sendAlertWithLocation(location)
            } else {
                sendAlertWithoutLocation()
            }
        }.addOnFailureListener {
            Log.e(TAG, "Error obteniendo ubicación: ${it.message}")
            sendAlertWithoutLocation()
        }
    }

    /**
     * : Envía alerta con ubicación usando ViewModel
     */
    private fun sendAlertWithLocation(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        Log.d(TAG, "Enviando alerta con ubicación: $latitude, $longitude")

        emergencyViewModel.sendEmergencyAlert(latitude, longitude)
    }

    /**
     * : Envía alerta sin ubicación usando ViewModel
     */
    private fun sendAlertWithoutLocation() {
        Log.d(TAG, "Enviando alerta sin ubicación")
        emergencyViewModel.sendEmergencyAlert()
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun showLoadingState() {
        binding.btnConfirmEmergency.apply {
            isEnabled = false
            text = "ENVIANDO..."
        }

        binding.btnCancelEmergency.isEnabled = false
        binding.progressBarEmergency.visibility = View.VISIBLE
    }

    private fun showSuccessState() {
        binding.progressBarEmergency.visibility = View.GONE
        Toast.makeText(
            requireContext(),
            "🚨 ALERTA ENVIADA\n✅ Tus cuidadores han sido notificados",
            Toast.LENGTH_LONG
        ).show()

        view?.postDelayed({
            resetEmergencyState()
            emergencyViewModel.resetAlertState()
        }, 2000)
    }

    /**
     * : Muestra estado de error
     */
    private fun showErrorState(message: String) {
        binding.progressBarEmergency.visibility = View.GONE
        Toast.makeText(
            requireContext(),
            "❌ Error: $message",
            Toast.LENGTH_LONG
        ).show()

        view?.postDelayed({
            resetEmergencyState()
        }, 2000)
    }

    private fun cancelEmergency() {
        resetEmergencyState()
        Toast.makeText(
            requireContext(),
            "Emergencia cancelada",
            Toast.LENGTH_SHORT
        ).show()
        Log.d(TAG, "Emergencia cancelada por el usuario")
    }

    private fun resetEmergencyState() {
        binding.layoutEmergencyButton.visibility = View.VISIBLE
        binding.layoutConfirmation.visibility = View.GONE
        binding.root.setBackgroundColor(
            ContextCompat.getColor(requireContext(), android.R.color.white)
        )

        binding.btnConfirmEmergency.apply {
            isEnabled = true
            text = "SÍ, ENVIAR ALERTA"
        }

        binding.btnCancelEmergency.isEnabled = true
        binding.progressBarEmergency.visibility = View.GONE
        isEmergencyActivated = false
        Log.d(TAG, "Estado de emergencia reseteado")
    }

    private fun vibratePhone() {
        vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "Vista destruida - recursos limpiados")
    }
}
