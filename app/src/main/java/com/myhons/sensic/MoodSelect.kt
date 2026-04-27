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

/**
 * Allows the user to select their current mood on start-up.
 */
class MoodSelect : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    // Late initialise all UI elements.
    private lateinit var btnHappy: Button
    private lateinit var btnSad: Button
    private lateinit var btnAnxious: Button
    private lateinit var btnAngry: Button
    private lateinit var btnEnergetic: Button
    private lateinit var btnExhausted: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the dark theme, if enabled.
        loadTheme()
        enableEdgeToEdge()
        setContentView(R.layout.activity_select_mood)
        // Initialise all UI elements.
        btnHappy = findViewById(R.id.btnHappy)
        btnSad = findViewById(R.id.btnSad)
        btnAnxious = findViewById(R.id.btnAnxious)
        btnAngry = findViewById(R.id.btnAngry)
        btnEnergetic = findViewById(R.id.btnEnergetic)
        btnExhausted = findViewById(R.id.btnExhausted)

        val btnArray = arrayOf(btnHappy, btnSad, btnAnxious, btnAngry, btnEnergetic, btnExhausted)
        // Load the main screen with each mood selected.
        for(btn in btnArray)
        {
            btn.setOnClickListener {
                loadMainScreen(btn.text.toString())
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Load the Main Menu.
     * @param mood: The current mood of the user.
     */
    private fun loadMainScreen(mood : String) {
        // Set up all contexts before using the app.
        ContextsHandler.initialiseContexts(applicationContext, mood)
        // Load the main screen.
        val intent = Intent(this, MainScreen::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * Load the UI theme.
     */
    private fun loadTheme()
    {
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        if(sharedPreferences.getBoolean("darkTheme", false))
        {
            // Enable the dark theme.
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        else
        {
            // Use the default theme.
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}