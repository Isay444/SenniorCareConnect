package com.isa.cuidadocompartidomayor.ui.agenda.dialog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import com.bumptech.glide.Glide
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.databinding.BottomSheetViewAllEventsBinding
import com.isa.cuidadocompartidomayor.ui.agenda.adapter.AgendaPagerAdapter
import com.isa.cuidadocompartidomayor.ui.agenda.viewmodel.AgendaViewModel

class ViewAllEventsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetViewAllEventsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AgendaViewModel by activityViewModels()
    private var profileImageUrl: String? = null
    private var elderlyName: String? = null

    companion object {
        private const val TAG = "ViewAllEventsBottomSheet"

        fun newInstance(elderlyName: String? = null, profileImageUrl: String? = null): ViewAllEventsBottomSheet {
            return ViewAllEventsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("elderlyName", elderlyName)
                    putString("profileImageUrl", profileImageUrl)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetViewAllEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            elderlyName = it.getString("elderlyName")
            profileImageUrl = it.getString("profileImageUrl")
        }

        setupHeader()
        setupViewPager()
        setupButtons()
        loadAllEvents()

        Log.d(TAG, "✅ ViewAllEventsBottomSheet inicializado y cargando eventos")
    }

    private fun setupHeader() {
        if (!elderlyName.isNullOrBlank()) {
            binding.tvTitle.text = "Eventos de $elderlyName"
        }

        if (!profileImageUrl.isNullOrBlank()) {
            binding.ivElderlyAvatar.visibility = View.VISIBLE
            Glide.with(this)
                .load(profileImageUrl)
                .placeholder(R.drawable.ic_avatar)
                .error(R.drawable.ic_avatar)
                .circleCrop()
                .into(binding.ivElderlyAvatar)
        } else if (!elderlyName.isNullOrBlank()) {
            // Mostrar avatar por defecto si hay nombre pero no foto
            binding.ivElderlyAvatar.visibility = View.VISIBLE
            binding.ivElderlyAvatar.setImageResource(R.drawable.ic_avatar)
        }
    }

    override fun onStart() {
        super.onStart()

        // ✅ AGREGAR: Expandir el BottomSheet al máximo
        val bottomSheet = dialog?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.skipCollapsed = true

            // ✅ IMPORTANTE: Establecer altura al 90% de la pantalla
            val windowHeight = requireContext().resources.displayMetrics.heightPixels
            val layoutParams = it.layoutParams
            layoutParams.height = (windowHeight * 0.9).toInt()
            it.layoutParams = layoutParams
        }
    }

    private fun setupViewPager() {
        // Reutilizamos el mismo adapter de pestañas
        val adapter = AgendaPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Citas Médicas"
                1 -> "Tareas"
                else -> ""
            }
        }.attach()
    }

    private fun setupButtons() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun loadAllEvents() {
        viewModel.loadAllEvents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
