package com.cbi.cmp_project.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Base64
import android.widget.TextView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.network.RetrofitClient
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import com.jaredrummler.materialspinner.BuildConfig
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object AppUtils {

    const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
    const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
        UPDATE_INTERVAL_IN_MILLISECONDS / 2
    const val LOG_LOC = "locationLog"

    const val REQUEST_CHECK_SETTINGS = 0x1

    object ApiCallManager {

        val apiCallList = listOf(
            Pair("datasetRegional.zip", RetrofitClient.instance::downloadDatasetRegionalJson),
            Pair("datasetWilayah.zip", RetrofitClient.instance::downloadDatasetWilayahJson),
            Pair("datasetDept.zip", RetrofitClient.instance::downloadDatasetDeptJson),
            Pair("datasetDivisi.zip", RetrofitClient.instance::downloadDatasetDivisiJson),
            Pair("datasetBlok.zip", RetrofitClient.instance::downloadDatasetBlokJson),
            Pair("datasetKaryawan.zip", RetrofitClient.instance::downloadDatasetKaryawanJson),
            Pair("datasetKemandoran.zip", RetrofitClient.instance::downloadDatasetKemandoranJson),
            Pair("datasetKemandoranDetail.zip", RetrofitClient.instance::downloadDatasetKemandoranDetailJson),
            Pair("datasetTPH.zip", RetrofitClient.instance::downloadDatasetTPHNewJson),
        )
    }
    /**
     * Gets the current app version from BuildConfig or string resources.
     * @param context The context used to retrieve the string resource.
     * @return The app version as a string.
     */
    fun getAppVersion(context: Context): String {
        return "Versi ${context.getString(R.string.app_version)}"
    }


    fun Context.stringXML(field: Int): String {
        return getString(field)
    }

    fun Context.vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    /**
     * Alternative method to fetch the version name directly from BuildConfig with a "V" prefix.
     * Use this if you don't want to rely on Gradle's `resValue`.
     */

    fun setupFeatureHeader(featureName: String?, tvFeatureName: TextView) {
        val context = tvFeatureName.context  // Get the context from the TextView
        val appVersion = getAppVersion(context)
        val headerText = "${featureName ?: "Default Feature Name"} ($appVersion)"
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
}