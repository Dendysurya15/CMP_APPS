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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.InspectionDetailModel
import com.cbi.mobile_plantation.data.model.InspectionWithDetailRelations
import com.cbi.mobile_plantation.databinding.ActivityFollowUpInspeksiBinding
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.viewModel.InspectionViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.vibrate
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class FollowUpInspeksi : AppCompatActivity() {
    private var prefManager: PrefManager? = null
    private lateinit var map: MapView
    private lateinit var binding: ActivityFollowUpInspeksiBinding
    private lateinit var locationOverlay: MyLocationNewOverlay
    private var featureName: String? = null
    private var userName: String? = null
    private var inspectionId: String? = null
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var inspectionViewModel: InspectionViewModel
    companion object {
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx: Context = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = packageName
        binding = ActivityFollowUpInspeksiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val inspectionIdInt = intent.getIntExtra("id_inspeksi", -1)
        inspectionId = if (inspectionIdInt != -1) {
            inspectionIdInt.toString()
        } else {
            null
        }

        map = binding.map
        prefManager = PrefManager(this)
        loadingDialog = LoadingDialog(this)
        setupHeader()
        initViewModel()
        setupMapTouchHandling()
        val backButton = binding.headerLayout.btnBack
        backButton.setOnClickListener {
            onBackPressed()
        }
        setupMap()
        setupButtonListeners()
        updateSatelliteButtonAppearance()
        updateButtonSelection("default") // Start with default instead of satellite
        loadInspectionData()

    }

    private fun loadInspectionData() {
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadingDialog.show()
                loadingDialog.setMessage("Loading inspection data...")
                delay(500)
            }

            try {
                AppLogger.d("inspectionID $inspectionId")
                if (!inspectionId.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {

                        inspectionViewModel.loadInspectionById(inspectionId!!)
                    }
                } else {
                    throw Exception("Inspection ID not found!")
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
//                    showErrorDialog("Error loading inspection data: ${e.message}")
                }
            }
        }
    }

    private fun initViewModel() {
        val factory = InspectionViewModel.InspectionViewModelFactory(application)
        inspectionViewModel = ViewModelProvider(this, factory)[InspectionViewModel::class.java]
        setupInspectionObserver()
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

    private fun setupInspectionObserver() {
        inspectionViewModel.inspectionWithDetails.observe(this) { inspectionData ->
            loadingDialog.dismiss()

            if (inspectionData.isNotEmpty()) {
                val inspection = inspectionData.first()
                updateMapWithInspectionData(inspection)

            } else {
//                showErrorDialog("Inspection data not found")
            }
        }
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


    data class TrackingPath(
        val start: LatLon,
        val end: LatLon
    )

    data class LatLon(
        val lat: Double,
        val lon: Double
    )

    private fun parseTrackingPath(trackingPathJson: String?): TrackingPath? {
        return try {
            if (trackingPathJson.isNullOrEmpty()) return null

            val jsonRegex = """"start":\{"lat":(-?\d+\.?\d*),"lon":(-?\d+\.?\d*)\},"end":\{"lat":(-?\d+\.?\d*),"lon":(-?\d+\.?\d*)\}""".toRegex()
            val matchResult = jsonRegex.find(trackingPathJson)

            matchResult?.let { match ->
                val (startLat, startLon, endLat, endLon) = match.destructured
                TrackingPath(
                    start = LatLon(startLat.toDouble(), startLon.toDouble()),
                    end = LatLon(endLat.toDouble(), endLon.toDouble())
                )
            }
        } catch (e: Exception) {
            AppLogger.e("Error parsing tracking path: ${e.message}")
            null
        }
    }

    private fun addTrackingMarker(latitude: Double, longitude: Double, title: String, type: String) {
        val marker = Marker(map)
        marker.position = GeoPoint(latitude, longitude)
        marker.title = title
        marker.snippet = "Tracking point"
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        // Different icons for start and end
        val drawable = when (type) {
            "start" -> ContextCompat.getDrawable(this, R.drawable.baseline_circle_24)
            "end" -> ContextCompat.getDrawable(this, R.drawable.baseline_flag_24) // Flag for end
            else -> ContextCompat.getDrawable(this, R.drawable.baseline_location_pin_24)
        }

        val colorRes = when (type) {
            "start" -> android.R.color.holo_green_dark
            "end" -> android.R.color.holo_red_dark
            else -> android.R.color.holo_blue_dark
        }

        drawable?.setTint(ContextCompat.getColor(this, colorRes))
        marker.icon = drawable

        // NO CLICK LISTENER - Remove touch events for start and end
        // marker.setOnMarkerClickListener { _, _ -> ... }

        map.overlays.add(marker)
        AppLogger.d("Added tracking marker: $title at $latitude, $longitude")
    }

    private fun updateMapWithInspectionData(inspection: InspectionWithDetailRelations) {
        AppLogger.d("Updating map with inspection data")
        clearMarkers()

        // Parse tracking path JSON
        val trackingPath = parseTrackingPath(inspection.inspeksi.tracking_path)

        // Store all points for drawing lines
        val allPoints = mutableListOf<GeoPoint>()

        if (trackingPath != null) {
            AppLogger.d("Adding start marker at: ${trackingPath.start.lat}, ${trackingPath.start.lon}")
            addTrackingMarker(trackingPath.start.lat, trackingPath.start.lon, "Start Point", "start")
            allPoints.add(GeoPoint(trackingPath.start.lat, trackingPath.start.lon))

            // Group inspection details by no_pokok and add markers
            val groupedDetails = inspection.detailInspeksi.groupBy { it.no_pokok }
            AppLogger.d("Grouped details: ${groupedDetails.size} trees")

            // Sort by no_pokok to maintain order
            groupedDetails.toSortedMap().forEach { (noPokak, details) ->
                val firstDetail = details.first()
                AppLogger.d("Adding tree marker for pokok $noPokak at: ${firstDetail.latIssue}, ${firstDetail.lonIssue}")
                addInspectionDetailMarker(
                    firstDetail.latIssue,
                    firstDetail.lonIssue,
                    "Tree #$noPokak",
                    details,
                    noPokak
                )
                allPoints.add(GeoPoint(firstDetail.latIssue, firstDetail.lonIssue))
            }

            AppLogger.d("Adding end marker at: ${trackingPath.end.lat}, ${trackingPath.end.lon}")
            addTrackingMarker(trackingPath.end.lat, trackingPath.end.lon, "End Point", "end")
            allPoints.add(GeoPoint(trackingPath.end.lat, trackingPath.end.lon))

            // Draw lines connecting all points
            if (allPoints.size > 1) {
                addConnectingLines(allPoints)
            }

            // Move map to start location
            moveToLocation(trackingPath.start.lat, trackingPath.start.lon, 16.0)
        } else {
            AppLogger.e("Could not parse tracking path")
        }

        map.invalidate()
    }

    // Add method to draw connecting lines
    private fun addConnectingLines(points: List<GeoPoint>) {
        if (points.size < 2) return

        // Create polyline connecting all points
        val polyline = org.osmdroid.views.overlay.Polyline()
        polyline.setPoints(points)
        polyline.color = ContextCompat.getColor(this, android.R.color.holo_blue_dark)
        polyline.width = 5.0f

        // Add the polyline to map
        map.overlays.add(polyline)

        AppLogger.d("Added connecting line with ${points.size} points")
    }

    // Alternative: Add individual lines between consecutive points
    private fun addIndividualLines(points: List<GeoPoint>) {
        for (i in 0 until points.size - 1) {
            val startPoint = points[i]
            val endPoint = points[i + 1]

            val polyline = org.osmdroid.views.overlay.Polyline()
            polyline.setPoints(listOf(startPoint, endPoint))

            // Different colors for different segments
            val color = when (i) {
                0 -> android.R.color.holo_green_dark // Start to first tree
                points.size - 2 -> android.R.color.holo_red_dark // Last tree to end
                else -> android.R.color.holo_orange_dark // Between trees
            }

            polyline.color = ContextCompat.getColor(this, color)
            polyline.width = 4.0f

            map.overlays.add(polyline)
        }

        AppLogger.d("Added ${points.size - 1} individual line segments")
    }

    // Update clearMarkers to also remove polylines
    private fun clearMarkers() {
        AppLogger.d("Clearing all markers and lines")
        val overlaysToRemove = map.overlays.filter {
            it !is MyLocationNewOverlay
        }
        map.overlays.removeAll(overlaysToRemove)
        map.invalidate()
    }

    // Option 1: Use OSMDroid's built-in InfoWindow
    private fun addInspectionDetailMarker(
        latitude: Double,
        longitude: Double,
        title: String,
        details: List<InspectionDetailModel>,
        noPokak: Int
    ) {
        val marker = Marker(map)
        marker.position = GeoPoint(latitude, longitude)
        marker.title = title
        marker.snippet = "${details.size} issues found"
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        // Use ONLY location pin icon
        val drawable = ContextCompat.getDrawable(this, R.drawable.baseline_location_pin_24)

        val hasUnresolvedIssues = details.any { it.status_pemulihan == 0.0 }
        val colorRes = if (hasUnresolvedIssues) {
            android.R.color.holo_orange_dark
        } else {
            android.R.color.holo_green_light
        }

        drawable?.setTint(ContextCompat.getColor(this, colorRes))
        marker.icon = drawable

        // Create custom info window content
        val totalIssues = details.size
        val resolvedIssues = details.count { it.status_pemulihan > 0.0 }
        val unresolvedIssues = totalIssues - resolvedIssues

        // Set marker info that will show in built-in popup
        marker.title = "Tree #$noPokak"
        marker.snippet = """Issues: $totalIssues | Resolved: $resolvedIssues | Unresolved: $unresolvedIssues"""

        // Option 1a: Use default InfoWindow (simple)
        marker.setOnMarkerClickListener { clickedMarker, _ ->
            clickedMarker.showInfoWindow()
            true
        }

        map.overlays.add(marker)
        AppLogger.d("Added detail marker: $title at $latitude, $longitude")
    }

    // Option 2: Create custom info window overlay
    private fun showCustomInfoWindow(latitude: Double, longitude: Double, noPokak: Int, details: List<InspectionDetailModel>) {
        // Remove any existing info windows first
        removeInfoWindows()

        val totalIssues = details.size
        val resolvedIssues = details.count { it.status_pemulihan > 0.0 }
        val unresolvedIssues = totalIssues - resolvedIssues

        // Create a text overlay that appears on the map
        val textOverlay = object : org.osmdroid.views.overlay.Overlay() {
            override fun draw(canvas: android.graphics.Canvas?, mapView: org.osmdroid.views.MapView?, shadow: Boolean) {
                if (canvas == null || mapView == null) return

                // Convert geo coordinates to screen coordinates
                val point = mapView.projection.toPixels(GeoPoint(latitude, longitude), null)

                // Create paint for text
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 40f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                val backgroundPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    alpha = 180
                }

                // Info text
                val infoText = "Tree #$noPokak\nIssues: $totalIssues\nResolved: $resolvedIssues\nUnresolved: $unresolvedIssues"
                val lines = infoText.split("\n")

                // Calculate text bounds
                val textHeight = paint.textSize
                val totalHeight = lines.size * textHeight + 20
                val maxWidth = lines.maxOfOrNull { paint.measureText(it) } ?: 0f

                // Draw background
                val rect = android.graphics.RectF(
                    point.x - maxWidth/2 - 10,
                    point.y - totalHeight - 60,
                    point.x + maxWidth/2 + 10,
                    (point.y - 60).toFloat()
                )
                canvas.drawRoundRect(rect, 10f, 10f, backgroundPaint)

                // Draw text lines
                lines.forEachIndexed { index, line ->
                    canvas.drawText(
                        line,
                        point.x.toFloat(),
                        point.y - totalHeight + (index + 1) * textHeight - 40,
                        paint
                    )
                }
            }
        }

        map.overlays.add(textOverlay)
        map.invalidate()

        // Auto-remove after 3 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            removeInfoWindows()
        }, 3000)
    }

    // Option 3: Enhanced marker with better click handling
    private fun addInspectionDetailMarkerWithPopup(
        latitude: Double,
        longitude: Double,
        title: String,
        details: List<InspectionDetailModel>,
        noPokak: Int
    ) {
        val marker = Marker(map)
        marker.position = GeoPoint(latitude, longitude)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        // Use ONLY location pin icon
        val drawable = ContextCompat.getDrawable(this, R.drawable.baseline_location_pin_24)

        val hasUnresolvedIssues = details.any { it.status_pemulihan == 0.0 }
        val colorRes = if (hasUnresolvedIssues) {
            android.R.color.holo_orange_dark
        } else {
            android.R.color.holo_green_light
        }

        drawable?.setTint(ContextCompat.getColor(this, colorRes))
        marker.icon = drawable

        // Enhanced click handling with map popup
        marker.setOnMarkerClickListener { _, _ ->
            showCustomInfoWindow(latitude, longitude, noPokak, details)
            true
        }

        map.overlays.add(marker)
        AppLogger.d("Added detail marker: $title at $latitude, $longitude")
    }

    // Helper method to remove info windows
    private fun removeInfoWindows() {
        // Remove any text overlays
        val overlaysToRemove = map.overlays.filter {
            it is org.osmdroid.views.overlay.Overlay &&
                    it !is Marker &&
                    it !is org.osmdroid.views.overlay.Polyline &&
                    it !is MyLocationNewOverlay
        }
        map.overlays.removeAll(overlaysToRemove)
        map.invalidate()
    }

    // Option 4: Simple floating text that follows the marker
    private fun showFloatingInfo(marker: Marker, noPokak: Int, details: List<InspectionDetailModel>) {
        val totalIssues = details.size
        val resolvedIssues = details.count { it.status_pemulihan > 0.0 }
        val unresolvedIssues = totalIssues - resolvedIssues

        // Update marker title and snippet for built-in popup
        marker.title = "🌴 Tree #$noPokak"
        marker.snippet = """
        📊 Total Issues: $totalIssues
        ✅ Resolved: $resolvedIssues  
        ❌ Unresolved: $unresolvedIssues
        📍 Tap outside to close
    """.trimIndent()

        // Show the built-in info window
        marker.showInfoWindow()

        // Center map on marker
        map.controller.animateTo(marker.position)
    }

    fun moveToLocation(latitude: Double, longitude: Double, zoom: Double = 15.0) {
        val geoPoint = GeoPoint(latitude, longitude)
        map.controller.animateTo(geoPoint)
        map.controller.setZoom(zoom)
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
            if (AppUtils.isNetworkAvailable(this)) {
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
        if (AppUtils.isNetworkAvailable(this)) {
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
        if (!AppUtils.isNetworkAvailable(this)) {
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
                if (!AppUtils.isNetworkAvailable(this)) {
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

    private fun updateButtonSelection(selectedType: String) {
        resetButtonStyles()
        when (selectedType) {
            "default" -> highlightButton(binding.btnDefault)
            "satellite" -> highlightButton(binding.btnSatellite)

        }
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

    private fun switchToDefault() {
        // Switch back to default OpenStreetMap
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.invalidate()
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
                val intent = Intent(this, ListFollowUpInspeksi::class.java)
                startActivity(intent)
                finishAffinity()
            },
            cancelFunction = {

            }
        )


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