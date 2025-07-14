package com.cbi.mobile_plantation.ui.view.followUpInspeksi

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.databinding.ActivityFollowUpInspeksiBinding
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.vibrate
import com.cbi.mobile_plantation.utils.PrefManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class FollowUpInspeksi : AppCompatActivity() {
    private var prefManager: PrefManager? = null
    private lateinit var map: MapView
    private lateinit var binding: ActivityFollowUpInspeksiBinding
    private lateinit var locationOverlay: MyLocationNewOverlay
    private var featureName: String? = null
    private var userName: String? = null

    companion object {
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize osmdroid configuration
        val ctx: Context = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = packageName

        binding = ActivityFollowUpInspeksiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        map = binding.map
        prefManager = PrefManager(this)
        setupHeader()
        setupMapTouchHandling()

        requestPermissionsIfNecessary(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.INTERNET,
                android.Manifest.permission.ACCESS_NETWORK_STATE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )

        val backButton = binding.headerLayout.btnBack
        backButton.setOnClickListener {
            onBackPressed()
        }
        setupMap()
        setupButtonListeners()
        updateSatelliteButtonAppearance()
        updateButtonSelection("default") // Start with default instead of satellite

        map.post {
            createMarkers()
        }
    }

    private fun setupHeader() {
        featureName = intent.getStringExtra("FEATURE_NAME").toString()
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        val userSection = findViewById<TextView>(R.id.userSection)
        val titleAppNameAndVersion = findViewById<TextView>(R.id.titleAppNameAndVersionFeature)
        val lastUpdateText = findViewById<TextView>(R.id.lastUpdate)
        val locationSection = findViewById<LinearLayout>(R.id.locationSection)

        locationSection.visibility = View.VISIBLE

        AppUtils.setupUserHeader(
            userName = userName,
            userSection = userSection,
            featureName = featureName,
            tvFeatureName = tvFeatureName,
            prefManager = prefManager,
            lastUpdateText = lastUpdateText,
            titleAppNameAndVersionText = titleAppNameAndVersion,
            context = this
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMapTouchHandling() {
        // Disable parent scrolling when touching the map
        map.setOnTouchListener { _, _ ->
            // Request that the parent (ScrollView) doesn't intercept touch events
            binding.scPanen.requestDisallowInterceptTouchEvent(true)
            false // Let the map handle the touch
        }

        // Alternative approach - you can also try this
        binding.scPanen.setOnTouchListener { _, _ ->
            // Allow scrollview to handle touches outside the map
            binding.scPanen.requestDisallowInterceptTouchEvent(false)
            false
        }
    }

    private fun setupMap() {
        // IMPORTANT: Enable multi-touch controls FIRST before setting tile source
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(true)

        // Set tile source to OpenStreetMap default (Mapnik)
        map.setTileSource(TileSourceFactory.MAPNIK) // Default OSM

        // Set initial zoom level and center point
        val mapController = map.controller
        mapController.setZoom(15.0)

        // Set default location (Surakarta, Central Java, Indonesia)
        val startPoint = GeoPoint(-7.5755, 110.8243)
        mapController.setCenter(startPoint)

        // CRITICAL: Additional touch and zoom settings
        map.isTilesScaledToDpi = true
        map.setUseDataConnection(true)

        // Set minimum and maximum zoom levels
        map.minZoomLevel = 3.0  // Increased range
        map.maxZoomLevel = 21.0

        // Enable gestures explicitly
        map.setMultiTouchControls(true) // Set again to ensure it's enabled

        // Setup location overlay
        setupLocationOverlay()

        // Add tile layer options
        addMapTypeOptions()

        // Check internet connection periodically for button updates
        checkInternetConnectionPeriodically()
    }

    private fun checkInternetConnectionPeriodically() {
        // Update button appearance every 5 seconds
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                updateSatelliteButtonAppearance()
                handler.postDelayed(this, 5000) // Check every 5 seconds
            }
        }
        handler.post(runnable)
    }

    private fun setupButtonListeners() {
        // Map type switcher buttons
        binding.btnDefault.setOnClickListener {
            switchToDefault()
            updateButtonSelection("default")
        }

        binding.btnSatellite.setOnClickListener {
            // Check internet connection before switching to satellite
            if (isNetworkAvailable(this)) {
                switchToGoogleSatellite()
                updateButtonSelection("satellite")
            } else {
                // Show custom alert for no internet connection
                showNoInternetAlert()
            }
        }

    }

    private fun showNoInternetAlert() {
        AlertDialogUtility.withSingleAction(
            this@FollowUpInspeksi,
            "Kembali",
            "Tidak Ada Koneksi Internet",
            "Fitur satelit memerlukan koneksi internet untuk memuat peta. Pastikan perangkat terhubung ke internet dan coba lagi.",
            "warning.json",
            R.color.colorRedDark
        ) {
            // When user dismisses the alert, go back to default button
            switchToDefault()
            updateButtonSelection("default")
        }
    }


    private fun updateSatelliteButtonAppearance() {
        if (isNetworkAvailable(this)) {
            // Internet available - show NO ICON, just text
            binding.btnSatellite.text = "Satelit"
            binding.btnSatellite.icon = null // Remove icon completely
            binding.btnSatellite.iconTint = null
        } else {
            // No internet - show warning icon with red color
            binding.btnSatellite.text = "Satelit"
            binding.btnSatellite.icon =
                ContextCompat.getDrawable(this, android.R.drawable.ic_dialog_alert)
            binding.btnSatellite.iconTint =
                ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
        }
    }

    private fun resetButtonStyles() {
        binding.btnDefault.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.grayBorder)
        binding.btnSatellite.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.grayBorder)

        // Reset text colors
        binding.btnDefault.setTextColor(ContextCompat.getColor(this, R.color.black))
        binding.btnSatellite.setTextColor(ContextCompat.getColor(this, R.color.black))

        // Reset default button icon tint
        binding.btnDefault.iconTint = ContextCompat.getColorStateList(this, R.color.black)

        // For satellite button, preserve the warning icon if no internet, otherwise no icon
        if (!isNetworkAvailable(this)) {
            // Keep the red warning icon when no internet
            binding.btnSatellite.iconTint =
                ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
        } else {
            // No icon when internet is available
            binding.btnSatellite.iconTint = null
        }
    }

    private fun highlightButton(button: com.google.android.material.button.MaterialButton) {
        button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.greenDarker)
        button.setTextColor(ContextCompat.getColor(this, R.color.white))

        // Handle icon tint based on which button and internet status
        when (button.id) {
            R.id.btnDefault -> {
                // Default button always has white icon when selected
                button.iconTint = ContextCompat.getColorStateList(this, R.color.white)
            }

            R.id.btnSatellite -> {
                if (!isNetworkAvailable(this)) {
                    // Keep red warning icon even when selected if no internet
                    button.iconTint =
                        ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
                } else {
                    // No icon when internet is available (icon is null)
                    button.iconTint = null
                }
            }
        }
    }

    // Add this utility function to your activity
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // if the android version is equal to M
        // or greater we need to use the
        // NetworkCapabilities to check what type of
        // network has the internet connection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Returns a Network object corresponding to
            // the currently active default data network.
            val network = connectivityManager.activeNetwork ?: return false

            // Representation of the capabilities of an active network.
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                // Indicates this network uses a Wi-Fi transport,
                // or WiFi has network connectivity
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

                // Indicates this network uses a Cellular transport. or
                // Cellular has network connectivity
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

                // else return false
                else -> false
            }
        } else {
            // if the android version is below M
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    private fun updateButtonSelection(selectedType: String) {
        // Reset all buttons to default state
        resetButtonStyles()

        // Highlight selected button
        when (selectedType) {
            "default" -> highlightButton(binding.btnDefault)
            "satellite" -> highlightButton(binding.btnSatellite)

        }
    }

    private fun addMapTypeOptions() {
        createMapTypeSwitcher()
    }

    private fun createMapTypeSwitcher() {
        // You can add buttons to your layout and call these methods:
        // 1. switchToDefault() - Standard OpenStreetMap
        // 2. switchToSatellite() - Esri World Imagery
        // 3. switchToGoogleSatellite() - Google Satellite
        // 4. switchToHybrid() - Google Hybrid (Satellite + Labels)
        // 5. switchToTerrain() - Topographic map
//         switchToSatellite()
//        switchToGoogleSatellite()
    }

    private fun switchToSatellite() {
        // Switch to satellite view using Esri World Imagery
        val satelliteSource = object : OnlineTileSourceBase(
            "EsriWorldImagery",
            0, 18, 256, ".png",
            arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "${baseUrl}$zoom/$y/$x"
            }
        }
        map.setTileSource(satelliteSource)
        map.invalidate()
    }

    private fun switchToGoogleSatellite() {
        // Alternative: Google Satellite (may require API key for production)
        val googleSatellite = object : OnlineTileSourceBase(
            "GoogleSatellite",
            0, 18, 256, ".png",
            arrayOf("https://mt1.google.com/vt/lyrs=s&x={x}&y={y}&z={z}")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return baseUrl.replace("{x}", x.toString())
                    .replace("{y}", y.toString())
                    .replace("{z}", zoom.toString())
            }
        }
        map.setTileSource(googleSatellite)
        map.invalidate()
    }

    private fun switchToHybrid() {
        // Google Hybrid (Satellite + Labels)
        val hybridSource = object : OnlineTileSourceBase(
            "GoogleHybrid",
            0, 18, 256, ".png",
            arrayOf("https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return baseUrl.replace("{x}", x.toString())
                    .replace("{y}", y.toString())
                    .replace("{z}", zoom.toString())
            }
        }
        map.setTileSource(hybridSource)
        map.invalidate()
    }

    private fun switchToDefault() {
        // Switch back to default OpenStreetMap
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.invalidate()
    }

    private fun switchToTerrain() {
        // Switch to terrain/topographic view
        map.setTileSource(TileSourceFactory.OpenTopo)
        map.invalidate()
    }

    private fun setupLocationOverlay() {
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation()

        // Customize location overlay appearance
        locationOverlay.setDrawAccuracyEnabled(true)

        // Add the overlay to the map
        map.overlays.add(locationOverlay)
    }

    private fun createMarkers() {
        // Sample markers for inspection locations
        val inspectionLocations = listOf(
            Triple(-7.5755, 110.8243, "Surakarta City Center"),
            Triple(-7.5650, 110.8100, "Manahan Stadium"),
            Triple(-7.5560, 110.8320, "Solo Square"),
            Triple(-7.5800, 110.8400, "Keraton Surakarta"),
            Triple(-7.5700, 110.8200, "Pasar Klewer")
        )

        inspectionLocations.forEach { (lat, lon, title) ->
            addInspectionMarker(lat, lon, title)
        }

        // Add a custom marker with different icon
        addCustomMarker(-7.5755, 110.8243, "Main Office", createCustomIcon())
    }

    private fun addInspectionMarker(latitude: Double, longitude: Double, title: String) {
        val marker = Marker(map)
        marker.position = GeoPoint(latitude, longitude)
        marker.title = title
        marker.snippet = "Inspection Location"
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        marker.setOnMarkerClickListener { marker, mapView ->
            true
        }

        map.overlays.add(marker)
    }

    private fun addCustomMarker(
        latitude: Double,
        longitude: Double,
        title: String,
        icon: Drawable
    ) {
        val marker = Marker(map)
        marker.position = GeoPoint(latitude, longitude)
        marker.title = title
        marker.snippet = "Custom Location"
        marker.icon = icon
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        map.overlays.add(marker)
    }

    private fun createCustomIcon(): Drawable {
        // Create a simple colored circle as custom icon
        val drawable = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation)
        return drawable ?: resources.getDrawable(android.R.drawable.ic_dialog_map, null)
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {


        vibrate()
        AlertDialogUtility.withTwoActions(
            this,
            "Keluar",
            getString(R.string.confirmation_dialog_title),
            getString(R.string.al_confirm_feature),
            "warning.json",
            ContextCompat.getColor(this, R.color.bluedarklight),
            function = {
                val intent = Intent(this, HomePageActivity::class.java)
                startActivity(intent)
                finishAffinity()
            },
            cancelFunction = {

            }
        )


    }


    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        val permissionsToRequest = ArrayList<String>()

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            // Refresh map after permissions are granted
            map.invalidate()
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationOverlay.isInitialized) {
            locationOverlay.disableMyLocation()
        }
    }
}