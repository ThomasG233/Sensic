package com.myhons.sensic

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible

class ContextsMenu : AppCompatActivity() {
    private lateinit var btnBack : ImageButton
    private lateinit var btnMoods : Button
    private lateinit var btnLocations : Button
    private lateinit var btnWeather : Button
    private lateinit var btnMovement : Button
    private lateinit var btnTime : Button
    private lateinit var viewMood : ConstraintLayout
    private lateinit var viewWeather : ConstraintLayout
    private lateinit var viewMovement : ConstraintLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_contexts_menu)

        btnBack = findViewById(R.id.btnBack)
        btnMoods = findViewById(R.id.btnMoods)
        btnLocations = findViewById(R.id.btnLocations)
        btnWeather = findViewById(R.id.btnWeather)
        btnMovement = findViewById(R.id.btnMovement)
        btnTime = findViewById(R.id.btnTime)

        viewMood = findViewById(R.id.viewMood)
        viewMovement = findViewById(R.id.viewMovement)
        viewWeather = findViewById(R.id.viewWeather)


        btnBack.setOnClickListener {
            finish()
        }

        val allViews = arrayOf(viewMood, viewMovement, viewWeather)
        for(view in allViews)
        {
            val viewType = view.tag.toString()
            for(nextBtn in view.children)
            {
                // This declaration has to be made, as nextBtn cannot have .getText() used upon it.
                val contextBtn = nextBtn as Button
                contextBtn.setOnClickListener {
                    // Load the mood screen with the correct label.
                    loadScreen(viewType, contextBtn.getText().toString())
                }
            }
        }

        // Maps each dropdown button to the view that displays and hides them.
        val dropdowns = mapOf(btnMoods to viewMood, btnMovement to viewMovement, btnWeather to viewWeather)

        //
        for(button in dropdowns)
        {
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
    private fun loadScreen(label : String, name : String)
    {
        val intent = Intent(this, AdjustContext::class.java)
        intent.putExtra("type", label)
        intent.putExtra("name", name)
        startActivity(intent)
    }

    private fun dropMenu(dropdown : ConstraintLayout)
    {
        if(dropdown.isVisible)
        {
            dropdown.visibility = View.GONE
        }
        else
        {
            dropdown.visibility = View.VISIBLE
        }
    }
}