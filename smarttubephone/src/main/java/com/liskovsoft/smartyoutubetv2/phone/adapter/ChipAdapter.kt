package com.liskovsoft.smartyoutubetv2.phone.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.liskovsoft.smartyoutubetv2.phone.R

data class PhoneChip(
    val id: Int,
    val title: String,
    val sectionId: Int
)

class ChipAdapter(
    private val onClick: (PhoneChip) -> Unit
) : RecyclerView.Adapter<ChipAdapter.Holder>() {
    private val items = mutableListOf<PhoneChip>()
    private var selectedId: Int? = null

    fun submit(chips: List<PhoneChip>, selected: Int?) {
        items.clear()
        items.addAll(chips)
        selectedId = selected
        notifyDataSetChanged()
    }

    fun select(id: Int) {
        val old = selectedId
        selectedId = id
        val oldIndex = items.indexOfFirst { it.id == old }
        val newIndex = items.indexOfFirst { it.id == id }
        if (oldIndex >= 0) notifyItemChanged(oldIndex)
        if (newIndex >= 0) notifyItemChanged(newIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chip, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val chip = items[position]
        val selected = chip.id == selectedId
        holder.title.text = chip.title
        holder.title.setBackgroundResource(if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip)
        holder.title.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (selected) R.color.phone_bg else R.color.phone_on_surface
            )
        )
        holder.itemView.setOnClickListener { onClick(chip) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.chip_title)
    }
}
