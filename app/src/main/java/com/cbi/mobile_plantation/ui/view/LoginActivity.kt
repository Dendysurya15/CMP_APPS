package com.cbi.mobile_plantation.ui.view

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.cbi.mobile_plantation.utils.LoadingDialog
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.setResponsiveTextSize
import com.cbi.mobile_plantation.utils.setResponsiveTextSizeWithConstraints
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    private var username = ""
    private var pass = ""

    private var prefManager: PrefManager? = null
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var authViewModel: AuthViewModel
    private lateinit var datasetViewModel: DatasetViewModel
    private val dateTimeCheckHandler = Handler(Looper.getMainLooper())
    private val dateTimeCheckRunnable = object : Runnable {
        override fun run() {
            checkDateTimeSettings()
            dateTimeCheckHandler.postDelayed(this, AppUtils.DATE_TIME_CHECK_INTERVAL)
        }
    }
    private var activityInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        //cek tanggal otomatis
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


    private fun initializeActivity() {
        if (!activityInitialized) {
            activityInitialized = true
            setupUI()
        }
    }

    private fun setupUI() {
        val btn_finger = findViewById<MaterialButton>(R.id.btn_finger)
        loadingDialog = LoadingDialog(this)
        prefManager = PrefManager(this)
        if (prefManager!!.rememberLogin) {
            if (AppUtils.checkBiometricSupport(this)) {
                btn_finger.visibility = View.VISIBLE

                biometricPrompt()
            } else {
                btn_finger.visibility = View.GONE

            }
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
                    stringXML(R.string.al_no_internet_connection),
                    stringXML(R.string.al_no_internet_connection_description_login),
                    "network_error.json",
                    R.color.colorRedDark
                ) {

                }
                return@observe
            }

            if (response.isSuccessful) {
                val loginResponse = response.body()
                AppLogger.d("Parsed Response: $loginResponse")
                if (loginResponse?.success == true) {
                    val token = loginResponse.data?.token
                    AppLogger.d("Login Success: $loginResponse")
                    if (token != null) {
                        datasetViewModel.clearAllData()
                        prefManager!!.isFirstTimeLaunch = true
                        prefManager!!.token = token
                        prefManager!!.username = usernameField.text.toString().trim()
                        prefManager!!.password = passwordField.text.toString().trim()
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
                        prefManager!!.lastSyncDate = null
                        prefManager!!.lastModifiedDatasetTPH = null
                    prefManager!!.lastModifiedDatasetKemandoran = null
                        prefManager!!.lastModifiedDatasetPemanen = null
                        prefManager!!.lastModifiedDatasetTransporter = null
                        prefManager!!.lastModifiedDatasetBlok = null
                        prefManager!!.clearDatasetMustUpdate()

                        Toasty.success(this, "Login Berhasil!", Toast.LENGTH_LONG, true).show()
                        navigateToHomePage()
                    } else {
                        AppLogger.d("Token not found in response")
                        Toasty.error(this, "Token not found!", Toast.LENGTH_LONG, true).show()
                    }
                } else {
                    AppLogger.d("Login Failed: ${loginResponse?.message}")

                    Toasty.error(
                        this,
                        loginResponse?.message ?: "Login failed",
                        Toast.LENGTH_LONG,
                        true
                    ).show()

                    lifecycleScope.launch {
                        delay(1000)

                        AlertDialogUtility.withSingleAction(
                            this@LoginActivity,
                            stringXML(R.string.al_back),
                            stringXML(R.string.al_failed_fetch_data),
                            loginResponse?.message ?: "Login failed",
                            "warning.json",
                            R.color.colorRedDark
                        ) {

                        }
                        hideLoading() // Hide loading after dialog
                    }
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


        val checkRememberMe = findViewById<CheckBox>(R.id.checkRememberMe)
        checkRememberMe.setResponsiveTextSizeWithConstraints(17F, 12F, 18F)
        checkRememberMe.setOnCheckedChangeListener { _, isChecked ->
            prefManager!!.rememberLogin = isChecked
        }

        setTampilan()

        loginButton.setOnClickListener {
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

            lifecycleScope.launch {
                loadingDialog.show()
                loadingDialog.setMessage(
                    "Sedang verifikasi kredensial...",
                    true
                ) // Checking credentials
                delay(1000)


                if (prefManager!!.username!!.isNotEmpty() && prefManager!!.password!!.isNotEmpty() && prefManager?.username == username && prefManager?.password == password) {
                    navigateToHomePage()
                } else {
                    if (AppUtils.isNetworkAvailable(this@LoginActivity)) {
                        authViewModel.login(username, password)
                    } else {
                        lifecycleScope.launch {
                            delay(500)

                            AlertDialogUtility.withSingleAction(
                                this@LoginActivity,
                                stringXML(R.string.al_back),
                                stringXML(R.string.al_no_internet_connection),
                                stringXML(R.string.al_no_internet_connection_description_login),
                                "network_error.json",
                                R.color.colorRedDark
                            ) {

                            }
                            hideLoading() // Hide loading after dialog
                        }
                    }
                }

            }

        }


        btn_finger.setOnClickListener {
            biometricPrompt()
        }
        setAppVersion()
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
        val checkRememberMe = findViewById<CheckBox>(R.id.checkRememberMe)
        val usernameField = findViewById<EditText>(R.id.usernameInput)
        val passwordField = findViewById<EditText>(R.id.passwordInput)

        if (prefManager!!.rememberLogin) {
            checkRememberMe.isChecked = true
            if (prefManager!!.username.toString().isNotEmpty() && prefManager!!.password.toString()
                    .isNotEmpty()
            ) {


                usernameField.setText(prefManager!!.username, TextView.BufferType.SPANNABLE)
                passwordField.setText(prefManager!!.password, TextView.BufferType.SPANNABLE)
                username = prefManager!!.username.toString()
                pass = prefManager!!.password.toString()
            }
        } else {
            checkRememberMe.isChecked = false
        }
    }

    private fun navigateToHomePage() {
        val checkRememberMe = findViewById<CheckBox>(R.id.checkRememberMe)
        if (!checkRememberMe.isChecked) {
            prefManager!!.username = ""
            prefManager!!.password = ""
            prefManager!!.rememberLogin = false
        }
        startActivity(Intent(this, HomePageActivity::class.java))
        finish()
    }

    private fun showLoading() {
        loadingDialog.show()
    }

    private fun hideLoading() {
        loadingDialog.dismiss()
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

        // Ensure handler callbacks are removed
        dateTimeCheckHandler.removeCallbacks(dateTimeCheckRunnable)
    }
}
