package com.isa.cuidadocompartidomayor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import android.util.Log
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.isa.cuidadocompartidomayor.databinding.ActivityMainBinding

import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.isa.cuidadocompartidomayor.ui.caregiver.CaregiverDashboardActivity
import com.isa.cuidadocompartidomayor.ui.elderly.ElderlyDashboardActivity
import com.isa.cuidadocompartidomayor.utils.Constants

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Verificar sesión antes de inflar la UI de Auth
        if (checkSession()) return

        // Inicializar el ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar firebase
        FirebaseApp.initializeApp(this)
        Log.d("Firebase", "Firebase initialized")

        setupNavigation()
    }

    /**
     * Verifica si hay una sesión activa y redirige al dashboard correspondiente
     * @return true si se redirigió, false si debe continuar con el flujo de auth
     */
    private fun checkSession(): Boolean {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
            val userType = prefs.getString(Constants.KEY_USER_TYPE, null)

            Log.d("MainActivity", "Sesión detectada para UID: ${currentUser.uid}, Tipo: $userType")

            when (userType) {
                Constants.USER_TYPE_CAREGIVER -> {
                    startActivity(Intent(this, CaregiverDashboardActivity::class.java))
                    finish()
                    return true
                }
                Constants.USER_TYPE_ELDERLY -> {
                    startActivity(Intent(this, ElderlyDashboardActivity::class.java))
                    finish()
                    return true
                }
                else -> {
                    // Si no tenemos el tipo de usuario guardado, cerramos sesión para evitar estados inconsistentes
                    Log.w("MainActivity", "Sesión activa pero tipo de usuario desconocido. Cerrando sesión.")
                    auth.signOut()
                }
            }
        }
        return false
    }

    /** Configura el Navigation Component **/
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController // Property "navController" is never used
    }

    /** Manejar el botón "Atrás" con Navigation **/
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
