package com.cbi.mobile_plantation.ui.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.ui.view.ListTPHApproval.Companion.EXTRA_QR_RESULT
import com.cbi.mobile_plantation.ui.view.panenTBS.ListPanenTBSActivity
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class ScanQR : AppCompatActivity() {
    private lateinit var barcodeView: DecoratedBarcodeView

    var menuString = ""

    // Add variables to store previous TPH data
    private var previousTph1 = ""
    private var previousTph0 = ""
    private var previousTph1IdPanen = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_espb)
        menuString = intent.getStringExtra("FEATURE_NAME").toString()
        // Get previous TPH data if available
        previousTph1 = intent.getStringExtra("tph_1") ?: ""
        previousTph0 = intent.getStringExtra("tph_0") ?: ""
        previousTph1IdPanen = intent.getStringExtra("tph_1_id_panen") ?: ""

        Log.d("ScanQR", "Previous tph1: $previousTph1")
        Log.d("ScanQR", "Previous tph0: $previousTph0")
        Log.d("ScanQR", "Previous tph1IdPanen: $previousTph1IdPanen")

        barcodeView = findViewById(R.id.barcode_scanner)
        barcodeView.findViewById<TextView>(com.google.zxing.client.android.R.id.zxing_status_view)?.visibility = View.GONE

        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.text?.let { qrCodeValue ->
                    handleQRCode(qrCodeValue)
                    barcodeView.pause() // ✅ Stop scanning after first scan
                }
            }

            override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
        })

        barcodeView.resume() // ✅ Start scanning
    }

    private fun handleQRCode(result: String) {
        var intent = Intent(this, ListTPHApproval::class.java).apply {
            putExtra("FEATURE_NAME", menuString)
            putExtra(EXTRA_QR_RESULT, result)
        }

        when (menuString) {
            "Buat eSPB" -> intent = Intent(this, ListPanenTBSActivity::class.java).apply {
                putExtra("scannedResult", result)
                Log.d("scannedResult", "result: $result")
                putExtra("FEATURE_NAME", menuString)

                // Pass previous TPH data if available
                if (previousTph1.isNotEmpty()) {
                    putExtra("previous_tph_1", previousTph1)
                }
                if (previousTph0.isNotEmpty()) {
                    putExtra("previous_tph_0", previousTph0)
                }
                if (previousTph1IdPanen.isNotEmpty()) {
                    putExtra("previous_tph_1_id_panen", previousTph1IdPanen)
                }
                Log.d("ListPanenTBSActivityPassData", "ScanQR previous_tph_1: $previousTph1")
                Log.d("ListPanenTBSActivityPassData", "ScanQR previous_tph_0: $previousTph0")
                Log.d("ListPanenTBSActivityPassData", "ScanQR previous_tph_1_id_panen: $previousTph1IdPanen")
                Log.d("ListPanenTBSActivityPassData", "ScanQR FEATURE_NAME: $menuString")
            }
        }

        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume() // ✅ Resume scanning when back to activity
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause() // ✅ Pause scanning when activity is not visible
    }

    // ✅ Override System Back Button
    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, HomePageActivity::class.java) // Change this to your desired activity
        startActivity(intent)
        finish()
    }
}