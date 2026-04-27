package com.myhons.sensic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

// Initially based from: https://developer.android.com/develop/sensors-and-location/location/transitions#kotlin
// Uses https://developers.google.com/android/reference/com/google/android/gms/location/ActivityRecognitionResult
/**
 * Receives broadcasts pertaining to the current activity.
 */
class ActivityCallback : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            // Get the list of probable activities.
            val result = ActivityRecognitionResult.extractResult(intent) as ActivityRecognitionResult
            // Set the current activity to the activity with the highest confidence level.
            val currentActivity = result.mostProbableActivity
            when (currentActivity.type) {
                // Set the current activity to "Walking" for anything on foot.
                DetectedActivity.ON_FOOT -> {
                    ContextsHandler.setCurrentActivity("Walking")
                }
                DetectedActivity.WALKING -> {
                    ContextsHandler.setCurrentActivity("Walking")
                }
                // Set the current activity to "On the Road" if driving is detected.
                DetectedActivity.IN_VEHICLE -> {
                    ContextsHandler.setCurrentActivity("On the Road")
                }
                // Set the current activity to "Lounging" if the device is idle.
                DetectedActivity.STILL -> {
                    ContextsHandler.setCurrentActivity("Lounging")
                }
                // Clear the current activity if it's not one that Sensic knows.
                else -> {
                    ContextsHandler.setCurrentActivity("")
                }
            }
        }
    }
}