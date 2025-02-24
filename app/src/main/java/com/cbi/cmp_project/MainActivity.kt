package com.cbi.cmp_project

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cbi.cmp_project.data.database.AppDatabase
import com.cbi.cmp_project.data.model.FlagESPBModel
import com.cbi.cmp_project.ui.view.LoginActivity
import com.cbi.cmp_project.ui.view.panenTBS.FeaturePanenTBSActivity
import com.cbi.cmp_project.ui.view.weighBridge.ListHistoryWeighBridgeActivity
import com.cbi.cmp_project.ui.view.weighBridge.ScanWeighBridgeActivity
import com.cbi.cmp_project.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    @SuppressLint("SetTextI18n")
    private fun setAppVersion() {
        val versionTextView: TextView = findViewById(R.id.version_app)
        val appVersion = AppUtils.getAppVersion(this)
        versionTextView.text = "Versi $appVersion"
    }

    private fun showMainContent() {
        if (!showingSplash) return
        showingSplash = false

        startActivity(Intent(this, LoginActivity::class.java))
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
            FlagESPBModel(id = 4, flag = "Mekanisasi"),
            FlagESPBModel(id = 5, flag = "Banjir")
        )

        defaultFlags.forEach { dao.insert(it) }
        Log.d("Database", "Default flags inserted successfully")
    }
}