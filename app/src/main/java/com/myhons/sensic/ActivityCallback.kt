package com.myhons.sensic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

// Based from: https://developer.android.com/develop/sensors-and-location/location/transitions#kotlin
class ActivityCallback : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent) as ActivityRecognitionResult
            val activity = result.mostProbableActivity
            when (activity.type) {
                DetectedActivity.ON_FOOT -> {
                    ContextsHandler.setCurrentActivity("Walking")
                }
                DetectedActivity.WALKING -> {
                    ContextsHandler.setCurrentActivity("Walking")
                }
                DetectedActivity.IN_VEHICLE -> {
                    ContextsHandler.setCurrentActivity("On the Road")
                }
                else -> {
                    ContextsHandler.setCurrentActivity("")
                }
            }
        }
    }
}