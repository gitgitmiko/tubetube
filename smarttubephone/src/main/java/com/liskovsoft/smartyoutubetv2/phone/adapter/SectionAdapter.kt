package com.liskovsoft.smartyoutubetv2.phone.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection
import com.liskovsoft.smartyoutubetv2.phone.R

class SectionAdapter(
    private val onClick: (BrowseSection, Int) -> Unit
) : RecyclerView.Adapter<SectionAdapter.Holder>() {
    private val sections = mutableListOf<BrowseSection>()
    private var selectedIndex = 0

    fun setSections(items: List<BrowseSection>) {
        sections.clear()
        sections.addAll(items)
        notifyDataSetChanged()
    }

    fun addSection(index: Int, section: BrowseSection) {
        val insert = if (index < 0 || index > sections.size) sections.size else index
        val existing = sections.indexOfFirst { it.id == section.id }
        if (existing >= 0) {
            sections.removeAt(existing)
            notifyItemRemoved(existing)
        }
        sections.add(insert.coerceAtMost(sections.size), section)
        notifyItemInserted(insert.coerceAtMost(sections.size - 1))
    }

    fun removeSection(section: BrowseSection) {
        val idx = sections.indexOfFirst { it.id == section.id }
        if (idx >= 0) {
            sections.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }

    fun clearSections() {
        sections.clear()
        notifyDataSetChanged()
    }

    fun select(index: Int) {
        val old = selectedIndex
        selectedIndex = index.coerceIn(0, (sections.size - 1).coerceAtLeast(0))
        if (old in sections.indices) notifyItemChanged(old)
        if (selectedIndex in sections.indices) notifyItemChanged(selectedIndex)
    }

    fun getSection(index: Int): BrowseSection? = sections.getOrNull(index)

    fun indexOf(sectionId: Int): Int = sections.indexOfFirst { it.id == sectionId }

    fun size(): Int = sections.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_section, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val section = sections[position]
        holder.title.text = section.title
        val selected = position == selectedIndex
        holder.title.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        holder.title.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (selected) R.color.phone_accent else R.color.phone_on_surface
            )
        )
        holder.itemView.setOnClickListener { onClick(section, position) }
    }

    override fun getItemCount(): Int = sections.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.section_title)
    }
}
