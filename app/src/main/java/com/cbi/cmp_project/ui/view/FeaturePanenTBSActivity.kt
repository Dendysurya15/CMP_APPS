package com.cbi.cmp_project.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.repository.CameraRepository
import com.cbi.cmp_project.data.repository.PanenTBSRepository
import com.cbi.cmp_project.databinding.PertanyaanSpinnerLayoutBinding
import com.cbi.cmp_project.ui.adapter.TakeFotoPreviewAdapter
import com.cbi.cmp_project.ui.view.ui.home.HomeFragment
import com.cbi.cmp_project.ui.viewModel.CameraViewModel
import com.cbi.cmp_project.ui.viewModel.LocationViewModel
import com.cbi.cmp_project.ui.viewModel.PanenTBSViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jaredrummler.materialspinner.MaterialSpinner
import org.json.JSONObject
import java.io.File
import kotlin.reflect.KMutableProperty0

open class FeaturePanenTBSActivity : AppCompatActivity(), CameraRepository.PhotoCallback {

    private var jumTBS = 0
    var tbsMasak = 0
    private var bMentah = 0
    private var bLewatMasak = 0
    private var jjgKosong = 0
    private var abnormal = 0
    private var seranganTikus = 0
    private var tangkaiPanjang = 0
    private var tidakVcut = 0
    private var lat: Double? = null
    private var lon: Double? = null
    var currentAccuracy : Float = 0F

    private var featureName: String? = null
    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var panenTBSViewModel: PanenTBSViewModel
    private var locationEnable:Boolean = false
    private var isPermissionRationaleShown = false

    enum class InputType {
        SPINNER,
    }

    // Add these new counter variables
    private var buahMasak = 0
    private var kirimPabrik = 0
    private var tbsDibayar = 0



