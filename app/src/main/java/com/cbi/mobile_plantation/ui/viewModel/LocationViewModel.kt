package com.cbi.mobile_plantation.ui.viewModel

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.utils.AppUtils
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar

class LocationViewModel(
    application: Application,
    private val imageView: ImageView,
    private val activity: Activity
) : AndroidViewModel(application) {

    private val _locationPermissions = MutableLiveData<Boolean>()
    val locationPermissions: LiveData<Boolean>
        get() = _locationPermissions

    private val _locationData = MutableLiveData<Location>()
    val locationData: LiveData<Location>
        get() = _locationData

    private val _locationAccuracy = MutableLiveData<Float>()
    val locationAccuracy: LiveData<Float>
        get() = _locationAccuracy

    private val _locationIconState = MutableLiveData<Boolean>()
    val locationIconState: LiveData<Boolean>
        get() = _locationIconState

    private val _airplaneModeState = MutableLiveData<Boolean>()
    val airplaneModeState: LiveData<Boolean>
        get() = _airplaneModeState
    private var currentSnackbar: Snackbar? = null
    private var isStartLocations = false
    private var locationCallback: LocationCallback? = null
    private var locationReceiver: BroadcastReceiver? = null

    private val mFusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)
    private val mSettingsClient: SettingsClient =
        LocationServices.getSettingsClient(application)
    private var mLocationSettingsRequest: LocationSettingsRequest? = null
    private var mLocationRequest: LocationRequest? = null

    init {
        _locationPermissions.value = checkLocationPermission()
        _airplaneModeState.value = isAirplaneModeOn()
        registerLocationSettingsReceiver()
    }

    private fun isAirplaneModeOn(): Boolean {
        return Settings.Global.getInt(
            getApplication<Application>().contentResolver,
            Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0
    }

    private fun unregisterLocationSettingsReceiver() {
        locationReceiver?.let {
            try {
                activity.unregisterReceiver(it)
                locationReceiver = null
            } catch (e: Exception) {
                Log.e(AppUtils.LOG_LOC, "Error unregistering receiver: ${e.message}")
            }
        }
    }

    private fun registerLocationSettingsReceiver() {
        val filter = IntentFilter().apply {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        }

        locationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                        val isAirplaneMode = isAirplaneModeOn()
                        _airplaneModeState.value = isAirplaneMode

                        if (isAirplaneMode) {
                            stopLocationUpdates()
                            showAirplaneModeSnackbar()
                            imageView.setImageResource(R.drawable.baseline_wrong_location_24)
                            imageView.imageTintList = ColorStateList.valueOf(
                                activity.resources.getColor(R.color.colorRedDark)
                            )
                        } else {
                            dismissCurrentSnackbar()
                            startLocationUpdates()
                        }
                    }
                    LocationManager.PROVIDERS_CHANGED_ACTION -> {
                        handleLocationProvidersChange()
                    }
                }
            }
        }
        activity.registerReceiver(locationReceiver, filter)
    }

    private fun dismissCurrentSnackbar() {
        currentSnackbar?.dismiss()
        currentSnackbar = null
    }

    private fun showAirplaneModeSnackbar() {
        // Dismiss any existing Snackbar first
        dismissCurrentSnackbar()

        currentSnackbar = Snackbar.make(
            activity.findViewById(android.R.id.content),
            "Please disable Airplane Mode to get location updates",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("Settings") {
            activity.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
        }

        currentSnackbar?.show()
    }

    override fun onCleared() {
        super.onCleared()
        dismissCurrentSnackbar()
        unregisterLocationSettingsReceiver()
        stopLocationUpdates()
    }

    private fun handleLocationProvidersChange() {
        val locationManager = getApplication<Application>()
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isLocationEnabled) {
            imageView.setImageResource(R.drawable.baseline_wrong_location_24)
            imageView.imageTintList = ColorStateList.valueOf(
                activity.resources.getColor(R.color.colorRedDark)
            )
            promptLocationSettings()
        } else if (!isAirplaneModeOn()) {
            startLocationUpdates()
        }
    }

    private fun promptLocationSettings() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val client: SettingsClient = LocationServices.getSettingsClient(activity)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(
                        activity,
                        AppUtils.REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(AppUtils.LOG_LOC, "Error showing location settings dialog", sendEx)
                }
            }
        }
    }

