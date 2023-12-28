package com.waywardTeam.wayward


import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.wayward.R
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.waywardTeam.wayward.utilities.*
import java.time.Duration
import java.time.LocalTime
import kotlin.concurrent.thread


@Suppress("SameParameterValue")
class RouteSearchActivity : AppCompatActivity() {
    // Differentiating between each input
    private val mapToPositionCode = 1
    private val mapFromPositionCode = 2

    private var fromLatLng: LatLng? = null
    private var toLatLng: LatLng? = null
    private var fromLocation = ""
    private var toLocation = ""

    data class Text(
        val fromPosition: AutocompleteSupportFragment,
        val toPosition: AutocompleteSupportFragment,
        val timeOffset: EditText
    )

    data class Buttons(val search: Button)

    // Late-init variables
    private lateinit var handler: Handler
    private lateinit var text: Text
    private lateinit var button: Buttons
    private lateinit var fileManager: FileManager
    private lateinit var userPreferences: UserPreferences
    private var placesClient: PlacesClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Setting context of the application, used in relative asset positioning
        setContentView(R.layout.activity_route_search)

        // Initializing variables
        handler = Handler(Looper.getMainLooper())
        text = Text(
            supportFragmentManager.findFragmentById(R.id.fromPositionTxt) as AutocompleteSupportFragment,
            supportFragmentManager.findFragmentById(R.id.toPositionTxt) as AutocompleteSupportFragment,
            findViewById(R.id.timeOffsetTxt)
        )
        button = Buttons(findViewById(R.id.searchBtn))
        fileManager = FileManager(this)
        userPreferences = fileManager.readPreferences()!!

        // Places for place searching
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.MAPS_API_KEY))
        }
        // Create a new Places client instance.
        placesClient = Places.createClient(this)
        text.fromPosition.setPlaceFields(
            listOf(
                Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG
            )
        )

        // Listeners
        text.fromPosition.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onError(p0: Status) {}

            override fun onPlaceSelected(p0: Place) {
                fromLocation = p0.name?.toString() ?: ""
                processData()
            }
        })

        text.toPosition.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onError(p0: Status) {}

            override fun onPlaceSelected(p0: Place) {
                toLocation = p0.name?.toString() ?: ""
                processData()
            }
        })

        val fromPositionButton: ImageButton = findViewById(R.id.fromPositionBtn)
        fromPositionButton.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("task", "get")
            startActivityForResult(intent, mapFromPositionCode)
        }

        val toPositionButton: ImageButton = findViewById(R.id.toPositionBtn)
        toPositionButton.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("task", "get")
            startActivityForResult(intent, mapToPositionCode)
        }

        button.search.setOnClickListener {
            thread(start = true) {
                val db = FirebaseFirestore.getInstance()

                val docRef = db.collection("Country").document("Slovakia")
                docRef.get().addOnFailureListener { exception ->
                    println("Database failed: $exception")
                }.addOnSuccessListener { document ->
                    searchRoute(document)
                }
            }
        }

        text.timeOffset.setText(userPreferences.timeBetweenWaiting.toString())
        text.timeOffset.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                try {
                    userPreferences.timeBetweenWaiting = s.toString().toLong()
                    fileManager.savePreferences(userPreferences)
                } catch (_: Exception) {
                }
            }
        })
    }

    // function that will handle everything around route searching, as argument it will have a document from a database
    private fun searchRoute(document: DocumentSnapshot) {
        if (document.data == null) return
        if (fromLatLng == null || toLatLng == null) return

        val stops = mutableListOf<Stop>()

        for ((city, markers) in document.data!!) {
            val markerList = markers as? List<Map<String, Any>> ?: continue

            for (markerData in markerList) {
                val name = markerData["name"] as? String ?: ""
                val location = markerData["location"] as? String ?: ""
                val types = (markerData["type"] as? List<*>)?.map { it.toString() }?.toTypedArray() ?: emptyArray()

                val stop = Stop(name, location, types)
                stops.add(stop)
            }
        }

        val fromStop = Miscellaneous().findClosestMarker(fromLatLng!!, stops)
        val toStop = Miscellaneous().findClosestMarker(toLatLng!!, stops)

        if (fromStop == null || toStop == null) return
        // Getting html page and its content
        val link = "cp.hnonline.sk/vlakbusmhd/spojenie/vysledky/"
        thread(start = true) {
            val routes = mutableListOf<PolylineRoute>()
            // Finding the first possible route
            // From starting position to first stop
            val (fromRoute, fromDuration) = Miscellaneous().getDirections(
                this@RouteSearchActivity, fromLatLng!!, Convert().toLatLng(fromStop.location)
            )
            // Between stops
            val (publicRoute, publicDuration) = Miscellaneous().getDirections(
                this@RouteSearchActivity,
                Convert().toLatLng(fromStop.location),
                Convert().toLatLng(toStop.location),
                "driving"
            )
            // From last stop to destination
            val (toRoute, toDuration) = Miscellaneous().getDirections(
                this@RouteSearchActivity, Convert().toLatLng(toStop.location), toLatLng!!
            )
            routes.add(PolylineRoute(fromRoute, Color.GREEN))
            routes.add(PolylineRoute(publicRoute, Color.RED))
            routes.add(PolylineRoute(toRoute, Color.GREEN))
            // Loading user specified delay

            // Calculating time
            val delay: Long = userPreferences.timeBetweenWaiting
            val timeToWalk = fromDuration.getString("value").toLong() / 60
            // Get current time
            val currentTime = LocalTime.now()
            // Calculating the time at the station
            val timeAtStation = currentTime.plusMinutes(timeToWalk).plusMinutes(delay)
            val route = getData(link, fromStop.name, toStop.name, timeAtStation)
            if (route != null) {
                val transportArrive = route[0].route[0].time
                val timeToDeparture = transportArrive.minusMinutes(timeToWalk).minusMinutes(delay)
                val freeTime = Duration.between(currentTime, timeToDeparture)

                // Notification
                Miscellaneous().showNotification(
                    this, 1, "Máte ešte ${Convert().formatTime(freeTime)} minút voľného času"
                )
                Handler(Looper.getMainLooper()).postDelayed({
                    Miscellaneous().showNotification(this, 0, "Je čas vyraziť")
                }, freeTime.toMillis())

                // Launching map
                val intent = Intent(this, MapActivity::class.java)
                intent.putExtra("task", "show")
                intent.putParcelableArrayListExtra("routes", ArrayList(routes))
                startActivityForResult(intent, mapToPositionCode)
            }
        }
    }

    // Function for enabling search button
    private fun processData() {
        button.search.isEnabled = (fromLatLng != null) && (toLatLng != null)
    }

    // Function for getting html page, trimming it and formatting it to proper data structure
    private fun getData(link: String, from: String, to: String, time: LocalTime): MutableList<Transportation>? {
        // Getting html page
        val page = Internet().getPublicRoute(link, from, to, Convert().formatTime(time)) ?: return null

        // removing unnecessary parts of the page
        val trimmedPage = Trimming(page).run()

        // Converting it to a proper data structure
        return Convert().toDataClass(trimmedPage)
    }

    // This function is called whenever user finishes permission allowing
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        when (requestCode) {
            mapFromPositionCode -> {
                val mapData: MapData? = data?.getParcelableExtra("location")
                fromLatLng = mapData?.location
                text.fromPosition.setText(mapData?.name)
                processData()
            }

            mapToPositionCode -> {
                val mapData: MapData? = data?.getParcelableExtra("location")
                toLatLng = mapData?.location
                text.toPosition.setText(mapData?.name)
                processData()
            }
        }
    }
}
