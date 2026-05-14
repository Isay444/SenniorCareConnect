package com.isa.cuidadocompartidomayor.ui.agenda.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.bumptech.glide.Glide
import com.isa.cuidadocompartidomayor.R
import com.isa.cuidadocompartidomayor.data.model.ElderlyItem
import com.isa.cuidadocompartidomayor.databinding.ItemSpinnerElderlyBinding

class ElderlySpinnerAdapter(
    context: Context,
    private val elderlyList: List<ElderlyItem>
) : ArrayAdapter<ElderlyItem>(context, R.layout.item_spinner_elderly, elderlyList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding: ItemSpinnerElderlyBinding
        val view: View

        if (convertView == null) {
            binding = ItemSpinnerElderlyBinding.inflate(LayoutInflater.from(context), parent, false)
            view = binding.root
            view.tag = binding
        } else {
            view = convertView
            binding = view.tag as ItemSpinnerElderlyBinding
        }

        val elderly = elderlyList[position]
        binding.tvElderlyName.text = elderly.name

        if (elderly.id == "all") {
            binding.ivElderlyAvatar.setImageResource(R.drawable.ic_avatar)
            binding.ivElderlyAvatar.setPadding(8, 8, 8, 8)
        } else {
            binding.ivElderlyAvatar.setPadding(0, 0, 0, 0)
            Glide.with(context)
                .load(elderly.profileImageUrl)
                .placeholder(R.drawable.ic_avatar)
                .error(R.drawable.ic_avatar)
                .circleCrop()
                .into(binding.ivElderlyAvatar)
        }

        return view
    }
}
