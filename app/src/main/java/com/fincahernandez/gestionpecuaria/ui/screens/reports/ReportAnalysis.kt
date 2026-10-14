package com.fincahernandez.gestionpecuaria.ui.screens.reports

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Registro normalizado que permite analizar módulos diferentes con el mismo filtro de fechas. */
data class ReportRecord(
    val id: String,
    val reportId: String,
    val dateMillis: Long,
    val title: String,
    val detail: String,
    val primaryValue: Double? = null,
    val secondaryValue: Double? = null,
    val kind: String = "",
    val attributes: Map<String, String> = emptyMap()
)

/** Totales generales y registros reales que alimentan todas las vistas previas. */
data class ReportDashboardData(
    val animalCount: Int,
    val lotCount: Int,
    val parcelCount: Int,
    val weighingCount: Int,
    val milkLiters: Double,
    val employeeCount: Int,
    val financialBalance: Double,
    val supplyCount: Int = 0,
    val supplyInventoryValue: Double = 0.0,
    val supplyAttentionCount: Int = 0,
    val records: List<ReportRecord> = emptyList()
)

data class ReportDateRange(
    val startMillis: Long,
    val endMillis: Long,
    val label: String,
    val previousStartMillis: Long? = null,
    val previousEndMillis: Long? = null,
    val previousLabel: String? = null
)

data class ReportMetric(
    val label: String,
    val value: String,
    val explanation: String
)

data class ReportAnalysis(
    val reportId: String,
    val title: String,
    val periodLabel: String,
    val headline: String,
    val headlineLabel: String,
    val metrics: List<ReportMetric>,
    val financialStatement: FinancialStatement? = null,
    /** Todos los registros del rango; se usan en los archivos exportados. */
    val allRecords: List<ReportRecord>,
    /** Resumen visual limitado a los diez registros más recientes. */
    val recentRecords: List<ReportRecord>
)

data class ExpenseCategoryShare(
    val category: String,
    val amount: Double,
    val percentage: Double
)

/** Estado de resultados administrativo construido exclusivamente con movimientos registrados. */
data class FinancialStatement(
    val sales: Double,
    val otherIncome: Double,
    val costOfSales: Double,
    val operatingExpenses: Double,
    val grossProfit: Double,
    val netProfit: Double,
    val expenseCategories: List<ExpenseCategoryShare>
)

/** Convierte los accesos rápidos de periodo en límites inclusivos y verificables. */
internal fun resolveReportDateRange(
    period: String,
    nowMillis: Long = System.currentTimeMillis(),
    anchorMillis: Long = nowMillis,
    customStart: String = "",
    customEnd: String = ""
): ReportDateRange? {
    val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val anchor = Calendar.getInstance().apply { timeInMillis = anchorMillis }
    val endToday = (now.clone() as Calendar).endOfDay()
    val start = when (period) {
        "30 días" -> (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -29)
        }.startOfDay()
        "Mes actual", "Mensual" -> (anchor.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }.startOfDay()
        "Año actual", "Anual" -> (anchor.clone() as Calendar).apply {
            set(Calendar.DAY_OF_YEAR, 1)
        }.startOfDay()
        "Personalizado" -> parseReportDate(customStart)?.startOfDay() ?: return null
        else -> return null
    }
    val end = when (period) {
        "Personalizado" -> parseReportDate(customEnd)?.endOfDay() ?: return null
        "Mes actual", "Mensual" -> (anchor.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }.endOfDay().let { selectedEnd ->
            if (sameMonth(anchor, now) && selectedEnd.after(endToday)) endToday else selectedEnd
        }
        "Año actual", "Anual" -> (anchor.clone() as Calendar).apply {
            set(Calendar.DAY_OF_YEAR, getActualMaximum(Calendar.DAY_OF_YEAR))
        }.endOfDay().let { selectedEnd ->
            if (anchor.get(Calendar.YEAR) == now.get(Calendar.YEAR) && selectedEnd.after(endToday)) {
                endToday
            } else selectedEnd
        }
        else -> endToday
    }
    if (start.timeInMillis > end.timeInMillis) return null
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val previous = previousComparableRange(period, start, end)
    return ReportDateRange(
        startMillis = start.timeInMillis,
        endMillis = end.timeInMillis,
        label = "${formatter.format(Date(start.timeInMillis))} – " +
            formatter.format(Date(end.timeInMillis)),
        previousStartMillis = previous?.first?.timeInMillis,
        previousEndMillis = previous?.second?.timeInMillis,
        previousLabel = previous?.let {
            "${formatter.format(Date(it.first.timeInMillis))} – " +
                formatter.format(Date(it.second.timeInMillis))
        }
    )
}

