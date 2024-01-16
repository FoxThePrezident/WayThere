package com.waywardTeam.wayward


import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.wayward.R
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.waywardTeam.wayward.utilities.*
import java.time.Duration
import java.time.LocalTime
import java.util.*
import kotlin.concurrent.thread


@Suppress("SameParameterValue")
class RouteSearchActivity : AppCompatActivity() {

    // Differentiating between each input
    private val mapToPositionCode = 1
    private val mapFromPositionCode = 2

    // Settings for searching route
    private var byArrive = false
    private var fromLatLng: LatLng? = null
    private var toLatLng: LatLng? = null
    private var fromLocation = ""
    private var toLocation = ""
    private var arrivalTime: LocalTime? = null

    // Data classes
    // For text related views
    data class Text(
        val fromPosition: AutocompleteSupportFragment,
        val toPosition: AutocompleteSupportFragment,
    )

    // Option sections
    // Separated with the intention of moving this section into different view
    data class Options(
        val timeOffset: EditText,
        val transOption: Spinner,
        val arriveTimeEditText: EditText,
        val typeOfRouteSearch: RadioGroup
    )

    // Buttons
    data class Buttons(val search: Button)

    // Late-init variables
    // Views
    private lateinit var text: Text
    private lateinit var options: Options
    private lateinit var button: Buttons
    private lateinit var loading: ProgressBar

    // Custom classes
    private lateinit var convert: Convert
    private lateinit var internet: Internet
    private lateinit var fileManager: FileManager
    private lateinit var notification: Notification
    private lateinit var miscellaneous: Miscellaneous
    private lateinit var userSettings: UserSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Setting context of the application, used in relative asset positioning
        setContentView(R.layout.activity_route_search)

        // Initializing variables
        text = Text(
            supportFragmentManager.findFragmentById(R.id.fromPositionTxt) as AutocompleteSupportFragment,
            supportFragmentManager.findFragmentById(R.id.toPositionTxt) as AutocompleteSupportFragment
        )
        options = Options(
            findViewById(R.id.timeOffsetTxt),
            findViewById(R.id.transOptions),
            findViewById(R.id.arriveTime),
            findViewById(R.id.typeOfRouteSearch)
        )
        button = Buttons(findViewById(R.id.searchBtn))
        loading = findViewById(R.id.loading)
        // Loading custom classes
        convert = Convert()
        internet = Internet(this)
        fileManager = FileManager(this)
        notification = Notification()
        miscellaneous = Miscellaneous(this)
        userSettings = fileManager.readPreferences()!!

