package com.fincahernandez.gestionpecuaria.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DatePickerFormattingTest {

    @Test
    fun `mantiene el dia elegido aunque la zona local este detras de UTC`() {
        val previousTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Guatemala"))
            val selectedDateMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(2026, Calendar.SEPTEMBER, 10)
            }.timeInMillis

            assertEquals("10/09/2026", formatDatePickerMillis(selectedDateMillis))
        } finally {
            TimeZone.setDefault(previousTimeZone)
        }
    }
}
