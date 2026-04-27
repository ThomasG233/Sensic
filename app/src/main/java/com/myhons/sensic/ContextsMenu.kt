package com.myhons.sensic

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible

/**
 * Menu that displays all of the contexts available in the app.
 */
class ContextsMenu : AppCompatActivity() {
    // Late initialise all UI elements.
    private lateinit var btnBack : ImageButton
    private lateinit var btnMoods : Button
    private lateinit var btnLocations : Button
    private lateinit var btnWeather : Button
    private lateinit var btnMovement : Button
    private lateinit var btnTime : Button
    private lateinit var viewMood : ConstraintLayout
    private lateinit var viewWeather : ConstraintLayout
    private lateinit var viewMovement : ConstraintLayout
    private lateinit var viewLocations : ConstraintLayout


    // Get the location name so that the button can be updated to reflect the new name.
    var locationName = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // If the location was changed.
        if(result.resultCode == RESULT_OK)
        {
            // Get the necessary location information.
            val returnData = result.data as Intent
            val locationLabel = returnData.getStringExtra("locationLetter")
            val newName = returnData.getStringExtra("name")
            lateinit var btnToChange : Button
            // Find the menu option to be updated.
            when(locationLabel)
            {
                "LocationA" -> btnToChange = findViewById(R.id.btnLocationA)
                "LocationB" -> btnToChange = findViewById(R.id.btnLocationB)
                "LocationC" -> btnToChange = findViewById(R.id.btnLocationC)
            }
            // Update the button text to the location name.
            btnToChange.text = newName
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_contexts_menu)

        // Initialise all UI elements.
        btnBack = findViewById(R.id.btnBack)
        btnMoods = findViewById(R.id.btnMoods)
        btnLocations = findViewById(R.id.btnLocations)
        btnWeather = findViewById(R.id.btnWeather)
        btnMovement = findViewById(R.id.btnMovement)
        btnTime = findViewById(R.id.btnTime)

        viewMood = findViewById(R.id.viewMood)
        viewMovement = findViewById(R.id.viewMovement)
        viewWeather = findViewById(R.id.viewWeather)
        viewLocations = findViewById(R.id.viewLocations)

        // Go back to the main menu when pressed.
        btnBack.setOnClickListener {
            finish()
        }

        // Loads the time context when tapped.
        btnTime.setOnClickListener {
            loadScreen("Time", "Time")
        }

        val allViews = arrayOf(viewMood, viewMovement, viewWeather, viewLocations)
        // For each button in each dropdown that appears.
        for(view in allViews)
        {
            val viewType = view.tag.toString()
            // For each of the buttons in this dropdown menu.
            for(nextBtn in view.children)
            {
                // This declaration has to be made, as nextBtn cannot have .getText() used upon it.
                val contextBtn = nextBtn as Button
                if(view == viewLocations)
                {
                    // Get the location name to be displayed on the button/
                    val locationName = ContextsHandler.getContext<LocationContext>(contextBtn.tag.toString(), "Location").getName()
                    // Keeps the text preset if a location name hasn't been saved yet.
                    if(!locationName.contains("Location"))
                    {
                        contextBtn.text = locationName
                    }
                }
                // Load the appropriate context when clicked.
                contextBtn.setOnClickListener {
                    // Load the mood screen with the correct label.
                    loadScreen(viewType, contextBtn.tag.toString())
                }
            }
        }

        // Maps each dropdown button to the view that displays and hides them.
        val dropdowns = mapOf(btnMoods to viewMood, btnMovement to viewMovement, btnWeather to viewWeather, btnLocations to viewLocations)
        for(button in dropdowns)
        {
            // Display/hide the dropdown menu.
           button.key.setOnClickListener {
               dropMenu(button.value)
           }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Load the context screen.
     * @param label: the type of context being loaded.
     * @param name: the name of the context.
     */
    private fun loadScreen(label : String, name : String)
    {
        val intent = Intent(this, AdjustContext::class.java)
        // Pass through the name and type of the context.
        intent.putExtra("type", label)
        intent.putExtra("name", name)
        // For locations, the new button text should be returned.
        if(label == "Location")
        {
            locationName.launch(intent)
        }
        else
        {
            startActivity(intent)
        }
    }

    /**
     * Display/hide the dropdown menu.
     * @param dropdown: The dropdown that needs to be displayed/hidden.
     */
    private fun dropMenu(dropdown : ConstraintLayout)
    {
        // If visible, hide the dropdown.
        if(dropdown.isVisible)
        {
            dropdown.visibility = View.GONE
        }
        // If invisible, show the dropdown.
        else
        {
            dropdown.visibility = View.VISIBLE
        }
    }
}