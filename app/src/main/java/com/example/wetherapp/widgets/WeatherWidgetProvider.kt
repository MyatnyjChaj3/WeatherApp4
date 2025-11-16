// Файл: widgets/WeatherWidgetProvider.kt
package com.example.wetherapp.widgets

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.wetherapp.R
import com.example.wetherapp.location.LocationManager
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.roundToInt

private const val API_KEY = "10f3a69ad29b42919c5140527250211"
private const val ACTION_UPDATE = "com.example.wetherapp.widget.UPDATE"

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, manager, appWidgetIds)
        for (id in appWidgetIds) updateWidget(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, WeatherWidgetProvider::class.java)
            )
            for (id in ids) updateWidget(context, manager, id)
        }
    }

    companion object {

        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val options = manager.getAppWidgetOptions(widgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val layoutId =
                if (minWidth >= 250) R.layout.widget_weather_4x2 else R.layout.widget_weather_2x2

            val views = RemoteViews(context.packageName, layoutId)

            // Клик по виджету = обновление
            val intent = Intent(context, WeatherWidgetProvider::class.java).apply {
                action = ACTION_UPDATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            views.setTextViewText(R.id.tvWidgetTemp, context.getString(R.string.na))
            views.setTextViewText(R.id.tvWidgetCondition, context.getString(R.string.widget_title))
            manager.updateAppWidget(widgetId, views)

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                views.setTextViewText(R.id.tvWidgetCondition, "No location permission")
                manager.updateAppWidget(widgetId, views)
                return
            }

            val locationManager = LocationManager(
                context = context,
                onLocationReceived = { coords ->
                    requestWeather(context, coords) { temp, condition, iconUrl, humidity, wind ->
                        updateViews(
                            context,
                            manager,
                            widgetId,
                            temp,
                            condition,
                            iconUrl,
                            humidity,
                            wind
                        )
                    }
                },
                onShowProgress = { },
                onShowGpsIndicator = { }
            )
            locationManager.requestLocation()
        }

        private fun requestWeather(
            context: Context,
            coords: String,
            callback: (String, String, String, String, String) -> Unit
        ) {
            try {
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val lang =
                    prefs.getString(context.getString(R.string.key_language), "RU")?.lowercase()
                val encoded = URLEncoder.encode(coords, "UTF-8")

                val url =
                    "https://api.weatherapi.com/v1/current.json?key=$API_KEY&q=$encoded&lang=$lang"

                val queue = Volley.newRequestQueue(context)
                val request = object : StringRequest(
                    Request.Method.GET,
                    url,
                    { response ->
                        try {
                            val obj = JSONObject(response)
                            val current = obj.getJSONObject("current")
                            val tempC = current.getDouble("temp_c").roundToInt().toString()
                            val cond = current.getJSONObject("condition").getString("text")
                            val icon =
                                "https:" + current.getJSONObject("condition").getString("icon")
                            val humidity = current.optString("humidity", "-")
                            val wind = current.optString("wind_kph", "-")
                            callback("$tempC°C", cond, icon, "$humidity%", "$wind km/h")
                        } catch (e: Exception) {
                            Log.e("Widget", "Parse error: ${e.message}")
                        }
                    },
                    { error ->
                        Log.e("Widget", "Network error: $error")
                    }
                ) {
                    override fun parseNetworkResponse(response: com.android.volley.NetworkResponse?): com.android.volley.Response<String> {
                        val parsed = String(response?.data ?: byteArrayOf(), Charsets.UTF_8)
                        return com.android.volley.Response.success(
                            parsed,
                            com.android.volley.toolbox.HttpHeaderParser.parseCacheHeaders(response)
                        )
                    }
                }
                queue.add(request)
            } catch (e: Exception) {
                Log.e("Widget", "Request error: ${e.message}")
            }
        }

        private fun updateViews(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            temp: String,
            condition: String,
            iconUrl: String,
            humidity: String,
            wind: String
        ) {
            val options = manager.getAppWidgetOptions(widgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val layoutId =
                if (minWidth >= 250) R.layout.widget_weather_4x2 else R.layout.widget_weather_2x2
            val views = RemoteViews(context.packageName, layoutId)

            views.setTextViewText(R.id.tvWidgetTemp, temp)
            views.setTextViewText(R.id.tvWidgetCondition, condition)

            if (layoutId == R.layout.widget_weather_4x2) {
                views.setTextViewText(R.id.tvWidgetHumidity, "💧 $humidity")
                views.setTextViewText(R.id.tvWidgetWind, "🌬 $wind")
            }

            Thread {
                try {
                    val bmp = Picasso.get().load(iconUrl).get()
                    Handler(Looper.getMainLooper()).post {
                        views.setImageViewBitmap(R.id.ivWidgetIcon, bmp)
                        manager.updateAppWidget(widgetId, views)
                    }
                } catch (e: Exception) {
                    Log.e("Widget", "Image load failed: ${e.message}")
                }
            }.start()
        }
    }
}