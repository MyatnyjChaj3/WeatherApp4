package com.example.wetherapp.fragments

import android.Manifest
import android.content.Context
import com.example.wetherapp.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.wetherapp.MainViewModel
import com.example.wetherapp.R
import com.example.wetherapp.adapters.VpAdapter
import com.example.wetherapp.adapters.WeatherModel
import com.example.wetherapp.databinding.FragmentMainBinding
import com.example.wetherapp.utils.UnitConverter
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

const val API_KEY = "your api (weatherapi.com)"

@Suppress("DEPRECATION")
class MainFragment : Fragment() {
    private lateinit var locationManager: LocationManager
    private val flist = listOf(
        HoursFragment.newInstance(),
        DaysFragment.newInstance()
    )
    private lateinit var tList: List<String>
    private lateinit var pLauncher: ActivityResultLauncher<String>
    private lateinit var binding: FragmentMainBinding
    private val model: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tList = listOf(
            getString(R.string.tab_hours),
            getString(R.string.tab_days)
        )
        checkPermission()
        initLocationManager()
        initUi()
        updateCurrentCard()
        observePendingRequest()
        model.liveDataCurrent.observe(viewLifecycleOwner) { updateCurrentCard() }


    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onResume() {
        super.onResume()

        model.liveDataCurrent.value?.let {
            model.liveDataCurrent.value = it
        }
        model.liveDataList.value?.let {
            model.liveDataList.value = model.liveDataList.value?.map { it.copy() }

        }

        if (model.isUsingLocation.value == true) {
            locationManager.requestLocation()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        locationManager.onDestroy()
    }
    private fun initLocationManager() {
        locationManager = LocationManager(
            context = requireContext(),
            onLocationReceived = { coords ->
                requestWeatherData(coords)
            },
            onShowProgress = { show ->
                binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
            },
            onShowGpsIndicator = { show ->
                binding.ivGpsIndicator.visibility = if (show) View.VISIBLE else View.GONE
            }
        )
    }

    private fun initUi() = with(binding) {
        val adapter = VpAdapter(requireActivity(), flist)
        vp.adapter = adapter
        TabLayoutMediator(tabLayout, vp) { tab, pos -> tab.text = tList[pos] }.attach()

        fabSync.setOnClickListener {
            tabLayout.selectTab(tabLayout.getTabAt(0))
            locationManager.requestLocation()
        }

        initTopBar()
    }
    private fun initTopBar() = with(binding) {
        ibMenu.setOnClickListener {
            openCityFragment()
        }
        ibAddFavorite.setOnClickListener {
            val currentCity = model.liveDataCurrent.value?.city ?: return@setOnClickListener
            val sharedPrefs = requireContext().getSharedPreferences("city_prefs", Context.MODE_PRIVATE)
            val json = sharedPrefs.getString(PREFS_FAVORITES, "[]") ?: "[]"
            val array = JSONArray(json)
            if (array.length() >= MAX_FAVORITES) {

                Toast.makeText(requireContext(), getString(R.string.toast_max_favorites), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            var exists = false
            for (i in 0 until array.length()) {
                if (array.getString(i) == currentCity) {
                    exists = true
                    break
                }
            }
            if (exists) {
                Toast.makeText(requireContext(), getString(R.string.toast_already_favorite), Toast.LENGTH_SHORT).show()
            } else {
                array.put(currentCity)
                sharedPrefs.edit().putString(PREFS_FAVORITES, array.toString()).apply()
                Toast.makeText(requireContext(), getString(R.string.toast_added_favorite), Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun openCityFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.placeholder, CityFragment.newInstance())
            .addToBackStack(null)
            .commit()
    }

    private fun updateCurrentCard() = with(binding){
        model.liveDataCurrent.observe(viewLifecycleOwner){

            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val tempUnit = prefs.getString(getString(R.string.key_temp_unit), "C")!!
            val windUnit = prefs.getString(getString(R.string.key_wind_unit), "KPH")!!
            val pressureUnit = prefs.getString(getString(R.string.key_pressure_unit), "HPA")!!
            val lang = prefs.getString(getString(R.string.key_language), "RU")!!
            val maxTempDisp = UnitConverter.formatTemp(requireContext(), it.maxTemp, tempUnit)
            val minTempDisp = UnitConverter.formatTemp(requireContext(), it.minTemp, tempUnit)
            val currentTempDisp = UnitConverter.formatTemp(requireContext(), it.currentTemp, tempUnit)
            val feelsLikeDisp = UnitConverter.formatTemp(requireContext(), it.feelsLikeTemp, tempUnit)
            val windDisp = UnitConverter.formatWind(requireContext(), it.windKph, windUnit)
            val pressureDisp = UnitConverter.formatPressure(requireContext(), it.pressureMb, pressureUnit)
            val maxMinTemp = "$maxTempDisp / $minTempDisp"
            tvData.text = it.time
            tvCity.text = it.city

            tvCondition.text = getConditionString(it.condition)
            tvCurrentTemp.text = if (currentTempDisp == getString(R.string.na)) maxMinTemp else currentTempDisp
            tvMaxMin.text = if (currentTempDisp == getString(R.string.na)) "" else maxMinTemp
            tvFeelsLike.text = if (it.feelsLikeTemp.isEmpty()) "" else getString(R.string.feels_like, feelsLikeDisp)
            Picasso.get().load("https:" + it.imageUrl).into(imWeather)

            tvHumidity.text = if (it.humidity.isEmpty()) "" else "${it.humidity}%"
            val direction = if (it.windDir.isNotEmpty()) " ${getWindDirectionString(it.windDir)}" else ""
            tvWind.text = if (it.windKph.isEmpty()) "" else windDisp + direction
            tvPressure.text = if (it.pressureMb.isEmpty()) "" else pressureDisp
            tvVisibility.text = if (it.visibilityKm.isEmpty()) "" else getString(R.string.unit_visibility_km, it.visibilityKm)

            tvSunrise.text = formatTime(it.sunrise, lang)
            tvSunset.text = formatTime(it.sunset, lang)

            val rainChance = it.chanceOfRain.toIntOrNull() ?: 0
            val snowChance = it.chanceOfSnow.toIntOrNull() ?: 0
            tvPrecip.text = when {
                rainChance > 0 && snowChance > 0 -> getString(R.string.rain_snow, "$rainChance", "$snowChance")
                rainChance > 0 -> getString(R.string.possible_rain, "$rainChance")
                snowChance > 0 -> getString(R.string.possible_snow, "$snowChance")
                else -> getString(R.string.no_precipitation)
            }
        }
    }

    private fun observePendingRequest() {
        model.pendingRequest.observe(viewLifecycleOwner) { request ->
            request?.let {
                if (it.startsWith("city:")) {
                    val city = it.substringAfter("city:")
                    requestWeatherData(city)
                } else if (it.startsWith("coords:")) {
                    val coords = it.substringAfter("coords:")
                    requestWeatherData(coords)
                }
                model.clearPendingRequest()
            }
        }
    }
    private fun permissionListener(){
        pLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){

        }
    }

    private fun checkPermission(){
        if(!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)){
            permissionListener()
            pLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    private fun requestWeatherData(city: String){
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val lang = prefs.getString(getString(R.string.key_language), "RU")?.lowercase()
        val encodedCity = URLEncoder.encode(city, "UTF-8")
        val url = "https://api.weatherapi.com/v1/forecast.json?key=" +
                API_KEY +
                "&q=" +
                encodedCity +
                "&days=" +
                "7" +
                "&aqi=no&alerts=no" +
                "&lang=$lang"
        val queue = Volley.newRequestQueue(context)

        // Преобразуем в object : StringRequest
        val request = object : StringRequest(
            Request.Method.GET,
            url,
            { result ->
                parseWeatherData(result)
            },
            { error ->
                Log.d("urlLog", "Error: $error")
            }
        ) {
            override fun parseNetworkResponse(response: com.android.volley.NetworkResponse?): com.android.volley.Response<String> {
                // Используем Kotlin-объект Charsets.UTF_8
                val parsed = String(response?.data ?: byteArrayOf(), Charsets.UTF_8)

                return com.android.volley.Response.success(
                    parsed,
                    com.android.volley.toolbox.HttpHeaderParser.parseCacheHeaders(response)
                )
            }
        }
        queue.add(request)
    }
    private fun parseWeatherData(result: String) {
        val mainObject = JSONObject(result)
        val list = parseDays(mainObject)
        parseCurrentData(mainObject, list[0])
    }

    private fun parseDays(mainObject: JSONObject): List<WeatherModel>{
        val list = ArrayList<WeatherModel>()
        val daysArray = mainObject.getJSONObject("forecast")
            .getJSONArray("forecastday")
        val name = mainObject.getJSONObject("location").getString("name")
        for (i in 0 until daysArray.length()){
            val day = daysArray[i] as JSONObject
            val conditionText = day.getJSONObject("day").getJSONObject("condition") // <-- Получаем текст
                .getString("text")
            val dayObj = day.getJSONObject("day")
            val astroObj = day.getJSONObject("astro")
            val item = WeatherModel(
                name,
                day.getString("date"),
                getConditionString(conditionText),
                "",
                day.getJSONObject("day").getString("maxtemp_c").toFloat().toInt().toString(),
                day.getJSONObject("day").getString("mintemp_c").toFloat().toInt().toString(),
                day.getJSONObject("day").getJSONObject("condition")
                    .getString("icon"),
                day.getJSONArray("hour").toString(),
                "",
                dayObj.getString("avghumidity"),
                dayObj.getString("maxwind_kph"),
                "",
                "",
                dayObj.getString("avgvis_km"),
                astroObj.getString("sunrise"),
                astroObj.getString("sunset"),
                dayObj.getString("daily_chance_of_rain"),
                dayObj.getString("daily_chance_of_snow")
            )
            list.add(item)
            Log.d("TempCheck", "max=${item.maxTemp}, min=${item.minTemp}, current=${item.currentTemp}")

        }

        model.liveDataList.value = list
        return list
    }
    private fun parseCurrentData(mainObject: JSONObject, weatherItem: WeatherModel) {
        val currentObj = mainObject.getJSONObject("current")
        val condObj = currentObj.getJSONObject("condition")
        val conditionText = condObj.getString("text")
        val forecastObj = mainObject.getJSONObject("forecast")
        val today = forecastObj.getJSONArray("forecastday").getJSONObject(0)
        val astroObj = today.getJSONObject("astro")
        val dayObj = today.getJSONObject("day")
        val item = WeatherModel(
            mainObject.getJSONObject("location").getString("name"),
            mainObject.getJSONObject("current").getString("last_updated"),
            getConditionString(conditionText),
            mainObject.getJSONObject("current").getString("temp_c").toFloat().toInt().toString(),
            weatherItem.maxTemp,
            weatherItem.minTemp,
            mainObject.getJSONObject("current")
                .getJSONObject("condition")
                .getString("icon"),
            weatherItem.hours,
            currentObj.getString("feelslike_c").toFloat().toInt().toString(),
            currentObj.getString("humidity"),
            currentObj.getString("wind_kph"),
            currentObj.getString("wind_dir"),
            currentObj.getString("pressure_mb"),
            currentObj.getString("vis_km"),
            astroObj.getString("sunrise"),
            astroObj.getString("sunset"),
            dayObj.getString("daily_chance_of_rain"),  // New
            dayObj.getString("daily_chance_of_snow")
        )
        model.liveDataCurrent.value = item
        Log.d("urlLog", "City: $item.city")
        Log.d("TempCheck", "max=${item.maxTemp}, min=${item.minTemp}, current=${item.currentTemp}")

    }

    private fun getConditionString(apiCondition: String): String {
        val ctx = context ?: return apiCondition
        val resourceKey = "condition_" + apiCondition
            .lowercase()
            .replace(" ", "_")
            .replace("-", "_")
            .replace("(", "")
            .replace(")", "")
            .replace(",", "")

        val resourceId = ctx.resources.getIdentifier(
            resourceKey,
            "string",
            ctx.packageName
        )

        return try {
            ctx.getString(resourceId)
        } catch (e: Exception) {
            apiCondition
        }
    }

    private fun formatTime(apiTime: String, lang: String): String {
        if (apiTime.isEmpty()) return ""
        return try {
            val inputFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.ENGLISH)
            val date = inputFormat.parse(apiTime) ?: return apiTime

            val outputFormatString = if (lang == "RU") "HH:mm" else "hh:mm a"
            val outputFormat = java.text.SimpleDateFormat(outputFormatString, java.util.Locale.getDefault())

            outputFormat.format(date)
        } catch (e: Exception) {
            apiTime
        }
    }

    private fun getWindDirectionString(apiDir: String): String {
        if (apiDir.isEmpty()) return ""

        val resourceKey = "wind_dir_" + apiDir.lowercase()

        val resourceId = resources.getIdentifier(
            resourceKey,
            "string",
            requireContext().packageName
        )

        return try {
            getString(resourceId)
        } catch (e: Exception) {
            apiDir
        }
    }
    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()

    }
}