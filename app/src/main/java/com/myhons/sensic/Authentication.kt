package com.myhons.sensic

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets


private lateinit var sharedPreferences : SharedPreferences
private lateinit var accessToken : String

private var expiresIn = 0

// Checks to see if the callback URI is called by Spotify.
class Authentication : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("accessExpiry", 0)

        val returnData = intent.data
        var obtainingToken = false
        // The coroutine cannot be declared at this stage.
        lateinit var tokenCoroutine : Job
        var toastMessage = "We could not authenticate your account with Spotify. Please try again later."
        // If the callback address has been used.
        if (returnData != null) {
            Log.d("AUTH", "$returnData")
            val errorCode = returnData.getQueryParameter("error")

            if (errorCode == null) {
                // AUTHENTICATION CODE!!!
                val authCode = returnData.getQueryParameter("code")
                obtainingToken = true
                tokenCoroutine = CoroutineScope(Dispatchers.IO).launch {
                    try
                    {
                        getAccessToken(authCode.toString())
                        sharedPreferences.edit {
                            putString("accessToken", accessToken)
                        }
                        toastMessage = "Account successfully authenticated!"
                    }
                    catch (e: Exception)
                    {
                        toastMessage = "Authentication failed. Please try again. ${e.message}"
                    }
                    finally
                    {
                        obtainingToken = false
                    }
                }
            }
        }
        // Check to see if a token is currently being obtained.
        if(obtainingToken)
        {
            while(tokenCoroutine.isActive)
            {
                // Wait on the coroutine completing.
            }
            tokenCoroutine.cancel()
        }
        Toast.makeText(applicationContext, toastMessage, Toast.LENGTH_LONG).show()
        finish()
    }

    // Adapted from https://stackoverflow.com/questions/63876345/how-to-get-access-token-from-spotify-api
    suspend fun getAccessToken(authCode: String) {
        // Get all needed parameters from the PKCEHandler needed in the post request.
        val verifier = PKCEHandler.getCodeVerifier()
        val redirectUri = PKCEHandler.getRedirectUri()
        val clientID = PKCEHandler.getClientID()
        // Spotify's access token URL.
        val url = URL("https://accounts.spotify.com/api/token")
        // Set up the POST request being made to Spotify.
        val postRequest = url.openConnection() as HttpURLConnection
        postRequest.requestMethod = "POST"
        postRequest.setDoOutput(true)
        postRequest.setRequestProperty("Content-type", "application/x-www-form-urlencoded")
        // Creates and encodes the parameters to be added in the request.
        val parameters =
            "grant_type=authorization_code&client_id=$clientID&code=$authCode&redirect_uri=$redirectUri&code_verifier=$verifier"
        val out = parameters.toByteArray(StandardCharsets.UTF_8)
        // Sends the POST request to Spotify's API.
        postRequest.outputStream.use { os ->
            os.write(out)
        }
        // Used to collect the response data.
        val gson = Gson()
        // If the request was not successful
        if (postRequest.responseCode !in 200..204)
        {
            // Read in the error returned from Spotify.
            val stream = postRequest.errorStream
            val returnedError = stream.bufferedReader().use { it.readText() }
            val jsonObject = gson.fromJson(returnedError, JsonObject::class.java)
            // Collect the error description from the response JSON.
            val errorDescription = jsonObject.get("error_description").toString()
            // Close the connection, and return an error.
            postRequest.disconnect()
            throw Exception("Error Code: ${postRequest.responseCode}: $errorDescription")
        }
        // Collect the response data from Spotify.
        val stream = postRequest.inputStream
        val response = stream.bufferedReader().use { it.readText() }
        // Collect the access token from the response JSON.
        val jsonObject = gson.fromJson(response, JsonObject::class.java)
        accessToken = jsonObject.get("access_token").toString()
        Log.e("Spotify", accessToken)
        // Close the connection after use.
        postRequest.disconnect()
    }
}
