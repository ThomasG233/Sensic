package com.myhons.sensic

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Displays information about the app.
 */
class AppInformation : AppCompatActivity() {

    // Initialise the only UI element on this screen.
    private lateinit var btnBackToMenu : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_app_information)

        // Initialise the back button.
        btnBackToMenu = findViewById(R.id.btnBackToMenu)
        btnBackToMenu.setOnClickListener {
            // Return to main menu when clicked.
            finish()
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}