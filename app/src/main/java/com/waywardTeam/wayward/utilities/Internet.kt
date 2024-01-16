package com.waywardTeam.wayward.utilities

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat.getString
import com.example.wayward.R
import com.google.android.gms.maps.model.LatLng
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Class for managing networking
 * @property context from an activity currently working from
 */
class Internet(private val context: Context) {
    /**
     * Read from html address page, contains try-catch in it
     * @param url is a page that will load from
     * @return html page
     */
    private fun getPage(url: String): String? {
        try {
            val urlObj = URL(url)
            val connection = urlObj.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36"
            )

            val responseCode = connection.responseCode

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }

            reader.close()
            return response.toString()

        } catch (e: IOException) {
            return null
        }
    }

    /**
     * Will get from Google api details about location
     * @param latLng location of the place that we want details about
     * @return name of the address
     */
    fun fromLatLngToName(latLng: LatLng): String? {
        val apiKey = getString(context, R.string.MAPS_API_KEY)
        val url =
            "https://maps.googleapis.com/maps/api/geocode/json?latlng=${latLng.latitude},${latLng.longitude}&key=$apiKey"

        try {
            val response = getPage(url) ?: return null

            // Parse JSON response
            val jsonResponse = JSONObject(response)
            val results = jsonResponse.getJSONArray("results")

            if (results.length() > 0) {
                val address = results.getJSONObject(0).getString("formatted_address")
                return address
            } else {
                return null
            }
        } catch (e: JSONException) {
            Log.e("GeocodingAPI", "Error parsing JSON", e)
            return null
        }
    }

    /**
     * Get information about route from pint A to point B
     * @param originLoc from where we want route
     * @param destLoc to where we want route
     * @param mode by which mode we want to travel, like walking or driving
     * @return JSON string about information about route
     */
    fun getDirection(originLoc: LatLng, destLoc: LatLng, mode: String): String? {
        val apiKey = getString(context, R.string.MAPS_API_KEY)
        // Build the request URL
        val origin = "${originLoc.latitude},${originLoc.longitude}"
        val dest = "${destLoc.latitude},${destLoc.longitude}"
        val url =
            "https://maps.googleapis.com/maps/api/directions/json?origin=$origin&destination=$dest&mode=$mode&key=$apiKey"
        return getPage(url)
    }

    /**
     * Will get information about public transport
     * @param page name of the page we want information from
     * @param fromStation origin station
     * @param toStation destination station
     * @param time for time of departure/arrive
     * @param byArrive true/false defining if time given is time that we want to depart or time that we want to be at end station
     * @return html page
     */
    fun getPublicRoute(
        page: String, fromStation: String, toStation: String, time: String, byArrive: Boolean
    ): String? {
        val phpParams = listOfNotNull(time.let { "time=$it" },
            fromStation.let { "f=$it" },
            toStation.let { "t=$it" },
            byArrive.let { "byarr=$byArrive" })

        val link = StringBuilder("https://$page?${phpParams.joinToString("&")}")
        return getPage(link.toString().replace(" ", "+"))
    }
}