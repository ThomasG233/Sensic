package com.myhons.sensic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

// Based from: https://developer.android.com/develop/sensors-and-location/location/transitions#kotlin
class ActivityCallback : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("Activity", "We're here.")
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            for (event in result!!.transitionEvents) {
                Log.d("Activity", "Checking types.")
                when (event.activityType) {
                    DetectedActivity.WALKING -> {
                        ContextsHandler.setCurrentActivity("Walking")
                        Log.d("Activity", "${event.activityType}")
                    }
                    DetectedActivity.IN_VEHICLE -> {
                        ContextsHandler.setCurrentActivity("Travelling")
                        Log.d("Activity", "${event.activityType}")
                    }
                    else -> {
                        ContextsHandler.setCurrentActivity("N/A")
                        Log.d("Activity", "${event.activityType}")
                    }
                }
            }
        }
    }
}