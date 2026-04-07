package com.myhons.sensic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.google.android.gms.maps.model.LatLng
import kotlin.coroutines.resume

class MainScreen : AppCompatActivity() {

    private lateinit var btnStart: Button
    private lateinit var btnAuth: Button
    private lateinit var btnSettings : Button
    private lateinit var btnContexts: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_screen)

        btnStart = findViewById(R.id.btnStart)
        btnAuth = findViewById(R.id.btnAuth)
        btnSettings = findViewById(R.id.btnSettings)
        btnContexts = findViewById(R.id.btnContexts)
        btnStart.setOnClickListener {
            val intent = Intent(this, GenerateRecommendations::class.java)
            startActivity(intent)
        }
        btnAuth.setOnClickListener {

            val clientID = PKCEHandler.getClientID()
            val redirectUri = PKCEHandler.getRedirectUri()
            val challenge = PKCEHandler.getCodeChallenge(64)

            val authenticateIntent = Intent(Intent.ACTION_VIEW,
                ("https://accounts.spotify.com/authorize?response_type=code&client_id=$clientID" +
                        "&code_challenge_method=S256" +
                        "&code_challenge=$challenge" +
                        "&redirect_uri=$redirectUri").toUri())
            startActivity(authenticateIntent)
        }
        btnSettings.setOnClickListener {
            val intent = Intent(this, AppSettings::class.java)
            startActivity(intent)
        }
        btnContexts.setOnClickListener {
            val intent = Intent(this, ContextsMenu::class.java)
            startActivity(intent)
        }
        /*
        if(getSharedPreferences("settings", MODE_PRIVATE).getString("accessToken", "") == "")
        {
            btnStart.isVisible = false
        }
        else
        {
            btnAuth.isVisible = false
        }
         */

        checkLocationPermissions()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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