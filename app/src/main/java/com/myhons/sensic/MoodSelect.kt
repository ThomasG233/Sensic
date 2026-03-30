package com.myhons.sensic

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class MoodSelect : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var btnHappy: Button
    private lateinit var btnSad: Button
    private lateinit var btnAnxious: Button
    private lateinit var btnAngry: Button
    private lateinit var btnEnergised: Button
    private lateinit var btnExhausted: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadTheme()
        enableEdgeToEdge()
        setContentView(R.layout.activity_select_mood)

        btnHappy = findViewById(R.id.btnHappy)
        btnSad = findViewById(R.id.btnSad)
        btnAnxious = findViewById(R.id.btnAnxious)
        btnAngry = findViewById(R.id.btnAngry)
        btnEnergised = findViewById(R.id.btnEnergised)
        btnExhausted = findViewById(R.id.btnExhausted)

        btnHappy.setOnClickListener {
            loadMainScreen()
        }

        btnSad.setOnClickListener {
            loadMainScreen()
        }

        btnAnxious.setOnClickListener {
            loadMainScreen()
        }

        btnAngry.setOnClickListener {
            loadMainScreen()
        }

        btnEnergised.setOnClickListener {
            loadMainScreen()
        }

        btnExhausted.setOnClickListener {
            loadMainScreen()
        }



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadMainScreen() {
        // Set all contexts before using the app.
        ContextsHandler.initialiseContexts(applicationContext)
        val intent = Intent(this, MainScreen::class.java)
        startActivity(intent)
        finish()
    }

    private fun loadTheme()
    {
        sharedPreferences = getSharedPreferences("darkTheme", MODE_PRIVATE)
        if(sharedPreferences.getBoolean("darkTheme", false))
        {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        else
        {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}