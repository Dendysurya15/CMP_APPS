package com.cbi.cmp_project.ui.view

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.cbi.cmp_project.R
import com.cbi.cmp_project.databinding.ActivityLoginBinding
import com.cbi.cmp_project.utils.LoadingDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        loadingDialog = LoadingDialog(this)

        // Set click listener for login button
        val loginButton = findViewById<MaterialButton>(R.id.btn_login_submit)

        loginButton.setOnClickListener { view ->

            showLoading()

            lifecycleScope.launch {
                delay(3500)
                hideLoading()
            }
        }

    }

    private fun showLoading() {
        loadingDialog.show()
    }

    private fun hideLoading() {
        loadingDialog.dismiss()
    }
}