package com.paytrack

import com.paytrack.payment.UpiQrParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UpiQrParserTest {

    @Test
    fun parse_returnsPayloadForValidUpiQr() {
        val parsed = UpiQrParser.parse(
            "upi://pay?pa=merchant@upi&pn=Corner%20Store&tn=Groceries"
        )

        assertNotNull(parsed)
        assertEquals("merchant@upi", parsed?.payeeVpa)
        assertEquals("Corner Store", parsed?.payeeName)
        assertEquals("Groceries", parsed?.note)
        assertNull(parsed?.amount)
    }

    @Test
    fun parse_readsEmbeddedAmount() {
        val parsed = UpiQrParser.parse(
            "upi://pay?pa=cafe@upi&pn=Coffee%20House&am=145.50"
        )

        assertEquals(145.50, parsed?.amount ?: 0.0, 0.001)
        assertEquals("Coffee House", parsed?.payeeName)
    }

    @Test
    fun parse_returnsNullForUnsupportedQr() {
        val parsed = UpiQrParser.parse("https://example.com/not-a-upi-code")

        assertNull(parsed)
    }
}
