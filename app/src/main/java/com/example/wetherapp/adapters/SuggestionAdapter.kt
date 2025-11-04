package com.example.wetherapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wetherapp.R

class SuggestionAdapter(private val onClick: (CityItem) -> Unit) : ListAdapter<CityItem, SuggestionAdapter.Holder>(Comparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return Holder(view, onClick)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    class Holder(view: View, private val onClick: (CityItem) -> Unit) : RecyclerView.ViewHolder(view) {
        private val text1 = view.findViewById<TextView>(android.R.id.text1)
        private val text2 = view.findViewById<TextView>(android.R.id.text2)
        fun bind(item: CityItem) {
            text1.text = item.city
            text2.text = item.region
            itemView.setOnClickListener { onClick(item) }
        }
    }

    class Comparator : DiffUtil.ItemCallback<CityItem>() {
        override fun areItemsTheSame(oldItem: CityItem, newItem: CityItem): Boolean = oldItem.city == newItem.city
        override fun areContentsTheSame(oldItem: CityItem, newItem: CityItem): Boolean = oldItem == newItem
    }
}