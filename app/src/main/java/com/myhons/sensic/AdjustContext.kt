package com.myhons.sensic

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible

/**
 * Activity used when the user wishes to edit the preference list for a context.
 */
class AdjustContext : AppCompatActivity() {

    // Late initialise all UI elements.
    private lateinit var btnConfirm : Button
    private lateinit var btnGetLocation : Button
    private lateinit var btnBack : ImageButton
    private lateinit var tvDescriptor : TextView
    private lateinit var tvTitle : TextView
    private lateinit var genres : LinearLayout
    private lateinit var viewTime : ConstraintLayout

    private lateinit var timePickerStart : TimePicker
    private lateinit var timePickerEnd : TimePicker

    // Tracks the number of genres selected by the app.
    private var genresSelected = 0
    // Enables/disables the genre list depending on the amount selected.
    private var preferencesEnabled = true
    // Represents version of the location, local to this activity.
    private var locationCoordinates = Coordinates(0.0, 0.0)
    private var locationName = "this location"
    // Used to collect the coordinates from the SelectLocation activity.
    var coordsFromMap = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // If coordinates were returned.
        if(result.resultCode == RESULT_OK)
        {
            val returnData = result.data as Intent
            // Get the location data from the result.
            val latitude = returnData.getDoubleExtra("latitude", 0.0)
            val longitude = returnData.getDoubleExtra("longitude", 0.0)
            val locationLabel = returnData.getStringExtra("name")
            // Save the location data returned.
            locationCoordinates = Coordinates(latitude, longitude)
            locationName = locationLabel.toString()
            tvDescriptor.text = "What should be playing when you're at $locationName?"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_adjust_context)
        // Saves the context name and type received from AdjustContext.
        val type = intent.getStringExtra("type") as String
        val name = intent.getStringExtra("name") as String

        // Initialise all UI elements.
        btnConfirm = findViewById(R.id.btnConfirm)
        btnGetLocation = findViewById(R.id.btnGetLocation)
        viewTime = findViewById(R.id.viewTimeOptions)
        genres = findViewById(R.id.genres)
        btnBack = findViewById(R.id.btnBack)
        tvDescriptor = findViewById(R.id.tvDescriptor)
        tvTitle = findViewById(R.id.tvTitle)
        timePickerStart = findViewById(R.id.timePickerStart)
        timePickerEnd = findViewById(R.id.timePickerEnd)
        // Set time pickers to appear as intended.
        timePickerStart.setIs24HourView(true)
        timePickerEnd.setIs24HourView(true)

