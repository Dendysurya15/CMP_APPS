package com.cbi.mobile_plantation.ui.view.panenTBS

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.repository.AppRepository
import com.cbi.mobile_plantation.data.model.KaryawanModel
import com.cbi.mobile_plantation.data.model.KemandoranModel
import com.cbi.mobile_plantation.ui.adapter.ListPanenTPHAdapter
import com.cbi.mobile_plantation.ui.view.espb.FormESPBActivity
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.view.ScanQR
import com.cbi.mobile_plantation.ui.viewModel.ESPBViewModel

import com.cbi.mobile_plantation.ui.viewModel.PanenViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.setMaxBrightness
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.AppUtils.vibrate
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.SoundPlayer
import com.cbi.mobile_plantation.utils.playSound
import com.cbi.mobile_plantation.utils.setResponsiveTextSizeWithConstraints
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Suppress("IMPLICIT_CAST_TO_ANY")
class ListPanenTBSActivity : AppCompatActivity() {
    private var featureName = ""
    private var listTPHDriver = ""
    private lateinit var panenViewModel: PanenViewModel
    private lateinit var espbViewModel: ESPBViewModel
    private lateinit var listAdapter: ListPanenTPHAdapter
    private lateinit var loadingDialog: LoadingDialog
    private var currentState = 0 // 0 for tersimpan, 1 for terscan
    private var prefManager: PrefManager? = null
    private var isSettingUpCheckbox = false
    private var activityInitialized = false

    private var globalFormattedDate: String = ""

    // Add views for buttons and counters
    private lateinit var cardTersimpan: MaterialCardView
    private lateinit var cardTerscan: MaterialCardView
    private lateinit var cardRekapPerPemanen: MaterialCardView
    private lateinit var counterTersimpan: TextView
    private lateinit var counterTerscan: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var speedDial: SpeedDialView

    private var isAscendingOrder = true

    private lateinit var searchEditText: EditText
    private lateinit var sortButton: ImageView // Add this at class level
    private lateinit var filterSection: LinearLayout
    private lateinit var filterName: TextView
    private lateinit var removeFilter: ImageView
    private var originalData: List<Map<String, Any>> = emptyList() // Store original data order

    private var userName: String? = null
    private var estateName: String? = null
    private var jabatanUser: String? = null
    private var afdelingUser: String? = null
    private lateinit var btnAddMoreTph: FloatingActionButton
    private var tph1IdPanen = ""
    private var tph1NoIdPanen = ""
    private var mappedData: List<Map<String, Any>> = emptyList()

    private var espbId = 0
    private var jjg = 0
    private var noespb = "NULL"
    private var blok = "NULL"
    private var tph = 0
    private var tph0 = ""
    private var tph1 = ""
    private var tphListScan: List<String> = emptyList()

    private var blok_jjg = "NULL"
    private var nopol = "NULL"
    private var driver = "NULL"
    private var pemuat_id = "NULL"
    private var kemandoran_id = "NULL"
    private var pemuat_nik = "NULL"
    private var transporter_id = 0
    private var mill_id = 0
    private var created_by_id = 0
    private var no_espb = "NULL"
    private var tph0QR = "NULL"
    private var tph1QR = "NULL"
    private var creatorInfo = "NULL"
    private var dateTime = "NULL"
    private lateinit var filterAllData: CheckBox
    private var idsToUpdate = "NULL"
    private val todayDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))
    private val todayDate = todayDateFormat.format(Date())
    private lateinit var ll_detail_espb: LinearLayout
    private lateinit var dateButton: Button
    private val dateTimeCheckHandler = Handler(Looper.getMainLooper())
    private val dateTimeCheckRunnable = object : Runnable {
        override fun run() {
            checkDateTimeSettings()
            dateTimeCheckHandler.postDelayed(this, AppUtils.DATE_TIME_CHECK_INTERVAL)
        }
    }
    private val dateIndexMap = mutableMapOf<String, Int>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_panen_tbs)
        //cek tanggal otomatis
        checkDateTimeSettings()
    }

    fun openDatePicker(view: View) {
        initMaterialDatePicker()
    }

    private fun initMaterialDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Pilih Tanggal")
        builder.setSelection(MaterialDatePicker.todayInUtcMilliseconds())

        val datePicker = builder.build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selection
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val displayDate = AppUtils.makeDateString(day, month, year)
            dateButton.text = displayDate

            val formattedDate = AppUtils.formatDateForBackend(day, month, year)
            globalFormattedDate = formattedDate
            AppUtils.setSelectedDate(formattedDate)
            processSelectedDate(formattedDate)
        }
        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun processSelectedDate(selectedDate: String) {
//        loadingDialog.show()
//        loadingDialog.setMessage("Sedang mengambil data...", true)

        val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainer)
        val nameFilterDate = findViewById<TextView>(R.id.name_filter_date)
        val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_date)

        val displayDate = AppUtils.formatSelectedDateForDisplay(selectedDate)
        nameFilterDate.text = displayDate


        if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
            if (currentState == 0) {
                panenViewModel.loadTPHNonESPB(0, 0, 0, selectedDate)
                panenViewModel.countTPHNonESPB(0, 0, 0, selectedDate)
                panenViewModel.countTPHESPB(1, 0, 0, selectedDate)
            } else if (currentState == 1) {
                panenViewModel.loadTPHESPB(1, 0, 0, selectedDate)
                panenViewModel.countTPHNonESPB(0, 0, 0, selectedDate)
                panenViewModel.countTPHESPB(1, 0, 0, selectedDate)
            } else if (currentState == 2) {
                panenViewModel.loadTPHNonESPB(1, 0, 0, selectedDate)
                panenViewModel.countTPHNonESPB(0, 0, 0, selectedDate)
                panenViewModel.countTPHESPB(1, 0, 0, selectedDate)
            }
        } else if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
            if (currentState == 0) {
                panenViewModel.loadTPHNonESPB(0, 0, 1, selectedDate)
                panenViewModel.countTPHNonESPB(0, 0, 1, selectedDate)
                panenViewModel.countTPHESPB(0, 1, 1, selectedDate)
            } else {
                panenViewModel.loadTPHESPB(0, 1, 1, selectedDate)
                panenViewModel.countTPHNonESPB(0, 0, 1, selectedDate)
                panenViewModel.countTPHESPB(0, 1, 1, selectedDate)
            }
        } else if (featureName == AppUtils.ListFeatureNames.BuatESPB) {
            panenViewModel.loadTPHNonESPB(0, 0, 1, selectedDate)
        }

        removeFilterDate.setOnClickListener {
            filterDateContainer.visibility = View.GONE
//            loadingDialog.show()
//            loadingDialog.setMessage("Sedang mengambil data...", true)
            // Get today's date in backend format
            val todayBackendDate = AppUtils.formatDateForBackend(
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                Calendar.getInstance().get(Calendar.MONTH) + 1,
                Calendar.getInstance().get(Calendar.YEAR)
            )

            // Reset the selected date in your utils
            AppUtils.setSelectedDate(todayBackendDate)

            // Update the dateButton to show today's date
            val todayDisplayDate = AppUtils.getTodaysDate()
            dateButton.text = todayDisplayDate

            if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                if (currentState == 0) {
                    panenViewModel.loadTPHNonESPB(0, 0, 0, todayBackendDate)
                    panenViewModel.countTPHNonESPB(0, 0, 0, todayBackendDate)
                    panenViewModel.countTPHESPB(1, 0, 0, todayBackendDate)
                } else if (currentState == 1) {
                    panenViewModel.loadTPHESPB(1, 0, 0, todayBackendDate)
                    panenViewModel.countTPHNonESPB(0, 0, 0, todayBackendDate)
                    panenViewModel.countTPHESPB(1, 0, 0, todayBackendDate)
                } else if (currentState == 2) {
                    panenViewModel.loadTPHNonESPB(1, 0, 0, todayBackendDate)
                    panenViewModel.countTPHNonESPB(0, 0, 0, todayBackendDate)
                    panenViewModel.countTPHESPB(1, 0, 0, todayBackendDate)
                }
            } else if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
                if (currentState == 0) {
                    panenViewModel.loadTPHNonESPB(0, 0, 1, todayBackendDate)
                    panenViewModel.countTPHNonESPB(0, 0, 1, todayBackendDate)
                    panenViewModel.countTPHESPB(0, 1, 1, todayBackendDate)
                } else {
                    panenViewModel.loadTPHESPB(0, 1, 1, todayBackendDate)
                    panenViewModel.countTPHNonESPB(0, 0, 1, todayBackendDate)
                    panenViewModel.countTPHESPB(0, 1, 1, todayBackendDate)
                }
            } else if (featureName == AppUtils.ListFeatureNames.BuatESPB) {
                panenViewModel.loadTPHNonESPB(0, 0, 1, todayBackendDate)
            }

        }

        filterDateContainer.visibility = View.VISIBLE
    }

    private fun setupUI() {
        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressed() }

        globalFormattedDate = AppUtils.currentDate
        if (featureName == AppUtils.ListFeatureNames.BuatESPB || featureName == AppUtils.ListFeatureNames.DetailESPB) {
            findViewById<LinearLayout>(R.id.calendarContainer).visibility = View.GONE
            findViewById<LinearLayout>(R.id.filterDateContainer).visibility = View.GONE


        } else {
            findViewById<LinearLayout>(R.id.calendarContainer).visibility = View.VISIBLE
            dateButton = findViewById(R.id.calendarPicker)
            dateButton.text = AppUtils.getTodaysDate()

            filterAllData = findViewById(R.id.calendarCheckbox)

            filterAllData.setOnCheckedChangeListener { _, isChecked ->
                val selectedDate = globalFormattedDate // Get the selected date
                val filterDateContainer = findViewById<LinearLayout>(R.id.filterDateContainer)
                val nameFilterDate = findViewById<TextView>(R.id.name_filter_date)
                if (isChecked) {
//                    loadingDialog.show()
//                    loadingDialog.setMessage("Sedang mengambil data...", true)


                    filterDateContainer.visibility = View.VISIBLE
                    nameFilterDate.text = "Semua Data"

                    dateButton.isEnabled = false
                    dateButton.alpha = 0.5f

                    if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                        if (currentState == 0) {
                            panenViewModel.loadTPHNonESPB(0, 0, 0)
                            panenViewModel.countTPHNonESPB(0, 0, 0)
                            panenViewModel.countTPHESPB(1, 0, 0)
                        } else if (currentState == 1) {
                            panenViewModel.loadTPHESPB(1, 0, 0)
                            panenViewModel.countTPHNonESPB(0, 0, 0)
                            panenViewModel.countTPHESPB(1, 0, 0)
                        } else if (currentState == 2) {
                            panenViewModel.loadTPHNonESPB(1, 0, 0)
                            panenViewModel.countTPHNonESPB(0, 0, 0)
                            panenViewModel.countTPHESPB(1, 0, 0)
                        }
                    } else if (featureName == AppUtils.ListFeatureNames.BuatESPB) {
                        panenViewModel.loadTPHNonESPB(0, 0, 1)
                    } else if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
                        if (currentState == 0) {
                            panenViewModel.loadTPHNonESPB(0, 0, 1)
                            panenViewModel.countTPHNonESPB(0, 0, 1)
                            panenViewModel.countTPHESPB(0, 1, 1)
                        } else if (currentState == 1) {
                            panenViewModel.loadTPHESPB(0, 1, 1)
                            panenViewModel.countTPHNonESPB(0, 0, 1)
                            panenViewModel.countTPHESPB(0, 1, 1)
                        }
                    }
                } else {
//                    loadingDialog.show()
//                    loadingDialog.setMessage("Sedang mengambil data...", true)


                    val displayDate = formatGlobalDate(globalFormattedDate)
                    dateButton.text = displayDate

                    if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                        if (currentState == 0) {
                            panenViewModel.loadTPHNonESPB(0, 0, 0, globalFormattedDate)
                            panenViewModel.countTPHNonESPB(0, 0, 0, globalFormattedDate)
                            panenViewModel.countTPHESPB(1, 0, 0, globalFormattedDate)
                        } else if (currentState == 1) {
                            panenViewModel.loadTPHESPB(1, 0, 0, globalFormattedDate)
                            panenViewModel.countTPHNonESPB(0, 0, 0, globalFormattedDate)
                            panenViewModel.countTPHESPB(1, 0, 0, globalFormattedDate)
                        } else if (currentState == 2) {
                            panenViewModel.loadTPHNonESPB(1, 0, 0, globalFormattedDate)
                            panenViewModel.countTPHNonESPB(0, 0, 0, globalFormattedDate)
                            panenViewModel.countTPHESPB(1, 0, 0, globalFormattedDate)
                        }
                    } else if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
                        if (currentState == 0) {
                            panenViewModel.loadTPHNonESPB(0, 0, 1, globalFormattedDate)
                            panenViewModel.countTPHNonESPB(0, 0, 1, globalFormattedDate)
                            panenViewModel.countTPHESPB(0, 1, 1, globalFormattedDate)
                        } else {
                            panenViewModel.loadTPHESPB(0, 1, 1, globalFormattedDate)
                            panenViewModel.countTPHNonESPB(0, 0, 1, globalFormattedDate)
                            panenViewModel.countTPHESPB(0, 1, 1, globalFormattedDate)
                        }
                    } else if (featureName == AppUtils.ListFeatureNames.BuatESPB) {
                        panenViewModel.loadTPHNonESPB(0, 0, 1, globalFormattedDate)
                    }

