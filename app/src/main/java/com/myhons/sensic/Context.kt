package com.myhons.sensic

import java.io.Serializable


/**
 * MusicContext: Represents the most basic form of a context.
 * @param name: Name of the context.
 * @param preferenceList: The genres preferred by the user when this context is present.
 * For the Preference List to be saved successfully, it must be serializable, but specifically with Java's Serializable library.
 */
open class MusicContext(
    private var name: String,
    private var preferenceList : Map<String, Boolean>
) : Serializable
{
    /**
     * Get the name of the context
     * @return name of the context.
     */
    fun getName() : String
    {
        return name
    }

    /**
     * Set the name of the context.
     * @param name: The new context name.
     */
    fun setName(name: String) {
        this.name = name
    }

    /**
     * Get the preferences associated with a given context.
     * @return the genre preferences for this context.
     */
    fun getPreferenceList(): Map<String, Boolean>
    {
        return preferenceList
    }

    /**
     * Set the preferences for this context.
     * @param preferenceList: The new preferences.
     */
    fun setPreferenceList(preferenceList : Map<String, Boolean>)
    {
        this.preferenceList = preferenceList
    }
}

/**
 * LocationContext: Represents a location.
 * @param coordinates: The coordinates for a given location.
 */
class LocationContext(
    name: String,
    preferenceList : Map<String, Boolean>,
    private var coordinates : Coordinates
) : MusicContext(name, preferenceList), Serializable
{
    /**
     * Set the coordinates for this location.
     * @param coordinates: The new coordinates.
     */
    fun setCoordinates(coordinates : Coordinates)
    {
        this.coordinates = coordinates
    }

    /**
     * Get the coordinates for this specific location.
     * @return the coordinates for this context.
     */
    fun getCoordinates() : Coordinates
    {
        return coordinates
    }
}

/**
 * TimeContext: represents a time.
 * @param startTime: The time in which the preferences should start being considered.
 * @param endTime: The time in which the preferences should stop being considered.
 */
class TimeContext(
    name: String,
    preferenceList : Map<String, Boolean>,
    private var startTime: Time,
    private var endTime : Time,
) : MusicContext(name, preferenceList), Serializable
{
    /**
     * Sets the start time for this context.
     * @param hour: The start hour to be set.
     * @param minute: The start minute to be set.
     */
    fun setStartTime(hour : Int, minute : Int)
    {
        startTime.setTime(hour, minute)
    }

    /**
     * Get the start time for this context.
     * @return the start time.
     */
    fun getStartTime() : Time
    {
        return startTime
    }

    /**
     * Returns the start time as a string.
     * @return the start time.
     */
    fun getStartText() : String
    {
        val startHour = "${startTime.getHour()}"
        val startMinute = startTime.getMinute()
        var minAsText : String
        // If the minute needs to have a 0 before it, add a 0.
        if(startTime.getMinute() < 10)
        {
            minAsText = "0${startMinute}"
        }
        else
        {
            minAsText = "$startMinute"
        }
        return "$startHour:$minAsText"
    }
    /**
     * Returns the end time as a string.
     * @return the end time.
     */
    fun getEndText() : String
    {
        val endHour = "${endTime.getHour()}"
        val endMinute = endTime.getMinute()
        var minAsText : String
        // If the minute needs to have a 0 before it, add a 0.
        if(endTime.getMinute() < 10)
        {
            minAsText = "0${endMinute}"
        }
        else
        {
            minAsText = "$endMinute"
        }
        return "$endHour:$minAsText"
    }
    /**
     * Returns the end time.
     * @return the end time.
     */
    fun getEndTime() : Time
    {
        return endTime
    }
    /**
     * Set the end time.
     * @param hour: the new hour to be set.
     * @param minute: the new minute to be set.
     */
    fun setEndTime(hour : Int, minute : Int)
    {
        endTime.setTime(hour, minute)
    }
}

/**
 * Time: Represents a time.
 * @param hour: represents the hour of a given time.
 * @param minute: represents the minute of a given time.
 */
class Time(private var hour : Int, private var minute : Int) : Serializable
{
    /**
     * Sets the current time.
     * @param hour: the new hour to be set.
     * @param minute: the new minute to be set.
     */
    fun setTime(hour : Int, minute: Int)
    {
        this.hour = hour
        this.minute = minute
    }

    /**
     * Get the hour of this time object.
     * @return the hour saved.
     */
    fun getHour() : Int
    {
        return hour
    }

    /**
     * Get the minute of this time object.
     * @return the minute saved.
     */
    fun getMinute() : Int
    {
        return minute
    }

    /**
     * Converts the current time to be in seconds.
     * @return the time object in seconds.
     */
    fun getInSeconds() : Int
    {
        val timeInSeconds = (hour * 3600) + (minute * 60)
        return timeInSeconds
    }
}

/**
 * Coordinates: represents a set of coordinates for a given location.
 * @param latititude: The latitude portion of the coordinates.
 * @param longitude: The longitude portion of the coordinates.
 */
class Coordinates(private var latitude : Double, private var longitude : Double) : Serializable
{
    /**
     * Get the latitude of the coordinates.
     * @return the latitude portion of the coordinates.
     */
    fun getLatitude() : Double
    {
        return latitude
    }

    /**
     * Get the longitude of the coordinates.
     * @return the longitude portion of the coordinates.
     */
    fun getLongitude() : Double
    {
        return longitude
    }
}
