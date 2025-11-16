package com.example.wetherapp.adapters

data class CityItem(
    val city: String,
    val region: String = "",
    var currentTemp: String = "N/A",
    var ImageUrl: String
)
