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
import com.cbi.cmp_project.data.database.AppDatabase
import com.cbi.cmp_project.data.model.FlagESPBModel
import com.cbi.cmp_project.data.network.RetrofitClient
import com.cbi.cmp_project.ui.view.HomePageActivity
import com.cbi.cmp_project.ui.view.LoginActivity
import com.cbi.cmp_project.ui.view.PanenTBS.FeaturePanenTBSActivity
import com.cbi.cmp_project.ui.view.PanenTBS.ListPanenTBSActivity
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.LoadingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    private var showingSplash = true
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        setAppVersion()

        // Initialize database
        lifecycleScope.launch(Dispatchers.IO) {
            initializeDatabase()

            // After database initialization, wait for splash screen
            withContext(Dispatchers.Main) {
                delay(1500) // Wait for 1.5 seconds
                showMainContent()
            }
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

    private fun setAppVersion() {
        val versionTextView: TextView = findViewById(R.id.version_app)
        val appVersion = AppUtils.getAppVersion(this)
        versionTextView.text = appVersion
    }

    private fun showMainContent() {
        if (!showingSplash) return
        showingSplash = false

        startActivity(Intent(this, ListPanenTBSActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Close database when activity is destroyed
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
            FlagESPBModel(id = 3, flag = "Restan")
        )

        defaultFlags.forEach { dao.insert(it) }
        Log.d("Database", "Default flags inserted successfully")
    }
}