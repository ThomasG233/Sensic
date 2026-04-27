package com.myhons.sensic

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
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

/**
 * Allows the user to select locations to be saved.
 * Implemented with the assistance of these tutorials: https://www.youtube.com/watch?v=JzxjNNCYt_o and https://www.youtube.com/watch?v=dcJ5DqYqBNA
  */

class SelectLocation : AppCompatActivity(), OnMapReadyCallback {

    // Late initialise all UI elements.
    private lateinit var searchView : SearchView
    private lateinit var gMap : GoogleMap
    private lateinit var map : FrameLayout
    private lateinit var btnConfirmLocation : Button
    // Used to get the user's current location.
    private lateinit var fusedClient : FusedLocationProviderClient
    // Used to get the information about a given location.
    private var geoCoder = Geocoder(this, Locale.getDefault())
    // Data to be returned to the user.
    private var coordsToReturn = LatLng(0.0,0.0)
    private var locationName = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_select_location)
        // Initialise all UI elements.
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation)
        map = findViewById(R.id.map)
        searchView = findViewById(R.id.searchView)
        // Get the latitude and longitude passed from the AdjustContext screen.
        val parsedLatitude = intent.getDoubleExtra("latitude", 0.0)
        val parsedLongitude = intent.getDoubleExtra("longitude", 0.0)
        // Set the current coordinates.
        coordsToReturn = LatLng(parsedLatitude, parsedLongitude)
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        // Check if the GPS is enabled.
        val locationManager = applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(GPS_PROVIDER)

        if(coordsToReturn.latitude == 0.0 && coordsToReturn.longitude == 0.0)
        {
            if(gpsEnabled)
            {
                // Use the current location of the user.
                getLocation()
            }
            else
            {
                // Initialise using the saved coordinates.
                initialiseMap()
            }
        }
        else
        {
            // Get the address for the location at the coordinates.
            val location = geoCoder.getFromLocation(coordsToReturn.latitude, coordsToReturn.longitude, 1) as List<Address>
            if(location.isNotEmpty())
            {
                // Set the location name, if one exists.
                locationName = getLocationLabel(location[0])
            }
            else
            {
                // Mark the location as the "selected location."
                locationName = "Selected Location"
            }
            initialiseMap()
        }
        btnConfirmLocation.setOnClickListener {
            // Return the coordinates selected by the user to the AdjustContext activity.
            intent.putExtra("latitude", coordsToReturn.latitude)
            intent.putExtra("longitude", coordsToReturn.longitude)
            // Get the location name of these coordinates.
            if(locationName == "Your Current Location")
            {
                // Get the address.
                val address = geoCoder.getFromLocation(coordsToReturn.latitude, coordsToReturn.longitude, 1) as List<Address>
                if(address.isNotEmpty())
                {
                    val currentAddress = address[0]
                    // Set the location name.
                    locationName = getLocationLabel(currentAddress)
                }
                else
                {
                    locationName = "this saved location"
                }
            }
            // Return the location info.
            intent.putExtra("name", locationName)
            setResult(RESULT_OK, intent)
            finish()
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            // When a location is entered.
            override fun onQueryTextSubmit(query : String) : Boolean {
                // Get the address entered by the user.
                val location = searchView.query.toString()
                try
                {
                    // Search for this location in the world.
                    val searchedAddress = geoCoder.getFromLocationName(location, 1) as List<Address>
                    if (searchedAddress.isNotEmpty())
                    {
                        // Location found. Get the address saved on the map.
                        val latLng = LatLng(searchedAddress[0].latitude, searchedAddress[0].longitude)
                        val address = searchedAddress[0]
                        locationName = getLocationLabel(address)
                        // Place the marker for this location on the map.
                        placeMarker(latLng, locationName)
                        return true
                    }
                    else
                    {
                        // Address is not a valid location.
                        Toast.makeText(applicationContext, "Address not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                catch(e : Exception)
                {
                    Log.e("SearchError", "$e")
                }
                return false
            }
            // Function is required upon declaration of a listener, despite not being used.
            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(map.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Get the user's current location, if permissions have been provided.
     */
    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // Make a request for the current location.
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
        }
        // Get the device's last saved location.
        val task = fusedClient.lastLocation
        // If the location could be obtained, save the locational data.
        task.addOnSuccessListener { location: Location ->
            coordsToReturn = LatLng(location.latitude, location.longitude)
            locationName = "Your Current Location"
            initialiseMap()
        }
        // Could not get the current location. Initialise the map anyway.
        task.addOnFailureListener {
            initialiseMap()
        }
    }

    /**
     * Initialises the map.
     */
    private fun initialiseMap()
    {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Called when the map is finished loading.
     */
    override fun onMapReady(p0: GoogleMap) {
        gMap = p0
        // If the coordinates are not set to Null Island, place a marker.
        if(coordsToReturn != LatLng(0.0,0.0))
        {
            placeMarker(coordsToReturn, locationName)
        }
        // If the map is tapped...
        gMap.setOnMapClickListener { point : LatLng ->
            // Get the data for the location tapped on the map.
            val location = geoCoder.getFromLocation(point.latitude, point.longitude, 1) as List<Address>
            if(locationName.isEmpty())
            {
                // Place a marker on this location with a preset name.
                placeMarker(point, "Selected Location")
                locationName = "Selected Location"
            }
            else
            {
                // Place a marker on this location with the address name.
                val addressName = getLocationLabel(location[0])
                locationName = addressName
                placeMarker(point, addressName)
            }
        }
    }

    /**
     * Checks the result of the location request.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 101)
        {
            // If permission has been provided for the user.
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                // Get the user's current location.
                getLocation()
            }
            else
            {
                // Inform the user that location permissions are required.
                Toast.makeText(applicationContext, "Please enable location permissions in order to make use of this feature.", Toast.LENGTH_LONG).show()
                setResult(RESULT_CANCELED, intent)
                finish()
            }
        }
    }

    /**
     * Get the first line of the address of a given location.
     * @param address: The full address of the location.
     */
    private fun getLocationLabel(address : Address) : String
    {
        val fullAddress = address.getAddressLine(0)
        // Takes the first line of the address as a substring.
        val addressFirstLine = fullAddress.split(",")[0]
        return addressFirstLine
    }

    /**
     * Places a marker on the map when prompted.
     * @param coordinates: The coordinates where the marker should be displayed.
     * @param name: The location name to be displayed when the marker is tapped.
     */
    private fun placeMarker(coordinates : LatLng, name : String)
    {
        // Set the location of the marker, along with the name.
        val markerOptions = MarkerOptions().position(coordinates).title(name)
        // Set the appearance of the marker on the map.
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        // Zoom in on where the marker is.
        val zoom = CameraUpdateFactory.newLatLngZoom(coordinates, 15f)
        // Remove the previous marker, add a new one, then zoom in on it.
        gMap.clear()
        gMap.addMarker(markerOptions)
        gMap.animateCamera(zoom)
        coordsToReturn = coordinates
    }
}

