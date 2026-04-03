package com.paytrack.data

data class PaymentTransaction(
    val id: String,
    val folderId: String,
    val folderName: String,
    val merchantName: String,
    val amount: Double,
    val upiAppPackage: String,
    val upiAppLabel: String,
    val payeeVpa: String,
    val note: String?,
    val createdAtMillis: Long,
    val status: String
)
