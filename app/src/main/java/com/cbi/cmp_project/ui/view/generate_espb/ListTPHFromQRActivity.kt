package com.cbi.cmp_project.ui.view.generate_espb

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cbi.cmp_project.R

class ListTPHFromQRActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_list_tphfrom_qractivity)
        val qrResult = intent.getStringExtra(EXTRA_QR_RESULT) ?: ""
        findViewById<TextView>(R.id.textViewResult).text = qrResult

        // Handle scan again button click
        findViewById<Button>(R.id.buttonScanAgain).setOnClickListener {
            finish()  // This will return to MainActivity
        }
    }

    companion object {
        const val EXTRA_QR_RESULT = "scannedResult"
    }
}