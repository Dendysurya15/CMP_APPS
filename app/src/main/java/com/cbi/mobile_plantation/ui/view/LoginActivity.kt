package com.cbi.mobile_plantation.ui.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.data.repository.AuthRepository
import com.cbi.mobile_plantation.ui.viewModel.AuthViewModel
import com.cbi.mobile_plantation.ui.viewModel.DatasetViewModel
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.GeoFenceHelper
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.setResponsiveTextSizeWithConstraints
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar
import java.util.Date

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1001
    }

    private var username = ""
    private var pass = ""

    private var prefManager: PrefManager? = null
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var authViewModel: AuthViewModel
    private lateinit var datasetViewModel: DatasetViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val dateTimeCheckHandler = Handler(Looper.getMainLooper())
    private val dateTimeCheckRunnable = object : Runnable {
        override fun run() {
            checkDateTimeSettings()
            dateTimeCheckHandler.postDelayed(this, AppUtils.DATE_TIME_CHECK_INTERVAL)
        }
    }
    private var activityInitialized = false
    private var permissionsGranted = false
    private var uiInitialized = false

    // Flag to track if location service dialog is currently showing
    private var isLocationDialogShowing = false

    // Activity result launcher for location settings
    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check again when user returns from settings
        isLocationDialogShowing = false
        if (isLocationEnabled()) {
            // Location is now enabled, try auto biometric if applicable
            tryAutoBiometricLogin()
        } else {
            // Still disabled, show dialog again
            showLocationServiceRequiredDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check date/time
        checkDateTimeSettings()
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

    @SuppressLint("SuspiciousIndentation")
    private fun initializeActivity() {
        if (!activityInitialized) {
            activityInitialized = true
            prefManager = PrefManager(this)

            // Check if the current year is before 2025
            val calendar = Calendar.getInstance()
            calendar.time = Date()
            val currentYear = calendar.get(Calendar.YEAR)
            val isYearValid = currentYear >= 2025

            AppLogger.d("Current year: $currentYear")
            AppLogger.d("Year valid (2025 or later): $isYearValid")

            // First check if year is valid
            if (!isYearValid) {
                // If year is not valid, show alert and exit
                AlertDialogUtility.withSingleAction(
                    this@LoginActivity,
                    stringXML(R.string.al_back),
                    "Sinkronisasi Tanggal Gagal",
                    "Sistem mendeteksi tanggal perangkat sebelum tahun 2025.\nSilakan melakukan sinkronisasi waktu dan tanggal dengan menhubungkan perangkat ke internet",
                    "warning.json",
                    R.color.colorRedDark
                ) {
                    finish()
                }
            } else {
                // Check and request permissions
                checkAndRequestPermissions()
            }
        }
    }

    /**
     * Check if location services (GPS) are enabled
     */
    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Check location service and show dialog if disabled
     * Returns true if location is enabled, false otherwise
     */
    private fun checkLocationServiceEnabled(): Boolean {
        if (!isLocationEnabled()) {
            showLocationServiceRequiredDialog()
            return false
        }
        isLocationDialogShowing = false
        return true
    }

    /**
     * Show dialog requiring user to enable location services
     */
    private fun showLocationServiceRequiredDialog() {
        if (isLocationDialogShowing) return
        isLocationDialogShowing = true

        AlertDialogUtility.withTwoActions(
            this@LoginActivity,
            "Aktifkan GPS",
            "Layanan Lokasi Diperlukan",
            "Aplikasi memerlukan layanan lokasi (GPS) untuk memverifikasi bahwa Anda berada di area yang diizinkan.\n\nSilakan aktifkan GPS untuk melanjutkan.",
            "warning.json",
            ContextCompat.getColor(this@LoginActivity, R.color.colorRedDark),
            "Keluar",
            function = {
                // Open location settings
                isLocationDialogShowing = false
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                locationSettingsLauncher.launch(intent)
            },
            cancelFunction = {
                // Exit app if user refuses to enable location
                isLocationDialogShowing = false
                finish()
            }
        )
    }

    /**
     * Check and request all necessary permissions
     */
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            val bluetoothPermissions = listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
            bluetoothPermissions.forEach {
                if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(it)
                }
            }
        } else {
            // Android 11 and below
            val bluetoothPermissions = listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
            bluetoothPermissions.forEach {
                if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(it)
                }
            }
        }

        // Location permission (CRITICAL for geofencing)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        // Storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            AppLogger.d("Requesting ${permissionsToRequest.size} permissions: $permissionsToRequest")
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        } else {
            // All permissions already granted
            permissionsGranted = true
            // Always setup UI first, then check location
            setupUI()
            // Check location service after UI is ready
            if (checkLocationServiceEnabled()) {
                // If location is enabled, try auto biometric
                tryAutoBiometricLogin()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                val deniedPermissions = mutableListOf<String>()
                var locationPermissionGranted = true

                permissions.forEachIndexed { index, permission ->
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        if (permission == Manifest.permission.ACCESS_FINE_LOCATION) {
                            locationPermissionGranted = false
                        }
                        deniedPermissions.add(permission)
                    }
                }

                // Location permission is CRITICAL - cannot proceed without it
                if (!locationPermissionGranted) {
                    AlertDialogUtility.withTwoActions(
                        this@LoginActivity,
                        "Pengaturan",
                        "Izin Lokasi Diperlukan",
                        "Aplikasi memerlukan izin lokasi untuk memverifikasi bahwa Anda berada di area yang diizinkan. Tanpa izin ini, Anda tidak dapat login.",
                        "warning.json",
                        ContextCompat.getColor(this@LoginActivity, R.color.colorRedDark),
                        "Keluar",
                        function = {
                            // Open app settings
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            }
                            startActivity(intent)
                        },
                        cancelFunction = {
                            finish()
                        }
                    )
                } else {
                    // Location permission granted, other permissions may be denied
                    if (deniedPermissions.isNotEmpty()) {
                        showPermissionsDeniedSnackbar(deniedPermissions)
                    }
                    permissionsGranted = true
                    // Always setup UI first
                    setupUI()
                    // Then check if location service is enabled
                    if (checkLocationServiceEnabled()) {
                        tryAutoBiometricLogin()
                    }
                }
            }
        }
    }

    private fun showPermissionsDeniedSnackbar(deniedPermissions: List<String>) {
        val message = buildString {
            append("Beberapa izin ditolak. Aplikasi mungkin tidak berfungsi penuh tanpa izin:\n")
            deniedPermissions.forEach {
                append("- ${it.replace("android.permission.", "")}\n")
            }
        }

        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .setAction("Pengaturan") {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }.apply {
                view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.maxLines = 5
            }.show()
    }

    /**
     * Try to show biometric prompt automatically if conditions are met
     */
    private fun tryAutoBiometricLogin() {
        val hasStoredCredentials = !prefManager!!.username.toString().isEmpty() &&
                !prefManager!!.password.toString().isEmpty()

        if (hasStoredCredentials && AppUtils.checkBiometricSupport(this) && isLocationEnabled()) {
            checkLocationAndBiometricLogin()
        }
    }

    private fun setupUI() {
        // Prevent multiple initializations
        if (uiInitialized) return
        uiInitialized = true

        loadingDialog = LoadingDialog(this)

        val btn_finger = findViewById<MaterialButton>(R.id.btn_finger)
        val hasStoredCredentials = !prefManager!!.username.toString().isEmpty() &&
                !prefManager!!.password.toString().isEmpty()

        // Always show fingerprint button if there are stored credentials and biometric is supported
        if (hasStoredCredentials && AppUtils.checkBiometricSupport(this)) {
            btn_finger.visibility = View.VISIBLE
        } else {
            btn_finger.visibility = View.GONE
        }

        val etPasswordLayout = findViewById<TextInputLayout>(R.id.etPasswordLayout)
        etPasswordLayout.setEndIconTintList(ColorStateList.valueOf(getColor(R.color.graytextdark)))

        val loginButton = findViewById<MaterialButton>(R.id.btn_login_submit)
        val usernameField = findViewById<EditText>(R.id.usernameInput)
        val passwordField = findViewById<EditText>(R.id.passwordInput)

        usernameField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val usernameInputLayout = findViewById<TextInputLayout>(R.id.etUsernameLayout)
                usernameInputLayout.error = null
                usernameInputLayout.isErrorEnabled = false
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        passwordField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val passwordInputLayout = findViewById<TextInputLayout>(R.id.etPasswordLayout)
                passwordInputLayout.error = null
                passwordInputLayout.isErrorEnabled = false
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val tvForgotLogin = findViewById<TextView>(R.id.tvForgotLogin)
        tvForgotLogin.setResponsiveTextSizeWithConstraints(17F, 12F, 18F)
        tvForgotLogin.setOnClickListener {
            AlertDialogUtility.withSingleAction(
                this@LoginActivity,
                stringXML(R.string.al_back),
                stringXML(R.string.al_features_still_in_development),
                stringXML(R.string.al_desc_features_still_in_development),
                "warning.json",
                R.color.yellowbutton
            ) {

            }
        }

        authViewModel = ViewModelProvider(
            this,
            AuthViewModel.Factory(AuthRepository())
        ).get(AuthViewModel::class.java)

        val factory = DatasetViewModel.DatasetViewModelFactory(application)
        datasetViewModel = ViewModelProvider(this, factory)[DatasetViewModel::class.java]

        authViewModel.loginResponse.observe(this) { response ->
            hideLoading()

            // Log raw response
            AppLogger.d("Raw Response: $response")

            if (response == null) {
                AppLogger.d("Response is null")

                AlertDialogUtility.withSingleAction(
                    this@LoginActivity,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_failed_fetch_data),
                    stringXML(R.string.al_no_internet_connection_description_login),
                    "network_error.json",
                    R.color.colorRedDark
                ) {

                }
            } else if (response.isSuccessful) {
                val loginResponse = response.body()

                if (loginResponse == null) {
                    AppLogger.d("Response body is null")
                    AlertDialogUtility.withSingleAction(
                        this@LoginActivity,
                        stringXML(R.string.al_back),
                        stringXML(R.string.al_failed_fetch_data),
                        stringXML(R.string.al_no_internet_connection_description_login),
                        "network_error.json",
                        R.color.colorRedDark
                    ) {

                    }
                    return@observe
                }

                AppLogger.d("Response Body: $loginResponse")

                if (loginResponse.success == true) {
                    AppLogger.d("Login successful")

                    val token = loginResponse.data?.token ?: ""

                    if (token.isNotEmpty()) {
                        prefManager!!.isFirstTimeLaunch = true
                        prefManager!!.token = token
                        prefManager!!.username = username
                        prefManager!!.password = pass
                        prefManager!!.nameUserLogin = loginResponse.data?.user?.nama
                        prefManager!!.idUserLogin = loginResponse.data?.user?.id!!
                        prefManager!!.jabatanUserLogin = loginResponse.data?.user?.jabatan
                        prefManager!!.estateUserLogin = loginResponse.data?.user?.dept_abbr
                        prefManager!!.estateUserLengkapLogin = loginResponse.data?.user?.dept_nama
                        prefManager!!.estateIdUserLogin = loginResponse.data?.user?.dept_id
                        prefManager!!.regionalIdUserLogin = loginResponse.data?.user?.regional
                        prefManager!!.companyIdUserLogin = loginResponse.data?.user?.company
                        prefManager!!.companyAbbrUserLogin = loginResponse.data?.user?.company_abbr
                        prefManager!!.companyNamaUserLogin = loginResponse.data?.user?.company_nama
                        prefManager!!.kemandoranPPROUserLogin = loginResponse.data?.user?.kemandoran_ppro
                        prefManager!!.kemandoranUserLogin = loginResponse.data?.user?.kemandoran
                        prefManager!!.kemandoranNamaUserLogin = loginResponse.data?.user?.kemandoran_nama
                        prefManager!!.kemandoranKodeUserLogin = loginResponse.data?.user?.kemandoran_kode
                        prefManager!!.afdelingIdUserLogin = loginResponse.data?.user?.divisi

                        lifecycleScope.launch {
                            delay(500)
                            navigateToHomePage()
                        }
                    }
                } else {
                    AppLogger.d("Login failed")
                    val message = loginResponse?.message ?: "Gagal Login"

                    AlertDialogUtility.withSingleAction(
                        this@LoginActivity,
                        stringXML(R.string.al_back),
                        "Gagal Login",
                        message,
                        "warning.json",
                        R.color.colorRedDark
                    ) {

                    }
                    hideLoading()
                }
            } else {
                val errorMessage = try {
                    val jsonObject = JSONObject(response.errorBody()?.string() ?: "{}")
                    jsonObject.getString("message")
                } catch (e: Exception) {
                    AppLogger.e("JSON Parsing Error: ${e.message}")
                    e.message ?: "Parsing error"
                }
                AppLogger.d("Response Error: Code ${response.code()} - Message: $errorMessage")

                AlertDialogUtility.withSingleAction(
                    this@LoginActivity,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_failed_fetch_data),
                    errorMessage,
                    "warning.json",
                    R.color.colorRedDark
                ) {

                }

                Toasty.error(this, errorMessage, Toast.LENGTH_SHORT, true).show()
            }
        }

        // Always set up the display with saved credentials
        setTampilan()

        loginButton.setOnClickListener {
            // First check if location service is enabled
            if (!isLocationEnabled()) {
                showLocationServiceRequiredDialog()
                return@setOnClickListener
            }

            val username = usernameField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            val usernameInputLayout = findViewById<TextInputLayout>(R.id.etUsernameLayout)
            val passwordInputLayout = findViewById<TextInputLayout>(R.id.etPasswordLayout)

            usernameInputLayout.error = null
            usernameInputLayout.isErrorEnabled = false
            passwordInputLayout.error = null
            passwordInputLayout.isErrorEnabled = false

            if (username.isEmpty() || password.isEmpty()) {
                // Show error for the username field if empty
                if (username.isEmpty()) {
                    usernameInputLayout.isErrorEnabled = true
                    usernameInputLayout.error = stringXML(R.string.alert_login_username_field_empty)
                }

                // Show error for the password field if empty
                if (password.isEmpty()) {
                    passwordInputLayout.isErrorEnabled = true
                    passwordInputLayout.error = stringXML(R.string.alert_login_password_field_empty)
                }

                return@setOnClickListener
            }

            if (prefManager!!.registeredDeviceUsername != null &&
                prefManager!!.registeredDeviceUsername!!.isNotEmpty() &&
                prefManager!!.registeredDeviceUsername != username
            ) {
                AlertDialogUtility.withSingleAction(
                    this@LoginActivity,
                    stringXML(R.string.al_back),
                    "Perangkat Terdaftar untuk Pengguna Lain",
                    "Perangkat ini sudah terdaftar untuk pengguna ${prefManager!!.registeredDeviceUsername}. Silakan gunakan akun yang terdaftar!",
                    "warning.json",
                    R.color.colorRedDark
                ) { }
                return@setOnClickListener
            }

            // Check location before proceeding with login
            checkLocationAndLogin(username, password)
        }

        btn_finger.setOnClickListener {
            // First check if location service is enabled
            if (!isLocationEnabled()) {
                showLocationServiceRequiredDialog()
                return@setOnClickListener
            }
            // Check location before biometric login
            checkLocationAndBiometricLogin()
        }
        setAppVersion()
    }

    /**
     * Check user's location and proceed with login if inside geofence
     */
    private fun checkLocationAndLogin(username: String, password: String) {
        // Check if location service is enabled first
        if (!isLocationEnabled()) {
            showLocationServiceRequiredDialog()
            return
        }

        if (!permissionsGranted || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            AlertDialogUtility.withSingleAction(
                this@LoginActivity,
                stringXML(R.string.al_back),
                "Izin Lokasi Diperlukan",
                "Aplikasi memerlukan izin lokasi untuk memverifikasi area Anda.",
                "warning.json",
                R.color.colorRedDark
            ) { }
            return
        }

        lifecycleScope.launch {
            loadingDialog.show()
            loadingDialog.setMessage("Memeriksa Lokasi...", true)

            try {
                val location = getCurrentLocation()

                if (location != null) {
                    val isInside = GeoFenceHelper.isLocationInsideBoundary(
                        this@LoginActivity,
                        location.latitude,
                        location.longitude
                    )

                    if (isInside) {
                        // Location is inside boundary, proceed with login
                        AppLogger.d("Location verified: User is inside allowed area")
                        loadingDialog.setMessage("Verifikasi Username & Password...", true)
                        delay(500)

                        proceedWithLogin(username, password)
                    } else {
                        // Location is outside boundary
                        hideLoading()

                        val distance = GeoFenceHelper.getDistanceToNearestBoundary(
                            this@LoginActivity,
                            location.latitude,
                            location.longitude
                        )

                        val distanceKm = distance / 1000.0
                        val distanceText = if (distanceKm < 1) {
                            "${String.format("%.0f", distance)} meter"
                        } else {
                            "${String.format("%.1f", distanceKm)} km"
                        }

                        AlertDialogUtility.withSingleAction(
                            this@LoginActivity,
                            stringXML(R.string.al_back),
                            "Lokasi di Luar Area",
                            "Anda berada di luar area yang diizinkan (± $distanceText dari batas terdekat).\n\nSilakan masuk ke area yang telah ditentukan untuk dapat login ke aplikasi.",
                            "warning.json",
                            R.color.colorRedDark
                        ) { }
                    }
                } else {
                    hideLoading()
                    AlertDialogUtility.withSingleAction(
                        this@LoginActivity,
                        stringXML(R.string.al_back),
                        "Gagal Mendapatkan Lokasi",
                        "Tidak dapat mendapatkan lokasi Anda. Pastikan GPS aktif dan coba lagi.",
                        "warning.json",
                        R.color.colorRedDark
                    ) { }
                }
            } catch (e: Exception) {
                hideLoading()
                AppLogger.e("Location error: ${e.message}")
                AlertDialogUtility.withSingleAction(
                    this@LoginActivity,
                    stringXML(R.string.al_back),
                    "Error Lokasi",
                    "Terjadi kesalahan saat memeriksa lokasi: ${e.message}",
                    "warning.json",
                    R.color.colorRedDark
                ) { }
            }
        }
    }

    /**
     * Check location before biometric login
     */
    private fun checkLocationAndBiometricLogin() {
        // Check if location service is enabled first
        if (!isLocationEnabled()) {
            showLocationServiceRequiredDialog()
            return
        }

        if (!permissionsGranted || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            AlertDialogUtility.withSingleAction(
                this@LoginActivity,
                stringXML(R.string.al_back),
                "Izin Lokasi Diperlukan",
                "Aplikasi memerlukan izin lokasi untuk memverifikasi area Anda.",
                "warning.json",
                R.color.colorRedDark
            ) { }
            return
        }

        lifecycleScope.launch {
            loadingDialog.show()
            loadingDialog.setMessage("Memeriksa Lokasi...", true)

            try {
                val location = getCurrentLocation()

                if (location != null) {
                    val isInside = GeoFenceHelper.isLocationInsideBoundary(
                        this@LoginActivity,
                        location.latitude,
                        location.longitude
                    )

                    hideLoading()

                    if (isInside) {
                        // Location is inside boundary, proceed with biometric
                        biometricPrompt()
                    } else {
                        // Location is outside boundary
                        val distance = GeoFenceHelper.getDistanceToNearestBoundary(
                            this@LoginActivity,
                            location.latitude,
                            location.longitude
                        )

                        val distanceKm = distance / 1000.0
                        val distanceText = if (distanceKm < 1) {
                            "${String.format("%.0f", distance)} meter"
                        } else {
                            "${String.format("%.1f", distanceKm)} km"
                        }

                        AlertDialogUtility.withSingleAction(
                            this@LoginActivity,
                            stringXML(R.string.al_back),
                            "Lokasi di Luar Area",
                            "Anda berada di luar area yang diizinkan (± $distanceText dari batas terdekat).\n\nSilakan masuk ke area yang telah ditentukan untuk dapat login ke aplikasi.",
                            "warning.json",
                            R.color.colorRedDark
                        ) { }
                    }
                } else {
                    hideLoading()
                    AlertDialogUtility.withSingleAction(
                        this@LoginActivity,
                        stringXML(R.string.al_back),
                        "Gagal Mendapatkan Lokasi",
                        "Tidak dapat mendapatkan lokasi Anda. Pastikan GPS aktif dan coba lagi.",
                        "warning.json",
                        R.color.colorRedDark
                    ) { }
                }
            } catch (e: Exception) {
                hideLoading()
                AppLogger.e("Location error: ${e.message}")
                AlertDialogUtility.withSingleAction(
                    this@LoginActivity,
                    stringXML(R.string.al_back),
                    "Error Lokasi",
                    "Terjadi kesalahan saat memeriksa lokasi: ${e.message}",
                    "warning.json",
                    R.color.colorRedDark
                ) { }
            }
        }
    }

    /**
     * Get current location using FusedLocationProviderClient
     */
    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Location? {
        return try {
            val cancellationTokenSource = CancellationTokenSource()

            val locationTask = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            )

            // Wait for location with timeout
            var location: Location? = null
            val timeoutMillis = 15000L // 15 seconds timeout

            lifecycleScope.launch {
                delay(timeoutMillis)
                if (location == null) {
                    cancellationTokenSource.cancel()
                }
            }

            locationTask.addOnSuccessListener { loc ->
                location = loc
                AppLogger.d("Location obtained: ${loc?.latitude}, ${loc?.longitude}")
            }.addOnFailureListener { e ->
                AppLogger.e("Failed to get location: ${e.message}")
            }

            // Wait for result
            var elapsed = 0L
            val checkInterval = 100L
            while (location == null && elapsed < timeoutMillis) {
                delay(checkInterval)
                elapsed += checkInterval
            }

            location
        } catch (e: Exception) {
            AppLogger.e("getCurrentLocation error: ${e.message}")
            null
        }
    }

    /**
     * Proceed with actual login after location verification
     */
    private fun proceedWithLogin(username: String, password: String) {
        lifecycleScope.launch {
            this@LoginActivity.username = username
            this@LoginActivity.pass = password

            // Check if credentials match saved credentials (offline login)
            val savedUsername = prefManager!!.username
            val savedPassword = prefManager!!.password

            val credentialsMatch = !savedUsername.isNullOrEmpty() &&
                    !savedPassword.isNullOrEmpty() &&
                    savedUsername == username &&
                    savedPassword == password

            if (credentialsMatch) {
                // Credentials match saved ones - allow offline login
                AppLogger.d("Offline login: Credentials match saved credentials")
                navigateToHomePage()
            } else {
                // Credentials don't match or no saved credentials - need network
                if (AppUtils.isNetworkAvailable(this@LoginActivity)) {
                    // Online - try to authenticate with server
                    authViewModel.login(username, password)
                } else {
                    // Offline and credentials don't match
                    hideLoading()
                    delay(300)

                    if (savedUsername.isNullOrEmpty() || savedPassword.isNullOrEmpty()) {
                        // No saved credentials - must be online for first login
                        AlertDialogUtility.withSingleAction(
                            this@LoginActivity,
                            stringXML(R.string.al_back),
                            stringXML(R.string.al_no_internet_connection),
                            "Login pertama kali memerlukan koneksi internet untuk verifikasi akun.",
                            "network_error.json",
                            R.color.colorRedDark
                        ) { }
                    } else {
                        // Has saved credentials but entered different ones
                        AlertDialogUtility.withSingleAction(
                            this@LoginActivity,
                            stringXML(R.string.al_back),
                            stringXML(R.string.al_no_internet_connection),
                            "Username atau password tidak sesuai dengan akun tersimpan.\n\nUntuk login dengan akun berbeda, silakan hubungkan ke internet.",
                            "network_error.json",
                            R.color.colorRedDark
                        ) { }
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setAppVersion() {
        val versionTextView: TextView = findViewById(R.id.version_app)
        versionTextView?.setResponsiveTextSizeWithConstraints(17F, 12F, 18F)
        val appVersion = AppUtils.getAppVersion(this)
        versionTextView.text = "Versi $appVersion"
    }


    private fun biometricPrompt() {
        AppUtils.showBiometricPrompt(this, prefManager!!.nameUserLogin.toString()) {
            runOnUiThread {
                loadingDialog.show()
            }

            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }

    private fun setTampilan() {
        val usernameField = findViewById<EditText>(R.id.usernameInput)
        val passwordField = findViewById<EditText>(R.id.passwordInput)

        AppLogger.d("Saved username: ${prefManager!!.username}")
        AppLogger.d("Saved password exists: ${!prefManager!!.password.isNullOrEmpty()}")

        if (!prefManager!!.username.isNullOrEmpty()) {
            usernameField.setText(prefManager!!.username, TextView.BufferType.SPANNABLE)
        }

        if (!prefManager!!.password.isNullOrEmpty()) {
            passwordField.setText(prefManager!!.password, TextView.BufferType.SPANNABLE)
        }
    }

    private fun navigateToHomePage() {
        hideLoading()
        startActivity(Intent(this, HomePageActivity::class.java))
        finish()
    }

    private fun showLoading() {
        if (::loadingDialog.isInitialized) {
            loadingDialog.show()
        }
    }

    private fun hideLoading() {
        if (::loadingDialog.isInitialized) {
            loadingDialog.dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        checkDateTimeSettings()
        if (activityInitialized && AppUtils.isDateTimeValid(this)) {
            startPeriodicDateTimeChecking()

            // Only check location service if UI is already initialized
            if (uiInitialized && permissionsGranted && !isLocationEnabled() && !isLocationDialogShowing) {
                showLocationServiceRequiredDialog()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
    }
}