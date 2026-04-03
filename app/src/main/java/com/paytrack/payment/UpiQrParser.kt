package com.paytrack.payment

import android.net.Uri
import java.util.Locale

data class ParsedUpiQr(
    val payeeVpa: String,
    val payeeName: String,
    val amount: Double?,
    val note: String?,
    val rawValue: String,
    val hasEmbeddedAmount: Boolean
)

object UpiQrParser {

    fun parse(rawValue: String): ParsedUpiQr? {
        val trimmed = rawValue.trim()
        if (!trimmed.startsWith("upi://pay", ignoreCase = true)) return null

        val uri = Uri.parse(trimmed)
        val payeeVpa = uri.getQueryParameter("pa")?.trim().orEmpty()
        if (payeeVpa.isBlank()) return null

        val payeeName = uri.getQueryParameter("pn")?.trim().takeUnless { it.isNullOrBlank() }
            ?: "UPI Merchant"
        val amount = uri.getQueryParameter("am")?.toDoubleOrNull()
        val note = uri.getQueryParameter("tn")?.trim().takeUnless { it.isNullOrBlank() }

        return ParsedUpiQr(
            payeeVpa = payeeVpa,
            payeeName = payeeName,
            amount = amount,
            note = note,
            rawValue = trimmed,
            hasEmbeddedAmount = uri.getQueryParameter("am") != null
        )
    }

    fun buildPaymentUri(
        payload: ParsedUpiQr,
        amount: Double
    ): Uri {
        val finalUri = if (payload.hasEmbeddedAmount) {
            payload.rawValue
        } else {
            appendOrReplaceAmount(
                rawValue = payload.rawValue,
                amount = String.format(Locale.US, "%.2f", amount)
            )
        }
        return Uri.parse(finalUri)
    }

    private fun appendOrReplaceAmount(
        rawValue: String,
        amount: String
    ): String {
        val amountRegex = Regex("([?&])am=[^&]*", RegexOption.IGNORE_CASE)
        return when {
            amountRegex.containsMatchIn(rawValue) -> {
                rawValue.replace(amountRegex, "$1am=$amount")
            }

            rawValue.contains("?") -> "$rawValue&am=$amount"
            else -> "$rawValue?am=$amount"
        }
    }
}
