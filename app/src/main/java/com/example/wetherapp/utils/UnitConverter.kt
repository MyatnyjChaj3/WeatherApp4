// Файл: utils/UnitConverter.kt
package com.example.wetherapp.utils

import android.annotation.SuppressLint
import android.content.Context
import com.example.wetherapp.R // <-- Убедитесь, что R импортирован
import java.math.RoundingMode
import java.util.Locale

object UnitConverter {

    /**
     * Форматирует температуру из Цельсия в C или F.
     */
    @SuppressLint("StringFormatInvalid")
    fun formatTemp(context: Context, celsius: String?, tempUnit: String): String {

        val naString = context.getString(R.string.na)
        val c = celsius?.toFloatOrNull() ?: return ""

        return if (tempUnit == "F") {
            val f = (c * 9 / 5) + 32

            context.getString(R.string.unit_fahrenheit, f.toRoundedInt())
        } else {

            context.getString(R.string.unit_celsius, c.toRoundedInt())
        }
    }

    /**
     * Форматирует скорость ветра из км/ч в км/ч, миль/ч или м/с.
     */
    fun formatWind(context: Context, kph: String?, windUnit: String): String {
        val k = kph?.toFloatOrNull() ?: return context.getString(R.string.na)

        return when (windUnit) {
            "MPH" -> {
                val mph = k * 0.621371f

                "${mph.toRoundedInt()} ${context.getString(R.string.unit_mph)}"
            }
            "MS" -> {
                val ms = k * 0.277778f

                "${ms.toRoundedInt()} ${context.getString(R.string.unit_ms)}"
            }
            else -> { // KPH

                "${k.toRoundedInt()} ${context.getString(R.string.unit_kmh)}"
            }
        }
    }

    /**
     * Форматирует давление из гПа (mb) в гПа, мм рт.ст. или дюймы.
     */
    fun formatPressure(context: Context, hpa: String?, pressureUnit: String): String {
        val h = hpa?.toFloatOrNull() ?: return context.getString(R.string.na)

        return when (pressureUnit) {
            "MMHG" -> {
                val mmhg = h * 0.750062f

                "${mmhg.toRoundedInt()} ${context.getString(R.string.unit_mmhg)}"
            }
            "IN" -> {
                val inhg = h * 0.02953f

                String.format(Locale.US, "%.2f", inhg) + " ${context.getString(R.string.unit_inhg)}"
            }
            else -> { // HPA

                "${h.toRoundedInt()} ${context.getString(R.string.unit_hpa)}"
            }
        }
    }

    private fun Float.toRoundedInt(): Int = this.toBigDecimal().setScale(0, RoundingMode.HALF_UP).toInt()
}