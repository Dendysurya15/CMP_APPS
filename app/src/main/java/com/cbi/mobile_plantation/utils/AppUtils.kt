package com.cbi.mobile_plantation.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.cbi.mobile_plantation.data.model.TPHNewModel
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.ui.view.followUpInspeksi.ListFollowUpInspeksi
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object AppUtils {

    const val TOTAL_MAX_TREES_INSPECTION: Int = 100
    const val MINIMAL_TAKE_SELFIE_INSPECTION: Int = 10
    const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
    const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
        UPDATE_INTERVAL_IN_MILLISECONDS / 2
    const val LOG_LOC = "locationLog"
    const val ZIP_PASSWORD = "CBI@2025"
    const val REQUEST_CHECK_SETTINGS = 0x1
    const val MAX_SELECTIONS_PER_TPH = 3
    const val MAX_ALERT_FOR_GENERATE_QR = 60
    const val max_data_in_zip = 12
    const val half_json_encrypted = "5nqHzPKdlILxS9ABpClq"

    object UploadStatusUtils {
        const val WAITING = "Menunggu"
        const val UPLOADING = "Sedang Upload"
        const val SUCCESS = "Berhasil Upload!"
        const val DOWNLOADING = "Sedang Mengunduh"
        const val DOWNLOADED = "Berhasil Mengunduh!"
        const val UPTODATE = "Tidak ada pembaruan yang diperlukan!"
        const val UPDATED = "Berhasil diperbarui"
        const val DONE_CHECK = "Sudah diperiksa"
        const val FAILED = "Gagal Upload!"
        const val ERROR = "ERROR!"
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

    object FileConstants {
        fun getRootJson(context: Context): String =
            File(context.getExternalFilesDir(null), "JSON").toString()

        fun getRootJsonInternal(context: Context): String =
            File(context.filesDir, "JSON").toString()
    }

    suspend fun writeJsonToFile(
        context: Context,
        jsonObject: JSONObject,
        fileName: String,
        addTimestamp: Boolean = true,
        useInternal: Boolean = false
    ) = withContext(Dispatchers.IO) {
        try {
            val finalFileName = if (addTimestamp) {
                val timestamp =
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                "${fileName.removeSuffix(".json")}_$timestamp.json"
            } else {
                if (!fileName.endsWith(".json")) "$fileName.json" else fileName
            }

            val rootPath = if (useInternal) {
                FileConstants.getRootJsonInternal(context)
            } else {
                FileConstants.getRootJson(context)
            }

            val file = File(rootPath, finalFileName)
            file.parentFile?.mkdirs()

            file.writeText(jsonObject.toString(4))
            AppLogger.d("JSON written to: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            AppLogger.e("Error writing JSON to file", e.toString())
            null
        }
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

    fun showSnackbarWithSettings(
        activity: Activity,
        message: String,
        actionText: String = "Settings"
    ) {
        Snackbar.make(
            activity.findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(actionText) {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", activity.packageName, null)
                )
                activity.startActivity(intent)
            }
            .show()
    }

    fun formatDateForBackend(day: Int, month: Int, year: Int): String {
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    fun getBoundaryAccuracy(prefManager: PrefManager?): Float {
        return prefManager?.radiusMinimum ?:15F
//        return 5000F
    }

    object DatabaseTables {
        const val MUTU_BUAH = "mutu_buah"
        const val PANEN = "panen_table"
        const val JENIS_TPH = "jenis_tph"
        const val INSPEKSI = "inspeksi"
        const val INSPEKSI_DETAIL = "inspeksi_detail"
        const val ESPB = "espb_table"
        const val ABSENSI = "absensi"
        const val AFDELING = "afdeling"
        const val MILL = "mill"
        const val TPH = "tph"
        const val ESTATE = "estate"
        const val PARAMETER = "parameter"
        const val KEMANDORAN = "kemandoran"
        const val KARYAWAN = "karyawan"
        const val TRANSPORTER = "transporter"
        const val KENDARAAN = "kendaraan"
        const val BLOK = "blok"
        const val UPLOADCMP = "upload_cmp"
        const val FLAGESPB = "flag_espb"
        const val HEKTAR_PANEN = "hektar_panen"

        //for upload hektaran and hektaran_detail
        const val HEKTARAN = "hektaran"
        const val HEKTARAN_DETAIL = "hektaran_detail"

        const val ABSENSI_DETAIL = "absensi_detail"
    }

    object ListFeatureByRoleUser {
        const val Manager = "Manager"
        const val GM = "GM"
        const val ASKEP = "ASKEP"
        const val MandorPanen = "Mandor Panen"
        const val KeraniTimbang = "Kerani Timbang"
        const val Asisten = "Asisten"
        const val Mandor1 = "Mandor 1"
        const val KeraniPanen = "Kerani Panen"
        const val IT = "IT"
    }


    object ListFeatureNames {
        const val PanenTBS = "Panen TBS"
        const val RekapHasilPanen = "Rekap Hasil Panen"
        const val AsistensiEstateLain = "Asistensi Estate Lain"
        const val ScanHasilPanen = "Scan Hasil Panen"
        const val RekapPanenDanRestan = "Rekap panen dan restan"
        const val BuatESPB = "Buat eSPB"
        const val RekapESPB = "Rekap eSPB"
        const val DetailESPB = "Detail eSPB"

        const val InspeksiPanen = "Inspeksi Panen"
        const val RekapInspeksiPanen = "Rekap Inspeksi Panen"
        const val FollowUpInspeksi = "Follow Up Inspeksi"
        const val ScanESPBTimbanganMill = "Scan e-SPB Timbangan Mill"
        const val RekapESPBTimbanganMill = "Rekap e-SPB Timbangan Mill"
        const val AbsensiPanen = "Absensi panen"
        const val RekapAbsensiPanen = "Rekap Absensi Panen"
        const val ScanAbsensiPanen = "Scan Absensi Panen"
        const val SinkronisasiData = "Sinkronisasi data"
        const val UnduhTPHAsistensi = "Unduh TPH Asistensi"
        const val UploadDataCMP = "Upload Data CMP"
        const val ListFollowUpInspeksi = "List Follow Up Inspeksi"

        const val ScanPanenMPanen = "Scan Mandor Panen"
        const val DaftarHektarPanen = "Daftar Hektar Panen"
        const val TransferHektarPanen = "Transfer Hektar Panen"
        const val MutuBuah = "Inspeksi Mutu Buah"
        const val RekapMutuBuah = "Rekap Mutu Buah"
        const val TransferInspeksiPanen = "Transfer Inspeksi Panen"

        const val ScanTransferInspeksiPanen = "Scan Transfer Inspeksi Panen"
    }

    object ExemptFeatures {
        val ALWAYS_ENABLED = listOf(
            ListFeatureNames.UploadDataCMP,
            ListFeatureNames.RekapHasilPanen,
            ListFeatureNames.RekapPanenDanRestan,
            ListFeatureNames.DaftarHektarPanen,
            ListFeatureNames.RekapESPB,
            ListFeatureNames.RekapInspeksiPanen,
            ListFeatureNames.RekapESPBTimbanganMill,
            ListFeatureNames.RekapAbsensiPanen,
            ListFeatureNames.ListFollowUpInspeksi,
            ListFeatureNames.SinkronisasiData
        )
    }

    // Global variable to track if features should be disabled
    var isAppUpdateRequired: Boolean = false

    // Check if a feature should be disabled
    fun isFeatureDisabled(featureName: String): Boolean {
        return isAppUpdateRequired && !ExemptFeatures.ALWAYS_ENABLED.contains(featureName)
    }

    object WaterMarkFotoDanFolder {
        const val WMPanenTPH = "PANEN TPH"
        const val WMBuktiInspeksiUser = "INSPEKSI_BY_USER"
        const val WMBuktiFUInspeksiUser = "INSPEKSI_FU_BY_USER"
        const val WMInspeksiTPH = "INSPEKSI_TPH"
        const val WMInspeksiPokok = "INSPEKSI_POKOK"
        const val WMFUInspeksiTPH = "FU_INSPEKSI_TPH"
        const val WMFUInspeksiPokok = "FU_INSPEKSI_POKOK"
        const val WMAbsensiPanen = "ABSENSI PANEN"
        const val WMESPB = "E-SPB"
        const val WMTransferHektarPanen = "TRANSFER HEKTAR PANEN"
        const val WMRekapPanenDanRestan = "REKAP PANEN DAN RESTAN"
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
        const val parameter = "parameter"
        const val estate = "estate"
        const val jenisTPH = "jenis_tph"
        const val pemanen = "pemanen"
        const val kemandoran = "kemandoran"
        const val transporter = "transporter"
        const val kendaraan = "kendaraan"
        const val updateSyncLocalData = "Update & Sinkronisasi Lokal Data"
        const val sinkronisasiRestan = "Sinkronisasi Data Restan"
        const val sinkronisasiDataUser = "Sinkronisasi Data User"
        const val sinkronisasiDataPanen = "Data Panen (H+1 hingga H+7)"
        const val sinkronisasiFollowUpInspeksi = "Data Inspeksi (H+1 hingga H+7)"
        const val settingJSON = "setting.json"
        const val checkAppVersion = "Cek Versi Aplikasi"
    }

    object kodeInspeksi {
        const val brondolanDigawangan = "Brondolan dibuang ke gawangan"
        const val brondolanTidakDikutip =
            "Brondolan tidak dikutip bersih di piringan, psr pikul dan ketiak pokok"
        const val buahMasakTidakDipotong = "Buah masak tidak dipotong"
        const val buahTertinggalPiringan =
            "Buah tertinggal di piringan dan buah diperam digawangan mati"
        const val buahTinggalTPH = "Buah tinggal di TPH"
        const val brondolanTinggalTPH = "Brondolan tinggal di TPH"
        const val susunanPelepahTidakSesuai = "Susunan pelepah tidak sesuai"
        const val terdapatPelepahSengkleh = "Terdapat pelepah sengkleh"
        const val overPruning = "Overpruning"
        const val underPruning = "Underpruning"
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

    // Add this new method to reset the date
    fun resetSelectedDate() {
        _selectedDate = null
    }

    fun getAppVersion(context: Context): String {
        return context.getString(R.string.app_version)
    }

    private fun getCleanVersionNumber(version: String): String {
        // Use regex to extract just the numeric parts with dots
        val numericVersion = Regex("""(\d+(\.\d+)*)""").find(version)?.value ?: version

        // Remove dots
        return numericVersion.replace(".", "")
    }


    fun clearTempJsonFiles(context: Context) {
        try {
            val tempDir = File(context.getExternalFilesDir(null), "TEMP")

            if (tempDir.exists() && tempDir.isDirectory) {
                tempDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.extension == "json") {
                        val deleted = file.delete()
                        if (deleted) {
                            AppLogger.d("Deleted JSON file: ${file.name}")
                        } else {
                            AppLogger.e("Failed to delete: ${file.name}")
                        }
                    }
                }
                AppLogger.d("Temp folder cleanup completed")
            } else {
                AppLogger.d("Temp directory doesn't exist")
            }
        } catch (e: Exception) {
            AppLogger.e("Error cleaning temp folder: ${e.message}")
            e.printStackTrace()
        }
    }

    fun createTempJsonFile(
        context: Context,
        baseFilename: String,
        jsonData: String,
        userId: String,
        dataDate: String? = null
    ): Pair<String, String> {
        try {
            // Create a TEMP directory in external files storage
            val tempDir = File(context.getExternalFilesDir(null), "TEMP").apply {
                if (!exists()) mkdirs()
            }

            val appVersion = getCleanVersionNumber(getAppVersion(context))

            // Use provided date as is, or use current date if not available
            var dateForFilename = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

            // Try to parse and use the provided date if available
            if (!dataDate.isNullOrEmpty()) {
                try {
                    // Attempt to parse the data date - assuming format "yyyy-MM-dd HH:mm:ss"
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    val date = inputFormat.parse(dataDate)
                    if (date != null) {
                        dateForFilename = outputFormat.format(date)
                    }
                } catch (e: Exception) {
                    AppLogger.d("Could not parse data date: $dataDate, using current date instead")
                }
            }
            val currentTime = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())

            val filename = "v${appVersion}_${userId}_${currentTime}.json"
            val tempFile = File(tempDir, filename)

            // Parse the JSON data
            val gson = Gson()
            val dataArray = gson.fromJson(jsonData, JsonArray::class.java)

            // Create a wrapper object with the baseFilename as the key
            val wrapper = JsonObject()
            wrapper.add(baseFilename, dataArray)

            // Write the properly structured JSON data to the file
            FileOutputStream(tempFile).use { fos ->
                fos.write(gson.toJson(wrapper).toByteArray())
            }

            AppLogger.d("Created temp JSON file: ${tempFile.absolutePath}")
            return Pair(tempFile.absolutePath, filename)
        } catch (e: Exception) {
            AppLogger.e("Error creating temp JSON file: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }


    fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        }
    }

    private fun convertDataToJsonString(data: List<Map<String, Any>>): String {
        val gson = GsonBuilder()
            .setPrettyPrinting()  // Makes JSON readable (optional)
            .serializeNulls()     // Include null values (optional)
            .create()

        return gson.toJson(data)
    }

    fun createAndSaveZipUploadCMPSingle(
        context: Context,
        featureDataList: List<Pair<String, List<Map<String, Any>>>>,
        userId: String,
        onResult: (Boolean, String, String, File) -> Unit
    ) {
        try {
            val dateTime = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())

            val appFilesDir = File(context.getExternalFilesDir(null), "Upload").apply {
                if (!exists()) mkdirs()
            }

            val picturesDirs = listOf(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                File(context.getExternalFilesDir(null)?.parent ?: "", "Pictures")
            ).filterNotNull()

            val zipFileName = "${userId}_${dateTime}.zip"
            val zipFile = File(appFilesDir, zipFileName)

            val zip = ZipFile(zipFile)
            zip.setPassword(ZIP_PASSWORD.toCharArray())

            val zipParams = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.ZIP_STANDARD
            }

            featureDataList.forEach { (featureKey, dataList) ->
                val jsonString = convertDataToJsonString(dataList)  // Now uses Gson
                val jsonBytes = jsonString.toByteArray()

                val inputStream = ByteArrayInputStream(jsonBytes)
                zip.addStream(
                    inputStream,
                    zipParams.apply { fileNameInZip = "$featureKey/data.json" }
                )

                addFeaturePhotosToZip(context, dataList, featureKey, picturesDirs, zip, zipParams)
            }

            onResult(true, zipFile.name, zipFile.absolutePath, zipFile)

        } catch (e: Exception) {
            val errorMessage = "❌ Error creating encrypted ZIP file: ${e.message}"
            AppLogger.e(errorMessage)
            onResult(false, errorMessage, "", File(""))
        }
    }

    fun createAndSaveZipUpload(
        context: Context,
        jsonData: String,
        userId: String,
        featureType: String, // e.g., "hektaran", "absensi"
        photosList: List<Map<String, String>> = emptyList(), // Optional parameter for photos, default is empty
        onResult: (Boolean, String, String, File) -> Unit
    ) {
        try {
            val dateTime = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())

            val appFilesDir = File(context.getExternalFilesDir(null), "Upload").apply {
                if (!exists()) mkdirs()
            }

            // Dynamic filename based on feature type
            val zipFileName = "${userId}_${featureType}_${dateTime}.zip"
            val zipFile = File(appFilesDir, zipFileName)

            val zip = ZipFile(zipFile)
            zip.setPassword(ZIP_PASSWORD.toCharArray())

            val zipParams = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.ZIP_STANDARD
            }

            // Add json data with the complete nested structure
            // The featureType determines the folder name in the ZIP
            val dataInputStream = ByteArrayInputStream(jsonData.toByteArray())
            zip.addStream(
                dataInputStream,
                zipParams.apply { fileNameInZip = "${featureType}/data.json" }
            )

            // If photo list is provided, add all photos to the zip
            if (photosList.isNotEmpty()) {
                AppLogger.d("Adding ${photosList.size} photos to ${featureType} zip")

                for (photoData in photosList) {
                    val photoPath = photoData["path"] ?: continue
                    val photoName = photoData["name"] ?: continue

                    val photoFile = File(photoPath)
                    if (photoFile.exists() && photoFile.isFile) {
                        try {
                            zipParams.fileNameInZip = "${featureType}/photos/$photoName"
                            zip.addFile(photoFile, zipParams)
                            AppLogger.d("Added photo to ${featureType} zip: $photoName")
                        } catch (e: Exception) {
                            AppLogger.e("Failed to add photo to ${featureType} zip: $photoName - ${e.message}")
                        }
                    } else {
                        AppLogger.w("Photo file not found: $photoPath")
                    }
                }
            }

            // Return the result using the callback
            onResult(true, zipFile.name, zipFile.absolutePath, zipFile)

        } catch (e: Exception) {
            val errorMessage = "❌ Error creating encrypted ZIP file: ${e.message}"
            AppLogger.e(errorMessage)
            e.printStackTrace()
            onResult(false, errorMessage, "", File(""))
        }
    }

    private fun addFeaturePhotosToZip(
        context: Context,
        dataList: List<Map<String, Any>>,
        featureKey: String,
        picturesDirs: List<File>,
        zip: ZipFile,
        zipParams: ZipParameters
    ) {
        val allCmpDirectories = findAllCmpDirectories(picturesDirs)

        if (allCmpDirectories.isEmpty()) {
            AppLogger.w("No CMP directories found in any Pictures location")
            return
        }

        dataList.forEach { data ->
            when {
                featureKey.lowercase().contains("inspeksi") -> {
                    // Handle inspeksi photos with proper folder organization
                    processInspeksiPhotos(data, allCmpDirectories, zip, zipParams, context)
                }

                featureKey.lowercase().contains("mutu_buah") -> {
                    // Handle MutuBuah photos with both foto and foto_selfie
                    processMutuBuahPhotos(data, allCmpDirectories, zip, zipParams, context)
                }

                else -> {
                    // Handle other features normally (PANEN, ESPB, etc.)
                    processRegularPhotos(
                        data,
                        featureKey,
                        allCmpDirectories,
                        zip,
                        zipParams,
                        context
                    )
                }
            }
        }
    }

    private fun processInspeksiPhotos(
        data: Map<String, Any>,
        allCmpDirectories: List<File>,
        zip: ZipFile,
        zipParams: ZipParameters,
        context: Context
    ) {
        AppLogger.d("Processing all inspeksi photos for id: ${data["id"]}")

        // Process main inspeksi selfie photos
        processInspeksiSelfiePhotos(data, allCmpDirectories, zip, zipParams, context)

        // Process inspeksi detail photos
        processInspeksiDetailPhotos(data, allCmpDirectories, zip, zipParams, context)
    }

    private fun processInspeksiSelfiePhotos(
        data: Map<String, Any>,
        allCmpDirectories: List<File>,
        zip: ZipFile,
        zipParams: ZipParameters,
        context: Context
    ) {
        // Handle main header selfie photos (foto) - these go to INSPEKSI_BY_USER folder
        val foto = data["foto_user"] as? String

        if (!foto.isNullOrBlank()) {
            val photoPaths = foto.split(";")

            photoPaths.forEach { photoPath ->
                if (photoPath.isNotBlank()) {
                    val photoFileName = photoPath.trim().substringAfterLast("/")

                    addPhotoToZip(
                        photoPath.trim(),
                        photoFileName,
                        "inspeksi/photos/${AppUtils.WaterMarkFotoDanFolder.WMBuktiInspeksiUser}",
                        allCmpDirectories,
                        zip,
                        zipParams,
                        context
                    )
                }
            }
        }

        // Handle main header follow-up selfie photos (foto_pemulihan) - also go to INSPEKSI_BY_USER folder
        val fotoPemulihan = data["foto_user_pemulihan"] as? String

        if (!fotoPemulihan.isNullOrBlank()) {
            val photoPaths = fotoPemulihan.split(";")

            photoPaths.forEach { photoPath ->
                if (photoPath.isNotBlank()) {
                    val photoFileName = photoPath.trim().substringAfterLast("/")

                    addPhotoToZip(
                        photoPath.trim(),
                        photoFileName,
                        "inspeksi/photos/${AppUtils.WaterMarkFotoDanFolder.WMBuktiFUInspeksiUser}",
                        allCmpDirectories,
                        zip,
                        zipParams,
                        context
                    )
                }
            }
        }
    }

    private fun processInspeksiDetailPhotos(
        data: Map<String, Any>,
        allCmpDirectories: List<File>,
        zip: ZipFile,
        zipParams: ZipParameters,
        context: Context
    ) {
        val inspeksiDetailList = data["inspeksi_detail"] as? List<Map<String, Any>>

        inspeksiDetailList?.forEach { detailData ->
            // Process regular detail photos
            val detailPhotoPathString = detailData["foto"] as? String
            val detailFollowUpPhotoString = detailData["foto_pemulihan"] as? String
            val noPokok = detailData["no_pokok"] as? Int ?: 0

            // Process regular detail photos
            if (!detailPhotoPathString.isNullOrBlank()) {
                val detailPhotoPaths = detailPhotoPathString.split(";")

                detailPhotoPaths.forEach { photoPath ->
                    if (photoPath.isNotBlank()) {
                        val photoFileName = photoPath.trim().substringAfterLast("/")

                        // Determine folder based on no_pokok and photo content
                        val targetFolder = if (noPokok == 0) {
                            AppUtils.WaterMarkFotoDanFolder.WMInspeksiTPH
                        } else {
                            AppUtils.WaterMarkFotoDanFolder.WMInspeksiPokok
                        }

                        addPhotoToZip(
                            photoPath.trim(),
                            photoFileName,
                            "inspeksi/photos/$targetFolder",
                            allCmpDirectories,
                            zip,
                            zipParams,
                            context
                        )
                    }
                }
            }

            // Process follow-up/recovery photos
            if (!detailFollowUpPhotoString.isNullOrBlank()) {
                val followUpPhotoPaths = detailFollowUpPhotoString.split(";")

                followUpPhotoPaths.forEach { photoPath ->
                    if (photoPath.isNotBlank()) {
                        val photoFileName = photoPath.trim().substringAfterLast("/")

                        // Determine folder based on no_pokok for follow-up photos
                        val targetFolder = if (noPokok == 0) {
                            AppUtils.WaterMarkFotoDanFolder.WMFUInspeksiTPH
                        } else {
                            AppUtils.WaterMarkFotoDanFolder.WMFUInspeksiPokok
                        }

                        addPhotoToZip(
                            photoPath.trim(),
                            photoFileName,
                            "inspeksi/photos/$targetFolder",
                            allCmpDirectories,
                            zip,
                            zipParams,
                            context
                        )
                    }
                }
            }
        }
    }

    private fun findAllCmpDirectories(picturesDirs: List<File>): List<File> {
        val allCmpDirectories = mutableListOf<File>()

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

            // Also check in the main Pictures directory itself
            val mainPicsDir = File(picturesDir.parentFile, "Pictures")
            if (mainPicsDir.exists() && mainPicsDir.isDirectory) {
                val additionalCmpDirectories = mainPicsDir.listFiles { file ->
                    file.isDirectory && file.name.startsWith("CMP")
                } ?: emptyArray()

                AppLogger.d("Looking for CMP directories in: ${mainPicsDir.absolutePath}")
                AppLogger.d("Found ${additionalCmpDirectories.size} CMP directories: ${additionalCmpDirectories.map { it.name }}")

                allCmpDirectories.addAll(additionalCmpDirectories)
            }
        }

        return allCmpDirectories
    }

    private fun processInspeksiMainPhotos(
        data: Map<String, Any>,
        allCmpDirectories: List<File>,
        zip: ZipFile,
        zipParams: ZipParameters,
        context: Context
    ) {
        val photoPathString = data["foto"] as? String
        AppLogger.d("Processing main inspeksi photos for id: ${data["id"]}")

        if (!photoPathString.isNullOrBlank()) {
            val photoPaths = photoPathString.split(";")

            photoPaths.forEach { photoPath ->
                if (photoPath.isNotBlank()) {
                    val photoFileName = photoPath.trim().substringAfterLast("/")
                    addPhotoToZip(
                        photoPath.trim(),
                        photoFileName,
                        "inspeksi/photos/inspeksi", // Main inspeksi photos folder
                        allCmpDirectories,
                        zip,
                        zipParams,
                        context
                    )
                }
            }
        }
    }


    private fun processRegularPhotos(
        data: Map<String, Any>,
        featureKey: String,
        allCmpDirectories: List<File>,
        zip: ZipFile,
        zipParams: ZipParameters,
        context: Context
    ) {
        val photoPathString = data["foto"] as? String
        AppLogger.d("Processing regular photos for feature: $featureKey, id: ${data["id"]}")

        if (!photoPathString.isNullOrBlank()) {
            val photoPaths = photoPathString.split(";")

            photoPaths.forEach { photoPath ->
                if (photoPath.isNotBlank()) {
                    val photoFileName = photoPath.trim().substringAfterLast("/")
                    addPhotoToZip(
                        photoPath.trim(),
                        photoFileName,
                        "$featureKey/photos", // Regular feature photos folder
                        allCmpDirectories,
                        zip,
                        zipParams,
                        context
                    )
                }
            }
        }
    }

    private fun addPhotoToZip(
        photoPath: String,
        photoFileName: String,
        targetFolder: String,
        allCmpDirectories: List<File>,
        zip: ZipFile,
        zipParams: ZipParameters,
        context: Context
    ) {
        var photoFound = false

        // First try: Look for the exact file
        val photoFile = File(photoPath)

        if (photoFile.exists() && photoFile.isFile) {
            try {
                val targetPath = "$targetFolder/$photoFileName"
                zipParams.fileNameInZip = targetPath
                zip.addFile(photoFile, zipParams)
                photoFound = true
                AppLogger.d("Added photo to zip (direct path): ${photoFile.absolutePath} -> $targetPath")
            } catch (e: Exception) {
                AppLogger.e("Failed to add photo to zip: ${photoFile.absolutePath}, Error: ${e.message}")
            }
        } else {
            // Find photo by searching in CMP directories
            val foundFile = findPhotoFile(allCmpDirectories, photoPath, photoFileName)

            if (foundFile != null) {
                try {
                    val targetPath = "$targetFolder/${foundFile.name}"
                    zipParams.fileNameInZip = targetPath
                    zip.addFile(foundFile, zipParams)
                    photoFound = true
                    AppLogger.d("Added photo to zip (from search): ${foundFile.absolutePath} -> $targetPath")
                } catch (e: Exception) {
                    AppLogger.e("Failed to add photo to zip: ${foundFile.absolutePath}, Error: ${e.message}")
                }
            }
        }

        if (!photoFound) {
            AppLogger.w("Photo not found in any directory: $photoPath")

            // One last attempt - check if it's a relative path
            val relativePathFile = File(context.getExternalFilesDir(null), photoPath)
            if (relativePathFile.exists() && relativePathFile.isFile) {
                try {
                    val targetPath = "$targetFolder/$photoFileName"
                    zipParams.fileNameInZip = targetPath
                    zip.addFile(relativePathFile, zipParams)
                    AppLogger.d("Added photo to zip (relative path): ${relativePathFile.absolutePath} -> $targetPath")
                } catch (e: Exception) {
                    AppLogger.e("Failed to add photo to zip: ${relativePathFile.absolutePath}, Error: ${e.message}")
                }
            }
        }
    }

    private fun processMutuBuahPhotos(
        data: Map<String, Any>,
        allCmpDirectories: List<File>,
        zip: ZipFile,
        zipParams: ZipParameters,
        context: Context
    ) {
        AppLogger.d("Processing MutuBuah photos for id: ${data["id"]}")

        // Process regular photos (foto)
        val photoPathString = data["foto"] as? String
        if (!photoPathString.isNullOrBlank()) {
            val photoPaths = photoPathString.split(";")

            photoPaths.forEach { photoPath ->
                if (photoPath.isNotBlank()) {
                    val photoFileName = photoPath.trim().substringAfterLast("/")
                    addPhotoToZip(
                        photoPath.trim(),
                        photoFileName,
                        "mutu_buah/photos/regular", // Regular photos folder
                        allCmpDirectories,
                        zip,
                        zipParams,
                        context
                    )
                }
            }
        }

        // Process selfie photos (foto_selfie)
        val selfiePathString = data["foto_selfie"] as? String
        if (!selfiePathString.isNullOrBlank()) {
            val selfiePaths = selfiePathString.split(";")

            selfiePaths.forEach { selfiePath ->
                if (selfiePath.isNotBlank()) {
                    val selfieFileName = selfiePath.trim().substringAfterLast("/")
                    addPhotoToZip(
                        selfiePath.trim(),
                        selfieFileName,
                        "mutu_buah/photos/selfie", // Selfie photos folder
                        allCmpDirectories,
                        zip,
                        zipParams,
                        context
                    )
                }
            }
        }
    }

    // You'll need this helper function (if not already exists)
    private fun findPhotoFile(
        cmpDirectories: List<File>,
        photoPath: String,
        photoFileName: String
    ): File? {
        for (cmpDir in cmpDirectories) {
            val photoFile = File(cmpDir, photoFileName)
            if (photoFile.exists() && photoFile.isFile) {
                return photoFile
            }
        }
        return null
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
            "Silakan aktifkan tanggal dan waktu otomatis untuk pengalaman aplikasi yang lebih baik."
        } else {
            "Silakan aktifkan tanggal dan waktu otomatis serta sambungkan ke internet untuk memastikan sinkronisasi waktu yang benar."
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

    fun compareVersions(currentVersion: String, requiredVersion: String): Int {
        // Remove .debug suffix and split by dots
        val current = currentVersion.replace(".debug", "").split(".").map { it.toIntOrNull() ?: 0 }
        val required = requiredVersion.split(".").map { it.toIntOrNull() ?: 0 }

        // Pad shorter version with zeros
        val maxLength = maxOf(current.size, required.size)
        val currentPadded = current + List(maxLength - current.size) { 0 }
        val requiredPadded = required + List(maxLength - required.size) { 0 }

        // Compare each part
        for (i in 0 until maxLength) {
            when {
                currentPadded[i] < requiredPadded[i] -> return -1 // Current is older
                currentPadded[i] > requiredPadded[i] -> return 1  // Current is newer
            }
        }
        return 0 // Versions are equal
    }

    /**
     * Checks if app version update is required
     * @param context Context to get device info
     * @param prefManager PrefManager to get latest system version
     * @return true if update is required, false otherwise
     */
    fun checkAppVersionUpdate(context: Context, prefManager: PrefManager): Boolean {
        val deviceInfo = getDeviceInfo(context)
        val currentAppVersion = deviceInfo.optString("app_version", "")
        val requiredVersion = prefManager.latestAppVersionSystem ?: ""

        AppLogger.d("Current app version: $currentAppVersion")
        AppLogger.d("Required app version: $requiredVersion")

        if (requiredVersion.isNotEmpty() && currentAppVersion.isNotEmpty()) {
            val comparison = compareVersions(currentAppVersion, requiredVersion)

            when {
                comparison < 0 -> {
                    AppLogger.w("App version is outdated. Current: $currentAppVersion, Required: $requiredVersion")
                    return true // Update required
                }

                comparison == 0 -> {
                    AppLogger.d("App version is up to date")
                    return false
                }

                comparison > 0 -> {
                    AppLogger.d("App version is newer than required")
                    return false
                }
            }
        } else {
            AppLogger.w("Cannot compare versions - missing data")
        }
        return false
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
            if (word.length <= 3) word.uppercase() else word.lowercase()
                .replaceFirstChar { it.uppercase() }
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
            prefManager!!.jabatanUserLogin?.takeIf { it.isNotEmpty() }?.let {
                if (length > 0) append(" - ")
                append(it)
            }
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
                    val outputFormat = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID"))
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

            val base64Decode = base64Data.replace(AppUtils.half_json_encrypted, "")

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

            AppLogger.d("e $e")
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

    fun showWithAnimation(view: View) {
        view.apply {
            if (visibility != View.VISIBLE) {
                visibility = View.VISIBLE
                translationY = 100f
                alpha = 0f
                animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }
    }

    fun hideWithAnimation(view: View, delay: Long = 300) {
        view.apply {
            if (visibility == View.VISIBLE) {
                animate()
                    .translationY(100f)
                    .alpha(0f)
                    .setDuration(delay)
                    .setInterpolator(AccelerateInterpolator())
                    .withEndAction { visibility = View.GONE }
                    .start()
            }
        }
    }

    fun getDistinctBlokNames(mappedData: List<Map<String, Any?>>): String {
        return mappedData
            .map { it["blok_name"].toString() }
            .distinct()
            .filter { it != "-" }
            .sorted()
            .joinToString(", ")
    }

    /**
     * Get blok display information based on featureName
     */

    fun getBlokDisplay(mappedData: List<Map<String, Any?>>, featureName: String?): String {
        return if (featureName == ListFeatureNames.RekapHasilPanen ||
            featureName == ListFeatureNames.RekapPanenDanRestan ||
            featureName == ListFeatureNames.DetailESPB ||
            featureName == ListFeatureNames.TransferHektarPanen ||
            featureName == ListFeatureNames.TransferInspeksiPanen
        ) {

            if (featureName == ListFeatureNames.TransferInspeksiPanen) {
                // For TransferInspeksiPanen, just show blok names without sum/count
                mappedData
                    .filter { it["blok_name"].toString() != "-" }
                    .map { it["blok_name"].toString() }
                    .distinct()
                    .sorted()
                    .joinToString(", ")
            } else {
                // For other features, show with sum and count
                val fieldToExtract =
                    if (featureName == ListFeatureNames.TransferHektarPanen) "PA" else "KP"

                mappedData
                    .filter { it["blok_name"].toString() != "-" }
                    .groupBy { it["blok_name"].toString() }
                    .mapValues { (_, items) ->
                        val count = items.size
                        val paSum = items.sumOf { item ->
                            extractJSONValue(item["jjg_json"].toString(), fieldToExtract)
                        }
                        "${paSum.toInt()}/$count"  // Convert double sum to integer for display
                    }
                    .toSortedMap() // Sort by blok_name
                    .map { (blokName, summary) -> "$blokName ($summary)" }
                    .joinToString(", ")
            }
        } else {
            getDistinctBlokNames(mappedData)
        }
    }

    /**
     * Calculate total JJG count
     */
    fun calculateTotalJjgCount(mappedData: List<Map<String, Any?>>, featureName: String?): Int {
        var totalJjgCount = 0
        mappedData.forEach { data ->
            try {
                val jjgJsonString = data["jjg_json"].toString()
                val jjgJson = JSONObject(jjgJsonString)
                val key = if (featureName == ListFeatureNames.TransferHektarPanen) "PA" else "KP"

                totalJjgCount += jjgJson.optInt(key, 0)
            } catch (e: Exception) {
                AppLogger.e("Error parsing jjg_json: ${e.message}")
            }
        }
        return totalJjgCount
    }

    /**
     * Get TPH count
     */
    fun getTphCount(mappedData: List<Map<String, Any?>>): Int {
        return mappedData
            .mapNotNull { it["tph_id"].toString().toIntOrNull() }
            .count()
    }

    /**
     * Extract a numeric value from a JSON string
     */
    private fun extractJSONValue(jsonString: String, key: String): Double {
        return try {
            val jsonObject = JSONObject(jsonString)
            jsonObject.optDouble(key, 0.0)
        } catch (e: Exception) {
            AppLogger.e("Error extracting JSON value: ${e.message}")
            0.0
        }
    }

    /**
     * Get a map of all calculated data for easy access
     */
    fun getPanenProcessedData(
        mappedData: List<Map<String, Any?>>,
        featureName: String?
    ): Map<String, Any> {
        val totalJjgCount = if (featureName == AppUtils.ListFeatureNames.RekapMutuBuah) {
            // For Mutu Buah, use jjg_panen instead of jjg_kirim
            mappedData.sumOf { data ->
                val jjgPanen = data["jjg_panen"] as? Int ?: 0
                jjgPanen
            }
        } else {
            // For other features, use the existing logic with jjg_json
            mappedData.sumOf { data ->
                try {
                    val jjgJsonStr = data["jjg_json"]?.toString() ?: "{}"
                    val jjgJson = JSONObject(jjgJsonStr)

                    when (featureName) {
                        AppUtils.ListFeatureNames.RekapPanenDanRestan -> jjgJson.optInt("KP", 0)
                        else -> jjgJson.optInt("KP", 0) // Default to KP for other features
                    }
                } catch (e: Exception) {
                    0
                }
            }
        }
        return mapOf(
            "blokNames" to getDistinctBlokNames(mappedData),
            "blokDisplay" to getBlokDisplay(mappedData, featureName),
            "totalJjgCount" to calculateTotalJjgCount(mappedData, featureName),
            "tphCount" to getTphCount(mappedData),
            "dataCount" to mappedData.size
        )
    }

}