    private lateinit var inputMappings: List<Triple<LinearLayout, String, InputType>>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_panen_tbs)
        initViewModel()
        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressed() }

        setupHeader()
        setupLayout()


        val mbSaveDataPanenTBS = findViewById<MaterialButton>(R.id.mbSaveDataPanenTBS)

        mbSaveDataPanenTBS.setOnClickListener{

            AlertDialogUtility.withTwoActions(
                this,
                "Simpan",
                getString(R.string.confirmation_dialog_title),
                getString(R.string.confirmation_dialog_description)
            ) {

                panenTBSViewModel.insertPanenTBSVM(
                    user_id = 1,
                    tanggal = "2024-12-26",
                    name = "John Doe",
                    estate = "Estate 1",
                    id_estate = 101,
                    afdeling = "Afdeling A",
                    id_afdeling = 202,
                    blok = "Blok X",
                    id_blok = 303,
                    tahun_tanam = 2020,
                    id_tt = 404,
                    ancak = "No",
                    id_ancak = 505,
                    tph = "TPH-1",
                    id_tph = 606,
                    jenis_panen = "Harvest Type A",
                    list_pemanen = "John, Alice, Bob",
                    list_idpemanen = "1,2,3",
                    tbs = 1000,
                    tbs_mentah = 200,
                    tbs_lewatmasak = 100,
                    tks = 50,
                    abnormal = 10,
                    tikus = 5,
                    tangkai_panjang = 30,
                    vcut = 5,
                    tbs_masak = 900,
                    tbs_dibayar = 800,
                    tbs_kirim = 750,
                    latitude = "1.234567",
                    longitude = "103.456789",
                    foto = "image_url.jpg"
                )

                panenTBSViewModel.insertDBPanenTBS.observe(this) { isInserted ->

                    Log.d("testing", isInserted.toString())
                    if (isInserted){
                        AlertDialogUtility.alertDialogAction(
                            this,
                            "Sukses",
                            "Data berhasil disimpan!",

                        ) {
                            Toast.makeText(
                                this,
                                "sukses bro",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }else{
                        Toast.makeText(
                            this,
                            "Gagal bro",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }


            }

        }
    }

    private fun setupHeader() {
        featureName = intent.getStringExtra("FEATURE_NAME")
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        AppUtils.setupFeatureHeader(featureName, tvFeatureName)
    }

    private fun initializeCounters() {
        updateCounterTextViews() // This will set all counters to "0 Buah" initially
    }

    private fun updateCounterTextViews() {
        findViewById<TextView>(R.id.tvCounterBuahMasak).text = "$buahMasak Buah"
        findViewById<TextView>(R.id.tvCounterKirimPabrik).text = "$kirimPabrik Buah"
        findViewById<TextView>(R.id.tvCounterTBSDibayar).text = "$tbsDibayar Buah"
    }

    private fun updateDependentCounters(layoutId: Int, change: Int) {
        when (layoutId) {
            R.id.layoutJumTBS -> {
                if (change > 0) {
                    jumTBS += 1
                    formulas()
                } else { // When decrementing
                    if (tbsMasak > 0) {
                        jumTBS -= 1
                        formulas()
                    }
                }
            }
            R.id.layoutBMentah -> {
                if (change > 0) {
                    if (tbsMasak > 0) {
                        bMentah += 1
                        formulas()
                    }
                } else { // When decrementing
                    if (bMentah > 0) {
                        bMentah -= 1
                        formulas()
                    }
                }
            }
            R.id.layoutBLewatMasak -> {
                // Case 3: When BLewatMasak changes, subtract from all EXCEPT buahMasak

            }
            R.id.layoutJjgKosong -> {

            }
        }
        updateCounterTextViews()
    }

    private fun formulas(){
        buahMasak = jumTBS - jjgKosong - bMentah - bLewatMasak

        Log.d("testing", buahMasak.toString())
        bMentah = jumTBS - bLewatMasak - jjgKosong - tbsMasak
        bLewatMasak = jumTBS - bMentah - tbsMasak - jjgKosong
        jjgKosong = jumTBS - bMentah - tbsMasak - bLewatMasak
        tbsDibayar = jumTBS - bMentah - jjgKosong
        kirimPabrik = jumTBS - jjgKosong - abnormal
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
            LocationViewModel.Factory(application,status_location, this)
        )[LocationViewModel::class.java]
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

    /**
     * Sets up all spinner mappings, counters, and the RecyclerView.
     */
    private fun setupLayout() {

        inputMappings = listOf(
            Triple(findViewById<LinearLayout>(R.id.layoutEstate), getString(R.string.field_estate), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutAfdeling), getString(R.string.field_afdeling), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutTahunTanam), getString(R.string.field_tahun_tanam), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutBlok), getString(R.string.field_blok), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutAncak), getString(R.string.field_ancak), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutTipePanen), getString(R.string.field_tipe_panen), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutNoTPH), getString(R.string.field_no_tph), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutKemandoran), getString(R.string.field_kemandoran), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutPemanen), getString(R.string.field_pemanen), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutKemandoranLain), getString(R.string.field_kemandoran_lain), InputType.SPINNER),
            Triple(findViewById<LinearLayout>(R.id.layoutPemanenLain), getString(R.string.field_pemanen_lain), InputType.SPINNER)
        )


        // First set up all spinners with empty data and text
        inputMappings.forEach { (layoutView, key, inputType) ->
            updateTextInPertanyaan(layoutView, key)
            when (inputType) {
                InputType.SPINNER -> setupSpinnerView(layoutView, emptyList())
            }
        }


        val counterMappings = listOf(
            Triple(R.id.layoutJumTBS, "Jumlah TBS", ::jumTBS),
            Triple(R.id.layoutBMentah, "Buah Mentah", ::bMentah),
            Triple(R.id.layoutBLewatMasak, "Buah Lewat Masak", ::bLewatMasak),
            Triple(R.id.layoutJjgKosong, "Janjang Kosong", ::jjgKosong),
            Triple(R.id.layoutAbnormal, "Abnormal", ::abnormal),
            Triple(R.id.layoutSeranganTikus, "Serangan Tikus", ::seranganTikus),
            Triple(R.id.layoutTangkaiPanjang, "Tangkai Panjang", ::tangkaiPanjang),
            Triple(R.id.layoutTidakVcut, "Tidak V-Cut", ::tidakVcut)
        )
        counterMappings.forEach { (layoutId, labelText, counterVar) ->
            setupPaneWithButtons(layoutId, R.id.tvNumberPanen, labelText, counterVar)
        }

        setupRecyclerViewTakePreviewFoto()
    }

    private fun setupSpinnerView(linearLayout: LinearLayout, data: List<String>) {
        // Find the spinner inside the provided LinearLayout
        val spinner = linearLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)

        spinner.visibility = View.VISIBLE

    }


    /**
     * Configures the RecyclerView to repeat the layout 3 times.
     */
    private fun setupRecyclerViewTakePreviewFoto() {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewFotoPreview)

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        val itemWidth = resources.getDimensionPixelSize(R.dimen.item_width)

        val spanCount = if (itemWidth > 0) (screenWidth / itemWidth).coerceAtLeast(1) else 1

        val layoutManager = GridLayoutManager(this, spanCount)
        recyclerView.layoutManager = layoutManager

        val adapter = TakeFotoPreviewAdapter(3, cameraViewModel, this, featureName)
        recyclerView.adapter = adapter
    }

    /**
     * Updates the text of a spinner's label in the included layout.
     */
