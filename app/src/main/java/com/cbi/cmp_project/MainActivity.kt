package com.cbi.cmp_project

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var showingSplash = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash_screen)

        // Start a coroutine to handle splash screen timing
        lifecycleScope.launch {
            delay(2000) // Wait for 2 seconds
            showMainContent()
        }
    }

    private fun showMainContent() {
        if (!showingSplash) return
        showingSplash = false

        // Switch to main layout
        setContentView(R.layout.activity_login)


    }
}