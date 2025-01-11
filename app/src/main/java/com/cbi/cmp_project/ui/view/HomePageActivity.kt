package com.cbi.cmp_project.ui.view

import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.view.View
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
import com.google.gson.Gson
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

    data class ErrorResponse(
        val statusCode: Int,
        val message: String,
        val error: String? = null
    )
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

//    private suspend fun downloadAndStoreFiles(apiCalls: List<Pair<String, suspend () -> Response<ResponseBody>>>) {
//        val downloadsDir = this.getExternalFilesDir(null)
//        val fileList = mutableListOf<String?>()
//
//        for ((fileName, apiCall) in apiCalls) { // Include both fileName and apiCall
//            val isSuccessful = downloadFile(fileName, apiCall, downloadsDir, fileList) // Pass fileName
//            if (!isSuccessful) {
//                Log.e("FileDownload", "File download failed for $fileName. Moving to next file.")
//            }
//        }
//
//        // Save the file list in PrefManager
//        prefManager!!.saveFileList(fileList)
//
//        // Log the saved file list
//        val savedFileList = prefManager!!.getFileList()
//        Log.d("FileList", "Downloaded files: $savedFileList")
//    }

    private fun startFileDownload() {
        lifecycleScope.launch {
            // Inflate dialog layout
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

            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.features_recycler_view)
            recyclerView?.layoutManager = LinearLayoutManager(this@HomePageActivity, LinearLayoutManager.VERTICAL, false)


            val apiCallsSize = ApiCallManager.apiCallList.size
            val progressList = MutableList(apiCallsSize) { 0 } // Initialize progress list with 0%
            val statusList = MutableList(apiCallsSize) { "Menunggu" } // Initialize with "Waiting" status
            val iconList = MutableList(apiCallsSize) { R.id.progress_circular_loading }
            val fileNames = ApiCallManager.apiCallList.map { it.first } // Get the file names from ApiCallManager

            val progressAdapter = ProgressUploadAdapter(progressList, statusList, iconList, fileNames.toMutableList()) // Pass fileNames
            recyclerView?.adapter = progressAdapter

            val titleTextView = dialogView.findViewById<TextView>(R.id.tvTitleProgressBarLayout)
            val counterTextView = dialogView.findViewById<TextView>(R.id.counter_dataset)
            counterTextView.text = "0 / $apiCallsSize"

            // Start the title animation
            lifecycleScope.launch(Dispatchers.Main) {
                var dots = 0
                while (alertDialog.isShowing) {
                    titleTextView.text = "Mengunduh Dataset" + ".".repeat(dots)
                    dots = if (dots >= 4) 1 else dots + 1
                    delay(500)
                }
            }

            for (i in 0 until apiCallsSize) {
                withContext(Dispatchers.Main) {
                    progressAdapter.updateProgress(i, 0, "Menunggu", R.id.progress_circular_loading)
                }
            }

            // Download files and update progress
            var completedCount = 0
            val downloadsDir = this@HomePageActivity.getExternalFilesDir(null)
            val fileList = mutableListOf<String?>()


            for ((index, apiCall) in ApiCallManager.apiCallList.withIndex()) {
                val fileName = apiCall.first
                val apiCallFunction = apiCall.second

                // Update progress as soon as downloading starts
                withContext(Dispatchers.Main) {
                    progressAdapter.resetProgress(index)
                    progressAdapter.updateProgress(index, 0, "Sedang Mengunduh",  R.id.progress_circular_loading)  // Show circular progress when download starts
                }


                for (progress in 0..100 step 10) {
                    withContext(Dispatchers.Main) {
                        progressAdapter.updateProgress(index, progress, "Sedang Mengunduh",  R.id.progress_circular_loading)
                    }
//                    delay(200)
                }

                val (isSuccessful, message) = downloadFile(fileName, apiCallFunction, downloadsDir, fileList)
                if (isSuccessful) {
                    completedCount++
                    withContext(Dispatchers.Main) {
                        progressAdapter.updateProgress(index, 100, message, R.drawable.baseline_check_24)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressAdapter.updateProgress(index, 100, message, R.drawable.baseline_close_24)
                    }
                }

                withContext(Dispatchers.Main) {
                    counterTextView.text = "$completedCount / $apiCallsSize"
                }
            }

            prefManager!!.saveFileList(fileList)


            val savedFileList = prefManager!!.getFileList()

            Log.d("testing", savedFileList.toString())

            // Add countdown message and delay before dismissing
            val closeText = dialogView.findViewById<TextView>(R.id.close_progress_statement)
            closeText.visibility = View.VISIBLE

            for (i in 3 downTo 1) {
                withContext(Dispatchers.Main) {
                    closeText.text = "Dialog tertutup otomatis dalam ${i} detik"
                    delay(1000) // Wait for 1 second
                }
            }

            alertDialog.dismiss()
        }
    }


    private suspend fun downloadFile(
        fileName: String,
        apiCall: suspend () -> Response<ResponseBody>,
        downloadsDir: File?,
        fileList: MutableList<String?>
    ): Pair<Boolean, String> {  // Changed return type to include message
        return try {
            withContext(Dispatchers.IO) {
                val response = apiCall()
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        val file = File(downloadsDir, fileName)
                        saveFileToStorage(responseBody, file)
                        fileList.add(fileName)
                        Log.d("FileDownload", "$fileName downloaded successfully.")
                        Pair(true, "Unduh Selesai")
                    } else {
                        fileList.add(null)
                        Log.e("FileDownload", "Response body is null.")
                        Pair(false, "Response body kosong")
                    }
                } else {
                    fileList.add(null)
                    try {
                        val errorBody = response.errorBody()?.string()
                        val gson = Gson()
                        val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                        Log.e("FileDownload", "Error message: ${errorResponse.message}")
                        Pair(false, "Unduh Gagal! ${errorResponse.message}")  // Added prefix here
                    } catch (e: Exception) {
                        Pair(false, "Unduh Gagal! Kode: ${response.code()}")  // Added prefix here
                    }
                }
            }
        } catch (e: Exception) {
            fileList.add(null)
            Log.e("FileDownload", "Error downloading file: ${e.message}")
            Pair(false, "Unduh Gagal! ${e.message}")  // Added prefix here
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

        Log.d("FileCheck", "Saved file list: $savedFileList")
        Log.d("FileCheck", "Downloads directory: $downloadsDir")
        Log.d("FileCheck", "Is first time launch: ${prefManager!!.isFirstTimeLaunch}")

        if (prefManager!!.isFirstTimeLaunch) {
            Log.d("FileCheck", "First time launch detected.")
            prefManager!!.isFirstTimeLaunch = false
            return true
        }

        if (savedFileList.isNotEmpty()) {
            // Check for null entries
            if (savedFileList.contains(null)) {
                Log.e("FileCheck", "Null entries found in savedFileList.")
                return true
            }

            // Check if all files exist
            val missingFiles = savedFileList.filterNot { fileName ->
                val file = File(downloadsDir, fileName)
                val exists = file.exists()
                Log.d("FileCheck", "Checking file: ${file.path} -> Exists: $exists")
                fileName != null && exists
            }

            if (missingFiles.isNotEmpty()) {
                Log.e("FileCheck", "Missing files detected: $missingFiles")
                return true
            }
        } else {
            Log.d("FileCheck", "Saved file list is empty.")
        }

        return false
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
                Log.d("FileCheck", "Starting file download...")
                startFileDownload()
            } else {
                Log.d("FileCheck", "File download not required.")
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
                    Log.d("FileCheck", "Starting file download...")
                    startFileDownload()
                } else {
                    Log.d("FileCheck", "File download not required.")
                }
            }
        }
    }
}