package com.cbi.cmp_project.ui.view

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cbi.cmp_project.R
import com.cbi.cmp_project.utils.AppUtils

class ListTPHFromQRActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_tphfrom_qractivity)
        val qrResult = intent.getStringExtra(EXTRA_QR_RESULT) ?: ""
        val jsonStr = AppUtils.readJsonFromEncryptedBase64Zip(qrResult)
        findViewById<TextView>(R.id.textViewResult).text = jsonStr
        Toast.makeText(this, jsonStr, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_QR_RESULT = "scannedResult"
    }
}