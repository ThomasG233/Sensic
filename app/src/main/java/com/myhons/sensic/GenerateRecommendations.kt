package com.myhons.sensic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Calendar
import java.util.LinkedList
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach
import kotlin.collections.set
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Screen for getting recommendations based on current contexts.
 */
class GenerateRecommendations : AppCompatActivity() {
    // Used to obtain the user's current location.
    private lateinit var fusedClient : FusedLocationProviderClient
    // Holds the state of the current contexts.
    private var inTimeRange = false
    private var weather = ""
    private var inSavedLocation = ""
    private var locationSubstring = ""
    private var currentContexts = ""
    private var currentActivity = ""
    private var currentCoordinates = LatLng(0.0, 0.0)
    // Late initialise all UI elements.
    private lateinit var tvGenerating : TextView
    private lateinit var listRecommendations: ListView
    private lateinit var clRecommendations : ConstraintLayout
    private lateinit var tvCurrentContexts : TextView
    private lateinit var btnCreatePlaylists : Button
    private lateinit var progressBar : ProgressBar
    private lateinit var btnBack : ImageButton

    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_generate_recommendations)

        // Initialise all UI elements and services.
        progressBar = findViewById(R.id.progressBar)
        tvGenerating = findViewById(R.id.tvGenerating)
        tvCurrentContexts = findViewById(R.id.tvCurrentContexts)
        clRecommendations = findViewById(R.id.clRecommendations)
        listRecommendations = findViewById(R.id.listRecommendations)
        btnCreatePlaylists = findViewById(R.id.btnCreatePlaylist)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        // Get the current device time.
        val calendar = Calendar.getInstance()
        val currentTime = Time(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))

        // Check if the current time is within the range saved by the user.
        inTimeRange = checkTimeRange(currentTime)
        // Run on a different thread so that the app does not freeze.
        lifecycleScope.launch(Dispatchers.IO) {
            // Set the progress bar to be empty.
            progressBar.setProgress(0, true)
            // Attempt to get the location of the device.
            val locationObtained = getCurrentLocation()
            if(locationObtained)
            {
                try
                {
                    val locations = arrayOf("LocationA", "LocationB", "LocationC")
                    // Check each saved location.
                    locations.forEach { location ->
                        val savedCoordinates = ContextsHandler.getContext<LocationContext>(location, "Location").getCoordinates()
                        // Check if the user is within range of this location.
                        if(checkLocationRange(savedCoordinates))
                        {
                            // Set the user's location.
                            inSavedLocation = location
                        }
                    }
                    // Reflect the progress for the user.
                    progressBar.setProgress(25, true)
                    // Use the location to check the weather.
                    checkWeatherInLocation()
                }
                catch (e: Exception)
                {
                    // Could not get the weather at this stage.
                    Log.e("Could not get locational data successfully.", "$e")
                }
            }
            // Find the top 5 genres that the user would most likely want to hear currently.
            val topGenres = getTopGenres()
            progressBar.setProgress(50, true)
            // If the user has selected some genres.
            if(topGenres.isNotEmpty())
            {
                try
                {
                    var genreSeeds = ""
                    var recommendations = arrayListOf<Song>()
                    progressBar.setProgress(75, true)
                    topGenres.forEach { genre ->
                        // Get seeds to represent this genre.
                        genreSeeds = searchSpotifyForSeeds(genre)
                        // If an error was returned from Spotify, exit.
                        if(genreSeeds == "400")
                        {
                            withContext(Dispatchers.Main)
                            {
                                // Inform the user to authenticate.
                                Toast.makeText(applicationContext, "You must authenticate with Spotify before you can generate a playlist.", Toast.LENGTH_LONG).show()
                                finish()
                            }
                        }
                        else if(genreSeeds != "")
                        {
                            // Get track recommendations based on the seeds.
                            recommendations += getRecommendations(genreSeeds)
                        }
                    }
                    progressBar.setProgress(100, true)
                    // Put the recommendations into a random order.
                    recommendations.shuffle()
                    // Return to the main thread so that the UI can be updated.
                    withContext(Dispatchers.Main)
                    {
                        // If no recommendations were found, inform the user they need to select more genres.
                        if(recommendations.isEmpty())
                        {
                            Toast.makeText(applicationContext, "Could not find suitable songs. Please add more genres to your contexts.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        else
                        {
                            // Display the appropriate UI elements for the recommendations.
                            tvGenerating.isGone = true
                            progressBar.isGone = true
                            clRecommendations.isVisible = true
                            tvCurrentContexts.text = currentContexts
                            // Use a custom adapter to display all songs returned.
                            val recommendationAdapter = SongListAdapter(applicationContext, android.R.layout.simple_list_item_2, recommendations)
                            listRecommendations.adapter = recommendationAdapter
                            // Allows the user to create their own playlist.
                            btnCreatePlaylists.setOnClickListener {
                                lifecycleScope.launch(Dispatchers.IO)
                                {
                                    // Try to create a playlist on spotify, saving the playlist ID.
                                    val playlistID = createPlaylistOnSpotify()
                                    if(playlistID != "")
                                    {
                                        // Fill the playlist on Spotify.
                                        if(fillPlaylistOnSpotify(playlistID, recommendations))
                                        {
                                            // Redirect the user to the playlist on Spotify.
                                            val playlistIntent = Intent(Intent.ACTION_VIEW, ("https://open.spotify.com/playlist/$playlistID").toUri())
                                            startActivity(playlistIntent)
                                        }
                                        // Playlist could not be filled.
                                        else
                                        {
                                            withContext(Dispatchers.Main)
                                            {
                                                // Inform the user the playlist was made, but not filled.
                                                Toast.makeText(applicationContext, "Playlist created on your account, but songs could not be added.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    // Playlist could not be created.
                                    else
                                    {
                                        withContext(Dispatchers.Main)
                                        {
                                            // Inform the user the playlist could not be created.
                                            Toast.makeText(applicationContext, "Could not create a playlist at this time. Please try again later.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                            }
                        }

                    }
                }
                catch(e: Exception)
                {
                    progressBar.setProgress(100, true)
                    withContext(Dispatchers.Main)
                    {
                        Toast.makeText(applicationContext, "An error occurred: $e", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                }
            }
            else
            {
                progressBar.setProgress(100, true)
                // User needs to select genres for their current contexts to get recommendations.
                withContext(Dispatchers.Main)
                {
                    Toast.makeText(applicationContext, "No genres selected for your current contexts.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Checks if the current time is within the saved range of the user.
     * @param currentTime: the device time.
     */
    private fun checkTimeRange(currentTime: Time) : Boolean
    {
        // Get the start and end times.
        val startTime = ContextsHandler.getContext<TimeContext>("Time", "Time").getStartTime()
        val endTime = ContextsHandler.getContext<TimeContext>("Time", "Time").getEndTime()
        // Convert all times to seconds.
        val startSecond = startTime.getInSeconds()
        val endSecond = endTime.getInSeconds()
        val currentSecond = currentTime.getInSeconds()
        // Return true if the time is in range.
        return currentSecond in startSecond..endSecond
    }


    /** Similar implementation referenced from https://www.movable-type.co.uk/scripts/latlong.html
     * Uses the Haversine formula to determine whether the user
     * @param savedCoordinates: the coordinates of the location context being checked.
     */
    private fun checkLocationRange(savedCoordinates : Coordinates) : Boolean
    {
        // Constants for the formula.
        val earthRadius = 6371e3;
        val piRadians = PI/180
        // Get the current latitude in pi radians.
        val currentLat = currentCoordinates.latitude * piRadians
        val currentLng = currentCoordinates.longitude * piRadians
        // Get the latitude and longitude of the location context in pi radians.
        val latToReach = savedCoordinates.getLatitude() * piRadians
        val lngToReach = savedCoordinates.getLongitude() * piRadians
        // Get the distance between each coordinate point.
        val distBetweenLats = (latToReach - currentLat)
        val distBetweenLngs = (lngToReach - currentLng)
        // Get half of the length between the current location and saved location on the globe, squared.
        val a = sin(distBetweenLats / 2).pow(2) + cos(currentLat) * cos(latToReach) * sin(distBetweenLngs/2).pow(2)
        // Calculates the distance between the saved and current location in radians.
        val c = 2 * atan2(sqrt(a), sqrt(1-a))
        // Get the distance between the two points in metres.
        val d = earthRadius * c
        // If the user is within a 50 metre radius, they are considered to be at the saved location.
        return if(d <= 50) {
            true
        } else {
            false
        }
    }

    /**
     * Return a list of top genres, based on the current contexts.
     * @return the top five genres.
     */
    private fun getTopGenres() : LinkedList<String>
    {
        val genreWeights = mutableMapOf<String, Int>()
        val genreList = ContextsHandler.getGenres()
        // Set all genres to a weight of 0 initially.
        for(genreName in genreList)
        {
            genreWeights[genreName] = 0
        }
        // Get the preference list for the current mood.
        val mood = ContextsHandler.getMood()
        val moodContext = ContextsHandler.getContext<MusicContext>(mood, "Mood").getPreferenceList()
        currentContexts = "Feeling $mood"
        // For each selected genre for this mood, add a weight of 10.
        addWeightsToOptions(moodContext, genreWeights, 10)
        // Check the current activity of the user.
        currentActivity = ContextsHandler.getCurrentActivity()
        if(currentActivity != "")
        {
            // Get the preference list for this activity.
            val activityPreferences = ContextsHandler.getContext<MusicContext>(currentActivity, "Movement").getPreferenceList()
            // Add a weight of 15 to each selected genre for this activity.
            addWeightsToOptions(activityPreferences, genreWeights, 15)
            currentContexts += " whilst ${currentActivity.lowercase()}"
        }
        if(inTimeRange)
        {
            // Get the preference list for the time context.
            val timeContext = ContextsHandler.getContext<TimeContext>("Time", "Time")
            val timePreferences = timeContext.getPreferenceList()
            // Add a weight of 15 for each genre selected for time.
            addWeightsToOptions(timePreferences, genreWeights, 15)
            currentContexts += " between ${timeContext.getStartText()} & ${timeContext.getEndText()}"
        }
        if(inSavedLocation != "")
        {
            // Get the name and preference list for the current location.
            val locationContext = ContextsHandler.getContext<LocationContext>(inSavedLocation, "Location")
            val locationPreferences = locationContext.getPreferenceList()
            val locationName = locationContext.getName()
            // Add a weight of 20 to each selected genre for this location.
            addWeightsToOptions(locationPreferences, genreWeights, 20)
            currentContexts += " at $locationName"
            // Saves the substring to remove when creating the playlist if necessary.
            locationSubstring = " at $locationName"
        }
        if(weather != "")
        {
            // Get the genre preferences for this type of weather.
            val weatherPreferences = ContextsHandler.getContext<MusicContext>(weather, "Weather").getPreferenceList()
            addWeightsToOptions(weatherPreferences, genreWeights, 20)
            // Changes text to clear skies if early in the morning/late at night.
            if(weather == "Sunny")
            {
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if(currentHour !in 7..18)
                {
                    weather = "Clear Skies"
                }
            }
            currentContexts += ", currently $weather"
        }
        currentContexts += "."
        // Establish a list of preferred genres.
        val preferredGenres = LinkedList<String>()
        var genresAdded = 0
        // Sort the genres in order of weights.
        val orderedMap = genreWeights.toSortedMap()
        orderedMap.forEach{ (genre, weight) ->
            // Adds up to a maximum of 5 genres.
            if(weight != 0 && genresAdded != 5)
            {
                // Add this genre to the list.
                preferredGenres.add(genre)
                genresAdded++
            }
            // Logged for debugging purposes.
            Log.d("Genre", "$genre: $weight")
        }
        return preferredGenres
    }

    /**
     * Adds a weight to a given genre.
     * @param preferences: the preference list being checked.
     * @param genres: a map of the genres and their respective weights.
     * @param weightToAdd: the weight to be added for each selected genre.
     */
    private fun addWeightsToOptions(preferences : Map<String, Boolean>, genres : MutableMap<String, Int>, weightToAdd : Int)
    {
        preferences.forEach { (genreName, selected) ->
            if(selected)
            {
                var newWeight = 0
                // Prevents any errors from occurring if nothing can be read.
                if(genres[genreName] != null)
                {
                    // Get the current weight of the genre.
                    newWeight = genres[genreName] as Int
                }
                // Add the weight to the genre.
                newWeight += weightToAdd
                genres[genreName] = newWeight
            }
        }
    }

    /**
     * Make a GET request to an API endpoint.
     * @param url: The endpoint trying to be reached.
     * @param requestProperties: The properties of the request being made.
     * @return the raw JSON response as a JSONObject.
     */
    private fun makeGetRequest(url : URL,  requestProperties : Map<String, String>) : JsonObject
    {
        // Set up the GET request.
        val getRequest = url.openConnection() as HttpURLConnection
        getRequest.requestMethod = "GET"
        // Add each property to the request.
        requestProperties.forEach { (key, value) ->
            getRequest.setRequestProperty(key, value)
        }
        getRequest.connect()

        val gson = Gson()
        // If the request was not successful, use the error stream.
        val stream : InputStream
        if (getRequest.responseCode in 300..504)
        {
            stream = getRequest.errorStream
        }
        else
        {
            stream = getRequest.inputStream
        }
        // Get the response data.
        val returnedData = stream.bufferedReader().use { it.readText() }
        val response = gson.fromJson(returnedData, JsonObject::class.java)
        // Close the connection.
        getRequest.disconnect()
        return response
    }

    /**
     * Feeds Spotify Seeds into ReccoBeats.
     * @param seedsForContext: The seeds to be given to ReccoBeats.
     * @return the recommendations collected from ReccoBeats.
     */
    private fun getRecommendations(seedsForContext : String) : ArrayList<Song>
    {
        // Make a GET request to the ReccoBeats recommendation endpoint.
        val url = URL("https://api.reccobeats.com/v1/track/recommendation?size=10&seeds=$seedsForContext")
        val requestProperties = mapOf("Content-type" to "application/json")
        val returnedJson = makeGetRequest(url, requestProperties)
        // If an error was returned, return an empty list.
        if(returnedJson.get("error") != null)
        {
            return ArrayList()
        }
        // Get the list of songs returned from the response.
        val musicList = returnedJson.getAsJsonArray("content")
        val musicPlaylist = ArrayList<Song>()

        // For each song returned...
        for(i in 0 until musicList.size())
        {
            val song = musicList[i] as JsonObject
            // Check that the song being checked is available in the UK.
            val region = song.get("availableCountries").asString
            if(region.contains("GB"))
            {
                // Get the song metadata to be saved.
                val songName = song.get("trackTitle").asString
                val artistList = mutableListOf<String>()
                val artists = song.getAsJsonArray("artists")
                // Get all of the artists involved with the song.
                for(i in 0 until artists.size())
                {
                    val artistName = (artists.get(i) as JsonObject).get("name").asString
                    artistList.add(artistName)
                }
                // Save the link to the song on Spotify, along with the ID for the song.
                val spotifyLink = song.get("href").asString
                val spotifyID = spotifyLink.substringAfterLast("/")
                // Create a Song object and add it to the playlist.
                val songToAdd = Song(songName, artistList, spotifyLink, spotifyID)
                musicPlaylist.add(songToAdd)
            }
        }
        return musicPlaylist
    }

    /**
     * Fill the playlist with the songs returned from ReccoBeats.
     * @param playlistID: the ID of the playlist being filled.
     * @param songsToAdd: the list of songs to be added.
     * @return if the playlist was filled successfully.
     */
    private fun fillPlaylistOnSpotify(playlistID : String, songsToAdd : ArrayList<Song>) : Boolean
    {
        // Get the access token from the settings.
        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        var accessToken = sharedPreferences.getString("accessToken", "")
        // Set up the request properties.
        val url = URL("https://api.spotify.com/v1/playlists/$playlistID/items")
        val requestProperties = mutableMapOf("Content-Type" to "application/x-www-form-urlencoded", "Accept" to "application/json", "Authorization" to "Bearer $accessToken")
        val songsForPlaylist = JsonArray()
        songsToAdd.forEach{ song ->
            // Add each track formatted how Spotify would like it in the request.
            songsForPlaylist.add("spotify:track:${song.getSpotifyID()}")
        }
        val parameters = JsonObject()
        parameters.add("uris", songsForPlaylist)
        parameters.addProperty("position", 0)
        // Attempt to make the playlist.
        var responseData = makePostRequest(url, requestProperties, parameters)

        // If there was an error when trying to create the playlist.
        if(responseData.has("error"))
        {
            // Get the error code.
            val error = responseData.get("error") as JsonObject
            val errorCode = error.get("status").asInt
            when(errorCode)
            {
                // Reauthentication required.
                401 -> {
                    // Refresh the access token.
                    refreshSpotifyToken()
                    // Get the new access token and reflect this in the request properties.
                    accessToken = sharedPreferences.getString("accessToken", "")
                    requestProperties["Authorization"] = "Bearer $accessToken"
                    // Make another request.
                    responseData = makePostRequest(url, requestProperties, parameters)
                    // If another error occurred, then the app cannot fill the playlist at present.
                    if(responseData.has("error"))
                    {
                        return false
                    }
                }
                else -> return false
            }
        }
        // Playlist successfully filled.
        return true
    }

    /**
     * Makes a POST request to an endpoint.
     * @param url: The endpoint that the POST request is being made to.
     * @param requestProperties: The properties of the POST request.
     * @param dataToSend: The data to be posted.
     */
    private fun makePostRequest(url : URL,  requestProperties : Map<String, String>, dataToSend : JsonObject) : JsonObject
    {
        // Set up the POST request.
        val postRequest = url.openConnection() as HttpURLConnection
        postRequest.requestMethod = "POST"
        requestProperties.forEach { (key, value) ->
            postRequest.setRequestProperty(key, value)
        }
        postRequest.setDoOutput(true)
        // Encode the parameters.
        val parameters = dataToSend.toString()
        val out = parameters.toByteArray(StandardCharsets.UTF_8)
        // Sends the POST request to the endpoint.
        postRequest.outputStream.use { os ->
            os.write(out)
        }
        val gson = Gson()
        val stream : InputStream
        // If the request was not successful, use the error stream.
        if (postRequest.responseCode != 201)
        {
            stream = postRequest.errorStream
        }
        else
        {
            stream = postRequest.inputStream
        }
        // Get the data returned from the request.
        val returnedData = stream.bufferedReader().use { it.readText() }
        val response = gson.fromJson(returnedData, JsonObject::class.java)
        // Close the connection.
        postRequest.disconnect()
        return response
    }

    /**
     * Create a new playlist on the user's Spotify account.
     * @return the ID of the newly created playlist.
     */
    private fun createPlaylistOnSpotify() : String
    {
        // Get the access token.
        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        var accessToken = sharedPreferences.getString("accessToken", "")
        // Set up the basic request properties.
        val url = URL("https://api.spotify.com/v1/me/playlists")
        val requestProperties = mutableMapOf("Content-Type" to "application/json", "Accept" to "application/json", "Authorization" to "Bearer $accessToken")
        val parameters = JsonObject()
        if(inSavedLocation != "")
        {
            // If the location privacy feature is enabled, remove the location from the playlist name.
            val locationPrivacy = sharedPreferences.getBoolean("hideLocation", true)
            if(locationPrivacy)
            {
                currentContexts = currentContexts.replace(locationSubstring, "")
            }
        }

        // Add all of the settings for the newly created playlist.
        parameters.addProperty("name", currentContexts)
        parameters.addProperty("description", "Generated by Sensic.")
        // Try and make the playlist private (this will fail but the attempt is still made).
        parameters.addProperty("collaborative", false)
        parameters.addProperty("public", false)
        // Make the POST request.
        var response = makePostRequest(url, requestProperties, parameters)
        if(!response.has("error"))
        {
            // Return the playlist ID if successful.
            val playlistID = response.get("id").asString
            return playlistID
        }
        else
        {
            // Check the error returned.
            val errorCode = (response.get("error") as JsonObject).get("status").asInt
            when(errorCode)
            {
                401 -> {
                    // Reauthentication required. Refresh the token.
                    refreshSpotifyToken()
                    accessToken = sharedPreferences.getString("accessToken", "")
                    // Update the request properties to reflect this change in token.
                    requestProperties["Authorization"] = "Bearer $accessToken"
                    // Make the request with this new token.
                    response = makeGetRequest(url, requestProperties)
                    if(response.get("error") != null)
                    {
                        // Playlist could not be created.
                        return ""
                    }
                    else
                    {
                        // Playlist created.
                        val playlistID = response.get("id").asString
                        return playlistID
                    }
                }
            }
        }
        // Playlist could not be created successfully.
        return ""
    }

    /**
     * Get a new access token and refresh token from Spotify.
     */
    private fun refreshSpotifyToken()
    {
        // Get the saved refresh token.
        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        val refreshToken = sharedPreferences.getString("refreshToken", "")
        // Set up the POST request for the reauthentication process.
        val url = URL("https://accounts.spotify.com/api/token")
        val postRequest = url.openConnection() as HttpURLConnection
        postRequest.requestMethod = "POST"
        postRequest.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        postRequest.setRequestProperty("Accept", "application/json")
        postRequest.setDoOutput(true)
        // Set up and encode the parameters.
        val parameters = "grant_type=refresh_token&refresh_token=$refreshToken&client_id=${PKCEHandler.getClientID()}"
        val out = parameters.toByteArray(StandardCharsets.UTF_8)
        // Sends the POST request to Spotify's API.
        postRequest.outputStream.use { os ->
            os.write(out)
        }
        val gson = Gson()
        val stream : InputStream
        var errorReturned = false
        // If the request was not successful
        if (postRequest.responseCode != 200)
        {
            // Use the error stream.
            stream = postRequest.errorStream
            errorReturned = true
        }
        else
        {
            stream = postRequest.inputStream
        }
        // Get the return data.
        val returnedData = stream.bufferedReader().use { it.readText() }
        val jsonObject = gson.fromJson(returnedData, JsonObject::class.java)
        if(!errorReturned)
        {
            // Save the tokens locally.
            val newToken = jsonObject.get("access_token").asString
            val newRefresh = jsonObject.get("refresh_token").asString
            sharedPreferences.edit {
                putString("accessToken", newToken)
                putString("refreshToken", newRefresh)
            }
        }
        // Close the connection.
        postRequest.disconnect()
    }

    /**
     * Find tracks/seeds that best represent the genre.
     * @param genre: The genre being searched for.
     */
    private fun searchSpotifyForSeeds(genre : String) : String
    {
        // Get the access token.
        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        var authToken = sharedPreferences.getString("accessToken", "")
        // Set up the request.
        val url = URL("https://api.spotify.com/v1/search?q=genre:$genre&type=track&limit=5")
        val requestProperties = mutableMapOf("Content-Type" to "application/json", "Authorization" to "Bearer $authToken")
        // Make the request.
        var returnedJson = makeGetRequest(url, requestProperties)
        if(returnedJson.has("error"))
        {
            // Get the error code.
            val error = returnedJson.get("error") as JsonObject
            val responseCode = error.get("status").asInt
            when(responseCode)
            {
                // Couldn't successfully search.
                400 -> {
                    return "400"
                }
                // Need to reauthenticate.
                401 -> {
                    refreshSpotifyToken()
                    // Get the new token and change the request properties to reflect this.
                    authToken = sharedPreferences.getString("accessToken", "")
                    requestProperties["Authorization"] = "Bearer $authToken"
                    // Make the request again.
                    returnedJson = makeGetRequest(url, requestProperties)
                    // If an error still occurs, the app cannot get seeds.
                    if(returnedJson.get("error") != null)
                    {
                        return ""
                    }
                }
            }
        }
        lateinit var returnedSongs : JsonArray
        try {
            // Get the list of returned songs.
            returnedSongs = returnedJson.getAsJsonObject("tracks").getAsJsonArray("items")
            if(returnedSongs.isEmpty)
            {
                // No songs returned.
                return ""
            }
            else
            {
                // Get the 5 seeds returned from the request.
                var seed = ""
                for(i in 0 until returnedSongs.size())
                {
                    // Split each individual seed with a comma.
                    seed += (returnedSongs[i] as JsonObject).get("id").asString + ","
                }
                // Remove the last comma, and return the seed list.
                seed = seed.dropLast(1)
                return seed
            }
        }
        // Could not get any tracks.
        catch (e : Exception)
        {
            return ""
        }
    }

    /**
     * Gets the current location of the user.
     */
    suspend fun getCurrentLocation() : Boolean = suspendCancellableCoroutine { coroutine ->
        // If permissions have been given to get the user's location.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            // Attempt to get the last saved location of the user.
            val task = fusedClient.lastLocation
            task.addOnSuccessListener { location: Location? ->
                if(location != null)
                {
                    // Set the current coordinates within the activity.
                    currentCoordinates = LatLng(location.latitude, location.longitude)
                    // Location was successfully collected. Can move on to check the location and weather contexts.
                    coroutine.resume(true)
                }
                else
                {
                    // Location couldn't be collected. Move on to the other contexts.
                    coroutine.resume(false)
                }
            }
            // If the location cannot be ascertained, skip the location and weather contexts.
            task.addOnFailureListener {
                coroutine.resume(false)
            }
        }
        else
        {
            // Request permissions to get the user's location.
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
            // Cannot use the location currently. Skip the location and weather for this generation.
            coroutine.resume(false)
        }
    }

    /**
     * Check the weather in this current location.
     */
    private fun checkWeatherInLocation()
    {
        // If the location has been established.
        if(currentCoordinates.latitude != 0.0 && currentCoordinates.longitude != 0.0)
        {
            // Get the API key from the manifest.
            val appMetadata = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData
            val apiKey = appMetadata.getString("com.google.android.geo.API_KEY")
            // Make a request to Google Cloud's weather API.
            val url = URL("https://weather.googleapis.com/v1/currentConditions:lookup?key=$apiKey&location.latitude=${currentCoordinates.latitude}&location.longitude=${currentCoordinates.longitude}")
            // Pass through the SHA-1 Fingerprint and package name in the request.
            val requestProperties = mapOf("Content-type" to "application/json", "X-Android-Package" to "com.myhons.sensic", "X-Android-Cert" to "E7B4BC25790D6D98A2FF49BB621886B7885C0323")
            val response = makeGetRequest(url, requestProperties)
            try {
                // Get the current weather conditions.
                val currentConditions = response.get("weatherCondition") as JsonObject
                weather = currentConditions.get("type").asString
                // Format the weather appropriately, based on what was returned.
                if(weather.contains("CLEAR"))
                {
                    weather = "Sunny"
                }
                else if(weather.contains("CLOUDY"))
                {
                    weather = "Cloudy"
                }
                else if(weather.contains("RAIN"))
                {
                    weather = "Raining"
                }
                else if(weather.contains("SNOW"))
                {
                    weather = "Snowing"
                }
                else
                {
                    weather = ""
                }
            }
            // Weather could not be obtained. Skips this context.
            catch(e : Exception)
            {
                Log.e("GoogleWeatherAPI", "$e. Could not get the most up to date information on the weather. Ignoring this context.")
                weather = ""
            }

        }
    }
}