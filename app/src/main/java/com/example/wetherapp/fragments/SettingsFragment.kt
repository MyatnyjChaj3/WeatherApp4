
package com.example.wetherapp.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.example.wetherapp.MainViewModel
import com.example.wetherapp.R
import com.example.wetherapp.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var prefs: SharedPreferences
    private val model: MainViewModel by activityViewModels()

    private lateinit var KEY_TEMP: String
    private lateinit var KEY_WIND: String
    private lateinit var KEY_PRESSURE: String
    private lateinit var KEY_LANG: String
    private lateinit var KEY_GPS_ACCURACY: String
    private lateinit var KEY_GPS_TIMEOUT: String
    private lateinit var KEY_UPDATE_FREQ: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        initKeys()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSettings()
        setupListeners()
    }

    private fun initKeys() {
        KEY_TEMP = getString(R.string.key_temp_unit)
        KEY_WIND = getString(R.string.key_wind_unit)
        KEY_PRESSURE = getString(R.string.key_pressure_unit)
        KEY_LANG = getString(R.string.key_language)
        KEY_GPS_ACCURACY = getString(R.string.key_gps_accuracy)
        KEY_GPS_TIMEOUT = getString(R.string.key_gps_timeout)
        KEY_UPDATE_FREQ = getString(R.string.key_update_frequency)
    }


    private fun loadSettings() = with(binding) {

        val celsiusUnit = getString(R.string.unit_celsius).replace("%d", "").trim()
        val fahrenheitUnit = getString(R.string.unit_fahrenheit).replace("%d", "").trim()

        rbCelsius.text = celsiusUnit
        rbFahrenheit.text = fahrenheitUnit
        rgTemperature.check(
            if (prefs.getString(KEY_TEMP, "C") == "F") rbFahrenheit.id else rbCelsius.id
        )

        rgWind.check(
            when (prefs.getString(KEY_WIND, "KPH")) {
                "MPH" -> rbMph.id
                "MS" -> rbMs.id
                else -> rbKmh.id
            }
        )

        rgPressure.check(
            when (prefs.getString(KEY_PRESSURE, "HPA")) {
                "MMHG" -> rbMmHg.id
                "IN" -> rbInHg.id
                else -> rbMb.id // hPa (mb)
            }
        )

        switchLanguage.isChecked = prefs.getString(KEY_LANG, "RU") == "RU"
        switchLanguage.text = getString(R.string.settings_lang_ru)

        val freqValues = resources.getStringArray(R.array.update_frequency_values)
        val currentFreq = prefs.getString(KEY_UPDATE_FREQ, "300000") // 5 мин по умолч.
        spinnerUpdateFrequency.setSelection(freqValues.indexOf(currentFreq).coerceAtLeast(0))

        val accuracyValues = resources.getStringArray(R.array.gps_accuracy_values)
        val currentAccuracy = prefs.getString(KEY_GPS_ACCURACY, "High")
        spinnerGpsAccuracy.setSelection(accuracyValues.indexOf(currentAccuracy).coerceAtLeast(0))

        etGpsTimeout.setText(prefs.getString(KEY_GPS_TIMEOUT, "10"))
    }

    private fun setupListeners() = with(binding) {

        rgTemperature.setOnCheckedChangeListener { _, checkedId ->
            prefs.edit().putString(KEY_TEMP, if (checkedId == rbFahrenheit.id) "F" else "C").apply()
            model.notifyUnitsChanged()
        }

        rgWind.setOnCheckedChangeListener { _, checkedId ->
            val value = when (checkedId) {
                rbMph.id -> "MPH"
                rbMs.id -> "MS"
                else -> "KPH"
            }
            prefs.edit().putString(KEY_WIND, value).apply()
            model.notifyUnitsChanged()
        }

        rgPressure.setOnCheckedChangeListener { _, checkedId ->
            val value = when (checkedId) {
                rbMmHg.id -> "MMHG"
                rbInHg.id -> "IN"
                else -> "HPA"
            }
            prefs.edit().putString(KEY_PRESSURE, value).apply()
            model.notifyUnitsChanged()
        }

        switchLanguage.setOnCheckedChangeListener { _, isChecked ->
            val lang = if (isChecked) "ru" else "en"
            prefs.edit().putString(KEY_LANG, lang.uppercase()).apply()


            val appLocale = LocaleListCompat.forLanguageTags(lang)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }

        val freqValues = resources.getStringArray(R.array.update_frequency_values)
        spinnerUpdateFrequency.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putString(KEY_UPDATE_FREQ, freqValues[position]).apply()
            }
        }

        val accuracyValues = resources.getStringArray(R.array.gps_accuracy_values)
        spinnerGpsAccuracy.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putString(KEY_GPS_ACCURACY, accuracyValues[position]).apply()
            }
        }

        etGpsTimeout.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                prefs.edit().putString(KEY_GPS_TIMEOUT, s.toString()).apply()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}