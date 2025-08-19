package com.cbi.mobile_plantation.ui.view.panenTBS

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cbi.mobile_plantation.data.model.JenisTPHModel
import com.cbi.mobile_plantation.data.model.TPHNewModel
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.model.AbsensiKemandoranRelations
import com.cbi.mobile_plantation.data.model.AfdelingModel
import com.cbi.mobile_plantation.data.model.EstateModel
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.data.model.dataset.DatasetRequest
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.data.repository.CameraRepository
import com.cbi.mobile_plantation.data.repository.PanenTBSRepository
import com.cbi.mobile_plantation.ui.adapter.ListTPHInsideRadiusAdapter
import com.cbi.mobile_plantation.ui.adapter.SelectedWorkerAdapter
import com.cbi.mobile_plantation.ui.adapter.TakeFotoPreviewAdapter
import com.cbi.mobile_plantation.ui.adapter.UploadCMPItem
import com.cbi.mobile_plantation.ui.adapter.UploadProgressCMPDataAdapter
import com.cbi.mobile_plantation.ui.adapter.Worker
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.viewModel.AbsensiViewModel
import com.cbi.mobile_plantation.ui.viewModel.CameraViewModel
import com.cbi.mobile_plantation.ui.viewModel.DatasetViewModel
import com.cbi.mobile_plantation.ui.viewModel.LocationViewModel
import com.cbi.mobile_plantation.ui.viewModel.MutuBuahViewModel
import com.cbi.mobile_plantation.ui.viewModel.PanenTBSViewModel
import com.cbi.mobile_plantation.ui.viewModel.PanenViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.MathFun
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.ScannedTPHLocation
import com.cbi.mobile_plantation.utils.ScannedTPHSelectionItem
import com.cbi.mobile_plantation.utils.SoundPlayer
import com.cbi.mobile_plantation.utils.playSound
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.jaredrummler.materialspinner.MaterialSpinner
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.reflect.KMutableProperty0
import android.text.InputType as AndroidInputType

@Suppress("UNCHECKED_CAST", "KotlinConstantConditions")
open class FeaturePanenTBSActivity : AppCompatActivity(),
    TakeFotoPreviewAdapter.LocationDataProvider, CameraRepository.PhotoCallback,
    ListTPHInsideRadiusAdapter.OnTPHSelectedListener {
    private var isSnackbarShown = false
    private var photoCount = 0
    private var photoCountSelfie = 0
    private val photoFiles = mutableListOf<String>() // Store filenames
    private val photoFilesSelfie = mutableListOf<String>() // Store filenames
    private val komentarFoto = mutableListOf<String>() // Store filenames

    private lateinit var layoutSelfiePhoto: View
    private var selfiePhotoFile: File? = null

    private var jumTBS = 0
    private var bMentah = 0
    private var bLewatMasak = 0
    private var jjgKosong = 0
    private var abnormal = 0
    private var seranganTikus = 0
    private var tangkaiPanjang = 0
    private var tidakVCut = 0
    private var lat: Double? = null
    private var lon: Double? = null
    private var finalLat: Double? = null
    private var finalLon: Double? = null
    private var selectedTPHIdByScan: Int? = null
    private var tph_otomatis_estate: Int? = null
    var currentAccuracy: Float = 0F
    private var prefManager: PrefManager? = null
    private val _masterEstateChoice = MutableLiveData<Map<String, Boolean>>(mutableMapOf())
    val masterEstateChoice: LiveData<Map<String, Boolean>> = _masterEstateChoice

    // Change this from Map<Int, Boolean> to Map<String, Boolean>
    val masterEstateHasBeenChoice = mutableMapOf<String, Boolean>()

    private var featureName: String? = null
    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var panenTBSViewModel: PanenTBSViewModel
    private var locationEnable: Boolean = false
    private lateinit var btnScanTPHRadius: MaterialButton
    private lateinit var tphScannedResultRecyclerView: RecyclerView
    private lateinit var titleScannedTPHInsideRadius: TextView
    private lateinit var descScannedTPHInsideRadius: TextView
    private lateinit var emptyScannedTPHInsideRadius: TextView
    private lateinit var alertCardScanRadius: MaterialCardView
    private lateinit var alertTvScannedRadius: TextView
    private lateinit var backButton: ImageView
    private lateinit var layoutAncak: View
    private lateinit var layoutNomorPemanen: View
    private lateinit var layoutEstate: LinearLayout
    private lateinit var layoutKemandoran: LinearLayout
    private lateinit var layoutKemandoranLain: LinearLayout
    private lateinit var layoutTipePanen: LinearLayout
    private lateinit var layoutPemanenLain: LinearLayout
    private lateinit var layoutPemanen: LinearLayout
    private lateinit var layoutTahunTanam: LinearLayout
    private lateinit var layoutMasterTPH: LinearLayout
    private lateinit var layoutNoTPH: LinearLayout
    private lateinit var layoutBlok: LinearLayout
    private lateinit var layoutSelAsistensi: LinearLayout
    private lateinit var tvErrorScannedNotSelected: TextView
    private lateinit var mbSaveDataPanenTBS: MaterialButton
    private lateinit var progressBarScanTPHManual: ProgressBar
    private lateinit var progressBarScanTPHAuto: ProgressBar
    private var keyboardBeingDismissed = false
    private var presentNikSet: Set<String> = emptySet()

    private var latLonMap: Map<Int, ScannedTPHLocation> = emptyMap()

    private lateinit var takeFotoPreviewAdapter: TakeFotoPreviewAdapter


    private var masterDeptInfoMap: Map<String, String> = emptyMap()
    private var estateList: List<EstateModel> = emptyList()
    private var divisiList: List<TPHNewModel> = emptyList()
    private var absensiList: List<AbsensiKemandoranRelations> = emptyList()
    private var blokList: List<TPHNewModel> = emptyList()
    private var karyawanList: List<KaryawanModel> = emptyList()
    private var karyawanLainList: List<KaryawanModel> = emptyList()
    private var kemandoranList: List<KemandoranModel> = emptyList()
    private var afdelingList: List<AfdelingModel> = emptyList()
    private var kemandoranLainList: List<KemandoranModel> = emptyList()
    private val karyawanNamaMap = mutableMapOf<String, String>()
    private val karyawanNamaLainMap = mutableMapOf<String, String>()
    private var tphList: List<TPHNewModel> = emptyList()

    private lateinit var loadingDialog: LoadingDialog
    private lateinit var selectedPemanenAdapter: SelectedWorkerAdapter
    private lateinit var selectedPemanenLainAdapter: SelectedWorkerAdapter
    private lateinit var rvSelectedPemanen: RecyclerView
    private lateinit var rvSelectedPemanenLain: RecyclerView
    private var selectedTPHJenisId: Int? = null

    enum class InputType {
        SPINNER,
        EDITTEXT,
        RADIO,
    }

    private var ancakInput: String = ""
    private var nomorPemanenInput: String = ""
    private var asistensi: Int = 0
    private var blokBanjir: Int = 0
    private var selectedTipePanen: String = ""
    private var selectedAfdeling: String = ""
    private var selectedAfdelingIdSpinner: Int = 0
    private var selectedKemandoranIdSpinner: Int = 0
    private var selectedTahunTanamIdSpinner: Int = 0
    private var selectedKemandoranLainIdSpinner: Int = 0
    private var selectedBlokIdSpinner: Int = 0
    private var selectedTPHIdSpinner: Int = 0
    private var selectedEstate: String = ""
    private var selectedEstateIdSpinner: Int = 0
    private var selectedBlok: String = ""
    private var selectedTPH: String = ""
    private var selectedTPHBackup: String = ""
    private var selectedKemandoran: String = ""
    private var selectedKemandoranLain: String = ""
    private var selectedPemanen: String = ""
    private var selectedPemanenLain: String = ""
    private var infoApp: String = ""
    private var selectedDivisiValue: Int? = null
    private var selectedDivisiValueBackup: Int? = null
    private var selectedBlokValue: Int? = null
    private var selectedBlokValueBackup: Int? = null
    private var selectedTahunTanamValue: String? = null
    private var selectedTahunTanamValueBackup: String? = null
    private var selectedTPHValue: Int? = null
    private var buahMasak = 0
    private var kirimPabrik = 0
    private var tbsDibayar = 0
    var persenMentah = 0f
    var persenLewatMasak = 0f
    var persenAbnormal = 0f
    var persenJjgKosong = 0f
    var persenMasak = 0f
    private lateinit var jjg_json: String
    private lateinit var inputMappings: List<Triple<LinearLayout, String, InputType>>
    private lateinit var datasetViewModel: DatasetViewModel
    private lateinit var panenViewModel: PanenViewModel
    private lateinit var mutuBuahViewModel: MutuBuahViewModel
    private lateinit var absensiViewModel: AbsensiViewModel
    private var regionalId: String? = null
    private var estateId: String? = null
    private var estateName: String? = null
    private var userName: String? = null
    private var userId: Int? = null
    private var jabatanUser: String? = null

    // This should be defined at the class level
    private data class TPHData(
        val count: Int,
        val jenisTPHId: Int,
        val limitTPH: String? = null,
        val workerNiks: List<String> = emptyList(),
        val blokKode: String? = null,
        val nomor: String? = null
    )

    private var panenStoredLocal: MutableMap<Int, TPHData> = mutableMapOf()
    private var radiusMinimum = 0F
    private var boundaryAccuracy = 0F
    private var isEmptyScannedTPH = true
    private var isTriggeredBtnScanned = false
    private var activityInitialized = false
    private val karyawanIdMap: MutableMap<String, Int> = mutableMapOf()
    private val kemandoranIdMap: MutableMap<String, Int> = mutableMapOf()
    private val karyawanLainIdMap: MutableMap<String, Int> = mutableMapOf()
    private val kemandoranLainIdMap: MutableMap<String, Int> = mutableMapOf()

    private val dateTimeCheckHandler = Handler(Looper.getMainLooper())
    private val dateTimeCheckRunnable = object : Runnable {
        override fun run() {
            checkDateTimeSettings()
            dateTimeCheckHandler.postDelayed(this, AppUtils.DATE_TIME_CHECK_INTERVAL)
        }
    }

    // Add these properties to your FeaturePanenTBSActivity class
    private var autoScanEnabled = false
    private val autoScanHandler = Handler(Looper.getMainLooper())
    private val autoScanInterval = 5000L // 5 seconds
    private lateinit var switchAutoScan: SwitchMaterial
    private lateinit var layoutAutoScan: LinearLayout
    private var jenisTPHListGlobal: List<JenisTPHModel> = emptyList()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_panen_tbs)
        //cek tanggal otomatis
        checkDateTimeSettings()
        initializeAutoScan()
    }

    private fun checkDateTimeSettings() {
        if (!AppUtils.isDateTimeValid(this)) {
            dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
            AppUtils.showDateTimeNetworkWarning(this)
        } else if (!activityInitialized) {
            initializeActivity()
            startPeriodicDateTimeChecking()
        }
    }

    private fun initializeActivity() {
        if (!activityInitialized) {
            activityInitialized = true
            setupUI()
        }
    }


    private fun startPeriodicDateTimeChecking() {
        dateTimeCheckHandler.postDelayed(dateTimeCheckRunnable, AppUtils.DATE_TIME_INITIAL_DELAY)

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI() {
        loadingDialog = LoadingDialog(this)
        prefManager = PrefManager(this)
        radiusMinimum = 200F
        boundaryAccuracy = 200F

        AppLogger.d("radiusMinimum $radiusMinimum")
        AppLogger.d("boundaryAccuracy $boundaryAccuracy")

        initViewModel()
        initUI()
        initializeJjgJson()

        regionalId = prefManager!!.regionalIdUserLogin
        estateId = prefManager!!.estateIdUserLogin
        estateName = prefManager!!.estateUserLogin
        userName = prefManager!!.nameUserLogin
        userId = prefManager!!.idUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin


        backButton.setOnClickListener {
            onBackPressed()
        }
        setupTitleEachGroupInput()
        setupHeader()
        if (featureName==AppUtils.ListFeatureNames.MutuBuah) {
            val tvDescFoto = findViewById<TextView>(R.id.tvDescFoto)
            tvDescFoto.text =  "Upload foto sebagai bukti pengisian form Mutu Buah (Minimal 1 Foto Selfie dan 1 foto TPH untuk simpan)"
        }

        infoApp = AppUtils.getDeviceInfo(this@FeaturePanenTBSActivity).toString()

        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadingDialog.show()
                loadingDialog.setMessage("Loading data...")
                delay(1000)
            }

            try {
                val estateIdStr = estateId?.trim()

                if (!estateIdStr.isNullOrEmpty() && estateIdStr.toIntOrNull() != null) {
                    val estateIdInt = estateIdStr.toInt()

                    if (featureName != AppUtils.ListFeatureNames.MutuBuah){
                        val panenDeferred = CompletableDeferred<List<PanenEntityWithRelations>>()

                        panenViewModel.getAllTPHHasBeenSelected()
                        delay(100)

                        withContext(Dispatchers.Main) {
                            panenViewModel.activePanenList.observe(this@FeaturePanenTBSActivity) { list ->
                                val tphDataMap = mutableMapOf<Int, TPHData>()

                                list?.forEach { panen ->
                                    val tphId = panen.tph?.id
                                    val jenisTPHId = panen.tph?.jenis_tph_id?.toInt()
                                    val limitTPH = panen.tph?.limit_tph
                                    val workerNiks =
                                        panen.panen.karyawan_nik?.split(",")?.map { it.trim() }
                                            ?: emptyList()
                                    val blokKode = panen.tph!!.blok_kode
                                    val nomor = panen.tph.nomor

                                    if (tphId != null && jenisTPHId != null) {
                                        val existingData = tphDataMap[tphId]
                                        if (existingData != null) {
                                            // Merge worker NIKs and increment count
                                            val mergedNiks =
                                                (existingData.workerNiks + workerNiks).distinct()
                                            tphDataMap[tphId] = existingData.copy(
                                                count = existingData.count + 1,
                                                workerNiks = mergedNiks,
                                                blokKode = blokKode,
                                                nomor = nomor
                                            )
                                        } else {
                                            // Create new entry for this TPH
                                            tphDataMap[tphId] = TPHData(
                                                count = 1,
                                                jenisTPHId = jenisTPHId,
                                                limitTPH = limitTPH!!,
                                                workerNiks = workerNiks,
                                                blokKode = blokKode,
                                                nomor = nomor
                                            )
                                        }
                                    }
                                }

                                panenStoredLocal.clear()
                                panenStoredLocal.putAll(tphDataMap)

                                panenDeferred.complete(list ?: emptyList())
                            }
                        }

                        val jenisTPHDeferred = CompletableDeferred<List<JenisTPHModel>>()

                        panenViewModel.getAllJenisTPH()
                        delay(100)

                        withContext(Dispatchers.Main) {
                            panenViewModel.jenisTPHList.observe(this@FeaturePanenTBSActivity) { list ->
                                jenisTPHListGlobal = list ?: emptyList()
                                jenisTPHDeferred.complete(list ?: emptyList())
                            }
                        }

                        val absensiDeferred = CompletableDeferred<List<AbsensiKemandoranRelations>>()

                        absensiViewModel.loadActiveAbsensi()
                        delay(100)

                        withContext(Dispatchers.Main) {
                            absensiViewModel.activeAbsensiList.observe(this@FeaturePanenTBSActivity) { absensiWithRelations ->
                                val absensiData = absensiWithRelations ?: emptyList()

                                // Store the absensi models in the global variable
                                absensiList = absensiData

                                // Extract all NIKs of present karyawan from all absensi entries
                                val newPresentNikSet = mutableSetOf<String>()

                                absensiData.forEach { absensiRelation ->
                                    val absensi = absensiRelation.absensi
                                    // Split the comma-separated NIK string and add each NIK to the set
                                    val niks = absensi.karyawan_msk_nik.split(",")
                                    newPresentNikSet.addAll(niks.filter {
                                        it.isNotEmpty() && it.trim().isNotEmpty()
                                    })
                                }

                                // Update the global set
                                presentNikSet = newPresentNikSet

                                AppLogger.d("Found ${presentNikSet.size} present NIKs from absensi data")
                                absensiDeferred.complete(absensiData)
                            }
                        }

                        val karyawanDeferred = CompletableDeferred<List<KaryawanModel>>()

                        panenViewModel.getAllKaryawan() // This should be added to your ViewModel
                        delay(100)

                        withContext(Dispatchers.Main) {
                            panenViewModel.allKaryawanList.observe(this@FeaturePanenTBSActivity) { list ->
                                val allKaryawan = list ?: emptyList()

                                // Get user's afdeling ID (which is same as divisi)
                                val userAfdelingId = prefManager!!.afdelingIdUserLogin?.toInt()
                                AppLogger.d("User's afdeling ID: $userAfdelingId")

                                // Only filter if presentNikSet has values
                                if (presentNikSet.isNotEmpty()) {
                                    // Filter to get only present karyawan
                                    val presentKaryawan = allKaryawan.filter { karyawan ->
                                        karyawan.nik != null && presentNikSet.contains(karyawan.nik)
                                    }

                                    // Filter by divisi - same divisi as user goes to karyawanList, others go to karyawanLainList
                                    if (userAfdelingId != null) {
                                        // Karyawan from same divisi as user
                                        karyawanList = presentKaryawan.filter { karyawan ->
                                            karyawan.divisi == userAfdelingId
                                        }

                                        // Karyawan from other divisi
                                        karyawanLainList = presentKaryawan.filter { karyawan ->
                                            karyawan.divisi != userAfdelingId
                                        }
                                    } else {

                                        karyawanList = presentKaryawan
                                        karyawanLainList = emptyList()
                                    }

                                    AppLogger.d("Total karyawan: ${allKaryawan.size}")
                                    AppLogger.d("Filtered to present karyawan: ${presentKaryawan.size}")
                                    AppLogger.d("Same divisi karyawan (karyawanList): ${karyawanList.size}")
                                    AppLogger.d("Other divisi karyawan (karyawanLainList): ${karyawanLainList.size}")

                                    // Complete the deferred with all present karyawan
                                    karyawanDeferred.complete(presentKaryawan)
                                } else {
                                    karyawanList = emptyList()
                                    karyawanLainList = emptyList()
                                    karyawanDeferred.complete(allKaryawan)
                                }
                            }
                        }

                        val allKaryawan = karyawanDeferred.await()

                        if (allKaryawan.isNotEmpty()) {
                            // Setup the karyawan dropdown
                            val nameCounts = mutableMapOf<String, Int>()
                            allKaryawan.forEach {
                                it.nama?.trim()?.let { nama ->
                                    nameCounts[nama] = (nameCounts[nama] ?: 0) + 1
                                }
                            }

                            allKaryawan.forEach {
                                it.nama?.trim()?.let { nama ->
                                    val key = if (nameCounts[nama]!! > 1) {
                                        "$nama - ${it.nik}"
                                    } else {
                                        nama
                                    }
                                    karyawanIdMap[key] = it.id!!
                                    if (it.kemandoran_id != null) {
                                        kemandoranIdMap[key] = it.kemandoran_id!!
                                    }
                                }
                            }
                        }
                    }

                    val tphOtomatisDeferred = async {
                        try {
                            val estateAbbr = prefManager!!.estateUserLogin
                            datasetViewModel.getTphOtomatisByEstate(estateAbbr!!)
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching tph_otomatis: ${e.message}")
                            null // Return null if error occurs
                        }
                    }

                    tph_otomatis_estate = tphOtomatisDeferred.await()

                    val divisiDeferred = async {
                        try {
                            datasetViewModel.getDivisiList(estateIdInt)
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching divisiList: ${e.message}")
                            emptyList() // Return an empty list to prevent crash
                        }
                    }

                    divisiList = divisiDeferred.await()

                    if (divisiList.isNullOrEmpty()) {
                        throw Exception("Periksa kembali dataset TPH dengan melakukan Sinkronisasi Data!")
                    }

                    datasetViewModel.getAllEstates()
                    delay(100)

                    withContext(Dispatchers.Main) {
                        datasetViewModel.allEstatesList.observe(this@FeaturePanenTBSActivity) { list ->
                            val allEstates = list ?: emptyList()
                            estateList = allEstates
                        }
                    }

                    if (estateList.isNullOrEmpty()) {
                        throw Exception("Periksa kembali dataset estate dengan melakukan Sinkronisasi Data!")
                    }

                    if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {
                        val departmentInfoDeferred = CompletableDeferred<Map<String, String>>()

                        withContext(Dispatchers.Main) {
                            datasetViewModel.distinctDeptInfoList.observe(this@FeaturePanenTBSActivity) { list ->
                                val distinctDeptInfos = list ?: emptyList()
                                val deptInfoMap =
                                    distinctDeptInfos.associate { it.dept to it.dept_abbr }
                                masterDeptInfoMap = deptInfoMap
                                departmentInfoDeferred.complete(deptInfoMap)
                            }

                            // Trigger the data fetch on the main thread
                            datasetViewModel.getDistinctMasterDeptInfo()
                        }

                        // Wait for the department info to be loaded
                        masterDeptInfoMap = departmentInfoDeferred.await()
                    }

                    // KEMANDORAN SETUP - Added proper error handling
                    try {
                        // Get the user's logged-in afdeling ID from prefManager
                        val userAfdelingId = prefManager!!.afdelingIdUserLogin?.toInt()
                        AppLogger.d("User's logged-in afdeling ID: $userAfdelingId")

                        if (userAfdelingId != null) {
                            // Get all divisi IDs except the user's afdeling
                            val allIdAfdeling = try {
                                divisiList.map { it.divisi }
                            } catch (e: Exception) {
                                AppLogger.e("Error mapping allIdAfdeling: ${e.message}")
                                throw Exception("Error mapping afdeling data: ${e.message}")
                            }

                            val otherDivisiIds = try {
                                allIdAfdeling.filter { divisiId ->
                                    userAfdelingId != divisiId
                                }
                            } catch (e: Exception) {
                                AppLogger.e("Error filtering otherDivisiIds: ${e.message}")
                                throw Exception("Error filtering afdeling data: ${e.message}")
                            }

                            // Load kemandoran data based on user's afdeling
                            if (featureName != AppUtils.ListFeatureNames.AsistensiEstateLain) {
                                val kemandoranDeferred = async {
                                    try {
                                        datasetViewModel.getKemandoranEstateExcept(
                                            estateId!!.toInt(),
                                            otherDivisiIds as List<Int>
                                        )
                                    } catch (e: Exception) {
                                        AppLogger.e("Error fetching kemandoranList: ${e.message}")
                                        throw Exception("Error fetching kemandoran data: ${e.message}")
                                    }
                                }
                                kemandoranList = kemandoranDeferred.await()
                            }

                            val kemandoranLainDeferred = async {
                                try {
                                    datasetViewModel.getKemandoranEstate(estateId!!.toInt())
                                } catch (e: Exception) {
                                    AppLogger.e("Error fetching kemandoranLainList: ${e.message}")
                                    throw Exception("Error fetching kemandoran lain data: ${e.message}")
                                }
                            }

                            if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {
                                kemandoranList = kemandoranLainDeferred.await()
                            } else {
                                kemandoranLainList = kemandoranLainDeferred.await()
                            }

                            // Setup the kemandoran spinners on main thread
                            withContext(Dispatchers.Main) {
                                try {
                                    val kemandoranNames = kemandoranList.map { it.nama }

                                    AppLogger.d("kemandoranNames $kemandoranNames")
                                    setupSpinnerView(
                                        layoutKemandoran,
                                        if (kemandoranNames.isNotEmpty()) kemandoranNames as List<String> else emptyList()
                                    )

                                    if (featureName != AppUtils.ListFeatureNames.AsistensiEstateLain) {
                                        val kemandoranLainListFiltered =
                                            kemandoranLainList.filter { kemandoran ->
                                                userAfdelingId != kemandoran.divisi // or whatever property holds the afdeling/divisi ID
                                            }

                                        val kemandoranLainListNames =
                                            kemandoranLainListFiltered.map { it.nama }
                                        setupSpinnerView(
                                            layoutKemandoranLain,
                                            if (kemandoranLainListNames.isNotEmpty()) kemandoranLainListNames as List<String> else emptyList()
                                        )
                                    }

                                    AppLogger.d("Kemandoran spinners setup completed based on user's afdeling")
                                } catch (e: Exception) {
                                    AppLogger.e("Error setting up kemandoran spinners: ${e.message}")
                                    throw Exception("Error setting up kemandoran spinners: ${e.message}")
                                }
                            }
                        } else {
                            throw Exception("User afdeling ID is null - cannot load kemandoran data")
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Error in kemandoran setup: ${e.message}")
                        throw Exception("Kemandoran setup failed: ${e.message}")
                    }
                }

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    setupLayout()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    val errorMessage = e.message?.let { "1. $it" } ?: "1. Unknown error"

                    val estateInfo = estateId?.takeIf { it.isBlank() }
                        ?.let { "2. ID Estate User Login: \"$it\"" }

                    // Combine messages dynamically (avoid extra \n\n if estateInfo is null)
                    val fullMessage = listOfNotNull(errorMessage, estateInfo).joinToString("\n\n")

                    AppLogger.e("Error fetching data: ${e.message}")

                    AlertDialogUtility.withSingleAction(
                        this@FeaturePanenTBSActivity,
                        stringXML(R.string.al_back),
                        stringXML(R.string.al_failed_fetch_data),
                        fullMessage,
                        "warning.json",
                        R.color.colorRedDark
                    ) {
                        finish()
                    }
                }
            }


        }
        findViewById<View>(android.R.id.content).setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val currentFocus = currentFocus
                if (currentFocus is EditText) {
                    imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
                    currentFocus.clearFocus()
                }
            }
            false
        }

