package com.example.mahalleustasi.data.model

data class Payment(
    val id: String = "",
    val jobId: String = "",
    val payerId: String = "",
    val payeeId: String = "",
    val amount: Double = 0.0,
    val tip: Double? = null,
    val method: String = "cash", // MVP: cash
    val confirmedByPayer: Boolean = false,
    val confirmedByPayee: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
