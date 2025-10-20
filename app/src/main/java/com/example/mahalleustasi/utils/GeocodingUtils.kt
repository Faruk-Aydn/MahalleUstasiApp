package com.example.mahalleustasi.utils

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object GeocodingUtils {
    suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            val list = geocoder.getFromLocation(lat, lng, 1)
            val addr = list?.firstOrNull()
            addr?.getAddressLine(0)
        }.getOrNull()
    }
}
