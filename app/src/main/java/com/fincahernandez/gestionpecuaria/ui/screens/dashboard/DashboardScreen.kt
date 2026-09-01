package com.fincahernandez.gestionpecuaria.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.ui.components.AppBottomBar
import com.fincahernandez.gestionpecuaria.ui.components.DemoModeNotice
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import com.fincahernandez.gestionpecuaria.ui.theme.GestionPecuariaTheme

/** Información visual de un indicador del dashboard. */
private data class DashboardStat(
    val title: String,
    val value: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
    val alert: Boolean = false
)

/**
 * Dashboard principal correspondiente a HU-02.
 *
 * Utiliza una cuadrícula adaptable: en teléfono presenta menos columnas y en
 * tableta aprovecha el ancho disponible sin duplicar la implementación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onMenuClick: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Finca Hernández",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Panel principal", style = MaterialTheme.typography.labelLarge)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                    }
                },
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Modo prueba", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AppBottomBar(
                selectedRoute = Routes.DASHBOARD,
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Resumen operativo",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Información general de la finca",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                // La advertencia evita que los valores de diseño se confundan con datos reales.
                DemoModeNotice()
            }

            items(dashboardStats, key = { it.title }) { stat ->
                StatCard(stat = stat, onClick = { onNavigate(stat.route) })
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionCard(title = "Producción de leche semanal") {
                    WeeklyProductionChart()
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionCard(title = "Ingresos y gastos") {
                    FinancialSummary()
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionCard(title = "Crecimiento por lote") {
                    LotGrowthSummary()
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionCard(title = "Acciones rápidas") {
                    QuickActions(onNavigate = onNavigate)
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionCard(title = "Notificaciones") {
                    NotificationSummary()
                }
            }
        }
    }
}

/** Tarjeta seleccionable que abre el módulo relacionado con el indicador. */
@Composable
private fun StatCard(stat: DashboardStat, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stat.title, style = MaterialTheme.typography.labelMedium)
                Icon(
                    stat.icon,
                    contentDescription = null,
                    tint = if (stat.alert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            Text(
                stat.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (stat.alert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(stat.description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Contenedor común para las secciones amplias del dashboard. */
@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

/** Gráfico sencillo construido con Compose, sin una biblioteca externa. */
@Composable
private fun WeeklyProductionChart() {
    val values = listOf(72, 58, 76, 63, 88, 64, 49)
    val labels = listOf("L", "M", "X", "J", "V", "S", "D")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, value ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(value.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(labels[index], style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Resumen comparable de ingresos y gastos. */
@Composable
private fun FinancialSummary() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FinancialValue("Ingresos", "Q 12,500", MaterialTheme.colorScheme.primary)
        FinancialValue("Gastos", "Q 4,200", MaterialTheme.colorScheme.error)
        FinancialValue("Margen", "66 %", MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun FinancialValue(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, color = color, fontWeight = FontWeight.Bold)
    }
}

/** Indicadores de crecimiento de lotes mostrados en el prototipo. */
@Composable
private fun LotGrowthSummary() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GrowthRow("Lote A-01 · Engorde", "+2.6 lb/día", 0.88f)
        GrowthRow("Lote B-04 · Terneros", "+1.8 lb/día", 0.64f)
        GrowthRow("Lote C-02 · Vacas", "+0.9 lb/día", 0.42f)
    }
}

@Composable
private fun GrowthRow(label: String, value: String, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, color = MaterialTheme.colorScheme.primary)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Accesos directos adaptables: dos columnas en teléfono y más en tableta. */
@Composable
private fun QuickActions(onNavigate: (String) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 600.dp) 4 else 2
        val actions = listOf(
            Triple("Registrar animal", Routes.ANIMAL_LIST, Icons.Default.Pets),
            Triple("Registrar pesaje", Routes.WEIGHINGS, Icons.Default.Scale),
            Triple("Producción de leche", Routes.MILK_PRODUCTION, Icons.Default.LocalDrink),
            Triple("Registrar gasto", Routes.FINANCE, Icons.Default.Payments)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            actions.chunked(columns).forEach { rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowActions.forEach { (label, route, icon) ->
                        OutlinedButton(
                            onClick = { onNavigate(route) },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                        ) {
                            Icon(icon, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label)
                        }
                    }
                    repeat(columns - rowActions.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Notificaciones visuales pendientes de conexión con datos reales. */
@Composable
private fun NotificationSummary() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NotificationRow("Vacunación pendiente", "Lote B-04 · Revisión requerida")
        NotificationRow("Entrega de alimento", "Programada para hoy a las 08:30")
        NotificationRow("Reporte mensual", "Disponible para revisión")
    }
}

@Composable
private fun NotificationRow(title: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private val dashboardStats = listOf(
    DashboardStat("ANIMALES", "1,284", "+12 este mes", Icons.Default.Pets, Routes.ANIMAL_LIST),
    DashboardStat("LECHE HOY", "450 L", "Promedio estable", Icons.Default.LocalDrink, Routes.MILK_PRODUCTION),
    DashboardStat("INGRESOS", "Q 12,500", "+8 %", Icons.Default.AccountBalanceWallet, Routes.FINANCE),
    DashboardStat("GASTOS", "Q 4,200", "Insumos y alimento", Icons.Default.ShoppingCart, Routes.FINANCE, true),
    DashboardStat("VACUNAS", "12", "Pendientes", Icons.Default.Vaccines, Routes.ANIMAL_LIST, true),
    DashboardStat("EMPLEADOS", "9", "Personal registrado", Icons.Default.Groups, Routes.EMPLOYEES)
)

@Preview(showBackground = true, showSystemUi = true, widthDp = 800, heightDp = 1100)
@Composable
private fun DashboardTabletPreview() {
    GestionPecuariaTheme {
        DashboardScreen(onMenuClick = {}, onNavigate = {})
    }
}
