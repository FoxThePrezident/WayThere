package com.wayThereTeam.wayThere.utilities

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

/**
 * Handling database related actions
 * @param context of the activity
 */
class Database(private var context: Context) {
    private var db = FirebaseFirestore.getInstance()
    private var fileManager = FileManager(context)
    private var userSettings = fileManager.readPreferences()!!

    /**
     * Will check if a user has stored database information, it is to prevent calling multiple times just for the same data
     * If data is stored in memory, it will load those
     * Otherwise it will request a new one
     * @param force request from database
     * @return List of stops
     */
    fun getStops(force: Boolean = false): List<Stop> {
        // Getting current time
        val currentDateTime = LocalDate.now()
        val stops: List<Stop>
        // Checking if we surpassed expiration date set on data
        if (currentDateTime.isAfter(userSettings.dbExpiration) || force) {
            // If yes, then request a new one
            val rawData = getData("Country", "Slovakia")
            stops = formatData(rawData)
            // Updating memory
            userSettings.stopsDB = stops
            // Adding one more day to expiration, later it will be more when data in a database stabilize
            userSettings.dbExpiration = LocalDate.now().plusDays(1)
            // Saving it
            fileManager.savePreferences(userSettings)
        } else {
            // Loading stops from memory
            stops = userSettings.stopsDB
        }

        return stops
    }

    /**
     * Formatting data from database to useful format
     * @param rawData that needs to be formatted
     * @return formatted list of stops
     */
    private fun formatData(rawData: Map<String, Any>): List<Stop> {
        val stops = mutableListOf<Stop>()

        // Looping through each list, it is getting the name of the City and its content
        // We are not using the name, so we just store it as _
        for ((_, stopsData) in rawData) {
            val stopsList = stopsData as List<Map<String, Any>>

            for (stopData in stopsList) {
                val name = stopData["name"] as String
                val location = stopData["location"] as String
                val type = stopData["type"] as List<*>

                val stop = Stop(name, location, type)
                stops.add(stop)
            }
        }

        return stops
    }

    /**
     * requesting data from database
     * @param collection name of a collection, that groups documents together
     * @param document name of a document, that we want to select from
     * @return Hash map of stops
     */
    @Suppress("SameParameterValue")
    private fun getData(collection: String, document: String): Map<String, Any> {
        val docRef = db.collection(collection).document(document)

        try {
            val doc = Tasks.await(docRef.get())
            if (doc == null || doc.data == null) {
                throw NullPointerException("Document or data is null")
            }

            return doc.data!!
        } catch (exception: Exception) {
            println("Database failed: $exception")
            throw exception
        }
    }
}
