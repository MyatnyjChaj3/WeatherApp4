package com.example.wetherapp.adapters

data class CityItem(
    val city: String,
    val region: String = "",
    var currentTemp: String = "N/A", // var для обновления температуры
    var ImageUrl: String
)
