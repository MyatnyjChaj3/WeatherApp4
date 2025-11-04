package com.example.wetherapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wetherapp.R
import com.example.wetherapp.databinding.ListItem2Binding
import com.example.wetherapp.databinding.ListItemBinding
import com.example.wetherapp.utils.UnitConverter
import com.squareup.picasso.Picasso

class FavoritesAdapter(
    private val onClick: (CityItem) -> Unit,
    private val onDelete: (String) -> Unit
) : ListAdapter<CityItem, FavoritesAdapter.Holder>(Comparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item2, parent, false)
        return Holder(view, onClick, onDelete)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    class Holder(
        view: View,
        private val onClick: (CityItem) -> Unit,
        private val onDelete: (String) -> Unit

    ) : RecyclerView.ViewHolder(view) {
        private val binding = ListItem2Binding.bind(view)
        private var item: CityItem? = null

        init {
            itemView.setOnClickListener { item?.let(onClick) }
            binding.ibDelete.setOnClickListener { item?.let { onDelete(it.city) } }  // Дописал: кнопка удаления
        }

        fun bind(item: CityItem) = with(binding) {
            this@Holder.item = item
            tvCity.text = item.city

            // --- ИЗМЕНЕНИЯ ЗДЕСЬ ---
            // 1. Получаем настройки
            val prefs = PreferenceManager.getDefaultSharedPreferences(itemView.context)
            val tempUnit = prefs.getString(itemView.context.getString(R.string.key_temp_unit), "C")!!
            val naString = itemView.context.getString(R.string.na) // "N/A"

            // 2. Конвертируем "сырое" значение (напр. "15") в "15°C" или "59°F"
            val tempDisplay = UnitConverter.formatTemp(item.currentTemp, tempUnit)

            // 3. Отображаем, с проверкой на N/A
            tvTemp.text = if(tempDisplay.contains(naString)) naString else tempDisplay

            // 4. Загружаем иконку
            Picasso.get().load("https:" + item.ImageUrl).into(im2)
            // --- КОНЕЦ ИЗМЕНЕНИЙ ---
        }
    }

    class Comparator : DiffUtil.ItemCallback<CityItem>() {
        override fun areItemsTheSame(oldItem: CityItem, newItem: CityItem): Boolean = oldItem.city == newItem.city
        override fun areContentsTheSame(oldItem: CityItem, newItem: CityItem): Boolean = oldItem == newItem
    }
}