internal fun shiftReportPeriodAnchor(period: String, anchorMillis: Long, amount: Int): Long {
    val anchor = Calendar.getInstance().apply { timeInMillis = anchorMillis }
    when (period) {
        "Mes actual", "Mensual" -> anchor.add(Calendar.MONTH, amount)
        "Año actual", "Anual" -> anchor.add(Calendar.YEAR, amount)
    }
    return anchor.timeInMillis
}

internal fun canMoveReportPeriodForward(
    period: String,
    anchorMillis: Long,
    nowMillis: Long = System.currentTimeMillis()
): Boolean {
    if (period !in setOf("Mes actual", "Mensual", "Año actual", "Anual")) return false
    val next = Calendar.getInstance().apply {
        timeInMillis = shiftReportPeriodAnchor(period, anchorMillis, 1)
    }
    val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
    return if (period in setOf("Mes actual", "Mensual")) {
        next.get(Calendar.YEAR) < now.get(Calendar.YEAR) ||
            next.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            next.get(Calendar.MONTH) <= now.get(Calendar.MONTH)
    } else {
        next.get(Calendar.YEAR) <= now.get(Calendar.YEAR)
    }
}

/** Calcula indicadores distintos según el módulo y conserva solo los 10 registros más recientes. */
internal fun buildReportAnalysis(
    data: ReportDashboardData,
    reportId: String,
    title: String,
    range: ReportDateRange
): ReportAnalysis {
    val records = data.records
        .asSequence()
        .filter { it.reportId == reportId && it.dateMillis in range.startMillis..range.endMillis }
        .sortedByDescending(ReportRecord::dateMillis)
        .toList()
    val recent = records.take(10)

    fun analysis(
        headline: String,
        headlineLabel: String,
        metrics: List<ReportMetric>,
        financialStatement: FinancialStatement? = null
    ) = ReportAnalysis(
        reportId = reportId,
        title = title,
        periodLabel = range.label,
        headline = headline,
        headlineLabel = headlineLabel,
        metrics = metrics,
        financialStatement = financialStatement,
        allRecords = records,
        recentRecords = recent
    )

    return when (reportId) {
        "financial" -> {
            val statement = buildFinancialStatement(records)
            val income = statement.sales + statement.otherIncome
            val expenses = statement.costOfSales + statement.operatingExpenses
            val previousStart = range.previousStartMillis
            val previousEnd = range.previousEndMillis
            val previousRecords = if (previousStart != null && previousEnd != null) {
                data.records.filter {
                    it.reportId == reportId &&
                        it.dateMillis in previousStart..previousEnd
                }
            } else emptyList()
            val previousStatement = if (previousStart != null && previousEnd != null) {
                buildFinancialStatement(previousRecords)
            } else null
            val comparisonMetrics = previousStatement?.let { previous ->
                val difference = statement.netProfit - previous.netProfit
                listOf(
                    metric(
                        "Resultado anterior",
                        formatQuetzales(previous.netProfit),
                        range.previousLabel ?: "Período anterior comparable"
                    ),
                    metric(
                        "Variación vs. anterior",
                        comparisonValue(difference, previous.netProfit),
                        comparisonDecision(difference)
                    )
                )
            }.orEmpty()
            analysis(
                headline = formatQuetzales(statement.netProfit),
                headlineLabel = "Resultado administrativo del período",
                metrics = listOf(
                    metric("Ingresos", formatQuetzales(income), "Entradas registradas"),
                    metric("Egresos", formatQuetzales(expenses), "Salidas registradas"),
                    metric("Ventas", formatQuetzales(statement.sales), "Venta de ganado, lotes y leche"),
                    metric("Otros ingresos", formatQuetzales(statement.otherIncome), "Ingresos distintos de ventas"),
                    metric("Costo de ventas", formatQuetzales(statement.costOfSales), "Egresos vinculados directamente a lotes"),
                    metric("Utilidad bruta", formatQuetzales(statement.grossProfit), "Ventas menos costo de ventas"),
                    metric("Gastos operativos", formatQuetzales(statement.operatingExpenses), "Egresos generales sin lote"),
                    metric("Utilidad neta", formatQuetzales(statement.netProfit), balanceDecision(statement.netProfit))
                ) + comparisonMetrics,
                financialStatement = statement
            )
        }
        "production" -> {
            val liters = records.sumOfValue()
            val confirmedIncome = records
                .filter { it.attributes["paymentStatus"] != "PENDING" }
                .sumOf { it.secondaryValue ?: 0.0 }
            val pendingIncome = records
                .filter { it.attributes["paymentStatus"] == "PENDING" }
                .sumOf { it.secondaryValue ?: 0.0 }
            val productiveDays = records.map { dayKey(it.dateMillis) }.distinct().size
            val average = if (productiveDays == 0) 0.0 else liters / productiveDays
            val averagePrice = records.mapNotNull { it.attributes["price"]?.toDoubleOrNull() }
                .averageOrZero()
            analysis(
                headline = "${formatNumber(liters)} L",
                headlineLabel = "Producción del periodo",
                metrics = listOf(
                    metric("Producción total", "${formatNumber(liters)} L", "Volumen registrado"),
                    metric("Promedio diario", "${formatNumber(average)} L", "$productiveDays días con producción"),
                    metric("Ingreso confirmado", formatQuetzales(confirmedIncome), "Pagos aprobados"),
                    metric("Monto pendiente", formatQuetzales(pendingIncome), "Aún no entra a Finanzas"),
                    metric("Precio promedio", "Q ${formatNumber(averagePrice)}/L", "Promedio de precios diarios")
                )
            )
        }
        "staff" -> {
            val employees = records.filter { it.kind == "EMPLEADO" }
            val payments = records.filter { it.kind == "PAGO" }
            val payroll = payments.sumOfValue()
            val baseSalary = employees.sumOfValue()
            analysis(
                headline = formatQuetzales(payroll),
                headlineLabel = "Pagos de nómina del periodo",
                metrics = listOf(
                    metric("Altas de personal", employees.size.toString(), "Empleados registrados en el rango"),
                    metric("Empleados activos", data.employeeCount.toString(), "Plantilla activa actual"),
                    metric("Pagos realizados", payments.size.toString(), "Comprobantes del periodo"),
                    metric("Salario base de altas", formatQuetzales(baseSalary), "Suma de salarios de nuevas altas")
                )
            )
        }
        "animals" -> {
            val weights = records.mapNotNull(ReportRecord::primaryValue)
            val females = records.count { it.attributes["sex"] == "HEMBRA" }
            val males = records.count { it.attributes["sex"] == "MACHO" }
            analysis(
                headline = records.size.toString(),
                headlineLabel = "Animales registrados en el periodo",
                metrics = listOf(
                    metric("Nuevos registros", records.size.toString(), "Ingresados dentro del rango"),
                    metric("Hembras / machos", "$females / $males", "Distribución de los nuevos registros"),
                    metric("Peso promedio", weights.averageLabel("lb"), "Último peso disponible"),
                    metric("Inventario activo", data.animalCount.toString(), "Animales activos actualmente")
                )
            )
        }
        "births" -> {
            val withMother = records.filter { it.attributes["motherId"].orEmpty().isNotBlank() }
            val births = withMother
                .map { "${it.attributes["motherId"]}|${dayKey(it.dateMillis)}" }
                .distinct()
                .size
            val withoutMother = records.size - withMother.size
            val females = records.count { it.attributes["sex"] == "HEMBRA" }
            val males = records.count { it.attributes["sex"] == "MACHO" }
            analysis(
                headline = records.size.toString(),
                headlineLabel = "Crías nacidas en finca",
                metrics = listOf(
                    metric("Partos", births.toString(), "Madres y fechas de parto distintas"),
                    metric("Crías", records.size.toString(), "Nacimientos dentro del rango"),
                    metric("Sin madre asociada", withoutMother.toString(), "Crías pendientes de relación genealógica"),
                    metric("Hembras / machos", "$females / $males", "Distribución de las crías")
                )
            )
        }
        "weighings" -> {
            val weights = records.mapNotNull(ReportRecord::primaryValue)
            val differences = records.mapNotNull(ReportRecord::secondaryValue)
            val gains = differences.count { it > 0.0 }
            val losses = differences.count { it < 0.0 }
            analysis(
                headline = records.size.toString(),
                headlineLabel = "Pesajes del periodo",
                metrics = listOf(
                    metric("Peso promedio", weights.averageLabel("lb"), "Promedio de mediciones"),
                    metric(
                        "Cambio promedio",
                        differences.takeIf { it.isNotEmpty() }
                            ?.let { signedPounds(it.average()) }
                            ?: "Sin comparación",
                        "Contra el pesaje anterior"
                    ),
                    metric("Ganaron peso", gains.toString(), "Mediciones con diferencia positiva"),
                    metric("Perdieron peso", losses.toString(), "Mediciones con diferencia negativa")
                )
            )
        }
        "lots" -> {
            val animalCounts = records.mapNotNull(ReportRecord::primaryValue)
            val active = records.count { it.attributes["status"] == "ACTIVO" }
            analysis(
                headline = records.size.toString(),
                headlineLabel = "Lotes creados en el periodo",
                metrics = listOf(
                    metric("Nuevos lotes", records.size.toString(), "Creados dentro del rango"),
                    metric("Activos del periodo", active.toString(), "Nuevos lotes aún activos"),
                    metric("Animales agrupados", formatNumber(animalCounts.sum()), "Asignaciones actuales"),
                    metric("Promedio por lote", formatNumber(animalCounts.averageOrZero()), "Tamaño medio de los nuevos lotes")
                )
            )
        }
        "parcels" -> {
            val hectares = records.sumOfValue()
            val capacity = records.mapNotNull(ReportRecord::secondaryValue).sum()
            val occupied = records.count { it.attributes["status"] == "OCUPADA" }
            analysis(
                headline = "${formatNumber(hectares)} ha",
                headlineLabel = "Superficie registrada en el periodo",
                metrics = listOf(
                    metric("Nuevas parcelas", records.size.toString(), "Registradas dentro del rango"),
                    metric("Superficie", "${formatNumber(hectares)} ha", "Área total de nuevas parcelas"),
                    metric("Capacidad", formatNumber(capacity), "Capacidad animal declarada"),
                    metric("Ocupadas", occupied.toString(), "Nuevas parcelas con lote activo")
                )
            )
        }
        "sanitary" -> {
            val animalsAttended = records.mapNotNull { it.attributes["animalId"] }.distinct().size
            val appliedMilliliters = records.mapNotNull(ReportRecord::primaryValue).sum()
            val critical = records.count { it.attributes["healthStatus"] == "CRÍTICO" }
            val followUps = records.count { it.attributes["hasNextControl"] == "true" }
            analysis(
                headline = records.size.toString(),
                headlineLabel = "Eventos sanitarios del periodo",
                metrics = listOf(
                    metric("Atenciones", records.size.toString(), "Eventos registrados dentro del rango"),
                    metric("Animales atendidos", animalsAttended.toString(), "Animales distintos con atención"),
                    metric("Medicamento aplicado", "${formatNumber(appliedMilliliters)} ml", "Dosis descontadas del inventario"),
                    metric("Estado crítico", critical.toString(), "Atenciones que dejaron estado crítico"),
                    metric("Próximos controles", followUps.toString(), "Eventos con seguimiento programado")
                )
            )
        }
        "supplies" -> {
            val entries = records.filter { it.kind == "ENTRADA" }
            val exits = records.filter { it.kind == "SALIDA" }
            val assignments = records.filter { it.kind == "ASIGNACION" }
            val returns = records.filter { it.kind == "DEVOLUCION" }
            val purchaseValue = entries.sumOf { it.secondaryValue ?: 0.0 }
            analysis(
                headline = formatQuetzales(purchaseValue),
                headlineLabel = "Compras de insumos del periodo",
                metrics = listOf(
                    metric("Entradas", entries.size.toString(), "Operaciones de compra o ingreso"),
                    metric("Salidas", exits.size.toString(), "Operaciones de consumo"),
                    metric("Préstamos / devoluciones", "${assignments.size} / ${returns.size}", "Movimientos de herramientas y equipos"),
                    metric("Productos activos", data.supplyCount.toString(), "Catálogo disponible actualmente"),
                    metric("Valor en inventario", formatQuetzales(data.supplyInventoryValue), "Existencia disponible y asignada actual"),
                    metric("Requieren atención", data.supplyAttentionCount.toString(), "Productos agotados o bajo el mínimo")
                )
            )
        }
        else -> analysis("0", "Sin datos", emptyList())
    }
}

