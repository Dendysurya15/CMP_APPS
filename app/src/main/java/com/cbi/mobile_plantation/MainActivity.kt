package com.cbi.mobile_plantation

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.cbi.mobile_plantation.data.database.AppDatabase
import com.cbi.mobile_plantation.data.model.FlagESPBModel
import com.cbi.mobile_plantation.ui.view.HektarPanen.TransferHektarPanenActivity
import com.cbi.mobile_plantation.ui.view.HomePageActivity
import com.cbi.mobile_plantation.ui.view.Inspection.FormInspectionActivity
import com.cbi.mobile_plantation.ui.view.Inspection.ListInspectionActivity
import com.cbi.mobile_plantation.ui.view.LoginActivity
import com.cbi.mobile_plantation.ui.view.panenTBS.FeaturePanenTBSActivity
import com.cbi.mobile_plantation.ui.view.panenTBS.ListPanenTBSActivity
import com.cbi.mobile_plantation.ui.view.weighBridge.ScanWeighBridgeActivity
import com.cbi.mobile_plantation.utils.AlertDialogUtility
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.cbi.mobile_plantation.utils.AppUtils.stringXML
import com.cbi.mobile_plantation.utils.PrefManager
import com.cbi.mobile_plantation.utils.setResponsiveTextSizeWithConstraints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer.Form

class MainActivity : AppCompatActivity() {
    private var showingSplash = true
    private lateinit var database: AppDatabase
    private var prefManager: PrefManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefManager = PrefManager(this)

        val layoutInflater = LayoutInflater.from(this)
        val rootView: View = if (!prefManager!!.isFirstTimeLaunch) {
            layoutInflater.inflate(R.layout.activity_welcome_screen, null)
        } else {
            layoutInflater.inflate(R.layout.activity_welcome_screen, null)
        }

        // Adjust the logo container's top margin based on screen height
        val logoContainer = rootView.findViewById<LinearLayout>(R.id.logo_container)
        if (logoContainer != null) {
            // Get the device screen height
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val screenHeightPx = displayMetrics.heightPixels

            // Get current layout parameters
            val layoutParams = logoContainer.layoutParams as MarginLayoutParams

            // For small screens, use 75dp margin
            if (screenHeightPx < 1800) { // Adjust this threshold as needed
                val marginTopDp = 125
                val marginTopPx = (marginTopDp * resources.displayMetrics.density).toInt()
                layoutParams.topMargin = marginTopPx
            }

            logoContainer.layoutParams = layoutParams
        }

        setAppVersion(rootView)
        setContentView(rootView)

        // Initialize database
        lifecycleScope.launch(Dispatchers.IO) {
            initializeDatabase()

            withContext(Dispatchers.Main) {
                delay(1500) // Wait for 1.5 seconds
                showMainContent()
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun setAppVersion(rootView: View) {
        val versionTextView: TextView? = rootView.findViewById(R.id.version_app)
        versionTextView?.setResponsiveTextSizeWithConstraints(17F, 12F,18F)
        if (versionTextView != null) {
            val appVersion = AppUtils.getAppVersion(this)
            versionTextView.text = "Versi $appVersion"
        } else {
            Log.e("MainActivity", "version_app TextView not found in the current layout")
        }
    }

    private suspend fun initializeDatabase() {
        try {
            database = AppDatabase.getDatabase(applicationContext)

            database.karyawanDao()
            database.kemandoranDao()
            database.tphDao()
            database.flagESPBModelDao()
            database.millDao()
            database.uploadCMPDao()
            database.absensiDao()
            database.parameterDao()
            insertDefaultFlags()

            Log.d("Database", "Database and tables initialized successfully")
        } catch (e: Exception) {
            Log.e("Database", "Error initializing database: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    "Error initializing database: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun showMainContent() {
        if (!showingSplash) return
        showingSplash = false

        startActivity(Intent(this, FormInspectionActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        AppDatabase.closeDatabase()
    }


    private suspend fun insertDefaultFlags() {
        val dao = database.flagESPBModelDao()

        val count = dao.getCount()
        if (count > 0) {
            Log.d("Database", "FlagESPBModel already initialized, skipping insertion.")
            return
        }

        val defaultFlags = listOf(
            FlagESPBModel(id = 0, flag = "Normal"),
            FlagESPBModel(id = 1, flag = "Addition"),
            FlagESPBModel(id = 2, flag = "Manual"),
            FlagESPBModel(id = 3, flag = "Restan"),
            FlagESPBModel(id = 4, flag = "Banjir")
        )

        defaultFlags.forEach { dao.insert(it) }
        Log.d("Database", "Default flags inserted successfully")
    }
}