package com.waywardTeam.wayward.utilities

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import java.time.LocalTime

// Data classes for public stops
data class Stop(val name: String = "", val location: String = "", val type: Array<String> = emptyArray()) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Stop

        if (name != other.name) return false
        if (location != other.location) return false
        if (!type.contentEquals(other.type)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + type.contentHashCode()
        return result
    }
}


// Routing using public transport
data class Transportation(val id: String, val provider: String, val route: MutableList<Route>)
data class Route(val time: LocalTime, val stop: String)

data class UserSettings(var timeBetweenWaiting: Long)

data class MapData(val name: String, val location: LatLng?) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString().toString(), parcel.readParcelable(LatLng::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeParcelable(location, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MapData> {
        override fun createFromParcel(parcel: Parcel): MapData {
            return MapData(parcel)
        }

        override fun newArray(size: Int): Array<MapData?> {
            return arrayOfNulls(size)
        }
    }
}

data class PolylineRoute(val route: MutableList<LatLng>, val color: Int) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.createTypedArrayList(LatLng.CREATOR)!!, parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(route)
        parcel.writeInt(color)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PolylineRoute> {
        override fun createFromParcel(parcel: Parcel): PolylineRoute {
            return PolylineRoute(parcel)
        }

        override fun newArray(size: Int): Array<PolylineRoute?> {
            return arrayOfNulls(size)
        }
    }
}

data class NotificationData(val importance: Int, val text: String) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(), parcel.readString().toString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(importance)
        parcel.writeString(text)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<NotificationData> {
        override fun createFromParcel(parcel: Parcel): NotificationData {
            return NotificationData(parcel)
        }

        override fun newArray(size: Int): Array<NotificationData?> {
            return arrayOfNulls(size)
        }
    }
}