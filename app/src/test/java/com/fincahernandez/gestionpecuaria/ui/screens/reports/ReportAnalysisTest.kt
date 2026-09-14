package com.fincahernandez.gestionpecuaria.ui.screens.reports

import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportAnalysisTest {
    private val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
        isLenient = false
    }

    @Test
    fun everyReportKeepsOnlyLatestTenRecordsInsideSelectedRange() {
        val range = requireNotNull(
            resolveReportDateRange(
                period = "Personalizado",
                customStart = "01/09/2026",
                customEnd = "10/09/2026"
            )
        )
        val reportIds = listOf(
            "financial", "production", "staff", "animals", "weighings", "lots", "parcels"
        )
        val records = reportIds.flatMap { reportId ->
            (1..12).map { day ->
                ReportRecord(
                    id = "$reportId-$day",
                    reportId = reportId,
                    dateMillis = formatter.parse("${day.toString().padStart(2, '0')}/09/2026")!!.time,
                    title = "Registro $day",
                    detail = "Dato real",
                    primaryValue = day.toDouble(),
                    kind = if (reportId == "financial") "INGRESO" else "REGISTRO",
                    attributes = when (reportId) {
                        "animals" -> mapOf("sex" to "HEMBRA")
                        "production" -> mapOf("price" to "4.5")
                        else -> emptyMap()
                    }
                )
            }
        }
        val data = emptyDashboard(records)

        reportIds.forEach { reportId ->
            val analysis = buildReportAnalysis(data, reportId, reportId, range)
            assertEquals(10, analysis.recentRecords.size)
            assertEquals("$reportId-10", analysis.recentRecords.first().id)
            assertEquals("$reportId-1", analysis.recentRecords.last().id)
            assertTrue(analysis.recentRecords.all { it.dateMillis in range.startMillis..range.endMillis })
        }
    }

    @Test
    fun financialMetricsUseIncomeAndExpenseRecordsFromRange() {
        val range = requireNotNull(
            resolveReportDateRange(
                period = "Personalizado",
                customStart = "01/09/2026",
                customEnd = "30/09/2026"
            )
        )
        val data = emptyDashboard(
            listOf(
                financialRecord("income", "05/09/2026", 100.0, "INGRESO"),
                financialRecord("expense", "06/09/2026", 30.0, "EGRESO"),
                financialRecord("outside", "01/08/2026", 500.0, "INGRESO")
            )
        )

        val analysis = buildReportAnalysis(data, "financial", "Finanzas", range)

        assertEquals("Q 70", analysis.headline)
        assertEquals("Q 100", analysis.metrics.first { it.label == "Ingresos" }.value)
        assertEquals("Q 30", analysis.metrics.first { it.label == "Egresos" }.value)
        assertEquals(2, analysis.recentRecords.size)
    }

    private fun financialRecord(id: String, date: String, amount: Double, type: String) =
        ReportRecord(
            id = id,
            reportId = "financial",
            dateMillis = formatter.parse(date)!!.time,
            title = id,
            detail = type,
            primaryValue = amount,
            kind = type
        )

    private fun emptyDashboard(records: List<ReportRecord>) = ReportDashboardData(
        animalCount = 0,
        lotCount = 0,
        parcelCount = 0,
        weighingCount = 0,
        milkLiters = 0.0,
        employeeCount = 0,
        financialBalance = 0.0,
        records = records
    )
}
