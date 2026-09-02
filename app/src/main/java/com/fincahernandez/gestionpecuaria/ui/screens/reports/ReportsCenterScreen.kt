package com.fincahernandez.gestionpecuaria.ui.screens.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.ui.components.AppBottomBar
import com.fincahernandez.gestionpecuaria.ui.components.DemoModeNotice
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import java.text.NumberFormat
import java.util.Locale

/** Valores actuales utilizados para construir las vistas previas de los reportes. */
data class ReportDashboardData(
    val animalCount: Int,
    val lotCount: Int,
    val parcelCount: Int,
    val weighingCount: Int,
    val milkLiters: Double,
    val employeeCount: Int,
    val financialBalance: Double
)

private data class ReportOption(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val currentValue: (ReportDashboardData) -> String
)

/**
 * Centro de reportes basado en el prototipo y en RF-023, RF-024 y RF-025.
 * En esta etapa genera una vista previa; la exportación real se conectará posteriormente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsCenterScreen(
    data: ReportDashboardData,
    onMenuClick: () -> Unit,
    onNavigateMain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by rememberSaveable { mutableStateOf("Mensual") }
    var selectedReportId by rememberSaveable { mutableStateOf(reportOptions.first().id) }
    var generatedPreviewId by rememberSaveable { mutableStateOf<String?>(null) }
    var exportMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedReport = reportOptions.first { it.id == selectedReportId }
    val generatedReport = reportOptions.firstOrNull { it.id == generatedPreviewId }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Centro de reportes",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AppBottomBar(selectedRoute = Routes.REPORTS, onNavigate = onNavigateMain)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { DemoModeNotice(compact = true) }
            item {
                Text(
                    "Seleccione el periodo y el tipo de informe que desea consultar.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            item {
                Text("Periodo", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("30 días", "Mensual", "Anual").forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = {
                                selectedPeriod = period
                                generatedPreviewId = null
                            },
                            label = { Text(period) },
                            leadingIcon = if (selectedPeriod == period) {
                                { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }
            item {
                Text(
                    "Reportes disponibles",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            items(reportOptions, key = { it.id }) { report ->
                val selected = report.id == selectedReportId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedReportId = report.id
                            generatedPreviewId = null
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            report.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(report.title, fontWeight = FontWeight.Bold)
                            Text(report.description, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            report.currentValue(data),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = { generatedPreviewId = selectedReport.id },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null)
                    Spacer(modifier = Modifier.padding(horizontal = 5.dp))
                    Text("Generar vista previa", fontWeight = FontWeight.Bold)
                }
            }
            generatedReport?.let { report ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "VISTA PREVIA • ${selectedPeriod.uppercase()}",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                report.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                report.currentValue(data),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Resumen calculado con los datos disponibles actualmente.",
                                color = Color.White
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                exportMessage =
                                    "La exportación PDF se conectará cuando los reportes usen todos los datos persistentes."
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Text(" PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                exportMessage =
                                    "La exportación Excel se habilitará junto con la generación definitiva de informes."
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null)
                            Text(" Excel")
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    exportMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { exportMessage = null },
            title = { Text("Exportación pendiente") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { exportMessage = null }) { Text("Entendido") }
            }
        )
    }
}

private val reportOptions = listOf(
    ReportOption(
        id = "financial",
        title = "Reportes financieros",
        description = "Balance, estado de resultados y comparación por periodo.",
        icon = Icons.Default.AccountBalance,
        currentValue = { formatQuetzales(it.financialBalance) }
    ),
    ReportOption(
        id = "production",
        title = "Producción",
        description = "Producción de leche y rendimiento productivo.",
        icon = Icons.Default.LocalDrink,
        currentValue = { "${formatNumber(it.milkLiters)} L" }
    ),
    ReportOption(
        id = "staff",
        title = "Personal",
        description = "Nómina, empleados activos y costos de mano de obra.",
        icon = Icons.Default.Badge,
        currentValue = { "${it.employeeCount} empleados" }
    ),
    ReportOption(
        id = "animals",
        title = "Sanidad e inventario",
        description = "Inventario animal y futura información sanitaria.",
        icon = Icons.Default.Pets,
        currentValue = { "${it.animalCount} animales" }
    ),
    ReportOption(
        id = "weighings",
        title = "Pesajes",
        description = "Registros de peso y seguimiento del crecimiento.",
        icon = Icons.Default.Scale,
        currentValue = { "${it.weighingCount} registros" }
    ),
    ReportOption(
        id = "lots",
        title = "Lotes",
        description = "Inventario agrupado y rendimiento por lote.",
        icon = Icons.Default.Groups,
        currentValue = { "${it.lotCount} lotes" }
    ),
    ReportOption(
        id = "parcels",
        title = "Parcelas",
        description = "Uso de terrenos y productividad de las parcelas.",
        icon = Icons.Default.Landscape,
        currentValue = { "${it.parcelCount} parcelas" }
    )
)

private fun formatQuetzales(value: Double): String = "Q ${formatNumber(value)}"

private fun formatNumber(value: Double): String =
    NumberFormat.getNumberInstance(Locale.US).format(value)
