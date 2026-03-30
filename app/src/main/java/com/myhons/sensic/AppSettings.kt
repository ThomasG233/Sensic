package com.myhons.sensic

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Switch
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.edit

class AppSettings : AppCompatActivity() {

    private lateinit var btnBack : ImageButton
    private lateinit var switchTheme : Switch
    private val backPressed = object : OnBackPressedCallback(false)
    {
        override fun handleOnBackPressed() {
            saveSettings()
        }
    }
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)


        sharedPreferences = getSharedPreferences("darkTheme", MODE_PRIVATE)
        onBackPressedDispatcher.addCallback(this, backPressed)
        backPressed.isEnabled = true

        btnBack = findViewById(R.id.btnBack)
        switchTheme = findViewById(R.id.switchTheme)

        if(sharedPreferences.getBoolean("darkTheme", false))
        {
            switchTheme.isChecked = true
        }
        btnBack.setOnClickListener {
            saveSettings()
        }
        switchTheme.setOnCheckedChangeListener { button, bool ->
            if(button.isChecked)
            {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            else
            {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun saveSettings()
    {
        sharedPreferences.edit {
            apply()
            {
                putBoolean("darkTheme", switchTheme.isChecked)
            }
        }
        finish()
    }
}