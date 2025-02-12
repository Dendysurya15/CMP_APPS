package com.cbi.cmp_project.ui.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cbi.cmp_project.R
import com.cbi.cmp_project.ui.view.ListTPHApproval.Companion.EXTRA_QR_RESULT
import com.cbi.cmp_project.ui.view.PanenTBS.ListPanenTBSActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class ScanQR : AppCompatActivity() {

    var menuString = ""

    private val qrCodeLauncher = registerForActivityResult(
        ScanContract()
    ) { result ->
        if (result.contents != null) {
            Log.d("testing",result.contents)
            // Launch result activity with scanned content
            var intent = Intent(this, ListTPHApproval::class.java).apply {

                putExtra(EXTRA_QR_RESULT, result.contents) }
            when (menuString) {
                "Buat eSPB" -> intent = Intent(this, ListPanenTBSActivity::class.java).apply {
                    Log.d("testing",result.contents)
                putExtra(EXTRA_QR_RESULT, result.contents)
                putExtra("FEATURE_NAME", menuString)}
            }
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(
                this,
                "Scanning cancelled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        menuString = intent.getStringExtra("FEATURE_NAME") ?: ""
        Log.d("testing", "menu string $menuString ")
        setContentView(R.layout.activity_generate_espb)
        checkPermissionAndStartScanning()
    }

    private fun checkPermissionAndStartScanning() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startScanning()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun startScanning() {
        // Configure scanner options
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setOrientationLocked(false)
            setBeepEnabled(true)
            setPrompt(menuString)
            setTimeout(60000)  // 60 second timeout
            setCameraId(0)  // Use back camera
            setBarcodeImageEnabled(true)  // Save scanned barcode image
        }

        // Launch the scanner
        qrCodeLauncher.launch(options)
    }

    override fun onResume() {
        super.onResume()
        // If returning from ResultActivity, we might want to start scanning again
        if (shouldAutoStartScanner) {
            checkPermissionAndStartScanning()
        }
    }

    companion object {
        var shouldAutoStartScanner = true
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}