package com.myhons.sensic

import android.R
import android.R.style
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate


// Adapted from: https://www.youtube.com/watch?v=E6vE8fqQPTE
class SongListAdapter(context : Context, private val resource : Int, objects: ArrayList<Song>) : ArrayAdapter<Song>(context, resource, objects)
{

    @SuppressLint("ViewHolder")
    override fun getView(position : Int, convertView : View?, parent : ViewGroup) : View
    {
        val songName = getItem(position)?.getTrackName()
        val artistName = getItem(position)?.getArtistName()

        val inflater = LayoutInflater.from(context)
        var viewConvert = convertView
        viewConvert = inflater.inflate(resource, parent, false)

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

        return viewConvert as View
    }
}