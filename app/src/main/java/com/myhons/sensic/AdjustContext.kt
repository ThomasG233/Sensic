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

        var genresSelected = 0
        val type = intent.getStringExtra("type")
        val name = intent.getStringExtra("name")


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

        btnConfirm.setOnClickListener {
            if (checkTimeValidity()) {
                savePreferences()
            }
        }

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
                }

                "Location" -> tvDescriptor.text = "What should be playing when you're at this location?"

                else -> tvDescriptor.text =
                    "What genres do you want to play when you're ${name.toString().lowercase()}?"
            }

            for (option in genres.children) {
                // Necessary call as the option cannot be modified unless directly specified as a CheckBox.
                val genre = option as CheckBox
                genre.setOnClickListener {
                    if (genre.isChecked) {
                        genresSelected++
                        Log.e("Selected", "$genresSelected")
                        if (genresSelected == 5) {
                            updateOptions(true)
                        }
                    } else {
                        genresSelected--
                        Log.e("Selected", "$genresSelected")
                        if ((genresSelected + 1) == 5) {
                            updateOptions(false)
                        }
                    }
                }
            }

            btnBack.setOnClickListener {
                finish()
            }

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        private fun checkTimeValidity(): Boolean {
            if (genresSelected != 0 && timePickerStart.hour < timePickerEnd.hour) {
                Log.d("TimeCheck", "Valid")
                return true
            } else {
                if ((timePickerStart.hour == timePickerEnd.hour) && (timePickerStart.minute < timePickerEnd.minute)) {
                    Log.d("TimeCheck", "Valid")
                    return true
                } else {
                    Log.e("TimeCheck", "Invalid")
                    return false
                }
            }
        }

        private fun savePreferences() {

        }

        private fun updateOptions(enabled: Boolean) {
            for (option in genres.children) {
                val genre = option as CheckBox
                if (!genre.isChecked) {
                    if (enabled) {
                        genre.setEnabled(false)
                    } else {
                        genre.setEnabled(true)
                    }
                }
            }
        }
    }