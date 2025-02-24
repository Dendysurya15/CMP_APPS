package com.cbi.cmp_project.ui.view.Absensi

import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.model.KaryawanModel
//import com.cbi.cmp_project.data.model.KemandoranDetailModel
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.cmp_project.data.repository.AbsensiRepository
import com.cbi.cmp_project.data.repository.CameraRepository
import com.cbi.cmp_project.data.repository.PanenTBSRepository
import com.cbi.cmp_project.ui.adapter.AbsensiAdapter
import com.cbi.cmp_project.ui.adapter.AbsensiDataList
import com.cbi.cmp_project.ui.adapter.SelectedWorkerAdapter
import com.cbi.cmp_project.ui.adapter.TakeFotoPreviewAdapter
import com.cbi.cmp_project.ui.adapter.Worker
import com.cbi.cmp_project.ui.view.panenTBS.FeaturePanenTBSActivity
import com.cbi.cmp_project.ui.viewModel.AbsensiViewModel
import com.cbi.cmp_project.ui.viewModel.CameraViewModel
import com.cbi.cmp_project.ui.viewModel.DatasetViewModel
import com.cbi.cmp_project.ui.viewModel.LocationViewModel
import com.cbi.cmp_project.ui.viewModel.PanenTBSViewModel
import com.cbi.cmp_project.ui.viewModel.PanenViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import android.text.InputType as AndroidInputType
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.stringXML
//import com.cbi.cmp_project.utils.DataCacheManager
import com.cbi.cmp_project.utils.LoadingDialog
import com.cbi.cmp_project.utils.PrefManager
import com.cbi.markertph.data.model.TPHNewModel
//import com.cbi.markertph.data.model.DeptModel
//import com.cbi.markertph.data.model.DivisiModel
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.materialspinner.MaterialSpinner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.jvm.java

open class FeatureAbsensiActivity : AppCompatActivity(), CameraRepository.PhotoCallback {

    private var photoCount = 0
    private val photoFiles = mutableListOf<String>() // Store filenames
    private val komentarFoto = mutableListOf<String>() // Store filenames

    private var lat: Double? = null
    private var lon: Double? = null
    var currentAccuracy: Float = 0F
    private var prefManager: PrefManager? = null

    private var featureName: String? = null
    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var locationViewModel: LocationViewModel
    private var locationEnable: Boolean = false
    private var isPermissionRationaleShown = false
    private lateinit var takeFotoPreviewAdapter: TakeFotoPreviewAdapter

//    private var deptList: List<DeptModel> = emptyList()
//    private var divisiList: List<DivisiModel> = emptyList()
    private var divisiList: List<TPHNewModel> = emptyList()

    private var karyawanList: List<KaryawanModel> = emptyList()
    private var karyawanLainList: List<KaryawanModel> = emptyList()
    private var kemandoranList: List<KemandoranModel> = emptyList()
    private var kemandoranLainList: List<KemandoranModel> = emptyList()
//    private var kemandoranDetailList: List<KemandoranDetailModel> = emptyList()

    private lateinit var absensiAdapter: AbsensiAdapter


    private lateinit var loadingDialog: LoadingDialog
//    private lateinit var dataCacheManager: DataCacheManager

    enum class InputType {
        SPINNER,
        EDITTEXT
    }

    private var selectedAfdeling: String = ""
    private var selectedKaryawan: String = ""
    private var selectedKemandoran: String = ""
    private var infoApp: String = ""

    private var selectedDivisiValue: Int? = null
    private var selectedDivisionSpinnerIndex: Int? = null