/**
 * Clasificación administrativa: un egreso con lote es costo directo de la venta;
 * los demás egresos son operativos. No se presenta como balance patrimonial porque
 * la aplicación aún no registra cuentas de activos ni pasivos.
 */
internal fun buildFinancialStatement(records: List<ReportRecord>): FinancialStatement {
    val incomes = records.filter { it.kind == "INGRESO" }
    val expenses = records.filter { it.kind == "EGRESO" }
    val sales = incomes.filter(::isSale).sumOfValue()
    val otherIncome = incomes.filterNot(::isSale).sumOfValue()
    val costOfSales = expenses.filter(::isDirectLotCost).sumOfValue()
    val operatingExpenses = expenses.filterNot(::isDirectLotCost).sumOfValue()
    val expenseTotal = expenses.sumOfValue()
    val distribution = expenses
        .groupBy { it.attributes["category"].orEmpty().ifBlank { it.title } }
        .map { (category, categoryRecords) ->
            val amount = categoryRecords.sumOfValue()
            ExpenseCategoryShare(
                category = category,
                amount = amount,
                percentage = if (expenseTotal == 0.0) 0.0 else amount / expenseTotal * 100.0
            )
        }
        .sortedWith(compareByDescending<ExpenseCategoryShare> { it.amount }.thenBy { it.category })
    return FinancialStatement(
        sales = sales,
        otherIncome = otherIncome,
        costOfSales = costOfSales,
        operatingExpenses = operatingExpenses,
        grossProfit = sales - costOfSales,
        netProfit = sales + otherIncome - costOfSales - operatingExpenses,
        expenseCategories = distribution
    )
}

