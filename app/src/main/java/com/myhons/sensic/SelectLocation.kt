package com.myhons.sensic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Geocoder.GeocodeListener
import android.location.Location
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

// Implemented based on this tutorial: https://www.youtube.com/watch?v=JzxjNNCYt_o and https://www.youtube.com/watch?v=dcJ5DqYqBNA
class SelectLocation : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var gMap : GoogleMap
    private lateinit var map : FrameLayout
    private lateinit var btnConfirmLocation : Button
    private lateinit var fusedClient : FusedLocationProviderClient
    private lateinit var searchView : SearchView
    private var geoCoder = Geocoder(this, Locale.getDefault())
    private var coordsToReturn = LatLng(0.0,0.0)
    private var locationName = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_select_location)

        btnConfirmLocation = findViewById(R.id.btnConfirmLocation)
        map = findViewById(R.id.map)
        searchView = findViewById(R.id.searchView)

        val savedCoordinates = ContextsHandler.getContext<LocationContext>("Location", "Location").getCoordinates()
        coordsToReturn = LatLng(savedCoordinates.getLatitude(), savedCoordinates.getLongitude())
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(GPS_PROVIDER)
        if(coordsToReturn.latitude == 0.0 && coordsToReturn.longitude == 0.0)
        {
            if(gpsEnabled)
            {
                getLocation()
            }
            else
            {
                initialiseMap()
            }

        }
        else
        {
            val location = geoCoder.getFromLocation(coordsToReturn.latitude, coordsToReturn.longitude, 1) as List<Address>
            if(location.isNotEmpty())
            {
                locationName = getLocationLabel(location[0])
            }
            else
            {
                locationName = "Selected Location"
            }
            initialiseMap()
        }

        btnConfirmLocation.setOnClickListener {
            ContextsHandler.getContext<LocationContext>("Location", "Location").setCoordinates(coordsToReturn.latitude, coordsToReturn.longitude)
            intent.putExtra("latitude", coordsToReturn.latitude)
            intent.putExtra("longitude", coordsToReturn.longitude)
            if(locationName == "Your Current Location")
            {
                val address = geoCoder.getFromLocation(coordsToReturn.latitude, coordsToReturn.longitude, 1) as List<Address>
                if(address.isNotEmpty())
                {
                    val currentAddress = address[0]
                    locationName = getLocationLabel(currentAddress)
                }
                else
                {
                    locationName = "this saved location"
                }

            }
            intent.putExtra("name", locationName)
            setResult(RESULT_OK, intent)
            finish()
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            override fun onQueryTextSubmit(query : String) : Boolean {
                val location = searchView.query.toString()
                try
                {
                    val searchedAddress = geoCoder.getFromLocationName(location, 1) as List<Address>
                    if (searchedAddress.isNotEmpty())
                    {
                        val latLng = LatLng(searchedAddress[0].latitude, searchedAddress[0].longitude)
                        val addressName = getLocationLabel(searchedAddress[0])
                        placeMarker(latLng, addressName)
                        return true
                    }
                    else
                    {
                        Toast.makeText(applicationContext, "Address not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                catch(e : Exception)
                {
                    e.printStackTrace()
                }
                return false
            }
        })

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(map.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
        }
        val task = fusedClient.lastLocation

        task.addOnSuccessListener { location: Location ->
            coordsToReturn = LatLng(location.latitude, location.longitude)
            locationName = "Your Current Location"
            initialiseMap()
        }
    }

    private fun initialiseMap()
    {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(p0: GoogleMap) {
        gMap = p0
        if(coordsToReturn != LatLng(0.0,0.0))
        {
            placeMarker(coordsToReturn, locationName)
        }


        gMap.setOnMapClickListener { point : LatLng ->
            val location = geoCoder.getFromLocation(point.latitude, point.longitude, 1) as List<Address>
            if(locationName.isEmpty())
            {
                placeMarker(point, "Selected Location")
                locationName = "Selected Location"
            }
            else
            {
                val addressName = getLocationLabel(location[0])
                locationName = addressName
                placeMarker(point, addressName)
            }

            Log.d("DEBUG", "${point.latitude}, ${point.longitude}")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 101)
        {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                getLocation()
            }
            else
            {
                // Handle Error
                Log.e("LOCATION", "Permissions not granted")
                Toast.makeText(applicationContext, "Please enable location permissions in order to make use of this feature.", Toast.LENGTH_LONG).show()
                setResult(RESULT_CANCELED, intent)
                finish()
            }
        }
    }

    private fun getLocationLabel(address : Address) : String
    {
        val fullAddress = address.getAddressLine(0)
        val addressFirstLine = fullAddress.split(",")[0]
        return addressFirstLine
    }

    private fun placeMarker(location : LatLng, name : String)
    {
        val markerOptions = MarkerOptions().position(location).title(name)
        markerOptions.icon(
            BitmapDescriptorFactory.defaultMarker(
                BitmapDescriptorFactory.HUE_ORANGE
            )
        )
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, 15f)
        gMap.clear()
        gMap.animateCamera(cameraUpdate)
        gMap.addMarker(markerOptions)
        coordsToReturn = location
    }
}

