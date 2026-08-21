package com.liskovsoft.smartyoutubetv2.phone.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.liskovsoft.smartyoutubetv2.common.app.models.search.vineyard.Tag
import com.liskovsoft.smartyoutubetv2.phone.R

class SuggestionAdapter(
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.Holder>() {
    private val items = mutableListOf<String>()

    fun submit(values: List<String>) {
        items.clear()
        items.addAll(values)
        notifyDataSetChanged()
    }

    fun remove(tag: Tag) {
        val idx = items.indexOf(tag.tag)
        if (idx >= 0) {
            items.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_suggestion, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val value = items[position]
        holder.text.text = value
        holder.itemView.setOnClickListener { onClick(value) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.suggestion_text)
    }
}
