package com.cbi.mobile_plantation.utils

import android.content.Context
import org.json.JSONObject
import java.io.InputStream
import kotlin.math.abs

/**
 * Helper class to check if a location is within GeoJSON polygon boundaries
 */
object GeoFenceHelper {

    /**
     * Check if a point is inside any polygon defined in the GeoJSON file
     * @param context Application context
     * @param latitude User's latitude
     * @param longitude User's longitude
     * @param geoJsonFileName Name of the GeoJSON file in assets folder
     * @return true if point is inside any polygon, false otherwise
     */
    fun isLocationInsideBoundary(
        context: Context,
        latitude: Double,
        longitude: Double,
        geoJsonFileName: String = "map.geojson"
    ): Boolean {
        try {
            // Load GeoJSON from assets
            val geoJsonString = loadGeoJsonFromAssets(context, geoJsonFileName)
            val geoJson = JSONObject(geoJsonString)
            
            // Get features array
            val features = geoJson.getJSONArray("features")
            
            // Check each polygon
            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val geometry = feature.getJSONObject("geometry")
                val type = geometry.getString("type")
                
                if (type == "Polygon") {
                    val coordinates = geometry.getJSONArray("coordinates")
                    
                    // For Polygon, coordinates[0] contains the outer ring
                    val polygon = coordinates.getJSONArray(0)
                    
                    if (isPointInPolygon(latitude, longitude, polygon)) {
                        AppLogger.d("Location is inside polygon #$i")
                        return true
                    }
                }
            }
            
            AppLogger.d("Location is outside all polygons")
            return false
            
        } catch (e: Exception) {
            AppLogger.e("Error checking geofence: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Load GeoJSON file from assets folder
     */
    private fun loadGeoJsonFromAssets(context: Context, fileName: String): String {
        return try {
            val inputStream: InputStream = context.assets.open(fileName)
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            AppLogger.e("Error loading GeoJSON: ${e.message}")
            throw e
        }
    }

    /**
     * Ray casting algorithm to check if a point is inside a polygon
     * @param lat Latitude of the point
     * @param lon Longitude of the point
     * @param polygon JSONArray containing polygon coordinates [[lon, lat], [lon, lat], ...]
     */
    private fun isPointInPolygon(lat: Double, lon: Double, polygon: org.json.JSONArray): Boolean {
        var inside = false
        val n = polygon.length()
        
        var p1lat: Double
        var p1lon: Double
        var p2lat: Double
        var p2lon: Double
        
        // Get first point
        val firstPoint = polygon.getJSONArray(0)
        p1lon = firstPoint.getDouble(0)
        p1lat = firstPoint.getDouble(1)
        
        for (i in 1..n) {
            // Get next point (wrap around to first point at end)
            val point = polygon.getJSONArray(i % n)
            p2lon = point.getDouble(0)
            p2lat = point.getDouble(1)
            
            // Ray casting algorithm
            if (lat > minOf(p1lat, p2lat)) {
                if (lat <= maxOf(p1lat, p2lat)) {
                    if (lon <= maxOf(p1lon, p2lon)) {
                        if (p1lat != p2lat) {
                            val xinters = (lat - p1lat) * (p2lon - p1lon) / (p2lat - p1lat) + p1lon
                            if (p1lon == p2lon || lon <= xinters) {
                                inside = !inside
                            }
                        }
                    }
                }
            }
            
            // Move to next edge
            p1lat = p2lat
            p1lon = p2lon
        }
        
        return inside
    }

    /**
     * Get readable location status message
     */
    fun getLocationStatusMessage(isInside: Boolean): String {
        return if (isInside) {
            "Lokasi Anda berada di dalam area yang diizinkan"
        } else {
            "Lokasi Anda berada di luar area yang diizinkan. Silakan masuk ke area yang telah ditentukan untuk dapat login."
        }
    }

    /**
     * Calculate approximate distance from point to nearest polygon edge (in meters)
     * This is a simplified calculation for user feedback
     */
    fun getDistanceToNearestBoundary(
        context: Context,
        latitude: Double,
        longitude: Double,
        geoJsonFileName: String = "map.geojson"
    ): Double {
        try {
            val geoJsonString = loadGeoJsonFromAssets(context, geoJsonFileName)
            val geoJson = JSONObject(geoJsonString)
            val features = geoJson.getJSONArray("features")
            
            var minDistance = Double.MAX_VALUE
            
            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val geometry = feature.getJSONObject("geometry")
                val type = geometry.getString("type")
                
                if (type == "Polygon") {
                    val coordinates = geometry.getJSONArray("coordinates")
                    val polygon = coordinates.getJSONArray(0)
                    
                    // Check distance to each edge
                    for (j in 0 until polygon.length() - 1) {
                        val point = polygon.getJSONArray(j)
                        val pointLon = point.getDouble(0)
                        val pointLat = point.getDouble(1)
                        
                        val distance = haversineDistance(latitude, longitude, pointLat, pointLon)
                        if (distance < minDistance) {
                            minDistance = distance
                        }
                    }
                }
            }
            
            return minDistance
            
        } catch (e: Exception) {
            AppLogger.e("Error calculating distance: ${e.message}")
            return Double.MAX_VALUE
        }
    }

    /**
     * Haversine formula to calculate distance between two coordinates
     */
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Earth's radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return R * c
    }
}
