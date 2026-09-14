package com.fincahernandez.gestionpecuaria.data.repository

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class MilkPaymentDateTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun dailyPaymentUsesProductionDate() {
        val productionDate = date(2026, Calendar.SEPTEMBER, 14)

        val result = calculateMilkPaymentDueDate(
            productionDate,
            MilkPaymentFrequency.DAILY,
            utc
        )

        assertEquals(productionDate, result)
    }

    @Test
    fun weeklyPaymentUsesFollowingSaturday() {
        val monday = date(2026, Calendar.SEPTEMBER, 14)
        val saturday = date(2026, Calendar.SEPTEMBER, 19)

        val result = calculateMilkPaymentDueDate(
            monday,
            MilkPaymentFrequency.EVERY_SATURDAY,
            utc
        )

        assertEquals(saturday, result)
    }

    @Test
    fun saturdayProductionIsPaidTheSameSaturday() {
        val saturday = date(2026, Calendar.SEPTEMBER, 19)

        val result = calculateMilkPaymentDueDate(
            saturday,
            MilkPaymentFrequency.EVERY_SATURDAY,
            utc
        )

        assertEquals(saturday, result)
    }

    private fun date(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day)
        }.timeInMillis
}
