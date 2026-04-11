package com.myhons.sensic

import android.R
import android.app.Activity
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Typeface
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri


// Adapted from: https://www.youtube.com/watch?v=E6vE8fqQPTE
class SongListAdapter(context : Context, private val resource : Int, objects: ArrayList<Song>) : ArrayAdapter<Song>(context, resource, objects)
{

    @SuppressLint("ViewHolder")
    override fun getView(position : Int, convertView : View?, parent : ViewGroup) : View
    {
        val songName = getItem(position)?.getTrackName()
        val artistName = getItem(position)?.getArtistName()

        val inflater = LayoutInflater.from(context)
        val viewConvert = inflater.inflate(resource, parent, false)

        val nightMode = AppCompatDelegate.getDefaultNightMode()

        val firstLine = viewConvert.findViewById<TextView>(R.id.text1)
        val secondLine = viewConvert.findViewById<TextView>(R.id.text2)
        val colourToUse : Int

        if(nightMode == AppCompatDelegate.MODE_NIGHT_NO)
        {
            colourToUse = R.color.black
        }
        else
        {
            colourToUse = R.color.white
        }

        firstLine.setTextColor(context.resources.getColor(colourToUse, context.theme))
        secondLine.setTextColor(context.resources.getColor(colourToUse, context.theme))
        firstLine.text = songName
        secondLine.text = artistName

        firstLine.setTypeface(Typeface.DEFAULT_BOLD)

        viewConvert.setOnClickListener {
            val songUrl = getItem(position)?.getSpotifyLink() as String
            val intent = Intent(Intent.ACTION_VIEW, songUrl.toUri())
            // While adding this flag isn't safe, this needs to be added in order to allow each item to direct to their Spotify page.
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        return viewConvert as View
    }
}