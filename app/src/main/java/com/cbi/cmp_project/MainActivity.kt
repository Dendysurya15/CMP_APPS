package com.cbi.cmp_project

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.cbi.cmp_project.data.database.DatabaseHelper
import com.cbi.cmp_project.data.network.RetrofitClient
import com.cbi.cmp_project.ui.view.FeaturePanenTBSActivity
import com.cbi.cmp_project.ui.view.LoginActivity
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.LoadingDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    private var showingSplash = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show splash screen layout first
        setContentView(R.layout.activity_splash_screen)

        // Set app version dynamically in the splash screen
        setAppVersion()
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase // Initialize the database

        if (db.isOpen) {
            Log.d("DatabaseCheck", "Database is open and accessible!")
        } else {
            Log.e("DatabaseCheck", "Database is closed!")
        }


        // Start a coroutine to handle splash screen timing
        lifecycleScope.launch {
            delay(1500) // Wait for 1.5
            showMainContent()
        }
    }

    private fun setAppVersion() {
        val versionTextView: TextView = findViewById(R.id.version_app)
        val appVersion = AppUtils.getAppVersion(this) // Use AppUtils here
        versionTextView.text = "$appVersion"
    }

    @SuppressLint("SuspiciousIndentation")
    private fun showMainContent() {
        if (!showingSplash) return
        showingSplash = false

//        // Check login status and navigate accordingly
//        if (isUserLoggedIn()) {
//            setContentView(R.layout.activity_main) // Switch to main layout
//            setupMainUI()
//        } else {
            startActivity(Intent(this, FeaturePanenTBSActivity::class.java))
            finish() // Remove MainActivity from the back stack
//        }
    }

    private fun isUserLoggedIn(): Boolean {
        // Replace with your logic to check if the user is logged in
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isLoggedIn", false)
    }

    private fun setupMainUI() {
        // Initialize main content views here
    }
}