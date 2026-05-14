package com.isa.cuidadocompartidomayor.ui.caregiver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.DocumentChange
import com.isa.cuidadocompartidomayor.MainActivity
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.CaregiverNotification
import com.isa.cuidadocompartidomayor.databinding.ActivityCaregiverDashboardBinding
import com.isa.cuidadocompartidomayor.utils.NotificationHelper

class CaregiverDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaregiverDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController
    private val db = FirebaseFirestore.getInstance()
    // Listener para notificaciones en tiempo real
    private var notificationsListener: ListenerRegistration? = null

    // Lista de fragments para ocultar toolbar y bottom nav
    private val wizardFragments = setOf(
        R.id.selectPatientFragment,
        R.id.medicationNameFragment,
        R.id.selectMedicationTypeFragment,
        R.id.selectFrequencyFragment,
        R.id.scheduleSingleDoseFragment,
        R.id.selectTimesPerDayFragment,
        R.id.scheduleMultipleDosesFragment,
        R.id.selectWeekDaysFragment,
        R.id.selectIntervalDaysFragment,
        R.id.selectIntervalWeeksFragment,
        R.id.selectIntervalMonthsFragment,
        R.id.enterDosageFragment,
        R.id.enterInstructionsFragment,
        //Wizard de Signos Vitales
        R.id.selectPatientForVitalSignsFragment,
        R.id.selectVitalSignTypesFragment,
        R.id.enterVitalSignValuesFragment,
        R.id.addVitalSignNotesFragment,
        //Wizard de Emociones y Comportamiento
        R.id.selectEmotionsFragment,
        R.id.selectSymptomsAndLevelsFragment,
        R.id.addMoodNotesFragment,
        R.id.selectPatientForMoodFragment,

        R.id.profileFragment

    )

    private val patientFragments = setOf(
        R.id.patientListFragment,
        R.id.patientDetailFragment,
        R.id.connectElderlyFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar ViewBinding
        binding = ActivityCaregiverDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Verificar que el usuario esté autenticado
        if (auth.currentUser == null) {
            redirectToAuth()
            return
        }

        // Crear canal de notificaciones
        NotificationHelper.createNotificationChannel(this)

        // Escuchar notificaciones en tiempo real
        startListeningForNotifications()

        // Configurar Navigation
        setupNavigation()

        // Configurar Toolbar
        setupToolbar()

        Log.d("CaregiverDashboard", "✅ Dashboard del cuidador inicializado")
    }

    /** Escucha y marca notificaciones */
    private fun startListeningForNotifications() {
        val currentUser = auth.currentUser ?: return

        notificationsListener = db.collection("caregiverNotifications")
            .whereEqualTo("caregiverId", currentUser.uid)
            .whereEqualTo("read", false)
            .addSnapshotListener { snapshots: QuerySnapshot?, error: FirebaseFirestoreException? ->
                if (error != null) {
                    Log.e("CaregiverDashboard", "❌ Error escuchando notificaciones", error)
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { change: DocumentChange ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val notification = change.document.toObject(CaregiverNotification::class.java)
                        val notificationId = change.document.id

                        // ✅ Mostrar notificación LOCAL en el dispositivo del cuidador
                        NotificationHelper.showCaregiverNotification(
                            context = this,
                            title = notification.title,
                            body = notification.body,
                            medicationId = notification.medicationId,
                            elderlyId = notification.elderlyId
                        )

                        Log.d("CaregiverDashboard", "✅ Notificación mostrada: ${notification.title}")
                        Log.d("CaregiverDashboard", "📋 Tipo: ${notification.type}")

                        if (notification.type != "EMERGENCY") {
                            // Marcar como leída automáticamente (REMINDER, MISSED, TAKEN, etc.)
                            db.collection("caregiverNotifications")
                                .document(notificationId)
                                .update("read", true)
                                .addOnSuccessListener {
                                    Log.d("CaregiverDashboard", "✅ Notificación de medicamento marcada como leída: $notificationId")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("CaregiverDashboard", "❌ Error marcando notificación como leída", e)
                                }
                        } else {
                            // Las notificaciones de EMERGENCIA NO se marcan automáticamente
                            Log.d("CaregiverDashboard", "⚠️ Notificación de EMERGENCIA - NO se marca como leída automáticamente")
                            Log.d("CaregiverDashboard", "   El cuidador debe marcarla manualmente desde el dashboard")
                        }

                    }
                }
            }
    }

    /** Configura el Navigation Component con Bottom Navigation */
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_caregiver) as NavHostFragment
        navController = navHostFragment.navController

        // Conectar BottomNavigation con Navigation Controller
        binding.bottomNavigation.setupWithNavController(navController)

        // ✅ Listener para detectar cambios de destino
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Ocultar/mostrar toolbar y bottom nav según el destino
            if (destination.id in wizardFragments || destination.id in patientFragments) {
                // Estamos en el wizard - ocultar
                hideToolbarAndBottomNav()
                Log.d("CaregiverDashboard", "🔒 Wizard detectado - Ocultando UI")
            } else {
                // Estamos en pantallas normales - mostrar
                showToolbarAndBottomNav()
                Log.d("CaregiverDashboard", "🔓 Pantalla normal - Mostrando UI")
            }
        }
    }

    /** Configura la Toolbar con eventos */
    private fun setupToolbar() {
        // Profile Button
        binding.toolbar.findViewById<android.widget.ImageButton>(R.id.btnProfile)?.setOnClickListener {
            // Navegar a ProfileFragment
            findNavController(R.id.nav_host_fragment_caregiver).navigate(R.id.profileFragment)
        }

        // Settings Button
        binding.toolbar.findViewById<android.widget.ImageButton>(R.id.btnSettings)?.setOnClickListener {
            // Por ahora, logout
            logout()
        }
    }

    /** Oculta el toolbar y bottom navigation con animación */
    private fun hideToolbarAndBottomNav() {
        // Animar toolbar hacia arriba
        binding.toolbar.animate()
            .translationY(-binding.toolbar.height.toFloat())
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.toolbar.visibility = View.GONE
            }
            .start()

        // Animar bottom nav hacia abajo
        binding.bottomNavigation.animate()
            .translationY(binding.bottomNavigation.height.toFloat())
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.bottomNavigation.visibility = View.GONE
            }
            .start()
    }

    /** Muestra el toolbar y bottom navigation con animación */
    private fun showToolbarAndBottomNav() {
        // Restaurar toolbar
        binding.toolbar.visibility = View.VISIBLE
        binding.toolbar.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(300)
            .start()

        // Restaurar bottom nav
        binding.bottomNavigation.visibility = View.VISIBLE
        binding.bottomNavigation.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    /** Cerrar sesión y regresar a Auth */
    private fun logout() {
        auth.signOut()
        redirectToAuth()
    }

    /** Redirigir a MainActivity (Auth) */
    private fun redirectToAuth() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /** Manejar el botón "Atrás" */
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_caregiver)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationsListener?.remove()
        Log.d("CaregiverDashboard", "🔌 Listener de notificaciones desconectado")
    }
}
