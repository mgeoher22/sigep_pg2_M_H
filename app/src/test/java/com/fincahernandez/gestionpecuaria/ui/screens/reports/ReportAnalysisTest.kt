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
                customEnd = "12/09/2026"
            )
        )
        val reportIds = listOf(
            "financial", "production", "staff", "animals", "births", "weighings", "lots",
            "parcels", "sanitary", "supplies"
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
            assertEquals(12, analysis.allRecords.size)
            assertEquals("$reportId-12", analysis.recentRecords.first().id)
            assertEquals("$reportId-3", analysis.recentRecords.last().id)
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

    @Test
    fun financialStatementSeparatesSalesDirectCostsAndOperatingExpenses() {
        val range = fullSeptemberRange()
        val records = listOf(
            financialRecord("sale", "05/09/2026", 1_000.0, "INGRESO", "Venta de lote"),
            financialRecord("other", "06/09/2026", 50.0, "INGRESO", "Otros ingresos"),
            financialRecord(
                "feed",
                "07/09/2026",
                400.0,
                "EGRESO",
                "Alimentación",
                lotId = "lot-1"
            ),
            financialRecord("maintenance", "08/09/2026", 100.0, "EGRESO", "Mantenimiento")
        )

        val statement = requireNotNull(
            buildReportAnalysis(emptyDashboard(records), "financial", "Finanzas", range)
                .financialStatement
        )

        assertEquals(1_000.0, statement.sales, 0.001)
        assertEquals(50.0, statement.otherIncome, 0.001)
        assertEquals(400.0, statement.costOfSales, 0.001)
        assertEquals(100.0, statement.operatingExpenses, 0.001)
        assertEquals(600.0, statement.grossProfit, 0.001)
        assertEquals(550.0, statement.netProfit, 0.001)
        assertEquals("Alimentación", statement.expenseCategories.first().category)
        assertEquals(80.0, statement.expenseCategories.first().percentage, 0.001)
    }

    @Test
    fun monthlyReportComparesSameElapsedDatesAgainstPreviousMonth() {
        val now = formatter.parse("15/09/2026")!!.time
        val range = requireNotNull(
            resolveReportDateRange(period = "Mensual", nowMillis = now, anchorMillis = now)
        )
        val records = listOf(
            financialRecord("current-sale", "05/09/2026", 1_000.0, "INGRESO", "Venta de ganado"),
            financialRecord("current-expense", "06/09/2026", 450.0, "EGRESO", "Mantenimiento"),
            financialRecord("previous-sale", "05/08/2026", 800.0, "INGRESO", "Venta de ganado"),
            financialRecord("previous-expense", "06/08/2026", 400.0, "EGRESO", "Mantenimiento"),
            financialRecord("outside-comparison", "20/08/2026", 9_000.0, "INGRESO", "Venta de ganado")
        )

        val analysis = buildReportAnalysis(emptyDashboard(records), "financial", "Finanzas", range)

        assertEquals("01/08/2026 – 15/08/2026", range.previousLabel)
        assertEquals("Q 400", analysis.metrics.first { it.label == "Resultado anterior" }.value)
        assertTrue(
            analysis.metrics.first { it.label == "Variación vs. anterior" }.value
                .contains("+Q 150")
        )
        assertTrue(
            analysis.metrics.first { it.label == "Variación vs. anterior" }.value
                .contains("37.5 %")
        )
        val csv = analysis.toAnalysisCsv()
        assertTrue(csv.contains("metrica;Resultado anterior;Q 400;"))
        assertTrue(csv.contains("metrica;Variación vs. anterior;+Q 150"))
        assertTrue(csv.contains("egreso_categoria;Mantenimiento;450.0;100 %"))
    }

    @Test
    fun monthlyAndAnnualAnchorsCannotMoveBeyondCurrentPeriod() {
        val now = formatter.parse("15/09/2026")!!.time
        val august = shiftReportPeriodAnchor("Mensual", now, -1)

        assertTrue(canMoveReportPeriodForward("Mensual", august, now))
        assertEquals(false, canMoveReportPeriodForward("Mensual", now, now))
        assertEquals(false, canMoveReportPeriodForward("Anual", now, now))
    }

    @Test
    fun sanitaryMetricsCountDistinctAnimalsAndAppliedDose() {
        val range = fullSeptemberRange()
        val records = listOf(
            ReportRecord(
                id = "s1",
                reportId = "sanitary",
                dateMillis = formatter.parse("05/09/2026")!!.time,
                title = "Vacunación",
                detail = "Aplicación",
                primaryValue = 5.0,
                attributes = mapOf(
                    "animalId" to "a1",
                    "healthStatus" to "EXCELENTE",
                    "hasNextControl" to "true"
                )
            ),
            ReportRecord(
                id = "s2",
                reportId = "sanitary",
                dateMillis = formatter.parse("06/09/2026")!!.time,
                title = "Tratamiento",
                detail = "Seguimiento",
                primaryValue = 3.0,
                attributes = mapOf(
                    "animalId" to "a1",
                    "healthStatus" to "CRÍTICO",
                    "hasNextControl" to "false"
                )
            )
        )

        val analysis = buildReportAnalysis(emptyDashboard(records), "sanitary", "Sanidad", range)

        assertEquals("2", analysis.headline)
        assertEquals("1", analysis.metrics.first { it.label == "Animales atendidos" }.value)
        assertEquals("8 ml", analysis.metrics.first { it.label == "Medicamento aplicado" }.value)
        assertEquals("1", analysis.metrics.first { it.label == "Estado crítico" }.value)
    }

    @Test
    fun birthMetricsDoNotDuplicateASeveralCalvesBirth() {
        val date = formatter.parse("05/09/2026")!!.time
        val records = listOf(
            ReportRecord(
                id = "calf-1",
                reportId = "births",
                dateMillis = date,
                title = "Cría 1",
                detail = "Madre: Luna",
                attributes = mapOf("motherId" to "mother-1", "sex" to "HEMBRA")
            ),
            ReportRecord(
                id = "calf-2",
                reportId = "births",
                dateMillis = date,
                title = "Cría 2",
                detail = "Madre: Luna",
                attributes = mapOf("motherId" to "mother-1", "sex" to "MACHO")
            )
        )

        val analysis = buildReportAnalysis(
            emptyDashboard(records),
            "births",
            "Partos y nacimientos",
            fullSeptemberRange()
        )

        assertEquals("2", analysis.headline)
        assertEquals("1", analysis.metrics.first { it.label == "Partos" }.value)
        assertEquals("2", analysis.metrics.first { it.label == "Crías" }.value)
    }

    @Test
    fun supplyMetricsUseLogicalOperationsAndCurrentInventory() {
        val range = fullSeptemberRange()
        val records = listOf(
            ReportRecord(
                id = "entry",
                reportId = "supplies",
                dateMillis = formatter.parse("05/09/2026")!!.time,
                title = "Maíz",
                detail = "Entrada",
                primaryValue = 2.0,
                secondaryValue = 150.0,
                kind = "ENTRADA"
            ),
            ReportRecord(
                id = "exit",
                reportId = "supplies",
                dateMillis = formatter.parse("06/09/2026")!!.time,
                title = "Maíz",
                detail = "Salida",
                primaryValue = 1.0,
                kind = "SALIDA"
            )
        )
        val data = emptyDashboard(records).copy(
            supplyCount = 4,
            supplyInventoryValue = 425.0,
            supplyAttentionCount = 1
        )

        val analysis = buildReportAnalysis(data, "supplies", "Insumos", range)

        assertEquals("Q 150", analysis.headline)
        assertEquals("Q 425", analysis.metrics.first { it.label == "Valor en inventario" }.value)
        assertEquals("1", analysis.metrics.first { it.label == "Requieren atención" }.value)
    }

    private fun fullSeptemberRange() = requireNotNull(
        resolveReportDateRange(
            period = "Personalizado",
            customStart = "01/09/2026",
            customEnd = "30/09/2026"
        )
    )

    private fun financialRecord(
        id: String,
        date: String,
        amount: Double,
        type: String,
        category: String = id,
        lotId: String? = null
    ) =
        ReportRecord(
            id = id,
            reportId = "financial",
            dateMillis = formatter.parse(date)!!.time,
            title = category,
            detail = type,
            primaryValue = amount,
            kind = type,
            attributes = buildMap {
                put("category", category)
                lotId?.let { put("lotId", it) }
            }
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
