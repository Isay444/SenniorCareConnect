package com.isa.cuidadocompartidomayor.ui.elderly

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.isa.cuidadocompartidomayor.R
import android.app.AlertDialog
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.isa.cuidadocompartidomayor.data.model.CaregiverRequest
import com.isa.cuidadocompartidomayor.databinding.FragmentCaregiverRequestsBinding
import com.isa.cuidadocompartidomayor.ui.elderly.adapter.CaregiverRequestAdapter
import com.isa.cuidadocompartidomayor.ui.elderly.viewmodel.CaregiverRequestViewModel


class CaregiverRequestsFragment : Fragment() {
    private var _binding: FragmentCaregiverRequestsBinding? = null
    private val binding get() = _binding!!
    private val caregiverRequestViewModel: CaregiverRequestViewModel by viewModels()
    private lateinit var pendingRequestsAdapter: CaregiverRequestAdapter
    private lateinit var approvedCaregiversAdapter: CaregiverRequestAdapter

    companion object {
        private const val TAG = "CaregiverRequestsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCaregiverRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupSwipeRefresh()
        setupObservers()

        Log.d(TAG, "✅ CaregiverRequestsFragment inicializado")
    }

    /** Configura los RecyclerViews para solicitudes pendientes y cuidadores aprobados */
    private fun setupRecyclerViews() {
        // Adapter para solicitudes pendientes
        pendingRequestsAdapter = CaregiverRequestAdapter(
            onApproveClick = { request ->
                showApprovalConfirmation(request)
            },
            onRejectClick = { request ->
                showRejectionConfirmation(request)
            },
            onRemoveClick = null, // No se usa para solicitudes pendientes
            isApprovedList = false
        )

        binding.rvPendingRequests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pendingRequestsAdapter
        }

        // Adapter para cuidadores aprobados
        approvedCaregiversAdapter = CaregiverRequestAdapter(
            onApproveClick = null, // No se usa para cuidadores aprobados
            onRejectClick = null, // No se usa para cuidadores aprobados
            onRemoveClick = { caregiver ->
                showRemovalConfirmation(caregiver)
            },
            isApprovedList = true
        )

