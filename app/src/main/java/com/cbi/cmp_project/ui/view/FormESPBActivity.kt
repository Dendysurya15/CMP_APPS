package com.cbi.cmp_project.ui.view

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import com.cbi.cmp_project.R
import com.cbi.cmp_project.utils.AppUtils

class FormESPBActivity : AppCompatActivity() {
    var featureName = ""
    var checkedResult = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_form_espbactivity)
        setupHeader()

        val formTransporter = findViewById<LinearLayout>(R.id.formEspbTransporter)
        val etTransporter = formTransporter.findViewById<EditText>(R.id.etPaneEt)

    }

    private fun setupHeader() {
        featureName = intent.getStringExtra("FEATURE_NAME").toString()
        checkedResult = intent.getStringExtra("CHECKED_RESULT").toString()

        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        AppUtils.setupFeatureHeader(featureName, tvFeatureName)
    }
}