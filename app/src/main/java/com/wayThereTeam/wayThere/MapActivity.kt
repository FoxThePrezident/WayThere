package com.wayThereTeam.wayThere

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.wayThereTeam.wayThere.databinding.ActivityMapsBinding
import com.wayThereTeam.wayThere.utilities.*
import kotlin.concurrent.thread


class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private var id: Int = 0
    private lateinit var task: String

    private val locationPermissionRequestCode = 1

    private lateinit var googleMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var searchButton: Button
    private var pickedName = ""
    private lateinit var pickedLocation: LatLng

    // Visual stuff for a map
    private var longPressMarker: Marker? = null

    // Custom classes
    private lateinit var convert: Convert
    private lateinit var internet: Internet
    private lateinit var database: Database

    // Function for setup activity on a first launch
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Getting information from intent that was provided by calling activity
        task = intent.getStringExtra("task").toString()
        id = intent.getIntExtra("id", 0)

        convert = Convert()
        internet = Internet(this)
        database = Database(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Button for handling returning to route search activity
        searchButton = findViewById(R.id.search_button)
        searchButton.setOnClickListener {
            val resultIntent = Intent()
            thread(start = true) {
                // Checking if we got name of the place
                if (pickedName == "") {
                    val response = internet.fromLatLngToName(pickedLocation)
                    if (response != null) {
                        pickedName = response
                    }
                }
                resultIntent.putExtra("location", MapData(pickedName, pickedLocation))
                resultIntent.putExtra("id", id)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(googleMap: GoogleMap) {
        // Setting up a map
        this.googleMap = googleMap
        this.googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))

        // Settings for a map
        val uiSettings = googleMap.uiSettings
        uiSettings.isZoomControlsEnabled = true
        uiSettings.isMapToolbarEnabled = true

        // Set the long click listener
        googleMap.setOnMapLongClickListener(this)
        googleMap.setOnMarkerClickListener { clickedMarker ->
            // Remove the previous marker if it exists
            longPressMarker?.remove()
            // Storing current picked position
            pickedName = clickedMarker.title.toString()
            pickedLocation = clickedMarker.position
            // Setting button to visible
            searchButton.visibility = View.VISIBLE

            // Retuning false, so we are indicating that we did not consume listener
            return@setOnMarkerClickListener false
        }

        // Setting up a default camera center and zoom, in case of not allowing location services
        // for now, it is centered on my school that I studied in
        val mySchool = LatLng(49.055834, 20.279786)
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mySchool, 18f))

        // Checking if application has permissions to access location
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionRequestCode
            )
        } else {
            // Permission is already granted
            locationHandle()
        }

        thread(start = true) {
            markerHandle()
        }

        // Checking if this activity was called with intention of showing final route
        if (task == "show") {
            val routes = intent.getParcelableArrayListExtra<PolylineRoute>("routes")
            if (routes != null) {
                val boundsBuilder = LatLngBounds.builder()
                // Looping for each route part
                for (route in routes) {
                    // Showing it on a map
                    val polylineOptions = PolylineOptions().apply {
                        color(route.color)
                        width(10f)
                        addAll(route.route)
                    }
                    googleMap.addPolyline(polylineOptions)
                    route.route.forEach { boundsBuilder.include(it) }
                }
                // Centering map based on a full route
                val bounds = boundsBuilder.build()
                val padding = 100
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            }
        }
    }

    /**
     * Handling all location-related stuff in case that location is allowed
     */
    @SuppressLint("MissingPermission")
    private fun locationHandle() {
        googleMap.isMyLocationEnabled = true

        // Checking if we need current location
        if (task == "get") {
            getLastKnownLocation(this) { location ->
                if (location != null) {
                    // Handle the location, for example, update the UI or use it for map markers.
                    val currentLocation = LatLng(location.latitude, location.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 18f))
                } else {
                    // Location is null; handle this case, perhaps by requesting a new location.
                }
            }
        }
    }

    /**
     * Handling marker related things
     */
    private fun markerHandle() {
        val markers = database.getStops()
        for (marker in markers) {
            // Setting up custom icon for marker
            val icon = if (marker.type.contains("train")) {
                R.drawable.baseline_train_24
            } else {
                R.drawable.baseline_bus_24
            }
            // Converting it to bitmap
            val drawable = ContextCompat.getDrawable(this, icon)
            val bitmap = Bitmap.createBitmap(
                drawable!!.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            val customMarker = BitmapDescriptorFactory.fromBitmap(bitmap)

            val latLng = convert.toLatLng(marker.location)
            // Add a marker for each stop
            val mark = MarkerOptions().position(latLng).title(marker.name).anchor(0.5f, 0.5f).icon(customMarker)
            runOnUiThread {
                googleMap.addMarker(mark)
            }
        }
    }

    /**
     * Getting the last known location
     * @param context of the application
     * @param onLocationReceived listener
     */
    private fun getLastKnownLocation(context: Context, onLocationReceived: (Location?) -> Unit) {
        // Initialize the FusedLocationProviderClient
        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

        try {
            // Request the last known location
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                // Handle the received location (maybe null)
                onLocationReceived(location)
            }.addOnFailureListener { _ ->
                // Handle any errors that may occur
                onLocationReceived(null)
            }
        } catch (e: SecurityException) {
            // Handle the case where location permissions are not granted
            onLocationReceived(null)
        }
    }

    /**
     * Its called when user longs clicked on the map
     * @param latLng location of the long click
     */
    override fun onMapLongClick(latLng: LatLng) {
        // Remove the previous marker if it exists
        longPressMarker?.remove()

        // Create a new marker at the long-pressed location
        longPressMarker = googleMap.addMarker(
            MarkerOptions().position(latLng)
        )
        if (task == "get") {
            pickedName = ""
            pickedLocation = latLng
            searchButton.visibility = View.VISIBLE
        }
    }

    // Function, that is called when application is calling for a permission
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Checking if permission asking was location related
        if (requestCode == locationPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
                locationHandle()
            } else {
                // Permission denied
            }
        }
    }
}