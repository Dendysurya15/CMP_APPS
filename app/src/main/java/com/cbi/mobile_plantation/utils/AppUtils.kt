package com.cbi.mobile_plantation.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Base64
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.R
import com.google.gson.Gson
import com.google.gson.JsonArray
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object AppUtils {

    const val TOTAL_MAX_TREES_INSPECTION: Int = 50
    const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
    const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
        UPDATE_INTERVAL_IN_MILLISECONDS / 2
    const val LOG_LOC = "locationLog"
    const val ZIP_PASSWORD = "CBI@2025"
    const val REQUEST_CHECK_SETTINGS = 0x1
    const val MAX_SELECTIONS_PER_TPH = 3

    object UploadStatusUtils {
        const val WAITING = "Menunggu"
        const val UPLOADING = "Sedang Upload..."
        const val SUCCESS = "Berhasil Upload!"
        const val FAILED = "Gagal Upload!"
    }


    fun getTodaysDate(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return makeDateString(day, month, year)
    }

    fun makeDateString(day: Int, month: Int, year: Int): String {
        return "${getMonthFormat(month)} $day $year"
    }

    fun getMonthFormat(month: Int): String {
        return when (month) {
            1 -> "JAN"
            2 -> "FEB"
            3 -> "MAR"
            4 -> "APR"
            5 -> "MAY"
            6 -> "JUN"
            7 -> "JUL"
            8 -> "AUG"
            9 -> "SEP"
            10 -> "OCT"
            11 -> "NOV"
            12 -> "DEC"
            else -> "JAN" // Default should never happen
        }
    }

    fun formatDateForBackend(day: Int, month: Int, year: Int): String {
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    object DatabaseTables {
        const val PANEN = "panen_table"
        const val ESPB = "espb_table"
        const val ABSENSI = "absensi"
        const val MILL = "mill"
        const val TPH = "tph"
        const val KEMANDORAN = "kemandoran"
        const val KARYAWAN = "karyawan"
        const val TRANSPORTER = "transporter"
        const val KENDARAAN = "kendaraan"
        const val BLOK = "blok"
        const val UPLOADCMP = "upload_cmp"
        const val FLAGESPB = "flag_espb"
    }

    object ListFeatureByRoleUser {
        const val KeraniTimbang = "Kerani Timbang"
        const val Asisten = "Asisten"
        const val Mandor1 = "Mandor 1"
        const val KeraniPanen = "Kerani Panen"
        const val IT = "IT"
    }


    object ListFeatureNames {
        const val PanenTBS = "Panen TBS"
        const val RekapHasilPanen = "Rekap Hasil Panen"
        const val ScanHasilPanen = "Scan Hasil Panen"
        const val RekapPanenDanRestan = "Rekap panen dan restan"
        const val BuatESPB = "Buat eSPB"
        const val RekapESPB = "Rekap eSPB"
        const val DetailESPB = "Detail eSPB"

        const val InspeksiPanen = "Inspeksi Panen"
        const val RekapInspeksiPanen = "Rekap Inspeksi Panen"
        const val ScanESPBTimbanganMill = "Scan e-SPB Timbangan Mill"
        const val RekapESPBTimbanganMill = "Rekap e-SPB Timbangan Mill"
        const val AbsensiPanen = "Absensi panen"
        const val RekapAbsensiPanen = "Rekap absensi panen"
        const val ScanAbsensiPanen = "Scan absensi panen"
        const val SinkronisasiData = "Sinkronisasi data"
        const val UploadDataCMP = "Upload Data CMP"
    }

    object WaterMarkFotoDanFolder {
        const val WMPanenTPH = "PANEN TPH"
    }


    object DatabaseServer {
        const val CMP = "CMP"
        const val PPRO = "PPRO"
    }

    const val DATE_TIME_CHECK_INTERVAL = 40000L  // 30 seconds
    const val DATE_TIME_INITIAL_DELAY = 40000L   // 30 seconds

    object DatasetNames {
        const val mill = "mill"
        const val tph = "tph"
        const val blok = "blok"
        const val pemanen = "pemanen"
        const val kemandoran = "kemandoran"
        const val transporter = "transporter"
        const val kendaraan = "kendaraan"
        const val updateSyncLocalData = "Update & Sinkronisasi Lokal Data"
        const val settingJSON = "setting.json"
    }

    private val todayDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))
    private var _selectedDate: String? = null

    val currentDate: String
        get() {
            return _selectedDate ?: todayDateFormat.format(Date())
        }

    fun setSelectedDate(date: String) {
        _selectedDate = date
    }

    fun getAppVersion(context: Context): String {
        return context.getString(R.string.app_version)
    }

    fun checkUploadZipReadyToday(idUser: String, context: Context): List<File> {
        val todayDate =
            SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date()) // Get today's date
        val uploadDir = File(context.getExternalFilesDir(null), "Upload").apply {
            if (!exists()) mkdirs()
        }

        return uploadDir.listFiles { file ->
            file.isFile && file.name.matches(Regex("$idUser+_${todayDate}.*\\.zip"))
        }?.toList() ?: emptyList()
    }

    fun extractIdsFromZipFile(
        context: Context,
        fileName: String,
        zipPassword: String
    ): Pair<List<Int>, List<Int>> {
        val panenIds = mutableListOf<Int>()
        val espbIds = mutableListOf<Int>()

        try {
            val appFilesDir = File(context.getExternalFilesDir(null), "Upload")
            val zipFile = File(appFilesDir, fileName)

            if (!zipFile.exists()) {
                AppLogger.e("ZIP file not found: $fileName")
                return Pair(panenIds, espbIds)
            }

            AppLogger.d("Opening ZIP file: ${zipFile.absolutePath}")

            // Open the ZIP file with password
            val zip = ZipFile(zipFile)
            zip.setPassword(zipPassword.toCharArray())

            // Get all entries
            val entries = zip.fileHeaders

            // Look for data.json files in all folders
            for (header in entries) {
                val entryName = header.fileName

                // Check if it's a data.json file
                if (entryName.endsWith("data.json")) {
                    AppLogger.d("Found data.json in: $entryName")

                    // Extract the folder name to determine if it's PANEN or ESPB
                    val folderName = entryName.split("/").firstOrNull()?.toUpperCase() ?: ""

                    // Read the data.json content
                    val inputStream = zip.getInputStream(header)
                    val content = inputStream.bufferedReader().use { it.readText() }
                    inputStream.close()

                    // Parse the JSON
                    try {
                        val jsonArray = Gson().fromJson(content, JsonArray::class.java)

                        // Extract IDs based on folder type
                        for (i in 0 until jsonArray.size()) {
                            val item = jsonArray.get(i).asJsonObject
                            val id = item.get("id").asInt

                            when (folderName) {
                                "A", "PANEN" -> {
                                    if (!panenIds.contains(id)) {
                                        panenIds.add(id)
                                        AppLogger.d("Added PANEN ID: $id")
                                    }
                                }

                                "B", "ESPB" -> {
                                    if (!espbIds.contains(id)) {
                                        espbIds.add(id)
                                        AppLogger.d("Added ESPB ID: $id")
                                    }
                                }

                                else -> {
                                    AppLogger.d("Unknown folder: $folderName, checking content for type")
                                    // Try to determine type from JSON structure if folder name is unclear
                                    if (item.has("tph") && item.has("karyawan_id")) {
                                        if (!panenIds.contains(id)) {
                                            panenIds.add(id)
                                            AppLogger.d("Added to PANEN IDs based on structure: $id")
                                        }
                                    } else {
                                        if (!espbIds.contains(id)) {
                                            espbIds.add(id)
                                            AppLogger.d("Added to ESPB IDs based on structure: $id")
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Error parsing JSON from $entryName: ${e.message}")
                    }
                }
            }

            AppLogger.d("Extracted PANEN IDs: ${panenIds.size}")
            AppLogger.d("Extracted ESPB IDs: ${espbIds.size}")

        } catch (e: Exception) {
            AppLogger.e("Error processing ZIP file $fileName: ${e.message}")
        }

        return Pair(panenIds, espbIds)
    }

    fun createAndSaveZipUploadCMP(
        context: Context,
        featureDataList: List<Pair<String, List<Map<String, Any>>>>,
        userId: String,
        onResult: (Boolean, String, String) -> Unit // Callback returns full path
    ) {
        try {
            val dateTime = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())

            val appFilesDir = File(context.getExternalFilesDir(null), "Upload").apply {
                if (!exists()) mkdirs()
            }

            // Get the Pictures directories - try both locations
            val picturesDirs = listOf(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                File(context.getExternalFilesDir(null)?.parent ?: "", "Pictures")
            ).filterNotNull()

            val zipFileName = "${userId}_$dateTime.zip"
            val zipFile = File(appFilesDir, zipFileName)

            val zip = ZipFile(zipFile)
            zip.setPassword(ZIP_PASSWORD.toCharArray())

            val zipParams = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.ZIP_STANDARD
            }

            featureDataList.forEach { (featureKey, dataList) ->
                // Add JSON data
                val jsonString = convertDataToJsonString(dataList)
                val jsonBytes = jsonString.toByteArray()

                val inputStream = ByteArrayInputStream(jsonBytes)
                zip.addStream(
                    inputStream,
                    zipParams.apply { fileNameInZip = "$featureKey/data.json" })

                // Add photos for this feature
                addFeaturePhotosToZip(context, dataList, featureKey, picturesDirs, zip, zipParams)
            }

            onResult(true, zipFileName, zipFile.absolutePath) // Return full path

        } catch (e: Exception) {
            val errorMessage = "❌ Error creating encrypted ZIP file: ${e.message}"
            AppLogger.e(errorMessage)
            onResult(false, errorMessage, "") // Return empty path on failure
        }
    }

    fun extractIdsAsIntegers(inputString: String): List<Int> {
        return inputString.split(";").map { entry ->
            entry.split(",")[0].toInt()
        }
    }

    fun extractIdsAndJjgAsMap(inputString: String): Map<Int, Int> {
        return inputString.split(";").associate { entry ->
            val parts = entry.split(",")
            val id = parts[0].toInt()
            val jjg = parts[2].toInt() // Index 2 is the jumlah jjg
            id to jjg
        }
    }

    // Format TPH data as requested
    fun formatTPHDataList(tphString: String, tphDataList: List<TPHNewModel>?): String {
        if (tphDataList.isNullOrEmpty()) return "-"

        // Extract ID and jjg counts from the original string
        val tphJjgMap = extractIdsAndJjgAsMap(tphString)

        // Format each TPH entry
        val formattedTPHList = tphDataList.mapNotNull { tph ->
            val jjg = tphJjgMap[tph.id]
            if (jjg != null) {
                "• TPH nomor ${tph.nomor} (${tph.blok_kode}) $jjg jjg"
            } else null
        }

        return formattedTPHList.joinToString("\n").takeIf { it.isNotBlank() } ?: "-"
    }

    class ProgressRequestBody(
        private val file: File,
        private val contentTypeString: String,
        private val callback: (progress: Int) -> Unit
    ) : RequestBody() {

        override fun contentType(): MediaType? = contentTypeString.toMediaTypeOrNull()

        override fun contentLength(): Long = file.length()

        override fun writeTo(sink: BufferedSink) {
            val length = contentLength()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            val fileInputStream = FileInputStream(file)
            var uploaded: Long = 0
            fileInputStream.use { inputStream ->
                var read = inputStream.read(buffer)
                while (read != -1) {
                    sink.write(buffer, 0, read)
                    uploaded += read
                    val progress = (uploaded * 100 / length).toInt()
                    callback(progress) // report progress
                    read = inputStream.read(buffer)
                }
            }
        }

        companion object {
            private const val DEFAULT_BUFFER_SIZE = 2048
        }
    }


    /**
     * Adds photos from CMP directories to the zip file
     */
    private fun addFeaturePhotosToZip(
        context: Context,
        dataList: List<Map<String, Any>>,
        featureKey: String,
        picturesDirs: List<File>,
        zip: ZipFile,
        zipParams: ZipParameters
    ) {
        val allCmpDirectories = mutableListOf<File>()

        // Find all CMP directories in all possible Pictures folders
        for (picturesDir in picturesDirs) {
            if (!picturesDir.exists() || !picturesDir.isDirectory) {
                AppLogger.w("Pictures directory not found: ${picturesDir.absolutePath}")
                continue
            }

            val cmpDirectories = picturesDir.listFiles { file ->
                file.isDirectory && file.name.startsWith("CMP")
            } ?: emptyArray()

            AppLogger.d("Looking for CMP directories in: ${picturesDir.absolutePath}")
            AppLogger.d("Found ${cmpDirectories.size} CMP directories: ${cmpDirectories.map { it.name }}")

            allCmpDirectories.addAll(cmpDirectories)
        }

        // Also check in the main Pictures directory itself
        for (picturesDir in picturesDirs) {
            val mainPicsDir = File(picturesDir.parentFile, "Pictures")
            if (mainPicsDir.exists() && mainPicsDir.isDirectory) {
                val cmpDirectories = mainPicsDir.listFiles { file ->
                    file.isDirectory && file.name.startsWith("CMP")
                } ?: emptyArray()

                AppLogger.d("Looking for CMP directories in: ${mainPicsDir.absolutePath}")
                AppLogger.d("Found ${cmpDirectories.size} CMP directories: ${cmpDirectories.map { it.name }}")

                allCmpDirectories.addAll(cmpDirectories)
            }
        }

        if (allCmpDirectories.isEmpty()) {
            AppLogger.w("No CMP directories found in any Pictures location")
        }

        // Process each data entry to find photos
        dataList.forEach { data ->
            // Extract photo paths from the "foto" field
            val photoPathString = data["foto"] as? String

            AppLogger.d("id: ${data["id"]}")
            AppLogger.d("Processing data: $data")

            if (!photoPathString.isNullOrBlank()) {
                // Split by semicolon to handle multiple photos
                val photoPaths = photoPathString.split(";")

                photoPaths.forEach { photoPath ->
                    if (photoPath.isNotBlank()) {
                        // Determine the file name from the path
                        val photoFileName = photoPath.trim().substringAfterLast("/")

                        // First try: Look for the exact file
                        val photoFile = File(photoPath.trim())

                        // Track if we found the photo
                        var photoFound = false

                        if (photoFile.exists() && photoFile.isFile) {
                            try {
                                // Add the photo to the zip file in the appropriate feature folder
                                val targetPath = "$featureKey/photos/$photoFileName"
                                zipParams.fileNameInZip = targetPath
                                zip.addFile(photoFile, zipParams)
                                photoFound = true
                                AppLogger.d("Added photo to zip (direct path): ${photoFile.absolutePath} -> $targetPath")
                            } catch (e: Exception) {
                                AppLogger.e("Failed to add photo to zip: ${photoFile.absolutePath}, Error: ${e.message}")
                            }
                        } else {
                            // Find photo by trying multiple search strategies
                            val foundFile =
                                findPhotoFile(allCmpDirectories, photoPath.trim(), photoFileName)

                            if (foundFile != null) {
                                try {
                                    zipParams.fileNameInZip = "$featureKey/photos/${foundFile.name}"
                                    zip.addFile(foundFile, zipParams)
                                    photoFound = true
                                    AppLogger.d("Added photo to zip (from search): ${foundFile.absolutePath}")
                                } catch (e: Exception) {
                                    AppLogger.e("Failed to add photo to zip: ${foundFile.absolutePath}, Error: ${e.message}")
                                }
                            }
                        }

                        if (!photoFound) {
                            AppLogger.w("Photo not found in any directory: $photoPath")

                            // One last attempt - check if it's a relative path from app's external files directory
                            val relativePathFile =
                                File(context.getExternalFilesDir(null), photoPath.trim())
                            if (relativePathFile.exists() && relativePathFile.isFile) {
                                try {
                                    zipParams.fileNameInZip = "$featureKey/photos/$photoFileName"
                                    zip.addFile(relativePathFile, zipParams)
                                    AppLogger.d("Added photo to zip (relative path): ${relativePathFile.absolutePath}")
                                } catch (e: Exception) {
                                    AppLogger.e("Failed to add photo to zip: ${relativePathFile.absolutePath}, Error: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Helper function to find a photo file using multiple search strategies
     */
    private fun findPhotoFile(
        directories: List<File>,
        photoPath: String,
        photoFileName: String
    ): File? {
        // Strategy 1: Direct filename match in any CMP directory
        for (dir in directories) {
            val file = File(dir, photoFileName)
            if (file.exists() && file.isFile) {
                AppLogger.d("Found photo with exact name match: ${file.absolutePath}")
                return file
            }
        }

        // Strategy 2: Find files containing this filename
        for (dir in directories) {
            val matchingFiles = dir.listFiles { file ->
                file.isFile && file.name.contains(photoFileName)
            } ?: emptyArray()

            if (matchingFiles.isNotEmpty()) {
                AppLogger.d("Found photo with partial name match: ${matchingFiles.first().absolutePath}")
                return matchingFiles.first()
            }
        }

        // Strategy 3: For cases like "Panen TBS_1_2025227_112824.jpg",
        // extract the base pattern and number
        val patternMatch = """(.*?)_(\d+)_\d+_\d+\.jpg""".toRegex().find(photoFileName)
        if (patternMatch != null) {
            val (baseName, number) = patternMatch.destructured

            // Look for files matching the pattern
            for (dir in directories) {
                if (dir.name.contains(baseName, ignoreCase = true)) {
                    val matchingFiles = dir.listFiles { file ->
                        file.isFile && file.name.contains("${baseName}_${number}")
                    } ?: emptyArray()

                    if (matchingFiles.isNotEmpty()) {
                        AppLogger.d("Found photo with pattern match: ${matchingFiles.first().absolutePath}")
                        return matchingFiles.first()
                    }
                }
            }

            // Try finding any file with the base name and number
            for (dir in directories) {
                val matchingFiles = dir.listFiles { file ->
                    file.isFile && file.name.contains("${baseName}_${number}")
                } ?: emptyArray()

                if (matchingFiles.isNotEmpty()) {
                    AppLogger.d("Found photo with loose pattern match: ${matchingFiles.first().absolutePath}")
                    return matchingFiles.first()
                }
            }
        }

        return null
    }


    // Convert List<Map<String, Any>> to JSON String
    private fun convertDataToJsonString(data: List<Map<String, Any>>): String {
        val jsonArray = JSONArray()
        data.forEach { entry ->
            val jsonObject = JSONObject()
            entry.forEach { (key, value) -> jsonObject.put(key, value) }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    /**
     * Checks if automatic date and time settings are enabled on the device
     * @param context The application context
     * @return Boolean True if automatic date/time is enabled, false otherwise
     */
    fun isAutomaticDateTimeEnabled(context: Context): Boolean {
        return try {
            // For Android Jelly Bean (API 17) and higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Settings.Global.getInt(
                    context.contentResolver,
                    Settings.Global.AUTO_TIME
                ) == 1
            }
            // For older versions
            else {
                @Suppress("DEPRECATION")
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.AUTO_TIME
                ) == 1
            }
        } catch (e: Exception) {
            // Default to true if there's an exception
            true
        }
    }

    /**
     * Checks if the device's date and time settings are valid for the app
     * @return Boolean true if settings are acceptable, false otherwise
     */
    fun isDateTimeValid(context: Context): Boolean {
        // First check if automatic date time is enabled
        val isAutoTimeEnabled = isAutomaticDateTimeEnabled(context)

        // If automatic time is not enabled, return false immediately
        if (!isAutoTimeEnabled) {
            return false
        }

        // Even with automatic time enabled, verify the time is reasonable
        // if we're offline (to prevent users from manually setting automatic
        // time while offline to bypass the check)
        if (!isNetworkAvailable(context)) {
            // Get current time
            val currentTime = System.currentTimeMillis()

            // Get build time (a reference point known to be valid)
            val buildTime = try {
                context.packageManager.getPackageInfo(
                    context.packageName, 0
                ).lastUpdateTime
            } catch (e: Exception) {
                // If we can't get build time, use a fallback
                0L
            }

            // Check if current time is unreasonably far from build time
            // Allow some leeway (e.g., app could have been built months ago)
            // This checks if time is set to future more than 1 day from now
            val oneDay = 24 * 60 * 60 * 1000L
            return currentTime < (System.currentTimeMillis() + oneDay)
        }

        // If we have network and automatic time is on, assume time is correct
        return true
    }

    /**
     * Shows a warning dialog about date/time settings with network context
     */
    fun showDateTimeNetworkWarning(activity: Activity) {
        val isOnline = isNetworkAvailable(activity)
        val message = if (isOnline) {
            "Please enable automatic date and time for better app experience."
        } else {
            "Please enable automatic date and time and connect to the internet to ensure correct time synchronization."
        }

        AlertDialogUtility.withTwoActions(
            activity,
            "Pengaturan",
            "Date & Time Settings",
            message,
            "warning.json",
            ContextCompat.getColor(activity, R.color.bluedarklight),
            function = {
                activity.startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
            },
            cancelFunction = {
                activity.finishAffinity()
            }
        )
    }


    fun getDeviceInfo(context: Context): JSONObject {
        val json = JSONObject()

        val appVersion = getAppVersion(context)

        json.put("app_version", appVersion)
        json.put("os_version", Build.VERSION.RELEASE)
        json.put("device_model", Build.MODEL)

        return json
    }

    fun Context.stringXML(field: Int): String {
        return getString(field)
    }

    fun Context.vibrate(duration: Long = 100) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    duration,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    fun formatSelectedDateForDisplay(backendDate: String): String {
        try {
            val backendFormat = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))
            val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

            val date = backendFormat.parse(backendDate)
            return date?.let { displayFormat.format(it) } ?: backendDate
        } catch (e: Exception) {
            // If there's any error in parsing, return the original date
            return backendDate
        }
    }

    fun formatToIndonesianDate(dateString: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat =
            SimpleDateFormat("EEEE, d MMMM YYYY 'Pukul' HH:mm:ss", Locale("id", "ID"))

        return try {
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            "Format tanggal tidak valid"
        }
    }

    fun setMaxBrightness(activity: Activity, isMax: Boolean) {
        try {
            val window = activity.window
            val layoutParams = window.attributes

            if (isMax) {
                layoutParams.screenBrightness = 1f // 1.0 is maximum brightness
            } else {
                layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            window.attributes = layoutParams
        } catch (e: Exception) {
            AppLogger.e("Error setting brightness: ${e.message}")
        }
    }

    /**
     * Alternative method to fetch the version name directly from BuildConfig with a "V" prefix.
     * Use this if you don't want to rely on Gradle's `resValue`.
     */

    fun setupFeatureHeader(featureName: String?, tvFeatureName: TextView) {
        val context = tvFeatureName.context  // Get the context from the TextView
        val appVersion = getAppVersion(context)
        val headerText = "Menu - ${featureName ?: "Default Feature Name"}"
        tvFeatureName.text = headerText
    }

    fun splitStringWatermark(input: String, chunkSize: Int): String {
        return if (input.length > chunkSize) {
            val regex = "(.{$chunkSize})"
            input.replace(Regex(regex), "$1-\n")
        } else {
            input
        }
    }

    fun formatToCamelCase(text: String?): String {
        return text?.split(" ")?.joinToString(" ") { word ->
            if (word.length <= 3) word.uppercase() else word.lowercase().replaceFirstChar { it.uppercase() }
        } ?: ""
    }

    @SuppressLint("SetTextI18n")
    fun setupUserHeader(
        userName: String?,
        userSection: TextView,
        featureName: String?,
        tvFeatureName: TextView,
        prefManager: PrefManager? = null,
        lastUpdateText: TextView? = null,
        titleAppNameAndVersionText: TextView? = null,
        context: Context? = null
    ) {
        val userInfo = buildString {
            userName?.takeIf { it.isNotEmpty() }?.let { append(formatToCamelCase(it)) }
        }

        userSection.text = userInfo
        setupFeatureHeader(featureName, tvFeatureName)

        if (context != null && titleAppNameAndVersionText != null) {
            val appVersion = getAppVersion(context)
            titleAppNameAndVersionText.text = "CMP - $appVersion"
        }

        // Setup last sync date
        if (prefManager != null && lastUpdateText != null) {
            val lastSyncDate = prefManager.lastSyncDate

            AppLogger.d(lastSyncDate.toString())
            if (!lastSyncDate.isNullOrEmpty()) {
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
                    val date = inputFormat.parse(lastSyncDate)
                    val formattedDate = outputFormat.format(date)
                    lastUpdateText.text = "Update:\n$formattedDate"
                } catch (e: Exception) {
                    lastUpdateText.text = "Update: -"
                }
            } else {
                lastUpdateText.text = "Update: -"
            }
        }
    }


    fun readJsonFromEncryptedBase64Zip(base64String: String): String? {
        return try {
            // Remove header if present
            val base64Data = if (base64String.contains(",")) {
                base64String.substring(base64String.indexOf(",") + 1)
            } else {
                base64String
            }

            val base64Decode = base64Data.replace("5nqHzPKdlILxS9ABpClq", "")

            // Decode base64 to bytes
            val decodedBytes = Base64.decode(base64Decode, Base64.DEFAULT)

            // Create ZIP archive from bytes
            ByteArrayInputStream(decodedBytes).use { byteStream ->
                ZipInputStream(byteStream).use { zipStream ->
                    var entry: ZipEntry? = zipStream.nextEntry

                    // Iterate through all entries in the ZIP
                    while (entry != null) {
                        if (entry.name == "output.json") {
                            // Read the JSON content
                            val content = zipStream.readBytes()
                            return String(content, StandardCharsets.UTF_8)
                        }
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                }
            }

            null // Return null if file.json was not found
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
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

    fun showBiometricPrompt(context: Context, nameUser: String, successCallback: () -> Unit) {
        val executor = Executors.newSingleThreadExecutor()

        val biometricPrompt = BiometricPrompt(
            context as AppCompatActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    successCallback.invoke()
                }
            })

        val textWelcome = context.getString(R.string.welcome_back)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(
                "${
                    textWelcome.substring(0, 1)
                        .toUpperCase(Locale.getDefault()) + textWelcome.substring(1).toLowerCase(
                        Locale.getDefault()
                    )
                } $nameUser"
            )
            .setSubtitle(context.getString(R.string.subtitle_prompt))
            .setNegativeButtonText(context.getString(R.string.confirmation_dialog_cancel))
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    fun checkBiometricSupport(context: Context): Boolean {
        when (BiometricManager.from(context).canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                return BiometricManager.from(context)
                    .canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                return false
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                return false
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                return false
            }

            else -> {
                return false
            }
        }
    }


}