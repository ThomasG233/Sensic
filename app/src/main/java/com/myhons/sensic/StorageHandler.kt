package com.myhons.sensic

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object StorageHandler
{
    private lateinit var sharedPreferences : SharedPreferences

    fun saveToStorage(label : String, data : Boolean)
    {
        sharedPreferences.edit {
            apply()
            {
                putBoolean(label, data)
            }
        }
    }

    fun saveToStorage(label : String, data : String)
    {
        sharedPreferences.edit {
            apply()
            {
                putString(label, data)
            }
        }
    }

    fun loadFromStorage(label : String)
    {

    }

    fun darkThemeEnabled() : Boolean
    {
        return sharedPreferences.getBoolean("darkTheme", false)
    }
}