private fun isSale(record: ReportRecord): Boolean {
    val category = record.attributes["category"].orEmpty().ifBlank { record.title }
    return category.startsWith("Venta", ignoreCase = true) ||
        category.equals("Producción de leche", ignoreCase = true)
}

private fun isDirectLotCost(record: ReportRecord): Boolean =
    record.attributes["lotId"].orEmpty().isNotBlank()

private fun comparisonValue(difference: Double, previous: Double): String {
    val amount = "${if (difference > 0.0) "+" else ""}${formatQuetzales(difference)}"
    if (previous == 0.0) return "$amount · sin base porcentual"
    val percentage = difference / kotlin.math.abs(previous) * 100.0
    return "$amount · ${if (percentage > 0.0) "+" else ""}${formatNumber(percentage)} %"
}

private fun comparisonDecision(difference: Double): String = when {
    difference > 0.0 -> "El resultado mejoró frente al período anterior"
    difference < 0.0 -> "El resultado disminuyó frente al período anterior"
    else -> "Sin variación frente al período anterior"
}

private fun previousComparableRange(
    period: String,
    start: Calendar,
    end: Calendar
): Pair<Calendar, Calendar>? = when (period) {
    "30 días" -> {
        val previousEnd = (start.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.endOfDay()
        val previousStart = (previousEnd.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -29)
        }.startOfDay()
        previousStart to previousEnd
    }
    "Mes actual", "Mensual" -> {
        val previousStart = (start.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val elapsedDay = end.get(Calendar.DAY_OF_MONTH)
        val previousEnd = (previousStart.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, elapsedDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
        }.endOfDay()
        previousStart to previousEnd
    }
    "Año actual", "Anual" -> {
        val previousStart = (start.clone() as Calendar).apply { add(Calendar.YEAR, -1) }
        val previousEnd = (end.clone() as Calendar).apply { add(Calendar.YEAR, -1) }
        previousStart to previousEnd
    }
    else -> null
}

private fun sameMonth(first: Calendar, second: Calendar): Boolean =
    first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.MONTH) == second.get(Calendar.MONTH)