    private lateinit var inputMappings: List<Triple<LinearLayout, String, FeatureAbsensiActivity.InputType>>
    private lateinit var datasetViewModel: DatasetViewModel
    private lateinit var absensiViewModel: AbsensiViewModel
    private var regionalId: String? = null
    private var regionalName: String? = null
    private var estateId: String? = null
    private var estateName: String? = null
    private var userName: String? = null
    private var userId: Int? = null
    private var jabatanUser: String? = null
    private var afdelingUser: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_absensi)
        loadingDialog = LoadingDialog(this)

        prefManager = PrefManager(this)
        initViewModel()
        regionalId = prefManager!!.regionalIdUserLogin
        estateId = prefManager!!.estateIdUserLogin
        estateName = prefManager!!.estateUserLogin
        userName = prefManager!!.nameUserLogin
        userId = prefManager!!.idUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin

        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressed() }

        setupHeader()

        infoApp = AppUtils.getDeviceInfo(this@FeatureAbsensiActivity).toString()

        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadingDialog.show()
                loadingDialog.setMessage("Loading data...")
            }

            try {
                val estateIdStr = estateId?.trim()

                if (!estateIdStr.isNullOrEmpty() && estateIdStr.toIntOrNull() != null) {
                    val estateIdInt = estateIdStr.toInt()

//                    val deptDeferred = async { datasetViewModel.getDeptList(estateIdStr) }
                    val divisiDeferred = async { datasetViewModel.getDivisiList(estateIdInt) }

                    divisiList = divisiDeferred.await()

                    AppLogger.d(divisiList.toString())
//                    deptList = deptDeferred.await()
                }

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    setupLayout()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {  // Ensure dialog is shown on Main thread
                    AppLogger.e("Error fetching data: ${e.message}")

                    AlertDialogUtility.withSingleAction(
                        this@FeatureAbsensiActivity,
                        stringXML(R.string.al_back),
                        stringXML(R.string.al_failed_fetch_data),
                        "${stringXML(R.string.al_failed_fetch_data_desc)},  ${e.message} penyebab ID Estate User Login: \"$estateId\"",
                        "warning.json",
                        R.color.colorRedDark
                    ) {
                        finish()
                    }
                }
            }
        }

//
//        // Save button
//        val mbSaveDataAbsensi: Button = findViewById(R.id.mbSaveDataAbsensi)
//        mbSaveDataAbsensi.setOnClickListener {
//            Toast.makeText(this, "Data Absensi Disimpan", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    // Sample data for RecyclerView
//    private fun getSampleData(): List<AbsensiViewModel> {
//        return listOf(
//            AbsensiViewModel("Dendy S D", "Mandor Panen A", false),
//            AbsensiViewModel("Siti R", "Asisten Lapangan", true),
//            AbsensiViewModel("Ahmad T", "Operator Mesin", false)
//        )

        val recyclerView = findViewById<RecyclerView>(R.id.rvTableDataAbsensi)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inisialisasi Adapter dengan List kosong
        absensiAdapter = AbsensiAdapter(emptyList())
        recyclerView.adapter = absensiAdapter

