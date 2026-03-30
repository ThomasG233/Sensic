package com.myhons.sensic

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.TimePicker
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible

/**
 * Activity called when the user wishes to edit the preference list for a context.
 */
class AdjustContext : AppCompatActivity() {

    private lateinit var btnConfirm : Button
    private lateinit var btnGetLocation : Button
    private lateinit var btnBack : ImageButton
    private lateinit var tvDescriptor : TextView
    private lateinit var tvTitle : TextView
    private lateinit var genres : ConstraintLayout
    private lateinit var viewTime : ConstraintLayout

    private lateinit var timePickerStart : TimePicker
    private lateinit var timePickerEnd : TimePicker

    private var genresSelected = 0
    private var preferencesEnabled = true
    var coordinates = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == RESULT_OK)
        {
            val returnData = result.data as Intent
            val latitude = returnData.getDoubleExtra("latitude", 0.0)
            val longitude = returnData.getDoubleExtra("longitude", 0.0)
            val coordinates = arrayOf(latitude, longitude)
            Log.d("LOCATION", "Returned Coordinates: ${coordinates[0]}, ${coordinates[1]}")
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

        btnGetLocation.setOnClickListener {
            val intent = Intent(this, SelectLocation::class.java)
            coordinates.launch(intent)
        }
            tvTitle.text = name
            when (type) {
                "Weather" -> tvDescriptor.text =
                    "What genres do you want to play when it's ${name.toString().lowercase()}?"
                "Time" -> {
                    tvDescriptor.text = "What genres do you want to be playing?"
                    viewTime.isVisible = true

                    val startTime = ContextsHandler.getTimeContext().getStartTime()
                    val endTime = ContextsHandler.getTimeContext().getEndTime()

                    timePickerStart.hour = startTime.getHour()
                    timePickerStart.minute = startTime.getMinute()

                    timePickerEnd.hour = endTime.getHour()
                    timePickerEnd.minute = endTime.getMinute()

                    btnConfirm.setOnClickListener {
                        if (checkTimeValidity()) {
                            ContextsHandler.getTimeContext().setStartTime(timePickerStart.hour, timePickerStart.minute)
                            ContextsHandler.getTimeContext().setEndTime(timePickerEnd.hour, timePickerEnd.minute)
                            saveUserPreferences("Time", "Time")
                            finish()
                        }
                    }
                }

                "Location" -> {
                    tvDescriptor.text = "What should be playing when you're at this location?"
                    btnGetLocation.isVisible = true
                }

                else -> {
                    tvDescriptor.text = "What genres do you want to play when you're ${name.toString().lowercase()}?"
                    btnConfirm.setOnClickListener {
                        saveUserPreferences(name as String, type as String)
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

            loadUserPreferences(name as String, type as String)

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
    private fun saveUserPreferences(name : String, type : String) : Boolean
    {
        val userPreferences = mutableMapOf<String, Boolean>()
        for(checkbox in genres.children)
        {
            val genre = checkbox as CheckBox
            userPreferences[genre.text as String] = genre.isChecked
        }
        ContextsHandler.setContextPreferences(name, type, userPreferences)
        return if(type != "Location" && type != "Time")
        {
            ContextsHandler.saveToFile(name, ContextsHandler.getContext<MusicContext>(name, type), applicationContext)
        }
        else if(type == "Time")
        {
            ContextsHandler.saveToFile(name, ContextsHandler.getContext<TimeContext>(name, type), applicationContext)
        }
        else
        {
            ContextsHandler.saveToFile(name, ContextsHandler.getContext<LocationContext>(name, type), applicationContext)
        }
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