//        masterEstateChoice.observe(this) { selections ->
//            updateDownloadMasterDataButtonText(selections)
//        }

        mbSaveDataPanenTBS.setOnClickListener {
            if (validateAndShowErrors()) {
                AlertDialogUtility.withTwoActions(
                    this,
                    "Simpan Data",
                    getString(R.string.confirmation_dialog_title),
                    getString(R.string.confirmation_dialog_description),
                    "warning.json",
                    function = {
                        lifecycleScope.launch(Dispatchers.Main) {
                            try {

                                val selectedPemanen = selectedPemanenAdapter.getSelectedWorkers()
                                val selectedPemanenLain =
                                    selectedPemanenLainAdapter.getSelectedWorkers()

                                val allSelectedWorkers = (selectedPemanen + selectedPemanenLain)
                                val uniqueWorkersByNik = mutableMapOf<String, Worker>()

                                allSelectedWorkers.forEach { worker ->
                                    val nik = if (worker.name.contains(" - ")) {
                                        // Find the LAST occurrence of " - " to extract NIK
                                        val lastDashIndex = worker.name.lastIndexOf(" - ")
                                        if (lastDashIndex != -1) {
                                            val potentialNik =
                                                worker.name.substring(lastDashIndex + 3).trim()
                                            // Accept any non-empty string as NIK (including mobile/string NIKs)
                                            if (potentialNik.isNotEmpty()) {
                                                potentialNik
                                            } else {
                                                worker.name // fallback if last segment is empty
                                            }
                                        } else {
                                            worker.name
                                        }
                                    } else {
                                        worker.name
                                    }

                                    // Only keep the first occurrence of each NIK
                                    if (!uniqueWorkersByNik.containsKey(nik)) {
                                        uniqueWorkersByNik[nik] = worker
                                    }
                                }

                                val uniqueWorkers = uniqueWorkersByNik.values.toList()

                                AppLogger.d("Unique workers after deduplication: ${uniqueWorkers.map { it.name }}")

                                // Helper function to extract name without NIK
                                fun getNameWithoutNik(fullName: String): String {
                                    val lastDashIndex = fullName.lastIndexOf(" - ")
                                    return if (lastDashIndex != -1) {
                                        val potentialNik =
                                            fullName.substring(lastDashIndex + 3).trim()
                                        // Only remove if the last part is not empty (could be string/mobile NIK)
                                        if (potentialNik.isNotEmpty()) {
                                            fullName.substring(0, lastDashIndex).trim()
                                        } else {
                                            fullName
                                        }
                                    } else {
                                        fullName
                                    }
                                }

// Now process the unique workers
                                val idKaryawanList = uniqueWorkers.mapNotNull { worker ->
                                    var id = karyawanIdMap[worker.name]

                                    if (id == null && worker.name.contains(" - ")) {
                                        val baseName = getNameWithoutNik(worker.name)
                                        id = karyawanIdMap[baseName]
                                    }

                                    if (id == null && !worker.name.contains(" - ")) {
                                        val possibleKey =
                                            karyawanIdMap.keys.find { it.startsWith("${worker.name} - ") }
                                        if (possibleKey != null) {
                                            id = karyawanIdMap[possibleKey]
                                        }
                                    }

                                    // If still not found, try karyawanLainIdMap
                                    if (id == null) {
                                        id = karyawanLainIdMap[worker.name]
                                        if (id == null && worker.name.contains(" - ")) {
                                            val baseName = getNameWithoutNik(worker.name)
                                            id = karyawanLainIdMap[baseName]
                                        }
                                        if (id == null && !worker.name.contains(" - ")) {
                                            val possibleKey =
                                                karyawanLainIdMap.keys.find { it.startsWith("${worker.name} - ") }
                                            if (possibleKey != null) {
                                                id = karyawanLainIdMap[possibleKey]
                                            }
                                        }
                                    }

                                    id
                                }

                                val kemandoranIdList = uniqueWorkers.mapNotNull { worker ->
                                    var id = kemandoranIdMap[worker.name]

                                    if (id == null && worker.name.contains(" - ")) {
                                        val baseName = getNameWithoutNik(worker.name)
                                        id = kemandoranIdMap[baseName]
                                    }

                                    if (id == null && !worker.name.contains(" - ")) {
                                        val possibleKey =
                                            kemandoranIdMap.keys.find { it.startsWith("${worker.name} - ") }
                                        if (possibleKey != null) {
                                            id = kemandoranIdMap[possibleKey]
                                        }
                                    }

                                    // If still not found, try kemandoranLainIdMap
                                    if (id == null) {
                                        id = kemandoranLainIdMap[worker.name]
                                        if (id == null && worker.name.contains(" - ")) {
                                            val baseName = getNameWithoutNik(worker.name)
                                            id = kemandoranLainIdMap[baseName]
                                        }
                                        if (id == null && !worker.name.contains(" - ")) {
                                            val possibleKey =
                                                kemandoranLainIdMap.keys.find { it.startsWith("${worker.name} - ") }
                                            if (possibleKey != null) {
                                                id = kemandoranLainIdMap[possibleKey]
                                            }
                                        }
                                    }

                                    id
                                }

                                val selectedNamaList = uniqueWorkers.mapNotNull { worker ->
                                    var nama = karyawanNamaMap[worker.name]

                                    if (nama == null && worker.name.contains(" - ")) {
                                        val nameWithoutNik = getNameWithoutNik(worker.name)
                                        nama = karyawanNamaMap[nameWithoutNik]
                                    }

                                    if (nama == null && !worker.name.contains(" - ")) {
                                        val possibleKey =
                                            karyawanNamaMap.keys.find { it.startsWith("${worker.name} - ") }
                                        if (possibleKey != null) {
                                            nama = karyawanNamaMap[possibleKey]
                                        } else {
                                            nama = worker.name
                                        }
                                    }

                                    // If still not found, try karyawanNamaLainMap
                                    if (nama == null) {
                                        nama = karyawanNamaLainMap[worker.name]
                                        if (nama == null && worker.name.contains(" - ")) {
                                            val nameWithoutNik = getNameWithoutNik(worker.name)
                                            nama = karyawanNamaLainMap[nameWithoutNik]
                                        }
                                        if (nama == null && !worker.name.contains(" - ")) {
                                            val possibleKey =
                                                karyawanNamaLainMap.keys.find { it.startsWith("${worker.name} - ") }
                                            if (possibleKey != null) {
                                                nama = karyawanNamaLainMap[possibleKey]
                                            } else {
                                                nama =
                                                    getNameWithoutNik(worker.name) // Use the clean name without NIK
                                            }
                                        }
                                    }

                                    nama
                                }

                                val selectedNikList = uniqueWorkers.mapNotNull { worker ->
                                    if (worker.name.contains(" - ")) {
                                        // Find the LAST occurrence of " - " to extract NIK
                                        val lastDashIndex = worker.name.lastIndexOf(" - ")
                                        if (lastDashIndex != -1) {
                                            val potentialNik =
                                                worker.name.substring(lastDashIndex + 3).trim()
                                            // Return any non-empty string as NIK (supports mobile/string NIKs)
                                            if (potentialNik.isNotEmpty()) {
                                                potentialNik
                                            } else {
                                                null
                                            }
                                        } else {
                                            null
                                        }
                                    } else {
                                        null
                                    }
                                }

                                val uniqueNamaPemanen = selectedNamaList.joinToString(",")
                                val uniqueNikPemanen = selectedNikList.joinToString(",")
                                val uniqueIdKaryawan =
                                    idKaryawanList.map { it.toString() }.joinToString(",")
                                val uniqueKemandoranId =
                                    kemandoranIdList.map { it.toString() }.joinToString(",")

                                AppLogger.d("Final counts:")
                                AppLogger.d("Names: ${selectedNamaList.size}")
                                AppLogger.d("NIKs: ${selectedNikList.size}")
                                AppLogger.d("IDs: ${idKaryawanList.size}")
                                AppLogger.d("Kemandoran IDs: ${kemandoranIdList.size}")
                                val photoFilesString = photoFiles.joinToString(";")
                                val photoFilesSelfieString = photoFilesSelfie.joinToString(";")
                                val komentarFotoString = komentarFoto.joinToString(";")
                                    .takeIf { it.isNotBlank() && it != ";" && !it.matches(Regex("^;+$")) }


                                AppLogger.d("tph id sebelum simpan $selectedTPHValue")
//                                selectedTPHValue = null

                                // Only check TPH validation if blokBanjir is 1
                                if (blokBanjir == 1) {
                                    if (selectedTPHValue == null ||
                                        selectedTPHValue.toString().isEmpty() ||
                                        selectedTPHValue.toString().isBlank() ||
                                        selectedTPHValue.toString() == "null" ||
                                        selectedTPHValue.toString() == "0"
                                    ) {
                                        // Use backup values if main values are null
                                        val tphToFind = selectedTPH ?: selectedTPHBackup
                                        val divisiToUse =
                                            selectedDivisiValue ?: selectedDivisiValueBackup
                                        val blokToUse = selectedBlokValue ?: selectedBlokValueBackup
                                        val tahunTanamToUse =
                                            selectedTahunTanamValue ?: selectedTahunTanamValueBackup

                                        val estateIdToUse =
                                            if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {
                                                selectedEstate.toIntOrNull()
                                            } else {
                                                estateId?.toIntOrNull()
                                            }

                                        // Find the TPH object from the list using backup values when needed
                                        val foundTPH = tphList.find {
                                            it.dept == estateIdToUse && // Using conditional estate ID
                                                    it.divisi == divisiToUse &&
                                                    it.blok == blokToUse &&
                                                    it.tahun == tahunTanamToUse &&
                                                    it.nomor == tphToFind
                                        }

                                        // Set selectedTPHValue to the found TPH's ID
                                        selectedTPHValue = foundTPH?.id

                                        AppLogger.d("tph id ketika trouble setelah simpan $selectedTPHValue")

                                        if (selectedTPHValue == null) {
                                            AlertDialogUtility.withSingleAction(
                                                this@FeaturePanenTBSActivity,
                                                "Kembali",
                                                "TPH Tidak Ditemukan",
                                                getString(R.string.al_no_tph_not_found_when_save),
                                                "warning.json",
                                                R.color.colorRedDark
                                            ) {
                                                layoutTahunTanam.findViewById<TextView>(R.id.tvErrorFormPanenTBS).visibility =
                                                    View.VISIBLE
                                                layoutTahunTanam.findViewById<TextView>(R.id.tvErrorFormPanenTBS).text =
                                                    "Silakan melakukan pemilihan ulang!"
                                                layoutTahunTanam.findViewById<MaterialCardView>(R.id.MCVSpinner).strokeColor =
                                                    ContextCompat.getColor(
                                                        this@FeaturePanenTBSActivity,
                                                        R.color.colorRedDark
                                                    )
                                                layoutBlok.findViewById<TextView>(R.id.tvErrorFormPanenTBS).visibility =
                                                    View.VISIBLE
                                                layoutBlok.findViewById<TextView>(R.id.tvErrorFormPanenTBS).text =
                                                    "Silakan melakukan pemilihan ulang!"
                                                layoutBlok.findViewById<MaterialCardView>(R.id.MCVSpinner).strokeColor =
                                                    ContextCompat.getColor(
                                                        this@FeaturePanenTBSActivity,
                                                        R.color.colorRedDark
                                                    )
                                                layoutNoTPH.findViewById<TextView>(R.id.tvErrorFormPanenTBS).visibility =
                                                    View.VISIBLE
                                                layoutNoTPH.findViewById<TextView>(R.id.tvErrorFormPanenTBS).text =
                                                    "Silakan melakukan pemilihan ulang!"
                                                layoutNoTPH.findViewById<MaterialCardView>(R.id.MCVSpinner).strokeColor =
                                                    ContextCompat.getColor(
                                                        this@FeaturePanenTBSActivity,
                                                        R.color.colorRedDark
                                                    )

                                                val scPanen = findViewById<ScrollView>(R.id.scPanen)
                                                scPanen.fullScroll(ScrollView.FOCUS_UP)
                                            }

                                            return@launch
                                        }
                                    }
                                }

                                val tph_id = selectedTPHValue?.toString() ?: ""
                                val date_created = SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss",
                                    Locale.getDefault()
                                ).format(Date())

                                val result = withContext(Dispatchers.IO) {
                                    if (featureName == AppUtils.ListFeatureNames.MutuBuah) {
                                        mutuBuahViewModel.saveDataMutuBuah(
                                            tph_id = tph_id,
                                            date_created = date_created,
                                            foto = photoFilesString,
                                            komentar = komentarFotoString ?: "",
                                            lat = finalLat ?: 0.0,
                                            lon = finalLon ?: 0.0,
                                            info = infoApp ?: "",
                                            nomorPemanenInput = nomorPemanenInput.toInt(),
                                            jjgPanen = jumTBS,
                                            jjgMasak = buahMasak,
                                            jjgMentah = bMentah,
                                            jjgLewatMasak = bLewatMasak,
                                            jjgKosong = jjgKosong,
                                            jjgSeranganTikus = seranganTikus,
                                            jjgPanjang = tangkaiPanjang,
                                            jjgTidakVcut = tidakVCut,
                                            jjgBayar = tbsDibayar,
                                            jjgKirim = kirimPabrik,
                                            created_by = userId!!,
                                            jjgAbnormal = abnormal,
                                            foto_selfie = photoFilesSelfieString,
                                            createdName = userName!!)
                                    }else{
                                        panenViewModel.saveDataPanen(
                                            tph_id = tph_id,
                                            date_created = date_created,
                                            created_by = userId!!,  // Prevent crash if userId is null
                                            karyawan_id = uniqueIdKaryawan,
                                            kemandoran_id = uniqueKemandoranId,
                                            karyawan_nik = uniqueNikPemanen,
                                            karyawan_nama = uniqueNamaPemanen,
                                            jjg_json = jjg_json,
                                            foto = photoFilesString,
                                            komentar = komentarFotoString ?: "",
                                            asistensi = if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) 2 else (asistensi
                                                ?: 0),
                                            lat = finalLat ?: 0.0,
                                            lon = finalLon ?: 0.0,
                                            jenis_panen = selectedTipePanen.toIntOrNull()
                                                ?: 0, // Avoid NumberFormatException
                                            ancakInput = ancakInput.toInt(),
                                            nomorPemanenInput = nomorPemanenInput.toInt(),
                                            info = infoApp ?: "",
                                            archive = 0,
                                            blokBanjir = blokBanjir
                                        )
                                    }
                                }

                                when (result) {
                                    is AppRepository.SaveResultPanen.Success,
                                    is AppRepository.SaveResultMutuBuah.Success  -> {
                                        playSound(R.raw.berhasil_simpan)
                                        AlertDialogUtility.withSingleAction(
                                            this@FeaturePanenTBSActivity,
                                            stringXML(R.string.al_back),
                                            stringXML(R.string.al_success_save_local),
                                            stringXML(R.string.al_description_success_save_local),
                                            "success.json",
                                            R.color.greenDefault
                                        ) {
                                            val tphId = selectedTPHValue!!.toInt()
                                            val tphData = panenStoredLocal[tphId]

                                            val currentWorkerNiks =
                                                uniqueNikPemanen.split(",").map { it.trim() }
                                                    .filter { it.isNotEmpty() }

                                            if (tphData != null) {
                                                val mergedNiks =
                                                    (tphData.workerNiks + currentWorkerNiks).distinct()
                                                panenStoredLocal[tphId] = tphData.copy(
                                                    count = tphData.count + 1,
                                                    workerNiks = mergedNiks,
                                                    blokKode = selectedBlok,
                                                    nomor = selectedTPH
                                                )
                                            } else {
                                                // Create new entry
                                                val jenisTPHId = selectedTPHJenisId ?: 0
                                                val limitTPH =
                                                    tphList.find { it.id == tphId }?.limit_tph

                                                panenStoredLocal[tphId] = TPHData(
                                                    count = 1,
                                                    jenisTPHId = jenisTPHId,
                                                    limitTPH = limitTPH,
                                                    workerNiks = currentWorkerNiks,
                                                    blokKode = selectedBlok,
                                                    nomor = selectedTPH
                                                )
                                            }

                                            resetFormAfterSaveData()
                                        }
                                    }
                                    is AppRepository.SaveResultPanen.Error -> {
                                        AlertDialogUtility.withSingleAction(
                                            this@FeaturePanenTBSActivity,
                                            stringXML(R.string.al_back),
                                            stringXML(R.string.al_failed_save_local),
                                            "${stringXML(R.string.al_description_failed_save_local)} : ${result.exception.message}",
                                            "warning.json",
                                            R.color.colorRedDark
                                        ) {}
                                    }
                                    is AppRepository.SaveResultMutuBuah.Error -> {
                                        AlertDialogUtility.withSingleAction(
                                            this@FeaturePanenTBSActivity,
                                            stringXML(R.string.al_back),
                                            stringXML(R.string.al_failed_save_local),
                                            "${stringXML(R.string.al_description_failed_save_local)} : ${result.exception.message}",
                                            "warning.json",
                                            R.color.colorRedDark
                                        ) {}
                                    }


                                }

                            } catch (e: Exception) {
                                AppLogger.d("Unexpected error: ${e.message}")

                                AlertDialogUtility.withSingleAction(
                                    this@FeaturePanenTBSActivity,
                                    stringXML(R.string.al_back),
                                    stringXML(R.string.al_failed_save_local),
                                    "${stringXML(R.string.al_description_failed_save_local)} : ${e.message}",
                                    "warning.json",
                                    R.color.colorRedDark
                                ) {}
                            }

                        }
                    },
                    cancelFunction = {
                    }
                )
            }
        }
    }

    private fun initializeJjgJson() {
        jjg_json = JSONObject().apply {
            put("TO", jumTBS)
            put("UN", bMentah)
            put("OV", bLewatMasak)
            put("EM", jjgKosong)
            put("AB", abnormal)
            put("RA", seranganTikus)
            put("LO", tangkaiPanjang)
            put("TI", tidakVCut)
            put("RI", buahMasak)
            put("KP", kirimPabrik)
            put("PA", tbsDibayar)
        }.toString() // Convert JSON object to string and store in jjg_json
    }


    private fun setupHeader() {
        featureName = intent.getStringExtra("FEATURE_NAME").toString()
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        val userSection = findViewById<TextView>(R.id.userSection)
        val titleAppNameAndVersion = findViewById<TextView>(R.id.titleAppNameAndVersionFeature)
        val lastUpdateText = findViewById<TextView>(R.id.lastUpdate)
        val locationSection = findViewById<LinearLayout>(R.id.locationSection)

        locationSection.visibility = View.VISIBLE

        AppUtils.setupUserHeader(
            userName = userName,
            userSection = userSection,
            featureName = featureName,
            tvFeatureName = tvFeatureName,
            prefManager = prefManager,
            lastUpdateText = lastUpdateText,
            titleAppNameAndVersionText = titleAppNameAndVersion,
            context = this
        )
    }

    private fun updateCounterTextViews() {
        findViewById<TextView>(R.id.tvCounterBuahMasak).text = "$jumTBS Buah"
        findViewById<TextView>(R.id.tvCounterKirimPabrik).text = "$kirimPabrik Buah"
        findViewById<TextView>(R.id.tvCounterTBSDibayar).text = "$tbsDibayar Buah"
//        findViewById<TextView>(R.id.tvPercentBuahMasak).text = "($persenMasak)%"
    }


    private fun initUI() {
        backButton = findViewById<ImageView>(R.id.btn_back)

        alertCardScanRadius = findViewById(R.id.alertCardScanRadius)
        alertTvScannedRadius = findViewById(R.id.alertTvScannedRadius)
        btnScanTPHRadius = findViewById(R.id.btnScanTPHRadius)
        tphScannedResultRecyclerView = findViewById(R.id.tphScannedResultRecyclerView)
        titleScannedTPHInsideRadius = findViewById(R.id.titleScannedTPHInsideRadius)
        descScannedTPHInsideRadius = findViewById(R.id.descScannedTPHInsideRadius)
        emptyScannedTPHInsideRadius = findViewById(R.id.emptyScanTPHInsideRadius)
        progressBarScanTPHManual = findViewById(R.id.progressBarScanTPHManual)
        progressBarScanTPHAuto = findViewById(R.id.progressBarScanTPHAuto)
        mbSaveDataPanenTBS = findViewById(R.id.mbSaveDataPanenTBS)

        layoutEstate = findViewById(R.id.layoutEstate)
        layoutAncak = findViewById(R.id.layoutAncak)
        layoutNomorPemanen = findViewById(R.id.layoutNomorPemanen)
        layoutPemanen = findViewById(R.id.layoutPemanen)
        layoutPemanenLain = findViewById(R.id.layoutPemanenLain)
        layoutBlok = findViewById(R.id.layoutBlok)
        layoutKemandoran = findViewById(R.id.layoutKemandoran)
        layoutKemandoranLain = findViewById(R.id.layoutKemandoranLain)
        layoutNoTPH = findViewById(R.id.layoutNoTPH)
        layoutSelAsistensi = findViewById(R.id.layoutSelAsistensi)
        layoutTipePanen = findViewById(R.id.layoutTipePanen)
        layoutTahunTanam = findViewById(R.id.layoutTahunTanam)
        layoutMasterTPH = findViewById(R.id.layoutMasterTPH)

        tvErrorScannedNotSelected = findViewById(R.id.tvErrorScannedNotSelected)
    }


    private fun resetFormAfterSaveData() {

        selectedPemanenAdapter.clearAllWorkers()
        selectedPemanenLainAdapter.clearAllWorkers()

        if (blokBanjir == 0) {
            tphScannedResultRecyclerView.visibility = View.GONE
            titleScannedTPHInsideRadius.visibility = View.GONE
            descScannedTPHInsideRadius.visibility = View.GONE
            emptyScannedTPHInsideRadius.visibility = View.GONE
        }

        val kemandoranLayout = findViewById<LinearLayout>(R.id.layoutKemandoran)
        val kemandoranLainLayout = findViewById<LinearLayout>(R.id.layoutKemandoranLain)
        val tahunTanamLayout = findViewById<LinearLayout>(R.id.layoutTahunTanam)

        // Reset main kemandoran filter container
        val kemandoranFilterContainer =
            kemandoranLayout.findViewById<MaterialCardView>(R.id.filter_container_pertanyaan_layout)
        if (kemandoranFilterContainer != null && kemandoranFilterContainer.visibility == View.VISIBLE) {
            kemandoranFilterContainer.visibility = View.GONE
        }

        if (featureName != AppUtils.ListFeatureNames.AsistensiEstateLain) {
            val kemandoranLainFilterContainer =
                kemandoranLainLayout.findViewById<MaterialCardView>(R.id.filter_container_pertanyaan_layout)
            if (kemandoranLainFilterContainer != null && kemandoranLainFilterContainer.visibility == View.VISIBLE) {
                kemandoranLainFilterContainer.visibility = View.GONE
            }
        }


        var divisiNames = emptyList<String>()
        if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {
            divisiNames =
                afdelingList.sortedBy { it.abbr }.mapNotNull { it.abbr }
        } else {
            divisiNames =
                divisiList.sortedBy { it.divisi_abbr }.mapNotNull { it.divisi_abbr }
        }

        setupSpinnerView(findViewById(R.id.layoutAfdeling), divisiNames)

        val afdelingLayout = findViewById<LinearLayout>(R.id.layoutAfdeling)
        val afdelingSpinner = afdelingLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)

        // First try to find the position by name (this handles reordering)
        var newPosition = divisiNames.indexOf(selectedAfdeling)

        // If we can't find the name or it's invalid, fall back to the stored index
        if (newPosition < 0) {
            newPosition = selectedAfdelingIdSpinner
        }

        // Ensure the position is valid for the current list
        val safePosition = when {
            newPosition < 0 -> 0  // Default to first item if negative
            newPosition >= divisiNames.size -> 0  // Default to first if beyond list bounds
            else -> newPosition  // Use the calculated position if it's valid
        }
        afdelingSpinner.setSelectedIndex(safePosition)
        if (safePosition >= 0 && safePosition < divisiNames.size) {
            val selectedItem = divisiNames[safePosition]
            selectedAfdelingIdSpinner = safePosition
            selectedAfdeling = selectedItem
            handleItemSelection(afdelingLayout, safePosition, selectedItem)
        }

        // Handle tahun tanam selection similarly
        val tahunTanamNames = if (blokList.isNotEmpty()) {
            blokList.mapNotNull { it.tahun }.distinct().sortedBy { it.toIntOrNull() }
        } else {
            emptyList()
        }

        setupSpinnerView(tahunTanamLayout, tahunTanamNames)
        val tahunTanamSpinner = tahunTanamLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)


        val tahunTanamPosition = tahunTanamNames.indexOf(selectedTahunTanamValue)
        AppLogger.d("Found tahunTanamPosition: $tahunTanamPosition for name: $selectedTahunTanamValue")

        // Only proceed with selection if we have a valid position and non-empty selection
        if (tahunTanamPosition >= 0 && selectedTahunTanamValue!!.isNotEmpty() && tahunTanamNames.isNotEmpty()) {
            try {
                // Force selection by posting to the main thread
                tahunTanamSpinner.post {
                    // Explicitly set the text first to override the hint
                    val textToSet = tahunTanamNames[tahunTanamPosition]
                    tahunTanamSpinner.setText(textToSet)

                    // Then set the selected index
                    tahunTanamSpinner.setSelectedIndex(tahunTanamPosition)
                    AppLogger.d("Set tahun tanam selectedIndex to: $tahunTanamPosition with text: $textToSet")

                    // Force UI update with a small delay to ensure rendering
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Update tracking variables
                        selectedTahunTanamIdSpinner = tahunTanamPosition
                        selectedTahunTanamValue = textToSet
                        AppLogger.d("Updated selectedTahunTanamValue to: $textToSet")

                        // Call handleItemSelection to trigger the regular selection handling logic
                        handleItemSelection(tahunTanamLayout, tahunTanamPosition, textToSet)
                    }, 200) // Small delay to ensure UI updates
                }
            } catch (e: Exception) {
                AppLogger.e("Error setting tahun tanam selection: ${e.message}")
            }
        } else {
            // If no valid tahun tanam is found or selection is empty, leave as hint
            AppLogger.d("No valid tahun tanam selection found, leaving as hint")

//            // Reset tahun tanam selection variables
//            selectedTahunTanamIdSpinner = -1
//            selectedTahunTanamValue = ""

            // Ensure spinner shows hint
            tahunTanamSpinner.setHint("Pilih Kategori Yang Sesuai")
        }

        val blokLayout = findViewById<LinearLayout>(R.id.layoutBlok)


        val estateIdToUse =
            if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {
                selectedEstate.toIntOrNull()
            } else {
                estateId?.toIntOrNull()
            }

        val filteredBlokList = blokList.filter { blok ->
            blok.dept == estateIdToUse &&
                    blok.divisi == selectedDivisiValue &&
                    blok.tahun == selectedTahunTanamValue
        }


        val blokNames = filteredBlokList.mapNotNull { it.blok_kode }
        setupSpinnerView(blokLayout, blokNames)
        val blokSpinner = blokLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)

        if (blokNames.isNotEmpty() && selectedBlokIdSpinner >= 0 && selectedBlokIdSpinner < blokNames.size) {
            try {
                // Use post to ensure spinner is initialized
                blokSpinner.post {
                    try {
                        // Set selection directly with the index we want
                        blokSpinner.setSelectedIndex(selectedBlokIdSpinner)
                        AppLogger.d("setSelectedIndex called with position: $selectedBlokIdSpinner")

                        // Update the selectedBlok value based on the selection
                        selectedBlok = blokNames[selectedBlokIdSpinner]
                        AppLogger.d("Updated selectedBlok to: $selectedBlok")

                        // Handle the selection with the updated values
                        handleItemSelection(blokLayout, selectedBlokIdSpinner, selectedBlok)
                    } catch (e: Exception) {
                        AppLogger.e("Error setting selection: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Error in blok selection: ${e.message}")
            }
        } else {
            AppLogger.d("No valid selection could be made - invalid index or empty list")
        }

        selectedTPH = ""
        selectedTPHBackup = ""
        selectedTPHValue = null

        val kemandoranNames = kemandoranList.mapNotNull { it.nama }
        setupSpinnerView(kemandoranLayout, kemandoranNames)
        val kemandoranSpinner = kemandoranLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)

        // Find the position of the saved kemandoran in the list
        val kemandoranPosition = kemandoranNames.indexOf(selectedKemandoran)
        AppLogger.d("Found kemandoranPosition: $kemandoranPosition for name: $selectedKemandoran")

        // Only proceed with selection if we have a valid position and non-empty selection
        if (kemandoranPosition >= 0 && selectedKemandoran.isNotEmpty() && kemandoranNames.isNotEmpty()) {
            try {
                // Force selection by posting to the main thread
                kemandoranSpinner.post {
                    // Explicitly set the text first to override the hint
                    val textToSet = kemandoranNames[kemandoranPosition]
                    kemandoranSpinner.setText(textToSet)

                    // Then set the selected index
                    kemandoranSpinner.setSelectedIndex(kemandoranPosition)
                    AppLogger.d("Set selectedIndex to: $kemandoranPosition with text: $textToSet")

                    // Force UI update with a small delay to ensure rendering
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Verify selection took effect
                        val actualSelection = kemandoranSpinner.selectedIndex
                        val actualText = kemandoranSpinner.text.toString()
                        AppLogger.d("Actual selection after setting: $actualSelection with text: $actualText")

                        // If the spinner still shows hint, try alternative approach
                        if (actualText != textToSet) {
                            // Alternative approach - try setting directly
                            try {
                                kemandoranSpinner.performClick()
                                Handler(Looper.getMainLooper()).postDelayed({
                                    kemandoranSpinner.setSelectedIndex(kemandoranPosition)
                                    kemandoranSpinner.setText(textToSet)
                                    kemandoranSpinner.performClick() // Close the dropdown
                                }, 100)
                            } catch (e: Exception) {
                                AppLogger.e("Error in alternative approach: ${e.message}")
                            }
                        }

                        // Update tracking variables
                        selectedKemandoranIdSpinner = kemandoranPosition
                        selectedKemandoran = textToSet
                        AppLogger.d("Updated selectedKemandoran to: $textToSet")

                        // Call handleItemSelection to trigger the regular selection handling logic
                        handleItemSelection(kemandoranLayout, kemandoranPosition, textToSet)
                    }, 200) // Small delay to ensure UI updates
                }
            } catch (e: Exception) {
                AppLogger.e("Error setting kemandoran selection: ${e.message}")
                // Call the suspend function from a coroutine
                lifecycleScope.launch {
                    try {
                        loadPemanenFullEstate(kemandoranLayout.rootView)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@FeaturePanenTBSActivity,
                            "Error loading full pemanen estate: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } else {
            // If no valid kemandoran is found or selection is empty, leave as hint
            AppLogger.d("No valid kemandoran selection found, leaving as hint")

            // Reset kemandoran selection variables
            selectedKemandoranIdSpinner = -1
            selectedKemandoran = ""

            // Ensure spinner shows hint
            kemandoranSpinner.setHint("Pilih Kategori Yang Sesuai")

            // Call the suspend function from a coroutine
            lifecycleScope.launch {
                try {
                    loadPemanenFullEstate(kemandoranLayout.rootView)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@FeaturePanenTBSActivity,
                        "Error loading full pemanen estate: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        val kemandoranLainNames = kemandoranLainList.mapNotNull { it.nama }
        setupSpinnerView(kemandoranLainLayout, kemandoranLainNames)
        val kemandoranLainSpinner =
            kemandoranLainLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)

// Find the position of the saved kemandoranLain in the list
        val kemandoranLainPosition = kemandoranLainNames.indexOf(selectedKemandoranLain)
        AppLogger.d("Found kemandoranLainPosition: $kemandoranLainPosition for name: $selectedKemandoranLain")

// Only proceed with selection if we have a valid position and non-empty selection
        if (kemandoranLainPosition >= 0 && selectedKemandoranLain.isNotEmpty() && kemandoranLainNames.isNotEmpty()) {
            try {
                // Force selection by posting to the main thread
                kemandoranLainSpinner.post {
                    // Explicitly set the text first to override the hint
                    val textToSet = kemandoranLainNames[kemandoranLainPosition]
                    kemandoranLainSpinner.setText(textToSet)

                    // Then set the selected index
                    kemandoranLainSpinner.setSelectedIndex(kemandoranLainPosition)
                    AppLogger.d("Set kemandoranLain selectedIndex to: $kemandoranLainPosition with text: $textToSet")

                    // Force UI update with a small delay to ensure rendering
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Verify selection took effect
                        val actualSelection = kemandoranLainSpinner.selectedIndex
                        val actualText = kemandoranLainSpinner.text.toString()
                        AppLogger.d("Actual kemandoranLain selection after setting: $actualSelection with text: $actualText")

                        // If the spinner still shows hint, try alternative approach
                        if (actualText != textToSet) {
                            // Alternative approach - try setting directly
                            try {
                                kemandoranLainSpinner.performClick()
                                Handler(Looper.getMainLooper()).postDelayed({
                                    kemandoranLainSpinner.setSelectedIndex(kemandoranLainPosition)
                                    kemandoranLainSpinner.setText(textToSet)
                                    kemandoranLainSpinner.performClick() // Close the dropdown
                                }, 100)
                            } catch (e: Exception) {
                                AppLogger.e("Error in alternative kemandoranLain approach: ${e.message}")
                            }
                        }

                        // Update tracking variables
                        selectedKemandoranLainIdSpinner = kemandoranLainPosition
                        selectedKemandoranLain = textToSet
                        AppLogger.d("Updated selectedKemandoranLain to: $textToSet")

                        // Call handleItemSelection to trigger the regular selection handling logic
                        handleItemSelection(kemandoranLainLayout, kemandoranLainPosition, textToSet)
                    }, 200) // Small delay to ensure UI updates
                }
            } catch (e: Exception) {
                AppLogger.e("Error setting kemandoranLain selection: ${e.message}")
                // Call the suspend function from a coroutine if needed
                lifecycleScope.launch {
                    try {
                        loadPemanenFullEstate(kemandoranLainLayout.rootView)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@FeaturePanenTBSActivity,
                            "Error loading full pemanen estate: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } else {
            // If no valid kemandoranLain is found or selection is empty, leave as hint
            AppLogger.d("No valid kemandoranLain selection found, leaving as hint")

            // Reset kemandoranLain selection variables
            selectedKemandoranLainIdSpinner = -1
            selectedKemandoranLain = ""

            // Ensure spinner shows hint
            kemandoranLainSpinner.setHint("Pilih Kategori Yang Sesuai")

            // Call the suspend function from a coroutine if needed
            lifecycleScope.launch {
                try {
                    loadPemanenFullEstate(kemandoranLainLayout.rootView)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@FeaturePanenTBSActivity,
                        "Error loading full pemanen estate: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        val tipePanenOptions = resources.getStringArray(R.array.tipe_panen_options).toList()
        setupSpinnerView(findViewById(R.id.layoutTipePanen), tipePanenOptions)

        val tipePanenLayout = findViewById<LinearLayout>(R.id.layoutTipePanen)
        val tipePanenSpinner = tipePanenLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)

        if (selectedTipePanen != null && selectedTipePanen.isNotEmpty()) {
            val tipePanenPosition = selectedTipePanen.toIntOrNull() ?: 0
            tipePanenSpinner.setSelectedIndex(tipePanenPosition)

            if (tipePanenPosition >= 0 && tipePanenPosition < tipePanenOptions.size) {
                val selectedItem = tipePanenOptions[tipePanenPosition]
                handleItemSelection(tipePanenLayout, tipePanenPosition, selectedItem)
            }
        }

        val etAncak = layoutAncak.findViewById<EditText>(R.id.etHomeMarkerTPH)
        etAncak.setText("")
        ancakInput = ""


        val etNomorPemanen = layoutNomorPemanen.findViewById<EditText>(R.id.etHomeMarkerTPH)
        etNomorPemanen.setText("")
        nomorPemanenInput = ""


        resetAllCounters()

        lifecycleScope.launch {

            loadingDialog.show()
            loadingDialog.setMessage("Sedang memproses data...")
            delay(1500)

            try {

                delay(100)

                // Now scroll to top AFTER all data loading and UI setup is complete
                val scPanen = findViewById<ScrollView>(R.id.scPanen)
                scPanen.fullScroll(ScrollView.FOCUS_UP)
            } catch (e: Exception) {
                AppLogger.e("Error reloading karyawan in reset: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FeaturePanenTBSActivity,
                        "Error reloading worker data: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    // Still try to scroll even if there was an error
                    val scPanen = findViewById<ScrollView>(R.id.scPanen)
                    scPanen.fullScroll(ScrollView.FOCUS_UP)
                }
            }
            loadingDialog.dismiss()
        }

        if (featureName == AppUtils.ListFeatureNames.MutuBuah) {
            // Reset selfie photo data
            photoCountSelfie = 0
            photoFilesSelfie.clear()
            selfiePhotoFile = null

            // Reset selfie UI - THIS IS THE KEY FIX
            val imageView = layoutSelfiePhoto.findViewById<ImageView>(R.id.ivAddFoto)
            imageView.setImageResource(R.drawable.baseline_camera_front_24)
            imageView.setColorFilter(
                ContextCompat.getColor(this, R.color.colorRedDark),
                PorterDuff.Mode.SRC_IN
            )

            // Reset scale type to default
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        }

        // Reset regular photos
        photoCount = 0
        photoFiles.clear()
        komentarFoto.clear()
        takeFotoPreviewAdapter?.resetAllSections()
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }

    @SuppressLint("SetTextI18n")
    private fun updateDependentCounters(
        layoutId: Int,
        change: Int,
        counterVar: KMutableProperty0<Int>,
        tvPercent: TextView?
    ) {
        val sisa = jumTBS - abnormal - bLewatMasak - bMentah - jjgKosong
        when (layoutId) {

            R.id.layoutJumTBS -> {
                if (change > 0) { // When change is positive (Increment)
                    jumTBS += change
                    counterVar.set(jumTBS)
                } else if (change < 0) {
                    if (jumTBS > 0 && sisa > 0) {
                        jumTBS += change
                        counterVar.set(jumTBS)

                        val updates = listOf(
                            Pair(::bMentah, R.id.layoutBMentah),
                            Pair(::bLewatMasak, R.id.layoutBLewatMasak),
                            Pair(::jjgKosong, R.id.layoutJjgKosong),
                            Pair(::abnormal, R.id.layoutAbnormal),
                            Pair(::seranganTikus, R.id.layoutSeranganTikus),
                            Pair(::tangkaiPanjang, R.id.layoutTangkaiPanjang),
                            Pair(::tidakVCut, R.id.layoutVcut)
                        )


                        Handler(Looper.getMainLooper()).post {
                            for ((counter, layout) in updates) {
                                if (counter.get() > jumTBS) {
                                    updateDependentCounters(layout, -1, counter, null)
                                    updateEditText(layout, counter.get())
                                }
                            }
                        }


                    } else {
                        vibrate()
                    }
                }
            }

            R.id.layoutBMentah -> {
                if (change > 0) {
                    if (jumTBS > 0 && bMentah < jumTBS && sisa > 0) {
                        bMentah += change
                        counterVar.set(bMentah)
                    } else {
                        vibrate()
                    }
                } else if (change < 0) {
                    if (bMentah > 0) {
                        bMentah += change
                        counterVar.set(bMentah)
                    } else {
                        vibrate()
                    }
                }
            }

            R.id.layoutBLewatMasak -> {
                if (change > 0) {
                    if (jumTBS > 0 && bLewatMasak < jumTBS && sisa > 0) {
                        bLewatMasak += change
                        counterVar.set(bLewatMasak)
                    } else {
                        vibrate()
                    }
                } else if (change < 0) {
                    if (bLewatMasak > 0) {
                        bLewatMasak += change
                        counterVar.set(bLewatMasak)
                    } else {
                        vibrate()
                    }
                }
            }

            R.id.layoutJjgKosong -> {
                if (change > 0) { // When change is positive (Increment)
                    if (jumTBS > 0 && jjgKosong < jumTBS && sisa > 0) {
                        jjgKosong += change
                        counterVar.set(jjgKosong)
                    } else {
                        vibrate()
                    }
                } else if (change < 0) { // When change is negative (Decrement)
                    if (jjgKosong > 0) { // Prevent going negative
                        jjgKosong += change
                        counterVar.set(jjgKosong)
                    } else {
                        vibrate()
                    }
                }
            }

            R.id.layoutAbnormal -> {
                if (change > 0) { // When change is positive (Increment)
                    AppLogger.d(jumTBS.toString())
                    AppLogger.d(abnormal.toString())
                    AppLogger.d(bLewatMasak.toString())
                    AppLogger.d(bMentah.toString())
                    AppLogger.d(jjgKosong.toString())



                    if (jumTBS > 0 && abnormal < jumTBS && sisa > 0) { // Prevent abnormal from exceeding tbs
                        abnormal += change
                        counterVar.set(abnormal)
                    } else {
                        vibrate()
                    }
                } else if (change < 0) { // When change is negative (Decrement)
                    if (abnormal > 0) { // Prevent going negative
                        abnormal += change
                        counterVar.set(abnormal)
                    } else {
                        vibrate()
                    }
                }
            }

            R.id.layoutSeranganTikus -> {
                if (change > 0) {
                    if (jumTBS > 0 && seranganTikus < jumTBS) {
                        seranganTikus += change
                        counterVar.set(seranganTikus)
                    } else {
                        vibrate()
                    }
                } else if (change < 0) { // When change is negative (Decrement)
                    if (seranganTikus > 0) { // Prevent going negative
                        seranganTikus += change
                        counterVar.set(seranganTikus)
                    } else {
                        vibrate()
                    }
                }
            }

            R.id.layoutTangkaiPanjang -> {
                if (change > 0) {
                    if (jumTBS > 0 && tangkaiPanjang < jumTBS) {
                        tangkaiPanjang += change
                        counterVar.set(tangkaiPanjang)
                    } else {
                        vibrate()
                    }
                } else if (change < 0) { // When change is negative (Decrement)
                    if (tangkaiPanjang > 0) { // Prevent going negative
                        tangkaiPanjang += change
                        counterVar.set(tangkaiPanjang)
                    } else {
                        vibrate()
                    }
                }
            }

            R.id.layoutVcut -> {
                if (change > 0) {
                    if (jumTBS > 0 && tidakVCut < jumTBS) {
                        tidakVCut += change
                        counterVar.set(tidakVCut)
                    } else {
                        vibrate()
                    }
                } else if (change < 0) {
                    if (tidakVCut > 0) {
                        tidakVCut += change
                        counterVar.set(tidakVCut)
                    } else {
                        vibrate()
                    }
                }
            }
        }

        formulas()
        updateCounterTextViews()

        if (layoutId == R.id.layoutBMentah) {
            tvPercent?.let {
                it.setText("${persenMentah}%")
            }
        } else if (layoutId == R.id.layoutBLewatMasak) {
            tvPercent?.let {
                it.setText("${persenLewatMasak}%")
            }
        } else if (layoutId == R.id.layoutJjgKosong) {
            tvPercent?.let {
                it.setText("${persenJjgKosong}%")
            }
        } else if (layoutId == R.id.layoutAbnormal) {
            tvPercent?.let {
                it.setText("${persenAbnormal}%")
            }
        }
    }

    private fun updateEditText(layoutId: Int, value: Int) {
        val includedLayout = findViewById<View>(layoutId)
        val etNumber = includedLayout?.findViewById<EditText>(R.id.etNumber)
        etNumber?.setText(value.toString())
    }

    private fun formulas() {
//        buahMasak = jumTBS - jjgKosong - bMentah - bLewatMasak
//        bMentah = jumTBS - bLewatMasak - jjgKosong - buahMasak
//        bLewatMasak = jumTBS - bMentah - buahMasak - jjgKosong
//        jjgKosong = jumTBS - bMentah - buahMasak - bLewatMasak
//        tbsDibayar = jumTBS - bMentah - jjgKosong
//        kirimPabrik = jumTBS - jjgKosong - abnormal

        tbsDibayar = jumTBS - bMentah - jjgKosong
        kirimPabrik = jumTBS - jjgKosong
        buahMasak = jumTBS - abnormal - bMentah - jjgKosong - bLewatMasak

        persenMentah = MathFun().round((bMentah.toFloat() / jumTBS.toFloat() * 100), 2)!!
        persenMasak = MathFun().round((bMentah.toFloat() / jumTBS.toFloat() * 100), 2)!!
        persenLewatMasak = MathFun().round((bLewatMasak.toFloat() / jumTBS.toFloat() * 100), 2)!!
        persenAbnormal = MathFun().round((abnormal.toFloat() / jumTBS.toFloat() * 100), 2)!!
        persenJjgKosong = MathFun().round((jjgKosong.toFloat() / jumTBS.toFloat() * 100), 2)!!


        initializeJjgJson()
    }

    private fun initViewModel() {
        panenTBSViewModel = ViewModelProvider(
            this,
            PanenTBSViewModel.Factory(application, PanenTBSRepository(this))
        )[PanenTBSViewModel::class.java]


        val idTakeFotoLayout = findViewById<View>(R.id.id_take_foto_layout)
        val idEditFotoLayout = findViewById<View>(R.id.id_editable_foto_layout)
        val cameraRepository = CameraRepository(this, window, idTakeFotoLayout, idEditFotoLayout)
        cameraRepository.setPhotoCallback(this)
        cameraViewModel = ViewModelProvider(
            this,
            CameraViewModel.Factory(cameraRepository)
        )[CameraViewModel::class.java]

        val status_location = findViewById<ImageView>(R.id.statusLocation)
        locationViewModel = ViewModelProvider(
            this,
            LocationViewModel.Factory(application, status_location, this, boundaryAccuracy)  // Pass boundaryAccuracy
        )[LocationViewModel::class.java]

        val factory = DatasetViewModel.DatasetViewModelFactory(application)
        datasetViewModel = ViewModelProvider(this, factory)[DatasetViewModel::class.java]

        val factoryPanenViewModel = PanenViewModel.PanenViewModelFactory(application)
        panenViewModel = ViewModelProvider(this, factoryPanenViewModel)[PanenViewModel::class.java]

        val factoryMutuBuahViewModel = MutuBuahViewModel.MutuBuahViewModelFactory(application)
        mutuBuahViewModel = ViewModelProvider(this, factoryMutuBuahViewModel)[MutuBuahViewModel::class.java]

        val factoryAbsensiViewModel = AbsensiViewModel.AbsensiViewModelFactory(application)
        absensiViewModel =
            ViewModelProvider(this, factoryAbsensiViewModel)[AbsensiViewModel::class.java]
    }


    /**
     * Sets up all spinner mappings, counters, and the RecyclerView.
     */
    @SuppressLint("SetTextI18n", "CutPasteId")
    private fun setupLayout() {
        val featureName = intent.getStringExtra("FEATURE_NAME")
        if (featureName == "Panen TBS") {
            findViewById<LinearLayout>(R.id.layoutEstate).visibility = View.GONE
        }


        AppLogger.d("radiasdklfjalskdjfladsf")
        AppLogger.d("radiusMinimum $radiusMinimum")
        val radiusText = "${radiusMinimum.toInt()} m"
        val fullText =
            "Berikut adalah daftar lokasi TPH yang berada dalam radius $radiusText dari lokasi anda:"
        val spannableString = SpannableString(fullText)
        val startIndex = fullText.indexOf(radiusText)
        val endIndex = startIndex + radiusText.length
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            StyleSpan(Typeface.ITALIC),
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        descScannedTPHInsideRadius.text = spannableString
        tphScannedResultRecyclerView.layoutManager = LinearLayoutManager(this)
        tphScannedResultRecyclerView.isNestedScrollingEnabled = false
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        tphScannedResultRecyclerView.removeItemDecoration(decoration) // Remove if applied

        btnScanTPHRadius.setOnClickListener {

            if (currentAccuracy <= boundaryAccuracy) {
//                // GPS is within boundary - proceed directly
                isTriggeredBtnScanned = true
                // Reset the selectedTPHIdByScan when manually refreshing
                selectedTPHIdByScan = null
                selectedTPHValue = null
                progressBarScanTPHManual.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed({
                    checkScannedTPHInsideRadius()
                }, 400)
            } else {
                // GPS is outside boundary - show error toast
                Toasty.error(this, "Akurasi GPS harus dalam radius ${boundaryAccuracy.toInt()} meter untuk melanjutkan!", Toast.LENGTH_LONG, true)
                    .show()
            }

        }


        inputMappings = listOf(
            Triple(
                findViewById<LinearLayout>(R.id.layoutEstate),
                getString(R.string.field_estate),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutAfdeling),
                getString(R.string.field_afdeling),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutMasterTPH),
                getString(R.string.field_master_tph_estate),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutTahunTanam),
                getString(R.string.field_tahun_tanam),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutBlok),
                getString(R.string.field_blok),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutTipePanen),
                getString(R.string.field_tipe_panen),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutNoTPH),
                getString(R.string.field_no_tph),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutAncak),
                getString(R.string.field_ancak),
                InputType.EDITTEXT
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutNomorPemanen),
                getString(R.string.field_nomor_pemanen),
                InputType.EDITTEXT
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutKemandoran),
                if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) getString(R.string.field_kemandoran) + " " + prefManager!!.estateUserLogin else getString(
                    R.string.field_kemandoran
                ),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutPemanen),
                if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) getString(R.string.field_pemanen) + " " + prefManager!!.estateUserLogin else getString(
                    R.string.field_pemanen
                ),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutKemandoranLain),
                getString(R.string.field_kemandoran_lain),
                InputType.SPINNER
            ),
            Triple(
                findViewById<LinearLayout>(R.id.layoutPemanenLain),
                getString(R.string.field_pemanen_lain),
                InputType.SPINNER
            )
        )

        inputMappings.forEach { (layoutView, key, inputType) ->
            updateTextInPertanyaan(layoutView, key)
            when (inputType) {
                InputType.SPINNER -> {
                    when (layoutView.id) {
                        R.id.layoutMasterTPH -> {
                            val estateNames =
                                estateList.sortedBy { it.nama }.mapNotNull { "${it.nama}" }
                            setupSpinnerView(layoutView, estateNames)
                        }

                        R.id.layoutEstate -> {
                            if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {
                                val masterDeptAbbrList = masterDeptInfoMap.values.toList()
                                setupSpinnerView(layoutView, masterDeptAbbrList)
                            } else {
                                val namaEstate = prefManager!!.estateUserLengkapLogin
                                AppLogger.d("estateIdUserLogin: ${prefManager!!.estateIdUserLogin}")
                                AppLogger.d("estateUserLengkapLogin: ${prefManager!!.estateUserLengkapLogin}")

                                val isGM = jabatanUser?.contains("GM", ignoreCase = true) == true

                                if (isGM) {
                                    // Split the estate names into a list for GM
                                    val estateList = namaEstate?.split(",")?.map { it.trim() } ?: emptyList()
                                    AppLogger.d("GM detected - Estate list: $estateList")
                                    setupSpinnerView(layoutView, estateList)
                                } else {
                                    // Single estate for non-GM users
                                    val singleEstateList = if (namaEstate.isNullOrEmpty()) {
                                        emptyList()
                                    } else {
                                        listOf(namaEstate)
                                    }
                                    AppLogger.d("Non-GM user - Single estate: $singleEstateList")
                                    setupSpinnerView(layoutView, singleEstateList)
                                    findViewById<MaterialSpinner>(R.id.spPanenTBS).setSelectedIndex(0)
                                }
                            }
                        }

                        R.id.layoutAfdeling -> {
                            if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {
                                setupSpinnerView(layoutView, emptyList())
                            } else {
                                val divisiNames =
                                    divisiList.sortedBy { it.divisi_abbr }
                                        .mapNotNull { it.divisi_abbr }
                                setupSpinnerView(layoutView, divisiNames)
                            }

                        }

                        R.id.layoutPemanen -> {
                            if (karyawanList.isNotEmpty()) {
                                val karyawanNames = karyawanList
                                    .sortedBy { it.nama }
                                    .map { "${it.nama} - ${it.nik ?: "N/A"}" }
                                setupSpinnerView(layoutView, karyawanNames)
                                layoutView.visibility = View.GONE
                            } else {
                                setupSpinnerView(layoutView, emptyList())
                                val pemanenSpinner =
                                    layoutView.findViewById<MaterialSpinner>(R.id.spPanenTBS)
                                pemanenSpinner.setHint("Tidak Ada Karyawan Hadir")
                            }
                        }

                        R.id.layoutPemanenLain -> {
                            // Prepare the data but don't show the dropdown yet
                            if (karyawanLainList.isNotEmpty()) {
                                val karyawanNames = karyawanLainList
                                    .sortedBy { it.nama }
                                    .map { "${it.nama} - ${it.nik ?: "N/A"}" }
                                setupSpinnerView(layoutView, karyawanNames)
                                // Initially hidden - will be shown when needed
                                layoutView.visibility = View.GONE
                            } else {

                                setupSpinnerView(layoutView, emptyList())
                                val pemanenSpinner =
                                    layoutView.findViewById<MaterialSpinner>(R.id.spPanenTBS)
                                pemanenSpinner.setHint("Tidak Ada Karyawan Hadir")
                            }
                        }

                        R.id.layoutTipePanen -> {
                            val tipePanenOptions =
                                resources.getStringArray(R.array.tipe_panen_options).toList()
                            setupSpinnerView(layoutView, tipePanenOptions)
                        }

//                        else -> {
//                            setupSpinnerView(layoutView, emptyList())
//                        }
                    }
                }

                InputType.EDITTEXT -> setupEditTextView(layoutView)
                else -> {}
            }
        }


        val counterMappings = listOf(
            Triple(R.id.layoutJumTBS, "Jumlah TBS", ::jumTBS),
            Triple(R.id.layoutBMentah, "Buah Mentah", ::bMentah),
            Triple(R.id.layoutBLewatMasak, "Buah Lewat Masak", ::bLewatMasak),
            Triple(R.id.layoutJjgKosong, "Janjang Kosong/Buah Busuk", ::jjgKosong),
            Triple(R.id.layoutAbnormal, "Abnormal", ::abnormal),
            Triple(R.id.layoutSeranganTikus, "Serangan Tikus", ::seranganTikus),
            Triple(R.id.layoutTangkaiPanjang, "Tangkai Panjang", ::tangkaiPanjang),
            Triple(R.id.layoutVcut, "Tidak V-Cut", ::tidakVCut)
        )
        counterMappings.forEach { (layoutId, labelText, counterVar) ->
            setupPaneWithButtons(layoutId, R.id.tvNumberPanen, labelText, counterVar)
        }

        rvSelectedPemanen = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.top_margin)
            }
            layoutManager = FlexboxLayoutManager(context).apply {
                justifyContent = JustifyContent.FLEX_START
            }
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        selectedPemanenAdapter = SelectedWorkerAdapter()
        rvSelectedPemanen.adapter = selectedPemanenAdapter


        val layoutPemanen = findViewById<LinearLayout>(R.id.layoutPemanen)
        val parentLayout = layoutPemanen.parent as ViewGroup
        val index = parentLayout.indexOfChild(layoutPemanen)
        parentLayout.addView(rvSelectedPemanen, index + 1)

        rvSelectedPemanenLain = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.top_margin)
            }
            layoutManager = FlexboxLayoutManager(context).apply {
                justifyContent = JustifyContent.FLEX_START
            }
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        selectedPemanenLainAdapter = SelectedWorkerAdapter()
        rvSelectedPemanenLain.adapter = selectedPemanenLainAdapter

        val layoutPemanenLain = findViewById<LinearLayout>(R.id.layoutPemanenLain)
        val parentLayoutLain = layoutPemanenLain.parent as ViewGroup
        val index2 = parentLayoutLain.indexOfChild(layoutPemanenLain)
        parentLayoutLain.addView(rvSelectedPemanenLain, index2 + 1)

        setupRecyclerViewTakePreviewFoto()
        setupSwitchBlokBanjir()
        setupSwitchAsistensi()
        setupFormulasView()

        //khusus menampilkan dan handle unduh dataset Master TPH
        if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {

            val tvDescMaster: TextView = findViewById(R.id.title_data_master)
            tvDescMaster.visibility = View.VISIBLE
            layoutMasterTPH.visibility = View.VISIBLE
            val btnDownloadDataset = findViewById<MaterialButton>(R.id.btnDownloadDataset)
            btnDownloadDataset.visibility = View.VISIBLE
            val dashedLine = findViewById<View>(R.id.dashedLine)
            dashedLine.visibility = View.VISIBLE

            btnDownloadDataset.setOnClickListener {
                if (AppUtils.isNetworkAvailable(this)) {
                    datasetViewModel.resetState()
                    // When processing selected estates, use the values directly
                    val selectedEstates =
                        masterEstateHasBeenChoice.filter { it.value }.keys.toList()

                    if (selectedEstates.isEmpty()) {
                        layoutMasterTPH.findViewById<TextView>(R.id.tvErrorFormPanenTBS).visibility =
                            View.VISIBLE
                        layoutMasterTPH.findViewById<TextView>(R.id.tvErrorFormPanenTBS).text =
                            getString(
                                R.string.al_must_checked_master_estate
                            )
                    } else {
                        // Create dataset requests for each selected estate
                        val datasetRequests = mutableListOf<DatasetRequest>()

                        selectedEstates.forEach { estateName ->
                            // Find the estate in your estate list by name
                            val estate =
                                estateList.find { it.abbr == estateName || it.nama == estateName }
                            estate?.let {
                                val estateId = it.id ?: 0
                                val estateAbbr = it.abbr ?: "unknown"
                                val lastModified = prefManager!!.getEstateLastModified(estateAbbr)

                                Log.d(
                                    "Estate Download",
                                    "Adding estate: $estateAbbr (ID: $estateId), Last modified: $lastModified"
                                )

                                // Add TPH dataset request for this estate
                                datasetRequests.add(
                                    DatasetRequest(
                                        estate = estateId,
                                        estateAbbr = estateAbbr,
                                        lastModified = lastModified,
                                        dataset = AppUtils.DatasetNames.tph,
                                        isDownloadMasterTPHAsistensi = true
                                    )
                                )
                            }
                        }

                        if (datasetRequests.isNotEmpty()) {
                            setupDownloadDialog(datasetRequests)
                        }
                    }
                } else {
                    AlertDialogUtility.withSingleAction(
                        this@FeaturePanenTBSActivity,
                        stringXML(R.string.al_back),
                        stringXML(R.string.al_no_internet_connection),
                        stringXML(R.string.al_no_internet_connection_description_login),
                        "network_error.json",
                        R.color.colorRedDark
                    ) {

                    }
                }


            }

            val warningCardLayout = findViewById<ViewGroup>(R.id.warning_card)
            warningCardLayout.visibility = View.VISIBLE

            // Find the views inside the included layout
            val btnCloseWarning = warningCardLayout.findViewById<ImageButton>(R.id.btnCloseWarning)

            // Set up close button click listener
            btnCloseWarning.setOnClickListener {
                warningCardLayout.visibility = View.GONE
            }
        }
    }

    private fun resetEstateSelection(successfulEstates: List<String>) {
        successfulEstates.forEach { estateAbbr ->
            // Find the estate by abbreviation
            val estate = estateList.find { it.abbr == estateAbbr }
            estate?.let {
                // Use the estate name as the key (since that's what's stored in masterEstateHasBeenChoice)
                val key = it.nama
                if (key != null) {
                    masterEstateHasBeenChoice[key] = false
                }
            }
        }

        // Update the button text to reflect the new state
        updateDownloadMasterDataButtonText(masterEstateHasBeenChoice)
    }


    fun setupDownloadDialog(datasetRequests: List<DatasetRequest>) {

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_download_progress, null)
        val titleTV = dialogView.findViewById<TextView>(R.id.tvTitleProgressBarLayout)
        titleTV.text = "Download Dataset"

        val counterTV = dialogView.findViewById<TextView>(R.id.counter_dataset)
        val counterSizeFile = dialogView.findViewById<LinearLayout>(R.id.counterSizeFile)
        counterSizeFile.visibility = View.VISIBLE

        // Get all buttons
        val closeDialogBtn = dialogView.findViewById<MaterialButton>(R.id.btnCancelDownloadDataset)
        val btnDownloadDataset = dialogView.findViewById<MaterialButton>(R.id.btnUploadDataCMP)
        val btnRetryDownload = dialogView.findViewById<MaterialButton>(R.id.btnRetryDownloadDataset)

        // Update button text to reflect download operation
        btnDownloadDataset.text = "Download Dataset"
        btnDownloadDataset.setIconResource(R.drawable.baseline_download_24) // Assuming you have this icon

        val containerDownloadDataset =
            dialogView.findViewById<LinearLayout>(R.id.containerDownloadDataset)
        containerDownloadDataset.visibility = View.VISIBLE

        // Initially show only close and download buttons
        closeDialogBtn.visibility = View.VISIBLE
        btnDownloadDataset.visibility = View.VISIBLE
        btnRetryDownload.visibility = View.GONE

        // Create upload items from dataset requests (we'll reuse the existing adapter)
        val downloadItems = mutableListOf<UploadCMPItem>()

        var itemId = 0
        datasetRequests.forEach { request ->
            downloadItems.add(
                UploadCMPItem(
                    id = itemId++,
                    title = "Master TPH ${request.estateAbbr}",
                    fullPath = "",
                    baseFilename = request.estateAbbr ?: "",
                    data = "",
                    type = "",
                    databaseTable = ""
                )
            )
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (counterTV.text == "0/0" && downloadItems.size > 0) {
                counterTV.text = "0/${downloadItems.size}"
            }
        }, 100)

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.features_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = UploadProgressCMPDataAdapter(downloadItems)
        recyclerView.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.show()

        fun startDownload(
            requestsToDownload: List<DatasetRequest> = datasetRequests,
            itemsToShow: List<UploadCMPItem> = downloadItems
        ) {
            // Check network connectivity first
            if (!AppUtils.isNetworkAvailable(this)) {
                AlertDialogUtility.withSingleAction(
                    this@FeaturePanenTBSActivity,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_no_internet_connection),
                    stringXML(R.string.al_no_internet_connection_description_login),
                    "network_error.json",
                    R.color.colorRedDark
                ) { }
                return
            }

            // Disable buttons during download
            btnDownloadDataset.isEnabled = false
            closeDialogBtn.isEnabled = false
            btnRetryDownload.isEnabled = false
            btnDownloadDataset.alpha = 0.7f
            closeDialogBtn.alpha = 0.7f
            btnRetryDownload.alpha = 0.7f
            btnDownloadDataset.iconTint = ColorStateList.valueOf(Color.parseColor("#80FFFFFF"))
            closeDialogBtn.iconTint = ColorStateList.valueOf(Color.parseColor("#80FFFFFF"))
            btnRetryDownload.iconTint = ColorStateList.valueOf(Color.parseColor("#80FFFFFF"))

            // Reset title color
            titleTV.setTextColor(ContextCompat.getColor(titleTV.context, R.color.black))
            titleTV.text = "Download Dataset"

            datasetViewModel.downloadDataset(requestsToDownload, itemsToShow)
        }

        btnDownloadDataset.setOnClickListener {
            if (AppUtils.isNetworkAvailable(this)) {
                AlertDialogUtility.withTwoActions(
                    this,
                    "Download",
                    getString(R.string.confirmation_dialog_title),
                    getString(R.string.al_confirm_upload),
                    "warning.json",
                    ContextCompat.getColor(this, R.color.bluedarklight),
                    function = { startDownload() },
                    cancelFunction = { }
                )
            } else {
                AlertDialogUtility.withSingleAction(
                    this@FeaturePanenTBSActivity,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_no_internet_connection),
                    stringXML(R.string.al_no_internet_connection_description_login),
                    "network_error.json",
                    R.color.colorRedDark
                ) {
                    // Do nothing
                }
            }
        }

        var failedRequests: List<DatasetRequest> = listOf()
        btnRetryDownload.setOnClickListener {
            if (AppUtils.isNetworkAvailable(this)) {
                // Create new download items only for failed requests
                val retryDownloadItems = mutableListOf<UploadCMPItem>()
                var itemId = 0

                AppLogger.d("failedRequests $failedRequests")

                failedRequests.forEach { request ->
                    retryDownloadItems.add(
                        UploadCMPItem(
                            id = itemId++,
                            title = "${request.estateAbbr} - ${request.dataset}",
                            fullPath = "",
                            baseFilename = request.estateAbbr ?: "",
                            data = "",
                            type = "",
                            databaseTable = ""
                        )
                    )
                }

                // Clear and update the RecyclerView with only failed items
                adapter.updateItems(retryDownloadItems)

                // Reset adapter state (progress bars, status icons, etc.)
                adapter.resetState()

                // Reset view model state
                datasetViewModel.resetState()

                // Update UI elements
                counterTV.text = "0/${retryDownloadItems.size}"
                titleTV.text = "Download Dataset"
                titleTV.setTextColor(ContextCompat.getColor(titleTV.context, R.color.black))

                // Hide retry button, show download button
                btnRetryDownload.visibility = View.GONE
                btnDownloadDataset.visibility = View.VISIBLE

                // Start download with only failed requests
                startDownload(failedRequests, retryDownloadItems)
            } else {
                AlertDialogUtility.withSingleAction(
                    this@FeaturePanenTBSActivity,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_no_internet_connection),
                    stringXML(R.string.al_no_internet_connection_description_login),
                    "network_error.json",
                    R.color.colorRedDark
                ) { }
            }
        }


        closeDialogBtn.setOnClickListener {
            datasetViewModel.processingComplete.removeObservers(this)
            datasetViewModel.itemProgressMap.removeObservers(this)
            datasetViewModel.completedCount.removeObservers(this)
            datasetViewModel.itemStatusMap.removeObservers(this)
            datasetViewModel.itemErrorMap.removeObservers(this)

            datasetViewModel.resetState()
            dialog.dismiss()
        }

        // Observe completed count (connect this to your actual download view model)
        datasetViewModel.completedCount.observe(this) { completed ->
            val total = datasetViewModel.totalCount.value ?: downloadItems.size
            counterTV.text = "$completed/$total"
        }

        // Observe download progress
        datasetViewModel.itemProgressMap.observe(this) { progressMap ->
            // Update progress for each item
            for ((id, progress) in progressMap) {
                AppLogger.d("Progress update for item $id: $progress%")
                adapter.updateProgress(id, progress)
            }

            // Update title if any download is in progress
            if (progressMap.values.any { it in 1..99 }) {
                titleTV.text = "Sedang Download Dataset..."
            }

        }


        datasetViewModel.processingComplete.observe(this) { isComplete ->
            if (isComplete) {
                val currentStatusMap = datasetViewModel.itemStatusMap.value ?: emptyMap()

                // Separate successful and failed downloads
                val successfulIds = mutableListOf<Int>()
                val failedIds = mutableListOf<Int>()

                currentStatusMap.forEach { (id, status) ->
                    if (status == AppUtils.UploadStatusUtils.DOWNLOADED) {
                        successfulIds.add(id)
                    } else {
                        failedIds.add(id)
                    }
                }

                // Get successful estate abbreviations
                val successfulEstates = datasetRequests.filterIndexed { index, _ ->
                    index in successfulIds
                }.mapNotNull { it.estateAbbr }

                // Store failed requests for retry
                failedRequests = datasetRequests.filterIndexed { index, _ ->
                    index in failedIds
                }

                // Always reset successful estates, even if some failed
                if (successfulEstates.isNotEmpty()) {
                    resetEstateSelection(successfulEstates)
                }

                // Refresh master data
                lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        // Update UI based on download results
                        if (failedIds.isEmpty()) {
                            // All successful
                            titleTV.text = "Download Berhasil"
                            titleTV.setTextColor(
                                ContextCompat.getColor(
                                    titleTV.context,
                                    R.color.greenDarker
                                )
                            )
                            btnDownloadDataset.visibility = View.GONE
                            btnRetryDownload.visibility = View.GONE
                            // Enable close button
                            closeDialogBtn.isEnabled = true
                            closeDialogBtn.alpha = 1f
                            closeDialogBtn.iconTint = ColorStateList.valueOf(Color.WHITE)
                        } else {
                            // Some or all failed
                            titleTV.text = "Terjadi Kesalahan Download"
                            titleTV.setTextColor(
                                ContextCompat.getColor(
                                    titleTV.context,
                                    R.color.colorRedDark
                                )
                            )
                            btnDownloadDataset.visibility = View.GONE
                            btnRetryDownload.visibility = View.VISIBLE
                            btnRetryDownload.isEnabled = true
                            btnRetryDownload.alpha = 1f
                            btnRetryDownload.iconTint = ColorStateList.valueOf(Color.WHITE)
                            // Enable close button
                            closeDialogBtn.isEnabled = true
                            closeDialogBtn.alpha = 1f
                            closeDialogBtn.iconTint = ColorStateList.valueOf(Color.WHITE)
                        }

                        datasetViewModel.getDistinctMasterDeptInfoCopy()
                        val departmentInfoDeferred = CompletableDeferred<Map<String, String>>()

                        delay(
                            1000
                        )
                        datasetViewModel.distinctDeptInfoListCopy.observe(this@FeaturePanenTBSActivity) { list ->
                            Log.d("DepartmentInfo", "Observed list size: ${list?.size}")
                            val distinctDeptInfos = list ?: emptyList()
                            val deptInfoMap =
                                distinctDeptInfos.associate { it.dept to it.dept_abbr }

                            Log.d("DepartmentInfo", "Department Map: $deptInfoMap")

                            masterDeptInfoMap = deptInfoMap
                            departmentInfoDeferred.complete(deptInfoMap)
                        }


                        // Wait for the deferred to complete
                        val fullDeptInfoMap = departmentInfoDeferred.await()

                        val masterDeptAbbrList = fullDeptInfoMap.values.toList()

                        Log.d("DepartmentInfo", "Master Dept Abbr List: $masterDeptAbbrList")

                        val layoutEstate = findViewById<LinearLayout>(R.id.layoutEstate)

                        Log.d("DepartmentInfo", "Setting up spinner with list")
                        setupSpinnerView(layoutEstate, masterDeptAbbrList)
                    }
                }
            }
        }

        datasetViewModel.itemStatusMap.observe(this) { statusMap ->
            // Update status for each item
            for ((id, status) in statusMap) {
                // No need for mapping - just pass the status directly to adapter
                adapter.updateStatus(id, status)
            }
        }

        // Observe errors for each item
        datasetViewModel.itemErrorMap.observe(this) { errorMap ->
            for ((id, error) in errorMap) {
                if (!error.isNullOrEmpty()) {
                    adapter.updateError(id, error)
                }
            }

            if (errorMap.values.any { !it.isNullOrEmpty() }) {
                titleTV.text = "Terjadi Kesalahan Download"
                titleTV.setTextColor(ContextCompat.getColor(titleTV.context, R.color.colorRedDark))
            }
        }
    }


    private fun setupTitleEachGroupInput() {
        val tvDescDatagrading: TextView = findViewById(R.id.title_data_grading)
        val tvDescLampiran: TextView = findViewById(R.id.title_lampiran_foto)
        val tvDescInformasiBlok: TextView = findViewById(R.id.title_data_informasi_blok)
        val tvDescMaster: TextView = findViewById(R.id.title_data_master)

        val textMaster = "Master Data Estate*"
        val textGrading = "Data Grading*"
        val textLampiran = "Lampiran Foto*"
        val textBlok = "Informasi Blok*"
        val spannable = SpannableString(textGrading)
        val spannable2 = SpannableString(textLampiran)
        val spannable3 = SpannableString(textBlok)
        val spannable4 = SpannableString(textMaster)

        val starColor = ContextCompat.getColor(this, R.color.colorRedDark) //
        spannable.setSpan(
            ForegroundColorSpan(starColor),
            textGrading.length - 1,
            textGrading.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable2.setSpan(
            ForegroundColorSpan(starColor),
            textLampiran.length - 1,
            textLampiran.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable3.setSpan(
            ForegroundColorSpan(starColor),
            textBlok.length - 1,
            textBlok.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable4.setSpan(
            ForegroundColorSpan(starColor),
            textMaster.length - 1,  // Use textMaster instead of textBlok
            textMaster.length,      // Use textMaster instead of textBlok
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvDescDatagrading.text = spannable
        tvDescLampiran.text = spannable2
        tvDescInformasiBlok.text = spannable3
        tvDescMaster.text = spannable4

    }

    private fun checkScannedTPHInsideRadius() {
        if (lat != null && lon != null) {
            val tphList = getTPHsInsideRadius(lat!!, lon!!, latLonMap)

            AppLogger.d("jenisTPHListGlobal $jenisTPHListGlobal")
            if (tphList.isNotEmpty() || selectedTPHIdByScan != null) {
                isEmptyScannedTPH = false
                tphScannedResultRecyclerView.visibility = View.VISIBLE
                titleScannedTPHInsideRadius.visibility = View.VISIBLE
                descScannedTPHInsideRadius.visibility = View.VISIBLE
                emptyScannedTPHInsideRadius.visibility = View.GONE
                tphScannedResultRecyclerView.adapter =
                    ListTPHInsideRadiusAdapter(tphList, this, jenisTPHListGlobal, true)


                val itemHeight = 50
                val maxHeight = 250

                val density = tphScannedResultRecyclerView.resources.displayMetrics.density
                val maxHeightPx = (maxHeight * density).toInt()
                val recyclerViewHeightPx = (tphList.size * itemHeight * density).toInt()

                tphScannedResultRecyclerView.layoutParams.height =
                    if (recyclerViewHeightPx > maxHeightPx) maxHeightPx else ViewGroup.LayoutParams.WRAP_CONTENT

                tphScannedResultRecyclerView.requestLayout()

                if (recyclerViewHeightPx > maxHeightPx) {
                    tphScannedResultRecyclerView.isNestedScrollingEnabled = true
                    tphScannedResultRecyclerView.overScrollMode = View.OVER_SCROLL_ALWAYS
                } else {
                    tphScannedResultRecyclerView.isNestedScrollingEnabled = false
                    tphScannedResultRecyclerView.overScrollMode = View.OVER_SCROLL_NEVER
                }

                tphScannedResultRecyclerView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                tphScannedResultRecyclerView.isVerticalScrollBarEnabled = true

                if (selectedTPHIdByScan != null) {
                    for (i in tphList.indices) {
                        if (tphList[i].id == selectedTPHIdByScan) {
                            tphScannedResultRecyclerView.scrollToPosition(i)
                            break
                        }
                    }
                }
            } else {
                tphScannedResultRecyclerView.visibility = View.GONE
                titleScannedTPHInsideRadius.visibility = View.VISIBLE
                descScannedTPHInsideRadius.visibility = View.VISIBLE
                emptyScannedTPHInsideRadius.visibility = View.VISIBLE
                isEmptyScannedTPH = true
            }
        } else {
            Toasty.error(this, "Sinyal GPS belum ditemukan! Silakan pindah ke area terbuka!", Toast.LENGTH_LONG, true)
                .show()
            isEmptyScannedTPH = true
        }

        if (progressBarScanTPHManual.visibility == View.VISIBLE) {
            progressBarScanTPHManual.visibility = View.GONE
        }

        if (progressBarScanTPHAuto.visibility == View.VISIBLE) {
            progressBarScanTPHAuto.visibility = View.GONE
        }
    }

    private fun setupFormulasView() {
        val tvFormulas = findViewById<TextView>(R.id.tvFormulas)

        tvFormulas.setOnClickListener {
            val view = LayoutInflater.from(this).inflate(R.layout.dialog_formulas_grading, null)

            view.background = ContextCompat.getDrawable(this, R.drawable.rounded_top_right_left)

            val dialog = BottomSheetDialog(this)
            dialog.setContentView(view)

            // Get screen width
            val displayMetrics = resources.displayMetrics
            val width = displayMetrics.widthPixels

            // Set bottom sheet width to 80% of screen width
            dialog.window?.apply {
                setLayout(
                    (width * 0.8).toInt(),
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            }

            // Set expanded state when showing
            dialog.setOnShowListener {
                val bottomSheet =
                    dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                val behavior = BottomSheetBehavior.from(bottomSheet!!)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }

            dialog.show()
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setupEditTextView(layoutView: LinearLayout) {
        val etHomeMarkerTPH = layoutView.findViewById<EditText>(R.id.etHomeMarkerTPH)
        val spHomeMarkerTPH = layoutView.findViewById<View>(R.id.spPanenTBS)
        val tvError = layoutView.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
        val MCVSpinner = layoutView.findViewById<View>(R.id.MCVSpinner)

        spHomeMarkerTPH.visibility = View.GONE
        etHomeMarkerTPH.visibility = View.VISIBLE


        // Set input type based on layout ID
        etHomeMarkerTPH.inputType = when (layoutView.id) {
            R.id.layoutAncak -> AndroidInputType.TYPE_CLASS_NUMBER
            R.id.layoutNomorPemanen -> AndroidInputType.TYPE_CLASS_NUMBER
            else -> AndroidInputType.TYPE_CLASS_TEXT
        }

        // Enhanced keyboard hiding function with debounce mechanism
        fun hideKeyboard() {
            if (keyboardBeingDismissed) return

            keyboardBeingDismissed = true
            val imm =
                application.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etHomeMarkerTPH.windowToken, 0)
            etHomeMarkerTPH.clearFocus()

            // Reset flag after delay to prevent rapid toggling
            Handler(Looper.getMainLooper()).postDelayed({
                keyboardBeingDismissed = false
            }, 300)
        }

        // Make EditText non-focusable when not editing
        etHomeMarkerTPH.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                true
            } else {
                false
            }
        }

        val touchInterceptor = object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event?.action == MotionEvent.ACTION_DOWN) {
                    hideKeyboard()
                }
                return false
            }
        }

        // Apply to parent views to ensure scrolling works
        val parentLayout = layoutView.findViewById<LinearLayout>(R.id.parentSpPanenTBS)
        parentLayout?.setOnTouchListener(touchInterceptor)

        // Apply specifically to the TextView area
        val tvTitle = layoutView.findViewById<TextView>(R.id.tvTitleFormPanenTBS)
        tvTitle?.setOnTouchListener(touchInterceptor)

        // Make MCVSpinner consume touch events but still hide keyboard
        MCVSpinner.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                hideKeyboard()
            }
            // Let the view handle the event normally after hiding keyboard
            false
        }

        // Add global touch listener to root view that doesn't interfere with scrolling
        val rootView = layoutView.rootView
        rootView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (etHomeMarkerTPH.hasFocus()) {
                    hideKeyboard()
                }
            }
            false
        }

        // Focus change listener
        etHomeMarkerTPH.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                hideKeyboard()
            }
        }

        // Add this to handle MaterialSpinner clicks
        spHomeMarkerTPH.setOnClickListener {
            hideKeyboard()
        }

        // Add text changed listener
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

                if (layoutView.id == R.id.layoutAncak) {
                    val inputText = s?.toString()?.trim() ?: ""
                    ancakInput = inputText

                    // Make another layout visible if there's text input
                    if (inputText.isNotEmpty()) {
                        // Replace R.id.yourOtherLayoutId with the actual ID of the layout you want to make visible
                        val otherLayout = findViewById<View>(R.id.layoutTipePanen)
                        otherLayout?.visibility = View.VISIBLE
                    }
                }
                else if (layoutView.id == R.id.layoutNomorPemanen) {
                    val inputText = s?.toString()?.trim() ?: ""
                    nomorPemanenInput = inputText


                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // Add this to your Activity class
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    v.clearFocus()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }


    private fun getTPHsInsideRadius(
        userLat: Double,
        userLon: Double,
        coordinates: Map<Int, ScannedTPHLocation>
    ): List<ScannedTPHSelectionItem> {
        val resultsList = mutableListOf<ScannedTPHSelectionItem>()
        var currentlySelectedIncluded = false

        // First, add all TPHs within radius
        for ((id, location) in coordinates) {
            val results = FloatArray(1)
            Location.distanceBetween(userLat, userLon, location.lat, location.lon, results)
            val distance = results[0]

            val jenisTPHId = location.jenisTPHId.toInt()

            val tphData = panenStoredLocal[id]
            val selectedCount = tphData?.count ?: 0
            val isSelected = selectedCount > 0
            val isCurrentlySelected = id == selectedTPHIdByScan

            // Get the default limit from jenisTPHListGlobal
            val defaultLimit = jenisTPHListGlobal.find { it.id == jenisTPHId }?.limit ?: 1

            // Calculate the final limit to use
            val limit =
                if (jenisTPHId == 2 && jenisTPHListGlobal.find { it.id == 2 }?.jenis_tph == "induk") {
                    // Special case for jenis_tph = induk (id = 2)
                    try {
                        val customLimit = tphData?.limitTPH?.toInt()

                        AppLogger.d("customLimit $customLimit")
                        if (customLimit != null && customLimit > 3 && customLimit <= 999) {
                            // Use the custom limit if it's greater than 3 and up to 999
                            customLimit
                        } else {
                            // Otherwise, use the default limit (7)
                            defaultLimit
                        }
                    } catch (e: Exception) {
                        defaultLimit
                    }
                } else {
                    defaultLimit
                }

            // Include if within radius OR is the currently selected TPH
            if (distance <= radiusMinimum || isCurrentlySelected) {
                resultsList.add(
                    ScannedTPHSelectionItem(
                        id = id,
                        number = location.nomor,
                        blockCode = location.blokKode,
                        distance = distance,
                        isAlreadySelected = isSelected,
                        selectionCount = selectedCount,
                        canBeSelectedAgain = selectedCount < limit,
                        isWithinRange = distance <= radiusMinimum,
                        jenisTPHId = jenisTPHId.toString(),
                        customLimit = limit.toString()
                    )
                )

                if (isCurrentlySelected) {
                    currentlySelectedIncluded = true
                }
            }
        }

        return resultsList.sortedBy { it.distance }
    }


    private fun resetDependentSpinners(rootView: View) {
        val switchAsistensi = rootView.findViewById<SwitchMaterial>(R.id.selAsistensi)
        if (switchAsistensi.isChecked) {
            switchAsistensi.isChecked = true
        }

        if (blokBanjir == 0) {
            layoutAncak.visibility = View.GONE
            layoutNomorPemanen.visibility = View.GONE
            layoutNoTPH.visibility = View.GONE
            layoutKemandoran.visibility = View.GONE
            layoutPemanen.visibility = View.GONE
            layoutSelAsistensi.visibility = View.GONE
            layoutTipePanen.visibility = View.GONE
        }

        val baseLayouts = listOf(
            R.id.layoutTahunTanam,
//            R.id.layoutBlok,
//            R.id.layoutNoTPH,
//            R.id.layoutKemandoran,
//            R.id.layoutKemandoranLain,
        )

        val dependentLayouts = if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {
            baseLayouts + R.id.layoutAfdeling
        } else {
            baseLayouts
        }

        dependentLayouts.forEach { layoutId ->
            val layout = rootView.findViewById<LinearLayout>(layoutId)
            setupSpinnerView(layout, emptyList())
        }

        // Reset related data
//        blokList = emptyList()
//        kemandoranList = emptyList()
//        kemandoranLainList = emptyList()
//        tphList = emptyList()


        if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {

            val kemandoranLainLayout = findViewById<LinearLayout>(R.id.layoutKemandoranLain)
            val kemandoranLainFilterContainer =
                kemandoranLainLayout.findViewById<MaterialCardView>(R.id.filter_container_pertanyaan_layout)
            kemandoranLainFilterContainer.visibility = View.GONE

            if (blokBanjir == 1) {
                lifecycleScope.launch {
                    loadPemanenFullEstate(rootView)
                }
            }

        }

        // Reset selected values
//        selectedTahunTanamValue = null
//        selectedBlok = ""
//        selectedBlokValue = null
//        selectedTPH = ""
//        selectedTPHValue = null
//        selectedKemandoranLain = ""

        // Clear adapters if they exist
//        selectedPemanenAdapter.clearAllWorkers()
//        selectedPemanenLainAdapter.clearAllWorkers()
    }

    private suspend fun loadPemanenFullEstate(rootView: View) {
        if (featureName != AppUtils.ListFeatureNames.MutuBuah){
            try {
                val karyawanDeferred = coroutineScope {
                    async {
                        panenViewModel.getAllKaryawan()
                        delay(100)
                        panenViewModel.allKaryawanList.value ?: emptyList()
                    }
                }

                val allKaryawan = karyawanDeferred.await()

                // Get user's afdeling ID (which is same as divisi)
                val userAfdelingId = prefManager!!.afdelingIdUserLogin?.toInt()
                AppLogger.d("User's afdeling ID: $userAfdelingId")

                // Only filter if presentNikSet has values
                if (presentNikSet.isNotEmpty()) {
                    // Filter karyawan list to only include those who are present
                    val presentKaryawan = allKaryawan.filter { karyawan ->
                        karyawan.nik != null && presentNikSet.contains(karyawan.nik)
                    }

                    // Filter by divisi - same divisi as user goes to karyawanList, others go to karyawanLainList
                    if (userAfdelingId != null) {
                        // Karyawan from same divisi as user
                        karyawanList = presentKaryawan.filter { karyawan ->
                            karyawan.divisi == userAfdelingId
                        }

                        // Karyawan from other divisi
                        karyawanLainList = presentKaryawan.filter { karyawan ->
                            karyawan.divisi != userAfdelingId
                        }
                    } else {
                        // If userAfdelingId is null, put all present karyawan in karyawanList
                        karyawanList = presentKaryawan
                        karyawanLainList = emptyList()
                    }

                    // Log statistics for debugging
                    AppLogger.d("Total karyawan: ${allKaryawan.size}")
                    AppLogger.d("Filtered to present karyawan: ${presentKaryawan.size}")
                    AppLogger.d("Same afdeling karyawan (karyawanList): ${karyawanList.size}")
                    AppLogger.d("Other afdeling karyawan (karyawanLainList): ${karyawanLainList.size}")

                    val karyawanNames = karyawanList
                        .sortedBy { it.nama }
                        .map { "${it.nama} - ${it.nik ?: "N/A"}" }

                    val karyawanLainNames = karyawanLainList
                        .sortedBy { it.nama }
                        .map { "${it.nama} - ${it.nik ?: "N/A"}" }

                    withContext(Dispatchers.Main) {
                        val layoutPemanen = rootView.findViewById<LinearLayout>(R.id.layoutPemanen)
                        layoutPemanen.visibility = View.VISIBLE

                        if (karyawanNames.isNotEmpty()) {
                            setupSpinnerView(layoutPemanen, karyawanNames)
                        } else {
                            setupSpinnerView(layoutPemanen, emptyList())
                            val pemanenSpinner =
                                layoutPemanen.findViewById<MaterialSpinner>(R.id.spPanenTBS)
                            pemanenSpinner.setHint("Tidak Ada Karyawan Hadir")
                        }

                        val layoutPemanenLain =
                            rootView.findViewById<LinearLayout>(R.id.layoutPemanenLain)
                        if (layoutPemanenLain != null) {
                            if (karyawanLainNames.isNotEmpty()) {
                                setupSpinnerView(layoutPemanenLain, karyawanLainNames)
                            } else {
                                setupSpinnerView(layoutPemanenLain, emptyList())
                                val pemanenLainSpinner =
                                    layoutPemanenLain.findViewById<MaterialSpinner>(R.id.spPanenTBS)
                                pemanenLainSpinner.setHint("Tidak Ada Karyawan Hadir")
                            }
                        }

                    }
                } else {
                    // No present karyawan - clear both lists
                    karyawanList = emptyList()
                    karyawanLainList = emptyList()

                    withContext(Dispatchers.Main) {
                        val layoutPemanen = rootView.findViewById<LinearLayout>(R.id.layoutPemanen)
                        layoutPemanen.visibility = View.VISIBLE

                        setupSpinnerView(layoutPemanen, emptyList())
                        val pemanenSpinner =
                            layoutPemanen.findViewById<MaterialSpinner>(R.id.spPanenTBS)
                        pemanenSpinner.setHint("Tidak Ada Karyawan Hadir")

                        val layoutPemanenLain =
                            rootView.findViewById<LinearLayout>(R.id.layoutPemanenLain)
                        setupSpinnerView(layoutPemanenLain, emptyList())
                        val pemanenLainSpinner =
                            layoutPemanenLain.findViewById<MaterialSpinner>(R.id.spPanenTBS)
                        pemanenLainSpinner.setHint("Tidak Ada Karyawan Hadir")

                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Error reloading all karyawan: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FeaturePanenTBSActivity,
                        "Error reloading worker data: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

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

    @SuppressLint("ClickableViewAccessibility")
    fun setupSpinnerView(
        linearLayout: LinearLayout,
        data: List<String>,
        isMultiSelect: Boolean = false,
        onItemSelected: (Int) -> Unit = {},
        onMultiItemsSelected: (List<String>, List<Int>) -> Unit = { _, _ -> }
    ) {
        try {
            // Make sure we're on the main thread
            if (Looper.myLooper() != Looper.getMainLooper()) {
                // If we're not on the main thread, post to the main thread
                Handler(Looper.getMainLooper()).post {
                    try {
                        setupSpinnerViewOnMainThread(
                            linearLayout,
                            data,
                            isMultiSelect,
                            onItemSelected,
                            onMultiItemsSelected
                        )
                    } catch (e: Exception) {
                        Log.e(
                            "SetupSpinnerView",
                            "Error setting up spinner on main thread: ${e.message}",
                            e
                        )
                        // Handle error - maybe show a fallback UI or toast notification
                    }
                }
            } else {
                // Already on main thread, proceed directly
                setupSpinnerViewOnMainThread(
                    linearLayout,
                    data,
                    isMultiSelect,
                    onItemSelected,
                    onMultiItemsSelected
                )
            }
        } catch (e: Exception) {
            Log.e("SetupSpinnerView", "Error in setupSpinnerView: ${e.message}", e)
            // Handle error - maybe show a fallback UI or toast notification
        }
    }

    // The actual implementation that should run on the main thread
    @SuppressLint("ClickableViewAccessibility")
    private fun setupSpinnerViewOnMainThread(
        linearLayout: LinearLayout,
        data: List<String>,
        isMultiSelect: Boolean = false,
        onItemSelected: (Int) -> Unit = {},
        onMultiItemsSelected: (List<String>, List<Int>) -> Unit = { _, _ -> }
    ) {
        try {
            val editText = linearLayout.findViewById<EditText>(R.id.etHomeMarkerTPH)
            val spinner = linearLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
            val tvError = linearLayout.findViewById<TextView>(R.id.tvErrorFormPanenTBS)

            // This was causing the crash - now safely on main thread
            spinner.setItems(data)

            // Hide keyboard helper
            fun ensureKeyboardHidden() {
                try {
                    val imm =
                        application.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(linearLayout.windowToken, 0)
                    editText.clearFocus()
                } catch (e: Exception) {
                    Log.e("SetupSpinnerView", "Error hiding keyboard: ${e.message}", e)
                }
            }

            // For layoutMasterTPH use multi-select with checkboxes
            if (linearLayout.id == R.id.layoutMasterTPH) {
                spinner.setOnTouchListener { _, event ->
                    try {
                        ensureKeyboardHidden()
                        if (event.action == MotionEvent.ACTION_UP) {
                            // Always use isMultiSelect=true for layoutMasterTPH
                            showPopupSearchDropdown(
                                spinner,
                                data,
                                editText,
                                linearLayout,
                                true // Force multi-select for this layout
                            ) { selectedItem, position ->
                                try {
                                    spinner.text = selectedItem // Update spinner UI
                                    tvError.visibility = View.GONE
                                    onItemSelected(position)
                                } catch (e: Exception) {
                                    Log.e(
                                        "SetupSpinnerView",
                                        "Error in item selection: ${e.message}",
                                        e
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SetupSpinnerView", "Error in touch listener: ${e.message}", e)
                    }
                    true // Consume event, preventing default behavior
                }

                try {
                    // Modified: Count directly from the value-based map
                    val selectedCount = masterEstateHasBeenChoice.count { it.value }
                    if (selectedCount > 0) {
                        // Modified: Get selected estate names directly from the keys
                        val selectedTexts =
                            masterEstateHasBeenChoice.filter { it.value }.keys.toList().sorted()
                    }

                    // Update the button text initially
                    updateDownloadMasterDataButtonText(masterEstateHasBeenChoice)
                } catch (e: Exception) {
                    Log.e("SetupSpinnerView", "Error processing selection count: ${e.message}", e)
                }
            }
            // For these layouts use regular search dropdown (no checkboxes)
            else if (linearLayout.id == R.id.layoutKemandoran || linearLayout.id == R.id.layoutPemanen ||
                linearLayout.id == R.id.layoutKemandoranLain || linearLayout.id == R.id.layoutPemanenLain
            ) {
                // Spinner with regular search (no checkboxes)
                spinner.setOnTouchListener { _, event ->
                    try {
                        ensureKeyboardHidden()
                        if (event.action == MotionEvent.ACTION_UP) {
                            // Always use isMultiSelect=false for these layouts
                            showPopupSearchDropdown(
                                spinner,
                                data,
                                editText,
                                linearLayout,
                                false // Force single-select for these layouts
                            ) { selectedItem, position ->
                                try {
                                    spinner.text = selectedItem // Update spinner UI
                                    tvError.visibility = View.GONE
                                    onItemSelected(position) // Single selection callback
                                } catch (e: Exception) {
                                    Log.e(
                                        "SetupSpinnerView",
                                        "Error in item selection: ${e.message}",
                                        e
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SetupSpinnerView", "Error in touch listener: ${e.message}", e)
                    }
                    true // Consume event, preventing default behavior
                }
            }


            if (linearLayout.id == R.id.layoutEstate) {
                // Check if jabatan contains "GM" instead of exact match
                val isGM = jabatanUser?.contains("GM", ignoreCase = true) == true

                val shouldDisable = featureName != AppUtils.ListFeatureNames.AsistensiEstateLain && !isGM

                spinner.isEnabled = !shouldDisable

                AppLogger.d("Estate spinner - Feature: $featureName, Jabatan: $jabatanUser, IsGM: $isGM, Enabled: ${spinner.isEnabled}")
                AppLogger.d("Expected GM value: ${AppUtils.ListFeatureByRoleUser.GM}")
            }

            spinner.setOnItemSelectedListener { _, position, _, item ->
                try {
                    tvError.visibility = View.GONE
                    handleItemSelection(
                        linearLayout,
                        position,
                        item.toString()
                    ) // ✅ Ensure `linearLayout` is passed
                } catch (e: Exception) {
                    Log.e("SetupSpinnerView", "Error in item selected listener: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("SetupSpinnerView", "Error in setupSpinnerViewOnMainThread: ${e.message}", e)
            // Handle error - maybe show a fallback UI or toast notification
        }
    }

    private fun setupSwitchBlokBanjir() {

        val layoutBlokBanjir = findViewById<LinearLayout>(R.id.layoutBlokBanjir)
        val switchBlokBanjir = findViewById<SwitchMaterial>(R.id.selBlokBanjir)

        val isGM = jabatanUser?.contains("GM", ignoreCase = true) == true
        layoutBlokBanjir.visibility = View.VISIBLE.takeIf { tph_otomatis_estate != 1 && !isGM } ?: View.GONE

        val tipePanenOptions = resources.getStringArray(R.array.tipe_panen_options).toList()
        val etAncak = layoutAncak.findViewById<EditText>(R.id.etHomeMarkerTPH)
        val etNomorPemanen = layoutNomorPemanen.findViewById<EditText>(R.id.etHomeMarkerTPH)
        val switchAsistensi = findViewById<SwitchMaterial>(R.id.selAsistensi)

        switchBlokBanjir.setOnCheckedChangeListener { _, isChecked ->
            val cachedKaryawanList = karyawanList
            val cachedKaryawanLainList = karyawanLainList

            if (isChecked) {
                // Stop auto scan if it's running
                autoScanEnabled = false
                switchAutoScan.isChecked = false
                autoScanHandler.removeCallbacks(autoScanRunnable)

                // Hide auto scan layout
                layoutAutoScan.visibility = View.GONE



                if (karyawanLainList.isNotEmpty()) {
                    switchAsistensi.isChecked = true
                }

                layoutTahunTanam.visibility = View.VISIBLE
                layoutBlok.visibility = View.VISIBLE
                layoutNoTPH.visibility = View.VISIBLE
                layoutAncak.visibility = View.VISIBLE
                layoutKemandoran.visibility = View.VISIBLE
                layoutPemanen.visibility = View.VISIBLE
                if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {
                    layoutSelAsistensi.visibility = View.GONE
                } else {
                    layoutSelAsistensi.visibility = View.VISIBLE
                }

                layoutTipePanen.visibility = View.VISIBLE
                selectedTPHIdByScan = null
                selectedTPHValue = null
//                kemandoranList = emptyList()
//                kemandoranLainList = emptyList()
                tphList = emptyList()

                setupSpinnerView(layoutBlok, emptyList())
                setupSpinnerView(layoutNoTPH, emptyList())
//                setupSpinnerView(layoutKemandoran, emptyList())

                // Repopulate pemanen spinners with cached data
                if (cachedKaryawanList.isNotEmpty()) {
                    val karyawanNames = cachedKaryawanList
                        .sortedBy { it.nama }
                        .map { "${it.nama} - ${it.nik ?: "N/A"}" }
                    setupSpinnerView(layoutPemanen, karyawanNames)
                } else {
                    setupSpinnerView(layoutPemanen, emptyList())
                }

                etAncak.setText("")
                ancakInput = ""

                etNomorPemanen.setText("")
                nomorPemanenInput = ""

                val tipePanenOptions = resources.getStringArray(R.array.tipe_panen_options).toList()
                setupSpinnerView(findViewById(R.id.layoutTipePanen), tipePanenOptions)

                val tipePanenLayout = findViewById<LinearLayout>(R.id.layoutTipePanen)
                val tipePanenSpinner =
                    tipePanenLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)

                if (selectedTipePanen != null && selectedTipePanen.isNotEmpty()) {
                    val tipePanenPosition = selectedTipePanen.toIntOrNull() ?: 0
                    tipePanenSpinner.setSelectedIndex(tipePanenPosition)

                    if (tipePanenPosition >= 0 && tipePanenPosition < tipePanenOptions.size) {
                        val selectedItem = tipePanenOptions[tipePanenPosition]
                        handleItemSelection(tipePanenLayout, tipePanenPosition, selectedItem)
                    }
                }

                // Reset selected values
                selectedTahunTanamValue = null
                selectedBlok = ""
                selectedBlokValue = null
                selectedTPH = ""
                selectedTPHValue = null
                selectedKemandoranLain = ""

                // Clear adapters if they exist
                selectedPemanenAdapter.clearAllWorkers()
                selectedPemanenLainAdapter.clearAllWorkers()

                blokBanjir = 1

                alertCardScanRadius.visibility = View.GONE
                alertTvScannedRadius.visibility = View.GONE
                btnScanTPHRadius.visibility = View.GONE
                titleScannedTPHInsideRadius.visibility = View.GONE
                descScannedTPHInsideRadius.visibility = View.GONE
                emptyScannedTPHInsideRadius.visibility = View.GONE
                tphScannedResultRecyclerView.visibility = View.GONE

                tvErrorScannedNotSelected.visibility = View.GONE

                resetAllCounters()

                //reset all image
                photoCount = 0
                photoCountSelfie  = 0
                photoFiles.clear()
                photoFilesSelfie.clear()
                komentarFoto.clear()
                takeFotoPreviewAdapter?.resetAllSections()

            } else {
                if (karyawanLainList.isNotEmpty()) {
                    switchAsistensi.isChecked = true
                    AppLogger.d("Masuk trus gess")
                }

                blokBanjir = 0
                setupSpinnerView(layoutBlok, emptyList())
                setupSpinnerView(layoutNoTPH, emptyList())

                etAncak.setText("")
                ancakInput = ""

                etNomorPemanen.setText("")
                nomorPemanenInput = ""

                selectedTipePanen = ""
                setupSpinnerView(layoutTipePanen, tipePanenOptions)
                layoutTahunTanam.visibility = View.GONE
                layoutBlok.visibility = View.GONE
                layoutNoTPH.visibility = View.GONE
                layoutAncak.visibility = View.GONE
                layoutKemandoran.visibility = View.GONE
                layoutPemanen.visibility = View.GONE
                layoutSelAsistensi.visibility = View.GONE
                layoutTipePanen.visibility = View.GONE
                blokList = emptyList()
                tphList = emptyList()


                selectedTahunTanamValue = null
                selectedBlok = ""
                selectedBlokValue = null
                selectedTPH = ""
                selectedTPHValue = null
                selectedKemandoranLain = ""


                selectedPemanenAdapter.clearAllWorkers()
                selectedPemanenLainAdapter.clearAllWorkers()
                resetAllCounters()

                //reset all image
                photoCount = 0
                photoCountSelfie  = 0
                photoFiles.clear()
                photoFilesSelfie.clear()
                komentarFoto.clear()
                takeFotoPreviewAdapter?.resetAllSections()

            }

            // Restore karyawan lists after all other operations
            karyawanList = cachedKaryawanList
            karyawanLainList = cachedKaryawanLainList
        }
    }


    private fun setupSwitchAsistensi() {
        val switchAsistensi = findViewById<SwitchMaterial>(R.id.selAsistensi)

        switchAsistensi.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                layoutKemandoranLain.visibility = View.VISIBLE
                layoutPemanenLain.visibility = View.VISIBLE

                asistensi = 1
            } else {
                // Hide layouts when switch is OFF
                asistensi = 0
                selectedPemanenLainAdapter.clearAllWorkers()
                layoutKemandoranLain.visibility = View.GONE
                layoutPemanenLain.visibility = View.GONE

            }
        }
    }

    private fun validateAndShowErrors(): Boolean {
        var isValid = true
        val missingFields = mutableListOf<String>()
        val errorMessages = mutableListOf<String>()

        if (!locationEnable || lat == 0.0 || lon == 0.0 || lat == null || lon == null) {
            isValid = false
            this.vibrate()
            errorMessages.add(stringXML(R.string.al_location_description_failed))
            missingFields.add("Location")
        }

        val switchAsistensi = findViewById<SwitchMaterial>(R.id.selAsistensi)
        val switchBlokBanjir = findViewById<SwitchMaterial>(R.id.selBlokBanjir)
        val isSwitchBlokBanjirEnabled = switchBlokBanjir.isChecked
        val isAsistensiEnabled = switchAsistensi.isChecked

        var isPrimaryGroupFilled = true
        var isSecondaryGroupFilled = true

        inputMappings.forEach { (layout, key, inputType) ->
            // Skip validation for kemandoran and pemanen fields initially
            if (layout.id == R.id.layoutKemandoran || layout.id == R.id.layoutPemanen ||
                layout.id == R.id.layoutKemandoranLain || layout.id == R.id.layoutPemanenLain
            ) {
                return@forEach
            }

            val tvError = layout.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
            val mcvSpinner = layout.findViewById<MaterialCardView>(R.id.MCVSpinner)
            val spinner = layout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
            val editText = layout.findViewById<EditText>(R.id.etHomeMarkerTPH)

            val isEmpty = when (inputType) {
                InputType.SPINNER -> {
                    when (layout.id) {
                        R.id.layoutAfdeling -> selectedAfdeling.isEmpty()
                        R.id.layoutTipePanen -> selectedTipePanen.isEmpty()
                        R.id.layoutNoTPH -> blokBanjir == 1 && selectedTPH.isEmpty()
                        R.id.layoutBlok -> blokBanjir == 1 && selectedBlok.isEmpty()
                        else -> spinner.selectedIndex == -1
                    }
                }

                InputType.EDITTEXT -> {
                    when (key) {
                        getString(R.string.field_ancak) -> ancakInput.trim().isEmpty()
                        getString(R.string.field_nomor_pemanen) -> nomorPemanenInput.trim().isEmpty()
                        else -> editText.text.toString().trim().isEmpty()
                    }
                }

                else -> false
            }

            val shouldValidate = featureName != AppUtils.ListFeatureNames.MutuBuah ||
                    key == getString(R.string.field_nomor_pemanen)

            AppLogger.d("shouldValidate $shouldValidate")
            if (isEmpty && shouldValidate) {
                tvError.visibility = View.VISIBLE
                mcvSpinner.strokeColor = ContextCompat.getColor(this, R.color.colorRedDark)
                missingFields.add(key)
                isValid = false
            } else {
                tvError.visibility = View.GONE
                mcvSpinner.strokeColor = ContextCompat.getColor(this, R.color.graytextdark)
            }
        }

        // Reset error indicators for primary group
        layoutPemanen.findViewById<TextView>(R.id.tvErrorFormPanenTBS).visibility = View.GONE
        layoutPemanen.findViewById<MaterialCardView>(R.id.MCVSpinner).strokeColor =
            ContextCompat.getColor(this, R.color.graytextdark)
        layoutKemandoran.findViewById<TextView>(R.id.tvErrorFormPanenTBS).visibility = View.GONE
        layoutKemandoran.findViewById<MaterialCardView>(R.id.MCVSpinner).strokeColor =
            ContextCompat.getColor(this, R.color.graytextdark)

        // Check if at least one worker is selected in primary group
        val isPemanenEmpty = selectedPemanen.isEmpty()
        val selectedPemanenWorkers = selectedPemanenAdapter.getSelectedWorkers()
        val arePemanenWorkersSelected = !selectedPemanenWorkers.isEmpty()

        // Primary group is valid if pemanen is selected with at least one worker
        isPrimaryGroupFilled = !isPemanenEmpty && arePemanenWorkersSelected

        if (isAsistensiEnabled) {
            // Reset error indicators for secondary group
            layoutPemanenLain.findViewById<TextView>(R.id.tvErrorFormPanenTBS).visibility =
                View.GONE
            layoutPemanenLain.findViewById<MaterialCardView>(R.id.MCVSpinner).strokeColor =
                ContextCompat.getColor(this, R.color.graytextdark)
            layoutKemandoranLain.findViewById<TextView>(R.id.tvErrorFormPanenTBS).visibility =
                View.GONE
            layoutKemandoranLain.findViewById<MaterialCardView>(R.id.MCVSpinner).strokeColor =
                ContextCompat.getColor(this, R.color.graytextdark)

            // Check secondary group only if primary group has no workers selected
            if (!arePemanenWorkersSelected) {
                val isPemanenLainEmpty = selectedPemanenLain.isEmpty()
                val selectedPemanenLainWorkers = selectedPemanenLainAdapter.getSelectedWorkers()
                val arePemanenLainWorkersSelected = !selectedPemanenLainWorkers.isEmpty()

                // Secondary group is filled if pemanen_lain is selected with at least one worker
                isSecondaryGroupFilled = !isPemanenLainEmpty && arePemanenLainWorkersSelected

                // If primary group has no workers AND secondary group is not filled, show errors
                if (!isSecondaryGroupFilled) {
                    isValid = false

                    if (isPemanenLainEmpty) {
                        layoutPemanenLain.findViewById<TextView>(R.id.tvErrorFormPanenTBS).visibility =
                            View.VISIBLE
                        layoutPemanenLain.findViewById<MaterialCardView>(R.id.MCVSpinner).strokeColor =
                            ContextCompat.getColor(this, R.color.colorRedDark)
                        missingFields.add(getString(R.string.field_pemanen_lain))
                    } else if (!arePemanenLainWorkersSelected) {
                        layoutPemanenLain.findViewById<TextView>(R.id.tvErrorFormPanenTBS).visibility =
                            View.VISIBLE
                        layoutPemanenLain.findViewById<TextView>(R.id.tvErrorFormPanenTBS).text =
                            stringXML(R.string.al_select_at_least_one_pemanen_lain)
                        errorMessages.add(stringXML(R.string.al_select_at_least_one_pemanen_lain))
                    }

                    errorMessages.add("Anda harus mengisi salah satu pemanen maupun asistensi")
                }
            } else {
                isSecondaryGroupFilled = true
            }
        } else {
            isSecondaryGroupFilled = false
        }

        // Check if at least one group is properly filled
        if (!isPrimaryGroupFilled && (!isSecondaryGroupFilled || !isAsistensiEnabled) && (featureName != AppUtils.ListFeatureNames.MutuBuah)) {
            isValid = false

            // Show errors for primary group if it's not properly filled
            if (isPemanenEmpty) {
                layoutPemanen.findViewById<TextView>(R.id.tvErrorFormPanenTBS).visibility =
                    View.VISIBLE
                layoutPemanen.findViewById<MaterialCardView>(R.id.MCVSpinner).strokeColor =
                    ContextCompat.getColor(this, R.color.colorRedDark)
                missingFields.add(getString(R.string.field_pemanen))
            } else if (!arePemanenWorkersSelected) {
                layoutPemanen.findViewById<TextView>(R.id.tvErrorFormPanenTBS).visibility =
                    View.VISIBLE
                layoutPemanen.findViewById<TextView>(R.id.tvErrorFormPanenTBS).text =
                    stringXML(R.string.al_select_at_least_one_pemanen)
                errorMessages.add(stringXML(R.string.al_select_at_least_one_pemanen))
            }

            // Only add general error message if no group is properly filled
            if (!isAsistensiEnabled || (isAsistensiEnabled && !isSecondaryGroupFilled)) {
                errorMessages.add(stringXML(R.string.al_must_fill_either_primary_or_secondary_group))
            }
        }

        // Continue with remaining validations that are unrelated to the groups
        if (!isSwitchBlokBanjirEnabled && selectedAfdeling.isNotEmpty() && isTriggeredBtnScanned) {
            if (isEmptyScannedTPH) {
                isValid = false
                errorMessages.add(stringXML(R.string.al_no_tph_detected_trigger_submit))

                tvErrorScannedNotSelected.text =
                    stringXML(R.string.al_no_tph_detected_trigger_submit)
                tvErrorScannedNotSelected.visibility = View.VISIBLE
            } else {
                if (selectedTPHIdByScan == null) {
                    isValid = false
                    errorMessages.add(stringXML(R.string.al_no_tph_selected_by_scanned))
                    tvErrorScannedNotSelected.text =
                        stringXML(R.string.al_no_tph_selected_by_scanned)
                    tvErrorScannedNotSelected.visibility = View.VISIBLE
                }
            }
        }

        if (!isSwitchBlokBanjirEnabled && !isTriggeredBtnScanned && selectedAfdeling.isNotEmpty() && !autoScanEnabled) {
            isValid = false
            errorMessages.add(stringXML(R.string.al_for_attempting_get_tph_in_radius))
            tvErrorScannedNotSelected.text = stringXML(R.string.al_for_attempting_get_tph_in_radius)
            tvErrorScannedNotSelected.visibility = View.VISIBLE
        }

        if (jumTBS <= 0) {
            isValid = false
            val layoutJumTBS = findViewById<ConstraintLayout>(R.id.layoutJumTBS)
            layoutJumTBS.findViewById<TextView>(R.id.tvErrorFormPanenTBS)?.apply {
                text = stringXML(R.string.al_total_tbs_description)
                visibility = View.VISIBLE
            }
            errorMessages.add(stringXML(R.string.al_total_tbs_description))
        } else {
            val layoutJumTBS = findViewById<ConstraintLayout>(R.id.layoutJumTBS)
            layoutJumTBS.findViewById<TextView>(R.id.tvErrorFormPanenTBS)?.apply {
                visibility = View.GONE
            }
        }

        if (selectedTPHValue != null && blokBanjir == 1) {
            val tphId = selectedTPHValue!!.toInt()
            val tphData = panenStoredLocal[tphId]
            val currentCount = tphData?.count ?: 0
            val jenisTPHId = selectedTPHJenisId ?: 0

            AppLogger.d("jenisTPHId $jenisTPHId")

            // Get the default limit from jenisTPHListGlobal
            val defaultLimit = jenisTPHListGlobal.find { it.id == jenisTPHId }?.limit ?: 1

            // Calculate the final limit to use
            val limitValue =
                if (jenisTPHId == 2 && jenisTPHListGlobal.find { it.id == 2 }?.jenis_tph == "induk") {
                    // Special case for jenis_tph = induk (id = 2)
                    // First check if limitTPH exists and can be converted to an Int
                    val customLimit = try {
                        tphData?.limitTPH?.toInt()
                    } catch (e: Exception) {
                        null
                    }

                    // Use the custom limit if it's valid and within range
                    if (customLimit != null && customLimit > 3 && customLimit <= 999) {
                        customLimit
                    } else {
                        // Otherwise, use the default limit (7)
                        defaultLimit // This should be 7 for jenisTPHId = 2
                    }
                } else {


                    AppLogger.d("masuk sini ges")
                    defaultLimit
                }

            AppLogger.d("limitValue $limitValue")
            // Now use the properly converted Int value for comparison
            if (currentCount >= limitValue) {
                isValid = false
                val layoutNoTPH = findViewById<LinearLayout>(R.id.layoutNoTPH)
                layoutNoTPH.findViewById<TextView>(R.id.tvErrorFormPanenTBS)?.apply {
                    text =
                        "TPH sudah terpilih $currentCount dari $limitValue kali, Harap ganti nomor TPH!"
                    visibility = View.VISIBLE
                }
                errorMessages.add("TPH sudah terpilih $currentCount dari $limitValue kali, Harap ganti nomor TPH!")
            }
        }

        if (photoCount == 0) {
            isValid = false
            errorMessages.add(stringXML(R.string.al_photo_minimal_one))

            val tvErrorNotAttachPhotos = findViewById<TextView>(R.id.tvErrorNotAttachPhotos)
            tvErrorNotAttachPhotos.visibility = View.VISIBLE
        }

        // Selfie photo validation for MutuBuah feature
        if (featureName == AppUtils.ListFeatureNames.MutuBuah && photoCountSelfie == 0) {
            isValid = false
            errorMessages.add("Wajib melakukan foto selfie dahulu")

            val tvErrorNotAttachPhotos = findViewById<TextView>(R.id.tvErrorNotAttachPhotos)
            tvErrorNotAttachPhotos.visibility = View.VISIBLE
        }

        if (!isValid) {
            vibrate()
            val combinedErrorMessage = buildString {
                val allMessages = mutableListOf<String>()
                if (missingFields.isNotEmpty()) {
                    allMessages.add(stringXML(R.string.al_pls_complete_data))
                }

                allMessages.addAll(errorMessages)
                allMessages.forEachIndexed { index, message ->
                    append("${index + 1}. $message")
                    if (index < allMessages.size - 1) append("\n")
                }
            }

            AlertDialogUtility.withSingleAction(
                this,
                stringXML(R.string.al_back),
                stringXML(R.string.al_data_not_completed),
                combinedErrorMessage,
                "warning.json",
                R.color.colorRedDark
            ) {}
        }

        return isValid
    }

    private fun handleItemSelection(
        linearLayout: LinearLayout,
        position: Int,
        selectedItem: String
    ) {

        val rootView = linearLayout.rootView
        val currentFocus = rootView.findFocus()
        if (currentFocus is EditText) {
            currentFocus.clearFocus()
            val imm =
                application.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }

        when (linearLayout.id) {

            R.id.layoutEstate -> {
                resetDependentSpinners(linearLayout.rootView)



                if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain){
                    selectedEstate = masterDeptInfoMap.entries.find { it.value == selectedItem }?.key!!
                }else{
                    selectedEstate = selectedItem
                }

                val selectedEstateId = try {
                    // Assuming you have an estate list with IDs corresponding to positions
                    // You might need to adjust this based on your estate data structure
                    val estateIds = prefManager!!.estateIdUserLogin?.split(",")?.map { it.trim().toInt() } ?: emptyList()
                    if (position < estateIds.size) {
                        estateIds[position]
                    } else {
                        AppLogger.e("Invalid estate position: $position")
                        return
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error getting estate ID: ${e.message}")
                    return
                }

                estateId = selectedEstateId.toString()

                selectedEstateIdSpinner = position

                lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        animateLoadingDots(linearLayout)
                        delay(300)
                    }

                    AppLogger.d("selectedEstate $selectedEstate")

                    if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {
                        // For AsistensiEstateLain - get afdeling list
                        val afdelingDeferred = async {
                            try {
                                datasetViewModel.getListAfdeling(selectedEstate)
                            } catch (e: Exception) {
                                AppLogger.e("Error fetching afdelingList: ${e.message}")
                                emptyList()
                            }
                        }
                        afdelingList = afdelingDeferred.await()

                        val layoutAfdeling = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutAfdeling)
                        setupSpinnerView(layoutAfdeling, afdelingList.mapNotNull { it.abbr })

                    } else {
                        // For other features - get divisi list
                        val divisiDeferred = async {
                            try {
                                datasetViewModel.getDivisiList(selectedEstateId)
                            } catch (e: Exception) {
                                AppLogger.e("Error fetching divisiList: ${e.message}")
                                emptyList()
                            }
                        }
                        divisiList = divisiDeferred.await()

                        val layoutAfdeling = linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutAfdeling)
                        setupSpinnerView(layoutAfdeling, divisiList.mapNotNull { it.divisi_abbr })
                    }

                    withContext(Dispatchers.Main) {
                        hideLoadingDots(linearLayout)
                    }
                }

            }


            R.id.layoutAfdeling -> {
                if (featureName != AppUtils.ListFeatureNames.AsistensiEstateLain) {
                    resetDependentSpinners(linearLayout.rootView)
                }
                val tvErrorScannedNotSelected =
                    findViewById<TextView>(R.id.tvErrorScannedNotSelected)
                tvErrorScannedNotSelected.visibility = View.GONE
                isTriggeredBtnScanned = false
                selectedAfdeling = selectedItem

                AppLogger.d("selectedAfdeling $selectedAfdeling")

                selectedAfdelingIdSpinner = position

                val selectedDivisiId = try {
                    if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {
                        afdelingList.find { it.abbr == selectedAfdeling }?.id
                    } else {
                        divisiList.find { it.divisi_abbr == selectedAfdeling }?.divisi
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error finding selectedDivisiId: ${e.message}")
                    null
                }
                AppLogger.d("selectedDivisiId $selectedDivisiId")
                AppLogger.d("prefManager!!.afdelingIdUserLogin ${prefManager!!.afdelingIdUserLogin}")

                if (blokBanjir == 0) {
                    alertCardScanRadius.visibility = View.GONE
                    btnScanTPHRadius.visibility = View.GONE
                    titleScannedTPHInsideRadius.visibility = View.GONE
                    descScannedTPHInsideRadius.visibility = View.GONE
                    emptyScannedTPHInsideRadius.visibility = View.GONE
                    tphScannedResultRecyclerView.visibility = View.GONE
                }

                selectedDivisiValue = selectedDivisiId
                selectedDivisiValueBackup = selectedDivisiId

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        if (estateId == null || selectedDivisiId == null) {
                            throw IllegalStateException("Estate ID or selectedDivisiId is null!")
                        }

                        // Only get blok list and tahun tanam - kemandoran setup moved to initial load
                        var tahunTanamList: List<String> = emptyList()

                        AppLogger.d(estateId.toString())
                        AppLogger.d(selectedDivisiId.toString())
                        val blokDeferred = async {
                            try {
                                val estateIdToUse =
                                    if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {
                                        selectedEstate.toInt()
                                    } else {
                                        estateId!!.toInt()
                                    }
                                datasetViewModel.getBlokList(estateIdToUse, selectedDivisiId)
                            } catch (e: Exception) {
                                AppLogger.e("Error fetching blokList: ${e.message}")
                                emptyList()
                            }
                        }
                        blokList = blokDeferred.await()
                        AppLogger.d(blokList.toString())
                        tahunTanamList = try {
                            blokList.mapNotNull { it.tahun }.distinct()
                                .sortedBy { it.toIntOrNull() }
                        } catch (e: Exception) {
                            AppLogger.e("Error processing tahunTanamList: ${e.message}")
                            emptyList()
                        }

                        if (blokBanjir == 0) {
                            latLonMap = emptyMap()
                            latLonMap = async {
                                try {
                                    val estateIdToUse =
                                        if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {
                                            selectedEstate.toInt()
                                        } else {
                                            estateId!!.toInt()
                                        }
                                    datasetViewModel.getLatLonDivisi(
                                        estateIdToUse,
                                        selectedDivisiId
                                    )
                                        .mapNotNull {
                                            val id = it.id
                                            val lat = it.lat?.toDoubleOrNull()
                                            val lon = it.lon?.toDoubleOrNull()
                                            val nomor = it.nomor ?: ""
                                            val blokKode = it.blok_kode ?: ""
                                            val jenisTPHId = it.jenis_tph_id ?: "1"

                                            if (id != null && lat != null && lon != null) {
                                                id to ScannedTPHLocation(
                                                    lat,
                                                    lon,
                                                    nomor,
                                                    blokKode,
                                                    jenisTPHId
                                                )
                                            } else {
                                                null
                                            }
                                        }
                                        .toMap()
                                } catch (e: Exception) {
                                    AlertDialogUtility.withSingleAction(
                                        this@FeaturePanenTBSActivity,
                                        stringXML(R.string.al_back),
                                        stringXML(R.string.al_failed_fetch_data),
                                        "Error fetching listLatLonAfd: ${e.message}",
                                        "warning.json",
                                        R.color.colorRedDark
                                    ) {}
                                    emptyMap()
                                }
                            }.await()
                        }

                        withContext(Dispatchers.Main) {
                            try {
                                val layoutTahunTanam =
                                    linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutTahunTanam)
                                setupSpinnerView(
                                    layoutTahunTanam,
                                    tahunTanamList.ifEmpty { emptyList() })
                                if (blokBanjir == 0) {
                                    setupScanTPHTrigger()
                                } else {

                                }
                                // Kemandoran spinner setup removed from here
                            } catch (e: Exception) {
                                AppLogger.e("Error updating UI: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Error fetching afdeling data: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@FeaturePanenTBSActivity,
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


            R.id.layoutTahunTanam -> {
//                resetTPHSpinner(linearLayout.rootView)
                val selectedTahunTanam = selectedItem.toString()
                selectedTahunTanamValue = selectedTahunTanam
                selectedTahunTanamValueBackup = selectedTahunTanam
                selectedTahunTanamIdSpinner = position

//                setupSpinnerView(layoutBlok, emptyList())
//                selectedBlok = ""
//                selectedBlokIdSpinner = 0
//                selectedBlokValue = null
//                setupSpinnerView(layoutNoTPH, emptyList())
//                selectedTPH = ""
//                selectedTPHIdSpinner = 0
//                selectedTPHJenisId = null
//                selectedTPHValue = null

                val filteredBlokCodes = blokList.filter {
                    val estateIdToUse =
                        if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {
                            selectedEstate.toInt()
                        } else {
                            estateId!!.toInt()
                        }

                    it.dept == estateIdToUse &&
                            it.divisi == selectedDivisiValue &&
                            it.tahun == selectedTahunTanamValue
                }

                val layoutBlok =
                    linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutBlok)
                if (filteredBlokCodes.isNotEmpty()) {
                    val blokNames = filteredBlokCodes.map { it.blok_kode }
                    setupSpinnerView(layoutBlok, blokNames as List<String>)
                    layoutBlok.visibility = View.VISIBLE
                } else {
                    setupSpinnerView(layoutBlok, emptyList())
                }
            }

            R.id.layoutTipePanen -> {
                selectedTipePanen = position.toString()

                if (blokBanjir == 0) {

                    layoutKemandoran.visibility = View.VISIBLE
                    layoutPemanen.visibility = View.VISIBLE

                    if (blokBanjir == 1) {
//                        setupSpinnerView(layoutBlok, emptyList())
//                        selectedBlok = ""
//                        selectedBlokIdSpinner = 0
//                        selectedBlokValue = null
//                        setupSpinnerView(layoutNoTPH, emptyList())
//                        selectedTPH = ""
//                        selectedTPHIdSpinner = 0
//                        selectedTPHJenisId = null
//                        selectedTPHValue = null

                    }

                    val switchAsistensi =
                        findViewById<LinearLayout>(R.id.layoutSelAsistensi)
                    switchAsistensi.visibility = View.VISIBLE
                    selectedTPHIdByScan?.let { tphId ->

                        val idList = listOf(tphId)

                        lifecycleScope.launch {
                            try {
                                val tphList = datasetViewModel.getTPHsByIds(idList)

                                if (tphList.isNotEmpty()) {
                                    val tph = tphList.first()
                                    Log.d(
                                        "TPH_DATA",
                                        "Retrieved TPH: ID=${tph.id}, Nomor=${tph.nomor}"
                                    )

                                } else {
                                    Toast.makeText(
                                        this@FeaturePanenTBSActivity,
                                        "TPH data not found",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Log.e("TPH_ERROR", "Error loading TPH data: ${e.message}", e)
                                Toast.makeText(
                                    this@FeaturePanenTBSActivity,
                                    "Failed to load TPH data",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } ?: run {

                        Log.w("TPH_WARNING", "No TPH selected")
                    }
                }
            }

            R.id.layoutBlok -> {
//                resetTPHSpinner(linearLayout.rootView)
                selectedBlok = selectedItem.toString()
                selectedBlokIdSpinner = position

                val selectedFieldId = try {
                    // Determine which estate ID to use
                    val estateIdToUse =
                        if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {
                            selectedEstate.toIntOrNull()
                        } else {
                            estateId?.toIntOrNull()
                        }

                    blokList.find { blok ->
                        blok.dept == estateIdToUse &&
                                blok.divisi == selectedDivisiValue &&
                                blok.tahun == selectedTahunTanamValue &&
                                blok.blok_kode == selectedBlok
                    }?.blok
                } catch (e: Exception) {
                    AppLogger.e("Error finding selected Blok ID: ${e.message}")
                    null
                }

                if (selectedFieldId != null) {
                    selectedBlokValue = selectedFieldId
                    selectedBlokValueBackup = selectedFieldId
                    AppLogger.d("Selected Blok ID: $selectedBlokValue")
                } else {
                    selectedBlokValue = null
                    AppLogger.e("Selected Blok ID is null, skipping processing.")
                    return
                }


                if (blokBanjir == 1) {
                    setupSpinnerView(layoutNoTPH, emptyList())
                    selectedTPH = ""
                    selectedTPHIdSpinner = 0
                    selectedTPHJenisId = null
                    selectedTPHValue = null
                }


                // In the handleItemSelection for R.id.layoutBlok
                lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        animateLoadingDots(linearLayout)
                        delay(1000)
                    }

                    try {
                        // Log all parameters to ensure they're not null
                        AppLogger.d("Loading TPHs with params: estateId=$estateId, selectedDivisiValue=$selectedDivisiValue, selectedTahunTanamValue=$selectedTahunTanamValue, selectedBlokValue=$selectedBlokValue")

                        if (estateId == null || selectedDivisiValue == null || selectedTahunTanamValue == null || selectedBlokValue == null) {
                            throw IllegalStateException("One or more required parameters are null!")
                        }

                        val tphDeferred = async {
                            val estateIdToUse =
                                if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {
                                    selectedEstate.toInt()
                                } else {
                                    estateId!!.toInt()
                                }

                            AppLogger.d("Getting TPH list with: estateId=$estateIdToUse, divisi=$selectedDivisiValue, tahunTanam=$selectedTahunTanamValue, blok=$selectedBlokValue")
                            datasetViewModel.getTPHList(
                                estateIdToUse,
                                selectedDivisiValue!!,
                                selectedTahunTanamValue!!,
                                selectedBlokValue!!
                            )
                        }

                        tphList = tphDeferred.await() ?: emptyList() // Avoid null crash
                        AppLogger.d("Retrieved tphList size: ${tphList.size}")

                        val normalTphList = tphList.filter { tph ->
                            // Convert jenis_tph_id to Int, defaulting to 0 if null or not a valid integer
                            val jenisId = tph.jenis_tph_id?.toIntOrNull() ?: 0

                            // Only include TPH with jenis_tph_id = 1 (normal)
                            jenisId == 1
                        }

                        panenStoredLocal.forEach { (tphId, data) ->
                            AppLogger.d("TPH ID: $tphId, Count: ${data.count}, JenisTPHId: ${data.jenisTPHId}")
                        }
                        val noTPHList = normalTphList.map { tph ->
                            val tphData = panenStoredLocal[tph.id]
                            val selectionCount = tphData?.count ?: 0
                            val jenisTPHId = tph.jenis_tph_id?.toIntOrNull() ?: 0

                            // Get the default limit from jenisTPHListGlobal
                            val defaultLimit =
                                jenisTPHListGlobal.find { it.id == jenisTPHId }?.limit ?: 1

                            // Use the default limit directly
                            val limit = defaultLimit

                            AppLogger.d("TPH ${tph.id} (${tph.nomor}): selectionCount=$selectionCount, jenisTPHId=$jenisTPHId, limit=$limit")

                            when (selectionCount) {
                                0 -> tph.nomor
                                else -> "${tph.nomor} (sudah terpilih ${selectionCount} dari ${limit} kali)"
                            }
                        }
                        AppLogger.d("Created noTPHList size: ${noTPHList.size}")
                        AppLogger.d(noTPHList.toString())
                        withContext(Dispatchers.Main) {
                            val layoutNoTPH =
                                linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutNoTPH)

                            AppLogger.d("Setting up TPH spinner with ${noTPHList.size} items")
                            if (noTPHList.isNotEmpty()) {


                                setupSpinnerView(layoutNoTPH, noTPHList as List<String>)
                                AppLogger.d("TPH spinner populated with ${noTPHList.size} items")
                            } else {
                                setupSpinnerView(layoutNoTPH, emptyList())
                                AppLogger.d("TPH spinner populated with empty list")
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Error fetching TPH data: ${e.message}")
                        AppLogger.e("Stack trace: ${e.stackTraceToString()}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@FeaturePanenTBSActivity,
                                "Error loading TPH data: ${e.message}",
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
//
//
            R.id.layoutNoTPH -> {
                val selectedText = selectedItem.trim()
                AppLogger.d("selectedText $selectedText")
                selectedTPH = selectedText.split(" (").firstOrNull()?.trim() ?: selectedText
                selectedTPHBackup = selectedText.split(" (").firstOrNull()?.trim() ?: selectedText
                selectedTPHIdSpinner = position

                val selectionCountMatch = Regex("sudah terpilih (\\d+) kali").find(selectedText)
                val selectionCount = selectionCountMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                if (selectionCount >= AppUtils.MAX_SELECTIONS_PER_TPH) {
                    Toasty.warning(
                        this,
                        "TPH ini sudah dipilih maksimal ${AppUtils.MAX_SELECTIONS_PER_TPH} kali, Mohon mengganti pilihan Nomor TPH!",
                        Toast.LENGTH_SHORT,
                        true
                    ).show()
                }

                val selectedTPHObject = try {
                    val estateIdToUse =
                        if (featureName == AppUtils.ListFeatureNames.AsistensiEstateLain) {
                            selectedEstate.toIntOrNull()
                        } else {
                            estateId?.toIntOrNull()
                        }

                    tphList?.find {
                        it.dept == estateIdToUse &&
                                it.divisi == selectedDivisiValue &&
                                it.blok == selectedBlokValue &&
                                it.tahun == selectedTahunTanamValue &&
                                it.nomor == selectedTPH
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error finding selected TPH: ${e.message}")
                    null
                }

                if (selectedTPHObject != null) {
                    selectedTPHValue = selectedTPHObject.id

                    val tphJenisId = selectedTPHObject.jenis_tph_id?.toIntOrNull() ?: 0
                    selectedTPHJenisId = jenisTPHListGlobal.find { it.id == tphJenisId }?.id

                    AppLogger.d("Selected TPH ID: $selectedTPHValue")
                    AppLogger.d("Selected TPH Jenis ID: $selectedTPHJenisId")

                    // Check if this TPH combination already has stored workers
                    val existingTPHData = panenStoredLocal.values.find { tphData ->
                        tphData.blokKode == selectedBlok && tphData.nomor == selectedTPH
                    }

                    if (existingTPHData != null && existingTPHData.workerNiks.isNotEmpty()) {
                        // Remove workers that already exist in panenStoredLocal for this blok/nomor
                        val currentPemanenWorkers =
                            selectedPemanenAdapter.getSelectedWorkers().toMutableList()
                        val currentPemanenLainWorkers =
                            selectedPemanenLainAdapter.getSelectedWorkers().toMutableList()

                        // Clear adapters temporarily
                        selectedPemanenAdapter.clearAllWorkers()
                        selectedPemanenLainAdapter.clearAllWorkers()

                        // Re-add workers that are NOT in the existing stored data
                        currentPemanenWorkers.forEach { worker ->
                            val workerNik =
                                extractNikFromWorkerName(worker.name) // You'll need this helper function
                            if (!existingTPHData.workerNiks.contains(workerNik)) {
                                selectedPemanenAdapter.addWorker(worker)
                            }
                        }

                        currentPemanenLainWorkers.forEach { worker ->
                            val workerNik =
                                extractNikFromWorkerName(worker.name) // You'll need this helper function
                            if (!existingTPHData.workerNiks.contains(workerNik)) {
                                selectedPemanenLainAdapter.addWorker(worker)
                            }
                        }

                        // Update spinner views
                        val availablePemanenWorkers = selectedPemanenAdapter.getAvailableWorkers()
                        if (availablePemanenWorkers.isNotEmpty()) {
                            setupSpinnerView(linearLayout, availablePemanenWorkers.map { it.name })
                        }

                        val availablePemanenLainWorkers =
                            selectedPemanenLainAdapter.getAvailableWorkers()
                        if (availablePemanenLainWorkers.isNotEmpty()) {
                            setupSpinnerView(
                                linearLayout,
                                availablePemanenLainWorkers.map { it.name })
                        }
                    }

                } else {
                    selectedTPHValue = null
                    selectedTPHJenisId = null
                    AppLogger.e("Selected TPH object is null, skipping processing.")
                }
            }


            R.id.layoutKemandoran -> {
                selectedKemandoran = selectedItem.toString()
                selectedKemandoranIdSpinner = position
                AppLogger.d("selectedItem $selectedItem")


                AppLogger.d("selectedKemandoran $selectedKemandoran")
                val filteredKemandoranId: Int? = try {
                    kemandoranList.find {
                        it.nama == selectedKemandoran
                    }?.id
                } catch (e: Exception) {
                    AppLogger.e("Error finding Kemandoran ID: ${e.message}")
                    null
                }

                if (filteredKemandoranId != null) {
                    AppLogger.d("Filtered Kemandoran ID: $filteredKemandoranId")

                    // Show the filter container
                    val filterContainer =
                        linearLayout.findViewById<MaterialCardView>(R.id.filter_container_pertanyaan_layout)
                    val removeFilterButton =
                        filterContainer.findViewById<ImageView>(R.id.remove_filter)
                    filterContainer.visibility = View.VISIBLE

// Set up the remove filter button click listener
                    removeFilterButton.setOnClickListener {
                        // Hide the filter container
                        vibrate()
                        filterContainer.visibility = View.GONE

                        // Don't try to reset the spinner index to -1
                        // Instead, just clear our tracking variables
                        selectedKemandoran = ""

                        // Get the current adapter items
                        val kemandoranItems = kemandoranList.map { it.nama }

                        // Recreate the spinner with the same items (this will reset to hint)
                        setupSpinnerView(linearLayout, kemandoranItems as List<String>)

                        // Reload all karyawan using the new function, but keeping the animations
                        lifecycleScope.launch {
                            try {
                                loadPemanenFullEstate(linearLayout.rootView)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@FeaturePanenTBSActivity,
                                    "Error load full pemanen estate: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                    // Use the original filtering approach
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

                            // Check if we have absensi data to filter with
                            if (presentNikSet.isNotEmpty()) {
                                // Filter karyawan list to only include those who are present
                                val presentKaryawan = karyawanList.filter { karyawan ->
                                    karyawan.nik != null && presentNikSet.contains(karyawan.nik)
                                }

                                val absentKaryawan = karyawanList.filter { karyawan ->
                                    karyawan.nik == null || !presentNikSet.contains(karyawan.nik)
                                }

                                // Log statistics
                                AppLogger.d("Total karyawan: ${karyawanList.size}")
                                AppLogger.d("Present karyawan: ${presentKaryawan.size}")
                                AppLogger.d("Absent karyawan: ${absentKaryawan.size}")

                                val karyawanNames = presentKaryawan
                                    .sortedBy { it.nama } // Sort by name alphabetically
                                    .map { "${it.nama} - ${it.nik ?: "N/A"}" }
                                AppLogger.d("Present karyawan names and NIKs: $karyawanNames")

                                val absentKaryawanNames = absentKaryawan
                                    .sortedBy { it.nama }
                                    .map { "${it.nama} - ${it.nik ?: "N/A"}" }
                                AppLogger.d("Absent karyawan names and NIKs: $absentKaryawanNames")

                                withContext(Dispatchers.Main) {
                                    val layoutPemanen =
                                        linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutPemanen)
                                    layoutPemanen.visibility = View.VISIBLE

                                    if (karyawanNames.isNotEmpty()) {
                                        setupSpinnerView(layoutPemanen, karyawanNames)
                                    } else {
                                        // Set empty and update hint
                                        setupSpinnerView(layoutPemanen, emptyList())
                                        val pemanenSpinner =
                                            layoutPemanen.findViewById<MaterialSpinner>(R.id.spPanenTBS)
                                        pemanenSpinner.setHint("Tidak Ada Karyawan Hadir")

                                    }
                                }
                            } else {
                                setupSpinnerView(layoutPemanen, emptyList())
                                val pemanenSpinner =
                                    layoutPemanen.findViewById<MaterialSpinner>(R.id.spPanenTBS)
                                pemanenSpinner.setHint("Tidak Ada Karyawan Hadir")

                            }
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching karyawan data: ${e.message}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@FeaturePanenTBSActivity,
                                    "Error loading worker data: ${e.message}",
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

            R.id.layoutPemanen -> {
                selectedPemanen = selectedItem.toString()
                AppLogger.d("Selected Pemanen: $selectedPemanen")

                // Extract NIK from the selection
                val lastDashIndex = selectedPemanen.lastIndexOf(" - ")
                val selectedNik =
                    if (lastDashIndex != -1 && lastDashIndex < selectedPemanen.length - 3) {
                        val potentialNik = selectedPemanen.substring(lastDashIndex + 3).trim()
                        if (potentialNik.all { it.isDigit() }) potentialNik else ""
                    } else ""

                // Find the selected employee in karyawanList
                var selectedEmployee = karyawanList.firstOrNull {
                    it.nik == selectedNik || it.nama?.trim()
                        ?.equals(selectedPemanen.trim(), ignoreCase = true) == true
                }

                // If not found by exact match, try partial match on name
                if (selectedEmployee == null && lastDashIndex != -1) {
                    val nameWithoutNik = selectedPemanen.substring(0, lastDashIndex).trim()
                    selectedEmployee = karyawanList.firstOrNull {
                        it.nama?.trim()?.equals(nameWithoutNik, ignoreCase = true) == true
                    }
                }

                if (selectedEmployee == null) {
                    selectedEmployee = karyawanList.firstOrNull {
                        it.nama?.contains(
                            selectedPemanen.split(" - ")[0],
                            ignoreCase = true
                        ) == true
                    }
                }

                if (selectedEmployee != null) {
                    val isWorkerAlreadyExists = panenStoredLocal.values.any { tphData ->
                        tphData.blokKode == selectedBlok &&
                                tphData.nomor == selectedTPH &&
                                tphData.workerNiks.contains(selectedEmployee!!.nik)
                    }

                    if (isWorkerAlreadyExists) {
                        Toasty.error(
                            this@FeaturePanenTBSActivity,
                            "Nama pemanen ${selectedEmployee.nama} sudah panen di nomor TPH ini",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    // Process the karyawan list to build the maps (existing code)
                    val nameCounts = mutableMapOf<String, Int>()
                    karyawanList.forEach {
                        it.nama?.trim()
                            ?.let { nama -> nameCounts[nama] = (nameCounts[nama] ?: 0) + 1 }
                    }

                    // Building the maps based on existing list
                    karyawanList.forEach {
                        it.nama?.trim()?.let { nama ->
                            val key = if (nameCounts[nama]!! > 1) {
                                "$nama - ${it.nik}"
                            } else {
                                nama
                            }
                            karyawanIdMap[key] = it.id!!
                            kemandoranIdMap[key] = it.kemandoran_id!!
                            karyawanNamaMap[key] = nama
                        }
                    }

                    // Explicitly ensure the selected employee is in the maps
                    val employeeName = selectedEmployee.nama?.trim() ?: ""
                    val selectionName = if (lastDashIndex != -1) {
                        selectedPemanen.substring(0, lastDashIndex).trim()
                    } else {
                        selectedPemanen.trim()
                    }

                    karyawanIdMap[selectedPemanen] = selectedEmployee.id!!
                    kemandoranIdMap[selectedPemanen] = selectedEmployee.kemandoran_id!!
                    karyawanIdMap[selectionName] = selectedEmployee.id!!
                    kemandoranIdMap[selectionName] = selectedEmployee.kemandoran_id!!
                    karyawanIdMap[employeeName] = selectedEmployee.id!!
                    kemandoranIdMap[employeeName] = selectedEmployee.kemandoran_id!!

                    val worker = Worker(selectedEmployee.id.toString(), selectedPemanen)
                    selectedPemanenAdapter.addWorker(worker)
                    val availableWorkers = selectedPemanenAdapter.getAvailableWorkers()

                    if (availableWorkers.isNotEmpty()) {
                        setupSpinnerView(
                            linearLayout,
                            availableWorkers.map { it.name }
                        )
                    }

                    AppLogger.d("Selected Worker: $selectedPemanen, ID: $selectedEmployee")
                } else {
                    AppLogger.d("Error: Could not find worker with name $selectedPemanen or NIK $selectedNik")
                }
            }


            R.id.layoutKemandoranLain -> {
                selectedKemandoranLain = selectedItem.toString()
                selectedKemandoranLainIdSpinner = position


                AppLogger.d("Selected Kemandoran Lain: $selectedKemandoranLain")
                val selectedIdKemandoranLain: Int? = try {
                    kemandoranLainList.find {
                        it.nama == selectedKemandoranLain
                    }?.id
                } catch (e: Exception) {
                    AppLogger.e("Error finding selected Kemandoran: ${e.message}")
                    null
                }

                if (selectedIdKemandoranLain != null) {
                    AppLogger.d("Selected ID Kemandoran Lain: $selectedIdKemandoranLain")

                    val filterContainer =
                        linearLayout.findViewById<MaterialCardView>(R.id.filter_container_pertanyaan_layout)
                    val removeFilterButton =
                        filterContainer.findViewById<ImageView>(R.id.remove_filter)
                    filterContainer.visibility = View.VISIBLE

                    // Set up the remove filter button click listener
                    removeFilterButton.setOnClickListener {
                        // Hide the filter container
                        vibrate()
                        filterContainer.visibility = View.GONE

                        // Don't try to reset the spinner index to -1
                        // Instead, just clear our tracking variables
                        selectedKemandoranLain = ""

                        // Get the current adapter items
                        val kemandoranLainItems = kemandoranLainList.map { it.nama }

                        // Recreate the spinner with the same items (this will reset to hint)
                        setupSpinnerView(linearLayout, kemandoranLainItems as List<String>)

                        // Reload all karyawan using the new function, but keeping the animations
                        lifecycleScope.launch {
                            try {
                                loadPemanenFullEstate(linearLayout.rootView)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@FeaturePanenTBSActivity,
                                    "Error load full pemanen estate: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            animateLoadingDots(linearLayout)
                            delay(1000) // 1 second delay
                        }

                        try {
                            val karyawanDeferred = async {
                                datasetViewModel.getKaryawanList(selectedIdKemandoranLain)
                            }

                            val kemandoranKaryawan = karyawanDeferred.await()

                            // Get user's afdeling ID (which is same as divisi)
                            val userAfdelingId = prefManager!!.afdelingIdUserLogin?.toInt()
                            AppLogger.d("User's afdeling ID for kemandoran filter: $userAfdelingId")

                            // Check if we have absensi data to filter with
                            if (presentNikSet.isNotEmpty()) {
                                // Filter karyawan list to only include those who are present
                                val presentKaryawan = kemandoranKaryawan.filter { karyawan ->
                                    karyawan.nik != null && presentNikSet.contains(karyawan.nik)
                                }

                                // Filter by divisi - only karyawan from other afdeling
                                if (userAfdelingId != null) {
                                    karyawanLainList = presentKaryawan.filter { karyawan ->
                                        karyawan.divisi != userAfdelingId
                                    }
                                } else {
                                    karyawanLainList = presentKaryawan
                                }

                                // Log statistics for debugging
                                AppLogger.d("Total karyawan lain for kemandoran: ${kemandoranKaryawan.size}")
                                AppLogger.d("Present karyawan for kemandoran: ${presentKaryawan.size}")
                                AppLogger.d("Other afdeling present karyawan: ${karyawanLainList.size}")

                                val namaKaryawanKemandoranLain = karyawanLainList
                                    .sortedBy { it.nama } // Sort by name alphabetically
                                    .map { "${it.nama} - ${it.nik ?: "N/A"}" }

                                withContext(Dispatchers.Main) {
                                    val layoutPemanenLain =
                                        linearLayout.rootView.findViewById<LinearLayout>(R.id.layoutPemanenLain)

                                    if (namaKaryawanKemandoranLain.isNotEmpty()) {
                                        setupSpinnerView(
                                            layoutPemanenLain,
                                            namaKaryawanKemandoranLain
                                        )
                                    } else {
                                        setupSpinnerView(
                                            layoutPemanenLain,
                                            emptyList()
                                        )
                                        val pemanenLainSpinner =
                                            layoutPemanenLain.findViewById<MaterialSpinner>(R.id.spPanenTBS)
                                        pemanenLainSpinner.setHint("Tidak Ada Karyawan Hadir")
                                    }
                                }
                            } else {
                                karyawanLainList = emptyList()

                                withContext(Dispatchers.Main) {
                                    setupSpinnerView(
                                        layoutPemanenLain,
                                        emptyList()
                                    )
                                    val pemanenLainSpinner =
                                        layoutPemanenLain.findViewById<MaterialSpinner>(R.id.spPanenTBS)
                                    pemanenLainSpinner.setHint("Tidak Ada Karyawan Hadir")
                                }
                            }
                        } catch (e: Exception) {
                            AppLogger.e("Error fetching kemandoran lain data: ${e.message}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@FeaturePanenTBSActivity,
                                    "Error loading kemandoran lain data: ${e.message}",
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
            }

            R.id.layoutPemanenLain -> {
                selectedPemanenLain = selectedItem

                val lastDashIndex = selectedPemanenLain.lastIndexOf(" - ")
                val selectedNik =
                    if (lastDashIndex != -1 && lastDashIndex < selectedPemanenLain.length - 3) {
                        val potentialNik = selectedPemanenLain.substring(lastDashIndex + 3).trim()
                        if (potentialNik.all { it.isDigit() }) {
                            potentialNik
                        } else {
                            val matchingKaryawan = karyawanLainList.firstOrNull {
                                it.nama?.trim() == selectedPemanenLain.trim()
                            }
                            matchingKaryawan?.nik ?: ""
                        }
                    } else {
                        val matchingKaryawan = karyawanLainList.firstOrNull {
                            it.nama?.trim() == selectedPemanenLain.trim()
                        }
                        matchingKaryawan?.nik ?: ""
                    }

                AppLogger.d("Selected Pemanen Lain: $selectedPemanenLain")
                AppLogger.d("Extracted NIK: $selectedNik")

                // Create NIK to employee map for lookup
                val nikToEmployeeMap = karyawanLainList.filter { it.nik != null }
                    .associateBy { it.nik!! }

                // Try to find the employee
                var selectedEmployee = nikToEmployeeMap[selectedNik]

                if (selectedEmployee == null) {
                    selectedEmployee = karyawanLainList.firstOrNull {
                        it.nama?.trim() == selectedPemanenLain.trim()
                    }
                }

                if (selectedEmployee == null && lastDashIndex != -1) {
                    val nameWithoutNik = selectedPemanenLain.substring(0, lastDashIndex).trim()
                    selectedEmployee = karyawanLainList.firstOrNull {
                        it.nama?.trim() == nameWithoutNik
                    }
                }

                if (selectedEmployee != null) {
                    // Check if worker already exists in panenStoredLocal for current blok and nomor
                    val isWorkerAlreadyExists = panenStoredLocal.values.any { tphData ->
                        tphData.blokKode == selectedBlok &&
                                tphData.nomor == selectedTPH &&
                                tphData.workerNiks.contains(selectedEmployee.nik)
                    }

                    if (isWorkerAlreadyExists) {
                        Toasty.error(
                            this@FeaturePanenTBSActivity,
                            "Nama pemanen ${selectedEmployee.nama} sudah panen di nomor TPH ini",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    // Create name counts for duplicate handling
                    val nameCounts = mutableMapOf<String, Int>()
                    karyawanLainList.forEach {
                        it.nama?.trim()
                            ?.let { nama -> nameCounts[nama] = (nameCounts[nama] ?: 0) + 1 }
                    }

                    // Set up maps with proper keys
                    karyawanLainList.forEach {
                        it.nama?.trim()?.let { nama ->
                            val key = if (nameCounts[nama]!! > 1) {
                                "$nama - ${it.nik}"
                            } else {
                                nama
                            }
                            karyawanLainIdMap[key] = it.id!!
                            kemandoranLainIdMap[key] = it.kemandoran_id!!
                            karyawanNamaLainMap[key] = nama
                        }
                    }

                    val worker = Worker(selectedEmployee.toString(), selectedPemanenLain)
                    selectedPemanenLainAdapter.addWorker(worker)

                    val availableWorkers = selectedPemanenLainAdapter.getAvailableWorkers()

                    if (availableWorkers.isNotEmpty()) {
                        setupSpinnerView(
                            linearLayout,
                            availableWorkers.map { it.name })
                    }

                    AppLogger.d("Selected Worker: $selectedPemanenLain, ID: $selectedEmployee")
                } else {
                    AppLogger.d("Error: Could not find worker with name $selectedPemanenLain or NIK $selectedNik")
                }
            }


        }
    }

    private fun setupRecyclerViewTakePreviewFoto() {
        if (featureName == AppUtils.ListFeatureNames.MutuBuah) {
            // Setup direct selfie photo layout instead of RecyclerView
            layoutSelfiePhoto = findViewById(R.id.layoutSelfiePhoto)
            layoutSelfiePhoto.visibility = View.VISIBLE

            setupSelfiePhotoLayout()
        }

        val waterMark = if(featureName == AppUtils.ListFeatureNames.MutuBuah){
            AppUtils.ListFeatureNames.MutuBuah.uppercase().replace(" ","_")
        }else{
            AppUtils.WaterMarkFotoDanFolder.WMPanenTPH
        }


        // Regular photo RecyclerView setup remains the same
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewFotoPreview)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER

        takeFotoPreviewAdapter = TakeFotoPreviewAdapter(
            5,
            cameraViewModel,
            this,
            waterMark
        ).apply {
            onPhotoDeleted = { fileName, position ->
                val index = photoFiles.indexOf(fileName)
                if (index != -1) {
                    photoFiles.removeAt(index)
                    if (index < komentarFoto.size) {
                        komentarFoto.removeAt(index)
                    }
                    photoCount--
                    AppLogger.d("Photo removed from activity: $fileName, new count: $photoCount")
                } else {
                    AppLogger.e("Failed to find photo $fileName in photoFiles list")
                }
            }
        }
        recyclerView.adapter = takeFotoPreviewAdapter
    }

    private fun updateTextInPertanyaan(linearLayout: LinearLayout, text: String) {
        // Assuming the TextView inside the LinearLayout has an ID, e.g., `tvTitleFormPanenTBS`
        val textView = linearLayout.findViewById<TextView>(R.id.tvTitleFormPanenTBS)
        textView.text = text
    }

    private fun setupSelfiePhotoLayout() {
        val imageView = layoutSelfiePhoto.findViewById<ImageView>(R.id.ivAddFoto)
        val commentTextView = layoutSelfiePhoto.findViewById<TextView>(R.id.tvPhotoComment)
        val titleCommentTextView = layoutSelfiePhoto.findViewById<TextView>(R.id.titleComment)

        // Hide comment section for selfie
        titleCommentTextView.visibility = View.GONE
        commentTextView.visibility = View.GONE

        // Set selfie camera icon
        imageView.setImageResource(R.drawable.baseline_camera_front_24)
        imageView.setColorFilter(
            ContextCompat.getColor(this, R.color.colorRedDark),
            PorterDuff.Mode.SRC_IN
        )

        // Handle selfie photo click
        imageView.setOnClickListener {
            handleSelfiePhotoClick()
        }
    }

    private fun handleSelfiePhotoClick() {
        // Check camera permission first
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                takeSelfiePhoto()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.CAMERA
            ) -> {
                showSnackbarWithSettings("Camera permission required to take photos. Enable it in Settings.")
            }
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun takeSelfiePhoto() {
        val locationData = getCurrentLocationData()
        val (currentLat, currentLon) = getCurrentCoordinates()

        // Check if GPS coordinates are available
        if (currentLat == null || currentLon == null) {
            Toast.makeText(
                this,
                "Pastikan GPS mendapatkan titik Koordinat!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Check required fields for MutuBuah
        when {
            locationData.estate.isNullOrEmpty() -> {
                Toast.makeText(this, "Pastikan sudah mengisi Estate terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return
            }
            locationData.afdeling.isNullOrEmpty() -> {
                Toast.makeText(this, "Pastikan sudah mengisi Afdeling terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Make sure the unique code contains "selfie"
        val uniqueKodeFoto = "selfie_1"
        val sourceFoto = "${locationData.estate} ${locationData.afdeling}"
        val imageView = layoutSelfiePhoto.findViewById<ImageView>(R.id.ivAddFoto)

        if (selfiePhotoFile != null) {
            // Show existing photo for edit/delete
            cameraViewModel.openZoomPhotos(
                file = selfiePhotoFile!!,
                position = "selfie_0",
                onChangePhoto = {
                    cameraViewModel.takeCameraPhotos(
                        this,
                        uniqueKodeFoto, // This should contain "selfie"
                        imageView,
                        0, // Position 0 for selfie
                        null,
                        "",
                        uniqueKodeFoto,
                        featureName!!.uppercase(), // This will add the feature name to filename
                        currentLat,
                        currentLon,
                        sourceFoto
                    )
                },
                onDeletePhoto = { _ ->
                    deleteSelfiePhoto()
                }
            )
        } else {
            // Take new selfie photo
            cameraViewModel.takeCameraPhotos(
                this,
                uniqueKodeFoto, // This should contain "selfie"
                imageView,
                0, // Position 0 for selfie
                null,
                "",
                uniqueKodeFoto,
                featureName!!.uppercase(), // This will add the feature name to filename
                currentLat,
                currentLon,
                sourceFoto,
                CameraRepository.CameraType.FRONT
            )
        }
    }

    private fun deleteSelfiePhoto() {
        selfiePhotoFile?.let { file ->
            val fileName = file.name

            // Remove from storage
            if (file.exists()) {
                file.delete()
            }

            // Clear from activity
            selfiePhotoFile = null
            photoCountSelfie = 0

            // Remove from filename list
            photoFilesSelfie.clear()

            // Reset UI
            val imageView = layoutSelfiePhoto.findViewById<ImageView>(R.id.ivAddFoto)
            imageView.setImageResource(R.drawable.baseline_camera_front_24)
            imageView.setColorFilter(
                ContextCompat.getColor(this, R.color.colorRedDark),
                PorterDuff.Mode.SRC_IN
            )

            AppLogger.d("Selfie photo deleted: $fileName")
        }
    }

    override fun getCurrentLocationData(): TakeFotoPreviewAdapter.LocationData {
        return TakeFotoPreviewAdapter.LocationData(
            estate = prefManager!!.estateUserLogin,
            afdeling = selectedAfdeling,
            blok = selectedBlok,
            tph = selectedTPH,
            blokBanjir = blokBanjir
        )
    }

    override fun getCurrentCoordinates(): Pair<Double?, Double?> {
        return Pair(lat, lon)
    }


    private fun resetAllCounters() {
        val counterMappings = listOf(
            Triple(R.id.layoutJumTBS, "Jumlah TBS", ::jumTBS),
            Triple(R.id.layoutBMentah, "Buah Mentah", ::bMentah),
            Triple(R.id.layoutBLewatMasak, "Buah Lewat Masak", ::bLewatMasak),
            Triple(R.id.layoutJjgKosong, "Janjang Kosong", ::jjgKosong),
            Triple(R.id.layoutAbnormal, "Abnormal", ::abnormal),
            Triple(R.id.layoutSeranganTikus, "Serangan Tikus", ::seranganTikus),
            Triple(R.id.layoutTangkaiPanjang, "Tangkai Panjang", ::tangkaiPanjang),
            Triple(R.id.layoutVcut, "Tidak V-Cut", ::tidakVCut)
        )
        counterMappings.forEach { (_, _, counterVar) ->
            counterVar.set(0)
        }

        counterMappings.forEach { (layoutId, _, counterVar) ->
            updateEditText(layoutId, counterVar.get()) // Update UI
        }

        formulas()
        updateCounterTextViews()
    }

    private fun setupScanTPHTrigger() {
        val alertCardScanRadius =
            findViewById<MaterialCardView>(R.id.alertCardScanRadius)
        alertCardScanRadius.visibility = View.VISIBLE

        val alertTvScannedRadius =
            findViewById<TextView>(R.id.alertTvScannedRadius)
        alertTvScannedRadius.visibility = View.VISIBLE

        val btnScanTPHRadius =
            findViewById<MaterialButton>(R.id.btnScanTPHRadius)

        if (autoScanEnabled) {
            btnScanTPHRadius.visibility = View.GONE
            selectedTPHIdByScan = null
            selectedTPHValue = null
        } else {
            btnScanTPHRadius.visibility = View.VISIBLE
        }

        // Show auto scan switch when scanning is available
        layoutAutoScan.visibility = View.VISIBLE

        val radiusText = "${radiusMinimum.toInt()} m"
        val text =
            "Lakukan Refresh saat $radiusText dalam radius terdekat TPH"
        val asterisk = "*"

        val spannableScanTPHTitle =
            SpannableString("$text $asterisk").apply {
                val startIndex = text.indexOf(radiusText)
                val endIndex = startIndex + radiusText.length

                setSpan(
                    StyleSpan(Typeface.BOLD), // Make text bold
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                setSpan(
                    StyleSpan(Typeface.ITALIC), // Make text bold
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                setSpan(
                    ForegroundColorSpan(Color.RED), // Make asterisk red
                    text.length,
                    length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

        alertTvScannedRadius.text = spannableScanTPHTitle
    }

    /**
     * Sets up a layout with increment and decrement buttons for counters.
     */
    private fun setupPaneWithButtons(
        layoutId: Int,
        textViewId: Int,
        labelText: String,
        counterVar: KMutableProperty0<Int>
    ) {
        val includedLayout = findViewById<View>(layoutId)
        val textView = includedLayout.findViewById<TextView>(textViewId)
        val etNumber = includedLayout.findViewById<EditText>(R.id.etNumber)
        val tvPercent = includedLayout.findViewById<TextView>(R.id.tvPercent)

        textView.text = labelText
        etNumber.setText(counterVar.get().toString())

        etNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                etNumber.removeTextChangedListener(this)

                try {
                    val newValue = if (s.isNullOrEmpty()) 0 else s.toString().toInt()

                    if (layoutId == R.id.layoutJumTBS) {
                        val oldValue = jumTBS
                        val sumOfOthers = abnormal + bLewatMasak + bMentah + jjgKosong

                        // For jumTBS: Don't allow changing to a value less than the sum of other counters
                        if (newValue < sumOfOthers) {
                            etNumber.setText(oldValue.toString())
                            vibrate()
                            etNumber.addTextChangedListener(this)
                            return
                        }

                        jumTBS = newValue
                        counterVar.set(jumTBS)

                        // Update dependent counters if needed
                        val updates = listOf(
                            Pair(::bMentah, R.id.layoutBMentah),
                            Pair(::bLewatMasak, R.id.layoutBLewatMasak),
                            Pair(::jjgKosong, R.id.layoutJjgKosong),
                            Pair(::abnormal, R.id.layoutAbnormal),
                            Pair(::seranganTikus, R.id.layoutSeranganTikus),
                            Pair(::tangkaiPanjang, R.id.layoutTangkaiPanjang),
                            Pair(::tidakVCut, R.id.layoutVcut)
                        )

                        Handler(Looper.getMainLooper()).post {
                            for ((counter, layout) in updates) {
                                if (counter.get() > newValue) {
                                    updateDependentCounters(layout, -1, counter, null)
                                    updateEditText(layout, counter.get())
                                }
                            }
                        }
                    } else {
                        // For other layouts (bMentah, bLewatMasak, etc.)
                        val totalOthers = when (layoutId) {
                            R.id.layoutBMentah -> abnormal + bLewatMasak + newValue + jjgKosong
                            R.id.layoutBLewatMasak -> abnormal + newValue + bMentah + jjgKosong
                            R.id.layoutJjgKosong -> abnormal + bLewatMasak + bMentah + newValue
                            R.id.layoutAbnormal -> newValue + bLewatMasak + bMentah + jjgKosong
                            else -> abnormal + bLewatMasak + bMentah + jjgKosong
                        }

                        if (jumTBS > 0 && newValue <= jumTBS && totalOthers <= jumTBS) {
                            counterVar.set(newValue)
                        } else {
                            // Reset to previous value
                            etNumber.setText(counterVar.get().toString())
                            vibrate()
                            etNumber.addTextChangedListener(this)
                            return
                        }
                    }

                    // Update formulas and UI for all cases
                    formulas()
                    updatePercentages()
                    updateCounterTextViews()

                } catch (e: NumberFormatException) {
                    etNumber.setText(counterVar.get().toString())
                    vibrate()
                }

                etNumber.addTextChangedListener(this)
            }
        })

        val btDec = includedLayout.findViewById<CardView>(R.id.btDec)
        val btInc = includedLayout.findViewById<CardView>(R.id.btInc)

        btDec.setOnClickListener {
            if (counterVar.get() > 0) {
                // Special handling for jumTBS decrement
                if (layoutId == R.id.layoutJumTBS) {
                    val sumOfOthers = abnormal + bLewatMasak + bMentah + jjgKosong
                    if (counterVar.get() - 1 < sumOfOthers) {
                        vibrate()
                        return@setOnClickListener
                    }
                }

                updateDependentCounters(
                    layoutId,
                    -1,
                    counterVar,
                    tvPercent
                )
                etNumber.setText(counterVar.get().toString())
            } else {
                vibrate()
            }
        }

        btInc.setOnClickListener {
            updateDependentCounters(
                layoutId,
                1,
                counterVar,
                tvPercent
            )
            etNumber.setText(counterVar.get().toString())
        }
    }

    // Helper function to update all percentage displays
    private fun updatePercentages() {
        findViewById<View>(R.id.layoutBMentah)?.findViewById<TextView>(R.id.tvPercent)
            ?.setText("${persenMentah}%")
        findViewById<View>(R.id.layoutBLewatMasak)?.findViewById<TextView>(R.id.tvPercent)
            ?.setText("${persenLewatMasak}%")
        findViewById<View>(R.id.layoutJjgKosong)?.findViewById<TextView>(R.id.tvPercent)
            ?.setText("${persenJjgKosong}%")
        findViewById<View>(R.id.layoutAbnormal)?.findViewById<TextView>(R.id.tvPercent)
            ?.setText("${persenAbnormal}%")
    }

    // Helper function for vibration
    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    50,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator.vibrate(50)
        }
    }


    override fun onTPHSelected(selectedTPHInLIst: ScannedTPHSelectionItem) {
        val tvErrorScannedNotSelected = findViewById<TextView>(R.id.tvErrorScannedNotSelected)
        tvErrorScannedNotSelected.visibility = View.GONE

        selectedTPHIdByScan = selectedTPHInLIst.id
        selectedBlok = selectedTPHInLIst.blockCode
        selectedTPHValue = selectedTPHIdByScan
        selectedTPH = selectedTPHInLIst.number
        if (featureName != AppUtils.ListFeatureNames.MutuBuah){
            layoutAncak.visibility = View.VISIBLE
            layoutNomorPemanen.visibility = View.VISIBLE
            layoutTipePanen.visibility = View.VISIBLE
            layoutKemandoran.visibility = View.VISIBLE
            layoutPemanen.visibility = View.VISIBLE
            layoutSelAsistensi.visibility = View.VISIBLE
        }else{
            layoutNomorPemanen.visibility = View.VISIBLE
        }

        val switchAsistensi = findViewById<SwitchMaterial>(R.id.selAsistensi)
        if (karyawanLainList.isNotEmpty()) {
            switchAsistensi.isChecked = true
        }
    }

    private fun resetSelfiePhoto() {
        if (featureName == AppUtils.ListFeatureNames.MutuBuah) {
            // Reset selfie photo data
            photoCountSelfie = 0
            photoFilesSelfie.clear()
            selfiePhotoFile = null

            // Reset selfie UI
            val imageView = layoutSelfiePhoto.findViewById<ImageView>(R.id.ivAddFoto)

            // Clear Glide cache first
            Glide.with(this).clear(imageView)

            // Set back to original camera icon
            imageView.setImageResource(R.drawable.baseline_camera_front_24)
            imageView.setColorFilter(
                ContextCompat.getColor(this, R.color.colorRedDark),
                PorterDuff.Mode.SRC_IN
            )

            // Ensure proper scale type
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER

            AppLogger.d("Selfie photo UI reset successfully")
        }
    }

    private fun showPopupSearchDropdown(
        spinner: MaterialSpinner,
        data: List<String>,
        editText: EditText,
        linearLayout: LinearLayout,
        isMultiSelect: Boolean = false,
        onItemSelected: (String, Int) -> Unit
    ) {
        val popupView =
            LayoutInflater.from(spinner.context).inflate(R.layout.layout_dropdown_search, null)
        val listView = popupView.findViewById<ListView>(R.id.listViewChoices)
        val editTextSearch = popupView.findViewById<EditText>(R.id.searchEditText)

        val scrollView = findScrollView(linearLayout)
        val rootView = linearLayout.rootView

        // Modified: Use a Map<String, Boolean> instead of Map<Int, Boolean>
        val selectedItems = if (linearLayout.id == R.id.layoutMasterTPH) {
            masterEstateHasBeenChoice // This should now be a Map<String, Boolean>
        } else {
            mutableMapOf<String, Boolean>()
        }

        fun isAnyCheckboxSelected(): Boolean {
            return selectedItems.values.any { it }
        }
        // Update button text initially
        if (linearLayout.id == R.id.layoutMasterTPH) {
            updateDownloadMasterDataButtonText(masterEstateHasBeenChoice)
        }


        val estateNameToAbbrMap = estateList.associate { it.nama to it.abbr }


        // Create PopupWindow first
        val popupWindow = PopupWindow(
            popupView,
            spinner.width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isFocusable = true
            isOutsideTouchable = true
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        var keyboardHeight = 0
        val rootViewLayout = rootView.viewTreeObserver
        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height

            // Get keyboard height
            val newKeyboardHeight = screenHeight - rect.bottom

            if (newKeyboardHeight != keyboardHeight) {
                keyboardHeight = newKeyboardHeight
                // If keyboard is shown and makes the EditText hidden
                if (keyboardHeight > 0) {
                    // Get spinner position
                    val spinnerLocation = IntArray(2)
                    spinner.getLocationOnScreen(spinnerLocation)

                    // If keyboard hides the EditText, scroll up
                    if (spinnerLocation[1] + spinner.height + popupWindow.height > rect.bottom) {
                        val scrollAmount =
                            spinnerLocation[1] - 400 // Scroll to show dropdown with extra space
                        scrollView?.smoothScrollBy(0, scrollAmount)
                    }
                }
            }
        }

        rootViewLayout.addOnGlobalLayoutListener(layoutListener)

        popupWindow.setOnDismissListener {
            rootViewLayout.removeOnGlobalLayoutListener(layoutListener)
        }

        var filteredData = data

        // Choose adapter based on selection mode
        val adapter = if (isMultiSelect) {
            object : ArrayAdapter<String>(
                spinner.context,
                R.layout.list_item_dropdown_multiple,
                R.id.text1,
                filteredData
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val checkbox = view.findViewById<CheckBox>(R.id.checkbox)
                    val textView = view.findViewById<TextView>(R.id.text1)
                    val itemValue = filteredData[position]

                    val estateAbbr = estateNameToAbbrMap[itemValue]

                    val hasExistingData = if (estateAbbr != null) {
                        prefManager?.getEstateLastModified(estateAbbr) != null
                    } else {
                        false
                    }

                    val isUserEstate = itemValue == prefManager!!.estateUserLengkapLogin

                    if (hasExistingData || isUserEstate) {
                        // Set a visual indicator that this estate already has data
                        textView.setTypeface(textView.typeface, Typeface.BOLD)
                        textView.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.greendarkerbutton
                            )
                        )

                        // Add indicator
                        val existingText = itemValue

                        if (isUserEstate) {
                            textView.text = "★ $existingText"
                            textView.setTypeface(textView.typeface, Typeface.BOLD)
                            textView.setTextColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.greendarkerbutton
                                )
                            )
                        } else {
                            textView.text = "✓ $existingText"
                        }

                        checkbox.isChecked = true
                        checkbox.isEnabled = true
                        checkbox.isClickable = false
                        checkbox.isFocusable = false

                        // Important: Set the specific color for these pre-checked items
                        checkbox.buttonTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.greendarkerbutton)
                        )

                        view.alpha = 1.0f
                        // Disable clicking on the entire row for this item
                        view.setOnTouchListener { _, _ -> true } // Consume touch to prevent popup closing
                    } else {
                        textView.setTextColor(Color.BLACK)  // Explicitly set color to black
                        textView.setTypeface(
                            textView.typeface,
                            Typeface.NORMAL
                        )  // Reset to normal typeface
                        textView.text = itemValue  // Reset text without symbols
                        checkbox.isEnabled = true
                        checkbox.isClickable = true  // Make sure this is clickable
                        checkbox.isFocusable = true  // Make sure this is focusable

                        view.isClickable = true
                        view.alpha = 1.0f
                        view.setOnTouchListener(null)  // Clear any touch listeners

                        val isChecked = selectedItems[itemValue] == true
                        checkbox.isChecked = isChecked

                        // Always set the color explicitly based on the current state
                        checkbox.buttonTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.greenBorder)
                        )

                        checkbox.setOnClickListener {
                            val nowChecked = checkbox.isChecked
                            selectedItems[itemValue] = nowChecked

                            // The color is always greenBorder for these items
                            checkbox.buttonTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(context, R.color.greenBorder)
                            )

                            val errorTextView =
                                layoutMasterTPH.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
                            if (errorTextView.visibility == View.VISIBLE && isAnyCheckboxSelected()) {
                                errorTextView.visibility = View.GONE
                            }
                            if (linearLayout.id == R.id.layoutMasterTPH) {
                                updateDownloadMasterDataButtonText(masterEstateHasBeenChoice)
                            }
                        }

                        view.setOnClickListener {
                            val nowChecked = !checkbox.isChecked
                            checkbox.isChecked = nowChecked
                            selectedItems[itemValue] = nowChecked

                            // The color is always greenBorder for these items
                            checkbox.buttonTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(context, R.color.greenBorder)
                            )

                            if (linearLayout.id == R.id.layoutMasterTPH) {
                                updateDownloadMasterDataButtonText(masterEstateHasBeenChoice)
                            }
                        }
                    }


                    return view
                }

                override fun isEnabled(position: Int): Boolean {
                    return filteredData.isNotEmpty()
                }
            }
        } else {
            object : ArrayAdapter<String>(
                spinner.context,
                android.R.layout.simple_list_item_1,
                filteredData
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val textView = view.findViewById<TextView>(android.R.id.text1)
                    textView.setTextColor(Color.BLACK)
                    return view
                }
            }
        }

        listView.adapter = adapter

        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val titleSearch = popupView.findViewById<TextView>(R.id.titleSearchDropdown)

                filteredData = if (!s.isNullOrEmpty()) {
                    titleSearch.visibility = View.VISIBLE
                    data.filter { it.contains(s, ignoreCase = true) }
                } else {
                    titleSearch.visibility = View.GONE
                    data
                }

                // Update adapter based on selection mode
                val filteredAdapter = if (isMultiSelect) {
                    object : ArrayAdapter<String>(
                        spinner.context,
                        R.layout.list_item_dropdown_multiple,
                        R.id.text1,
                        if (filteredData.isEmpty() && !s.isNullOrEmpty()) {
                            listOf("Data tidak tersedia!")
                        } else {
                            filteredData
                        }
                    ) {
                        override fun getView(
                            position: Int,
                            convertView: View?,
                            parent: ViewGroup
                        ): View {
                            val view = super.getView(position, convertView, parent)
                            val textView = view.findViewById<TextView>(R.id.text1)
                            val checkbox = view.findViewById<CheckBox>(R.id.checkbox)

                            if (filteredData.isEmpty() && !s.isNullOrEmpty()) {
                                textView.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.colorRedDark
                                    )
                                )
                                textView.setTypeface(textView.typeface, Typeface.ITALIC)
                                checkbox.visibility = View.GONE
                                view.isEnabled = false
                            } else {
                                textView.setTextColor(Color.BLACK)
                                textView.setTypeface(textView.typeface, Typeface.NORMAL)
                                checkbox.visibility = View.VISIBLE

                                // Modified: Get the current item value
                                val itemValue = filteredData[position]
                                checkbox.isChecked = selectedItems[itemValue] == true

                                // Modified: Handle checkbox clicks using value as key
                                checkbox.setOnClickListener {
                                    selectedItems[itemValue] = checkbox.isChecked
                                    if (linearLayout.id == R.id.layoutMasterTPH) {
                                        updateDownloadMasterDataButtonText(masterEstateHasBeenChoice)
                                    }
                                }

                                view.setOnClickListener {
                                    checkbox.isChecked = !checkbox.isChecked
                                    selectedItems[itemValue] = checkbox.isChecked
                                    if (linearLayout.id == R.id.layoutMasterTPH) {
                                        updateDownloadMasterDataButtonText(masterEstateHasBeenChoice)
                                    }
                                }

                                view.isEnabled = true
                            }
                            return view
                        }

                        override fun isEnabled(position: Int): Boolean {
                            return filteredData.isNotEmpty()
                        }
                    }
                } else {
                    object : ArrayAdapter<String>(
                        spinner.context,
                        android.R.layout.simple_list_item_1,
                        if (filteredData.isEmpty() && !s.isNullOrEmpty()) {
                            listOf("Data tidak tersedia!")
                        } else {
                            filteredData
                        }
                    ) {
                        override fun getView(
                            position: Int,
                            convertView: View?,
                            parent: ViewGroup
                        ): View {
                            val view = super.getView(position, convertView, parent)
                            val textView = view.findViewById<TextView>(android.R.id.text1)

                            if (filteredData.isEmpty() && !s.isNullOrEmpty()) {
                                textView.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.colorRedDark
                                    )
                                )
                                textView.setTypeface(textView.typeface, Typeface.ITALIC)
                                view.isEnabled = false
                            } else {
                                textView.setTextColor(Color.BLACK)
                                textView.setTypeface(textView.typeface, Typeface.NORMAL)
                                view.isEnabled = true
                            }
                            return view
                        }

                        override fun isEnabled(position: Int): Boolean {
                            return filteredData.isNotEmpty()
                        }
                    }
                }

                listView.adapter = filteredAdapter
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })


        // Set normal click listener for single selection
        listView.setOnItemClickListener { _, _, position, _ ->
            if (filteredData.isNotEmpty()) {
                val selectedItem = filteredData[position]
                val originalPosition = data.indexOf(selectedItem)
                spinner.text = selectedItem
                editText.setText(selectedItem)
                onItemSelected(selectedItem, originalPosition)
                handleItemSelection(linearLayout, originalPosition, selectedItem)
                popupWindow.dismiss()
            }
        }


        popupWindow.showAsDropDown(spinner)

        editTextSearch.requestFocus()
        val imm =
            spinner.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editTextSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun extractNikFromWorkerName(workerName: String): String {
        val lastDashIndex = workerName.lastIndexOf(" - ")
        return if (lastDashIndex != -1 && lastDashIndex < workerName.length - 3) {
            val potentialNik = workerName.substring(lastDashIndex + 3).trim()
            if (potentialNik.all { it.isDigit() }) potentialNik else ""
        } else ""
    }

    // Helper function to find ScrollView
    private fun findScrollView(view: View): ScrollView? {
        var parent = view.parent
        while (parent != null) {
            if (parent is ScrollView) {
                return parent
            }
            parent = parent.parent
        }
        return null
    }

    private fun updateDownloadMasterDataButtonText(selectedItems: Map<String, Boolean>) {
        val count = selectedItems.count { it.value }
        val downloadButton = findViewById<MaterialButton>(R.id.btnDownloadDataset)
        downloadButton.text = "Unduh $count master dataset"
    }

    override fun getCurrentlySelectedTPHId(): Int? {
        return selectedTPHIdByScan
    }

    private fun showErrorDialog(errorMessage: String) {
        AppLogger.d("Showing error dialog with message: $errorMessage")
        AlertDialogUtility.withSingleAction(
            this@FeaturePanenTBSActivity,
            stringXML(R.string.al_back),
            stringXML(R.string.al_failed_fetch_data),
            "${stringXML(R.string.al_failed_fetch_data_desc)}, $errorMessage",
            "warning.json",
            R.color.colorRedDark
        ) {
//            dialog.dismiss()  // Dismiss the download progress dialog
        }
    }


    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        when {

            cameraViewModel.isZoomViewVisible() -> {
                cameraViewModel.closeZoomView()
            }

            cameraViewModel.statusCamera() -> {
                cameraViewModel.closeCamera()
            }

            !isAnySelectionFilled() -> {
                // If at least one field is filled, simply return without showing the alert
                vibrate()
                val intent = Intent(this, HomePageActivity::class.java)
                startActivity(intent)
                finishAffinity()
            }

            else -> {
                vibrate()
//                backButton.isEnabled = false

                AlertDialogUtility.withTwoActions(
                    this,
                    "Keluar",
                    getString(R.string.confirmation_dialog_title),
                    getString(R.string.al_confirm_feature),
                    "warning.json",
                    ContextCompat.getColor(this, R.color.bluedarklight),
                    function = {
//                        backButton.isEnabled = true // Re-enable button when user cancels
                        val intent = Intent(this, HomePageActivity::class.java)
                        startActivity(intent)
                        finishAffinity()
                    },
                    cancelFunction = {
//                        backButton.isEnabled = true // Re-enable button when user cancels
                    }
                )
            }
        }
    }


    private fun isAnySelectionFilled(): Boolean {
        return selectedAfdeling.isNotEmpty() ||
                selectedTipePanen.isNotEmpty() ||
                selectedKemandoran.isNotEmpty() ||
                selectedPemanen.isNotEmpty() ||
                (blokBanjir == 1 && selectedTPH.isNotEmpty()) ||
                (blokBanjir == 1 && selectedBlok.isNotEmpty()) ||
                photoFiles.isNotEmpty()
    }


    @Override
    @SuppressLint("DefaultLocale")
    override fun onResume() {
        super.onResume()

        // Manually check location permission
        val isLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (isLocationGranted) {
            locationViewModel.startLocationUpdates()
            isSnackbarShown = false // Reset snackbar flag
        } else if (!isSnackbarShown) {
            showSnackbarWithSettings("Location permission is required for this app. Enable it in Settings.")
            isSnackbarShown = true // Prevent duplicate snackbars
        }


        locationViewModel.airplaneModeState.observe(this) { isAirplaneMode ->
            if (isAirplaneMode) {
                locationViewModel.stopLocationUpdates()
            } else {
                // Only restart if we have permission
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationViewModel.startLocationUpdates()
                }
            }
        }

        // Observe location updates
        locationViewModel.locationData.observe(this) { location ->
            locationEnable = true
            lat = location.latitude
            lon = location.longitude
        }

        locationViewModel.locationAccuracy.observe(this) { accuracy ->
            findViewById<TextView>(R.id.accuracyLocation).text = String.format("%.1f m", accuracy)
            currentAccuracy = accuracy
        }

        checkDateTimeSettings()
        if (activityInitialized && AppUtils.isDateTimeValid(this)) {
            startPeriodicDateTimeChecking()
        }
    }


    private fun showSnackbarWithSettings(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE)
            .setAction("Settings") {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )
                startActivity(intent)
            }
            .show()
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showSnackbarWithSettings("Location permission is required for this app. Enable it in Settings.")
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                locationViewModel.startLocationUpdates()
                isSnackbarShown = false
            } else {
                showSnackbarWithSettings("Location permission denied. Enable it in Settings.")
            }
        }

    override fun onPause() {
        super.onPause()
        // Stop auto scanning when activity is paused
        autoScanEnabled = false
        autoScanHandler.removeCallbacks(autoScanRunnable)

        // Your existing onPause code...
        locationViewModel.stopLocationUpdates()
        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
    }

    // Add this to your setupUI or initializeActivity function
    private fun initializeAutoScan() {
        setupAutoScanSwitch()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationViewModel.stopLocationUpdates()

        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
        SoundPlayer.releaseMediaPlayer()
    }

    // Update the onPhotoTaken method to handle selfie photos:
    override fun onPhotoTaken(
        photoFile: File,
        fname: String,
        resultCode: String,
        deletePhoto: View?,
        position: Int,
        komentar: String?,
        latitude: Double?,
        longitude: Double?
    ) {
        // Check if this is a selfie photo based on position and feature name
        val isSelfiePhoto = featureName == AppUtils.ListFeatureNames.MutuBuah &&
                position == 0 &&
                fname.contains("selfie")

        if (isSelfiePhoto) {
            // Handle selfie photo
            AppLogger.d("Handling selfie photo: $fname")

            val tvErrorNotAttachPhotos = findViewById<TextView>(R.id.tvErrorNotAttachPhotos)
            tvErrorNotAttachPhotos.visibility = View.GONE

            selfiePhotoFile = photoFile
            photoCountSelfie = 1

            // Clear and add to selfie list
            photoFilesSelfie.clear()
            photoFilesSelfie.add(fname)

            // Update UI
            val imageView = layoutSelfiePhoto.findViewById<ImageView>(R.id.ivAddFoto)

            // Load the image and clear any color filters
            Glide.with(this)
                .load(photoFile)
                .into(imageView)

            // Clear color filter to remove the red tint
            imageView.clearColorFilter()

            // Ensure the image view shows the photo properly
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            AppLogger.d("Selfie photo processed successfully: $fname")

        } else {
            // Handle regular photos (existing code)
            AppLogger.d("Handling regular photo: $fname at position: $position")

            val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewFotoPreview)
            val adapter = recyclerView.adapter as? TakeFotoPreviewAdapter

            val tvErrorNotAttachPhotos = findViewById<TextView>(R.id.tvErrorNotAttachPhotos)
            tvErrorNotAttachPhotos.visibility = View.GONE

            adapter?.addPhotoFile("$position", photoFile)

            finalLat = latitude
            finalLon = longitude

            if (position < photoFiles.size) {
                photoFiles[position] = fname
                komentarFoto[position] = komentar ?: ""
            } else {
                while (photoFiles.size < position) {
                    photoFiles.add("")
                    komentarFoto.add("")
                }
                photoFiles.add(fname)
                komentarFoto.add(komentar ?: "")
                photoCount++
            }

            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as? TakeFotoPreviewAdapter.FotoViewHolder
            viewHolder?.let {
                Glide.with(this)
                    .load(photoFile)
                    .into(it.imageView)
            }
        }
    }

    private val autoScanRunnable = object : Runnable {
        override fun run() {
            if (autoScanEnabled) {
                progressBarScanTPHAuto.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed({
                    checkScannedTPHInsideRadius()
                    autoScanHandler.postDelayed(this, autoScanInterval)
                }, 400) // Shorter delay for auto-scan (300ms)
            }
        }
    }

    // Add this function to setup the auto scan switch
    private fun setupAutoScanSwitch() {
        layoutAutoScan = findViewById(R.id.layoutAutoScan)
        switchAutoScan = findViewById(R.id.switchAutoScan)

        switchAutoScan.setOnCheckedChangeListener { _, isChecked ->
            autoScanEnabled = isChecked

            if (isChecked) {
                autoScanHandler.post(autoScanRunnable)
                btnScanTPHRadius.visibility = View.GONE
                Toast.makeText(
                    this@FeaturePanenTBSActivity,
                    "Auto-refresh TPH setiap 5 detik diaktifkan",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Stop automatic scanning
                autoScanHandler.removeCallbacks(autoScanRunnable)
                btnScanTPHRadius.visibility = View.VISIBLE
                Toast.makeText(
                    this@FeaturePanenTBSActivity,
                    "Auto-refresh TPH dinonaktifkan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


}
