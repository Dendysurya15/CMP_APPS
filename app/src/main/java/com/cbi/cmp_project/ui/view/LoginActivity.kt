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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("LoginActivity", "onCreate started")

//        loadingDialog = LoadingDialog(this)

        binding.btnLoginSubmit.setOnClickListener {
            Log.d("LoginActivity", "Button clicked!")
//            showLoading()
//
//            lifecycleScope.launch {
//                delay(5000)
//                hideLoading()
//            }
        }

    }

    private fun showLoading() {
        loadingDialog.show()
    }

    private fun hideLoading() {
        loadingDialog.dismiss()
    }
}