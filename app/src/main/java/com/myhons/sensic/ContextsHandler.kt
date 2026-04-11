package com.myhons.sensic
import android.content.Context
import android.util.Log
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Stores all contexts, acting as an access point for editing, saving and loading preferences.
 */
object ContextsHandler {

    // Moods, weather, and movement are all built-in objects, identifiable with their name.
    private lateinit var moods : MutableMap<String, MusicContext>
    private lateinit var weather : MutableMap<String, MusicContext>
    private lateinit var movement : MutableMap<String, MusicContext>
    // Location and Time are children of MusicContext.
    private lateinit var locations : MutableMap<String, LocationContext>
    private lateinit var time : TimeContext
    // Represents the current state of certain contexts.
    private lateinit var currentActivity : String
    private lateinit var currentMood : String

    private val moodTypes = arrayOf("Happy", "Sad", "Anxious", "Angry", "Energetic", "Exhausted")
    private val locationLabels = arrayOf("LocationA", "LocationB", "LocationC")
    private val weatherTypes = arrayOf("Sunny", "Cloudy", "Raining", "Snowing")
    private val movementTypes = arrayOf("Walking", "On the Road")
    private val genres = arrayOf("Chill", "Classical", "EDM", "Folk", "K-Pop", "Lo-Fi", "Metal", "Pop", "Rock", "Sleep")

    fun getMoodTypes() : Array<String>
    {
        return moodTypes
    }

    fun getWeatherTypes() : Array<String>
    {
        return weatherTypes
    }
    fun getMovementTypes() : Array<String>
    {
        return movementTypes
    }
    fun getGenres() : Array<String>
    {
        return genres
    }

    /**
     * Returns a saved context from the handler.
     * @param contextName: the name of the context to be edited.
     * @param contextType: used to find the context.
     * @return the desired MusicContext object.
     */
    fun <T> getContext(contextName : String, contextType : String): T
    {
        return when(contextType) {
            "Mood" -> moods[contextName]
            "Weather" -> weather[contextName]
            "Movement" -> movement[contextName]
            "Time" -> time
            "Location" -> locations[contextName]
            else -> null
        } as T
    }

    fun setCurrentActivity(activity : String)
    {
        currentActivity = activity
    }

    fun getCurrentActivity() : String
    {
        return currentActivity
    }

    fun getMood() : String
    {
        return currentMood
    }

    /**
     * Set the preference list
     * @param contextName:
     * @param contextType:
     * @param newPreferences:
     */
    fun setContextPreferences(contextName : String, contextType : String, newPreferences : MutableMap<String, Boolean>)
    {
        when(contextType) {
            "Mood" -> moods[contextName]?.setPreferenceList(newPreferences)
            "Weather" -> weather[contextName]?.setPreferenceList(newPreferences)
            "Movement" -> movement[contextName]?.setPreferenceList(newPreferences)
            "Time" -> time.setPreferenceList(newPreferences)
            "Location" -> locations[contextName]?.setPreferenceList(newPreferences)
        }
    }
    fun resetAllToDefault(applicationContext : Context)
    {
        for(mood in moodTypes)
        {
            moods[mood] = loadDefaultPreferences(mood, "Mood")
            saveToFile(mood, moods[mood], applicationContext)
        }
        for(climate in weatherTypes)
        {
            weather[climate] = loadDefaultPreferences(climate, "Weather")
            saveToFile(climate, weather[climate], applicationContext)
        }
        for(motion in movementTypes)
        {
            movement[motion] = loadDefaultPreferences(motion, "Movement")
            saveToFile(motion, movement[motion], applicationContext)
        }
        for(location in locationLabels)
        {
            locations = loadDefaultPreferences(location, "Location")
            saveToFile("Location", locations[location], applicationContext)
        }

        time = loadDefaultPreferences("Time", "Time")
        saveToFile("Time", time, applicationContext)

    }

    /**
     * Calls the loadFromFile function for each context during the initialization step.
     * @param contextSubclasses: the list of preset subclasses, such as moods or weather.
     * @param applicationContext:
     * @return
     */
    fun <T>loadPreferencesIntoContext(contextSubclasses : Array<String>, contextType : String, applicationContext : Context) : MutableMap<String, T>
    {
        val context = mutableMapOf<String, T>()
        for(label in contextSubclasses)
        {
            context[label] = loadFromFile(label, contextType, applicationContext)
        }
        return context
    }

    /**
     * Initialise all contexts at app startup.
     * @param currentContext: the application context. Required in order to save and load preferences into the app.
     */
    fun initialiseContexts(currentContext: Context, mood: String)
    {
        moods = mutableMapOf()
        weather = mutableMapOf()
        movement = mutableMapOf()
        currentActivity = ""
        currentMood = mood


        moods = loadPreferencesIntoContext( moodTypes, "Mood", currentContext)
        weather = loadPreferencesIntoContext( weatherTypes, "Weather", currentContext)
        movement = loadPreferencesIntoContext( movementTypes, "Movement", currentContext)
        locations = loadPreferencesIntoContext(locationLabels, "Location", currentContext)

        time = loadFromFile("Time", "Time", currentContext)

    }

    /**
     * Save preferences into a file.
     * @param file:
     * @param preferences:
     * @param applicationContext:
     */
    fun <T> saveToFile(file : String, contextToSave : T, applicationContext: Context) : Boolean
    {
        val filename = "$file.dat"
        try
        {
            val fileOutputStream = applicationContext.openFileOutput(filename, Context.MODE_PRIVATE)
            val objectOutputStream = ObjectOutputStream(fileOutputStream)

            objectOutputStream.writeObject(contextToSave)

            objectOutputStream.close()
            fileOutputStream.close()
        }
        catch(e : Exception)
        {
            Log.e("Object Writing Error", "$e")
            return false
        }
        return true
    }

    fun <T> loadDefaultPreferences(contextName : String, contextType: String) : T
    {
        val preferenceDefault = mutableMapOf<String, Boolean>()
        for(genre in genres)
        {
            preferenceDefault[genre] = false
        }
        if(contextType != "Time" && contextType != "Location")
        {
            return MusicContext(contextName, preferenceDefault) as T
        }
        else if(contextType == "Location")
        {
            return LocationContext(contextName, preferenceDefault, Coordinates(0.0, 0.0)) as T
        }
        else
        {
            return TimeContext(contextName, preferenceDefault, Time(12, 0), Time(13, 0)) as T
        }
    }


    /** Save and Load adapted from: https://www.youtube.com/watch?v=VaXNqzpjyEE
     * Loads a preference list from a file
     * @param contextName:
     * @param applicationContext:
     */
    fun <T>loadFromFile(contextName : String, contextType : String, applicationContext : Context) : T
    {
        val filename = "$contextName.dat"
        try
        {
            val ois = ObjectInputStream(applicationContext.openFileInput(filename))
            val loadedContext = ois.readObject() as T
            ois.close()
            Log.d("FileHandling", "Loaded preference list for $contextName")
            return loadedContext
        }
        catch(e: Exception)
        {
            Log.e("Error", "$e caused by $contextName")
        }

        val preferenceDefault = mutableMapOf<String, Boolean>()
        for(genre in genres)
        {
            preferenceDefault[genre] = false
        }
        val musicContext = loadDefaultPreferences<T>(contextName, contextType)
        if(!saveToFile(contextName, musicContext, applicationContext))
        {
            Log.e("Error", "Could not successfully load or save preferences. Using default empty preference list for context $contextName")
        }
        return musicContext
    }
}