package com.cbi.cmp_project.ui.view

import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.network.RetrofitClient
import com.cbi.cmp_project.databinding.ActivityHomePageBinding
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppUtils.stringXML
import com.cbi.cmp_project.utils.LoadingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class HomePageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomePageBinding
    private lateinit var loadingDialog: LoadingDialog
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,)

    private val permissionRequestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadingDialog = LoadingDialog(this)
        // Check and request permissions
        checkPermissions()

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_home_page)

        navView.setupWithNavController(navController)
    }


    private suspend fun downloadFile() {
        // Show loading dialog
        runOnUiThread { loadingDialog.show() }

        val progressJob = lifecycleScope.launch(Dispatchers.Main) {
            var dots = 1
            while (true) {
                loadingDialog.setMessage("${stringXML(R.string.fetching_data)}${".".repeat(dots)}")
                dots = if (dots >= 3) 1 else dots + 1
                delay(500) // Update every 500ms
            }
        }

        // Check for internet connection
        val hasInternet = withContext(Dispatchers.IO) { isInternetAvailable() }
        if (!hasInternet) {
            runOnUiThread {
                AlertDialogUtility.withSingleAction(
                    this,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_no_internet_connection),
                    stringXML(R.string.al_no_internet_connection_description_download_dataset),
                    "network_error.json",
                    R.color.colorRedDark
                ) {}
            }

            runOnUiThread { loadingDialog.dismiss()
                }

            runOnUiThread{
                progressJob.cancel()
            }
            return
        }

        try {
            // Perform file download
            val response: Response<ResponseBody> = withContext(Dispatchers.IO) {
                RetrofitClient.instance.downloadDataset()
            }

            if (response.isSuccessful) {
                val responseBody = response.body()

                if (responseBody == null) {
                    Log.e("FileDownload", "Response body is null")
                    runOnUiThread {
                        Toast.makeText(this, "No content in response", Toast.LENGTH_LONG).show()
                    }
                    runOnUiThread { loadingDialog.dismiss()
                        }
                    runOnUiThread{
                        progressJob.cancel()
                    }
                    return
                }

                val file = withContext(Dispatchers.IO) { saveFileToStorage(responseBody) }

                if (file != null) {
                    Log.d("FileDownload", "File downloaded successfully: ${file.absolutePath}")

                    runOnUiThread {
                        AlertDialogUtility.alertDialogAction(
                            this,
                            stringXML(R.string.al_success_download_dataset),
                            stringXML(R.string.al_success_download_dataset_description),
                            "success.json"
                        ) {
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Error saving file", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Log.e("FileDownload", "Error downloading file: ${response.message()}")
                runOnUiThread {
                    Toast.makeText(this, "Error downloading file", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e("FileDownload", "Exception during file download: ${e.message}")
            runOnUiThread {
                AlertDialogUtility.withSingleAction(
                    this,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_no_internet_connection),
                    "Download failed: ${e.message}",
                    "network_error.json",
                    R.color.colorRedDark
                ) {}
                Toast.makeText(this, "Unduh dataset gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } finally {
            // Hide loading dialog
            runOnUiThread { loadingDialog.dismiss()
                }

            runOnUiThread{
                progressJob.cancel()
            }
        }
    }

    /**
     * Checks for internet availability.
     */
    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        // Check internet capability and perform ping
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && pingGoogle()
    }

    /**
     * Pings Google to verify internet connectivity.
     */
    private fun pingGoogle(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 www.google.com")
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            Log.e("PingGoogle", "Ping failed: ${e.message}")
            false
        }
    }



    private fun saveFileToStorage(body: ResponseBody): File? {
        val downloadsDir = this.getExternalFilesDir(null)
        val file = File(downloadsDir, "dataset_tph.txt") // Save as txt file

        return try {
            // Read the file stream in chunks
            val inputStream: InputStream = body.byteStream()
            val outputStream: OutputStream = FileOutputStream(file)

            val buffer = ByteArray(1024 * 8)  // Use a larger buffer for better performance
            var bytesRead: Int
            var totalBytesRead = 0L
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            Log.d("FileDownload", "File saved: ${file.absolutePath}, Size: $totalBytesRead bytes")
            file
        } catch (e: IOException) {
            Log.e("FileDownload", "Error saving file: ${e.message}")
            null
        }
    }


    // Check if the required permissions are granted
    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                permissionRequestCode
            )
        }else{
            startFileDownload()
        }
    }

    private fun startFileDownload() {

        val downloadsDir = this.getExternalFilesDir(null)
        val file = File(downloadsDir, "dataset_tph.txt") // Save as txt file

        // Check if the file already exists
        if (!file.exists()) {
            lifecycleScope.launch {
                downloadFile()
            }
        }

    }


    // Handle the result of permission request
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == permissionRequestCode) {
            val deniedPermissions = mutableListOf<String>()
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i])
                }
            }

            if (deniedPermissions.isNotEmpty()) {
                Toast.makeText(
                    this,
                    "The following permissions are required: ${deniedPermissions.joinToString()}",
                    Toast.LENGTH_LONG
                ).show()
            }else {
                // Permissions granted, proceed to download file
                startFileDownload()
            }
        }
    }
}