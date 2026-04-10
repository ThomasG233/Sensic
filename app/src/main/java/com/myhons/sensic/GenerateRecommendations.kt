package com.myhons.sensic

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Calendar
import java.util.LinkedList
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class GenerateRecommendations : AppCompatActivity(){

    private var currentCoordinates = LatLng(0.0, 0.0)
    private var weather = ""
    private lateinit var fusedClient : FusedLocationProviderClient
    private var inTimeRange = false
    private var inLocation = false
    private var currentContexts = ""
    private var currentActivity = ""
    private lateinit var tvGenerating : TextView
    private lateinit var listRecommendations: ListView
    private lateinit var clRecommendations : ConstraintLayout
    private lateinit var tvCurrentContexts : TextView
    private lateinit var btnCreatePlaylists : Button


    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_generate_recommendations)

        tvGenerating = findViewById(R.id.tvGenerating)
        tvCurrentContexts = findViewById(R.id.tvCurrentContexts)
        clRecommendations = findViewById(R.id.clRecommendations)
        listRecommendations = findViewById(R.id.listRecommendations)
        btnCreatePlaylists = findViewById(R.id.btnCreatePlaylist)
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        val calendar = Calendar.getInstance()
        val currentTime = Time(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
        Log.e("Current Time", "${currentTime.getHour()}:${currentTime.getMinute()}")

        inTimeRange = checkTimeRange(currentTime)

        Log.e("TimeRange", inTimeRange.toString())

        // Get current mood. XX
        Log.e("Mood", ContextsHandler.getMood())

        // Get current movement ???

        lifecycleScope.launch(Dispatchers.IO) {
            val locationObtained = getCurrentLocation()
            if(locationObtained)
            {
                try
                {
                    val savedCoordinates = ContextsHandler.getContext<LocationContext>("Location", "Location").getCoordinates()
                    inLocation = checkLocationRange(savedCoordinates)
                    checkWeatherInLocation()
                }
                catch (e: Exception)
                {
                    Log.e("Error", "$e")
                }
            }
            val topGenres = getTopGenres()

            if(topGenres.isNotEmpty())
            {
                try {
                    var seeds = ""
                    topGenres.forEach { genre ->
                        val seedToAdd = searchSpotifyForSeed(genre)
                        if (seedToAdd == "400") {
                            withContext(Dispatchers.Main)
                            {
                                Toast.makeText(
                                    applicationContext,
                                    "You must authenticate with Spotify before you can generate a playlist.",
                                    Toast.LENGTH_LONG
                                ).show()
                                finish()
                            }
                        } else if (seedToAdd != "") {
                            seeds += "$seedToAdd,"
                        }
                    }
                    seeds = seeds.dropLast(1)
                    val recommendations = getRecommendations(seeds)
                    withContext(Dispatchers.Main)
                    {
                        if(recommendations.isEmpty())
                        {
                            Toast.makeText(applicationContext, "Could not find suitable songs. Please add more genres to your contexts.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        else
                        {
                            tvGenerating.isGone = true
                            clRecommendations.isVisible = true
                            tvCurrentContexts.text = currentContexts
                            val recommendationAdapter = SongListAdapter(
                                applicationContext,
                                android.R.layout.simple_list_item_2,
                                recommendations
                            )
                            listRecommendations.adapter = recommendationAdapter
                            btnCreatePlaylists.setOnClickListener {
                                lifecycleScope.launch(Dispatchers.IO)
                                {
                                    var playlistID = createPlaylistOnSpotify()
                                    if(playlistID != "")
                                    {
                                        if(fillPlaylistOnSpotify(playlistID, recommendations))
                                        {
                                            Log.d("Spotify", "Check Spotify.")
                                        }
                                        else
                                        {
                                            Log.e("Spotify", "No.")
                                        }
                                    }
                                }

                            }
                        }

                    }
                }
                catch(e: Exception)
                {
                    Log.e("Error", "$e")
                    finish()
                }
            }
            else
            {
                withContext(Dispatchers.Main)
                {
                    Toast.makeText(applicationContext, "No genres selected for your current context.", Toast.LENGTH_SHORT).show()
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

    private fun checkTimeRange(currentTime: Time) : Boolean
    {
        val startTime = ContextsHandler.getContext<TimeContext>("Time", "Time").getStartTime()
        val endTime = ContextsHandler.getContext<TimeContext>("Time", "Time").getEndTime()

        val startSecond = startTime.getInSeconds()
        val endSecond = endTime.getInSeconds()
        val currentSecond = currentTime.getInSeconds()

        Log.e("TimeRange", "Start: $startSecond. Currently: $currentSecond. Ends at: $endSecond")

        return currentSecond in startSecond..endSecond
    }


    // Haversine Formula https://www.movable-type.co.uk/scripts/latlong.html
    private fun checkLocationRange(savedCoordinates : Coordinates) : Boolean
    {
        val earthRadius = 6371e3;
        val piRadians = PI/180

        val currentLat = currentCoordinates.latitude * piRadians
        val currentLng = currentCoordinates.longitude * piRadians

        val latToReach = savedCoordinates.getLatitude() * piRadians
        val lngToReach = savedCoordinates.getLongitude() * piRadians
        val distBetweenLats = (latToReach - currentLat)
        val distBetweenLngs = (lngToReach - currentLng)

        val a = sin(distBetweenLats / 2).pow(2) + cos(currentLat) * cos(latToReach) * sin(distBetweenLngs/2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1-a))

        val d = earthRadius * c

        return if(d <= 50) {
            true
        } else {
            false
        }
    }

    private fun getTopGenres() : LinkedList<String>
    {
        val genreWeights = mutableMapOf<String, Int>()
        val genreList = ContextsHandler.getGenres()

        for(genreName in genreList)
        {
            genreWeights[genreName] = 0
        }

        val mood = ContextsHandler.getMood()
        Log.e("Mood", mood)

        val moodContext = ContextsHandler.getContext<MusicContext>(mood, "Mood").getPreferenceList()
        currentContexts = "Feeling $mood, "
        addWeightsToOptions(moodContext, genreWeights)
        currentActivity = ContextsHandler.getCurrentActivity()
        if(currentActivity != "")
        {
            val activityPreferences = ContextsHandler.getContext<MusicContext>(currentActivity, "Movement").getPreferenceList()
            addWeightsToOptions(activityPreferences, genreWeights)
            currentContexts += "whilst ${currentActivity.lowercase()}, "
        }
        if(inTimeRange)
        {
            val timeContext = ContextsHandler.getContext<TimeContext>("Time", "Time")
            val timePreferences = timeContext.getPreferenceList()
            addWeightsToOptions(timePreferences, genreWeights)
            currentContexts += "between ${timeContext.getStartText()} & ${timeContext.getEndText()}, "
        }
        if(inLocation)
        {
            val locationContext = ContextsHandler.getContext<LocationContext>("Location", "Location")
            val locationPreferences = locationContext.getPreferenceList()
            val locationName = locationContext.getName()
            addWeightsToOptions(locationPreferences, genreWeights)
            currentContexts += "at $locationName, "
        }
        if(weather != "")
        {
            val weatherPreferences = ContextsHandler.getContext<MusicContext>(weather, "Weather").getPreferenceList()
            addWeightsToOptions(weatherPreferences, genreWeights)
            currentContexts += "currently $weather, "
        }

        currentContexts = currentContexts.substringBeforeLast(", ") + "."


        val preferredGenres = LinkedList<String>()
        var genresAdded = 0
        val orderedMap = genreWeights.toSortedMap()
        orderedMap.forEach{ (genre, weight) ->
            if(weight != 0 && genresAdded != 5)
            {
                preferredGenres.add(genre)
                genresAdded++
            }
            Log.e("Genre", "$genre: $weight")
        }
        return preferredGenres
    }

    private fun addWeightsToOptions(preferences : Map<String, Boolean>, genres : MutableMap<String, Int>)
    {
        preferences.forEach { (genreName, selected) ->
            if(selected)
            {
                var newWeight = 0
                if(genres[genreName] != null)
                {
                    newWeight = genres[genreName] as Int
                }
                newWeight += 10
                genres[genreName] = newWeight
            }
        }
    }

    private fun makeGetRequest(url : URL,  requestProperties : Map<String, String>) : JsonObject
    {
        val getRequest = url.openConnection() as HttpURLConnection
        getRequest.requestMethod = "GET"
        requestProperties.forEach { (key, value) ->
            getRequest.setRequestProperty(key, value)
        }
        getRequest.connect()

        val gson = Gson()
        // If the request was not successful
        val stream : InputStream
        if (getRequest.responseCode in 300..504)
        {
            stream = getRequest.errorStream
        }
        else
        {
            stream = getRequest.inputStream
        }

        val returnedData = stream.bufferedReader().use { it.readText() }
        val jsonObject = gson.fromJson(returnedData, JsonObject::class.java)
        // Close the connection.
        getRequest.disconnect()
        return jsonObject
    }

    // Use Spotify's Search endpoint, then feed into ReccoBeats instead. https://developer.spotify.com/documentation/web-api/reference/search
    private fun getRecommendations(seedsForContext : String) : ArrayList<Song>
    {
        val url = URL("https://api.reccobeats.com/v1/track/recommendation?size=30&seeds=$seedsForContext")
        Log.d("URL", "$url")
        val requestProperties = mapOf("Content-type" to "application/json")
        val returnedJson = makeGetRequest(url, requestProperties)
        Log.d("Recommendations", "$returnedJson")
        if(returnedJson.get("error") != null)
        {
            Log.e("ReccoError", "Bad Seed.")
            return ArrayList()
        }
        val musicList = returnedJson.getAsJsonArray("content")
        Log.d("Music List", "$musicList")
        val musicPlaylist = ArrayList<Song>()

        for(i in 0 until musicList.size())
        {
            val song = musicList[i] as JsonObject
            val region = song.get("availableCountries").asString
            if(region.contains("GB"))
            {
                val songName = song.get("trackTitle").asString
                val artistName = (song.getAsJsonArray("artists").get(0) as JsonObject).get("name").asString
                val spotifyLink = song.get("href").asString
                val spotifyID = spotifyLink.substringAfterLast("/")
                val songToAdd = Song(songName, artistName, spotifyLink, spotifyID)
                musicPlaylist.add(songToAdd)
            }
        }
        musicPlaylist.forEach { song ->
            Log.d("Playlist", "${song.getTrackName()} by ${song.getArtistName()}, available at: ${song.getSpotifyLink()}. ID: ${song.getSpotifyID()}")
        }
        return musicPlaylist
    }
    private fun fillPlaylistOnSpotify(playlistID : String, songsToAdd : ArrayList<Song>) : Boolean
    {
        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        val accessToken = sharedPreferences.getString("accessToken", "")
        val url = URL("https://api.spotify.com/v1/playlists/$playlistID/items")
        val postRequest = url.openConnection() as HttpURLConnection
        postRequest.requestMethod = "POST"
        postRequest.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        postRequest.setRequestProperty("Accept", "application/json")
        postRequest.setRequestProperty("Authorization", "Bearer $accessToken")
        postRequest.setDoOutput(true)
        val parameters = JsonObject()
        val songsForPlaylist = JsonArray()
        songsToAdd.forEach{ song ->
            songsForPlaylist.add("spotify:track:${song.getSpotifyID()}")
        }
        parameters.add("uris", songsForPlaylist)
        parameters.addProperty("position", 0)
        val parametersToUse = parameters.toString()
        val out = parametersToUse.toByteArray(StandardCharsets.UTF_8)
        // Sends the POST request to Spotify's API.
        postRequest.outputStream.use { os ->
            os.write(out)
        }

        val gson = Gson()
        // If the request was not successful
        val stream : InputStream
        var errorReturned = false
        if (postRequest.responseCode != 201)
        {
            stream = postRequest.errorStream
            errorReturned = true
        }
        else
        {
            stream = postRequest.inputStream
        }

        val returnedData = stream.bufferedReader().use { it.readText() }
        Log.e("SpotifyResponse", returnedData)
        val jsonObject = gson.fromJson(returnedData, JsonObject::class.java)
        if(!errorReturned)
        {
            postRequest.disconnect()
            return true
        }
        // Close the connection.
        postRequest.disconnect()
        return false
    }

    private fun createPlaylistOnSpotify() : String
    {
        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        val accessToken = sharedPreferences.getString("accessToken", "")
        val url = URL("https://api.spotify.com/v1/me/playlists")
        val postRequest = url.openConnection() as HttpURLConnection
        postRequest.requestMethod = "POST"
        postRequest.setRequestProperty("Content-Type", "application/json")
        postRequest.setRequestProperty("Accept", "application/json")
        postRequest.setRequestProperty("Authorization", "Bearer $accessToken")
        postRequest.setDoOutput(true)
        val parameters = JsonObject()
        parameters.addProperty("name", currentContexts)
        parameters.addProperty("description", "Generated by Sensic.")
        parameters.addProperty("collaborative", false)
        parameters.addProperty("public", false)

        val convertedParameters = parameters.toString()
        Log.d("Parameters", convertedParameters)
        val out = convertedParameters.toByteArray(StandardCharsets.UTF_8)
        // Sends the POST request to Spotify's API.
        postRequest.outputStream.use { os ->
            os.write(out)
        }

        val gson = Gson()
        // If the request was not successful
        val stream : InputStream
        var errorReturned = false
        if (postRequest.responseCode != 201)
        {
            stream = postRequest.errorStream
            errorReturned = true
        }
        else
        {
            stream = postRequest.inputStream
        }

        val returnedData = stream.bufferedReader().use { it.readText() }
        Log.e("SpotifyResponse", returnedData)
        val jsonObject = gson.fromJson(returnedData, JsonObject::class.java)
        if(!errorReturned)
        {
            val playlistID = jsonObject.get("id").asString
            postRequest.disconnect()
            return playlistID
        }
        // Close the connection.
        postRequest.disconnect()
        return ""
    }


    private fun refreshSpotifyToken()
    {
        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        val refreshToken = sharedPreferences.getString("refreshToken", "")
        val url = URL("https://accounts.spotify.com/api/token")
        val postRequest = url.openConnection() as HttpURLConnection
        postRequest.requestMethod = "POST"
        postRequest.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        postRequest.setRequestProperty("Accept", "application/json")
        postRequest.setDoOutput(true)
        val parameters = "grant_type=refresh_token&refresh_token=$refreshToken&client_id=${PKCEHandler.getClientID()}"
        val out = parameters.toByteArray(StandardCharsets.UTF_8)
        // Sends the POST request to Spotify's API.
        postRequest.outputStream.use { os ->
            os.write(out)
        }

        val gson = Gson()
        // If the request was not successful
        val stream : InputStream
        var errorReturned = false
        if (postRequest.responseCode != 200)
        {
            stream = postRequest.errorStream
            errorReturned = true
        }
        else
        {
            stream = postRequest.inputStream
        }

        val returnedData = stream.bufferedReader().use { it.readText() }
        Log.e("SpotifyReturnData", returnedData)
        val jsonObject = gson.fromJson(returnedData, JsonObject::class.java)
        if(!errorReturned)
        {
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

    private fun searchSpotifyForSeed(genre : String) : String
    {
        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        var authToken = sharedPreferences.getString("accessToken", "")

        val url = URL("https://api.spotify.com/v1/search?q=genre:$genre&type=track")
        val requestProperties = mutableMapOf("Content-Type" to "application/json",
            "Authorization" to "Bearer $authToken")

        var returnedJson = makeGetRequest(url, requestProperties)
        if(returnedJson.get("error") != null)
        {
            val error = returnedJson.get("error") as JsonObject
            val responseCode = error.get("status").asInt
            when(responseCode)
            {
                400 -> {
                    return "400"
                }
                401 -> {
                    Log.e("Spotify", "Need to reauthenticate.")
                    refreshSpotifyToken()
                    authToken = sharedPreferences.getString("accessToken", "")
                    requestProperties["Authorization"] = "Bearer $authToken"
                    returnedJson = makeGetRequest(url, requestProperties)
                    if(returnedJson.get("error") != null)
                    {
                        return ""
                    }
                }
            }
        }
        lateinit var returnedSongs : JsonArray
        try {
            returnedSongs = returnedJson.getAsJsonObject("tracks").getAsJsonArray("items")
            Log.e("Returned Json 2", "$returnedSongs")
            if(returnedSongs.isEmpty)
            {
                Log.e("Song returned", "None")
                return ""
            }
            else
            {
                Log.e("Song returned", (returnedSongs[0] as JsonObject).get("id").asString)
                return (returnedSongs[0] as JsonObject).get("id").asString
            }
        }
        catch (e : Exception)
        {
            return ""
        }
    }

    suspend fun getCurrentLocation() : Boolean = suspendCancellableCoroutine { coroutine ->

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {

            val task = fusedClient.lastLocation
            task.addOnSuccessListener { location: Location? ->
                if(location != null)
                {
                    currentCoordinates = LatLng(location.latitude, location.longitude)
                    coroutine.resume(true)
                }
                else
                {
                    coroutine.resume(false)
                }


            }
            task.addOnFailureListener {
                coroutine.resume(false)
            }

        }
        else
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
            coroutine.resume(false)
        }
    }

    private fun checkWeatherInLocation()
    {
        // Location was verified.
        if(currentCoordinates.latitude != 0.0 && currentCoordinates.longitude != 0.0)
        {
            val appMetadata = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData
            val apiKey = appMetadata.getString("com.google.android.geo.API_KEY")
            val url = URL("https://weather.googleapis.com/v1/currentConditions:lookup?key=$apiKey&location.latitude=${currentCoordinates.latitude}&location.longitude=${currentCoordinates.longitude}")
            val requestProperties = mapOf("Content-type" to "application/json", "X-Android-Package" to "com.myhons.sensic", "X-Android-Cert" to "E7B4BC25790D6D98A2FF49BB621886B7885C0323")

            val jsonObject = makeGetRequest(url, requestProperties)
            val currentConditions = jsonObject.get("weatherCondition") as JsonObject
            weather = currentConditions.get("type").asString
            Log.d("Weather Returned", weather)
            if(weather.contains("CLEAR"))
            {
                weather = "Sunny"
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
    }
}