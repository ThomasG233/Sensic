package com.myhons.sensic

import android.os.Bundle
import android.util.Log
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children


class AdjustContext : AppCompatActivity() {

    private lateinit var btnBack : ImageButton
    private lateinit var tvDescriptor : TextView
    private lateinit var tvTitle : TextView
    private lateinit var genres : ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_adjust_context)

        var genresSelected = 0


        genres = findViewById(R.id.genres)
        btnBack = findViewById(R.id.btnBack)
        tvDescriptor = findViewById(R.id.tvDescriptor)
        tvTitle = findViewById(R.id.tvTitle)

        val type = intent.getStringExtra("type")
        val name = intent.getStringExtra("name")
        tvTitle.text = name
        when(type)
        {
            "Weather" -> tvDescriptor.text = "What genres do you want to play when it's ${name.toString().lowercase()}?"
            else -> tvDescriptor.text = "What genres do you want to play when you're ${name.toString().lowercase()}?"
        }

        for(option in genres.children)
        {
            // Necessary call as the option cannot be modified unless directly specified as a CheckBox.
            val genre = option as CheckBox
            genre.setOnClickListener {
                if(genre.isChecked)
                {
                    genresSelected++
                    Log.e("Selected", "$genresSelected")
                    if(genresSelected == 5)
                    {
                        updateOptions(true)
                    }
                }
                else
                {
                    genresSelected--
                    Log.e("Selected", "$genresSelected")
                    if((genresSelected + 1) == 5)
                    {
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
    private fun updateOptions(enabled : Boolean)
    {
        for(option in genres.children)
        {
            val genre = option as CheckBox
            if(!genre.isChecked)
            {
                if(enabled)
                {
                    genre.setEnabled(false)
                }
                else
                {
                    genre.setEnabled(true)
                }

            }
        }
    }
}