private fun metric(label: String, value: String, explanation: String) =
    ReportMetric(label, value, explanation)

private fun Iterable<ReportRecord>.sumOfValue(): Double = sumOf { it.primaryValue ?: 0.0 }

private fun Iterable<Double>.averageOrZero(): Double {
    val values = toList()
    return if (values.isEmpty()) 0.0 else values.average()
}

private fun Collection<Double>.averageLabel(unit: String): String =
    if (isEmpty()) "Sin datos" else "${formatNumber(average())} $unit"

private fun formatQuetzales(value: Double): String = "Q ${formatNumber(value)}"

private fun formatNumber(value: Double): String =
    NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 2 }.format(value)

private fun signedPounds(value: Double): String =
    "${if (value > 0.0) "+" else ""}${formatNumber(value)} lb"

private fun balanceDecision(balance: Double): String = when {
    balance > 0.0 -> "Resultado positivo del periodo"
    balance < 0.0 -> "Los egresos superaron los ingresos"
    else -> "Ingresos y egresos equilibrados"
}

private fun dayKey(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))

private fun parseReportDate(value: String): Calendar? = runCatching {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
    Calendar.getInstance().apply { time = formatter.parse(value)!! }
}.getOrNull()

private fun Calendar.startOfDay(): Calendar = apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

private fun Calendar.endOfDay(): Calendar = apply {
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
}
