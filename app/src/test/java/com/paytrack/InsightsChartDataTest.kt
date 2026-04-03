package com.paytrack

import com.paytrack.data.Category
import com.paytrack.data.PaymentTransaction
import com.paytrack.viewmodel.buildInsightsChartData
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsChartDataTest {

    @Test
    fun `week mode returns exactly seven buckets and aggregates current week`() {
        val now = calendarOf(2026, Calendar.APRIL, 2).timeInMillis
        val transactions = listOf(
            transaction("t1", "food", "Food", 100.0, 2026, Calendar.MARCH, 30),
            transaction("t2", "food", "Food", 50.0, 2026, Calendar.APRIL, 2),
            transaction("t3", "travel", "Travel", 75.0, 2026, Calendar.APRIL, 5),
            transaction("t4", "food", "Food", 999.0, 2026, Calendar.MARCH, 21)
        )

        val result = buildInsightsChartData(
            categories = categories(),
            transactions = transactions,
            nowMillis = now
        )

        assertEquals(7, result.weeklyDailyTotals.size)
        assertEquals(100.0, result.weeklyDailyTotals[0].amount, 0.0)
        assertEquals(50.0, result.weeklyDailyTotals[3].amount, 0.0)
        assertEquals(75.0, result.weeklyDailyTotals[6].amount, 0.0)
    }

    @Test
    fun `month mode returns one bucket per day in current month`() {
        val now = calendarOf(2026, Calendar.APRIL, 2).timeInMillis
        val transactions = listOf(
            transaction("t1", "food", "Food", 30.0, 2026, Calendar.APRIL, 1),
            transaction("t2", "food", "Food", 45.0, 2026, Calendar.APRIL, 30),
            transaction("t3", "travel", "Travel", 60.0, 2026, Calendar.MARCH, 30)
        )

        val result = buildInsightsChartData(
            categories = categories(),
            transactions = transactions,
            nowMillis = now
        )

        assertEquals(30, result.currentMonthDailyTotals.size)
        assertEquals(30.0, result.currentMonthDailyTotals[0].amount, 0.0)
        assertEquals(45.0, result.currentMonthDailyTotals[29].amount, 0.0)
        assertEquals(0.0, result.currentMonthDailyTotals[1].amount, 0.0)
    }

    @Test
    fun `monthly trend keeps twelve months and folder totals are merged`() {
        val now = calendarOf(2026, Calendar.APRIL, 2).timeInMillis
        val transactions = listOf(
            transaction("t1", "food", "Food", 30.0, 2025, Calendar.MAY, 10),
            transaction("t2", "food", "Food", 70.0, 2026, Calendar.APRIL, 1),
            transaction("t3", "travel", "Travel", 50.0, 2026, Calendar.APRIL, 2),
            transaction("t4", "travel", "Travel", 40.0, 2024, Calendar.DECEMBER, 15)
        )

        val result = buildInsightsChartData(
            categories = categories(),
            transactions = transactions,
            nowMillis = now
        )

        assertEquals(12, result.last12MonthTotals.size)
        assertEquals("May", result.last12MonthTotals.first().label)
        assertEquals("Apr", result.last12MonthTotals.last().label)
        assertEquals(30.0, result.last12MonthTotals.first().amount, 0.0)
        assertEquals(120.0, result.last12MonthTotals.last().amount, 0.0)

        assertEquals(2, result.folderTransactionTotals.size)
        assertEquals("Food", result.folderTransactionTotals[0].name)
        assertEquals(100.0, result.folderTransactionTotals[0].amount, 0.0)
        assertEquals("Travel", result.folderTransactionTotals[1].name)
        assertEquals(50.0, result.folderTransactionTotals[1].amount, 0.0)
        assertTrue(result.folderTransactionTotals[0].amount >= result.folderTransactionTotals[1].amount)
    }

    private fun categories(): List<Category> {
        return listOf(
            Category(id = "food", name = "Food", amount = 1000.0, accentColor = 0xFFFF8D8D),
            Category(id = "travel", name = "Travel", amount = 800.0, accentColor = 0xFFAEC7D6)
        )
    }

    private fun transaction(
        id: String,
        folderId: String,
        folderName: String,
        amount: Double,
        year: Int,
        month: Int,
        dayOfMonth: Int
    ): PaymentTransaction {
        return PaymentTransaction(
            id = id,
            folderId = folderId,
            folderName = folderName,
            merchantName = "Merchant $id",
            amount = amount,
            upiAppPackage = "app",
            upiAppLabel = "UPI",
            payeeVpa = "test@upi",
            note = null,
            createdAtMillis = calendarOf(year, month, dayOfMonth).timeInMillis,
            status = "SUCCESS"
        )
    }

    private fun calendarOf(year: Int, month: Int, dayOfMonth: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