        // Set the title of the screen to the name of the context.
        tvTitle.text = name
        when (type) {
            "Weather" -> {
                // Set the descriptor to have the correct wording for a weather context.
                tvDescriptor.text = "What genres do you want to play when it's ${name.lowercase()}?"
                // Save the context when the user confirms, then close this screen.
                btnConfirm.setOnClickListener {
                    saveUserPreferences<MusicContext>(name, type)
                    finish()
                }
            }
            "Time" -> {
                // Set the descriptor to have the correct wording for a time context.
                tvDescriptor.text = "What genres do you want to be playing?"
                // Displays the time pickers which are otherwise hidden from view.
                viewTime.isVisible = true
                // Get the saved start/end times.
                val startTime = ContextsHandler.getContext<TimeContext>(name, type).getStartTime()
                val endTime = ContextsHandler.getContext<TimeContext>(name, type).getEndTime()
                // Set the times to reflect the saved data.
                timePickerStart.hour = startTime.getHour()
                timePickerStart.minute = startTime.getMinute()
                timePickerEnd.hour = endTime.getHour()
                timePickerEnd.minute = endTime.getMinute()
                btnConfirm.setOnClickListener {
                    if (checkTimeValidity()) {
                        // Save the new start/end times if the time is valid.
                        ContextsHandler.getContext<TimeContext>(name, type).setStartTime(timePickerStart.hour, timePickerStart.minute)
                        ContextsHandler.getContext<TimeContext>(name, type).setEndTime(timePickerEnd.hour, timePickerEnd.minute)
                        saveUserPreferences<TimeContext>("Time", "Time")
                        finish()
                    }
                    else
                    {
                        // Prompt the user to enter a valid time range.
                        Toast.makeText(applicationContext, "Please enter a valid time range.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            "Location" -> {
                // Removes the additional letter that is passed through so that the title only reads as "Location"
                tvTitle.text = tvTitle.text.dropLast(1)
                // Get location information to be displayed and stored locally.
                locationCoordinates = ContextsHandler.getContext<LocationContext>(name, type).getCoordinates()
                val nameToDisplay = ContextsHandler.getContext<LocationContext>(name, type).getName()
                // If a location has been previously stored, display the location name.
                if(nameToDisplay != "" && !nameToDisplay.contains("Location"))
                {
                    locationName = nameToDisplay
                }
                // Set the descriptor to have the correct wording for a location context.
                tvDescriptor.text = "What should be playing when you're at $locationName?"
                // Display the button to select locations.
                btnGetLocation.isVisible = true

                btnGetLocation.setOnClickListener {
                    // Pass the local coordinates through to the SelectLocation activity.
                    val intent = Intent(this, SelectLocation::class.java)
                    intent.putExtra("Location", name)
                    intent.putExtra("latitude", locationCoordinates.getLatitude())
                    intent.putExtra("longitude", locationCoordinates.getLongitude())
                    coordsFromMap.launch(intent)
                }

                btnConfirm.setOnClickListener {
                    // Save the coordinates.
                    ContextsHandler.getContext<LocationContext>(name, type).setCoordinates(locationCoordinates)
                    ContextsHandler.getContext<LocationContext>(name, type).setName(locationName)
                    saveUserPreferences<LocationContext>(name, type)

                    if(locationName != "this location")
                    {
                        // If there is a location name, return the location name to the context menu to update the button text.
                        intent.putExtra("name", locationName)
                        intent.putExtra("locationLetter", name)
                        setResult(RESULT_OK, intent)
                    }
                    else
                    {
                        // No changes made.
                        setResult(RESULT_CANCELED, intent)
                    }
                    finish()
                }
            }
            else -> {
                // Set the descriptor to have the correct wording for any other context.
                tvDescriptor.text = "What genres do you want to play when you're ${name.lowercase()}?"
                btnConfirm.setOnClickListener {
                    // Save the context when the user confirms, then close this screen.
                    saveUserPreferences<MusicContext>(name, type)
                    finish()
                }
            }
        }

        // For each genre listed.
        for (option in genres.children) {
            // Necessary as the option cannot be modified unless directly specified as a CheckBox.
            val genre = option as CheckBox
            genre.setOnClickListener {
                // Increment/decrement the genre numbers depending on if it's selected/unselected.
                if (genre.isChecked)
                {
                    genresSelected++
                }
                else
                {
                    genresSelected--
                }
                // Check if options need to be enabled/disabled.
                updateOptions()
            }
        }
        // Return to the contexts menu.
        btnBack.setOnClickListener {
            finish()
        }

        // Load the genre preferences that apply to this context.
        loadUserPreferences(name, type)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Checks if the current time selected by the user falls within a valid range of the day.
     * @return the validity of the time range.
     */
    private fun checkTimeValidity(): Boolean {
        // If the time range provided is valid.
        if (timePickerStart.hour < timePickerEnd.hour || (timePickerStart.hour == timePickerEnd.hour && timePickerStart.minute < timePickerEnd.minute))
        {
            return true
        }
        else
        {
            // Time range is not valid.
            return false
        }
    }

    /**
     * Saves the genre preferences to file.
     * @param name: The context name.
     * @param type: The type of context being saved.
     */
    private fun <T> saveUserPreferences(name : String, type : String) : Boolean
    {
        val userPreferences = mutableMapOf<String, Boolean>()
        // Save each genre into a map.
        for(checkbox in genres.children)
        {
            val genre = checkbox as CheckBox
            userPreferences[genre.text as String] = genre.isChecked
        }
        // Set the preferences in the ContextHandler, then save it to file.
        ContextsHandler.setContextPreferences(name, type, userPreferences)
        return ContextsHandler.saveToFile<T>(name, ContextsHandler.getContext(name, type), applicationContext)
    }

    /**
     * Load the user preferences from file.
     * @param name: The context name.
     * @param type: The type of context being loaded.
     */
    private fun loadUserPreferences(name : String, type : String)
    {
        // Returns if the context could not be loaded. This leaves everything unchecked.
        val context : MusicContext = ContextsHandler.getContext(name, type)
        val preferences = context.getPreferenceList()
        // Go through the contents of the preference list.
        for(checkbox in genres.children)
        {
            val genre = checkbox as CheckBox
            // If the preference is selected.
            if(preferences[genre.text as String] == true)
            {
                // Set the genre to be selected on the UI, incrementing the amount of genres.
                genre.isChecked = true
                genresSelected++
            }
        }
        // Checks for a case where there are 5 genres selected.
        updateOptions()
    }

    /**
     * Updates the UI to enable/disable checkbox options if a maximum of 5 has been hit.
     */
    private fun updateOptions() {
        // If an edge case has been reached.
        if((genresSelected == 5 && preferencesEnabled) || (genresSelected == 4 && !preferencesEnabled))
        {
            for (option in genres.children) {
                val genre = option as CheckBox
                // If the genre isn't checked, disable it so it cannot be selected.
                if (!genre.isChecked) {
                    genre.setEnabled(!preferencesEnabled)
                }
            }
            // Enable/disable the preference list.
            preferencesEnabled = !preferencesEnabled
        }
    }
}