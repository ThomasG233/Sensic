package com.myhons.sensic
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
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
    // Location and Time are children of MusicContext, with additional information included.
    private lateinit var location : LocationContext
    private lateinit var time : TimeContext

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
            "Location" -> location
            else -> null
        } as T
    }

    /**
     * Returns the time context object.
     * @return time context.
     */
    fun getTimeContext() : TimeContext
    {
        return time
    }

    /**
     * Returns the location context object.
     * @return location context.
     */
    fun getLocationContext() : LocationContext
    {
        return location
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
            "Location" -> location.setPreferenceList(newPreferences)
        }
    }

    /**
     * Calls the loadFromFile function for each context during the initialization step.
     * @param contextSubclasses: the list of preset subclasses, such as moods or weather.
     * @param applicationContext:
     * @return
     */
    fun loadPreferencesIntoContext(contextSubclasses : Array<String>, contextType : String, applicationContext : Context) : MutableMap<String, MusicContext>
    {
        val context = mutableMapOf<String, MusicContext>()
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
    fun initialiseContexts(currentContext : Context)
    {
        moods = mutableMapOf()
        weather = mutableMapOf()
        movement = mutableMapOf()

        val moodTypes = arrayOf("Happy", "Sad", "Anxious", "Angry", "Energetic", "Exhausted")
        val weatherTypes = arrayOf("Sunny", "Raining", "Snowing")
        val movementTypes = arrayOf("Walking", "On the Road")

        moods = loadPreferencesIntoContext( moodTypes, "Mood", currentContext)
        weather = loadPreferencesIntoContext( weatherTypes, "Weather", currentContext)
        movement = loadPreferencesIntoContext( movementTypes, "Movement", currentContext)


        time = loadFromFile("Time", "Time", currentContext)
        location = loadFromFile("Location", "Time", currentContext)
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
            Log.d("Object Writing Error", "$e")
            return false
        }
        return true
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
            val loadedContext = ois.readObject()
            ois.close()
            Log.d("FileHandling", "Loaded preference list for $contextName")
            return loadedContext as T
        }
        catch(e: Exception)
        {
            Log.e("Error", "$e caused by $contextName")
        }
        val genres = arrayOf("Chill", "Classical", "EDM", "Folk", "K-Pop", "Lo-Fi", "Metal", "Pop", "Rock", "Sleep")
        val preferenceDefault = mutableMapOf<String, Boolean>()
        for(genre in genres)
        {
            preferenceDefault[genre] = false
        }
        val musicContext : MusicContext
        if(contextType != "Location" && contextType != "Time")
        {
            musicContext = MusicContext(contextName, preferenceDefault)
        }
        else if(contextType == "Time")
        {
            musicContext = TimeContext(contextName, preferenceDefault, Time(12, 0), Time(13, 0))
        }
        else
        {
            musicContext = LocationContext(contextName, preferenceDefault, Coordinates(0.0,0.0))
        }

        if(!saveToFile(contextName, musicContext, applicationContext))
        {
            Log.e("Error", "Could not successfully load or save preferences. Using default empty preference list for context $contextName")
        }
        return musicContext as T
    }
}