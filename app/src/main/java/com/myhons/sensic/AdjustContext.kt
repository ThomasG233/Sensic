package com.myhons.sensic

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TimePicker
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import com.google.android.gms.location.ActivityTransition

/**
 * Activity called when the user wishes to edit the preference list for a context.
 */
class AdjustContext : AppCompatActivity() {



    val transitions = mutableListOf<ActivityTransition>()
    private lateinit var btnConfirm : Button
    private lateinit var btnGetLocation : Button
    private lateinit var btnBack : ImageButton
    private lateinit var tvDescriptor : TextView
    private lateinit var tvTitle : TextView
    private lateinit var genres : LinearLayout
    private lateinit var viewTime : ConstraintLayout

    private lateinit var timePickerStart : TimePicker
    private lateinit var timePickerEnd : TimePicker

    private var genresSelected = 0
    private var preferencesEnabled = true



    private var locationCoordinates = Coordinates(0.0, 0.0)
    private var locationName = "this location"
    var coordsFromMap = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == RESULT_OK)
        {
            val returnData = result.data as Intent
            val latitude = returnData.getDoubleExtra("latitude", 0.0)
            val longitude = returnData.getDoubleExtra("longitude", 0.0)
            val locationLabel = returnData.getStringExtra("name")
            locationCoordinates = Coordinates(latitude, longitude)
            locationName = locationLabel.toString()
            tvDescriptor.text = "What should be playing when you're at $locationName?"
            Log.d("LOCATION", "Returned Coordinates: $latitude, $longitude")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_adjust_context)

        val type = intent.getStringExtra("type") as String
        val name = intent.getStringExtra("name") as String

        btnConfirm = findViewById(R.id.btnConfirm)
        btnGetLocation = findViewById(R.id.btnGetLocation)

        viewTime = findViewById(R.id.viewTimeOptions)
        genres = findViewById(R.id.genres)
        btnBack = findViewById(R.id.btnBack)
        tvDescriptor = findViewById(R.id.tvDescriptor)
        tvTitle = findViewById(R.id.tvTitle)

        timePickerStart = findViewById(R.id.timePickerStart)
        timePickerEnd = findViewById(R.id.timePickerEnd)

        timePickerStart.setIs24HourView(true)
        timePickerEnd.setIs24HourView(true)



            tvTitle.text = name
            when (type) {
                "Weather" -> {
                    tvDescriptor.text = "What genres do you want to play when it's ${name.lowercase()}?"
                    btnConfirm.setOnClickListener {
                        saveUserPreferences<MusicContext>(name, type)
                        finish()
                    }
                }
                "Time" -> {
                    tvDescriptor.text = "What genres do you want to be playing?"
                    viewTime.isVisible = true

                    val startTime = ContextsHandler.getContext<TimeContext>(name, type).getStartTime()
                    val endTime = ContextsHandler.getContext<TimeContext>(name, type).getEndTime()

                    timePickerStart.hour = startTime.getHour()
                    timePickerStart.minute = startTime.getMinute()

                    timePickerEnd.hour = endTime.getHour()
                    timePickerEnd.minute = endTime.getMinute()

                    btnConfirm.setOnClickListener {
                        if (checkTimeValidity()) {
                            ContextsHandler.getContext<TimeContext>(name, type).setStartTime(timePickerStart.hour, timePickerStart.minute)
                            ContextsHandler.getContext<TimeContext>(name, type).setEndTime(timePickerEnd.hour, timePickerEnd.minute)
                            saveUserPreferences<TimeContext>("Time", "Time")
                            finish()
                        }
                    }
                }

                "Location" -> {
                    locationCoordinates = ContextsHandler.getContext<LocationContext>(name, type).getCoordinates()
                    val nameToDisplay = ContextsHandler.getContext<LocationContext>(name, type).getName()
                    if(nameToDisplay != "" && !nameToDisplay.contains("Location"))
                    {
                        locationName = nameToDisplay
                    }
                    tvDescriptor.text = "What should be playing when you're at $locationName?"
                    btnGetLocation.isVisible = true

                    btnGetLocation.setOnClickListener {
                        val intent = Intent(this, SelectLocation::class.java)
                        intent.putExtra("Location", name)
                        intent.putExtra("latitude", locationCoordinates.getLatitude())
                        intent.putExtra("longitude", locationCoordinates.getLongitude())
                        coordsFromMap.launch(intent)
                    }
                    btnConfirm.setOnClickListener {
                        ContextsHandler.getContext<LocationContext>(name, type).setCoordinates(locationCoordinates)
                        ContextsHandler.getContext<LocationContext>(name, type).setName(locationName)
                        saveUserPreferences<LocationContext>(name, type)
                        intent.putExtra("name", locationName)
                        intent.putExtra("locationLetter", name)
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                }

                else -> {
                    tvDescriptor.text = "What genres do you want to play when you're ${name.lowercase()}?"
                    btnConfirm.setOnClickListener {
                        saveUserPreferences<MusicContext>(name, type)
                        finish()
                    }
                }
            }

            for (option in genres.children) {
                // Necessary call as the option cannot be modified unless directly specified as a CheckBox.
                val genre = option as CheckBox
                genre.setOnClickListener {
                    if (genre.isChecked)
                    {
                        genresSelected++
                    }
                    else
                    {
                        genresSelected--
                    }
                    updateOptions()

                    Log.e("Selected", genresSelected.toString())
                }
            }

            btnBack.setOnClickListener {
                finish()
            }

            loadUserPreferences(name, type)

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

    private fun checkTimeValidity(): Boolean {
        if (timePickerStart.hour < timePickerEnd.hour || (timePickerStart.hour == timePickerEnd.hour && timePickerStart.minute < timePickerEnd.minute))
        {
            Log.d("TimeCheck", "Valid")
            return true
        }
        else
        {
            Log.e("TimeCheck", "Invalid")
            return false
        }
    }
    private fun <T> saveUserPreferences(name : String, type : String) : Boolean
    {
        val userPreferences = mutableMapOf<String, Boolean>()
        for(checkbox in genres.children)
        {
            val genre = checkbox as CheckBox
            userPreferences[genre.text as String] = genre.isChecked
        }
        ContextsHandler.setContextPreferences(name, type, userPreferences)
        return ContextsHandler.saveToFile<T>(name, ContextsHandler.getContext(name, type), applicationContext)
    }

    private fun loadUserPreferences(name : String, type : String)
    {
        // Returns if the context could not be loaded. This leaves everything unchecked.
        val context : MusicContext = ContextsHandler.getContext(name, type)
        val preferences = context.getPreferenceList()
        for(checkbox in genres.children)
        {
            val genre = checkbox as CheckBox
            if(preferences[genre.text as String] == true)
            {
                genre.isChecked = true
                genresSelected++
            }
        }
        updateOptions()
    }

    private fun updateOptions() {
        if((genresSelected == 5 && preferencesEnabled) || (genresSelected == 4 && !preferencesEnabled))
        {
            for (option in genres.children) {
                val genre = option as CheckBox
                if (!genre.isChecked) {
                    genre.setEnabled(!preferencesEnabled)
                }
            }
            preferencesEnabled = !preferencesEnabled
        }
    }
}