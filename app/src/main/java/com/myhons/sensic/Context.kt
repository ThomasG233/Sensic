package com.myhons.sensic

import java.io.Serializable

// For the Preference List to be saved successfully, it must be serializable, but with Java.
open class MusicContext(
    private var name: String,
    private var preferenceList : Map<String, Boolean>
) : Serializable
{
    fun getName() : String
    {
        return name
    }

    fun setName(name: String) {
        this.name = name
    }

    fun getPreferenceList(): Map<String, Boolean>
    {
        return preferenceList
    }

    fun setPreferenceList(preferenceList : Map<String, Boolean>)
    {
        this.preferenceList = preferenceList
    }
}

class LocationContext(
    name: String,
    preferenceList : Map<String, Boolean>,
    private var coordinates : Coordinates
) : MusicContext(name, preferenceList), Serializable
{
    fun setCoordinates(latitude : Double, longitude: Double)
    {
        this.coordinates = Coordinates(latitude, longitude)
    }
    fun setCoordinates(coordinates : Coordinates)
    {
        this.coordinates = coordinates
    }
    fun getCoordinates() : Coordinates
    {
        return coordinates
    }
}

class TimeContext(
    name: String,
    preferenceList : Map<String, Boolean>,
    private var startTime: Time,
    private var endTime : Time,
) : MusicContext(name, preferenceList), Serializable
{
    fun setStartTime(hour : Int, minute : Int)
    {
        startTime.setTime(hour, minute)
    }

    fun getStartTime() : Time
    {
        return startTime
    }

    fun getEndTime() : Time
    {
        return endTime
    }

    fun setEndTime(hour : Int, minute : Int)
    {
        endTime.setTime(hour, minute)
    }
}

class Time(private var hour : Int, private var minute : Int) : Serializable
{
    fun setTime(hour : Int, minute: Int)
    {
        this.hour = hour
        this.minute = minute
    }

    fun getHour() : Int
    {
        return hour
    }
    fun getMinute() : Int
    {
        return minute
    }

    // Uses seconds to determine whether the current time is in range.
    fun getInSeconds() : Int
    {
        val timeInSeconds = (hour * 3600) + (minute * 60)
        return timeInSeconds
    }
}

class Coordinates(private var latitude : Double, private var longitude : Double) : Serializable
{
    fun setCoordinates(latitude : Double, longitude: Double)
    {
        this.latitude = latitude
        this.longitude = longitude
    }

    fun getLatitude() : Double
    {
        return latitude
    }
    fun getLongitude() : Double
    {
        return longitude
    }
}
