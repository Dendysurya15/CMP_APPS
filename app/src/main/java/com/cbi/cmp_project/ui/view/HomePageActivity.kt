package com.cbi.cmp_project.ui.view

import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.database.AppDatabase
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.KemandoranDetailModel
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.cmp_project.data.model.dataset.DatasetRequest
import com.cbi.cmp_project.data.network.RetrofitClient
import com.cbi.cmp_project.data.repository.CameraRepository
import com.cbi.cmp_project.data.repository.PanenTBSRepository
import com.cbi.cmp_project.databinding.ActivityHomePageBinding
import com.cbi.cmp_project.ui.adapter.DownloadItem
import com.cbi.cmp_project.ui.adapter.DownloadProgressDatasetAdapter
import com.cbi.cmp_project.ui.adapter.ProgressUploadAdapter
import com.cbi.cmp_project.ui.view.ui.home.HomeViewModel
import com.cbi.cmp_project.ui.viewModel.CameraViewModel
import com.cbi.cmp_project.ui.viewModel.DatasetViewModel
import com.cbi.cmp_project.ui.viewModel.LocationViewModel
import com.cbi.cmp_project.ui.viewModel.PanenTBSViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.stringXML
import com.cbi.cmp_project.utils.LoadingDialog
import com.cbi.cmp_project.utils.PrefManager
import com.cbi.markertph.data.model.BlokModel
import com.cbi.markertph.data.model.DeptModel
import com.cbi.markertph.data.model.DivisiModel
import com.cbi.markertph.data.model.RegionalModel
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.markertph.data.model.WilayahModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.GZIPInputStream

class HomePageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomePageBinding
    private lateinit var loadingDialog: LoadingDialog
    private var prefManager: PrefManager? = null
    private var isTriggerButton: Boolean = false
    private lateinit var dialog: Dialog
    data class ErrorResponse(
        val statusCode: Int,
        val message: String,
        val error: String? = null
    )
    private var hasShownErrorDialog = false  // Add this property
    private val permissionRequestCode = 1001
    private lateinit var adapter: DownloadProgressDatasetAdapter

    private var datasetMustUpdate = mutableListOf<String>()
    private var regionalList: List<RegionalModel> = emptyList()
    private var wilayahList: List<WilayahModel> = emptyList()
    private var deptList: List<DeptModel> = emptyList()
    private var divisiList: List<DivisiModel> = emptyList()
    private var blokList: List<BlokModel> = emptyList()
    private var karyawanList: List<KaryawanModel> = emptyList()
    private var kemandoranList: List<KemandoranModel> = emptyList()
    private var kemandoranDetailList: List<KemandoranDetailModel> = emptyList()
    private var tphList: List<TPHNewModel>? = null

    private lateinit var datasetViewModel: DatasetViewModel
    private lateinit var homeViewModel: HomeViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.d("HomePage: onCreate started")

        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefManager = PrefManager(this)

        loadingDialog = LoadingDialog(this)
        initViewModel()
        setupName()
        setupDownloadDialog()
        checkPermissions()


        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_home_page)
        navView.setupWithNavController(navController)
    }

    private fun setupDownloadDialog() {

        dialog = Dialog(this)

        val view = layoutInflater.inflate(R.layout.list_card_upload, null)
        dialog.setContentView(view)

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )


        val recyclerView = view.findViewById<RecyclerView>(R.id.features_recycler_view)
        adapter = DownloadProgressDatasetAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)


        val titleTV = view.findViewById<TextView>(R.id.tvTitleProgressBarLayout)
        titleTV.text = "Progress Import Dataset..."
        val counterTV = view.findViewById<TextView>(R.id.counter_dataset)

        val closeStatement = view.findViewById<TextView>(R.id.close_progress_statement)

        val retryDownloadDataset = view.findViewById<MaterialButton>(R.id.btnRetryDownloadDataset)
        val cancelDownloadDataset = view.findViewById<MaterialButton>(R.id.btnCancelDownloadDataset)
        val containerDownloadDataset = view.findViewById<LinearLayout>(R.id.containerDownloadDataset)
        cancelDownloadDataset.setOnClickListener {
            isTriggerButton = false
            dialog.dismiss()
        }
        retryDownloadDataset.setOnClickListener{



            val storedList = prefManager!!.datasetMustUpdate // Retrieve list

            AppLogger.d(storedList.toString())
            containerDownloadDataset.visibility = View.GONE
            cancelDownloadDataset.visibility = View.GONE
            retryDownloadDataset.visibility = View.GONE
            closeStatement.visibility = View.GONE
            startDownloads()
        }

        datasetViewModel.downloadStatuses.observe(this) { statusMap ->

            val downloadItems = statusMap.map { (dataset, resource) ->
                when (resource) {
                    is DatasetViewModel.Resource.Success -> {
                        AppLogger.d("Download Status: $dataset completed")
                        DownloadItem(
                            dataset = dataset,
                            progress = 100,
                            isCompleted = false,
                            isExtractionCompleted = false,
                            isStoringCompleted = true  // Final state is storage complete
                        )
                    }
                    is DatasetViewModel.Resource.Error -> {
                        AppLogger.d("Download Status: $dataset failed with error: ${resource.message}")

                        if (!hasShownErrorDialog) {
                            val errorMessage = resource.message ?: "Unknown error occurred"
                            if (errorMessage.contains("host", ignoreCase = true)) {
                                showErrorDialog("Mohon cek koneksi Internet Smartphone anda!")
                            } else {
                                showErrorDialog(errorMessage)
                            }
                            hasShownErrorDialog = true
                        }
                        DownloadItem(dataset = dataset, error = resource.message)
                    }
                    is DatasetViewModel.Resource.Loading -> {
                        AppLogger.d("Download Status: $dataset loading")
                        DownloadItem(dataset = dataset, progress = resource.progress, isLoading = true)
                    }
                    is DatasetViewModel.Resource.Extracting -> {
                        AppLogger.d("Download Status: $dataset is being extracted")
                        DownloadItem(
                            dataset = dataset,
                            progress = 100,
                            isLoading = false,
                            isExtracting = true
                        )
                    }
                    is DatasetViewModel.Resource.Storing -> {
                        AppLogger.d("Download Status: $dataset is being stored")
                        DownloadItem(
                            dataset = dataset,
                            progress = 100,
                            isLoading = false,
                            isExtracting = false,
                            isStoring = true
                        )

                    }
                    is DatasetViewModel.Resource.UpToDate -> {

                        DownloadItem(
                            dataset = dataset,
                            progress = 100,
                            isUpToDate = true  // Set isUpToDate to true
                        )
                    }
                }
            }

            adapter.updateItems(downloadItems)

            val completedCount = downloadItems.count { it.isStoringCompleted || it.isUpToDate || it.error != null }
            AppLogger.d("Progress: $completedCount/${downloadItems.size} completed")
            counterTV.text = "$completedCount/${downloadItems.size}"


            if (downloadItems.all { it.isStoringCompleted || it.isUpToDate || it.error != null }) {


                if (prefManager!!.isFirstTimeLaunch && downloadItems.any { it.isStoringCompleted || it.isUpToDate || it.error != null}) {
                    prefManager!!.isFirstTimeLaunch = false
                    AppLogger.d("First-time launch flag updated to false")
                }

                if (downloadItems.any { it.error != null }) {
                    containerDownloadDataset.visibility = View.VISIBLE
                    retryDownloadDataset.visibility = View.VISIBLE
                    cancelDownloadDataset.visibility = View.VISIBLE

                }
                else {
                    containerDownloadDataset.visibility = View.VISIBLE
//                    var countdown =5
//                    closeStatement.visibility = View.VISIBLE
//                    val handler = Handler(Looper.getMainLooper())
//
//                    handler.postDelayed(object : Runnable {
//                        override fun run() {
//                            if (countdown > 0) {
//                                closeStatement.text = "Dialog tertutup dalam $countdown detik"
//                                countdown--
//                                handler.postDelayed(this, 1000)
//                            } else dialog.dismiss()
//                        }
//                    }, 0)

                    cancelDownloadDataset.visibility = View.VISIBLE


                }

            }
        }
    }

    private fun startDownloads() {
        val estateIdString = prefManager!!.estateIdUserLogin
        val lastModifiedDatasetTPH = prefManager!!.lastModifiedDatasetTPH
        val lastModifiedDatasetBlok = prefManager!!.lastModifiedDatasetBlok
        val lastModifiedDatasetKemandoran = prefManager!!.lastModifiedDatasetKemandoran
        val lastModifiedDatasetPemanen = prefManager!!.lastModifiedDatasetPemanen

        if (estateIdString.isNullOrEmpty() || estateIdString.isBlank()) {
            AppLogger.d("Downloads: Estate ID is null or empty, aborting download")
            showErrorDialog("Estate ID is not valid. Current value: '$estateIdString'")
            return
        }

        try {
            val estateId = estateIdString.toInt()
            if (estateId <= 0) {
                AppLogger.d("Downloads: Estate ID is not a valid positive number: $estateId")
                showErrorDialog("Estate ID must be a positive number")
                return
            }

            AppLogger.d(isTriggerButton.toString())
            val filteredRequests = if (isTriggerButton) {

                getDatasetsToDownload(estateId, lastModifiedDatasetTPH, lastModifiedDatasetBlok, lastModifiedDatasetPemanen, lastModifiedDatasetKemandoran)
            } else {
                AppLogger.d("xixidi")
                getDatasetsToDownload(estateId, lastModifiedDatasetTPH, lastModifiedDatasetBlok, lastModifiedDatasetPemanen, lastModifiedDatasetKemandoran)
                    .filterNot { prefManager!!.datasetMustUpdate.contains(it.dataset) }
            }

            if (filteredRequests.isNotEmpty()) {
                dialog.show()
                datasetViewModel.downloadMultipleDatasets(filteredRequests)
            } else {
                AppLogger.d("All datasets are up-to-date, no download needed.")
            }


        } catch (e: NumberFormatException) {
            AppLogger.d("Downloads: Failed to parse Estate ID to integer: ${e.message}")
            showErrorDialog("Invalid Estate ID format: ${e.message}")
        }
    }

    private fun getDatasetsToDownload(
        estateId: Int,
        lastModifiedDatasetTPH: String?,
        lastModifiedDatasetBlok: String?,
        lastModifiedDatasetPemanen: String?,
        lastModifiedDatasetKemandoran: String?
    ): List<DatasetRequest> {
        return listOf(
            DatasetRequest(estate = estateId, lastModified = lastModifiedDatasetTPH, dataset = "tph"),
            DatasetRequest(estate = estateId, lastModified = lastModifiedDatasetBlok, dataset = "blok"),
            DatasetRequest(estate = estateId, lastModified = lastModifiedDatasetPemanen, dataset = "pemanen"),
            DatasetRequest(estate = estateId, lastModified = lastModifiedDatasetKemandoran, dataset = "kemandoran")
        )
    }



    private fun showErrorDialog(errorMessage: String) {
        AppLogger.d("Showing error dialog with message: $errorMessage")
        AlertDialogUtility.withSingleAction(
            this@HomePageActivity,
            stringXML(R.string.al_back),
            stringXML(R.string.al_failed_fetch_data),
            "${stringXML(R.string.al_failed_fetch_data_desc)}, $errorMessage",
            "warning.json",
            R.color.colorRedDark
        ) {
//            dialog.dismiss()  // Dismiss the download progress dialog
        }
    }



    private fun initViewModel() {
        val factory = DatasetViewModel.DatasetViewModelFactory(application)
        datasetViewModel = ViewModelProvider(this, factory)[DatasetViewModel::class.java]
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        homeViewModel.startSinkronisasiData.observe(this) { shouldStart ->
            if (shouldStart) {
                isTriggerButton = true
                startDownloads()
            }
        }
    }
    private fun setupName(){
        val tvUserNameLogin = findViewById<TextView>(R.id.userNameLogin)
        tvUserNameLogin.text = prefManager!!.nameUserLogin

        val jabatanUserLogin = findViewById<TextView>(R.id.jabatanUserLogin)
        jabatanUserLogin.text = "${prefManager!!.jabatanUserLogin} - ${prefManager!!.estateUserLogin}"

        val fullName = prefManager!!.nameUserLogin!!.trim()
        val nameParts = fullName.split(" ")

        val initials = when (nameParts.size) {
            0 -> "" // No name provided
            1 -> nameParts[0].take(1).uppercase() // Only one name, take the first letter
            else -> (nameParts[0].take(1) + nameParts[1].take(1)).uppercase() // Take first letter from first two names
        }

        val initalName = findViewById<TextView>(R.id.initalName)
        initalName.text = initials

    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 and above
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            // Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                permissionRequestCode
            )
        }else{
            startDownloads()
        }
    }

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
                startDownloads()
            }
        }
    }

}