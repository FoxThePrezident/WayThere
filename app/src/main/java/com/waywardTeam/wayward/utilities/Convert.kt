package com.waywardTeam.wayward.utilities

import com.google.android.gms.maps.model.LatLng
import java.time.LocalTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import java.util.*


// Class with function related to a managing data and formatting it
class Convert {
    /**
     * @param clearData string that will be formatted
     * @return list of data classes Transportation
     */
    fun toDataClass(clearData: String): MutableList<Transportation> {
        val scanner = Scanner(clearData)
        val data = mutableListOf<Transportation>()
        var routesList: MutableList<Route>? = null

        while (scanner.hasNextLine()) {
            var line = scanner.nextLine()

            when {
                line.startsWith("Bus") || line.startsWith("Os") -> {
                    routesList = mutableListOf()
                    val transport = Transportation(line, scanner.nextLine(), routesList)
                    data.add(transport)
                }

                line.contains(":") -> {
                    if (line.length < 5) line = "0$line"
                    val time = LocalTime.parse(line, ISO_LOCAL_TIME)
                    routesList?.add(Route(time, scanner.nextLine()))
                }

                else -> println("Unknown handling: $line")
            }
        }

        scanner.close()
        return data
    }

    /**
     * Converting string to LatLng class
     * @param location string that will be formatted, like 49.055834,20.279786
     * @return LatLng, like LatLng(49.055834, 20.279786)
     */
    fun toLatLng(location: String): LatLng {
        val (latString, lngString) = location.split(", ")

        // Process latitude
        val lat = if (latString.contains("N")) {
            latString.replace("N", "").toDouble()
        } else if (latString.contains("S")) {
            -latString.replace("S", "").toDouble()
        } else {
            latString.toDouble()
        }

        // Process longitude
        val lng = if (lngString.contains("E")) {
            lngString.replace("E", "").toDouble()
        } else if (lngString.contains("W")) {
            -lngString.replace("W", "").toDouble()
        } else {
            lngString.toDouble()
        }

        return LatLng(lat, lng)
    }
}
