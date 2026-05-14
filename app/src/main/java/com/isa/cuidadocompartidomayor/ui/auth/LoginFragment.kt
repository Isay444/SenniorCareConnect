package com.isa.cuidadocompartidomayor.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.isa.cuidadocompartidomayor.databinding.FragmentLoginBinding
import androidx.navigation.fragment.findNavController
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.ui.caregiver.CaregiverDashboardActivity
import com.isa.cuidadocompartidomayor.ui.elderly.ElderlyDashboardActivity
import com.isa.cuidadocompartidomayor.utils.Constants

class LoginFragment : Fragment(){
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            authViewModel.loginUser(email, password)
        }

        binding.tvRegisterLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun observeViewModel() {
        authViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnLogin.isEnabled = !isLoading
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

                    // Guardar tipo de usuario en SharedPreferences para persistencia de sesión
                    val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString(Constants.KEY_USER_TYPE, user?.userType).apply()

                    // Abrir Activity según el tipo de usuario
                    when (user?.userType) {
                        Constants.USER_TYPE_ELDERLY -> {
                            val intent = Intent(requireContext(), ElderlyDashboardActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()      // Evita volver al login
                        }
                        Constants.USER_TYPE_CAREGIVER -> {
                            val intent = Intent(requireContext(), CaregiverDashboardActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        }
                        else -> Toast.makeText(
                            requireContext(),
                            "Tipo de usuario desconocido",
                            Toast.LENGTH_LONG
                        ).show()
                    }
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