package com.isa.cuidadocompartidomayor.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.databinding.FragmentRegisterBinding
import com.isa.cuidadocompartidomayor.ui.caregiver.CaregiverDashboardActivity
import com.isa.cuidadocompartidomayor.ui.elderly.ElderlyDashboardActivity
import com.isa.cuidadocompartidomayor.utils.Constants

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            val userType = when (binding.rgUserType.checkedRadioButtonId) {
                R.id.rbCaregiver -> Constants.USER_TYPE_CAREGIVER
                R.id.rbElderly -> Constants.USER_TYPE_ELDERLY
                else -> {
                    Toast.makeText(requireContext(), "Por favor selecciona un tipo de usuario", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            authViewModel.registerUser(name, email, password, userType)
        }

        binding.tvLoginLink.setOnClickListener {
            // Navegar al fragmento de inicio de sesión
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun observeViewModel() {
        authViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnRegister.isEnabled = !isLoading
        }

        authViewModel.validationError.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                authViewModel.clearErrors()
            }
        }

        authViewModel.authResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    val user = it.getOrNull()
                    Log.d("RegisterFragment", "Registro exitoso para: ${user?.name}")
                    Toast.makeText(
                        requireContext(),
                        "¡Cuenta creada! Bienvenido ${user?.name}",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Guardar tipo de usuario en SharedPreferences para persistencia de sesión
                    val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString(Constants.KEY_USER_TYPE, user?.userType).apply()

                    // ✅ DIRECCIONAR SEGÚN TIPO DE USUARIO
                    if (user?.userType == Constants.USER_TYPE_CAREGIVER) {
                        // Ir al Dashboard del Cuidador
                        val intent =
                            Intent(requireContext(), CaregiverDashboardActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        // Ir al Dashboard del viejo
                        val intent =
                            Intent(requireContext(), ElderlyDashboardActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    }
                } else {
                    val error = it.exceptionOrNull()?.message ?: "Error desconocido"
                    Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_LONG).show()
                    authViewModel.clearErrors()
                }
                authViewModel.clearErrors()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}