package com.cbi.cmp_project.ui.view

import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.network.RetrofitClient
import com.cbi.cmp_project.databinding.ActivityHomePageBinding
import com.cbi.cmp_project.ui.adapter.ProgressUploadAdapter
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppUtils.stringXML
import com.cbi.cmp_project.utils.LoadingDialog
import com.cbi.cmp_project.utils.PrefManager
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
    private var prefManager: PrefManager? = null
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,)

    private val permissionRequestCode = 1001

    object ApiCallManager {
        val apiCallList = listOf(
            Pair("datasetCompanyCode.zip", RetrofitClient.instance::downloadDatasetCompany),
            Pair("datasetBUnitCode.zip", RetrofitClient.instance::downloadDatasetBUnit),
            Pair("datasetDivisionCode.zip", RetrofitClient.instance::downloadDatasetDivision),
            Pair("datasetTPHCode.zip", RetrofitClient.instance::downloadDatasetTPH),
            Pair("datasetFieldCode.zip", RetrofitClient.instance::downloadDatasetField),
            Pair("datasetWorkerInGroup.zip", RetrofitClient.instance::downloadDatasetWorkerInGroup),
            Pair("datasetWorkerGroup.zip", RetrofitClient.instance::downloadDatasetWorkerGroup),
            Pair("datasetWorker.zip", RetrofitClient.instance::downloadDatasetWorker)
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefManager = PrefManager(this)
        loadingDialog = LoadingDialog(this)
        // Check and request permissions
        checkPermissions()

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_home_page)

        navView.setupWithNavController(navController)
    }

    private suspend fun downloadAndStoreFiles(apiCalls: List<Pair<String, suspend () -> Response<ResponseBody>>>) {
        val downloadsDir = this.getExternalFilesDir(null)
        val fileList = mutableListOf<String?>()

        for ((fileName, apiCall) in apiCalls) { // Include both fileName and apiCall
            val isSuccessful = downloadFile(fileName, apiCall, downloadsDir, fileList) // Pass fileName
            if (!isSuccessful) {
                Log.e("FileDownload", "File download failed for $fileName. Moving to next file.")
            }
        }

        // Save the file list in PrefManager
        prefManager!!.saveFileList(fileList)

        // Log the saved file list
        val savedFileList = prefManager!!.getFileList()
        Log.d("FileList", "Downloaded files: $savedFileList")
    }

        private fun startFileDownload() {
            lifecycleScope.launch {
//                runOnUiThread { loadingDialog.show() }
//
//                val progressJob = lifecycleScope.launch(Dispatchers.Main) {
//                    var dots = 1
//                    while (true) {
//                        loadingDialog.setMessage("${stringXML(R.string.download_dataset)}${".".repeat(dots)}")
//                        dots = if (dots >= 3) 1 else dots + 1
//                        delay(500) // Update every 500ms
//                    }
//                }

                val dialogView = layoutInflater.inflate(R.layout.list_card_upload, null)
                val alertDialog = AlertDialog.Builder(this@HomePageActivity)
                    .setCancelable(false)
                    .setView(dialogView)
                    .create()


                alertDialog.show()
                alertDialog.window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                val recyclerView = alertDialog.findViewById<RecyclerView>(R.id.features_recycler_view)
                recyclerView?.layoutManager = LinearLayoutManager(this@HomePageActivity, LinearLayoutManager.VERTICAL, false)
                recyclerView?.adapter = ProgressUploadAdapter()


                val titleTextView = dialogView.findViewById<TextView>(R.id.tvTitleProgressBarLayout)
                val counterTextView = dialogView.findViewById<TextView>(R.id.counter_dataset)

                val apiCallsSize = ApiCallManager.apiCallList.size

                counterTextView.text = "0 / $apiCallsSize"

                lifecycleScope.launch(Dispatchers.Main) {
                    var dots = 0
                    while (alertDialog.isShowing) {
                        titleTextView.text = "Mengunduh Dataset" + ".".repeat(dots)
                        dots = if (dots >= 4) 1 else dots + 1
                        delay(500) // Update every 500ms
                    }
                }

//                val hasInternet = withContext(Dispatchers.IO) { isInternetAvailable() }
//                if (!hasInternet) {
//                    runOnUiThread {
//                        AlertDialogUtility.withSingleAction(
//                            this@HomePageActivity,
//                            stringXML(R.string.al_back),
//                            stringXML(R.string.al_no_internet_connection),
//                            stringXML(R.string.al_no_internet_connection_description_download_dataset),
//                            "network_error.json",
//                            R.color.colorRedDark
//                        ) {}
//                    }
////                    progressJob.cancel()
////                    loadingDialog.dismiss()
//                    return@launch
//                }
//
//                if (prefManager!!.isFirstTimeLaunch) {
//                    prefManager!!.isFirstTimeLaunch = false
//                }
//
//                // Pass the global list with both file names and API calls
//                val apiCalls = ApiCallManager.apiCallList
//
//                downloadAndStoreFiles(apiCalls)
//                alertDialog.dismiss()
//                progressJob.cancel()
//                loadingDialog.dismiss()
            }
        }



    private suspend fun downloadFile(
        fileName: String, // Use file name from the global variable
        apiCall: suspend () -> Response<ResponseBody>,
        downloadsDir: File?,
        fileList: MutableList<String?>
    ): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val response = apiCall()
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        val file = File(downloadsDir, fileName) // Use the file name directly
                        saveFileToStorage(responseBody, file)
                        fileList.add(fileName) // Add the file name to the list
                        Log.d("FileDownload", "$fileName downloaded successfully.")
                        true
                    } else {
                        fileList.add(null) // Add null for failed download
                        Log.e("FileDownload", "Response body is null.")
                        false
                    }
                } else {
                    fileList.add(null) // Add null for failed download
                    Log.e("FileDownload", "Download failed: ${response.message()}")
                    false
                }
            }
        } catch (e: Exception) {
            fileList.add(null) // Add null for exception
            Log.e("FileDownload", "Error downloading file: ${e.message}")
            false
        }
    }



    private fun saveFileToStorage(body: ResponseBody, file: File): Boolean {
        return try {
            body.byteStream().use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("FileDownload", "Error saving file: ${e.message}")
            false
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

    private fun shouldStartFileDownload(): Boolean {
        val savedFileList = prefManager!!.getFileList() // Retrieve the saved file list
        val downloadsDir = this.getExternalFilesDir(null) // Get the downloads directory

        // Check if it's the first time launch
        if (prefManager!!.isFirstTimeLaunch) {
            prefManager!!.isFirstTimeLaunch = false // Set it to false after the first launch
            return true // Need to start the file download
        }

        if (savedFileList.isNotEmpty()) {
            // Log the file list and download directory for debugging
            Log.d("FileCheck", "Saved file list: $savedFileList")
            Log.d("FileCheck", "Downloads directory: $downloadsDir")

            // Check if any entry is null
            if (savedFileList.contains(null)) {
                Log.e("FileCheck", "Null entries found in savedFileList.")
                return true // Restart download if any null exists
            }

            // Check if all files exist
            val missingFiles = savedFileList.filterNot { fileName ->
                val file = File(downloadsDir, fileName)
                val exists = file.exists()
                Log.d("FileCheck", "Checking if file exists: ${file.path} -> $exists")
                fileName != null && exists
            }

            if (missingFiles.isNotEmpty()) {
                Log.e("FileCheck", "Missing files detected: $missingFiles")
                return true // Restart download if any file is missing
            }
        }

        return false // No need to start the download
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
            if (shouldStartFileDownload()) {
                startFileDownload()
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
                if (shouldStartFileDownload()) {
                    startFileDownload()
                }
            }
        }
    }
}