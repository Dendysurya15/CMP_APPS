package com.cbi.cmp_project.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Base64
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.network.RetrofitClient
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import com.jaredrummler.materialspinner.BuildConfig
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object AppUtils {

    const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
    const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
        UPDATE_INTERVAL_IN_MILLISECONDS / 2
    const val LOG_LOC = "locationLog"

    const val REQUEST_CHECK_SETTINGS = 0x1


    /**
     * Gets the current app version from BuildConfig or string resources.
     * @param context The context used to retrieve the string resource.
     * @return The app version as a string.
     */
    fun getAppVersion(context: Context): String {
        return context.getString(R.string.app_version)
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

    fun Context.vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    fun formatToIndonesianDate(dateString: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEEE, d MMMM YYYY 'Pukul' HH:mm:ss", Locale("id", "ID"))

        return try {
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            "Format tanggal tidak valid"
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

    @SuppressLint("SetTextI18n")
    fun setupUserHeader(
        userName: String?,
        jabatanUser: String?,
        estateName: String?,
        afdelingUser: String?,
        userSection: TextView,
        featureName: String?,
        tvFeatureName: TextView
    ) {
        val userInfo = buildString {
            // Append userName if it's not null or empty
            userName?.takeIf { it.isNotEmpty() }?.let { append(it) }

            // Append jabatanUser if it's not null or empty
            jabatanUser?.takeIf { it.isNotEmpty() }?.let {
                if (isNotEmpty()) append("\n") // Add \n only if previous content exists
                append(it)
            }

            // Append estateName and afdelingUser if not both are null or empty
            if (!estateName.isNullOrEmpty() || !afdelingUser.isNullOrEmpty()) {
                if (isNotEmpty()) append("\n") // Add \n only if previous content exists
                estateName?.takeIf { it.isNotEmpty() }?.let {
                    append(it)
                    if (!afdelingUser.isNullOrEmpty()) append(" - ")
                }
                afdelingUser?.takeIf { it.isNotEmpty() }?.let {
                    append(it)
                }
            }
        }

        userSection.text = userInfo
        AppUtils.setupFeatureHeader(featureName, tvFeatureName)
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
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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