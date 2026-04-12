package com.myhons.sensic

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.maps.model.LatLng
import kotlin.coroutines.resume

class MainScreen : AppCompatActivity() {

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

        activityIntent = PendingIntent.getBroadcast(this, 0, Intent(this, ActivityCallback::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)


        btnStart = findViewById(R.id.btnStart)
        btnSettings = findViewById(R.id.btnSettings)
        btnContexts = findViewById(R.id.btnContexts)
        btnAbout = findViewById(R.id.btnAbout)
        btnStart.setOnClickListener {
            val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
            // Authenticate, if not already authenticated.
            if(sharedPreferences.getString("accessToken", "") == "")
            {
                val clientID = PKCEHandler.getClientID()
                val redirectUri = PKCEHandler.getRedirectUri()
                val challenge = PKCEHandler.getCodeChallenge(64)

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
                val intent = Intent(this, GenerateRecommendations::class.java)
                startActivity(intent)
            }

        }

        btnSettings.setOnClickListener {
            val intent = Intent(this, AppSettings::class.java)
            startActivity(intent)
        }
        btnContexts.setOnClickListener {
            val intent = Intent(this, ContextsMenu::class.java)
            startActivity(intent)
        }

        btnAbout.setOnClickListener {
            val intent = Intent(this, AppInformation::class.java)
            startActivity(intent)
        }

        Log.d("Permissions", ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION).toString())

        getCurrentActivity()
        checkLocationPermissions()


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 102)
        {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                getCurrentActivity()
            }
        }
    }


    // https://developer.android.com/develop/sensors-and-location/location/transitions#kotlin
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getCurrentActivity() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 102)
        }
        else
        {
            // Register for activity updates every 10 seconds.
            ActivityRecognition.getClient(this).requestActivityUpdates(10000, activityIntent)
        }
    }

    private fun checkLocationPermissions()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
        }
    }
}