//
//        absensiAdapter = AbsensiAdapter(absensiList) { position, isChecked ->
//            absensiList[position].isChecked = isChecked
//            Log.d("Absensi", "Karyawan ${absensiList[position].nama} hadir: $isChecked")
//        }
//        recyclerView.adapter = absensiAdapter

    }

    private fun initViewModel() {
        val idTakeFotoLayout = findViewById<View>(R.id.fotoAbsensi)
        val idEditFotoLayout = findViewById<View>(R.id.editFotoAbsensi)
        val cameraRepository = CameraRepository(this, window, idTakeFotoLayout, idEditFotoLayout)
        cameraRepository.setPhotoCallback(this)
        cameraViewModel = ViewModelProvider(
            this,
            CameraViewModel.Factory(cameraRepository)
        )[CameraViewModel::class.java]

        val status_location = findViewById<ImageView>(R.id.statusLocation)
        locationViewModel = ViewModelProvider(
            this,
            LocationViewModel.Factory(application, status_location, this)
        )[LocationViewModel::class.java]

        val factory = DatasetViewModel.DatasetViewModelFactory(application)
        datasetViewModel = ViewModelProvider(this, factory)[DatasetViewModel::class.java]
    }


    private fun setupHeader() {
        featureName = intent.getStringExtra("FEATURE_NAME")
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        val userSection = findViewById<TextView>(R.id.userSection)
        val locationSection = findViewById<LinearLayout>(R.id.locationSection)
        locationSection.visibility = View.VISIBLE

        AppUtils.setupUserHeader(
            userName = userName,
            jabatanUser = jabatanUser,
            estateName = estateName,
            afdelingUser = afdelingUser,
            userSection = userSection,
            featureName = featureName,
            tvFeatureName = tvFeatureName
        )
    }

    private fun updateTextInPertanyaan(linearLayout: LinearLayout, text: String) {
        // Assuming the TextView inside the LinearLayout has an ID, e.g., `tvTitleFormPanenTBS`
        val textView = linearLayout.findViewById<TextView>(R.id.tvTitleFormPanenTBS)
        textView.text = text
    }

    private fun setupLayout() {
        inputMappings = listOf(
            Triple(
                findViewById<LinearLayout>(R.id.layoutEstateAbsensi),
                getString(R.string.field_estate),
                FeatureAbsensiActivity.InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutAfdelingAbsensi),
                getString(R.string.field_afdeling),
                FeatureAbsensiActivity.InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutkemandoranAbsensi),
                getString(R.string.field_kemandoran),
                FeatureAbsensiActivity.InputType.SPINNER
            )
        )

        inputMappings.forEach { (layoutView, key, inputType) ->
            updateTextInPertanyaan(layoutView, key)
            when (inputType) {
                FeatureAbsensiActivity.InputType.SPINNER -> {
                    when (layoutView.id) {
                        R.id.layoutEstateAbsensi -> {
                            val namaEstate = listOf(prefManager!!.estateUserLengkapLogin ?: "")
                            setupSpinnerView(layoutView, namaEstate)
                            findViewById<MaterialSpinner>(R.id.spPanenTBS).setSelectedIndex(0)
                        }

                        R.id.layoutAfdelingAbsensi -> {
                            AppLogger.d(divisiList.toString())
                            val divisiNames = divisiList.mapNotNull { it.divisi_abbr }
                            setupSpinnerView(layoutView, divisiNames)
                        }

                        else -> {
                            setupSpinnerView(layoutView, emptyList())
                        }

                    }
                }

                FeatureAbsensiActivity.InputType.EDITTEXT -> setupEditTextView(layoutView)

            }
        }

        setupRecyclerViewTakePreviewFoto()
    }

    private fun setupRecyclerViewTakePreviewFoto() {
        val recyclerView: RecyclerView = findViewById(R.id.rcFotoPreview)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER

        takeFotoPreviewAdapter = TakeFotoPreviewAdapter(1, cameraViewModel, this, featureName)
        recyclerView.adapter = takeFotoPreviewAdapter
    }

    private fun setupEditTextView(layoutView: LinearLayout) {
        val etHomeMarkerTPH = layoutView.findViewById<EditText>(R.id.etHomeMarkerTPH)
        val spHomeMarkerTPH = layoutView.findViewById<View>(R.id.spPanenTBS)
        val tvError = layoutView.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
        val MCVSpinner = layoutView.findViewById<View>(R.id.MCVSpinner)

        spHomeMarkerTPH.visibility = View.GONE
        etHomeMarkerTPH.visibility = View.VISIBLE

        etHomeMarkerTPH.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm =
                    application.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                findViewById<MaterialSpinner>(R.id.spPanenTBS)?.requestFocus()
                true
            } else {
                false
            }
        }

        etHomeMarkerTPH.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvError.visibility = View.GONE
                MCVSpinner.setBackgroundColor(
                    ContextCompat.getColor(
                        layoutView.context,
                        R.color.graytextdark
                    )
                )
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun animateLoadingDots(linearLayout: LinearLayout) {
        val loadingContainer = linearLayout.findViewById<LinearLayout>(R.id.loadingDotsContainer)
        val spinner = linearLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        val dots = listOf(
            loadingContainer.findViewById<TextView>(R.id.dot1),
            loadingContainer.findViewById<TextView>(R.id.dot2),
            loadingContainer.findViewById<TextView>(R.id.dot3),
            loadingContainer.findViewById<TextView>(R.id.dot4)
        )

        spinner.visibility = View.INVISIBLE
        loadingContainer.visibility = View.VISIBLE

        // Animate each dot
        dots.forEachIndexed { index, dot ->
            val animation = ObjectAnimator.ofFloat(dot, "translationY", 0f, -10f, 0f)
            animation.duration = 600
            animation.repeatCount = ObjectAnimator.INFINITE
            animation.repeatMode = ObjectAnimator.REVERSE
            animation.startDelay = (index * 100).toLong() // Stagger the animations
            animation.start()
        }
    }

    private fun hideLoadingDots(linearLayout: LinearLayout) {
        val loadingContainer = linearLayout.findViewById<LinearLayout>(R.id.loadingDotsContainer)
        val spinner = linearLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)

        loadingContainer.visibility = View.GONE
        spinner.visibility = View.VISIBLE
    }

    private fun setupSpinnerView(
        linearLayout: LinearLayout,
        data: List<String>,
        onItemSelected: (Int) -> Unit = {}
    ) {
        val spinner = linearLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        val tvError = linearLayout.findViewById<TextView>(R.id.tvErrorFormPanenTBS)

        spinner.setItems(data)
        spinner.setTextSize(18f)

        if (linearLayout.id == R.id.layoutEstateAbsensi) {
            spinner.isEnabled = false // Disable the spinner
        }
        spinner.setOnItemSelectedListener { _, position, _, item ->
            tvError.visibility = View.GONE

            when (linearLayout.id) {
                R.id.layoutAfdelingAbsensi -> {
//                    resetDependentSpinners(linearLayout.rootView)
                    selectedAfdeling = item.toString()

                    val selectedDivisiId = try {
                        divisiList.find { it.divisi_abbr == selectedAfdeling }?.divisi
                    } catch (e: Exception) {
                        AppLogger.e("Error finding selectedDivisiId: ${e.message}")
                        null
                    }

                    val selectedDivisiIdList = selectedDivisiId?.let { listOf(it) } ?: emptyList()
                    selectedDivisionSpinnerIndex = position
                    selectedDivisiValue = selectedDivisiId

                    val nonSelectedAfdelingKemandoran = try {
                        divisiList.filter { it.divisi_abbr != selectedAfdeling }
                    } catch (e: Exception) {
                        AppLogger.e("Error filtering nonSelectedAfdelingKemandoran: ${e.message}")
                        emptyList()
                    }

                    val nonSelectedIdAfdeling = try {
                        nonSelectedAfdelingKemandoran.map { it.divisi }
                    } catch (e: Exception) {
                        AppLogger.e("Error mapping nonSelectedIdAfdeling: ${e.message}")
                        emptyList()
                    }

                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            animateLoadingDots(linearLayout)
                            delay(1000) // 1 second delay
                        }

                        try {
                            if (estateId == null || selectedDivisiId == null) {
                                throw IllegalStateException("Estate ID or selectedDivisiId is null!")
                            }

                            val kemandoranDeferred = async {
                                try {
                                    datasetViewModel.getKemandoranAbsensiList(
                                        estateId!!.toInt(),
                                        selectedDivisiIdList
                                    )
                                } catch (e: Exception) {
                                    AppLogger.e("Error fetching kemandoranList: ${e.message}")
                                    emptyList()
                                }
                            }

                            val kemandoranLainDeferred = async {
                                try {
                                    datasetViewModel.getKemandoranAbsensiList(
                                        estateId!!.toInt(),
                                        nonSelectedIdAfdeling as List<Int>
                                    )
                                } catch (e: Exception) {
                                    AppLogger.e("Error fetching kemandoranLainList: ${e.message}")
                                    emptyList()
                                }
                            }

                            kemandoranList = kemandoranDeferred.await()
                            kemandoranLainList = kemandoranLainDeferred.await()

                            withContext(Dispatchers.Main) {
                                try {
                                    val layoutKemandoran =
                                        linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutkemandoranAbsensi)
                                    val layoutKemandoranLain =
                                        linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutKemandoranLain)

                                    val kemandoranNames = kemandoranList.map { it.nama }
                                    setupSpinnerView(
                                        layoutKemandoran,
                                        if (kemandoranNames.isNotEmpty()) kemandoranNames as List<String> else emptyList()
                                    )

                                    val kemandoranLainListNames = kemandoranLainList.map { it.nama }
                                    setupSpinnerView(
                                        layoutKemandoranLain,
                                        if (kemandoranLainListNames.isNotEmpty()) kemandoranLainListNames as List<String> else emptyList()
                                    )
                                } catch (e: Exception) {
                                    AppLogger.e("Error updating UI: ${e.message}")
                                }
                            }
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching afdeling data: ${e.message}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@FeatureAbsensiActivity,
                                    "Error loading afdeling data: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                hideLoadingDots(linearLayout)
                            }
                        }
                    }
                }

                R.id.layoutkemandoranAbsensi -> {
                    selectedKemandoran = item.toString()

                    AppLogger.d(estateId.toString())
                    AppLogger.d(selectedDivisiValue.toString())
                    val filteredKemandoranId: Int? = try {
                        kemandoranList.find {
                            it.dept == estateId?.toIntOrNull() && // Avoids force unwrap (!!)
                                    it.divisi == selectedDivisiValue &&
                                    it.nama == selectedKemandoran
                        }?.id
                    } catch (e: Exception) {
                        AppLogger.e("Error finding Kemandoran ID: ${e.message}")
                        null
                    }

                    if (filteredKemandoranId != null) {
                        AppLogger.d("Filtered Kemandoran ID: $filteredKemandoranId")

                        lifecycleScope.launch(Dispatchers.IO) {
                            withContext(Dispatchers.Main) {
                                animateLoadingDots(linearLayout)
                                delay(1000) // 1 second delay
                            }

                            try {
                                val karyawanDeferred = async {
                                    datasetViewModel.getKaryawanList(filteredKemandoranId)
                                }

                                karyawanList = karyawanDeferred.await()

                                val karyawanNames = karyawanList.map { it.nama }
                                AppLogger.d(karyawanNames.toString())
                                AppLogger.d(karyawanList.toString())

                                val absensiList = karyawanList.map { karyawan ->
                                    AbsensiDataList(
                                        nama = "${karyawan.nik} - ${karyawan.nama}",
                                        jabatan = karyawan.jabatan ?: "-" // Tangani null
                                    )
                                }
                                withContext(Dispatchers.Main) {

                                    absensiAdapter.updateList(absensiList)

//                                    val layoutPemanen =
//                                        linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutPemanen)
//                                    if (karyawanNames.isNotEmpty()) {
//                                        setupSpinnerView(layoutPemanen,
//                                            karyawanNames as List<String>
//                                        )
//                                    } else {
//                                        setupSpinnerView(layoutPemanen, emptyList())
//                                    }
                                }
                            } catch (e: Exception) {
                                AppLogger.e("Error fetching afdeling data: ${e.message}")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@FeatureAbsensiActivity,
                                        "Error loading kemandoran: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    hideLoadingDots(linearLayout)
                                }
                            }
                        }
                    } else {
                        AppLogger.e("Filtered Kemandoran ID is null, skipping data fetch.")
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        locationViewModel.locationPermissions.observe(this) { isLocationEnabled ->
            if (!isLocationEnabled) {
                requestLocationPermission()
            } else {
                locationViewModel.startLocationUpdates()
            }
        }

        locationViewModel.locationData.observe(this) { location ->
            locationEnable = true
            lat = location.latitude
            lon = location.longitude
        }

        locationViewModel.locationAccuracy.observe(this) { accuracy ->
            findViewById<TextView>(R.id.accuracyLocation).text = String.format("%.1f m", accuracy)

            currentAccuracy = accuracy
        }

    }

    override fun onPause() {
        super.onPause()
        locationViewModel.stopLocationUpdates()

    }

    override fun onDestroy() {
        super.onDestroy()
        locationViewModel.stopLocationUpdates()


    }

    override fun onPhotoTaken(
        photoFile: File,
        fname: String,
        resultCode: String,
        deletePhoto: View?,
        position: Int,
        komentar: String?
    ) {
        val recyclerView = findViewById<RecyclerView>(R.id.rcFotoPreview)
        val adapter = recyclerView.adapter as? TakeFotoPreviewAdapter

        adapter?.addPhotoFile("$position", photoFile)

        photoCount++
        photoFiles.add(fname)
        komentarFoto.add(komentar!!)

        val viewHolder =
            recyclerView.findViewHolderForAdapterPosition(position) as? TakeFotoPreviewAdapter.FotoViewHolder
        viewHolder?.let {
            Glide.with(this)
                .load(photoFile)
                .into(it.imageView)
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showSnackbar("Location permission is required for this app. Change in Settings App")
            isPermissionRationaleShown = true
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                locationViewModel.startLocationUpdates()
            } else {
                showSnackbar("Location permission denied.")
            }
        }

}