//                    filterDateContainer.visibility = View.GONE
                    nameFilterDate.text = displayDate
                    dateButton.isEnabled = true
                    dateButton.alpha = 1f // Make the button appear darker
                    Log.d("FilterAllData", "Checkbox is UNCHECKED. Button enabled.")
                }


                val removeFilterDate = findViewById<ImageView>(R.id.remove_filter_date)

                removeFilterDate.setOnClickListener {
                    if (filterAllData.isChecked) {
                        filterAllData.isChecked = false
                    }

                    filterDateContainer.visibility = View.GONE


//            loadingDialog.show()
//            loadingDialog.setMessage("Sedang mengambil data...", true)
                    // Get today's date in backend format
                    val todayBackendDate = AppUtils.formatDateForBackend(
                        Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                        Calendar.getInstance().get(Calendar.MONTH) + 1,
                        Calendar.getInstance().get(Calendar.YEAR)
                    )

                    // Reset the selected date in your utils
                    AppUtils.setSelectedDate(todayBackendDate)

                    // Update the dateButton to show today's date
                    val todayDisplayDate = AppUtils.getTodaysDate()
                    dateButton.text = todayDisplayDate

                    if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                        if (currentState == 0) {
                            panenViewModel.loadTPHNonESPB(0, 0, 0, todayBackendDate)
                            panenViewModel.countTPHNonESPB(0, 0, 0, todayBackendDate)
                            panenViewModel.countTPHESPB(1, 0, 0, todayBackendDate)
                        } else if (currentState == 1) {
                            panenViewModel.loadTPHESPB(1, 0, 0, todayBackendDate)
                            panenViewModel.countTPHNonESPB(0, 0, 0, todayBackendDate)
                            panenViewModel.countTPHESPB(1, 0, 0, todayBackendDate)
                        } else if (currentState == 2) {
                            panenViewModel.loadTPHNonESPB(1, 0, 0, todayBackendDate)
                            panenViewModel.countTPHNonESPB(0, 0, 0, todayBackendDate)
                            panenViewModel.countTPHESPB(1, 0, 0, todayBackendDate)
                        }
                    } else if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
                        if (currentState == 0) {
                            panenViewModel.loadTPHNonESPB(0, 0, 1, todayBackendDate)
                            panenViewModel.countTPHNonESPB(0, 0, 1, todayBackendDate)
                            panenViewModel.countTPHESPB(0, 1, 1, todayBackendDate)
                        } else {
                            panenViewModel.loadTPHESPB(0, 1, 1, todayBackendDate)
                            panenViewModel.countTPHNonESPB(0, 0, 1, todayBackendDate)
                            panenViewModel.countTPHESPB(0, 1, 1, todayBackendDate)
                        }
                    } else if (featureName == AppUtils.ListFeatureNames.BuatESPB) {
                        panenViewModel.loadTPHNonESPB(0, 0, 1, todayBackendDate)
                    }

                }
            }
        }

        listTPHDriver = try {
            AppUtils.readJsonFromEncryptedBase64Zip(
                intent.getStringExtra("scannedResult").toString()
            ).toString()
        } catch (e: Exception) {
            Toasty.error(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            ""
        }

        Log.d("listTPHDriver", listTPHDriver.toString())

        prefManager = PrefManager(this)
        userName = prefManager!!.nameUserLogin
        estateName = prefManager!!.estateUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin

        // Get previous TPH data if available
        val previousTph1 = intent.getStringExtra("previous_tph_1") ?: ""
        val previousTph0 = intent.getStringExtra("previous_tph_0") ?: ""
        val previousTph1IdPanen = intent.getStringExtra("previous_tph_1_id_panen") ?: ""

        if (previousTph1.isNotEmpty()) {
            Log.d("ListPanenTBSActivity", "Previous tph1 found: $previousTph1")
            tph1 = previousTph1
        }

        if (previousTph0.isNotEmpty()) {
            Log.d("ListPanenTBSActivity", "Previous tph0 found: $previousTph0")
            tph0 = previousTph0
        }

        if (previousTph1IdPanen.isNotEmpty()) {
            Log.d("ListPanenTBSActivity", "Previous tph1IdPanen found: $previousTph1IdPanen")
            tph1IdPanen = previousTph1IdPanen
        }

        if (listTPHDriver.isNotEmpty()) {

//playSound(R.raw.berhasil_scan)
            // Extract TPH IDs from the current scan
            val currentScanTphIds = try {
                val tphString = listTPHDriver
                    .removePrefix("""{"tph":"""")
                    .removeSuffix(""""}""")
                tphString
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }

            // If we have previous scan data stored in tph1, extract TPH IDs
            val previousScanTphIds = if (tph1.isNotEmpty()) {
                // Get the TPH IDs from tph1 string
                tph1.split(";").mapNotNull { entry ->
                    try {
                        entry.split(",").firstOrNull()
                    } catch (e: Exception) {
                        Toasty.error(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        null
                    }
                }.joinToString(";")
            } else ""

            // Combine previous and current scan TPH IDs
            val combinedTphIds =
                if (previousScanTphIds.isNotEmpty() && currentScanTphIds.isNotEmpty()) {
                    """{"tph":"$previousScanTphIds;$currentScanTphIds"}"""
                } else if (currentScanTphIds.isNotEmpty()) {
                    """{"tph":"$currentScanTphIds"}"""
                } else if (previousScanTphIds.isNotEmpty()) {
                    """{"tph":"$previousScanTphIds"}"""
                } else ""

            // Update listTPHDriver with combined data
            if (combinedTphIds.isNotEmpty()) {
                listTPHDriver = combinedTphIds
                Log.d("ListPanenTBSActivity", "Combined TPH IDs: $listTPHDriver")
            }
        }

        setupHeader()
        initViewModel()
        initializeViews()
        loadingDialog = LoadingDialog(this)
        setupRecyclerView()
        setupSearch()
        setupObservers()
        if (featureName != "Buat eSPB" && featureName != "Detail eSPB") {
            setupSpeedDial()
            setupCheckboxControl()  // Add this
        }
        setupCardListeners()
        initializeFilterViews()
        setupSortButton()
        currentState = 0
        setActiveCard(cardTersimpan)
        if (featureName == "Detail eSPB") {
            // set to gone card_item_tersimpan
            cardTersimpan.visibility = View.GONE
            // set to gone card_item_terscan
            cardTerscan.visibility = View.GONE
            val app = AppRepository(application)
            val espbViewModelFactory = ESPBViewModel.ESPBViewModelFactory(app)
            espbViewModel = ViewModelProvider(this, espbViewModelFactory)[ESPBViewModel::class.java]
            espbId = try {
                intent.getStringExtra("id_espb").toString().toInt()
            } catch (e: Exception) {
                Toasty.error(this, "Error mengambil id eSPB: ${e.message}", Toast.LENGTH_LONG)
                    .show()
                0
            }

            findViewById<LinearLayout>(R.id.calendarContainer).visibility = View.GONE
            findViewById<LinearLayout>(R.id.filterDateContainer).visibility = View.GONE
        }
        lifecycleScope.launch {
            if (featureName == "Buat eSPB") {
                findViewById<SpeedDialView>(R.id.dial_tph_list).visibility = View.GONE
                val isStopScan = intent.getBooleanExtra("IS_STOP_SCAN_ESPB", false)
                if (!isStopScan) {
                    playSound(R.raw.berhasil_scan)
                }
                panenViewModel.loadTPHNonESPB(0, 0, 1, AppUtils.currentDate)
                findViewById<HorizontalScrollView>(R.id.horizontalCardFeature).visibility =
                    View.GONE
            }else if (featureName == "Rekap panen dan restan") {

                findViewById<SpeedDialView>(R.id.dial_tph_list).visibility = View.GONE
                findViewById<TextView>(R.id.list_item_tersimpan).text = "Rekap TPH"
                findViewById<TextView>(R.id.list_item_terscan).text = "TPH Menjadi E-SPB"
                panenViewModel.loadTPHNonESPB(0, 0, 1, AppUtils.currentDate)
                panenViewModel.countTPHNonESPB(0, 0, 1, AppUtils.currentDate)
                panenViewModel.countTPHESPB(0, 1, 1, AppUtils.currentDate)

            } else if (featureName == "Detail eSPB") {
                val btnEditEspb = findViewById<FloatingActionButton>(R.id.btnEditEspb)
                btnEditEspb.visibility = View.VISIBLE
                ll_detail_espb = findViewById<LinearLayout>(R.id.ll_detail_espb)
                ll_detail_espb.visibility = View.VISIBLE
                espbViewModel.getESPBById(espbId)
                espbViewModel.espbEntity.observe(this@ListPanenTBSActivity) { espbWithRelations ->
                    if (espbWithRelations != null) {
                        try {
                            // Extract ESPB data
                            val espb = espbWithRelations

                            // Find all included layouts
                            val tvNoEspb = findViewById<View>(R.id.tv_no_espb)
                            val tvNoPol = findViewById<View>(R.id.tv_no_pol)
                            val tvTransporter = findViewById<View>(R.id.tv_transporter)
                            val tvDriver = findViewById<View>(R.id.tv_driver)
                            val tvMill = findViewById<View>(R.id.tv_mill)
                            val tvMekanisasi = findViewById<View>(R.id.tv_mekanisasi)
//                            val tvDraft = findViewById<View>(R.id.tv_draft)

                            blok_jjg = espb.blok_jjg
                            nopol = espb.nopol
                            driver = espb.driver
                            pemuat_id = espb.pemuat_id
                            transporter_id = espb.transporter_id
                            mill_id = espb.mill_id
                            created_by_id = espb.created_by_id
                            no_espb = espb.noESPB
                            tph0QR = espb.tph0
                            tph1QR = espb.tph1
                            creatorInfo = espb.creator_info
                            dateTime = espb.created_at
                            kemandoran_id = espb.kemandoran_id
                            pemuat_nik = espb.pemuat_nik
                            tph1 = espb.tph1
                            tph0 = espb.tph0
                            idsToUpdate = espb.ids_to_update

                            btnEditEspb.setOnClickListener {
                                AlertDialogUtility.withTwoActions(
                                    this@ListPanenTBSActivity,
                                    "EDIT",
                                    "Edit eSPB",
                                    "Apakah anda yakin ingin mengedit eSPB ini?",
                                    "warning.json",
                                    function = {
                                        val intent = Intent(
                                            this@ListPanenTBSActivity,
                                            FormESPBActivity::class.java
                                        )
                                        intent.putExtra("tph_1", tph1)
                                        Log.d("ListPanenTBSActivity", "tph1: $tph1")
                                        intent.putExtra("tph_0", tph0)
                                        Log.d("ListPanenTBSActivity", "tph0: $tph0")
                                        intent.putExtra("id_espb", espbId)
                                        Log.d("ListPanenTBSActivity", "id_espb: $espbId")
                                        intent.putExtra("tph_1_id_panen", idsToUpdate)
                                        Log.d(
                                            "ListPanenTBSActivity",
                                            "tph_1_id_panen: $idsToUpdate"
                                        )
                                        playSound(R.raw.berhasil_edit_data)
                                        intent.putExtra("FEATURE_NAME", featureName)
                                        Log.d("ListPanenTBSActivity", "FEATURE_NAME: $featureName")
                                        startActivity(intent)
                                        finishAffinity()
                                    }
                                )
                            }

                            // Set No eSPB
                            tvNoEspb.findViewById<TextView>(R.id.tvTitleEspb).text = "No eSPB"
                            noespb = espb.noESPB
                            panenViewModel.getAllPanenWhereESPB(noespb)
                            tvNoEspb.findViewById<TextView>(R.id.tvSubTitleEspb).text = espb.noESPB

                            // Set No Polisi
                            tvNoPol.findViewById<TextView>(R.id.tvTitleEspb).text = "No Polisi"
                            tvNoPol.findViewById<TextView>(R.id.tvSubTitleEspb).text = espb.nopol

                            // Launch coroutines to fetch transporter and mill names
                            lifecycleScope.launch {
                                try {
                                    // Set Transporter
                                    tvTransporter.findViewById<TextView>(R.id.tvTitleEspb).text =
                                        "Transporter"
                                    val transporterName = withContext(Dispatchers.IO) {
                                        try {
                                            espbViewModel.getTransporterNameById(espb.transporter_id)
                                                ?: "Internal"
                                        } catch (e: Exception) {
                                            Log.e(
                                                "ListPanenTBSActivity",
                                                "Error fetching transporter",
                                                e
                                            )
                                            "Internal"
                                        }
                                    }
                                    tvTransporter.findViewById<TextView>(R.id.tvSubTitleEspb).text =
                                        transporterName

                                    // Set Mill
                                    tvMill.findViewById<TextView>(R.id.tvTitleEspb).text = "Mill"
                                    val millAbbr = withContext(Dispatchers.IO) {
                                        try {
                                            espbViewModel.getMillNameById(espb.mill_id) ?: "Unknown"
                                        } catch (e: Exception) {
                                            Log.e("ListPanenTBSActivity", "Error fetching mill", e)
                                            "Unknown"
                                        }
                                    }
                                    tvMill.findViewById<TextView>(R.id.tvSubTitleEspb).text =
                                        millAbbr
                                } catch (e: Exception) {
                                    Log.e("ListPanenTBSActivity", "Error in coroutine", e)
                                    Toasty.error(
                                        this@ListPanenTBSActivity,
                                        "Error loading details: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            // Set Driver
                            tvDriver.findViewById<TextView>(R.id.tvTitleEspb).text = "Driver"
                            tvDriver.findViewById<TextView>(R.id.tvSubTitleEspb).text = espb.driver

                            // Set Mekanisasi status
                            tvMekanisasi.findViewById<TextView>(R.id.tvTitleEspb).text =
                                "Mekanisasi"
                            val mekanisasiStatus =
                                if (espb.status_mekanisasi == 1) "Ya" else "Tidak"
                            tvMekanisasi.findViewById<TextView>(R.id.tvSubTitleEspb).text =
                                mekanisasiStatus

//                            // Set Draft status
//                            tvDraft.findViewById<TextView>(R.id.tvTitleEspb).text = "Status"
//                            val draftStatus = if (espb.status_draft == 1) "Draft" else "Final"
//                            tvDraft.findViewById<TextView>(R.id.tvSubTitleEspb).text = draftStatus

                            // Make the layout visible now that we've populated it
                            ll_detail_espb.visibility = View.VISIBLE

                        } catch (e: Exception) {
                            Toasty.error(
                                this@ListPanenTBSActivity,
                                "Error displaying eSPB details: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("ListPanenTBSActivity", "Error displaying eSPB details", e)
                        }
                    } else {
                        Toasty.error(
                            this@ListPanenTBSActivity,
                            "eSPB data not found",
                            Toast.LENGTH_LONG
                        ).show()
                        ll_detail_espb.visibility = View.GONE
                    }
                }
            } else {
                findViewById<SpeedDialView>(R.id.dial_tph_list).visibility = View.VISIBLE
                panenViewModel.loadTPHNonESPB(0, 0, 0, AppUtils.currentDate)
                panenViewModel.countTPHNonESPB(0, 0, 0, AppUtils.currentDate)
                panenViewModel.countTPHESPB(1, 0, 0, AppUtils.currentDate)
//                panenViewModel.loadPanenCountArchive()
            }

        }

        setupButtonGenerateQR()

        if (featureName == "Buat eSPB") {
            btnAddMoreTph = FloatingActionButton(this)
            btnAddMoreTph.id = View.generateViewId()
            btnAddMoreTph.setImageResource(R.drawable.baseline_add_24) // Make sure you have this resource, or use baseline_add_24
            btnAddMoreTph.contentDescription = "Add More TPH"

            // Set button background color to green
            btnAddMoreTph.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    this,
                    android.R.color.holo_green_dark
                )
            )

            // Set icon color to white
            btnAddMoreTph.imageTintList = ColorStateList.valueOf(Color.WHITE)

            // Add the button to the layout
            val rootLayout =
                findViewById<ConstraintLayout>(R.id.clParentListPanen) // Assuming your root layout is a ConstraintLayout
            val params = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            try {
                params.bottomToTop = R.id.btnGenerateQRTPH
            } catch (e: Exception) {
                params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                Toasty.error(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            // Convert dp to pixels for proper margin setting
            val scale = resources.displayMetrics.density
            val marginInPixels = (30 * scale + 0.5f).toInt()
            params.setMargins(
                0,
                0,
                marginInPixels,
                marginInPixels
            ) // Set right and bottom margins to 30dp
            rootLayout.addView(btnAddMoreTph, params)

            btnAddMoreTph.setOnClickListener {
                getAllDataFromList(false)
                val intent = Intent(this, ScanQR::class.java)
                intent.putExtra("tph_1", tph1)
                intent.putExtra("tph_0", tph0)
                intent.putExtra("tph_1_id_panen", tph1IdPanen)
                intent.putExtra("FEATURE_NAME", featureName)
                Log.d("ListPanenTBSActivityPassData", "List tph1: $tph1")
                Log.d("ListPanenTBSActivityPassData", "List tph0: $tph0")
                Log.d("ListPanenTBSActivityPassData", "List tph1IdPanen: $tph1IdPanen")
                Log.d("ListPanenTBSActivityPassData", "List FEATURE_NAME: $featureName")
                startActivity(intent)
                finishAffinity()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(
                    Intent(
                        this@ListPanenTBSActivity,
                        HomePageActivity::class.java
                    )
                )
                finishAffinity()
            }
        })
    }

    private fun setupCardListeners() {
        cardTersimpan.setOnClickListener {
            currentState = 0
            setActiveCard(cardTersimpan)
            loadingDialog.show()
            if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                val standardHeaders = listOf("BLOK", "NO TPH", "TOTAL JJG", "JAM")
                updateTableHeaders(standardHeaders)
            }

            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            listAdapter.updateArchiveState(currentState)
            val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
            headerCheckBox.visibility = View.VISIBLE
            val flCheckBoxTableHeaderLayout = findViewById<ConstraintLayout>(R.id.tableHeader)
                .findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
            flCheckBoxTableHeaderLayout.visibility = View.VISIBLE
            speedDial.visibility =
                if (listAdapter.getSelectedItems().isNotEmpty()) View.VISIBLE else View.GONE

            // Check if filterAllData is checked
            val isAllDataFiltered = filterAllData.isChecked
            val dateToUse = if (isAllDataFiltered) null else AppUtils.currentDate

            if (featureName == "Buat eSPB") {
                panenViewModel.loadActivePanenESPB()
            } else if (featureName == "Rekap panen dan restan") {
                loadingDialog.setMessage("Loading data tph...")
                if (isAllDataFiltered) {
                    panenViewModel.loadTPHNonESPB(0, 0, 1)
                    panenViewModel.countTPHNonESPB(0, 0, 1)
                    panenViewModel.countTPHESPB(0, 1, 1)
                } else {
                    panenViewModel.loadTPHNonESPB(0, 0, 1, dateToUse)
                    panenViewModel.countTPHNonESPB(0, 0, 1, dateToUse)
                    panenViewModel.countTPHESPB(0, 1, 1, dateToUse)
                }
            } else {
                loadingDialog.setMessage("Loading data tersimpan...")
                if (isAllDataFiltered) {
                    panenViewModel.loadTPHNonESPB(0, 0, 0)
                    panenViewModel.countTPHNonESPB(0, 0, 0)
                    panenViewModel.countTPHESPB(1, 0, 0)
                } else {
                    panenViewModel.loadTPHNonESPB(0, 0, 0, dateToUse)
                    panenViewModel.countTPHNonESPB(0, 0, 0, dateToUse)
                    panenViewModel.countTPHESPB(1, 0, 0, dateToUse)
                }
            }
        }

        cardTerscan.setOnClickListener {
            currentState = 1
            setActiveCard(cardTerscan)
            loadingDialog.show()

            if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                val standardHeaders = listOf("BLOK", "NO TPH", "TOTAL JJG", "JAM")
                updateTableHeaders(standardHeaders)
            }

            val isAllDataFiltered = filterAllData.isChecked
            val dateToUse = if (isAllDataFiltered) null else AppUtils.currentDate

            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            speedDial.visibility = View.GONE
            listAdapter.updateArchiveState(currentState)
            val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
            headerCheckBox.visibility = View.VISIBLE
            val flCheckBoxTableHeaderLayout = findViewById<ConstraintLayout>(R.id.tableHeader)
                .findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
            flCheckBoxTableHeaderLayout.visibility = View.VISIBLE

            if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
                loadingDialog.setMessage("Loading TPH menjadi E-SPB...")

                if (isAllDataFiltered) {
                    panenViewModel.loadTPHESPB(0, 1, 1)
                    panenViewModel.countTPHNonESPB(0, 0, 1)
                    panenViewModel.countTPHESPB(0, 1, 1)
                } else {
                    panenViewModel.loadTPHESPB(0, 1, 1, dateToUse)
                    panenViewModel.countTPHNonESPB(0, 0, 1, dateToUse)
                    panenViewModel.countTPHESPB(0, 1, 1, dateToUse)
                }
            } else {
                loadingDialog.setMessage("Loading data terscan...")
                if (isAllDataFiltered) {
                    panenViewModel.loadTPHESPB(1, 0, 0)
                    panenViewModel.countTPHESPB(1, 0, 0)
                    panenViewModel.countTPHNonESPB(0, 0, 0)
                } else {
                    panenViewModel.loadTPHESPB(1, 0, 0, dateToUse)
                    panenViewModel.countTPHESPB(1, 0, 0, dateToUse)
                    panenViewModel.countTPHNonESPB(0, 0, 0, dateToUse)
                }
            }
        }

        cardRekapPerPemanen.setOnClickListener {
            currentState = 2
            setActiveCard(cardRekapPerPemanen)
            if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) {
                val rekapHeaders =
                    listOf("NAMA\nPEMANEN", "BLOK/JJG", "JUMLAH\nTPH", "TOTAL JJG/\nJJG DIBAYAR")
                updateTableHeaders(rekapHeaders)
            }
            loadingDialog.show()

            val isAllDataFiltered = filterAllData.isChecked
            val dateToUse = if (isAllDataFiltered) null else AppUtils.currentDate

            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            speedDial.visibility = View.GONE
            listAdapter.updateArchiveState(currentState)
            val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
            headerCheckBox.visibility = View.GONE
            val flCheckBoxTableHeaderLayout = findViewById<ConstraintLayout>(R.id.tableHeader)
                .findViewById<FrameLayout>(R.id.flCheckBoxTableHeaderLayout)
            flCheckBoxTableHeaderLayout.visibility = View.GONE

            loadingDialog.setMessage("Loading Rekap Per Pemanen...")
            if (isAllDataFiltered) {
                panenViewModel.loadTPHNonESPB(1, 0, 0)
                panenViewModel.loadPanenCountArchive()
                panenViewModel.countTPHESPB(1, 0, 0)
                panenViewModel.countTPHNonESPB(0, 0, 0)
            } else {
                panenViewModel.loadTPHNonESPB(1, 0, 0, dateToUse)
                panenViewModel.loadPanenCountArchive()
                panenViewModel.countTPHESPB(1, 0, 0, dateToUse)
                panenViewModel.countTPHNonESPB(0, 0, 0, dateToUse)
            }
        }
    }

    private fun initializeViews() {
        cardTersimpan = findViewById(R.id.card_item_tersimpan)
        cardTerscan = findViewById(R.id.card_item_terscan)
        cardRekapPerPemanen = findViewById(R.id.card_rekap_per_pemanen)
        cardRekapPerPemanen.visibility =
            if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen) View.VISIBLE else View.GONE
        counterTersimpan = findViewById(R.id.counter_item_tersimpan)
        counterTerscan = findViewById(R.id.counter_item_terscan)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        recyclerView = findViewById(R.id.rvTableData) // Initialize RecyclerView
    }

    private fun setActiveCard(activeCard: MaterialCardView) {

        cardTersimpan.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
        }

        cardTerscan.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
        }

        cardRekapPerPemanen.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.graylightDarker)
        }

        // Set active card colors
        activeCard.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.bgSelectWorkerGreen))
            strokeColor = ContextCompat.getColor(context, R.color.strokeSelectWorkerGreen)
        }
    }

    private fun formatPanenDataJSONForQR(jsonData: String): String {
        return try {
            // Parse the JSON array from the provided string
            val jsonArray = JSONArray(jsonData)

            // Map the JSON into the format needed
            val mappedData = mutableListOf<Map<String, Any?>>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                mappedData.add(
                    mapOf(
                        "tph_id" to item.getLong("tph"),
                        "date_created" to item.getString("tanggal"),
                        "jjg_json" to item.getString("jjg_json")
                    )
                )
            }

            if (mappedData.isEmpty()) {
                throw IllegalArgumentException("Data TPH is empty.")
            }

            val dateIndexMap = mutableMapOf<String, Int>()
            val formattedData = buildString {
                mappedData.forEach { data ->
                    try {
                        val tphId = data["tph_id"]?.toString()
                            ?: throw IllegalArgumentException("Missing tph_id.")
                        val dateCreated = data["date_created"]?.toString()
                            ?: throw IllegalArgumentException("Missing date_created.")

                        val jjgJsonString = data["jjg_json"]?.toString()
                            ?: throw IllegalArgumentException("Missing jjg_json.")
                        val jjgJson = try {
                            JSONObject(jjgJsonString)
                        } catch (e: JSONException) {
                            throw IllegalArgumentException("Invalid JSON format in jjg_json: $jjgJsonString")
                        }

                        val key = "KP"  // Using "KP" as the key based on your data

                        val toValue = if (jjgJson.has(key)) {
                            jjgJson.getInt(key)
                        } else {
                            throw IllegalArgumentException("Missing '$key' key in jjg_json: $jjgJsonString")
                        }

                        // Extract date and time parts
                        val dateParts = dateCreated.split(" ")
                        if (dateParts.size != 2) {
                            throw IllegalArgumentException("Invalid date_created format: $dateCreated")
                        }

                        val date = dateParts[0]  // 2025-04-03
                        val time = dateParts[1]  // 07:53:02

                        // Use dateIndexMap.size as the index for new dates
                        append("$tphId,${dateIndexMap.getOrPut(date) { dateIndexMap.size }},${time},$toValue;")
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Error processing data entry: ${e.message}")
                    }
                }
            }

            val username = try {
                PrefManager(this).username.toString().split("@")[0].takeLast(3).uppercase()
            } catch (e: Exception) {
                Toasty.error(this, "Error mengambil username: ${e.message}", Toast.LENGTH_LONG)
                    .show()
                "NULL"
            }

            // Create the tgl object with date mappings
            val tglJson = JSONObject()
            dateIndexMap.forEach { (date, index) ->
                tglJson.put(index.toString(), date)
            }

            return JSONObject().apply {
                put("tph_0", formattedData)
                put("username", username)
                put("tgl", tglJson)
            }.toString()
        } catch (e: Exception) {
            AppLogger.e("formatPanenDataForQR Error: ${e.message}")
            throw e
        }
    }

    private fun formatPanenDataForQR(mappedData: List<Map<String, Any?>>): String {
        return try {
            if (mappedData.isEmpty()) {
                throw IllegalArgumentException("Data TPH is empty.")
            }

            val formattedData = buildString {
                mappedData.forEach { data ->
                    try {
                        val tphId = data["tph_id"]?.toString()
                            ?: throw IllegalArgumentException("Missing tph_id.")
                        val dateCreated = data["date_created"]?.toString()
                            ?: throw IllegalArgumentException("Missing date_created.")

                        val jjgJsonString = data["jjg_json"]?.toString()
                            ?: throw IllegalArgumentException("Missing jjg_json.")
                        val jjgJson = try {
                            JSONObject(jjgJsonString)
                        } catch (e: JSONException) {
                            throw IllegalArgumentException("Invalid JSON format in jjg_json: $jjgJsonString")
                        }

                        val key =
                            if (featureName == "Rekap panen dan restan" || featureName == "Detail eSPB") "KP" else "TO"

                        val toValue = if (jjgJson.has(key)) {
                            jjgJson.getInt(key)
                        } else {
                            throw IllegalArgumentException("Missing '$key' key in jjg_json: $jjgJsonString")
                        }

                        // Extract date and time parts
                        val dateParts = dateCreated.split(" ")
                        if (dateParts.size != 2) {
                            throw IllegalArgumentException("Invalid date_created format: $dateCreated")
                        }

                        val date = dateParts[0]  // 2025-03-28
                        val time = dateParts[1]  // 13:15:18

                        // Use dateIndexMap.size as the index for new dates
                        append("$tphId,${dateIndexMap.getOrPut(date) { dateIndexMap.size }},${time},$toValue;")
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Error processing data entry: ${e.message}")
                    }
                }
            }

            val username = try {
                PrefManager(this).username.toString().split("@")[0].takeLast(3).uppercase()
            } catch (e: Exception) {
                Toasty.error(this, "Error mengambil username: ${e.message}", Toast.LENGTH_LONG)
                    .show()
                "NULL"
            }

            // Create the tgl object with date mappings
            val tglJson = JSONObject()
            dateIndexMap.forEach { (date, index) ->
                tglJson.put(index.toString(), date)
            }

            return JSONObject().apply {
                put("tph_0", formattedData)
                put("username", username)
                put("tgl", tglJson)
            }.toString()
        } catch (e: Exception) {
            AppLogger.e("formatPanenDataForQR Error: ${e.message}")
            throw e
        }
    }


    fun convertToFormattedString(input: String, int: Int = 1): String {
        try {
            // Remove the outer brackets
            val content = input.trim().removeSurrounding("[", "]")

            // Split into individual objects
            val objects = content.split("}, {")

            return objects.joinToString(";") { objStr ->
                // Clean up the object string
                val cleanObj = objStr.trim()
                    .removePrefix("{")
                    .removeSuffix("}")

                // Split into key-value pairs
                val map = cleanObj.split(", ").associate { pair ->
                    val (key, value) = pair.split("=", limit = 2)
                    key to value
                }

                // Extract jjg_json value
                val jjgJson = map["jjg_json"]?.trim() ?: "{}"

                // Construct the formatted string
                "${map["tph_id"]},${map["date_created"]},${jjgJson},$int"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun convertToFormattedString(input: String, tphFilter: String): String {
        try {
            // Parse TPH filter string into a list of IDs
            val tphIds = tphFilter
                .trim()
                .removeSurrounding("{", "}")
                .substringAfter("\"tph\":\"")  // Get content after "tph":"
                .substringBefore("\"")         // Get content before the closing quote
                .split(";")
                .map { it.trim() }
                .toSet()

            Log.d("ListPanenTBSActivityESPB", "tphIds: $tphIds")

            // Remove the outer brackets
            val content = input.trim().removeSurrounding("[", "]")

            // Split into individual objects
            val objects = content.split("}, {")

            return objects
                .filter { objStr ->
                    // Extract tph_id from each object and check if it's in our filter list
                    val cleanObj = objStr.trim()
                        .removePrefix("{")
                        .removeSuffix("}")
                    val map = cleanObj.split(", ").associate { pair ->
                        val (key, value) = pair.split("=", limit = 2)
                        key to value
                    }
                    tphIds.contains(map["tph_id"])
                }
                .joinToString(";") { objStr ->
                    // Clean up the object string
                    val cleanObj = objStr.trim()
                        .removePrefix("{")
                        .removeSuffix("}")

                    // Split into key-value pairs
                    val map = cleanObj.split(", ").associate { pair ->
                        val (key, value) = pair.split("=", limit = 2)
                        key to value
                    }

                    // Extract jjg_json value
                    val jjgJson = map["jjg_json"]?.trim() ?: "{}"

                    // Construct the formatted string
                    "${map["tph_id"]},${map["date_created"]},${jjgJson},1"
                }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    data class Entry(
        val id: String,
        val timestamp: String,
        val value: Int,
        val type: Int
    ) {
        override fun toString(): String = "$id,$timestamp,$value,$type"
    }

    fun String.toEntries(): Set<Entry> {
        if (this.isEmpty()) return emptySet()
        return split(";").map { entry ->
            val parts = entry.split(",")
            Entry(
                id = parts[0],
                timestamp = parts[1],
                value = parts[2].toInt(),
                type = parts[3].toInt()
            )
        }.toSet()
    }

    fun Set<Entry>.toString(): String {
        return if (isEmpty()) "" else joinToString(";")
    }

private fun getAllDataFromList(playSound : Boolean =true) {
        //get manually selected items
        val selectedItems = listAdapter.getSelectedItems()
        Log.d("ListPanenTBSActivityESPB", "selectedItems: $selectedItems")
        val tph1AD0 =
            convertToFormattedString(selectedItems.toString(), 0).replace("{\"KP\": ", "")
                .replace("},", ",")
        Log.d("ListPanenTBSActivityESPB", "formatted selectedItemsAD: $tph1AD0")
        val tph1AD2 =
            convertToFormattedString(selectedItems.toString(), 1).replace("{\"KP\": ", "")
                .replace("},", ",")
        Log.d("ListPanenTBSActivityESPB", "formatted selectedItemsAD: $tph1AD2")

        //get automatically selected items
        val selectedItems2 = listAdapter.getSelectedItems()
        Log.d("ListPanenTBSActivityESPB", "selectedItems2:$selectedItems2")

        // Extract the id values from the matches and join them with commas
        val newTph1IdPanen = try {
            val pattern = Regex("\\{id=(\\d+),")
            val matches = pattern.findAll(selectedItems2.toString())
            matches.map { it.groupValues[1] }.joinToString(", ")

        } catch (e: Exception) {
            Toasty.error(this, "Error parsing panen IDs: ${e.message}", Toast.LENGTH_LONG).show()
            ""
        }

        // Combine with existing tph1IdPanen if it exists
        tph1IdPanen = if (tph1IdPanen.isEmpty()) {
            newTph1IdPanen
        } else {
            "$tph1IdPanen, $newTph1IdPanen"
        }
        Log.d("ListPanenTBSActivityESPB", "tph1IdPanen:$tph1IdPanen")


    //get automatically selected items
        val preSelectedItems = listAdapter.getPreSelectedItems()
        Log.d("ListPanenTBSActivityESPB", "preSelectedItems:$preSelectedItems")

        // Extract the id values from the matches and join them with commas
        val newTph1NoIdPanen = try {
            val pattern = Regex("\\{id=(\\d+),")
            val matches = pattern.findAll(preSelectedItems.toString())
            matches.map { it.groupValues[1] }.joinToString(", ")

        } catch (e: Exception) {
            Toasty.error(this, "Error parsing panen IDs: ${e.message}", Toast.LENGTH_LONG).show()
            ""
        }

        // Combine with existing tph1IdPanen if it exists
        tph1NoIdPanen = if (tph1NoIdPanen.isEmpty()) {
            newTph1NoIdPanen
        } else {
            "$tph1NoIdPanen, $newTph1NoIdPanen"
    }
    Log.d("ListPanenTBSActivityESPB", "tph1NoIdPanen:$tph1NoIdPanen")


    if (playSound) {
        playSound(R.raw.berhasil_scan)
    }

        val allItems = listAdapter.getCurrentData()
        Log.d("ListPanenTBSActivityESPB", "listTPHDriver: $listTPHDriver")
        val tph1NO = convertToFormattedString(
            selectedItems2.toString(),
            listTPHDriver
        ).replace("{\"KP\": ", "").replace("},", ",")
        Log.d("ListPanenTBSActivityESPB", "formatted selectedItemsNO: $tph1NO")

        //get item which is not selected
        val tph0before =
            convertToFormattedString(allItems.toString(), 0).replace("{\"KP\": ", "")
                .replace("},", ",")
        Log.d("ListPanenTBSActivityESPB", "formatted selectedItems0: $tph0before")

        val set1 = tph1AD0.toEntries()
        val set2 = tph1AD2.toEntries()
        val set3 = tph1NO.toEntries()
        val set4 = tph0before.toEntries()

        // Calculate string5 = string4 - string1 - string3
        val newTph0 = (set4 - set1 - set3).toString().replace("[", "").replace("]", "")
            .replace(", ", ";")
        Log.d("ListPanenTBSActivityESPB", "New tph0: $newTph0")

        // Calculate string6 = string2 + string3
        val newTph1 =
            (set2 + set3).toString().replace("[", "").replace("]", "").replace(", ", ";")
        Log.d("ListPanenTBSActivityESPB", "New tph1: $newTph1")

        // Combine with existing data if it exists
        if (tph0.isNotEmpty() && newTph0.isNotEmpty()) {
            tph0 = "$tph0;$newTph0"
        } else if (newTph0.isNotEmpty()) {
            tph0 = newTph0
        }

        if (tph1.isNotEmpty() && newTph1.isNotEmpty()) {
            tph1 = "$tph1;$newTph1"
        } else if (newTph1.isNotEmpty()) {
            tph1 = newTph1
        }

        // Remove any duplicate entries from tph0 and tph1
        tph0 = removeDuplicateEntries(tph0)
        tph1 = removeDuplicateEntries(tph1)

        Log.d("ListPanenTBSActivityESPB", "Final tph0: $tph0")
        Log.d("ListPanenTBSActivityESPB", "Final tph1: $tph1")
        Log.d("ListPanenTBSActivityESPB", "Final tph1IdPanen: $tph1IdPanen")


    }

    private fun setupButtonGenerateQR() {
        val btnGenerateQRTPH = findViewById<FloatingActionButton>(R.id.btnGenerateQRTPH)
        if (featureName == "Buat eSPB") {
            btnGenerateQRTPH.setImageResource(R.drawable.baseline_save_24)
            btnGenerateQRTPH.setOnClickListener {
                getAllDataFromList(false    )
                if (tph1.isEmpty() && tph1IdPanen.isEmpty()) {
                    // No selected items, show error message
                    AlertDialogUtility.withSingleAction(
                        this@ListPanenTBSActivity,
                        stringXML(R.string.al_back),
                        stringXML(R.string.al_have_check_data),
                        "${stringXML(R.string.al_must_have_check_data)}",
                        "warning.json",
                        R.color.colorRedDark
                    ) {
                    }
                }else{
                    AlertDialogUtility.withTwoActions(
                        this,
                        "LANJUT",
                        "PERHATIAN!",
                        "Apakah anda ingin membuat eSPB dengan data ini?",
                        "warning.json", function = {
                            val intent = Intent(this, FormESPBActivity::class.java)
                            intent.putExtra("tph_1", tph1)
                            intent.putExtra("tph_normal", tph1NoIdPanen)
                            intent.putExtra("tph_0", tph0)
                            intent.putExtra("tph_1_id_panen", tph1IdPanen)
                            intent.putExtra("FEATURE_NAME", featureName)
                            startActivity(intent)
                            finishAffinity()
                        }
                    ) {
                    }
                }

            }
        } else {
            btnGenerateQRTPH.setOnClickListener {
                AlertDialogUtility.withTwoActions(
                    this,
                    "Generate QR",
                    getString(R.string.confirmation_dialog_title),
                    getString(R.string.al_confirm_generate_qr),
                    "warning.json",
                    ContextCompat.getColor(this, R.color.bluedarklight),
                    function = {

                        val view =
                            layoutInflater.inflate(
                                R.layout.layout_bottom_sheet_generate_qr_panen,
                                null
                            )

                        val dialog = BottomSheetDialog(this@ListPanenTBSActivity)
                        dialog.setContentView(view)

                        // Get references to views
                        val loadingLogo: ImageView = view.findViewById(R.id.loading_logo)
//                        val qrCodeImageView: com.github.chrisbanes.photoview.PhotoView = view.findViewById(R.id.qrCodeImageView)
                        val qrCodeImageView: ImageView = view.findViewById(R.id.qrCodeImageView)

                        val tvTitleQRGenerate: TextView =
                            view.findViewById(R.id.textTitleQRGenerate)
                        tvTitleQRGenerate.setResponsiveTextSizeWithConstraints(23F, 22F, 25F)
                        val dashedLine: View = view.findViewById(R.id.dashedLine)
                        val loadingContainer: LinearLayout =
                            view.findViewById(R.id.loadingDotsContainerBottomSheet)

                        val titleQRConfirm: TextView = view.findViewById(R.id.titleAfterScanQR)
                        val descQRConfirm: TextView = view.findViewById(R.id.descAfterScanQR)
                        val confimationContainer: LinearLayout =
                            view.findViewById(R.id.confirmationContainer)
                        val scrollContent: NestedScrollView = view.findViewById(R.id.scrollContent)
                        scrollContent.post {
                            // Scroll to the middle to show QR code in center
                            // 300dp space + approximately half of QR view height (125dp)
                            scrollContent.smoothScrollTo(0, 600)
                        }

//                        // Configure for better zooming
//                        qrCodeImageView.apply {
//                            // Set minimum scale lower to allow for initial smaller size
//                            minimumScale = 0.5f  // This allows scaling down to 50%
//                            maximumScale = 5.0f  // Maximum zoom
//                            mediumScale = 2.5f   // Medium zoom
//
//                             scale = 0.8f  // This will work since it's above minimumScale
//
//                            // Enable zooming
//                            isZoomable = true
//                        }
                        val btnConfirmScanPanenTPH: MaterialButton =
                            view.findViewById(R.id.btnConfirmScanPanenTPH)

                        // Initially hide QR code and dashed line, show loading
                        qrCodeImageView.visibility = View.GONE
                        loadingLogo.visibility = View.VISIBLE
                        loadingContainer.visibility = View.VISIBLE

                        // Initial setup for text elements
                        titleQRConfirm.setResponsiveTextSizeWithConstraints(21F, 17F, 25F)
                        descQRConfirm.setResponsiveTextSizeWithConstraints(19F, 15F, 23F)

                        // Load and start bounce animation
                        val bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce)
                        loadingLogo.startAnimation(bounceAnimation)

                        // Setup dots animation
                        val dots = listOf(
                            loadingContainer.findViewById<View>(R.id.dot1),
                            loadingContainer.findViewById<View>(R.id.dot2),
                            loadingContainer.findViewById<View>(R.id.dot3),
                            loadingContainer.findViewById<View>(R.id.dot4)
                        )

                        dots.forEachIndexed { index, dot ->
                            val translateAnimation =
                                ObjectAnimator.ofFloat(dot, "translationY", 0f, -10f, 0f)
                            val scaleXAnimation =
                                ObjectAnimator.ofFloat(dot, "scaleX", 1f, 0.8f, 1f)
                            val scaleYAnimation =
                                ObjectAnimator.ofFloat(dot, "scaleY", 1f, 0.8f, 1f)

                            listOf(
                                translateAnimation,
                                scaleXAnimation,
                                scaleYAnimation
                            ).forEach { animation ->
                                animation.duration = 500
                                animation.repeatCount = ObjectAnimator.INFINITE
                                animation.repeatMode = ObjectAnimator.REVERSE
                                animation.startDelay = (index * 100).toLong()
                                animation.start()
                            }
                        }

                        val maxHeight = (resources.displayMetrics.heightPixels * 0.85).toInt()

                        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                            ?.let { bottomSheet ->
                                val behavior = BottomSheetBehavior.from(bottomSheet)

                                behavior.apply {
                                    this.peekHeight =
                                        maxHeight  // Set the initial height when peeking
                                    this.state =
                                        BottomSheetBehavior.STATE_EXPANDED  // Start fully expanded
                                    this.isFitToContents =
                                        true  // Content will determine the height (up to maxHeight)
                                    this.isDraggable =
                                        false  // Prevent user from dragging the sheet
                                }

                                // Set a fixed height for the bottom sheet
                                bottomSheet.layoutParams?.height = maxHeight
                            }

                        dialog.show()

                        if (featureName != AppUtils.ListFeatureNames.RekapPanenDanRestan){
                            btnConfirmScanPanenTPH.setOnClickListener {
                                AlertDialogUtility.withTwoActions(
                                    this@ListPanenTBSActivity,
                                    getString(R.string.al_yes),
                                    getString(R.string.confirmation_dialog_title),
                                    "${getString(R.string.al_make_sure_scanned_qr)}",
                                    "warning.json",
                                    ContextCompat.getColor(
                                        this@ListPanenTBSActivity,
                                        R.color.bluedarklight
                                    ),
                                    function = {
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            try {
                                                withContext(Dispatchers.Main) {
                                                    loadingDialog.show()
                                                }

                                                // Validate data first
                                                if (mappedData.isEmpty()) {
                                                    throw Exception("No data to archive")
                                                }

                                                var hasError = false
                                                var successCount = 0
                                                val errorMessages = mutableListOf<String>()

                                                mappedData.forEach { item ->
                                                    try {
                                                        // Null check for item
                                                        if (item == null) {
                                                            errorMessages.add("Found null item in data")
                                                            hasError = true
                                                            return@forEach
                                                        }

                                                        // ID validation
                                                        val id = when (val idValue = item["id"]) {
                                                            null -> {
                                                                errorMessages.add("ID is null")
                                                                hasError = true
                                                                return@forEach
                                                            }

                                                            !is Number -> {
                                                                errorMessages.add("Invalid ID format: $idValue")
                                                                hasError = true
                                                                return@forEach
                                                            }

                                                            else -> idValue.toInt()
                                                        }

                                                        if (id <= 0) {
                                                            errorMessages.add("Invalid ID value: $id")
                                                            hasError = true
                                                            return@forEach
                                                        }

                                                        try {
                                                            panenViewModel.archivePanenById(id)
                                                            successCount++
                                                        } catch (e: SQLiteException) {
                                                            errorMessages.add("Database error for ID $id: ${e.message}")
                                                            hasError = true
                                                        } catch (e: Exception) {
                                                            errorMessages.add("Error archiving ID $id: ${e.message}")
                                                            hasError = true
                                                        }

                                                    } catch (e: Exception) {
                                                        errorMessages.add("Unexpected error processing item: ${e.message}")
                                                        hasError = true
                                                    }
                                                }

                                                // Show results
                                                withContext(Dispatchers.Main) {
                                                    try {
                                                        loadingDialog.dismiss()

                                                        when {
                                                            successCount == 0 -> {
                                                                val errorDetail =
                                                                    errorMessages.joinToString("\n")
                                                                AppLogger.e("Archive failed. Errors:\n$errorDetail")
                                                                Toast.makeText(
                                                                    this@ListPanenTBSActivity,
                                                                    "Gagal mengarsipkan data",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }

                                                            hasError -> {
                                                                val errorDetail =
                                                                    errorMessages.joinToString("\n")
                                                                AppLogger.e("Partial success. Errors:\n$errorDetail")
                                                                Toast.makeText(
                                                                    this@ListPanenTBSActivity,
                                                                    "Beberapa data berhasil diarsipkan ($successCount/${mappedData.size})",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }

                                                            else -> {
                                                                AppLogger.d("All items archived successfully")
                                                                playSound(R.raw.berhasil_konfirmasi)
                                                                Toast.makeText(
                                                                    this@ListPanenTBSActivity,
                                                                    "Semua data berhasil diarsipkan",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                        dialog.dismiss()
                                                    } catch (e: Exception) {
                                                        AppLogger.e("Error in UI update: ${e.message}")
                                                        Toast.makeText(
                                                            this@ListPanenTBSActivity,
                                                            "Terjadi kesalahan pada UI",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }

                                            } catch (e: Exception) {
                                                AppLogger.e("Fatal error in archiving process: ${e.message}")
                                                withContext(Dispatchers.Main) {
                                                    try {
                                                        loadingDialog.dismiss()
                                                        Toast.makeText(
                                                            this@ListPanenTBSActivity,
                                                            "Terjadi kesalahan saat mengarsipkan data: ${e.message}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        dialog.dismiss()
                                                    } catch (dialogException: Exception) {
                                                        AppLogger.e("Error dismissing dialogs: ${dialogException.message}")
                                                    }
                                                }
                                            }

                                            panenViewModel.loadTPHNonESPB(0, 0, 0, globalFormattedDate)
                                            panenViewModel.countTPHNonESPB(0, 0, 0, globalFormattedDate)
                                            panenViewModel.countTPHESPB(1, 0, 0, globalFormattedDate)
                                        }
                                    }
                                ) {

                                }
                            }
                        }else{
                            btnConfirmScanPanenTPH.setOnClickListener {
                                onBackPressed()
                            }
                        }


                        lifecycleScope.launch {
                            try {
                                delay(1000)

                                val jsonData = withContext(Dispatchers.IO) {
                                    try {
                                        if (featureName == "Detail eSPB") {

                                            val gson = Gson()
                                            val espbObject = JsonObject().apply {
                                                addProperty("blok_jjg", blok_jjg)
                                                addProperty("nopol", nopol)
                                                addProperty("driver", driver)
                                                addProperty("pemuat_id", pemuat_id)
                                                addProperty("kemandoran_id", kemandoran_id)
                                                addProperty("pemuat_nik", pemuat_nik)
                                                addProperty("transporter_id", transporter_id)
                                                addProperty("mill_id", mill_id)
                                                addProperty("created_by_id", created_by_id)
                                                addProperty("creator_info", creatorInfo)
                                                addProperty("no_espb", no_espb)
                                                addProperty("created_at", dateTime)
                                            }

                                            val rootObject = JsonObject().apply {
                                                add("espb", espbObject)
                                                addProperty("tph_0", tph0)
                                                addProperty("tph_1", tph1)
                                            }

                                            gson.toJson(rootObject)
                                        } else {
                                            formatPanenDataForQR(mappedData)
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("Error generating JSON data: ${e.message}")
                                        throw e
                                    }
                                }

                                AppLogger.d("jsonData $jsonData")

                                val encodedData = withContext(Dispatchers.IO) {
                                    try {
                                        encodeJsonToBase64ZipQR(jsonData)
                                            ?: throw Exception("Encoding failed")
                                    } catch (e: Exception) {
                                        AppLogger.e("Error encoding data: ${e.message}")
                                        throw e
                                    }
                                }

                                // Switch to the main thread for UI updates
                                withContext(Dispatchers.Main) {
                                    try {
                                        generateHighQualityQRCode(encodedData, qrCodeImageView)
                                        val fadeOut =
                                            ObjectAnimator.ofFloat(loadingLogo, "alpha", 1f, 0f)
                                                .apply {
                                                    duration = 250
                                                }
                                        val fadeOutDots =
                                            ObjectAnimator.ofFloat(
                                                loadingContainer,
                                                "alpha",
                                                1f,
                                                0f
                                            )
                                                .apply {
                                                    duration = 250
                                                }

                                        // Ensure QR code and other elements start invisible
                                        qrCodeImageView.alpha = 0f
                                        dashedLine.alpha = 0f
                                        tvTitleQRGenerate.alpha = 0f
                                        titleQRConfirm.alpha = 0f
                                        confimationContainer.alpha = 0f
                                        descQRConfirm.alpha = 0f
                                        btnConfirmScanPanenTPH.alpha = 0f


                                        // Create fade-in animations
                                        val fadeInQR =
                                            ObjectAnimator.ofFloat(qrCodeImageView, "alpha", 0f, 1f)
                                                .apply {
                                                    duration = 250
                                                    startDelay = 150
                                                }
                                        val fadeInDashedLine =
                                            ObjectAnimator.ofFloat(dashedLine, "alpha", 0f, 1f)
                                                .apply {
                                                    duration = 250
                                                    startDelay = 150
                                                }
                                        val fadeInTitle =
                                            ObjectAnimator.ofFloat(
                                                tvTitleQRGenerate,
                                                "alpha",
                                                0f,
                                                1f
                                            )
                                                .apply {
                                                    duration = 250
                                                    startDelay = 150
                                                }
                                        val fadeInTitleConfirm =
                                            ObjectAnimator.ofFloat(titleQRConfirm, "alpha", 0f, 1f)
                                                .apply {
                                                    duration = 250
                                                    startDelay = 150
                                                }


                                        val fadeInDescConfirm =
                                            ObjectAnimator.ofFloat(descQRConfirm, "alpha", 0f, 1f)
                                                .apply {
                                                    duration = 250
                                                    startDelay = 150
                                                }

                                        val fadeInConfirmationContainer = ObjectAnimator.ofFloat(
                                            confimationContainer,
                                            "alpha",
                                            0f,
                                            1f
                                        ).apply {
                                            duration = 250
                                            startDelay = 150
                                        }
                                        val fadeInButton =
                                            ObjectAnimator.ofFloat(
                                                btnConfirmScanPanenTPH,
                                                "alpha",
                                                0f,
                                                1f
                                            )
                                                .apply {
                                                    duration = 250
                                                    startDelay = 150
                                                }


                                        // Run animations sequentially
                                        AnimatorSet().apply {
                                            playTogether(fadeOut, fadeOutDots)
                                            addListener(object : AnimatorListenerAdapter() {
                                                override fun onAnimationEnd(animation: Animator) {
                                                    // Hide loading elements
                                                    loadingLogo.visibility = View.GONE
                                                    loadingContainer.visibility = View.GONE

                                                    // Show elements
                                                    confimationContainer.visibility = View.VISIBLE
                                                    tvTitleQRGenerate.visibility = View.VISIBLE
                                                    qrCodeImageView.visibility = View.VISIBLE
                                                    dashedLine.visibility = View.VISIBLE

                                                    btnConfirmScanPanenTPH.visibility = View.VISIBLE

                                                    lifecycleScope.launch {
                                                        delay(200)
playSound(R.raw.berhasil_generate_qr)
                                                    }


                                                    // Start fade-in animations
                                                    fadeInQR.start()
                                                    fadeInDashedLine.start()
                                                    fadeInTitle.start()
                                                    fadeInTitleConfirm.start()
                                                    fadeInConfirmationContainer.start()
                                                    fadeInDescConfirm.start()
                                                    fadeInButton.start()

                                                }
                                            })
                                            start()
                                        }
                                    } catch (e: Exception) {
                                        // Handle UI-related errors on the main thread
                                        loadingLogo.animation?.cancel()
                                        loadingLogo.clearAnimation()
                                        loadingLogo.visibility = View.GONE
                                        loadingContainer.visibility = View.GONE
                                        AppLogger.e("QR Generation UI Error: ${e.message}")
                                        showErrorMessageGenerateQR(
                                            view,
                                            "Error generating QR code: ${e.message}"
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                // Handle any other errors
                                withContext(Dispatchers.Main) {
                                    AppLogger.e("Error in QR process: ${e.message}")
                                    stopLoadingAnimation(loadingLogo, loadingContainer)
                                    showErrorMessageGenerateQR(
                                        view,
                                        "Error processing QR code: ${e.message}"
                                    )
                                }
                            }
                        }
                    },
                    cancelFunction = {

                    }
                )

            }


        }
    }

    // Helper function to stop the loading animation and hide UI
    private fun stopLoadingAnimation(
        loadingLogo: ImageView,
        loadingContainer: LinearLayout
    ) {
        loadingLogo.animation?.cancel()
        loadingLogo.clearAnimation()
        loadingLogo.visibility = View.GONE
        loadingContainer.visibility = View.GONE
    }


    // Helper function to show errors
    fun showErrorMessageGenerateQR(view: View, message: String) {
        val errorCard = view.findViewById<MaterialCardView>(R.id.errorCard)
        val errorText = view.findViewById<TextView>(R.id.errorText)
        errorText.text = message
        errorCard.visibility = View.VISIBLE
    }


    fun encodeJsonToBase64ZipQR(jsonData: String): String? {
        return try {
            if (jsonData.isBlank()) throw IllegalArgumentException("JSON data is empty")

            // Minify JSON first
            val minifiedJson = JSONObject(jsonData).toString()

            // Reject empty JSON
            if (minifiedJson == "{}") {
                AppLogger.e("Empty JSON detected, returning null")
                throw IllegalArgumentException("Empty JSON detected")
            }

            // Create a byte array output stream to hold the zip data
            ByteArrayOutputStream().use { byteArrayOutputStream ->
                ZipOutputStream(byteArrayOutputStream).apply {
                    setLevel(Deflater.BEST_COMPRESSION)
                }.use { zipOutputStream ->
                    val entry = ZipEntry("output.json")
                    zipOutputStream.putNextEntry(entry)
                    zipOutputStream.write(minifiedJson.toByteArray(StandardCharsets.UTF_8))
                    zipOutputStream.closeEntry()
                }

                val zipBytes = byteArrayOutputStream.toByteArray()
                val base64Encoded = Base64.encodeToString(zipBytes, Base64.NO_WRAP)

                val midPoint = base64Encoded.length / 2
                val firstHalf = base64Encoded.substring(0, midPoint)
                val secondHalf = base64Encoded.substring(midPoint)

                firstHalf + "5nqHzPKdlILxS9ABpClq" + secondHalf
            }
        } catch (e: JSONException) {
            AppLogger.e("JSON Processing Error: ${e.message}")
            throw IllegalArgumentException(e.message.toString())
        } catch (e: IOException) {
            AppLogger.e("IO Error: ${e.message}")
            throw IllegalArgumentException("${e.message}")
        } catch (e: Exception) {
            AppLogger.e("Encoding Error: ${e.message}")
            throw IllegalArgumentException("${e.message}")
        }
    }


    private fun setupObservers() {
        val listBlok = findViewById<TextView>(R.id.listBlok)
        val totalJjg = findViewById<TextView>(R.id.totalJjg)
        val totalTPH = findViewById<TextView>(R.id.totalTPH)
        val blokSection = findViewById<LinearLayout>(R.id.blok_section)
        val totalSection = findViewById<LinearLayout>(R.id.total_section)
        val btnGenerateQRTPH = findViewById<FloatingActionButton>(R.id.btnGenerateQRTPH)

        blokSection.visibility = View.GONE
        totalSection.visibility = View.GONE

        loadingDialog.show()
        loadingDialog.setMessage("Loading data...")

        panenViewModel.panenCountActive.observe(this) { count ->
            counterTersimpan.text = count.toString()
        }
        panenViewModel.panenCountArchived.observe(this) { count ->
            counterTerscan.text = count.toString()
        }


        panenViewModel.activePanenList.observe(this) { panenList ->
            if (currentState == 0 || currentState == 2) {
                listAdapter.updateData(emptyList())
                Handler(Looper.getMainLooper()).postDelayed({
                    loadingDialog.dismiss()

                    lifecycleScope.launch {

                        if (panenList.isNotEmpty()) {
                            tvEmptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                            val allWorkerData = mutableListOf<Map<String, Any>>()


                            panenList.map { panenWithRelations ->

                                if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen && currentState == 2) {
                                    // Parse karyawan IDs, kemandoran IDs, and NIKs
                                    val karyawanIds =
                                        panenWithRelations.panen.karyawan_id.toString().split(",")
                                            .map { it.trim() }
                                            .filter { it.isNotEmpty() }

                                    val kemandoranIds =
                                        panenWithRelations.panen.kemandoran_id?.toString()
                                            ?.split(",")
                                            ?.map { it.trim() }
                                            ?.filter { it.isNotEmpty() } ?: listOf()

                                    val karyawanNiks =
                                        panenWithRelations.panen.karyawan_nik?.toString()
                                            ?.split(",")
                                            ?.map { it.trim() }
                                            ?.filter { it.isNotEmpty() } ?: listOf()

                                    val jjgJsonStr =
                                        panenWithRelations.panen.jjg_json as? String
                                            ?: "{}" // Ensure it's a valid JSON string
                                    val jjgJson = JSONObject(jjgJsonStr) // Convert to JSONObject

                                    // Calculate total jjg count
                                    val totalTO = jjgJson["TO"].toString().toInt()
                                    val totalUN = jjgJson["UN"].toString().toInt()
                                    val totalOV = jjgJson["OV"].toString().toInt()
                                    val totalEM = jjgJson["EM"].toString().toInt()
                                    val totalAB = jjgJson["AB"].toString().toInt()
                                    val totalRA = jjgJson["RA"].toString().toInt()
                                    val totalLO = jjgJson["LO"].toString().toInt()
                                    val totalTI = jjgJson["TI"].toString().toInt()
                                    val totalRI = jjgJson["RI"].toString().toInt()
                                    val totalKP = jjgJson["KP"].toString().toInt()
                                    val totalPA = jjgJson["PA"].toString().toInt()

                                    // Determine how many workers we need to distribute the jjg to
                                    val workerCount = maxOf(karyawanIds.size, kemandoranIds.size, 1)

                                    // If there are multiple workers, create separate entries for each
                                    val multiWorkerData = mutableListOf<Map<String, Any>>()

                                    for (i in 0 until workerCount) {
                                        // Get the corresponding IDs for this worker
                                        val karyawanId =
                                            if (i < karyawanIds.size) karyawanIds[i] else ""
                                        val kemandoranId =
                                            if (i < kemandoranIds.size) kemandoranIds[i] else ""
                                        val karyawanNik =
                                            if (i < karyawanNiks.size) karyawanNiks[i] else ""

                                        // Divide the jjg counts equally among workers as a decimal value
                                        // Use toDouble() to ensure decimal division
                                        val workerTO = totalTO.toDouble() / workerCount
                                        val workerUN = totalUN.toDouble() / workerCount
                                        val workerOV = totalOV.toDouble() / workerCount
                                        val workerEM = totalEM.toDouble() / workerCount
                                        val workerAB = totalAB.toDouble() / workerCount
                                        val workerRA = totalRA.toDouble() / workerCount
                                        val workerLO = totalLO.toDouble() / workerCount
                                        val workerTI = totalTI.toDouble() / workerCount
                                        val workerRI = totalRI.toDouble() / workerCount
                                        val workerKP = totalKP.toDouble() / workerCount
                                        val workerPA = totalPA.toDouble() / workerCount

                                        // Create worker-specific jjg_json
                                        val workerJjgJson = JsonObject().apply {
                                            addProperty("TO", workerTO)
                                            addProperty("UN", workerUN)
                                            addProperty("OV", workerOV)
                                            addProperty("EM", workerEM)
                                            addProperty("AB", workerAB)
                                            addProperty("RA", workerRA)
                                            addProperty("LO", workerLO)
                                            addProperty("TI", workerTI)
                                            addProperty("RI", workerRI)
                                            addProperty("KP", workerKP)
                                            addProperty("PA", workerPA)
                                        }

                                        // Fetch karyawan name for this specific worker
                                        val singlePemuatData = withContext(Dispatchers.IO) {
                                            try {
                                                panenViewModel.getPemuatByIdList(listOf(karyawanId))
                                            } catch (e: Exception) {
                                                AppLogger.e("Error fetching Single Pemuat Data: ${e.message}")
                                                null
                                            }
                                        }

                                        val workerName = singlePemuatData?.firstOrNull()?.nama ?: "-"
                                        val singleKaryawanNama = if (workerName != "-" && karyawanNik.isNotEmpty()) {
                                            "$workerName - $karyawanNik"
                                        } else {
                                            workerName
                                        }

                                        // Fetch kemandoran name for this specific worker
                                        val singleKemandoranData = withContext(Dispatchers.IO) {
                                            try {
                                                panenViewModel.getKemandoranById(listOf(kemandoranId))
                                            } catch (e: Exception) {
                                                AppLogger.e("Error fetching Single Kemandoran Data: ${e.message}")
                                                null
                                            }
                                        }

                                        val singleKemandoranNama =
                                            singleKemandoranData?.firstOrNull()?.nama?.let { "$it" }
                                                ?: "-"

                                        // Create the map for this worker
                                        val workerData = mapOf<String, Any>(
                                            "id" to (panenWithRelations.panen.id as Any),
                                            "tph_id" to (panenWithRelations.panen.tph_id as Any),
                                            "date_created" to (panenWithRelations.panen.date_created as Any),
                                            "blok_name" to (panenWithRelations.tph?.blok_kode
                                                ?: "Unknown"),
                                            "nomor" to (panenWithRelations.tph!!.nomor as Any),
                                            "created_by" to (panenWithRelations.panen.created_by as Any),
                                            "karyawan_id" to (karyawanId as Any),
                                            "kemandoran_id" to (kemandoranId as Any),
                                            "karyawan_nik" to (karyawanNik as Any),
                                            "jjg_json" to (workerJjgJson.toString() as Any),
                                            "foto" to (panenWithRelations.panen.foto as Any),
                                            "komentar" to (panenWithRelations.panen.komentar as Any),
                                            "asistensi" to (panenWithRelations.panen.asistensi as Any),
                                            "lat" to (panenWithRelations.panen.lat as Any),
                                            "lon" to (panenWithRelations.panen.lon as Any),
                                            "jenis_panen" to (panenWithRelations.panen.jenis_panen as Any),
                                            "ancak" to (panenWithRelations.panen.ancak as Any),
                                            "archive" to (panenWithRelations.panen.archive as Any),
                                            "nama_estate" to (panenWithRelations.tph.dept_abbr as Any),
                                            "nama_afdeling" to (panenWithRelations.tph.divisi_abbr as Any),
                                            "blok_banjir" to (panenWithRelations.panen.status_banjir as Any),
                                            "tahun_tanam" to (panenWithRelations.tph.tahun as Any),
                                            "nama_karyawans" to (singleKaryawanNama as Any),
                                            "nama_kemandorans" to (singleKemandoranNama as Any),
                                            "username" to (panenWithRelations.panen.username as Any)
                                        )

                                        multiWorkerData.add(workerData)
                                    }

                                    allWorkerData.addAll(multiWorkerData)

                                    emptyList<Map<String, Any>>()
                                } else {
                                    val pemuatList = panenWithRelations.panen.karyawan_id.split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }

                                    val pemuatData: List<KaryawanModel>? =
                                        withContext(Dispatchers.IO) {
                                            try {
                                                panenViewModel.getPemuatByIdList(pemuatList)
                                            } catch (e: Exception) {
                                                AppLogger.e("Error fetching Pemuat Data: ${e.message}")
                                                null
                                            }
                                        }

                                    val rawKemandoran: List<String> = pemuatData
                                        ?.mapNotNull { it.kemandoran_id?.toString() }
                                        ?.distinct() ?: emptyList()

                                    val kemandoranData: List<KemandoranModel>? =
                                        withContext(Dispatchers.IO) {
                                            try {
                                                panenViewModel.getKemandoranById(rawKemandoran)
                                            } catch (e: Exception) {
                                                AppLogger.e("Error fetching Kemandoran Data: ${e.message}")
                                                null
                                            }
                                        }

                                    val kemandoranNamas = kemandoranData?.mapNotNull { it.nama }
                                        ?.takeIf { it.isNotEmpty() }
                                        ?.joinToString("\n") { "• $it" } ?: "-"

                                    val karyawanNamas = pemuatData?.mapNotNull { karyawan ->
                                        karyawan.nama?.let { nama ->
                                            // Always append NIK for every worker
                                            "$nama - ${karyawan.nik ?: "N/A"}"
                                        }
                                    }?.takeIf { it.isNotEmpty() }
                                        ?.joinToString(", ") ?: "-"


                                    val standardData = mapOf<String, Any>(
                                        "id" to (panenWithRelations.panen.id as Any),
                                        "tph_id" to (panenWithRelations.panen.tph_id as Any),
                                        "date_created" to (panenWithRelations.panen.date_created as Any),
                                        "blok_name" to (panenWithRelations.tph?.blok_kode
                                            ?: "Unknown"),
                                        "nomor" to (panenWithRelations.tph!!.nomor as Any),
                                        "created_by" to (panenWithRelations.panen.created_by as Any),
                                        "jjg_json" to (panenWithRelations.panen.jjg_json as Any),
                                        "foto" to (panenWithRelations.panen.foto as Any),
                                        "komentar" to (panenWithRelations.panen.komentar as Any),
                                        "asistensi" to (panenWithRelations.panen.asistensi as Any),
                                        "lat" to (panenWithRelations.panen.lat as Any),
                                        "lon" to (panenWithRelations.panen.lon as Any),
                                        "jenis_panen" to (panenWithRelations.panen.jenis_panen as Any),
                                        "ancak" to (panenWithRelations.panen.ancak as Any),
                                        "archive" to (panenWithRelations.panen.archive as Any),
                                        "nama_estate" to (panenWithRelations.tph.dept_abbr as Any),
                                        "nama_afdeling" to (panenWithRelations.tph.divisi_abbr as Any),
                                        "blok_banjir" to (panenWithRelations.panen.status_banjir as Any),
                                        "tahun_tanam" to (panenWithRelations.tph.tahun as Any),
                                        "nama_karyawans" to karyawanNamas as Any,
                                        "nama_kemandorans" to kemandoranNamas as Any,
                                        "username" to (panenWithRelations.panen.username as Any)
                                    )

                                    // Add this standard data to our global collection too
                                    allWorkerData.add(standardData)

                                    // Return this data for the map operation
                                    listOf(standardData)
                                }
                            }.flatten() // Flatten the list of lists into a single list

                            if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen && currentState == 2) {
                                val globalMergedWorkerMap = mutableMapOf<String, MutableMap<String, Any>>()

                                // Define all JJG types to handle
                                val jjgTypes = listOf("TO", "UN", "OV", "EM", "AB", "RA", "LO", "TI", "RI", "KP", "PA")

                                for (workerData in allWorkerData) {
                                    val workerName = workerData["nama_karyawans"].toString()
                                    AppLogger.d("Global processing: $workerName")

                                    val blokName = workerData["blok_name"].toString()
                                    val tphId = workerData["tph_id"].toString()
                                    val jjgJson = JSONObject(workerData["jjg_json"].toString())

                                    // Extract all JJG values
                                    val jjgValues = jjgTypes.associateWith { type ->
                                        jjgJson.optDouble(type, 0.0)
                                    }

                                    if (globalMergedWorkerMap.containsKey(workerName)) {
                                        AppLogger.d("Found duplicate worker globally: $workerName")
                                        val existingWorkerData = globalMergedWorkerMap[workerName]!!

                                        val existingJjgJson = JSONObject(existingWorkerData["jjg_json"].toString())

                                        // Update all JJG types in the existing JSON
                                        for (type in jjgTypes) {
                                            val existingValue = existingJjgJson.optDouble(type, 0.0)
                                            val newValue = jjgValues[type] ?: 0.0
                                            val totalValue = existingValue + newValue
                                            existingJjgJson.put(type, totalValue)
                                        }

                                        // Update the JJG JSON in the existing worker data
                                        existingWorkerData["jjg_json"] = existingJjgJson.toString()

                                        // Use PA for jjg_dibayar as in the original code
                                        val existingJjgDibayar = existingWorkerData["jjg_dibayar"]?.toString()?.toDoubleOrNull() ?: jjgValues["PA"] ?: 0.0
                                        val newJjgDibayar = existingJjgDibayar + (jjgValues["PA"] ?: 0.0)
                                        existingWorkerData["jjg_dibayar"] = if (newJjgDibayar == newJjgDibayar.toInt().toDouble()) {
                                            newJjgDibayar.toInt().toString()
                                        } else {
                                            String.format("%.1f", newJjgDibayar)
                                        }

                                        // Use TO for jjg_total_blok as in the original code
                                        val newTotalTO = existingJjgJson.optDouble("TO", 0.0)
                                        existingWorkerData["jjg_total_blok"] = if (newTotalTO == newTotalTO.toInt().toDouble()) {
                                            newTotalTO.toInt().toString()
                                        } else {
                                            String.format("%.1f", newTotalTO)
                                        }

                                        // Update occurrence counter
                                        val existingOccurrences = (existingWorkerData["occurrence_count"]?.toString()?.toIntOrNull() ?: 1) + 1
                                        existingWorkerData["occurrence_count"] = existingOccurrences.toString()

                                        // Update TPH ID tracking
                                        val tphIds = (existingWorkerData["tph_ids"]?.toString() ?: "").split(",")
                                            .filter { it.isNotEmpty() }.toMutableSet()
                                        tphIds.add(tphId)
                                        existingWorkerData["tph_ids"] = tphIds.joinToString(",")
                                        existingWorkerData["tph_count"] = tphIds.size.toString()

                                        // Update the jjg_each_blok field
                                        val existingJjgEachBlok = existingWorkerData["jjg_each_blok"]?.toString() ?: ""

                                        // Check if this blok already exists in our tracking (accounting for newlines)
                                        if (existingJjgEachBlok.split("\n").any { it.startsWith("$blokName(") }) {
                                            // Blok already exists, find and update its count
                                            val lines = existingJjgEachBlok.split("\n")
                                            val updatedLines = lines.map { line ->
                                                if (line.startsWith("$blokName(")) {
                                                    val regex = "$blokName\\(([0-9.]+)\\)".toRegex()
                                                    val matchResult = regex.find(line)
                                                    if (matchResult != null) {
                                                        val currentCount = matchResult.groupValues[1].toDouble()
                                                        val newCount = currentCount + (jjgValues["TO"] ?: 0.0)

                                                        // Format based on whether it's a whole number
                                                        val formattedCount = if (newCount == newCount.toInt().toDouble()) {
                                                            newCount.toInt().toString()
                                                        } else {
                                                            String.format("%.1f", newCount)
                                                        }

                                                        "$blokName($formattedCount)"
                                                    } else {
                                                        line
                                                    }
                                                } else {
                                                    line
                                                }
                                            }
                                            existingWorkerData["jjg_each_blok"] = updatedLines.joinToString("\n")

                                            // Update the bullet point version after updating jjg_each_blok
                                            val updatedBulletFormat = updatedLines.map { line ->
                                                val regex = "([A-Z0-9-]+)\\(([0-9.]+)\\)".toRegex()
                                                val matchResult = regex.find(line)

                                                if (matchResult != null) {
                                                    val blokNameMatch = matchResult.groupValues[1]
                                                    val count = matchResult.groupValues[2]
                                                    "• $blokNameMatch ($count Jjg)"
                                                } else {
                                                    "• $line"
                                                }
                                            }.joinToString("\n")

                                            existingWorkerData["jjg_each_blok_bullet"] = updatedBulletFormat

                                        } else {
                                            // This is a new blok for this worker
                                            val jjgTO = jjgValues["TO"] ?: 0.0
                                            val formattedJjgTO = if (jjgTO == jjgTO.toInt().toDouble()) {
                                                jjgTO.toInt().toString()
                                            } else {
                                                String.format("%.1f", jjgTO)
                                            }

                                            val updatedJjgEachBlok = if (existingJjgEachBlok.isEmpty()) {
                                                "$blokName($formattedJjgTO)"
                                            } else {
                                                "$existingJjgEachBlok\n$blokName($formattedJjgTO)"
                                            }

                                            existingWorkerData["jjg_each_blok"] = updatedJjgEachBlok

                                            // Update the bullet point version after adding new blok
                                            val updatedJjgEachBlokLines = updatedJjgEachBlok.split("\n")
                                            val updatedBulletFormat = updatedJjgEachBlokLines.map { line ->
                                                val regex = "([A-Z0-9-]+)\\(([0-9.]+)\\)".toRegex()
                                                val matchResult = regex.find(line)

                                                if (matchResult != null) {
                                                    val blokNameMatch = matchResult.groupValues[1]
                                                    val count = matchResult.groupValues[2]
                                                    "• $blokNameMatch ($count Jjg)"
                                                } else {
                                                    "• $line"
                                                }
                                            }.joinToString("\n")

                                            existingWorkerData["jjg_each_blok_bullet"] = updatedBulletFormat
                                        }
                                    } else {
                                        val mutableWorkerData = workerData.toMutableMap()

                                        // Format JJG values based on whether they're whole numbers
                                        val jjgTO = jjgValues["TO"] ?: 0.0
                                        val formattedJjgTO = if (jjgTO == jjgTO.toInt().toDouble()) {
                                            jjgTO.toInt().toString()
                                        } else {
                                            String.format("%.1f", jjgTO)
                                        }
                                        mutableWorkerData["jjg_each_blok"] = "$blokName($formattedJjgTO)"

                                        // Add the bullet point version for new workers
                                        mutableWorkerData["jjg_each_blok_bullet"] = "• $blokName ($formattedJjgTO Jjg)"

                                        mutableWorkerData["jjg_total_blok"] = if (jjgTO == jjgTO.toInt().toDouble()) {
                                            jjgTO.toInt().toString()
                                        } else {
                                            String.format("%.1f", jjgTO)
                                        }

                                        val jjgPA = jjgValues["PA"] ?: 0.0
                                        mutableWorkerData["jjg_dibayar"] = if (jjgPA == jjgPA.toInt().toDouble()) {
                                            jjgPA.toInt().toString()
                                        } else {
                                            String.format("%.1f", jjgPA)
                                        }

                                        // Initialize occurrence counter
                                        mutableWorkerData["occurrence_count"] = "1"

                                        // Initialize TPH ID tracking
                                        mutableWorkerData["tph_ids"] = tphId
                                        mutableWorkerData["tph_count"] = "1"

                                        globalMergedWorkerMap[workerName] = mutableWorkerData
                                    }
                                }

                                val finalMergedData = globalMergedWorkerMap.values.toList().sortedBy {
                                    it["nama_karyawans"].toString()
                                }

                                AppLogger.d("Final merged data: $finalMergedData")
                                mappedData = finalMergedData
                            } else {
                                mappedData = allWorkerData
                                AppLogger.d("Using standard data (no global merging): $mappedData")
                            }
                            AppLogger.d("mapped data $mappedData")
                            val distinctBlokNames = mappedData
                                .map { it["blok_name"].toString() }
                                .distinct()
                                .filter { it != "-" }
                                .sorted()
                                .joinToString(", ")

                            val blokDisplay = if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen || featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
                                val fieldToExtract = if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) "KP" else "TO"
                                mappedData
                                    .filter { it["blok_name"].toString() != "-" }
                                    .groupBy { it["blok_name"].toString() }
                                    .mapValues { (_, items) ->
                                        val count = items.size
                                        val toSum = items.sumOf { item ->
                                            extractJSONValue(item["jjg_json"].toString(), fieldToExtract)
                                        }
                                        "${toSum.toInt()}/$count"  // Convert double sum to integer for display
                                    }
                                    .toSortedMap() // Sort by blok_name
                                    .map { (blokName, summary) -> "$blokName ($summary)" }
                                    .joinToString(", ")
                            } else {
                                distinctBlokNames
                            }


                            AppLogger.d("gas $blokDisplay")

                            var totalJjgCount = 0
                            mappedData.forEach { data ->
                                try {
                                    val jjgJsonString = data["jjg_json"].toString()
                                    val jjgJson = JSONObject(jjgJsonString)
                                    val key =
                                        if (featureName == "Rekap panen dan restan" || featureName == "Detail eSPB") "KP" else "TO"
                                    totalJjgCount += jjgJson.optInt(key, 0)
                                } catch (e: Exception) {
                                    AppLogger.e("Error parsing jjg_json: ${e.message}")
                                }
                            }

                            // Calculate distinct TPH count
                            val tphCount = mappedData
                                .mapNotNull { it["tph_id"].toString().toIntOrNull() }
//                                .distinct()
                                .count()

                            if (featureName != "Detail eSPB") {
                                blokSection.visibility = View.VISIBLE
                                totalSection.visibility = View.VISIBLE
                            }

                            blok = distinctBlokNames.ifEmpty { "-" }
                            listBlok.text = blokDisplay
                            jjg = totalJjgCount
                            totalJjg.text = jjg.toString()
                            tph = tphCount
                            totalTPH.text = tph.toString()

                            // Set Blok
                            val tvBlok = findViewById<View>(R.id.tv_blok)
                            tvBlok.findViewById<TextView>(R.id.tvTitleEspb).text = "Blok"
                            tvBlok.findViewById<TextView>(R.id.tvSubTitleEspb).text = blok

                            // Set jjg
                            val tvJjg = findViewById<View>(R.id.tv_jjg)
                            tvJjg.findViewById<TextView>(R.id.tvTitleEspb).text = "Janjang"
                            tvJjg.findViewById<TextView>(R.id.tvSubTitleEspb).text = jjg.toString()

                            // Set jjg
                            val tvTph = findViewById<View>(R.id.tv_total_tph)
                            tvTph.findViewById<TextView>(R.id.tvTitleEspb).text = "Jumlah TPH"
                            tvTph.findViewById<TextView>(R.id.tvSubTitleEspb).text = tph.toString()

                            listAdapter.updateData(mappedData)
                            originalData =
                                emptyList() // Reset original data when new data is loaded
                            filterSection.visibility =
                                View.GONE // Hide filter section for new data


                        } else {


                            val emptyStateMessage =
                                if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen && currentState == 2)
                                    "Belum ada rekap data pemanen atau pastikan sudah menyimpan/konfirmasi scan"
                                else
                                    "No saved data available"

                            tvEmptyState.text = emptyStateMessage
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                            blokSection.visibility = View.GONE
                            totalSection.visibility = View.GONE
                        }

                    }


                    if (panenList.size == 0 && featureName == "Rekap Hasil Panen") {
                        btnGenerateQRTPH.visibility = View.GONE

                    } else if (panenList.size > 0 && featureName == "Rekap Hasil Panen" && currentState != 2) {
                        btnGenerateQRTPH.visibility = View.VISIBLE
                    }
                }, 500)
            }
        }

        panenViewModel.archivedPanenList.observe(this) { panenList ->
            if (currentState == 1) {
                listAdapter.updateData(emptyList())
                btnGenerateQRTPH.visibility = View.GONE
                val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                    .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
                headerCheckBox.visibility = View.GONE
                Handler(Looper.getMainLooper()).postDelayed({


                    loadingDialog.dismiss()
                    lifecycleScope.launch {

                        if (panenList.isNotEmpty()) {
                            AppLogger.d("kasdjflkd")
                            tvEmptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE

                            mappedData = panenList.map { panenWithRelations ->
                                val pemuatList = panenWithRelations.panen.karyawan_id.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }

                                val pemuatData: List<KaryawanModel>? = withContext(Dispatchers.IO) {
                                    try {
                                        panenViewModel.getPemuatByIdList(pemuatList)
                                    } catch (e: Exception) {
                                        AppLogger.e("Error fetching Pemuat Data: ${e.message}")
                                        null
                                    }
                                }
                                val rawKemandoran: List<String> = pemuatData
                                    ?.mapNotNull { it.kemandoran_id?.toString() }
                                    ?.distinct() ?: emptyList()

                                val kemandoranData: List<KemandoranModel>? =
                                    withContext(Dispatchers.IO) {
                                        try {
                                            panenViewModel.getKemandoranById(rawKemandoran)
                                        } catch (e: Exception) {
                                            AppLogger.e("Error fetching Kemandoran Data: ${e.message}")
                                            null
                                        }
                                    }

                                val kemandoranNamas = kemandoranData?.mapNotNull { it.nama }
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.joinToString("\n") { "• $it" } ?: "-"

                                val karyawanNamas = pemuatData?.mapNotNull { karyawan ->
                                    karyawan.nama?.let { nama ->
                                        // Always append NIK for every worker
                                        "$nama - ${karyawan.nik ?: "N/A"}"
                                    }
                                }?.takeIf { it.isNotEmpty() }
                                    ?.joinToString(", ") ?: "-"

                                mapOf<String, Any>(
                                    "id" to (panenWithRelations.panen.id as Any),
                                    "tph_id" to (panenWithRelations.panen.tph_id as Any),
                                    "date_created" to (panenWithRelations.panen.date_created as Any),
                                    "blok_name" to (panenWithRelations.tph?.blok_kode
                                        ?: "Unknown"), // Handle null safely
                                    "nomor" to (panenWithRelations.tph!!.nomor as Any),
                                    "created_by" to (panenWithRelations.panen.created_by as Any),
//                                    "karyawan_id" to (panenWithRelations.panen.karyawan_id as Any),
                                    "jjg_json" to (panenWithRelations.panen.jjg_json as Any),
                                    "foto" to (panenWithRelations.panen.foto as Any),
                                    "komentar" to (panenWithRelations.panen.komentar as Any),
                                    "asistensi" to (panenWithRelations.panen.asistensi as Any),
                                    "lat" to (panenWithRelations.panen.lat as Any),
                                    "lon" to (panenWithRelations.panen.lon as Any),
                                    "jenis_panen" to (panenWithRelations.panen.jenis_panen as Any),
                                    "ancak" to (panenWithRelations.panen.ancak as Any),
                                    "archive" to (panenWithRelations.panen.archive as Any),
                                    "nama_estate" to (panenWithRelations.tph.dept_abbr as Any),
                                    "nama_afdeling" to (panenWithRelations.tph.divisi_abbr as Any),
                                    "blok_banjir" to (panenWithRelations.panen.status_banjir as Any),
                                    "tahun_tanam" to (panenWithRelations.tph.tahun as Any),
                                    "nama_karyawans" to karyawanNamas as Any,
                                    "nama_kemandorans" to kemandoranNamas as Any,
                                    "username" to (panenWithRelations.panen.username as Any)
                                )
                            }

                            AppLogger.d("mapped data $mappedData")

                            val distinctBlokNames    = mappedData
                                .map { it["blok_name"].toString() }
                                .distinct()
                                .filter { it != "-" }
                                .sorted()
                                .joinToString(", ")

                            val blokDisplay = if (featureName == AppUtils.ListFeatureNames.RekapHasilPanen || featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) {
                                val fieldToExtract = if (featureName == AppUtils.ListFeatureNames.RekapPanenDanRestan) "KP" else "TO"

                                mappedData
                                    .filter { it["blok_name"].toString() != "-" }
                                    .groupBy { it["blok_name"].toString() }
                                    .mapValues { (_, items) ->
                                        val count = items.size
                                        val toSum = items.sumOf { item ->
                                            extractJSONValue(item["jjg_json"].toString(), fieldToExtract)
                                        }
                                        "${toSum.toInt()}/$count"  // Convert double sum to integer for display
                                    }
                                    .toSortedMap() // Sort by blok_name
                                    .map { (blokName, summary) -> "$blokName ($summary)" }
                                    .joinToString(", ")
                            } else {
                                distinctBlokNames
                            }

                            // Calculate total JJG by parsing JSON and summing TO values
                            var totalJjgCount = 0
                            mappedData.forEach { data ->
                                try {
                                    val jjgJsonString = data["jjg_json"].toString()
                                    val jjgJson = JSONObject(jjgJsonString)
                                    val key =
                                        if (featureName == "Rekap panen dan restan" || featureName == "Detail eSPB") "KP" else "TO"

                                    totalJjgCount += jjgJson.optInt(key, 0)
                                } catch (e: Exception) {
                                    AppLogger.e("Error parsing jjg_json: ${e.message}")
                                }
                            }

                            // Calculate distinct TPH count
                            val tphCount = mappedData
                                .mapNotNull { it["tph_id"].toString().toIntOrNull() }
//                                .distinct()
                                .count()

                            if (featureName != "Detail eSPB") {
                                blokSection.visibility = View.VISIBLE
                                totalSection.visibility = View.VISIBLE
                            }

                            listBlok.text = blokDisplay
                            totalJjg.text = totalJjgCount.toString()
                            totalTPH.text = tphCount.toString()

                            listAdapter.updateData(mappedData)
                            originalData =
                                emptyList() // Reset original data when new data is loaded
                            filterSection.visibility =
                                View.GONE // Hide filter section for new data
                        } else {
                            tvEmptyState.text = "No scanned data available"
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                            blokSection.visibility = View.GONE
                            totalSection.visibility = View.GONE

                            AppLogger.d("kasdjflkd")
                        }

                    }
                    counterTerscan.text = panenList.size.toString()
                }, 500)
            }
        }

        panenViewModel.error.observe(this) { errorMessage ->
            loadingDialog.dismiss()
            showErrorDialog(errorMessage)
        }
    }

    private fun initViewModel() {
        val factory = PanenViewModel.PanenViewModelFactory(application)
        panenViewModel = ViewModelProvider(this, factory)[PanenViewModel::class.java]
    }

    fun extractJSONValue(jjgJson: String, fieldName: String): Double {
        return try {
            // Try to parse as JSON first
            val jsonObject = JSONObject(jjgJson)
            if (jsonObject.has(fieldName)) {
                jsonObject.getDouble(fieldName)
            } else {
                0.0
            }
        } catch (e: Exception) {
            try {
                // Fallback to string parsing if JSON parsing fails
                val fieldString = "\"$fieldName\":"
                val startIndex = jjgJson.indexOf(fieldString) + fieldString.length

                // Find the end of the value (either comma or closing brace)
                val commaIndex = jjgJson.indexOf(",", startIndex)
                val braceIndex = jjgJson.indexOf("}", startIndex)

                // Choose the appropriate end index
                val endIndex = if (commaIndex > 0 && (braceIndex < 0 || commaIndex < braceIndex)) {
                    commaIndex
                } else {
                    braceIndex
                }

                jjgJson.substring(startIndex, endIndex).trim().toDouble()
            } catch (e: Exception) {
                AppLogger.d("Failed to extract $fieldName from JSON: $jjgJson")
                0.0 // Return 0.0 if all parsing attempts fail
            }
        }
    }

    private fun setupSearch() {
        searchEditText = findViewById(R.id.search_feature)
        val tvEmptyState = findViewById<TextView>(R.id.tvEmptyState)

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                listAdapter.filterData(query)

                // Handle empty state
                if (listAdapter.itemCount == 0) {
                    tvEmptyState.text = "Tidak ada data yang dicari"
                    tvEmptyState.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
        })
    }


    private fun showErrorDialog(errorMessage: String) {
        AlertDialogUtility.withSingleAction(
            this@ListPanenTBSActivity,
            stringXML(R.string.al_back),
            stringXML(R.string.al_failed_fetch_data),
            "${stringXML(R.string.al_failed_fetch_data_desc)} ${errorMessage}",
            "warning.json",
            R.color.colorRedDark
        ) {
            finish()
        }
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

    private fun startPeriodicDateTimeChecking() {
        dateTimeCheckHandler.postDelayed(dateTimeCheckRunnable, AppUtils.DATE_TIME_INITIAL_DELAY)

    }

    override fun onResume() {
        super.onResume()
        checkDateTimeSettings()
        if (activityInitialized && AppUtils.isDateTimeValid(this)) {
            startPeriodicDateTimeChecking()
        }
    }

    override fun onPause() {
        super.onPause()

        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()

        SoundPlayer.releaseMediaPlayer()
        // Ensure handler callbacks are removed
        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
    }

    private fun initializeActivity() {
        if (!activityInitialized) {
            activityInitialized = true
            setupUI()
        }
    }

    private fun setupCheckboxControl() {
        val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
            .findViewById<CheckBox>(R.id.headerCheckBoxPanen)

        headerCheckBox.apply {
            visibility = View.VISIBLE
            setOnCheckedChangeListener(null)
            setOnCheckedChangeListener { _, isChecked ->
                if (!isSettingUpCheckbox) {
                    listAdapter.selectAll(isChecked)
                    speedDial.visibility = if (isChecked) View.VISIBLE else View.GONE
                }
            }
        }

        listAdapter.setOnSelectionChangedListener { selectedCount ->
            isSettingUpCheckbox = true
            headerCheckBox.isChecked = listAdapter.isAllSelected()

            speedDial.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
            isSettingUpCheckbox = false
        }
    }

    fun formatGlobalDate(dateString: String): String {
        // Parse the date string in format "YYYY-MM-DD"
        val parts = dateString.split("-")
        if (parts.size != 3) return dateString // Return original if format doesn't match

        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()

        // Return formatted date string using getMonthFormat
        return "${AppUtils.getMonthFormat(month)} $day $year"
    }

    fun generateHighQualityQRCode(
        content: String,
        imageView: ImageView,
        sizePx: Int = 1000
    ) {
        try {
            // Create encoding hints for better quality
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(
                    EncodeHintType.ERROR_CORRECTION,
                    ErrorCorrectionLevel.M
                ) // Change to M for balance
                put(EncodeHintType.MARGIN, 1) // Smaller margin
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                // Remove fixed QR version to allow automatic scaling
            }

            // Create QR code writer with hints
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )

            // Create bitmap with appropriate size
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // Fill the bitmap
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            // Set the bitmap to ImageView with high quality scaling
            imageView.apply {
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleDelete(selectedItems: List<Map<String, Any>>) {
        this.vibrate()
        AlertDialogUtility.withTwoActions(
            this,
            getString(R.string.al_delete),
            getString(R.string.confirmation_dialog_title),
            "${getString(R.string.al_make_sure_delete)} ${selectedItems.size} data?",
            "warning.json",
            ContextCompat.getColor(this, R.color.colorRedDark),
            function = {
                loadingDialog.show()
                loadingDialog.setMessage("Deleting items...")

                panenViewModel.deleteMultipleItems(selectedItems)

                // Observe delete result
                panenViewModel.deleteItemsResult.observe(this) { isSuccess ->
                    loadingDialog.dismiss()
                    if (isSuccess) {
                        playSound(R.raw.data_terhapus)
                        Toast.makeText(
                            this,
                            "${getString(R.string.al_success_delete)} ${selectedItems.size} data",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Reload data based on current state
                        if (currentState == 0) {
                            panenViewModel.loadTPHNonESPB(0, 0, 0, globalFormattedDate)
                            panenViewModel.countTPHNonESPB(0, 0, 0, globalFormattedDate)
                            panenViewModel.countTPHESPB(1, 0, 0, globalFormattedDate)
                        } else {
                            panenViewModel.loadArchivedPanen()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "${getString(R.string.al_failed_delete)} data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    // Reset UI state
                    val headerCheckBox = findViewById<ConstraintLayout>(R.id.tableHeader)
                        .findViewById<CheckBox>(R.id.headerCheckBoxPanen)
                    headerCheckBox.isChecked = false
                    listAdapter.clearSelections()
                    speedDial.visibility = View.GONE
                }

                // Observe errors
                panenViewModel.error.observe(this) { errorMessage ->
                    loadingDialog.dismiss()
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        ) {

        }
    }


    private fun setupSpeedDial() {
        speedDial = findViewById(R.id.dial_tph_list)

        speedDial.apply {
//            addActionItem(
//                SpeedDialActionItem.Builder(R.id.scan_qr, R.drawable.baseline_qr_code_scanner_24)
//                    .setLabel(getString(R.string.generate_qr))
//                    .setFabBackgroundColor(
//                        ContextCompat.getColor(
//                            this@ListPanenTBSActivity,
//                            R.color.yellowbutton
//                        )
//                    )
//                    .create()
//            )

            addActionItem(
                SpeedDialActionItem.Builder(
                    R.id.deleteSelected,
                    R.drawable.baseline_delete_forever_24
                )
                    .setLabel(getString(R.string.dial_delete_item))
                    .setFabBackgroundColor(
                        ContextCompat.getColor(
                            this@ListPanenTBSActivity,
                            R.color.colorRedDark
                        )
                    )
                    .create()
            )

            visibility = View.GONE

            setOnActionSelectedListener { actionItem ->
                when (actionItem.id) {
                    R.id.scan_qr -> {
//                        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)
//
//                        view.background = ContextCompat.getDrawable(this@ListPanenTBSActivity, R.drawable.rounded_top_right_left)
//
//                        val dialog = BottomSheetDialog(this@ListPanenTBSActivity)
//                        dialog.setContentView(view)
////                        view.layoutParams.height = 500.toPx()
//
//                        val qrCodeImageView: ImageView = view.findViewById(R.id.qrCodeImageView)
//                        val data = "test"
//                        generateHighQualityQRCode(data, qrCodeImageView)
//                        dialog.setOnShowListener {
//                            val bottomSheet =
//                                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
//                            val behavior = BottomSheetBehavior.from(bottomSheet!!)
//                            behavior.state = BottomSheetBehavior.STATE_EXPANDED
//                        }
//                        dialog.show()
                        true
                    }
//                    R.id.cancelSelection -> {
//                        listAdapter.clearSelections()
//                        true
//                    }
                    R.id.deleteSelected -> {
                        val selectedItems = listAdapter.getSelectedItems()
                        handleDelete(selectedItems)
                        true
                    }

                    R.id.uploadSelected -> {
                        val selectedItems = listAdapter.getSelectedItems()

//                        if (AppUtils.isInternetAvailable(this@ListPanenTBSActivity)) {
//                            handleUpload(selectedItems)
//                        } else {
//                            AlertDialogUtility.withSingleAction(
//                                this@ListPanenTBSActivity,
//                                getString(R.string.al_back),
//                                getString(R.string.al_no_internet_connection),
//                                getString(R.string.al_no_internet_connection_description),
//                                "network_error.json",
//                                R.color.colorRedDark
//                            ) {}
//                        }
                        true
                    }

                    else -> false
                }
            }
        }


    }

    fun Int.toPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun initializeFilterViews() {
        filterSection = findViewById(R.id.filterSection)
        filterName = findViewById(R.id.filterName)
        removeFilter = findViewById(R.id.removeFilter)

        // Initially hide the filter section
        filterSection.visibility = View.GONE
    }

    private fun setupSortButton() {
        sortButton = findViewById(R.id.btn_sort)
        updateSortIcon() // Set initial icon state

        sortButton.setOnClickListener {
            // Store original data order if this is the first sort
            if (originalData.isEmpty()) {
                originalData = listAdapter.getCurrentData()
            }

            isAscendingOrder = !isAscendingOrder
            updateSortIcon() // Update icon on click

            listAdapter.sortData(isAscendingOrder)
            listAdapter.sortByCheckedItems(false)
            updateFilterDisplay()
        }

        setupRemoveFilter()
    }


    private fun updateFilterDisplay() {
        filterSection.visibility = View.VISIBLE
        filterName.text =
            if (isAscendingOrder) "Urutan Nomor TPH Kecil - Besar" else "Urutan Nomor TPH Besar - Kecil"
    }


    private fun setupRemoveFilter() {
        removeFilter.setOnClickListener {
            // Get current search query
            val currentSearchQuery = searchEditText.text.toString().trim()

            // Reset sort state
            isAscendingOrder = true
            updateSortIcon()

            if (originalData.isNotEmpty()) {
                // Reset the sort but maintain the filter
                listAdapter.resetSort()
                if (currentSearchQuery.isNotEmpty()) {
                    listAdapter.filterData(currentSearchQuery)
                }
                originalData = emptyList()
            }

            // Hide filter section
            filterSection.visibility = View.GONE
        }
    }

//    @SuppressLint("MissingSuperCall")
//    override fun onBackPressed() {
//        vibrate()
////        AlertDialogUtility.withTwoActions(
////            this,
////            "Simpan",
////            getString(R.string.confirmation_dialog_title),
////            getString(R.string.al_confirm_feature),
////            "warning.json"
////        ) {
//        val intent = Intent(this, HomePageActivity::class.java)
//        startActivity(intent)
//        finishAffinity()
////        }
//
//    }

    private fun updateSortIcon() {
        sortButton.animate()
            .scaleY(if (isAscendingOrder) 1f else -1f)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }


    private fun setupHeader() {
        featureName = intent.getStringExtra("FEATURE_NAME").toString()
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        val userSection = findViewById<TextView>(R.id.userSection)
        val titleAppNameAndVersion = findViewById<TextView>(R.id.titleAppNameAndVersionFeature)
        val lastUpdateText = findViewById<TextView>(R.id.lastUpdate)
        val locationSection = findViewById<LinearLayout>(R.id.locationSection)

        locationSection.visibility = View.GONE

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

    private fun setupRecyclerView() {
        val totalSection: LinearLayout = findViewById(R.id.total_section)
        val blokSection: LinearLayout = findViewById(R.id.blok_section)
        val totalJjgTextView: TextView = findViewById(R.id.totalJjg)
        val totalTphTextView: TextView = findViewById(R.id.totalTPH)
        val tvTotalTPH: TextView = findViewById(R.id.tvTotalTPH)
        val listBlokTextView: TextView = findViewById(R.id.listBlok) // Add this line

        val headers = if (featureName == "Buat eSPB") {
            listOf("BLOK", "NO TPH/JJG", "JAM", "KP")
        } else {
            listOf("BLOK", "NO TPH", "TOTAL JJG", "JAM")
        }
        updateTableHeaders(headers)

        listAdapter = ListPanenTPHAdapter()
        recyclerView.apply {
            adapter = listAdapter
            layoutManager = LinearLayoutManager(this@ListPanenTBSActivity)
        }

            tphListScan = processScannedResult(listTPHDriver)

            if (tphListScan.isEmpty()) {
                Toast.makeText(this, "Failed to process TPH QR", Toast.LENGTH_SHORT).show()
            } else {
                listAdapter.setFeatureAndScanned(featureName, tphListScan)
            }

        if (featureName == AppUtils.ListFeatureNames.BuatESPB) {
            listAdapter.setOnTotalsUpdateListener { tphCount, jjgCount, formattedBlocks ->
                if (tphCount > 0) {
                    totalSection.visibility = View.VISIBLE
                    blokSection.visibility = View.VISIBLE
                    totalTphTextView.text = tphCount.toString()
                    totalJjgTextView.text = jjgCount.toString()
                    tvTotalTPH.text = "Jmlh Transaksi: "

                    // No need to format again, just join the already formatted blocks
                    val blocksText = formattedBlocks.joinToString(", ")
                    listBlokTextView.text = blocksText
                    listBlokTextView.visibility = View.VISIBLE
                } else {
                    totalSection.visibility = View.GONE
                    blokSection.visibility = View.GONE
                    listBlokTextView.visibility = View.GONE
                }
            }
        }
    }

    private fun updateTableHeaders(headerNames: List<String>) {
        val tableHeader = findViewById<View>(R.id.tableHeader)

        // Adjust the header ID list to accommodate 5 columns if needed
        val headerIds = if (headerNames.size == 5) {
            listOf(R.id.th1, R.id.th2, R.id.th3, R.id.th4, R.id.th5)

        } else {
            listOf(R.id.th1, R.id.th2, R.id.th3, R.id.th4)
        }

        for (i in headerNames.indices) {
            val textView = tableHeader.findViewById<TextView>(headerIds[i])
            textView.apply {
                visibility = View.VISIBLE  // Make all headers visible
                text = headerNames[i]
            }
        }

        // Hide extra columns if not used (only applicable when switching from 5 to 4)
        if (headerNames.size < headerIds.size) {
            for (i in headerNames.size until headerIds.size) {
                tableHeader.findViewById<TextView>(headerIds[i]).visibility = View.GONE
            }
        }
    }

    fun processScannedResult(scannedResult: String): List<String> {
        // First check if it's already a list of IDs
        if (scannedResult.startsWith("[") && !scannedResult.contains("tph_0")) {
            return try {
                // This handles the case: [172355, 172357, 102354, ...]
                val listString = scannedResult.trim('[', ']')
                listString.split(", ").map { it.trim() }
            } catch (e: Exception) {
                Log.e("ListPanenTBSActivity", "Error parsing list format: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }

        if (scannedResult.contains("tph_0")) {
            Log.e("ListPanenTBSActivity", "Invalid data format containing tph_0")
            return emptyList() // Return null to indicate invalid format
        }
        // Default case - try the original parsing method
        return try {
            val tphString = scannedResult
                .removePrefix("""{"tph":"""")
                .removeSuffix(""""}""")
            tphString.split(";")
        } catch (e: Exception) {
            Log.e("ListPanenTBSActivity", "Error with default parsing: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }


    // Add a helper function to remove duplicate entries
    private fun removeDuplicateEntries(entries: String): String {
        if (entries.isEmpty()) return ""

        val uniqueEntries = entries.split(";")
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(";")

        return uniqueEntries
    }

}
