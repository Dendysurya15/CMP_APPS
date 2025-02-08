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
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
    private lateinit var dialog: Dialog
    data class ErrorResponse(
        val statusCode: Int,
        val message: String,
        val error: String? = null
    )
    private var hasShownErrorDialog = false  // Add this property
    private val permissionRequestCode = 1001
    private lateinit var adapter: DownloadProgressDatasetAdapter

    private var filesToUpdate = mutableListOf<String>()
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





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.d("HomePage: onCreate started")

        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefManager = PrefManager(this)
        AppLogger.d("HomePage: PrefManager initialized")

        loadingDialog = LoadingDialog(this)
        initViewModel()
        AppLogger.d("HomePage: ViewModel initialized")

        setupName()
        AppLogger.d("HomePage: Name setup completed")

        checkPermissions()
        AppLogger.d("HomePage: Permissions checked")

        setupDownloadDialog()
        AppLogger.d("HomePage: Download dialog setup completed")

        startDownloads()
        AppLogger.d("HomePage: Downloads initiated")

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_home_page)
        navView.setupWithNavController(navController)
        AppLogger.d("HomePage: Navigation setup completed")
    }

    private fun setupDownloadDialog() {
        AppLogger.d("Download Dialog: Setup started")
        dialog = Dialog(this)

        val view = layoutInflater.inflate(R.layout.list_card_upload, null)
        dialog.setContentView(view)
        AppLogger.d("Download Dialog: Layout inflated")

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        AppLogger.d("Download Dialog: Window size set")

        val recyclerView = view.findViewById<RecyclerView>(R.id.features_recycler_view)
        adapter = DownloadProgressDatasetAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        AppLogger.d("Download Dialog: RecyclerView setup completed")

        val titleTV = view.findViewById<TextView>(R.id.tvTitleProgressBarLayout)
        titleTV.text = "Download Progress"
        val counterTV = view.findViewById<TextView>(R.id.counter_dataset)
        val closeBtn = view.findViewById<FloatingActionButton>(R.id.close_progress_bar)
        val closeStatement = view.findViewById<TextView>(R.id.close_progress_statement)
        AppLogger.d("Download Dialog: Views initialized")

        closeBtn.setOnClickListener {
            AppLogger.d("Download Dialog: Close button clicked")
            dialog.dismiss()
        }

        datasetViewModel.downloadStatuses.observe(this) { statusMap ->
            AppLogger.d("Download Status: Received update with ${statusMap.size} items")
            val downloadItems = statusMap.map { (dataset, resource) ->
                when (resource) {
                    is DatasetViewModel.Resource.Success -> {
                        AppLogger.d("Download Status: $dataset completed successfully")
                        DownloadItem(dataset = dataset, progress = 100, isCompleted = true)
                    }
                    is DatasetViewModel.Resource.Error -> {
                        AppLogger.d("Download Status: $dataset failed with error: ${resource.message}")
                        // Only show error dialog for the first error
                        if (!hasShownErrorDialog) {
                            showErrorDialog(resource.message ?: "Unknown error occurred")
                            hasShownErrorDialog = true
                        }
                        DownloadItem(dataset = dataset, error = resource.message)
                    }
                    is DatasetViewModel.Resource.Loading -> {
                        AppLogger.d("Download Status: $dataset loading")
                        DownloadItem(dataset = dataset, isLoading = true)
                    }
                }
            }
            adapter.updateItems(downloadItems)

            val completedCount = downloadItems.count { it.isCompleted }
            AppLogger.d("Download Progress: $completedCount/${downloadItems.size} completed")
            counterTV.text = "$completedCount/${downloadItems.size}"

            if (downloadItems.all { it.isCompleted || it.error != null }) {
                AppLogger.d("Download Status: All downloads finished")
                closeBtn.visibility = View.VISIBLE
                closeStatement.visibility = View.VISIBLE
                closeStatement.text = "All downloads completed"
            }
        }
        AppLogger.d("Download Dialog: Setup completed")
    }

    private fun startDownloads() {
        AppLogger.d("Downloads: Starting download process")

        // Check if estateId exists and is valid
        val estateIdString = prefManager!!.estateIdUserLogin
//        AppLogger.d("Downloads: Estate ID from prefManager: $estateIdString")

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

            val regionalId = prefManager!!.regionalIdUserLogin!!.toInt()
//            AppLogger.d("Downloads: Estate ID: $estateId, Regional ID: $regionalId")

            val requests = listOf(
                DatasetRequest(estate = estateId, lastModified = null, dataset = "tph"),
                DatasetRequest(estate = estateId, lastModified = null, dataset = "blok"),
            )
//            AppLogger.d("Downloads: Created ${requests.size} download requests")

            dialog.show()
//            AppLogger.d("Downloads: Dialog shown")

            datasetViewModel.downloadMultipleDatasets(requests)
//            AppLogger.d("Downloads: Download requests sent to ViewModel")

        } catch (e: NumberFormatException) {
            AppLogger.d("Downloads: Failed to parse Estate ID to integer: ${e.message}")
            showErrorDialog("Invalid Estate ID format: ${e.message}")
            return
        }
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
            dialog.dismiss()  // Dismiss the download progress dialog
        }
    }



    private fun initViewModel() {
        val factory = DatasetViewModel.DatasetViewModelFactory(application)
        datasetViewModel = ViewModelProvider(this, factory)[DatasetViewModel::class.java]
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

            }
        }
    }

}