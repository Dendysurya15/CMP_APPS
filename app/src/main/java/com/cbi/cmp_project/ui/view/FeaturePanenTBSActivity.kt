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
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.ui.adapter.TakeFotoPreviewAdapter
import com.cbi.cmp_project.ui.view.ui.home.HomeFragment
import com.cbi.cmp_project.ui.viewModel.LocationViewModel
import com.cbi.cmp_project.utils.AppUtils
import com.google.android.material.snackbar.Snackbar
import kotlin.reflect.KMutableProperty0

class FeaturePanenTBSActivity : AppCompatActivity() {

    private var jumTBS = 0
    private var bMentah = 0
    private var bLewatMasak = 0
    private var jjgKosong = 0
    private var abnormal = 0
    private var seranganTikus = 0
    private var tangkaiPanjang = 0
    private var tidakVcut = 0
    private var lat: Double? = null
    private var lon: Double? = null

    private lateinit var locationViewModel: LocationViewModel
    private var locationEnable:Boolean = false
    private var isPermissionRationaleShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_panen_tbs)

        // Set up the back button
        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressed() }

        setupHeader()
        setupLayout()
        initViewModel()
    }

    private fun setupHeader() {
        val featureName = intent.getStringExtra("FEATURE_NAME")
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        AppUtils.setupFeatureHeader(featureName, tvFeatureName)
    }

    private fun initViewModel() {
        val status_location = findViewById<ImageView>(R.id.status_location)
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
        // Update spinner mappings
        val spinnerMappings = listOf(
            Pair(R.id.layoutEstate, "Estate"),
            Pair(R.id.layoutAfdeling, "Afdeling"),
            Pair(R.id.layoutTahunTanam, "Tahun Tanam"),
            Pair(R.id.layoutBlok, "Blok"),
            Pair(R.id.layoutAncak, "Ancak"),
            Pair(R.id.layoutTipePanen, "Tipe Panen"),
            Pair(R.id.layoutNoTPH, "No. TPH"),
            Pair(R.id.layoutKemandoran, "Kemandoran"),
            Pair(R.id.layoutPemanen, "Pemanen"),
            Pair(R.id.layoutKemandoranLain, "Kemandoran Lain"),
            Pair(R.id.layoutPemanenLain, "Pemanen Lain")
        )
        spinnerMappings.forEach { (layoutId, text) ->
            updateTextInPertanyaanSpinner(layoutId, R.id.tvPanenTBS, text)
        }

        // Setup panes with counters
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

        val adapter = TakeFotoPreviewAdapter(3)
        recyclerView.adapter = adapter
    }

    /**
     * Updates the text of a spinner's label in the included layout.
     */
    private fun updateTextInPertanyaanSpinner(layoutId: Int, textViewId: Int, newText: String) {
        val includedLayout = findViewById<View>(layoutId)
        val textView = includedLayout.findViewById<TextView>(textViewId)
        textView.text = newText
    }

    /**
     * Sets up a layout with increment and decrement buttons for counters.
     */
    private fun setupPaneWithButtons(layoutId: Int, textViewId: Int, labelText: String, counterVar: KMutableProperty0<Int>) {
        val includedLayout = findViewById<View>(layoutId)
        val textView = includedLayout.findViewById<TextView>(textViewId)
        val etNumber = includedLayout.findViewById<EditText>(R.id.etNumber)

        // Set the initial label text
        textView.text = labelText
        etNumber.setText(counterVar.get().toString())

        // Set up listeners for increment and decrement buttons
        val btDec = includedLayout.findViewById<CardView>(R.id.btDec)
        val btInc = includedLayout.findViewById<CardView>(R.id.btInc)

        // Sync the value of counterVar with etNumber
        fun syncCounterWithEditText() {
            val enteredValue = etNumber.text.toString().toIntOrNull()
            if (enteredValue != null) {
                counterVar.set(enteredValue)
            }
        }

        fun vibrate() {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(100) // For devices below Android O
            }
        }

        fun changeEditTextStyle(isNegativeOrZero: Boolean) {
            if (isNegativeOrZero) {

                val redColor = ContextCompat.getColor(this, R.color.colorRedDark) // or use redColor defined in colors.xml
                etNumber.setTextColor(ColorStateList.valueOf(redColor))
                etNumber.setTypeface(null, Typeface.BOLD) // Set to bold style
            } else {
                etNumber.setTextColor(ColorStateList.valueOf(Color.BLACK))
                etNumber.setTypeface(null, Typeface.NORMAL) // Reset to normal weight
            }
        }

        btDec.setOnClickListener {
            syncCounterWithEditText()

            if (counterVar.get() > 0) {
                counterVar.set(counterVar.get() - 1)
                etNumber.setText(counterVar.get().toString())
            } else {
                vibrate() // Vibrate if counter is negative or zero

                changeEditTextStyle(counterVar.get() <= 0) // Apply red and bold style if value is zero or negative
            }
        }

        btInc.setOnClickListener {
            syncCounterWithEditText()
            counterVar.set(counterVar.get() + 1)
            etNumber.setText(counterVar.get().toString())

            changeEditTextStyle(counterVar.get() <= 0) // Apply red and bold style if value is zero or negative
        }

    }


    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, HomePageActivity::class.java)
        startActivity(intent)
        finishAffinity()
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

    }
}
