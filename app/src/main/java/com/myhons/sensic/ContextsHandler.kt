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

    // Represents all preset types for each context.
    private val moodTypes = arrayOf("Happy", "Sad", "Anxious", "Angry", "Energetic", "Exhausted")
    private val locationLabels = arrayOf("LocationA", "LocationB", "LocationC")
    private val weatherTypes = arrayOf("Sunny", "Cloudy", "Raining", "Snowing")
    private val movementTypes = arrayOf("Walking", "On the Road", "Lounging")
    // Represents all genres saved by the application.
    private val genres = arrayOf("Chill", "Classical", "EDM", "Folk", "K-Pop", "Lo-Fi", "Metal", "Pop", "Rock", "Sleep")

    /**
     * Return the full list of genres available in the application.
     * @return genres
     */
    fun getGenres() : Array<String>
    {
        return genres
    }
    /**
     * Sets the current activity of the user.
     * @param activity: The new activity detected by the application.
     */
    fun setCurrentActivity(activity : String)
    {
        currentActivity = activity
    }

    /**
     * Gets the most recent activity saved.
     * @return the current activity of the user.
     */
    fun getCurrentActivity() : String
    {
        return currentActivity
    }

    /**
     * Gets the current mood set by the user.
     * @return the current mood.
     */
    fun getMood() : String
    {
        return currentMood
    }

    /**
     * Set the preference list
     * @param contextName: the name of the context to be saved.
     * @param contextType: the type of context being saved.
     * @param newPreferences: the preferences to be saved for this context.
     */
    fun setContextPreferences(contextName : String, contextType : String, newPreferences : MutableMap<String, Boolean>)
    {
        when(contextType) {
            "Mood" -> moods[contextName]?.setPreferenceList(newPreferences)
            "Weather" -> weather[contextName]?.setPreferenceList(newPreferences)
            "Movement" -> movement[contextName]?.setPreferenceList(newPreferences)
            "Location" -> locations[contextName]?.setPreferenceList(newPreferences)
            "Time" -> time.setPreferenceList(newPreferences)
        }
    }
    /**
     * Returns a saved context from the handler.
     * @param contextName: the name of the context to be found.
     * @param contextType: the type of context being searched for.
     * @return the context being searched for.
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

    /**
     * Resets all context to the default state.
     * @param applicationContext: The application, required for saving/loading.
     */
    fun resetAllToDefault(applicationContext : Context)
    {
        // Reset all moods to default state.
        for(moodType in moodTypes)
        {
            moods[moodType] = loadDefaultPreferences(moodType, "Mood")
            // Save the default settings.
            saveToFile(moodType, moods[moodType], applicationContext)
        }
        // Reset all weathers to the default state.
        for(climate in weatherTypes)
        {
            weather[climate] = loadDefaultPreferences(climate, "Weather")
            // Save the default settings.
            saveToFile(climate, weather[climate], applicationContext)
        }
        // Reset all locations to the default state.
        for(motion in movementTypes)
        {
            movement[motion] = loadDefaultPreferences(motion, "Movement")
            // Save the default settings.
            saveToFile(motion, movement[motion], applicationContext)
        }
        // Reset all weathers to the default state.
        for(location in locationLabels)
        {
            locations[location] = loadDefaultPreferences(location, "Location")
            // Save the default settings.
            saveToFile(location, locations[location], applicationContext)
        }
        // Reset and save the time to default.
        time = loadDefaultPreferences("Time", "Time")
        saveToFile("Time", time, applicationContext)
    }

    /**
     * Calls the loadFromFile function for each context during the initialization step.
     * @param contextSubclasses: the list of preset subclasses, such as moods or weather.
     * @param applicationContext: The application, required for saving/loading.
     * @return the context loaded from file.
     */
    fun <T>loadPreferencesIntoContext(contextSubclasses : Array<String>, contextType : String, applicationContext : Context) : MutableMap<String, T>
    {
        val context = mutableMapOf<String, T>()
        // For each sub-context for this context, load in the sub-context.
        for(label in contextSubclasses)
        {
            context[label] = loadFromFile(label, contextType, applicationContext)
        }
        return context
    }

    /**
     * Initialise all contexts at app startup.
     * @param currentContext: the application context. Required in order to save and load preferences into the app.
     * @param mood: the mood selected by the user at start-up.
     */
    fun initialiseContexts(currentContext: Context, mood: String)
    {
        // Initialise all contexts.
        moods = mutableMapOf()
        weather = mutableMapOf()
        movement = mutableMapOf()
        currentActivity = ""
        currentMood = mood
        // Load all contexts.
        moods = loadPreferencesIntoContext( moodTypes, "Mood", currentContext)
        weather = loadPreferencesIntoContext( weatherTypes, "Weather", currentContext)
        movement = loadPreferencesIntoContext( movementTypes, "Movement", currentContext)
        locations = loadPreferencesIntoContext(locationLabels, "Location", currentContext)
        time = loadFromFile("Time", "Time", currentContext)

    }

    /**
     * Save preferences into a file.
     * @param contextName: the name of the context being saved.
     * @param preferences: the list of preferences to be saved.
     * @param applicationContext: The context of the application, required for saving/loading.
     * @return if the save was successful.
     */
    fun <T> saveToFile(contextName : String, contextToSave : T, applicationContext: Context) : Boolean
    {
        // Establish the file as a .dat file.
        val filename = "$contextName.dat"
        try
        {
            // Set up both file streams.
            val fileOutputStream = applicationContext.openFileOutput(filename, Context.MODE_PRIVATE)
            val objectOutputStream = ObjectOutputStream(fileOutputStream)
            // Save the context into the file.
            objectOutputStream.writeObject(contextToSave)
            // Close the file streams.
            objectOutputStream.close()
            fileOutputStream.close()
        }
        // If an error occurred, the default settings are used instead.
        catch(e : Exception)
        {
            Log.e("Object Writing Error", "$e")
            return false
        }
        // Save was successful.
        return true
    }

    /**
     * Set the preferences to the default state.
     * @param contextName: name of the context being set to default.
     * @param contextType: the type of context being reset.
     * @return a context object, with default genre preferences.
     */
    fun <T> loadDefaultPreferences(contextName : String, contextType: String) : T
    {
        val preferenceDefault = mutableMapOf<String, Boolean>()
        // Set all genres to unchecked.
        for(genre in genres)
        {
            preferenceDefault[genre] = false
        }
        // If the context is a MusicContext object, return a MusicContext with the default settings.
        if(contextType != "Time" && contextType != "Location")
        {
            return MusicContext(contextName, preferenceDefault) as T
        }
        // If the context is a LocationContext object, return a LocationContext with the default settings.
        else if(contextType == "Location")
        {
            return LocationContext(contextName, preferenceDefault, Coordinates(0.0, 0.0)) as T
        }
        else
        {
            // Return a TimeContext with the default settings.
            return TimeContext(contextName, preferenceDefault, Time(12, 0), Time(13, 0)) as T
        }
    }


    /** Save and Load adapted from: https://www.youtube.com/watch?v=VaXNqzpjyEE
     * Loads a preference list from a file
     * @param contextName: The name of the context to save.
     * @param contextType: The type of context
     * @param applicationContext: The context of the application, required for saving/loading.
     */
    fun <T>loadFromFile(contextName : String, contextType : String, applicationContext : Context) : T
    {
        val filename = "$contextName.dat"
        try
        {
            // Open an object stream to load a full object from the file.
            val objectStream = ObjectInputStream(applicationContext.openFileInput(filename))
            // Read the context from file as the desired object.
            val loadedContext = objectStream.readObject() as T
            // Close the stream and return the context.
            objectStream.close()
            return loadedContext
        }
        // Error occurred when loading context, use the default settings for this context.
        catch(e: Exception)
        {
            Log.e("Error", "$e caused by $contextName")
        }
        // Load the default preferences into the file.
        val musicContext = loadDefaultPreferences<T>(contextName, contextType)
        // Try to save the default options.
        if(!saveToFile(contextName, musicContext, applicationContext))
        {
            Log.e("Error", "Could not successfully load or save preferences. Using default empty preference list for context $contextName")
        }
        // Return the default music context object.
        return musicContext
    }
}