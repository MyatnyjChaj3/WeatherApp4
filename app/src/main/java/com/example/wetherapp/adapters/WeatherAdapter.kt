package com.example.wetherapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wetherapp.R
import com.example.wetherapp.databinding.ListItemBinding
import com.example.wetherapp.utils.UnitConverter
import com.squareup.picasso.Picasso

class WeatherAdapter(val listener: Listener?) : ListAdapter<WeatherModel, WeatherAdapter.Holder>(Comparator()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return Holder(view, listener)
    }

    override fun onBindViewHolder(
        holder: Holder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    class Holder(view: View, val listener: Listener?) : RecyclerView.ViewHolder(view){
        val binding = ListItemBinding.bind(view)
        var itemTemp: WeatherModel? = null
        init {
            itemView.setOnClickListener {
                itemTemp?.let { it1 -> listener?.onClick(it1)}
            }
        }
        private fun getConditionString(apiCondition: String): String {
            val context = itemView.context
            val resourceKey = "condition_" + apiCondition
                .lowercase()
                .replace(" ", "_")
                .replace("-", "_")
                .replace("(", "")
                .replace(")", "")
                .replace(",", "")

            val resourceId = context.resources.getIdentifier(
                resourceKey,
                "string",
                context.packageName
            )
            return try {
                context.getString(resourceId)
            } catch (e: Exception) {
                apiCondition
            }
        }
        fun bind(item: WeatherModel) = with(binding){
            itemTemp = item
            val context = itemView.context
            val prefs = PreferenceManager.getDefaultSharedPreferences(itemView.context)
            val tempUnit = prefs.getString(itemView.context.getString(R.string.key_temp_unit), "C")!!

            // ИЗМЕНЕНО: Форматируем температуру
            val currentTempDisp = UnitConverter.formatTemp(item.currentTemp, tempUnit)
            val maxTempDisp = UnitConverter.formatTemp(item.maxTemp, tempUnit)
            val minTempDisp = UnitConverter.formatTemp(item.minTemp, tempUnit)
            val naString = itemView.context.getString(R.string.na)

            tvDate.text = item.time
            tvCondition2.text = getConditionString(item.condition)
            // ИЗМЕНЕНО: Используем форматированные значения

            val maxMin = "$maxTempDisp / $minTempDisp".replace("$naString / $naString", "")
            tvTemp.text = currentTempDisp.ifEmpty { maxMin }

            Picasso.get().load("https:" + item.imageUrl).into(im)

            val rainChance = item.chanceOfRain.toIntOrNull() ?: 0
            val snowChance = item.chanceOfSnow.toIntOrNull() ?: 0

            // Логика взята из MainFragment
            tvPrecip.text = when {
                rainChance > 0 && snowChance > 0 -> context.getString(R.string.rain_snow, "$rainChance", "$snowChance")
                rainChance > 0 -> context.getString(R.string.possible_rain, "$rainChance")
                snowChance > 0 -> context.getString(R.string.possible_snow, "$snowChance")
                else -> "" // В списке не показываем "Без осадков", оставляем пустым
            }
        }
    }

    class Comparator : DiffUtil.ItemCallback<WeatherModel>(){
        override fun areItemsTheSame(oldItem: WeatherModel, newItem: WeatherModel): Boolean {
           return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: WeatherModel, newItem: WeatherModel): Boolean {
            return oldItem == newItem
        }
    }
    interface Listener{
        fun onClick(item: WeatherModel)
    }
}