package com.example.mahalleustasi.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val phone: String? = null,
    val photoUrl: String? = null,
    val ratingAvg: Double? = null,
    val ratingCount: Int? = null,
    val location: GeoLocation? = null
) {
    data class GeoLocation(
        val lat: Double = 0.0,
        val lng: Double = 0.0
    )
}