//    private fun updateTextInPertanyaanSpinner(layoutId: Int, textViewId: Int, newText: String) {
//        val includedLayout = findViewById<View>(layoutId)
//        val textView = includedLayout.findViewById<TextView>(textViewId)
//        textView.text = newText
//    }

    private fun updateTextInPertanyaan(linearLayout: LinearLayout, text: String) {
        // Assuming the TextView inside the LinearLayout has an ID, e.g., `tvTitleFormPanenTBS`
        val textView = linearLayout.findViewById<TextView>(R.id.tvTitleFormPanenTBS)
        textView.text = text
    }

    /**
     * Sets up a layout with increment and decrement buttons for counters.
     */
    private fun setupPaneWithButtons(layoutId: Int, textViewId: Int, labelText: String, counterVar: KMutableProperty0<Int>) {
        val includedLayout = findViewById<View>(layoutId)
        val textView = includedLayout.findViewById<TextView>(textViewId)
        val etNumber = includedLayout.findViewById<EditText>(R.id.etNumber)

        textView.text = labelText
        etNumber.setText(counterVar.get().toString())

        val btDec = includedLayout.findViewById<CardView>(R.id.btDec)
        val btInc = includedLayout.findViewById<CardView>(R.id.btInc)

        fun syncCounterWithEditText() {
            val enteredValue = etNumber.text.toString().toIntOrNull()
            if (enteredValue != null) {
                counterVar.set(enteredValue)
            }
        }

        fun vibrate() {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(50)
            }
        }

        fun changeEditTextStyle(isNegativeOrZero: Boolean) {
            if (isNegativeOrZero) {
                val redColor = ContextCompat.getColor(this, R.color.colorRedDark)
                etNumber.setTextColor(ColorStateList.valueOf(redColor))
                etNumber.setTypeface(null, Typeface.BOLD)
            } else {
                etNumber.setTextColor(ColorStateList.valueOf(Color.BLACK))
                etNumber.setTypeface(null, Typeface.NORMAL)
            }
        }

        btDec.setOnClickListener {
            syncCounterWithEditText()
            if (counterVar.get() > 0) {
                counterVar.set(counterVar.get() - 1)
                etNumber.setText(counterVar.get().toString())
                updateDependentCounters(layoutId, -1)
            } else {
                vibrate()
                changeEditTextStyle(counterVar.get() <= 0)
            }
        }

        btInc.setOnClickListener {
            syncCounterWithEditText()
            counterVar.set(counterVar.get() + 1)
            etNumber.setText(counterVar.get().toString())
            updateDependentCounters(layoutId, 1)
            changeEditTextStyle(counterVar.get() <= 0)
        }
    }


    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        when {
            cameraViewModel.statusCamera() -> {
                // If in camera mode, close camera and return to previous screen
                cameraViewModel.closeCamera()
            }
            else -> {
                val intent = Intent(this, HomePageActivity::class.java)
                startActivity(intent)
                finishAffinity()
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
    ) {

        // Update the RecyclerView item with the new photo
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewFotoPreview)
        val adapter = recyclerView.adapter as? TakeFotoPreviewAdapter

        adapter?.addPhotoFile("${position}", photoFile)

        // Find the specific ImageView in the RecyclerView item
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as? TakeFotoPreviewAdapter.FotoViewHolder
        viewHolder?.let {
            // Load the image using Glide or your preferred image loading library
            Glide.with(this)
                .load(photoFile)
                .into(it.imageView)
        }
    }


}
