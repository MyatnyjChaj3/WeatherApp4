package com.example.wetherapp.adapters

data class WeatherModel(
    val city: String,
    val time: String,
    val condition: String,
    val currentTemp: String,
    val maxTemp: String,
    val minTemp: String,
    val imageUrl: String,
    val hours: String,
    val feelsLikeTemp: String,
    val humidity: String,
    val windKph: String,
    val windDir: String,
    val pressureMb: String,
    val visibilityKm: String,
    val sunrise: String,
    val sunset: String,
    val chanceOfRain: String,
    val chanceOfSnow: String

)
