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

// class for managing networking
class Internet {
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

    fun fromLatLngToName(context: Context, latLng: LatLng): String? {
        val apiKey = getString(context, R.string.MAPS_API_KEY)
        val url =
            "https://maps.googleapis.com/maps/api/geocode/json?latlng=${latLng.latitude},${latLng.longitude}&key=$apiKey"

        try {
            val response = getPage(url) ?: return null

            // Parse JSON response
            val jsonResponse = JSONObject(response.toString())
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

    fun getDirection(context: Context, originLoc: LatLng, destLoc: LatLng, mode: String): String? {
        val apiKey = getString(context, R.string.MAPS_API_KEY)
        // Build the request URL
        val origin = "${originLoc.latitude},${originLoc.longitude}"
        val dest = "${destLoc.latitude},${destLoc.longitude}"
        val url =
            "https://maps.googleapis.com/maps/api/directions/json?origin=$origin&destination=$dest&mode=$mode&key=$apiKey"
        return getPage(url)
    }

    fun getPublicRoute(
        page: String, fromStation: String? = null, toStation: String? = null, time: String? = null
    ): String? {
        val phpParams = mutableListOf<String>()
        // building php parameters
        if (time != null) phpParams.add("time=$time")
        if (fromStation != null) phpParams.add("f=$fromStation")
        if (toStation != null) phpParams.add("t=$toStation")

        val link = StringBuilder().append("https://").append(page).append("?")
            .append(String(phpParams[0].toByteArray(), Charsets.UTF_8))
        phpParams.removeFirst()
        for (param in phpParams) {
            link.append("&" + String(param.toByteArray(), Charsets.UTF_8))
        }
        val link1 = link.toString().replace(" ", "+")
        return getPage(link1)
    }
}