package com.cbi.cmp_project.ui.view

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.cbi.cmp_project.R
import com.cbi.cmp_project.utils.AppUtils

class FormESPBActivity : AppCompatActivity() {
    var featureName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_form_espbactivity)
        setupHeader()
    }

    private fun setupHeader() {
        featureName = intent.getStringExtra("FEATURE_NAME").toString()
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        AppUtils.setupFeatureHeader(featureName, tvFeatureName)
    }
}