package com.myhons.sensic

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.net.toUri
import com.google.android.gms.location.ActivityRecognition

/**
 * Main Menu.
 */
class MainScreen : AppCompatActivity() {

    // Late initialise the UI elements.
    private lateinit var btnStart: Button
    private lateinit var btnSettings : Button
    private lateinit var btnContexts: Button
    private lateinit var btnAbout : Button

    private lateinit var activityIntent : PendingIntent
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_screen)
        // Receive updates on the users current activities. Any changes are notified to the broadcast receiver.
        activityIntent = PendingIntent.getBroadcast(this, 0, Intent(this, ActivityCallback::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        // Initialise all UI elements.
        btnStart = findViewById(R.id.btnStart)
        btnSettings = findViewById(R.id.btnSettings)
        btnContexts = findViewById(R.id.btnContexts)
        btnAbout = findViewById(R.id.btnAbout)
        btnStart.setOnClickListener {
            // Checks if the user is authenticated.
            val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
            if(sharedPreferences.getString("accessToken", "") == "")
            {
                // Authenticate, if not already authenticated.
                val clientID = PKCEHandler.getClientID()
                val redirectUri = PKCEHandler.getRedirectUri()
                val challenge = PKCEHandler.getCodeChallenge(64)
                // Redirect the user to the authentication page.
                val authenticateIntent = Intent(Intent.ACTION_VIEW,
                    ("https://accounts.spotify.com/authorize?response_type=code&client_id=$clientID" +
                            "&scope=playlist-modify-public playlist-modify-private " +
                            "&code_challenge_method=S256" +
                            "&code_challenge=$challenge" +
                            "&redirect_uri=$redirectUri").toUri())
                startActivity(authenticateIntent)
            }
            else
            {
                // Authenticated, we can generate recommendations.
                val intent = Intent(this, GenerateRecommendations::class.java)
                startActivity(intent)
            }

        }
        // Open the settings screen.
        btnSettings.setOnClickListener {
            val intent = Intent(this, AppSettings::class.java)
            startActivity(intent)
        }
        // Open the contexts menu.
        btnContexts.setOnClickListener {
            val intent = Intent(this, ContextsMenu::class.java)
            startActivity(intent)
        }
        // Open the about page.
        btnAbout.setOnClickListener {
            val intent = Intent(this, AppInformation::class.java)
            startActivity(intent)
        }
        // Receive updates on the activity.
        getCurrentActivity()
        // Get permissions from the user.
        checkLocationPermissions()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    /**
     * Checks the result from the permissions request made to the user.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 102)
        {
            // If permissions have been provided for the activity.
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                getCurrentActivity()
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    /**
     * Start requesting updates on the user's activities.
     */
    private fun getCurrentActivity() {
        // If permissions have not been provided for checking activity.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
        {
            // Request permissions for the physical activity services.
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 102)
        }
        else
        {
            // Register for activity updates every 20 seconds.
            ActivityRecognition.getClient(this).requestActivityUpdates(20000, activityIntent)
        }
    }

    /**
     * Check if the user has provided location permissions.
     */
    private fun checkLocationPermissions()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // Make a request for location permissions if they have not been provided.
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
        }
    }
}