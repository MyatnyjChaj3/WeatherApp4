// Файл: utils/UnitConverter.kt
package com.example.wetherapp.utils

import java.math.RoundingMode

object UnitConverter {

    /**
     * Форматирует температуру из Цельсия в C или F.
     */
    fun formatTemp(celsius: String?, tempUnit: String): String {
        val c = celsius?.toFloatOrNull() ?: return ""
        return if (tempUnit == "F") {
            val f = (c * 9 / 5) + 32
            "${f.toRoundedInt()}°F"
        } else {
            "${c.toRoundedInt()}°C"
        }
    }

    /**
     * Форматирует скорость ветра из км/ч в км/ч, миль/ч или м/с.
     */
    fun formatWind(kph: String?, windUnit: String): String {
        val k = kph?.toFloatOrNull() ?: return ""
        return when (windUnit) {
            "MPH" -> {
                val mph = k * 0.621371f
                "${mph.toRoundedInt()} mph"
            }
            "MS" -> {
                val ms = k * 0.277778f
                "${ms.toRoundedInt()} m/s"
            }
            else -> "${k.toRoundedInt()} km/h"
        }
    }

    /**
     * Форматирует давление из гПа (mb) в гПа, мм рт.ст. или дюймы.
     */
    fun formatPressure(hpa: String?, pressureUnit: String): String {
        val h = hpa?.toFloatOrNull() ?: return ""
        return when (pressureUnit) {
            "MMHG" -> {
                val mmhg = h * 0.750062f
                "${mmhg.toRoundedInt()} mm Hg"
            }
            "IN" -> {
                val inhg = h * 0.02953f
                "${inhg.toRoundedString(2)} in"
            }
            else -> "${h.toRoundedInt()} hPa"
        }
    }

    private fun Float.toRoundedInt(): Int = this.toBigDecimal().setScale(0, RoundingMode.HALF_UP).toInt()
    private fun Float.toRoundedString(scale: Int): String = this.toBigDecimal().setScale(scale, RoundingMode.HALF_UP).toString()
}