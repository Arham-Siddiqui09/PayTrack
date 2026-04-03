package com.paytrack.payment

import org.json.JSONObject

data class PendingPayment(
    val folderId: String,
    val folderName: String,
    val merchantName: String,
    val amount: Double,
    val upiAppPackage: String,
    val upiAppLabel: String,
    val payeeVpa: String,
    val note: String?,
    val rawQrValue: String
) {
    fun toJson(): String {
        return JSONObject()
            .put("folderId", folderId)
            .put("folderName", folderName)
            .put("merchantName", merchantName)
            .put("amount", amount)
            .put("upiAppPackage", upiAppPackage)
            .put("upiAppLabel", upiAppLabel)
            .put("payeeVpa", payeeVpa)
            .put("note", note)
            .put("rawQrValue", rawQrValue)
            .toString()
    }

    companion object {
        fun fromJson(value: String?): PendingPayment? {
            if (value.isNullOrBlank()) return null
            val json = JSONObject(value)
            return PendingPayment(
                folderId = json.getString("folderId"),
                folderName = json.getString("folderName"),
                merchantName = json.getString("merchantName"),
                amount = json.getDouble("amount"),
                upiAppPackage = json.getString("upiAppPackage"),
                upiAppLabel = json.getString("upiAppLabel"),
                payeeVpa = json.getString("payeeVpa"),
                note = json.optString("note").takeIf(String::isNotBlank),
                rawQrValue = json.getString("rawQrValue")
            )
        }
    }
}
