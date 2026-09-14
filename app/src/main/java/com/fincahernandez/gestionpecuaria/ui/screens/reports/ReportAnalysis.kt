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
    val records: List<ReportRecord> = emptyList()
)

data class ReportDateRange(
    val startMillis: Long,
    val endMillis: Long,
    val label: String
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
    val recentRecords: List<ReportRecord>
)

/** Convierte los accesos rápidos de periodo en límites inclusivos y verificables. */
internal fun resolveReportDateRange(
    period: String,
    nowMillis: Long = System.currentTimeMillis(),
    customStart: String = "",
    customEnd: String = ""
): ReportDateRange? {
    val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val endToday = (now.clone() as Calendar).endOfDay()
    val start = when (period) {
        "30 días" -> (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -29)
        }.startOfDay()
        "Mes actual" -> (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }.startOfDay()
        "Año actual" -> (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_YEAR, 1)
        }.startOfDay()
        "Personalizado" -> parseReportDate(customStart)?.startOfDay() ?: return null
        else -> return null
    }
    val end = if (period == "Personalizado") {
        parseReportDate(customEnd)?.endOfDay() ?: return null
    } else {
        endToday
    }
    if (start.timeInMillis > end.timeInMillis) return null
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return ReportDateRange(
        startMillis = start.timeInMillis,
        endMillis = end.timeInMillis,
        label = "${formatter.format(Date(start.timeInMillis))} – " +
            formatter.format(Date(end.timeInMillis))
    )
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
        metrics: List<ReportMetric>
    ) = ReportAnalysis(
        reportId = reportId,
        title = title,
        periodLabel = range.label,
        headline = headline,
        headlineLabel = headlineLabel,
        metrics = metrics,
        recentRecords = recent
    )

    return when (reportId) {
        "financial" -> {
            val income = records.filter { it.kind == "INGRESO" }.sumOfValue()
            val expenses = records.filter { it.kind == "EGRESO" }.sumOfValue()
            val balance = income - expenses
            analysis(
                headline = formatQuetzales(balance),
                headlineLabel = "Balance del periodo",
                metrics = listOf(
                    metric("Ingresos", formatQuetzales(income), "Entradas registradas"),
                    metric("Egresos", formatQuetzales(expenses), "Salidas registradas"),
                    metric("Balance", formatQuetzales(balance), balanceDecision(balance)),
                    metric("Movimientos", records.size.toString(), "Registros dentro del rango")
                )
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
        else -> analysis("0", "Sin datos", emptyList())
    }
}

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
