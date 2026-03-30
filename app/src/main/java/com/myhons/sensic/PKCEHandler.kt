package com.myhons.sensic

import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64


// Spotify's PKCE flow for web version. https://developer.spotify.com/documentation/web-api/tutorials/code-pkce-flow
// The PKCE flow was adapted with the assistance of SecureAuth: https://docs.secureauth.com/iam/build-an-android-app-using-oauth-2-0-and-pkce
object PKCEHandler
{

    private const val CLIENT_ID : String = "b36bc5a934fc4cc79633c26420b3d0d7"
    private const val REDIRECT_URI : String = "com.myhons.sensic://callback"
    private var verifier = ""

    /**
     *
     */
    fun getClientID() : String
    {
        return CLIENT_ID
    }

    /**
     *
     */
    fun getRedirectUri() : String
    {
        return REDIRECT_URI
    }

    /**
     * Getter Function for the Code Verifier.
     * @return verifier
     */
    fun getCodeVerifier() : String
    {
        return verifier
    }

    /**
     * Getter Function for the Code Challenger.
     * @param codeLength, the designed length of the code challenge (this will be 64 in this instance)
     * @return codeChallenge
     */
    fun getCodeChallenge(codeLength: Int) : String
    {
        // If a verifier has not been generated yet, then it must be generated first.
        if(verifier == "")
        {
            verifier = generateCodeVerifier(codeLength)
        }
        // Hash the verifier.
        return generateCodeChallenge(verifier)
    }

    /**
     * Generates the Code Verifier using Base64 encoding and SecureRandom.
     * @param codeLength, the desired length of the code challenge.
     * @return the verifier; a completely random string (64 characters in length)
     */
    private fun generateCodeVerifier(codeLength : Int) : String
    {
        val rdm = SecureRandom()
        val codeVerifier = ByteArray(codeLength)
        rdm.nextBytes(codeVerifier)
        return Base64.encodeToString(codeVerifier, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }


    /**
     * Generates the code challenge using the code verifier, hashed using SHA-256.
     */
    private fun generateCodeChallenge(codeVerifier : String) : String
    {
        val bytes = codeVerifier.toByteArray(charset("US-ASCII"))
        val messageDigest : MessageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}