        binding.rvApprovedCaregivers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = approvedCaregiversAdapter
        }

        Log.d(TAG, "RecyclerViews configurados")
    }

    /** Configura el SwipeRefreshLayout */
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            caregiverRequestViewModel.refreshData()
        }

        // Colores de la animación de refresh
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.green_one,
            R.color.azul_principal,
            R.color.blue_darker
        )

        Log.d(TAG, "SwipeRefresh configurado")
    }

    /** Configura los observadores del ViewModel */
    private fun setupObservers() {
        // Solicitudes pendientes
        caregiverRequestViewModel.pendingRequests.observe(viewLifecycleOwner) { requests ->
            updatePendingRequests(requests)
        }

        // Cuidadores aprobados
        caregiverRequestViewModel.approvedCaregivers.observe(viewLifecycleOwner) { caregivers ->
            updateApprovedCaregivers(caregivers)
        }

        // Estado de carga general
        caregiverRequestViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            updateLoadingState(isLoading)
        }

        // Estado de refresh
        caregiverRequestViewModel.isRefreshing.observe(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefreshLayout.isRefreshing = isRefreshing
        }

        // Mensajes de éxito
        caregiverRequestViewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                showSuccessMessage(it)
                caregiverRequestViewModel.clearMessages()
            }
        }

        // Mensajes de error
        caregiverRequestViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                showErrorMessage(it)
                caregiverRequestViewModel.clearMessages()
            }
        }

        // Contador de solicitudes pendientes
        caregiverRequestViewModel.pendingCount.observe(viewLifecycleOwner) { count ->
            updatePendingCountBadge(count)
        }

        Log.d(TAG, "Observadores configurados")
    }

    /**
     * Actualiza la lista de solicitudes pendientes
     */
    private fun updatePendingRequests(requests: List<CaregiverRequest>) {
        if (requests.isNotEmpty()) {
            binding.layoutNoPendingRequests.visibility = View.GONE
            binding.layoutPendingRequests.visibility = View.VISIBLE

            binding.tvPendingCount.text = "${requests.size} solicitud${if (requests.size != 1) "es" else ""} pendiente${if (requests.size != 1) "s" else ""}"

            pendingRequestsAdapter.updateRequests(requests)
        } else {
            binding.layoutNoPendingRequests.visibility = View.VISIBLE
            binding.layoutPendingRequests.visibility = View.GONE
        }

        Log.d(TAG, "Solicitudes pendientes actualizadas: ${requests.size}")
    }

    /**
     * Actualiza la lista de cuidadores aprobados
     */
    private fun updateApprovedCaregivers(caregivers: List<CaregiverRequest>) {
        if (caregivers.isNotEmpty()) {
            binding.layoutNoApprovedCaregivers.visibility = View.GONE
            binding.layoutApprovedCaregivers.visibility = View.VISIBLE

            binding.tvApprovedCount.text = "${caregivers.size} cuidador${if (caregivers.size != 1) "es" else ""} conectado${if (caregivers.size != 1) "s" else ""}"

            approvedCaregiversAdapter.updateRequests(caregivers)
        } else {
            binding.layoutNoApprovedCaregivers.visibility = View.VISIBLE
            binding.layoutApprovedCaregivers.visibility = View.GONE
        }

        Log.d(TAG, "Cuidadores aprobados actualizados: ${caregivers.size}")
    }

    /**
     * Actualiza el estado de carga general
     */
    private fun updateLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

        // Desactivar SwipeRefresh si está cargando de otra manera
        if (isLoading && !binding.swipeRefreshLayout.isRefreshing) {
            binding.swipeRefreshLayout.isEnabled = false
        } else if (!isLoading) {
            binding.swipeRefreshLayout.isEnabled = true
        }
    }

    /** Actualiza el badge de contador de solicitudes pendientes */
    private fun updatePendingCountBadge(count: Int) {
        // TODO: Actualizar badge en bottom navigation si es necesario
        Log.d(TAG, "Contador de solicitudes pendientes: $count")
    }

    /** Muestra confirmación para aprobar una solicitud */
    private fun showApprovalConfirmation(request: CaregiverRequest) {
        val confirmationText = caregiverRequestViewModel.getApprovalConfirmationText(request)

        AlertDialog.Builder(requireContext(), R.style.Theme_ElderlyDialog)
            .setTitle("✅ Aprobar Cuidador")
            .setMessage(confirmationText)
            .setPositiveButton("SÍ, APROBAR") { _, _ ->
                approveRequest(request)
            }
            .setNegativeButton("CANCELAR", null)
            .show()

        Log.d(TAG, "Mostrando confirmación de aprobación para: ${request.caregiverName}")
    }

    /** Muestra confirmación para rechazar una solicitud */
    private fun showRejectionConfirmation(request: CaregiverRequest) {
        val confirmationText = caregiverRequestViewModel.getRejectionConfirmationText(request)

        AlertDialog.Builder(requireContext(), R.style.Theme_ElderlyDialog)
            .setTitle("❌ Rechazar Solicitud")
            .setMessage(confirmationText)
            .setPositiveButton("SÍ, RECHAZAR") { _, _ ->
                rejectRequest(request)
            }
            .setNegativeButton("CANCELAR", null)
            .show()

        Log.d(TAG, "Mostrando confirmación de rechazo para: ${request.caregiverName}")
    }

    /** Muestra confirmación para remover un cuidador */
    private fun showRemovalConfirmation(caregiver: CaregiverRequest) {
        val confirmationText = caregiverRequestViewModel.getRemovalConfirmationText(caregiver)

        AlertDialog.Builder(requireContext(), R.style.Theme_ElderlyDialog)
            .setTitle("🗑️ Remover Cuidador")
            .setMessage(confirmationText)
            .setPositiveButton("SÍ, REMOVER") { _, _ ->
                removeCaregiver(caregiver)
            }
            .setNegativeButton("CANCELAR", null)
            .show()

        Log.d(TAG, "Mostrando confirmación de remoción para: ${caregiver.caregiverName}")
    }

    /** Aprueba una solicitud */
    private fun approveRequest(request: CaregiverRequest) {
        caregiverRequestViewModel.approveRequest(request)
        Log.d(TAG, "Aprobando solicitud: ${request.id}")
    }

    /** Rechaza una solicitud */
    private fun rejectRequest(request: CaregiverRequest) {
        caregiverRequestViewModel.rejectRequest(request)
        Log.d(TAG, "Rechazando solicitud: ${request.id}")
    }

    /** Remueve un cuidador aprobado */
    private fun removeCaregiver(caregiver: CaregiverRequest) {
        caregiverRequestViewModel.removeCaregiver(caregiver)
        Log.d(TAG, "Removiendo cuidador: ${caregiver.id}")
    }

    /** Muestra mensaje de éxito con estilo accesible */
    private fun showSuccessMessage(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    /** Muestra mensaje de error con estilo accesible */
    private fun showErrorMessage(error: String) {
        Toast.makeText(
            requireContext(),
            "❌ $error",
            Toast.LENGTH_LONG
        ).show()
    }

    /** Refresca los datos cuando la pantalla vuelve a ser visible */
    override fun onResume() {
        super.onResume()
        caregiverRequestViewModel.loadAllData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "Vista destruida - recursos limpiados")
    }
}