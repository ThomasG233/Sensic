package com.myhons.sensic

import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri


/**
 * Adapts a list of song objects for a ListView.
 * Implementation adapted from: https://www.youtube.com/watch?v=E6vE8fqQPTE
  */
class SongListAdapter(context : Context, private val resource : Int, objects: ArrayList<Song>) : ArrayAdapter<Song>(context, resource, objects)
{

    @SuppressLint("ViewHolder")
    override fun getView(position : Int, convertView : View?, parent : ViewGroup) : View
    {
        // Get the track and artist names to be displayed for each item.
        val songName = getItem(position)?.getTrackName()
        val artistNames = getItem(position)?.getArtistNames()

        val inflater = LayoutInflater.from(context)
        val viewConvert = inflater.inflate(resource, parent, false)
        // Used to check if it is currently nightmode.
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        // Get the first and second line of the new entry in the ListView.
        val firstLine = viewConvert.findViewById<TextView>(R.id.text1)
        val secondLine = viewConvert.findViewById<TextView>(R.id.text2)
        val colourToUse : Int
        // Make the text colour black if night mode isn't enabled.
        if(nightMode == AppCompatDelegate.MODE_NIGHT_NO)
        {
            colourToUse = R.color.black
        }
        // Make the text colour white if night mode is enabled.
        else
        {
            colourToUse = R.color.white
        }
        // Set the text colours based on the theme.
        firstLine.setTextColor(context.resources.getColor(colourToUse, context.theme))
        secondLine.setTextColor(context.resources.getColor(colourToUse, context.theme))
        // Set the text to be the song and artist names.
        firstLine.text = songName
        secondLine.text = artistNames
        // Use the same font as the application does.
        firstLine.setTypeface(Typeface.DEFAULT_BOLD)
        viewConvert.setOnClickListener {
            // Redirect the user to the song on Spotify if tapped.
            val songUrl = getItem(position)?.getSpotifyLink() as String
            val intent = Intent(Intent.ACTION_VIEW, songUrl.toUri())
            // While adding this flag isn't safe, this needs to be added in order to allow each item to direct to their Spotify page.
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        return viewConvert as View
    }
}