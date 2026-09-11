package com.fincahernandez.gestionpecuaria.ui.screens.reports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Comprueba que un resumen exportado pueda importarse sin alterar sus valores. */
class ReportCsvTest {
    @Test
    fun exportedSummaryCanBeImported() {
        val original = ReportDashboardData(
            animalCount = 15,
            lotCount = 3,
            parcelCount = 4,
            weighingCount = 28,
            milkLiters = 128.75,
            employeeCount = 6,
            financialBalance = -450.25
        )

        val imported = parseReportCsv(original.toReportCsv())

        assertEquals(original, imported)
    }

    @Test
    fun unknownCsvIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            parseReportCsv("campo;valor\nanimalCount;10")
        }
    }
}
