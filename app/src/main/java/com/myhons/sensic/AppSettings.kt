package com.myhons.sensic

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.edit
import kotlin.apply

class AppSettings : AppCompatActivity() {

    private lateinit var btnBack : ImageButton
    private lateinit var btnResetContexts : Button
    private lateinit var btnUnlink : Button
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var switchTheme : Switch
    private val backPressed = object : OnBackPressedCallback(false)
    {
        override fun handleOnBackPressed() {
            saveSettings()
            finish()
        }
    }
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)


        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        onBackPressedDispatcher.addCallback(this, backPressed)
        backPressed.isEnabled = true

        btnBack = findViewById(R.id.btnBack)
        btnResetContexts = findViewById(R.id.btnResetContexts)
        btnUnlink = findViewById(R.id.btnUnlink)
        switchTheme = findViewById(R.id.switchTheme)

        if(sharedPreferences.getBoolean("darkTheme", false))
        {
            switchTheme.isChecked = true
        }
        btnBack.setOnClickListener {
            finish()
        }

        btnUnlink.setOnClickListener {
            sharedPreferences.edit{
                apply()
                {
                    putString("accessToken", "")
                    putString("refreshToken", "")
                }
            }
            Toast.makeText(applicationContext, "Unlinked from Spotify.", Toast.LENGTH_SHORT).show()
        }

        btnResetContexts.setOnClickListener {
            ContextsHandler.resetAllToDefault(applicationContext)
            Toast.makeText(applicationContext, "All contexts reset to default.", Toast.LENGTH_SHORT).show()
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
            saveSettings()
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
    }
}