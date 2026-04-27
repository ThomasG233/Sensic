package com.myhons.sensic

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

/**
 * Screen for the App's Settings.
 */
class AppSettings : AppCompatActivity() {

    // Late initialise all UI elements.
    private lateinit var btnBack : ImageButton
    private lateinit var btnResetContexts : Button
    private lateinit var btnUnlink : Button

    private lateinit var switchTheme : Switch
    private lateinit var switchPrivacy : Switch
    // Override the back button being pressed on the phone so that settings get saved.
    private val backPressed = object : OnBackPressedCallback(false)
    {
        override fun handleOnBackPressed() {
            // Save and close.
            saveSettings()
            finish()
        }
    }
    // Used to get the settings saved.
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        // Gets the app settings from the shared preferences.
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        // Update the back button to the new callback.
        onBackPressedDispatcher.addCallback(this, backPressed)
        backPressed.isEnabled = true
        // Initialise all UI elements.
        btnBack = findViewById(R.id.btnBack)
        btnResetContexts = findViewById(R.id.btnResetContexts)
        btnUnlink = findViewById(R.id.btnUnlink)
        switchTheme = findViewById(R.id.switchTheme)
        switchPrivacy = findViewById(R.id.switchPrivacy)
        // If the dark theme is on, set the switch to be checked.
        if(sharedPreferences.getBoolean("darkTheme", false))
        {
            switchTheme.isChecked = true

        }
        // If the location should be hidden in playlist names, set the switch to be checked.
        if(sharedPreferences.getBoolean("hideLocation", true))
        {
            switchPrivacy.isChecked = true
        }
        // Save settings and close if the back button is pressed.
        btnBack.setOnClickListener {
            saveSettings()
            finish()
        }

        btnUnlink.setOnClickListener {
            // Clear the access and refresh tokens.
            sharedPreferences.edit{
                apply()
                {
                    putString("accessToken", "")
                    putString("refreshToken", "")
                }
            }
            // Inform the user they have unlinked.
            Toast.makeText(applicationContext, "Unlinked from Spotify.", Toast.LENGTH_SHORT).show()
        }
        // Reset the contexts to their default state in the ContextsHandler.
        btnResetContexts.setOnClickListener {
            ContextsHandler.resetAllToDefault(applicationContext)
            // Inform the user the contexts have been reset.
            Toast.makeText(applicationContext, "All contexts reset to default.", Toast.LENGTH_SHORT).show()
        }
        // Set the night mode depending on if the switch is enabled or not.
        switchTheme.setOnCheckedChangeListener { button, _ ->
            if(button.isChecked)
            {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            else
            {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            // Save the settings.
            saveSettings()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Saves all settings with SharedPreferences.
     */
    private fun saveSettings()
    {
        sharedPreferences.edit {
            apply()
            {
                // Saves the darkTheme and hideLocation values.
                putBoolean("darkTheme", switchTheme.isChecked)
                putBoolean("hideLocation", switchPrivacy.isChecked)
            }
        }
    }
}