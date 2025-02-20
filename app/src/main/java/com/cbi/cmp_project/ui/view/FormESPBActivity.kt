package com.cbi.cmp_project.ui.view

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.cbi.cmp_project.R
import com.cbi.cmp_project.ui.view.panenTBS.ListPanenTBSActivity
import com.cbi.cmp_project.utils.AppUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormESPBActivity : AppCompatActivity() {
    var featureName = ""
    var tph0 = ""
    var tph1 = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_form_espbactivity)
        featureName = intent.getStringExtra("FEATURE_NAME").toString()
        tph0 = intent.getStringExtra("tph_0").toString()
        tph1 = intent.getStringExtra("tph_1").toString()

        setupHeader()
        //NBM 115
        //transporter 1
        val formEspbNopol = findViewById<LinearLayout>(R.id.formEspbNopol)
        val tvEspbNopol = formEspbNopol.findViewById<TextView>(R.id.tvTitlePaneEt)
        tvEspbNopol.text = "No. Polisi"
        val etEspbNopol = formEspbNopol.findViewById<EditText>(R.id.etPaneEt)
        val formEspbDriver = findViewById<LinearLayout>(R.id.formEspbDriver)
        val tvEspbDriver = formEspbDriver.findViewById<TextView>(R.id.tvTitlePaneEt)
        tvEspbDriver.text = "Driver"
        val etEspbDriver = formEspbDriver.findViewById<EditText>(R.id.etPaneEt)
        val formEspbTransporter = findViewById<LinearLayout>(R.id.formEspbTransporter)
        val tvEspbTransporter = formEspbTransporter.findViewById<TextView>(R.id.tvTitlePaneEt)
        tvEspbTransporter.text = "Transporter"
        val etTransporter = formEspbTransporter.findViewById<EditText>(R.id.etPaneEt)

        var blok_jjg =  "12356,312;12357,154;12358,321;12359,215;12360,421;12361,233"
        val btnGenerateQRESPB = findViewById<FloatingActionButton>(R.id.btnGenerateQRESPB)
        btnGenerateQRESPB.setOnClickListener {
            val json = "{" +
                    "\"espb\": {" +
                    "\"blok_jjg\": \"$blok_jjg\"," +
                    "\"nopol\": \"${etEspbNopol.text}\","+
                    "\"driver\": \"${etEspbDriver.text}\","+
                    "\"pemuat\": 1,"+
                    "\"mill_id\": 115,"+
                    "\"created_by_id\": 1,"+
                    "\"no_espb\": \"SSS-SLE/OC/02/01/25/140012000\","+
                    "\"created_at\": \"${getCurrentDateTime()}\""+
                    "},"+
                    "\"tph_0\": \"$tph0\","+
                    "\"tph_1\": \"$tph1\"}"
            val encodedData = ListPanenTBSActivity().encodeJsonToBase64ZipQR(json)
            val qrCodeImageView: ImageView = findViewById(R.id.qrCodeImageViewESPB)

            ListPanenTBSActivity().generateHighQualityQRCode(encodedData!!, qrCodeImageView)
        }

    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun setupHeader() {
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        AppUtils.setupFeatureHeader(featureName, tvFeatureName)
    }
}