// LocationManager.kt
package com.example.wetherapp.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.example.wetherapp.R
import com.example.wetherapp.managers.DialogManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

private const val PREFS_LAST_LOCATION = "last_location"
private const val PREF_LAT = "lat"
private const val PREF_LON = "lon"
private const val MIN_DISTANCE_METERS = 5000f // 5 км

class LocationManager(
    private val context: Context,
    private val onLocationReceived: (String) -> Unit, // "lat,lon"
    private val onShowProgress: (Boolean) -> Unit,
    private val onShowGpsIndicator: (Boolean) -> Unit
) {
    private val fLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_LAST_LOCATION, Context.MODE_PRIVATE)

    private val settingsPrefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private var locationCallback: LocationCallback? = null
    private var lastLocation: Location? = null

    private var gpsPriority: Int = Priority.PRIORITY_HIGH_ACCURACY
    private var updateInterval: Long = 300000L // 5 мин
    private var minUpdateInterval: Long = 60000L // 1 мин

    init {
        loadGpsSettings()
        loadLastLocationAndUpdate()
    }

    private fun loadGpsSettings() {

        val accuracyKey = context.getString(R.string.key_gps_accuracy)
        val accuracyValue = settingsPrefs.getString(accuracyKey, "High")
        gpsPriority = when (accuracyValue) {
            "Balanced" -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
            "Low" -> Priority.PRIORITY_LOW_POWER
            else -> Priority.PRIORITY_HIGH_ACCURACY
        }

        val freqKey = context.getString(R.string.key_update_frequency)
        val defaultInterval = "300000" // 5 минут
        updateInterval = settingsPrefs.getString(freqKey, defaultInterval)?.toLongOrNull() ?: defaultInterval.toLong()
        minUpdateInterval = (updateInterval / 5).coerceAtLeast(60000L) // Мин. интервал не чаще 1/5 от основного, но не меньше 1 мин
    }

    fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun requestLocation() {
        if (!isLocationEnabled()) {
            showLocationSettingsDialog()
            return
        }
        loadGpsSettings()
        onShowProgress(true)
        getCurrentLocation()
        startLocationUpdates()
    }

    private fun showLocationSettingsDialog() {
        DialogManager.locatiionSettingsDialog(context, object : DialogManager.Listener {
            override fun onClick(name: String?) {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        })
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun getCurrentLocation() {
        if (!hasLocationPermission()) {
            onShowProgress(false)
            return
        }

        val ct = CancellationTokenSource()
        fLocationClient.getCurrentLocation(gpsPriority, ct.token)
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val coords = "${it.latitude},${it.longitude}"
                    onLocationReceived(coords)
                    saveLastLocation(it)
                    onShowGpsIndicator(true)
                    lastLocation = it
                }
                onShowProgress(false)
            }
            .addOnFailureListener {
                Log.e("LocationManager", "Failed to get location", it)
                onShowProgress(false)
            }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { newLocation ->
                    lastLocation?.let { old ->
                        if (old.distanceTo(newLocation) > MIN_DISTANCE_METERS) {
                            val coords = "${newLocation.latitude},${newLocation.longitude}"
                            onLocationReceived(coords)
                            saveLastLocation(newLocation)
                            lastLocation = newLocation
                        }
                    } ?: run {
                        lastLocation = newLocation
                        saveLastLocation(newLocation)
                    }
                }
            }
        }

        val request = LocationRequest.Builder(gpsPriority, updateInterval)
            .setMinUpdateIntervalMillis(minUpdateInterval)
            .build()

        fLocationClient.requestLocationUpdates(request, locationCallback!!, null)
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    private fun saveLastLocation(location: Location) {
        sharedPrefs.edit().apply {
            putFloat(PREF_LAT, location.latitude.toFloat())
            putFloat(PREF_LON, location.longitude.toFloat())
            apply()
        }
    }

    private fun loadLastLocationAndUpdate() {
        val lat = sharedPrefs.getFloat(PREF_LAT, 0f).toDouble()
        val lon = sharedPrefs.getFloat(PREF_LON, 0f).toDouble()
        if (lat != 0.0 && lon != 0.0) {
            onLocationReceived("$lat,$lon")
            onShowGpsIndicator(true)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onDestroy() {
        stopLocationUpdates()
    }
}