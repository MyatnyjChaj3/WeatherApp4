package com.example.wetherapp.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.wetherapp.MainViewModel
import com.example.wetherapp.R
import com.example.wetherapp.adapters.CityItem
import com.example.wetherapp.adapters.FavoritesAdapter
import com.example.wetherapp.adapters.HistoryAdapter
import com.example.wetherapp.adapters.SuggestionAdapter
import com.example.wetherapp.databinding.FragmentCityBinding
import com.example.wetherapp.managers.DialogManager
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

const val PREFS_FAVORITES = "prefs_favorites"
const val PREFS_SEARCH_HISTORY = "prefs_search_history"
const val MAX_FAVORITES = 10
private const val MAX_HISTORY = 5

class CityFragment : Fragment() {
    private lateinit var binding: FragmentCityBinding
    private val model: MainViewModel by activityViewModels()
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var favoritesAdapter: FavoritesAdapter
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var suggestionAdapter: SuggestionAdapter
    private lateinit var queue: RequestQueue

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPrefs = requireContext().getSharedPreferences("city_prefs", Context.MODE_PRIVATE)
        queue = Volley.newRequestQueue(requireContext())
        initRecyclerViews()
        loadFavoritesAndHistory()
        setupSearchListener()
        setupClearHistory()
        initTopBar()
    }

    private fun initTopBar() {
        binding.ibSettings.setOnClickListener {
            // ИЗМЕНЕНО: Открываем SettingsFragment в основном контейнере
            parentFragmentManager.beginTransaction()
                .replace(R.id.placeholder, SettingsFragment.newInstance()) // <-- Используем R.id.placeholder
                .addToBackStack("settings")
                .commit()
        }
    }

    private fun initRecyclerViews() = with(binding) {
        // Suggestions
        rvSuggestions.layoutManager = LinearLayoutManager(requireContext())
        suggestionAdapter = SuggestionAdapter { item -> onItemClick(item) }
        rvSuggestions.adapter = suggestionAdapter

        // Favorites
        rvFavorites.layoutManager = LinearLayoutManager(requireContext())
        favoritesAdapter = FavoritesAdapter({ item -> onItemClick(item) }, { city -> removeFromFavorites(city) })
        rvFavorites.adapter = favoritesAdapter

        // History
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        historyAdapter = HistoryAdapter { query -> onItemClick(query) }
        rvHistory.adapter = historyAdapter
    }

    private fun setupSearchListener() {
        var searchHandler = Handler(Looper.getMainLooper())
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                searchHandler.removeCallbacksAndMessages(null)
                if (query.length > 2) {
                    searchHandler.postDelayed({
                        searchCities(query)
                        binding.rvSuggestions.visibility = View.VISIBLE
                    }, 300)
                } else {
                    binding.rvSuggestions.visibility = View.GONE
                    suggestionAdapter.submitList(emptyList())
                }
            }
        })
    }

    private fun searchCities(query: String) {
        val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val lang = defaultPrefs.getString(getString(R.string.key_language), "RU")?.lowercase()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.weatherapi.com/v1/search.json?key=$API_KEY&q=$encodedQuery&lang=$lang"


        val request = object : StringRequest(Request.Method.GET, url,
            { result ->
                // ... (ваш код парсинга JSON) ...
                val jsonArray = JSONArray(result)
                val suggestions = mutableListOf<CityItem>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val city = obj.getString("name")
                    val region = obj.getString("region")
                    suggestions.add(CityItem(city, region, "",""))
                }
                suggestionAdapter.submitList(suggestions)
            },
            { error -> Log.e("CitySearch", "Error: $error") }
        ) {
            override fun parseNetworkResponse(response: com.android.volley.NetworkResponse?): Response<String> {
                // Используем Kotlin-объект Charsets.UTF_8
                val parsed = String(response?.data ?: byteArrayOf(), Charsets.UTF_8)

                return Response.success(
                    parsed,
                    HttpHeaderParser.parseCacheHeaders(response)
                )
            }
        }
        queue.add(request)
    }

    private fun loadFavoritesAndHistory() {
        loadFavorites()
        loadHistory()
    }

    private fun loadFavorites() {
        val json = sharedPrefs.getString(PREFS_FAVORITES, "[]") ?: "[]"
        val array = JSONArray(json)

        val cityNames = (0 until array.length()).map { array.getString(it) }

        if (cityNames.isEmpty()) {
            favoritesAdapter.submitList(emptyList())
            return
        }

        val resultMap = mutableMapOf<String, CityItem>()
        var pending = cityNames.size

        for (cityStr in cityNames) {
            fetchTempForCity(cityStr) { temp, iconUrl ->

                val item = CityItem(
                    city = cityStr,
                    region = "",
                    currentTemp = if (temp.isNotEmpty()) temp else getString(R.string.na),
                    ImageUrl = iconUrl
                )

                synchronized(resultMap) {
                    resultMap[cityStr] = item
                }

                pending--
                if (pending == 0) {
                    val finalList = cityNames
                        .mapNotNull { resultMap[it] }
                        .sortedBy { it.city }

                    activity?.runOnUiThread {
                        favoritesAdapter.submitList(finalList)
                    }
                }
            }
        }
    }

    private fun loadHistory() {
        val json = sharedPrefs.getString(PREFS_SEARCH_HISTORY, "[]") ?: "[]"
        val array = JSONArray(json)
        val history = mutableListOf<String>()
        for (i in 0 until array.length()) {
            history.add(array.getString(i))
        }
        history.reverse()
        historyAdapter.submitList(history)
        binding.btnClearHistory.visibility = if (history.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun fetchTempForCity(city: String, callback: (temp: String, iconUrl: String) -> Unit) {
        val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val lang = defaultPrefs.getString(getString(R.string.key_language), "RU")?.lowercase()
        val encodedCity = URLEncoder.encode(city, "UTF-8")
        val url = "https://api.weatherapi.com/v1/forecast.json?key=$API_KEY&q=$encodedCity&days=1&aqi=no&alerts=no&lang=$lang"

        val request = object : StringRequest(Request.Method.GET, url,
            { result ->
                try {
                    val json = JSONObject(result)
                    val current = json.getJSONObject("current")
                    val condition = current.getJSONObject("condition")

                    val temp = current.getString("temp_c").toFloat().toInt().toString()
                    val iconUrl = condition.getString("icon")

                    callback(temp, iconUrl)

                } catch (e: Exception) {
                    callback("", "") // Возвращаем пустые строки при ошибке
                    Log.e("FetchTemp", "Error: $e")
                }
            },
            {
                callback("", "") // Возвращаем пустые строки при ошибке
                Log.e("FetchTemp", "Network error")
            }
        ) {
            // Вставляем ваш идиоматичный парсер UTF-8
            override fun parseNetworkResponse(response: com.android.volley.NetworkResponse?): com.android.volley.Response<String> {
                val parsed = String(response?.data ?: byteArrayOf(), Charsets.UTF_8)
                return com.android.volley.Response.success(
                    parsed,
                    com.android.volley.toolbox.HttpHeaderParser.parseCacheHeaders(response)
                )
            }
        }
        queue.add(request)
    }

    private fun addToFavorites(city: String) {
        if (favoritesAdapter.currentList.size >= MAX_FAVORITES) {

            Toast.makeText(requireContext(), getString(R.string.toast_max_favorites), Toast.LENGTH_SHORT).show()
            return
        }
        val json = sharedPrefs.getString(PREFS_FAVORITES, "[]") ?: "[]"
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            if (array.getString(i) == city) {

                Toast.makeText(requireContext(), getString(R.string.toast_already_favorite), Toast.LENGTH_SHORT).show()
                return
            }
        }
        array.put(city)
        sharedPrefs.edit().putString(PREFS_FAVORITES, array.toString()).apply()
        loadFavorites()

        Toast.makeText(requireContext(), getString(R.string.toast_added_favorite), Toast.LENGTH_SHORT).show()
    }

    fun removeFromFavorites(city: String) {
        val json = sharedPrefs.getString(PREFS_FAVORITES, "[]") ?: "[]"
        val array = JSONArray(json)
        val newArray = JSONArray()
        for (i in 0 until array.length()) {
            if (array.getString(i) != city) newArray.put(array.getString(i))
        }
        sharedPrefs.edit().putString(PREFS_FAVORITES, newArray.toString()).apply()
        loadFavorites()
        Toast.makeText(requireContext(), getString(R.string.toast_removed_favorite), Toast.LENGTH_SHORT).show()
    }

    private fun addToHistory(query: String) {
        val json = sharedPrefs.getString(PREFS_SEARCH_HISTORY, "[]") ?: "[]"
        val array = JSONArray(json)
        val historyList = mutableListOf<String>()

        for (i in 0 until array.length()) {
            historyList.add(array.getString(i))
        }

        historyList.removeAll { it == query }
        historyList.add(query)
        while (historyList.size > MAX_HISTORY) {
            historyList.removeAt(0)
        }

        val newArray = JSONArray(historyList)
        sharedPrefs.edit().putString(PREFS_SEARCH_HISTORY, newArray.toString()).apply()
        loadHistory() // Обновляем UI
    }

    private fun clearHistory() {
        sharedPrefs.edit().remove(PREFS_SEARCH_HISTORY).apply()
        loadHistory()
        Toast.makeText(context, getString(R.string.toast_history_cleared), Toast.LENGTH_SHORT).show()
    }

    private fun setupClearHistory() {
        binding.btnClearHistory.setOnClickListener { clearHistory() }
    }

    private fun onItemClick(item: Any) {
        val city = when (item) {
            is CityItem -> item.city
            is String -> item
            else -> return
        }
        addToHistory(city)


        model.setCurrentCity(city)
        binding.root.post {
            parentFragmentManager.popBackStack()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = CityFragment()
    }
}