//    private fun showAirplaneModeSnackbar() {
//        Snackbar.make(
//            activity.findViewById(android.R.id.content),
//            "Please disable Airplane Mode to get location updates",
//            Snackbar.LENGTH_INDEFINITE
//        ).setAction("Settings") {
//            activity.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
//        }.show()
//    }

    private fun checkLocationPermission(): Boolean {
        val locationManager =
            getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        return ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && isLocationEnabled
    }

    fun startLocationUpdates() {
        if (isAirplaneModeOn()) {
            showAirplaneModeSnackbar()
            return
        }

        mLocationRequest = LocationRequest.create().apply {
            interval = AppUtils.UPDATE_INTERVAL_IN_MILLISECONDS
            fastestInterval = AppUtils.FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequest!!)
        mLocationSettingsRequest = builder.build()

        checkLocationSettings()
    }

    private fun checkLocationSettings() {
        if (isAirplaneModeOn()) {
            showAirplaneModeSnackbar()
            return
        }

        mSettingsClient.checkLocationSettings(mLocationSettingsRequest!!)
            .addOnSuccessListener {
                if (checkLocationPermission()) {
                    try {
                        Log.i(AppUtils.LOG_LOC, "All location settings are satisfied.")
                        locationCallback = object : LocationCallback() {
                            override fun onLocationResult(locationResult: LocationResult) {
                                locationResult.lastLocation?.let { location ->
                                    if (location.latitude.toString().isNotEmpty()) {
                                        _locationData.value = location
                                        _locationAccuracy.value = location.accuracy

                                        val locationManager = getApplication<Application>()
                                            .getSystemService(Context.LOCATION_SERVICE) as LocationManager

                                        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                                                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                                        if (isLocationEnabled && !isAirplaneModeOn()) {
                                            imageView.setImageResource(R.drawable.baseline_location_on_24)
                                            imageView.imageTintList = ColorStateList.valueOf(
                                                activity.resources.getColor(R.color.greenbutton)
                                            )
                                        } else {
                                            imageView.setImageResource(R.drawable.baseline_wrong_location_24)
                                            imageView.imageTintList = ColorStateList.valueOf(
                                                activity.resources.getColor(R.color.colorRedDark)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        mFusedLocationClient.requestLocationUpdates(
                            mLocationRequest!!,
                            locationCallback!!,
                            Looper.getMainLooper()
                        )
                        isStartLocations = true
                    } catch (e: SecurityException) {
                        Log.e(AppUtils.LOG_LOC, "Error requesting location updates", e)
                    }
                }
            }
            .addOnFailureListener { e ->
                imageView.setImageResource(R.drawable.baseline_wrong_location_24)
                imageView.imageTintList = ColorStateList.valueOf(
                    activity.resources.getColor(R.color.colorRedDark)
                )

                when ((e as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        Log.i(
                            AppUtils.LOG_LOC,
                            "Location settings are not satisfied. Attempting to upgrade location settings"
                        )
                        try {
                            val rae = e as ResolvableApiException
                            rae.startResolutionForResult(
                                activity,
                                AppUtils.REQUEST_CHECK_SETTINGS
                            )
                        } catch (sie: IntentSender.SendIntentException) {
                            Log.i(AppUtils.LOG_LOC, "PendingIntent unable to execute request.")
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val errorMessage = "Location settings are inadequate and cannot be fixed here. Fix in Settings."
                        Log.e(AppUtils.LOG_LOC, errorMessage)
                    }
                }
            }
    }

    private fun updateLocationIcon(isEnabled: Boolean) {
        _locationIconState.value = isEnabled
        imageView.setImageResource(
            if (isEnabled) R.drawable.baseline_location_on_24
            else R.drawable.baseline_wrong_location_24
        )
        imageView.imageTintList = ColorStateList.valueOf(
            activity.resources.getColor(
                if (isEnabled) R.color.greenbutton else R.color.colorRedDark
            )
        )
    }

    fun refreshLocationStatus() {
        val isEnabled = checkLocationPermission() && isStartLocations && !isAirplaneModeOn()
        updateLocationIcon(isEnabled)
    }

    fun stopLocationUpdates() {
        if (isStartLocations) {
            try {
                locationCallback?.let {
                    mFusedLocationClient.removeLocationUpdates(it)
                    locationCallback = null
                }
                Log.i(AppUtils.LOG_LOC, "Location updates stopped.")
            } finally {
                isStartLocations = false
            }
        }
    }

//    override fun onCleared() {
//        super.onCleared()
//        unregisterLocationSettingsReceiver()
//        stopLocationUpdates()
//    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val application: Application,
        private val imageView: ImageView,
        private val activity: Activity
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LocationViewModel::class.java)) {
                return LocationViewModel(application, imageView, activity) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}