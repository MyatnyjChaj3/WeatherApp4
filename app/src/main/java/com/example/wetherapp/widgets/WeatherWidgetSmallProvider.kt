package com.example.wetherapp.widgets

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
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
private const val ACTION_UPDATE_SMALL = "com.example.wetherapp.widget.UPDATE_SMALL"

class WeatherWidgetSmallProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateWidget(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_SMALL) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, WeatherWidgetSmallProvider::class.java)
            )
            for (id in ids) updateWidget(context, manager, id)
        }
    }

    companion object {
        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_weather_2x2)

            val intent = Intent(context, WeatherWidgetSmallProvider::class.java).apply {
                action = ACTION_UPDATE_SMALL
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

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
                    requestWeather(context, coords) { city, temp, cond, icon ->
                        updateViews(context, manager, widgetId, city, temp, cond, icon)
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
            callback: (String, String, String, String) -> Unit
        ) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val lang = prefs.getString(context.getString(R.string.key_language), "RU")?.lowercase()
            val encoded = URLEncoder.encode(coords, "UTF-8")
            val url =
                "https://api.weatherapi.com/v1/current.json?key=$API_KEY&q=$encoded&lang=$lang"

            val queue = Volley.newRequestQueue(context)
            val request = object : StringRequest(Request.Method.GET, url,
                { response ->
                    try {
                        val obj = JSONObject(response)
                        val loc = obj.getJSONObject("location")
                        val current = obj.getJSONObject("current")
                        val city = loc.getString("name")
                        val tempC = current.getDouble("temp_c").roundToInt().toString()
                        val cond = current.getJSONObject("condition").getString("text")
                        val icon = "https:" + current.getJSONObject("condition").getString("icon")
                        callback(city, "$tempC°C", cond, icon)
                    } catch (e: Exception) {
                        Log.e("WidgetSmall", "Parse error", e)
                    }
                },
                { Log.e("WidgetSmall", "Network error: $it") }) {
                override fun parseNetworkResponse(response: com.android.volley.NetworkResponse?)
                        : com.android.volley.Response<String> {
                    val parsed = String(response?.data ?: byteArrayOf(), Charsets.UTF_8)
                    return com.android.volley.Response.success(
                        parsed,
                        com.android.volley.toolbox.HttpHeaderParser.parseCacheHeaders(response)
                    )
                }
            }
            queue.add(request)
        }

        private fun updateViews(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            city: String,
            temp: String,
            cond: String,
            iconUrl: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_weather_2x2)
            views.setTextViewText(R.id.tvWidgetCity, city)
            views.setTextViewText(R.id.tvWidgetTemp, temp)
            views.setTextViewText(R.id.tvWidgetCondition, cond)

            Thread {
                try {
                    val bmp = Picasso.get().load(iconUrl).get()
                    Handler(Looper.getMainLooper()).post {
                        views.setImageViewBitmap(R.id.ivWidgetIcon, bmp)
                        manager.updateAppWidget(widgetId, views)
                    }
                } catch (e: Exception) {
                    Log.e("WidgetSmall", "Image load failed", e)
                }
            }.start()
        }
    }
}