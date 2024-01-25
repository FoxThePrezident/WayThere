package com.wayThereTeam.wayThere.utilities

import android.content.Context
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import org.json.JSONObject

/**
 * Various functions that weren't sorted
 * @property context from an activity currently working from
 */
class Miscellaneous(private var context: Context) {
    /**
     * Calculate nearest given marker
     * @param location location that we want the closest marker to
     * @param stops list of markers that is chosen from
     * @return marker that is closest
     */
    fun findClosestMarker(location: LatLng, stops: List<Stop>): Stop? {
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

    /**
     * Getting useful information from a Google direction api
     * @param from location of a place that we are departure from
     * @param to location that we want to arrive
     * @param mode mode of route search will influence the speed of traveling and roads that are chosen from
     * @return list of LatLng points that represent the route and duration of a trip
     */
    fun getDirections(
        from: LatLng, to: LatLng, mode: String = "walking"
    ): Pair<MutableList<LatLng>, Long> {
        val directionsJson = Internet(context).getDirection(from, to, mode)

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
        val duration = leg.getJSONObject("duration").getString("value").toLong()

        return Pair(decodedPath, duration)
    }
}