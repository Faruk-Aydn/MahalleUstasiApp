package com.example.mahalleustasi.data.model

data class Job(
    val id: String = "",
    val ownerId: String = "",
    val ownerName: String? = null,
    val title: String = "",
    val description: String = "",
    val price: Double? = null,
    val isCash: Boolean = true,
    val location: JobLocation? = null,
    val category: String? = null,
    val photoUrls: List<String> = emptyList(),
    val status: String = "open", // open | assigned | in_progress | awaiting_confirmation | completed | disputed | cancelled
    val assignedProId: String? = null,
    val scheduledAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    data class JobLocation(
        val lat: Double = 0.0,
        val lng: Double = 0.0,
        val address: String? = null
    )
}
