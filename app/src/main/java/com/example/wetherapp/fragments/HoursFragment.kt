package com.example.wetherapp.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wetherapp.MainViewModel
import com.example.wetherapp.R
import com.example.wetherapp.adapters.WeatherAdapter
import com.example.wetherapp.adapters.WeatherModel
import com.example.wetherapp.databinding.FragmentHoursBinding
import org.json.JSONArray
import org.json.JSONObject


class HoursFragment : Fragment() {

    private lateinit var binding: FragmentHoursBinding
    private lateinit var adapter: WeatherAdapter
    private val model: MainViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHoursBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRcView()
        model.liveDataCurrent.observe(viewLifecycleOwner){
            adapter.submitList(getHoursList(it))
        }
    }
    private fun initRcView() = with(binding){
        rcView.layoutManager = LinearLayoutManager(activity)
        adapter = WeatherAdapter(null)
        rcView.adapter = adapter

    }
    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        binding.rcView.adapter?.notifyDataSetChanged()
    }

    private fun getHoursList(witem: WeatherModel): List<WeatherModel>{
        val hoursArray = JSONArray(witem.hours)
        val list = ArrayList<WeatherModel>()
        for (i in 0 until hoursArray.length()){
            val hourObj = hoursArray[i] as JSONObject

            val conditionText = hourObj.getJSONObject("condition").getString("text")

            val item = WeatherModel(
                witem.city,
                hourObj.getString("time"),
                // 2. Переводим текст СРАЗУ ЗДЕСЬ
                getConditionString(conditionText),
                hourObj.getString("temp_c"),
                "",
                "",
                hourObj.getJSONObject("condition").getString("icon"),
                "",
                "","","",
                "","","","","",
                hourObj.getString("chance_of_rain"),
                hourObj.getString("chance_of_snow")
            )
            list.add(item)
        }
        return list
    }

    private fun getConditionString(apiCondition: String): String {
        val resourceKey = "condition_" + apiCondition
            .lowercase()
            .replace(" ", "_")
            .replace("-", "_")
            .replace("(", "")
            .replace(")", "")
            .replace(",", "")

        val resourceId = resources.getIdentifier(
            resourceKey,
            "string",
            requireContext().packageName
        )

        return try {
            getString(resourceId)
        } catch (e: Exception) {
            apiCondition // Возвращаем то, что прислал API, если перевода нет
        }
    }
    companion object {

        @JvmStatic
        fun newInstance() = HoursFragment()


    }
}