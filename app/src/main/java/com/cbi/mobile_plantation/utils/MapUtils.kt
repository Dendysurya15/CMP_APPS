package com.cbi.mobile_plantation.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.modules.IArchiveFile
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

object MapUtils {

    fun getTileSource(
        context: Context,
        prefManager: PrefManager,
        mapView: MapView? = null
    ): ITileSource {
        return when {
            prefManager.isDownloadedMapOffline -> {
                AppLogger.d("Using offline map tiles")
                loadOfflineMapTileSource(context, prefManager, mapView)
            }
            else -> {
                createOnlineTileSource(context)
            }
        }
    }

    private fun loadOfflineMapTileSource(
        context: Context,
        prefManager: PrefManager,
        mapView: MapView?
    ): ITileSource {
        try {
            val userEstate = prefManager.estateUserLogin
            val tilesDir = File(
                context.getExternalFilesDir(null),
                "map_offline/$userEstate/tiles"
            )

            AppLogger.d("Loading offline tiles from: ${tilesDir.absolutePath}")

            if (!tilesDir.exists()) {
                AppLogger.e("Tiles directory doesn't exist")
                return createOnlineTileSource(context)
            }

            val metadataFile = File(tilesDir, "region_metadata.json")
            if (!metadataFile.exists()) {
                AppLogger.e("region_metadata.json not found")
                return createOnlineTileSource(context)
            }

            val metadata = JSONObject(metadataFile.readText())
            val minZoom = metadata.getInt("minZoom")
            val maxZoom = metadata.getInt("maxZoom")
            val estatePath = metadata.getString("path")
            val estateName = metadata.getString("name")

            AppLogger.d("Estate: $estateName")
            AppLogger.d("Zoom range: $minZoom - $maxZoom")

            val estateFolder = File(tilesDir, estatePath)
            AppLogger.d("Estate folder: ${estateFolder.absolutePath}")

            if (!estateFolder.exists()) {
                AppLogger.e("Estate folder doesn't exist")
                return createOnlineTileSource(context)
            }

            // Set map center from bounds (only if mapView provided)
            mapView?.let {
                val bounds = metadata.getJSONObject("bounds")
                val centerLat = (bounds.getDouble("sw_lat") + bounds.getDouble("ne_lat")) / 2
                val centerLng = (bounds.getDouble("sw_lng") + bounds.getDouble("ne_lng")) / 2
                it.controller?.setCenter(GeoPoint(centerLat, centerLng))
                AppLogger.d("Map centered at: $centerLat, $centerLng")
            }

            // Create custom tile source
            val offlineTileSource = object : BitmapTileSourceBase(
                "OfflineMap",
                minZoom,
                maxZoom,
                256,
                ".webp"
            ) {
                override fun getDrawable(aFilePath: String?): Drawable? {
                    return try {
                        if (aFilePath == null) return null
                        val file = File(aFilePath)
                        if (!file.exists()) {
                            AppLogger.d("Tile not found: ${file.name}")
                            return null
                        }
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            AppLogger.d("✅ Loaded: ${file.name}")
                            BitmapDrawable(context.resources, bitmap)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Error loading tile: ${e.message}")
                        null
                    }
                }

                override fun getDrawable(aFileInputStream: InputStream?): Drawable? {
                    return try {
                        if (aFileInputStream == null) return null
                        val bitmap = BitmapFactory.decodeStream(aFileInputStream)
                        if (bitmap != null) {
                            BitmapDrawable(context.resources, bitmap)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Error decoding stream: ${e.message}")
                        null
                    }
                }
            }

            // Create custom archive file for x-y.webp format
            val archiveProvider = MapTileFileArchiveProvider(
                SimpleRegisterReceiver(context),
                offlineTileSource,
                arrayOf(object : IArchiveFile {
                    override fun init(pFile: File?) {}

                    override fun getInputStream(
                        pTileSource: ITileSource?,
                        pTile: Long
                    ): InputStream? {
                        val zoom = MapTileIndex.getZoom(pTile)
                        val x = MapTileIndex.getX(pTile)
                        val y = MapTileIndex.getY(pTile)

                        val tileFile = File(estateFolder, "$zoom/$x-$y.webp")

                        return if (tileFile.exists()) {
                            FileInputStream(tileFile)
                        } else {
                            null
                        }
                    }

                    override fun close() {}

                    override fun getTileSources(): MutableSet<String> {
                        return mutableSetOf("OfflineMap")
                    }

                    override fun setIgnoreTileSource(pIgnoreTileSource: Boolean) {}
                })
            )

            // Set the tile source and provider
            mapView?.apply {
                setTileSource(offlineTileSource)
                tileProvider = MapTileProviderArray(
                    offlineTileSource,
                    SimpleRegisterReceiver(context),
                    arrayOf<MapTileModuleProviderBase>(archiveProvider)
                )
            }

            return offlineTileSource

        } catch (e: Exception) {
            AppLogger.e("Error loading offline map: ${e.message}")
            e.printStackTrace()
            return createOnlineTileSource(context)
        }
    }

    fun loadOfflineMapTileSourceForFullscreen(
        context: Context,
        prefManager: PrefManager,
        mapView: MapView
    ): ITileSource {
        try {
            val userEstate = prefManager.estateUserLogin
            val tilesDir = File(
                context.getExternalFilesDir(null),
                "map_offline/$userEstate/tiles"
            )

            AppLogger.d("🗺️ [FULLSCREEN] Loading offline tiles from: ${tilesDir.absolutePath}")

            if (!tilesDir.exists()) {
                AppLogger.e("❌ [FULLSCREEN] Tiles directory doesn't exist")
                return createOnlineTileSource(context)
            }

            val metadataFile = File(tilesDir, "region_metadata.json")
            if (!metadataFile.exists()) {
                AppLogger.e("❌ [FULLSCREEN] region_metadata.json not found")
                return createOnlineTileSource(context)
            }

            val metadata = JSONObject(metadataFile.readText())
            val minZoom = metadata.getInt("minZoom")
            val maxZoom = metadata.getInt("maxZoom")
            val estatePath = metadata.getString("path")
            val estateName = metadata.getString("name")

            AppLogger.d("✅ [FULLSCREEN] Estate: $estateName")
            AppLogger.d("✅ [FULLSCREEN] Zoom range: $minZoom - $maxZoom")

            val estateFolder = File(tilesDir, estatePath)
            AppLogger.d("📁 [FULLSCREEN] Estate folder: ${estateFolder.absolutePath}")
            AppLogger.d("📁 [FULLSCREEN] Estate folder exists: ${estateFolder.exists()}")

            if (!estateFolder.exists()) {
                AppLogger.e("❌ [FULLSCREEN] Estate folder doesn't exist")
                return createOnlineTileSource(context)
            }

            // List some sample files to verify structure
            val zoomFolders = estateFolder.listFiles()?.filter { it.isDirectory }
            AppLogger.d("📂 [FULLSCREEN] Found zoom folders: ${zoomFolders?.map { it.name }}")
            zoomFolders?.firstOrNull()?.let { zoomFolder ->
                val sampleTiles = zoomFolder.listFiles()?.take(3)?.map { it.name }
                AppLogger.d("📄 [FULLSCREEN] Sample tiles in ${zoomFolder.name}: $sampleTiles")
            }

            // Create custom tile source
            val offlineTileSource = object : BitmapTileSourceBase(
                "OfflineMapFullscreen",
                minZoom,
                maxZoom,
                256,
                ".webp"
            ) {
                override fun getDrawable(aFilePath: String?): Drawable? {
                    return try {
                        if (aFilePath == null) return null
                        val file = File(aFilePath)
                        if (!file.exists()) {
                            return null
                        }
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            BitmapDrawable(context.resources, bitmap)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        AppLogger.e("❌ [FULLSCREEN] Error loading tile: ${e.message}")
                        null
                    }
                }

                override fun getDrawable(aFileInputStream: InputStream?): Drawable? {
                    return try {
                        if (aFileInputStream == null) return null
                        val bitmap = BitmapFactory.decodeStream(aFileInputStream)
                        if (bitmap != null) {
                            BitmapDrawable(context.resources, bitmap)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        AppLogger.e("❌ [FULLSCREEN] Error decoding stream: ${e.message}")
                        null
                    }
                }
            }

            // ✅ CREATE THE TILE PROVIDER WITH CUSTOM ARCHIVE
            AppLogger.d("🔧 [FULLSCREEN] Setting up tile provider with custom archive")

            val archiveProvider = MapTileFileArchiveProvider(
                SimpleRegisterReceiver(context),
                offlineTileSource,
                arrayOf(object : IArchiveFile {
                    override fun init(pFile: File?) {
                        AppLogger.d("🔧 [FULLSCREEN] IArchiveFile init called")
                    }

                    override fun getInputStream(
                        pTileSource: ITileSource?,
                        pTile: Long
                    ): InputStream? {
                        val zoom = MapTileIndex.getZoom(pTile)
                        val x = MapTileIndex.getX(pTile)
                        val y = MapTileIndex.getY(pTile)

                        val tileFile = File(estateFolder, "$zoom/$x-$y.webp")

                        AppLogger.d("🔍 [FULLSCREEN] Requesting tile: $zoom/$x-$y.webp")
                        AppLogger.d("📍 [FULLSCREEN] Full path: ${tileFile.absolutePath}")
                        AppLogger.d("✓ [FULLSCREEN] File exists: ${tileFile.exists()}")

                        return if (tileFile.exists()) {
                            AppLogger.d("✅ [FULLSCREEN] Loading tile: $zoom/$x-$y.webp")
                            FileInputStream(tileFile)
                        } else {
                            AppLogger.w("⚠️ [FULLSCREEN] Tile not found: $zoom/$x-$y.webp")
                            null
                        }
                    }

                    override fun close() {
                        AppLogger.d("🔧 [FULLSCREEN] IArchiveFile close called")
                    }

                    override fun getTileSources(): MutableSet<String> {
                        return mutableSetOf("OfflineMapFullscreen")
                    }

                    override fun setIgnoreTileSource(pIgnoreTileSource: Boolean) {}
                })
            )

            // Set the tile provider on the map view
            mapView.tileProvider = MapTileProviderArray(
                offlineTileSource,
                SimpleRegisterReceiver(context),
                arrayOf<MapTileModuleProviderBase>(archiveProvider)
            )

            AppLogger.d("✅ [FULLSCREEN] Tile provider configured successfully")
            return offlineTileSource

        } catch (e: Exception) {
            AppLogger.e("❌ [FULLSCREEN] Error loading offline map: ${e.message}")
            e.printStackTrace()
            return createOnlineTileSource(context)
        }
    }

     fun createOnlineTileSource(context: Context): ITileSource {
        return if (AppUtils.isNetworkAvailable(context)) {
            AppLogger.d("Using online Google Satellite")
            object : OnlineTileSourceBase(
                "GoogleSatellite",
                0, 18, 256, ".webp",
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
        } else {
            AppLogger.d("No internet, using MAPNIK fallback")
            TileSourceFactory.MAPNIK
        }
    }

    suspend fun mergeChunksToZip(offlineMapDir: File): File = withContext(Dispatchers.IO) {
        val chunkFiles = offlineMapDir.listFiles { file ->
            file.name.startsWith("chunk_") && file.extension == "bin"
        }?.sortedBy { it.name } ?: emptyList()

        if (chunkFiles.isEmpty()) {
            throw Exception("No chunk files found")
        }

        val mergedZipFile = File(offlineMapDir, "merged_map.zip")

        AppLogger.d("Merging ${chunkFiles.size} chunks...")

        mergedZipFile.outputStream().use { output ->
            chunkFiles.forEach { chunkFile ->
                chunkFile.inputStream().use { input ->
                    input.copyTo(output)
                }
                AppLogger.d("Merged ${chunkFile.name}")
            }
        }

        AppLogger.d("Merged ZIP: ${mergedZipFile.length()} bytes")
        mergedZipFile
    }

    /**
     * Extract merged ZIP file to tiles directory
     */
    suspend fun extractZipToTiles(zipFile: File, baseDir: File) = withContext(Dispatchers.IO) {
        val tilesDir = File(baseDir, "tiles")
        if (!tilesDir.exists()) {
            tilesDir.mkdirs()
        }

        AppLogger.d("Extracting ZIP to ${tilesDir.absolutePath}")

        val zip = net.lingala.zip4j.ZipFile(zipFile)
        zip.extractAll(tilesDir.absolutePath)

        AppLogger.d("Extraction completed")
    }

    /**
     * Clean up temporary chunk files and merged ZIP after extraction
     */
    suspend fun cleanupTempFiles(offlineMapDir: File, mergedZipFile: File) = withContext(Dispatchers.IO) {
        AppLogger.d("Cleaning up temp files...")

        // Delete chunks
        val chunkFiles = offlineMapDir.listFiles { file ->
            file.name.startsWith("chunk_") && file.extension == "bin"
        } ?: emptyArray()

        chunkFiles.forEach { it.delete() }

        // Delete merged ZIP
        mergedZipFile.delete()

        AppLogger.d("Cleanup completed")
    }
}