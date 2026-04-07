package com.myhons.sensic

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.internal.concurrent.Task
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

class GenerateRecommendations : AppCompatActivity() {

    private var currentCoordinates = LatLng(0.0, 0.0)
    private var weather = ""
    private lateinit var fusedClient : FusedLocationProviderClient
    private var inTimeRange = false
    private var inLocation = false


    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_generate_recommendations)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)


        val movement = mutableListOf<ActivityTransition>()

        movement += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.IN_VEHICLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build()

        movement += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.IN_VEHICLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build()

        movement += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build()

        movement += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build()

        val calendar = Calendar.getInstance()
        val currentTime = Time(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
        Log.e("Current Time", "${currentTime.getHour()}:${currentTime.getMinute()}")

        inTimeRange = checkTimeRange(currentTime)

        Log.e("TimeRange", inTimeRange.toString())

        // Get current mood. XX
        Log.e("Mood", ContextsHandler.getMood())

        // Gets current coordinates. XX
        // Gets current weather conditions. XX
        // Gets current time. XX
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

                try
                {
                    var seeds = ""
                    topGenres.forEach { genre ->
                        seeds += searchSpotifyForSeed(genre) + ","
                    }
                    seeds = seeds.dropLast(1)

                    getRecommendations(seeds)
                }
                catch(e: Exception)
                {
                    Log.e("Error", "$e")
                    finish()
                }
                finally
                {

                }
            }
            else
            {
                Looper.prepare()
                Toast.makeText(applicationContext, "No genres selected for your current context.", Toast.LENGTH_LONG).show()
                finish()
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

    // https://developer.android.com/develop/sensors-and-location/location/transitions#kotlin
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getCurrentActivity(transitions : MutableList<ActivityTransition>)
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                101
            )
        }

        val broadcastIntent = PendingIntent.getActivity(applicationContext, 0, Intent(this, ActivityCallback::class.java), PendingIntent.FLAG_IMMUTABLE)
        val request = ActivityTransitionRequest(transitions)
        val task = ActivityRecognition.getClient(this).requestActivityTransitionUpdates(request, broadcastIntent)
        task.addOnSuccessListener {
            Log.d("ActivityCollection", ContextsHandler.getCurrentActivity())
        }
        task.addOnFailureListener { e: Exception ->
            Log.e("ActivityCollection", "${e.message}")
        }
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

        return if(d <= 100) {
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

        val moodContext = ContextsHandler.getContext<MusicContext>("Happy", "Mood").getPreferenceList()
        addWeightsToOptions(moodContext, genreWeights)

        if(inTimeRange)
        {
            val timePreferences = ContextsHandler.getContext<TimeContext>("Time", "Time").getPreferenceList()
            addWeightsToOptions(timePreferences, genreWeights)

        }
        if(inLocation)
        {
            val locationPreferences = ContextsHandler.getContext<LocationContext>("Location", "Location").getPreferenceList()
            addWeightsToOptions(locationPreferences, genreWeights)
        }
        if(weather != "")
        {
            val weatherPreferences = ContextsHandler.getContext<MusicContext>(weather, "Weather").getPreferenceList()
            addWeightsToOptions(weatherPreferences, genreWeights)
        }

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
    private fun getRecommendations(seedsForContext : String)
    {
        val url = URL("https://api.reccobeats.com/v1/track/recommendation?size=20&seeds=$seedsForContext")
        Log.d("URL", "$url")
        val requestProperties = mapOf("Content-type" to "application/json")
        val returnedJson = makeGetRequest(url, requestProperties)
        Log.d("Recommendations", "$returnedJson")
        val musicList = returnedJson.getAsJsonArray("content")
        Log.d("Music List", "$musicList")
        val musicPlaylist = LinkedList<Song>()

        for(i in 0 until musicList.size())
        {
            val song = musicList[i] as JsonObject
            val songName = song.get("trackTitle").asString
            val artistName = (song.getAsJsonArray("artists").get(0) as JsonObject).get("name").asString
            val spotifyLink = song.get("href").asString
            val spotifyID = spotifyLink.substringAfterLast("/")
            val songToAdd = Song(songName, artistName, spotifyLink, spotifyID)
            musicPlaylist.add(songToAdd)
        }
        musicPlaylist.forEach { song ->
            Log.e("Playlist", "${song.getTrackName()} by ${song.getArtistName()}, available at: ${song.getSpotifyLink()}. ID: ${song.getSpotifyID()}")
        }
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
                    finish()
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
        val returnedSongs = returnedJson.getAsJsonObject("tracks").getAsJsonArray("items")
        val seed = (returnedSongs[0] as JsonObject).get("id").asString
        return seed
    }

    suspend fun getCurrentLocation() : Boolean = suspendCancellableCoroutine { coroutine ->

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            val task = fusedClient.lastLocation
            task.addOnSuccessListener { location: Location ->
                currentCoordinates = LatLng(location.latitude, location.longitude)
                coroutine.resume(true)
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