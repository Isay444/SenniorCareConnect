package com.isa.cuidadocompartidomayor.ui.elderly

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.isa.cuidadocompartidomayor.MainActivity
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.databinding.ActivityElderlyDashboardBinding
import com.isa.cuidadocompartidomayor.utils.NotificationHelper
import com.isa.cuidadocompartidomayor.utils.PermissionsHelper

class ElderlyDashboardActivity : AppCompatActivity(){
    private lateinit var binding: ActivityElderlyDashboardBinding
    private lateinit var navController: NavController

    private lateinit var auth: FirebaseAuth

    companion object {
        private const val TAG = "ElderlyDashboardActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar View Binding
        binding = ActivityElderlyDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Verificar que el usuario esté autenticado
        if (auth.currentUser == null) {
            redirectToAuth()
            return
        }


        setupToolbar()

        // : Crear canal de notificaciones
        NotificationHelper.createNotificationChannel(this)

        setupNavigation()
        setupAccessibilityFeatures()

        // : Solicitar permisos después de configurar la navegación
        checkAndRequestPermissions()

        Log.d(TAG, "✅ ElderlyDashboardActivity iniciado correctamente")
    }

    /** Configura la navegación principal */
    private fun setupNavigation() {
        // Obtener NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_elderly) as NavHostFragment
        navController = navHostFragment.navController

        // Configurar BottomNavigationView con NavController
        binding.bottomNavigationElderly.setupWithNavController(navController)

        // Listener para cambios de destino (opcional para logs)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d(TAG, "Navegando a: ${destination.label}")
        }

        Log.d(TAG, "Navegación configurada correctamente")
    }

    /** Configura características de accesibilidad */
    private fun setupAccessibilityFeatures() {
        // Aumentar el tamaño de los elementos táctiles
        binding.bottomNavigationElderly.apply {
            // Configurar altura mínima para mejor accesibilidad
            minimumHeight = resources.getDimensionPixelSize(R.dimen.bottom_nav_height_elderly)

            // Habilitar descripciones de contenido
            itemIconTintList = null // Mantener colores originales de iconos
        }

        // Configurar vibración y feedback táctil (será implementado después)
        // TODO: Agregar configuraciones de vibración para feedback táctil

        Log.d(TAG, "Características de accesibilidad configuradas")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ElderlyDashboardActivity destruido - limpiando recursos")
    }

    /** Configura la Toolbar con eventos */
    private fun setupToolbar() {
        // Profile Button
        binding.toolbar.findViewById<android.widget.ImageButton>(R.id.btnProfile)?.setOnClickListener {
            // Navegar a un ProfileFragment
            findNavController(R.id.nav_host_fragment_elderly).navigate(R.id.profileFragment)
            }

        // Settings
        binding.toolbar.findViewById<android.widget.ImageButton>(R.id.btnSettings)?.setOnClickListener {
            Toast.makeText(this, "Cerrar Sesion", Toast.LENGTH_SHORT).show()
            //logout por ahora
            logout()
        }
    }

    /** Cerrar sesión y regresar a Auth */
    private fun logout() {
        auth.signOut()
        redirectToAuth()
    }
    private fun redirectToAuth() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /** Manejo del botón de retroceso */
    override fun onSupportNavigateUp(): Boolean {
        //return navController.navigateUp() || super.onSupportNavigateUp()
        val navController = findNavController(R.id.nav_host_fragment_elderly)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // : Solicitar permisos de notificaciones
    private fun checkAndRequestPermissions() {
        // 1. Verificar si ya tiene todos los permisos
        if (NotificationHelper.hasAllPermissions(this)) {
            // Ya tiene todos los permisos
            return
        }

        // 2. Solicitar permiso de notificaciones (Android 13+)
        if (!NotificationHelper.hasNotificationPermission(this)) {
            showNotificationPermissionDialog()
        }
        // 3. Verificar permiso de alarmas exactas (Android 12+)
        else if (!NotificationHelper.canScheduleExactAlarms(this)) {
            showExactAlarmPermissionDialog()
        }
    }

    /** Muestra diálogo educativo antes de solicitar permiso de notificaciones */
    private fun showNotificationPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("📬 Activar Notificaciones")
            .setMessage(
                "Para recordarte cuándo tomar tus medicamentos, necesitamos enviarte notificaciones.\n\n" +
                        "Esto es muy importante para tu salud. ¿Deseas activarlas?"
            )
            .setPositiveButton("Sí, activar") { _, _ ->
                PermissionsHelper.requestNotificationPermission(this)
            }
            .setNegativeButton("Ahora no") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    this,
                    "Puedes activarlas más tarde desde Configuración",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setCancelable(false)
            .show()
    }

    /** Muestra diálogo para solicitar permiso de alarmas exactas */
    private fun showExactAlarmPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("⏰ Activar Alarmas Precisas")
            .setMessage(
                "Para asegurar que las notificaciones lleguen a la hora exacta, necesitamos tu permiso.\n\n" +
                        "Esto garantiza que no olvides tomar tus medicamentos."
            )
            .setPositiveButton("Activar") { _, _ ->
                NotificationHelper.openExactAlarmSettings(this)
            }
            .setNegativeButton("Ahora no") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    this,
                    "Las notificaciones podrían no llegar a tiempo",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Maneja el resultado de la solicitud de permisos
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionsHelper.REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "✅ Notificaciones activadas", Toast.LENGTH_SHORT).show()

                    // Verificar permiso de alarmas exactas
                    if (!NotificationHelper.canScheduleExactAlarms(this)) {
                        showExactAlarmPermissionDialog()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "⚠️ Sin notificaciones no podremos recordarte tus medicamentos",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /** Vuelve a verificar permisos cuando la actividad regresa del foreground */
    override fun onResume() {
        super.onResume()
        // Verificar de nuevo al volver de configuración
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!NotificationHelper.canScheduleExactAlarms(this)) {
                // El usuario volvió sin activar las alarmas exactas
                // No molestar, pero registrar en log
            }
        }
    }
}