        // Places for place searching
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.MAPS_API_KEY))
        }
        // Create a new Places client instance.
        Places.createClient(this)
        text.fromPosition.setPlaceFields(
            listOf(
                Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG
            )
        )

        // Setting up listeners and views
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

        // Buttons for an opening map and searching
        val fromPositionButton: ImageButton = findViewById(R.id.fromPositionBtn)
        fromPositionButton.setOnClickListener { buttonOnClickListener(mapFromPositionCode) }

        val toPositionButton: ImageButton = findViewById(R.id.toPositionBtn)
        toPositionButton.setOnClickListener { buttonOnClickListener(mapToPositionCode) }

        // Search route button
        button.search.setOnClickListener {
            loading.visibility = View.VISIBLE
            thread(start = true) {
                val db = FirebaseFirestore.getInstance()

                val docRef = db.collection("Country").document("Slovakia")
                docRef.get().addOnFailureListener { exception ->
                    loading.visibility = View.INVISIBLE
                    println("Database failed: $exception")
                }.addOnSuccessListener { document ->
                    searchRoute(document)
                    loading.visibility = View.INVISIBLE
                }
            }
        }

        // Input for time offset
        options.timeOffset.setText(userSettings.timeBetweenWaiting.toString())
        options.timeOffset.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                try {
                    userSettings.timeBetweenWaiting = s.toString().toLong()
                    fileManager.savePreferences(userSettings)
                } catch (_: Exception) {
                }
            }
        })

        // Time input arrival time
        options.arriveTimeEditText.setText(
            getString(
                R.string.time_format, LocalTime.now().hour, LocalTime.now().minute
            )
        )
        options.arriveTimeEditText.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                options.arriveTimeEditText.setText(getString(R.string.time_format, hour, minute))
                arrivalTime = LocalTime.of(hour, minute)
                processData()
            }
            TimePickerDialog(
                this, timeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
            ).show()
        }

        // How fast we want to get to the desired destination
        val publicTxt = findViewById<TextView>(R.id.publicTxt)
        publicTxt.text = resources.getStringArray(R.array.transOptionTxt)[2]
        options.typeOfRouteSearch.setOnCheckedChangeListener { _, id ->
            // Now perform the switch case based on the selected radio button
            when (id) {
                // If "Destination ASAP" is selected, hide the EditText
                R.id.destinationASAP -> {
                    publicTxt.visibility = View.VISIBLE
                    findViewById<LinearLayout>(R.id.transport).visibility = View.GONE
                    byArrive = false
                }
                // If "Specified Time" is selected, show the EditText
                R.id.specifiedTime -> {
                    publicTxt.visibility = View.GONE
                    findViewById<LinearLayout>(R.id.transport).visibility = View.VISIBLE
                    byArrive = true
                }
            }
        }


        val adapter = ArrayAdapter.createFromResource(
            this, R.array.transOptionTxt, android.R.layout.simple_spinner_item
        ).apply {
            // Specify the layout to use when the list of choices appears.
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // drop-down menu for a type of transport we want to take
        options.transOption.adapter = adapter
        options.transOption.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {}
            override fun onNothingSelected(parentView: AdapterView<*>) {}
        }
    }

    // Extracted function for map button clicks
    private fun RouteSearchActivity.buttonOnClickListener(code: Int) {
        val intent = Intent(this, MapActivity::class.java)
        intent.putExtra("task", "get")
        intent.putExtra("id", code)
        launcher.launch(intent)
    }

    /**
     * Handle everything around route searching, as argument it will have a document from a database
     * @param document from the database call
     */
    private fun searchRoute(document: DocumentSnapshot) {
        if (document.data == null) return
        if (fromLatLng == null || toLatLng == null) {
            return
        }

        val stops = mutableListOf<Stop>()

        // Looping over every object in a document provided by database
        for ((_, markers) in document.data!!) {
            val markerList = when (markers) {
                is List<*> -> markers.filterIsInstance<Map<String, Any>>()
                else -> null
            }

            // Formatting data to proper format
            if (markerList != null) {
                for (markerData in markerList) {
                    val name = markerData["name"] as? String ?: ""
                    val location = markerData["location"] as? String ?: ""
                    val types = (markerData["type"] as? List<*>)?.map { it.toString() }?.toTypedArray() ?: emptyArray()

                    val stop = Stop(name, location, types)
                    stops.add(stop)
                }
            }
        }

        // Finding the closest public transport stops to start and end position
        val fromStop = miscellaneous.findClosestMarker(fromLatLng!!, stops)
        val toStop = miscellaneous.findClosestMarker(toLatLng!!, stops)
        if (fromStop == null || toStop == null) {
            return
        }
        // Getting html page and its content
        val link = "cp.hnonline.sk/vlakbusmhd/spojenie/vysledky/"
        thread(start = true) {
            val routes = mutableListOf<PolylineRoute>()
            // Finding the first possible route
            // Loading user specified delay
            val delay: Long = userSettings.timeBetweenWaiting
            // Get current time
            val currentTime = LocalTime.now()

            val selectedTransportId = options.transOption.selectedItemId.toInt()
            val (fromRoute, fromDuration) = when (selectedTransportId) {
                // From starting position to first stop
                0 -> miscellaneous.getDirections(fromLatLng!!, toLatLng!!)
                // From starting position to first stop
                1 -> miscellaneous.getDirections(fromLatLng!!, toLatLng!!, "driving")
                else -> {
                    // From starting position to first stop
                    val data = miscellaneous.getDirections(
                        fromLatLng!!, convert.toLatLng(fromStop.location)
                    )
                    // Between stops
                    val (publicRoute, _) = miscellaneous.getDirections(
                        convert.toLatLng(fromStop.location), convert.toLatLng(toStop.location), "driving"
                    )
                    // From last stop to destination
                    val (toRoute, _) = miscellaneous.getDirections(
                        convert.toLatLng(toStop.location), toLatLng!!
                    )
                    routes.add(PolylineRoute(publicRoute, Color.RED))
                    routes.add(PolylineRoute(toRoute, Color.GREEN))

                    data
                }
            }
            routes.add(PolylineRoute(fromRoute, Color.GREEN))

            // Calculating time
            val timeToWalk = fromDuration / 60
            val timeAtStation = currentTime.plusMinutes(timeToWalk).plusMinutes(delay)
            if (selectedTransportId == 3) {
                val route = getData(link, fromStop.name, toStop.name, timeAtStation) ?: return@thread
                arrivalTime = route[0].route[0].time
            }
            if (arrivalTime == null) {
                return@thread
            }
            val timeToDeparture = arrivalTime!!.minusMinutes(timeToWalk).minusMinutes(delay)
            val freeTime = Duration.between(currentTime, timeToDeparture)

            // Notification
            val hours = freeTime.toHours()
            val minutes = freeTime.toMinutes() - (hours * 24)
            var notData = NotificationData(
                NotificationManager.IMPORTANCE_DEFAULT,
                getString(R.string.freeTime, getString(R.string.time_format, hours, minutes))
            )
            notification.send(this, notData)
            notData = NotificationData(NotificationManager.IMPORTANCE_HIGH, getString(R.string.timeToDeparture))
            notification.delay(this, notData, freeTime)

            // Launching map
            loading.visibility = View.INVISIBLE
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("task", "show")
            intent.putParcelableArrayListExtra("routes", ArrayList(routes))
            launcher.launch(intent)
//            startActivityForResult(intent, mapToPositionCode)
        }
        loading.visibility = View.INVISIBLE
    }

    /**
     * Checking if search button could be enabled
     */
    private fun processData() {
        button.search.isEnabled = (fromLatLng != null) && (toLatLng != null) && (arrivalTime != null)
    }

    /**
     * Handling getting data about public transports
     * @param link for a page that we want to communicate to
     * @param from name of a station that we want to depart from
     * @param to name of a station that we want to arrive to
     * @param time of departure/arrival
     * @return list of public transport stops
     */
    private fun getData(link: String, from: String, to: String, time: LocalTime): MutableList<Transportation>? {
        // Getting html page
        val page =
            internet.getPublicRoute(link, from, to, getString(R.string.time_format, time.hour, time.minute), byArrive)
                ?: return null

        // removing unnecessary parts of the page
        val trimmedPage = Trimming(page).run()

        // Converting it to a proper data structure
        return convert.toDataClass(trimmedPage)
    }

    /**
     * Launcher variable used for launching other activities and managing returned codes
     */
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        when (result.data?.getIntExtra("id", 0)) {
            mapFromPositionCode -> {
                val mapData: MapData? = result.data?.getParcelableExtra("location")
                fromLatLng = mapData?.location
                text.fromPosition.setText(mapData?.name)
                processData()
            }

            mapToPositionCode -> {
                val mapData: MapData? = result.data?.getParcelableExtra("location")
                toLatLng = mapData?.location
                text.toPosition.setText(mapData?.name)
                processData()
            }
        }
    }
}
