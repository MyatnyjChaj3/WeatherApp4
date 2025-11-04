package com.example.wetherapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.wetherapp.adapters.CityItem
import com.example.wetherapp.adapters.WeatherModel

class MainViewModel : ViewModel() {
    val liveDataCurrent = MutableLiveData<WeatherModel>()
    val liveDataList = MutableLiveData<List<WeatherModel>>()
    val isUsingLocation = MutableLiveData<Boolean>(true)
    private val _pendingRequest = MutableLiveData<String?>(null)
    val pendingRequest: LiveData<String?> = _pendingRequest
    fun setCurrentCity(city: String) {
        _pendingRequest.value = "city:$city"
        isUsingLocation.value = false
    }
    fun requestLocationUpdate(coords: String) {
        _pendingRequest.value = "coords:$coords"
        isUsingLocation.value = true
    }

    // Дописал: очистка pendingRequest после обработки
    fun clearPendingRequest() {
        _pendingRequest.value = null
    }
    fun notifyUnitsChanged() {
        // Повторная установка того же значения заставит LiveData
        // оповестить своих наблюдателей
        liveDataCurrent.value = liveDataCurrent.value
        liveDataList.value = liveDataList.value
    }

}