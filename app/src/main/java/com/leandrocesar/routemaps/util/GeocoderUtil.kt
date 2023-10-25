package com.leandrocesar.routemaps.util

import android.content.Context
import android.location.Geocoder
import android.location.Location
import java.io.IOException

class GeocoderUtil(private val context: Context) {

    fun getAddressFromLocation(location: Location): String {
        val geocoder = Geocoder(context)
        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (addresses != null) {
                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val streetName = address.thoroughfare ?: "Unknown Street"
                    val houseNumber = address.subThoroughfare ?: "Unknown Number"
                    //val city = address.locality ?: "Unknown City"
                    //val state = address.adminArea ?: "Unknown State"
                    //val country = address.countryName ?: "Unknown Country"
                    return "$streetName, $houseNumber"  //, $city, $state, $country"
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return "Address not found"
    }
}