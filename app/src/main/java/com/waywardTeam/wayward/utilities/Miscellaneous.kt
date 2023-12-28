package com.waywardTeam.wayward.utilities

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import org.json.JSONObject

class Miscellaneous {
    // Function that will calculate the nearest marker from a list
    // Input - location=location that we want to achieve, stops-list of Stops that we will iterate over
    fun findClosestMarker(location: LatLng, stops: MutableList<Stop>): Stop? {
        var closestStop: Stop? = null
        var smallestDistance = Float.MAX_VALUE

        // Looping over each stop
        for (stop in stops) {
            val distance = FloatArray(1)
            val latLng = Convert().toLatLng(stop.location)
            // Calculating distance
            Location.distanceBetween(
                location.latitude, location.longitude, latLng.latitude, latLng.longitude, distance
            )

            // Checking if we found something closer that we already had
            if (distance[0] < smallestDistance) {
                smallestDistance = distance[0]
                closestStop = stop
            }
        }
        return closestStop
    }

    // Function that will request google map direction api for directions to a certain point
    fun getDirections(
        context: Context, from: LatLng, to: LatLng, mode: String = "walking"
    ): Pair<MutableList<LatLng>, JSONObject> {
        val directionsJson = Internet().getDirection(context, from, to, mode)

        // Getting first route
        val route =
            JSONObject(directionsJson!!).getJSONArray("routes").getJSONObject(0) // Assuming there's only one route

        // Getting Polyline of a route
        val overviewPolyline = route.getJSONObject("overview_polyline")
        val encodedPolyline = overviewPolyline.getString("points")

        // Decode the polyline string
        val decodedPath = PolyUtil.decode(encodedPolyline)

        // Getting time that it takes to finish route that we requested
        val leg = route.getJSONArray("legs").getJSONObject(0) // Assuming there's only one "leg"
        val duration = leg.getJSONObject("duration")

        return Pair(decodedPath, duration)
    }

    // Function for managing notifications
    @SuppressLint("MissingPermission")
    fun showNotification(context: Context, id: Int, text: String) {
        // Unique ID for the notification channel
        val channelId = "default_channel_id"

        // Create a notification manager
        val notificationManager = NotificationManagerCompat.from(context)

        // Create a notification channel if not already created
        val channel = NotificationChannel(
            channelId, "Default Channel", NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        // Create a notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Notification Title").setContentText(text).setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(id, builder.build())
    }
}