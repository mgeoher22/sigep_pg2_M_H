package com.fincahernandez.gestionpecuaria.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.ui.components.AppBottomBar
import com.fincahernandez.gestionpecuaria.ui.components.BrandedTopAppBar
import com.fincahernandez.gestionpecuaria.ui.components.LocalAllowedMainRoutes
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import com.fincahernandez.gestionpecuaria.ui.theme.GestionPecuariaTheme
import java.text.DecimalFormat
import java.util.Locale

/** Valores que el dashboard calcula a partir del estado actual de los módulos. */
data class DashboardUiData(
    val activeAnimals: Int = 0,
    val totalAnimals: Int = 0,
    val activeLots: Int = 0,
    val totalLots: Int = 0,
    val availableParcels: Int = 0,
    val totalParcels: Int = 0,
    val weighingRecords: Int = 0,
    val averageWeightPounds: Double? = null,
    val latestMilkLiters: Double? = null,
    val latestMilkDate: String = "",
    val income: Double = 0.0,
    val expenses: Double = 0.0,
    val activeEmployees: Int = 0,
    val totalEmployees: Int = 0,
    val activeUsers: Int = 0,
    val totalUsers: Int = 0,
    val recentItems: List<DashboardRecentItem> = emptyList()
)

/** Registro resumido procedente de uno de los módulos existentes. */
data class DashboardRecentItem(
    val title: String,
    val detail: String,
    val route: String
)

private data class DashboardStat(
    val title: String,
    val value: String,
    val description: String,
    val icon: ImageVector,
    val route: String
)

private data class QuickAction(
    val label: String,
    val destination: String,
    val permissionRoute: String,
    val icon: ImageVector
)

/**
 * Panel principal funcional. Todos los indicadores se reciben desde el estado
 * que administra AppNavigation, por lo que cambian después de cada registro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    data: DashboardUiData,
    userName: String,
    roleName: String,
    onMenuClick: () -> Unit,
    onNavigate: (String) -> Unit,
    onQuickAction: (String) -> Unit,
    modifier: Modifier = Modifier,
    syncStatus: String = "",
    onSyncClick: () -> Unit = {}
) {
    val allowedRoutes = LocalAllowedMainRoutes.current
    val stats = dashboardStats(data).filter { it.route in allowedRoutes }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = {
                    Column {
                        Text(
                            "Panel principal",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            roleName,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                    }
                }
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
            columns = GridCells.Adaptive(minSize = 175.dp),
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
                        "Bienvenido, ${userName.ifBlank { "usuario" }}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Resumen calculado con los registros disponibles en la aplicación.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (syncStatus.isNotBlank()) androidx.compose.material3.TextButton(onClick = onSyncClick) {
                        Text(syncStatus, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            items(stats, key = { it.title }) { stat ->
                StatCard(stat = stat, onClick = { onNavigate(stat.route) })
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionCard(title = "Acciones rápidas") {
                    QuickActions(
                        allowedRoutes = allowedRoutes,
                        onQuickAction = onQuickAction
                    )
                }
            }

            if (data.recentItems.any { it.route in allowedRoutes }) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionCard(title = "Actividad reciente") {
                        RecentActivity(
                            items = data.recentItems.filter { it.route in allowedRoutes },
                            onNavigate = onNavigate
                        )
                    }
                }
            }

            if (Routes.FINANCE in allowedRoutes) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionCard(title = "Resumen financiero") {
                        FinancialSummary(data)
                    }
                }
            }
        }
    }
}

/** Tarjeta seleccionable que abre el módulo relacionado. */
@Composable
private fun StatCard(stat: DashboardStat, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.height(148.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stat.title, style = MaterialTheme.typography.labelMedium)
                Icon(stat.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                stat.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                stat.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Contenedor visual compartido por las secciones amplias. */
@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

/** Botones que abren directamente los formularios ya implementados. */
@Composable
private fun QuickActions(
    allowedRoutes: Set<String>,
    onQuickAction: (String) -> Unit
) {
    val actions = listOf(
        QuickAction("Registrar animal", Routes.ANIMAL_FORM, Routes.ANIMAL_LIST, Icons.Default.Pets),
        QuickAction("Registrar pesaje", Routes.WEIGHING_FORM, Routes.WEIGHINGS, Icons.Default.Scale),
        QuickAction(
            "Registrar producción",
            Routes.MILK_PRODUCTION_FORM,
            Routes.MILK_PRODUCTION,
            Icons.Default.LocalDrink
        ),
        QuickAction("Registrar movimiento", Routes.FINANCE_FORM, Routes.FINANCE, Icons.Default.AccountBalanceWallet)
    ).filter { it.permissionRoute in allowedRoutes }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 700.dp) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            actions.chunked(columns).forEach { rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowActions.forEach { action ->
                        OutlinedButton(
                            onClick = { onQuickAction(action.destination) },
                            modifier = Modifier
                                .weight(1f)
                                .height(62.dp)
                        ) {
                            Icon(action.icon, contentDescription = null)
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                action.label,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
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

/** Últimos elementos agregados en los módulos visibles para el rol actual. */
@Composable
private fun RecentActivity(
    items: List<DashboardRecentItem>,
    onNavigate: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.take(5).forEach { item ->
            Card(
                onClick = { onNavigate(item.route) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        iconForRoute(item.route),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold)
                        Text(
                            item.detail,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** Totales calculados con los movimientos existentes, sin valores fijos. */
@Composable
private fun FinancialSummary(data: DashboardUiData) {
    val balance = data.income - data.expenses
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FinancialValue("Ingresos", formatCurrency(data.income))
        FinancialValue("Egresos", formatCurrency(data.expenses))
        FinancialValue("Balance", formatCurrency(balance))
    }
}

@Composable
private fun FinancialValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

private fun dashboardStats(data: DashboardUiData): List<DashboardStat> = listOf(
    DashboardStat(
        "ANIMALES",
        data.activeAnimals.toString(),
        "${data.totalAnimals} registros totales",
        Icons.Default.Pets,
        Routes.ANIMAL_LIST
    ),
    DashboardStat(
        "LOTES",
        data.activeLots.toString(),
        "${data.totalLots} lotes registrados",
        Icons.AutoMirrored.Filled.List,
        Routes.LOTS
    ),
    DashboardStat(
        "PARCELAS",
        data.availableParcels.toString(),
        "Disponibles de ${data.totalParcels}",
        Icons.Default.Landscape,
        Routes.PARCELS
    ),
    DashboardStat(
        "PESAJES",
        data.weighingRecords.toString(),
        data.averageWeightPounds?.let { "Promedio ${formatPounds(it)}" } ?: "Sin pesajes",
        Icons.Default.Scale,
        Routes.WEIGHINGS
    ),
    DashboardStat(
        "ÚLTIMA LECHE",
        data.latestMilkLiters?.let { "${formatNumber(it)} L" } ?: "Sin datos",
        data.latestMilkDate.ifBlank { "Sin producción registrada" },
        Icons.Default.LocalDrink,
        Routes.MILK_PRODUCTION
    ),
    DashboardStat(
        "BALANCE",
        formatCurrency(data.income - data.expenses),
        "Ingresos menos egresos",
        Icons.Default.AccountBalanceWallet,
        Routes.FINANCE
    ),
    DashboardStat(
        "EMPLEADOS",
        data.activeEmployees.toString(),
        "Activos de ${data.totalEmployees}",
        Icons.Default.Groups,
        Routes.EMPLOYEES
    ),
    DashboardStat(
        "USUARIOS",
        data.activeUsers.toString(),
        "Activos de ${data.totalUsers}",
        Icons.Default.ManageAccounts,
        Routes.USERS
    )
)

private fun iconForRoute(route: String): ImageVector = when (route) {
    Routes.ANIMAL_LIST -> Icons.Default.Pets
    Routes.WEIGHINGS -> Icons.Default.Scale
    Routes.MILK_PRODUCTION -> Icons.Default.LocalDrink
    Routes.FINANCE -> Icons.Default.AccountBalanceWallet
    Routes.LOTS -> Icons.AutoMirrored.Filled.List
    Routes.PARCELS -> Icons.Default.Landscape
    Routes.EMPLOYEES -> Icons.Default.Groups
    Routes.USERS -> Icons.Default.ManageAccounts
    else -> Icons.Default.Pets
}

private fun formatCurrency(value: Double): String =
    DecimalFormat("'Q' #,##0.00").format(value)

private fun formatNumber(value: Double): String =
    String.format(Locale.getDefault(), "%.1f", value)

private fun formatPounds(value: Double): String = "${formatNumber(value)} lb"

@Preview(showBackground = true, showSystemUi = true, widthDp = 800, heightDp = 1100)
@Composable
private fun DashboardTabletPreview() {
    GestionPecuariaTheme {
        DashboardScreen(
            data = DashboardUiData(
                activeAnimals = 6,
                totalAnimals = 7,
                activeLots = 2,
                totalLots = 2,
                availableParcels = 1,
                totalParcels = 3,
                weighingRecords = 4,
                averageWeightPounds = 512.5,
                latestMilkLiters = 118.0,
                latestMilkDate = "29/08/2026",
                income = 4250.0,
                expenses = 1850.0,
                activeEmployees = 2,
                totalEmployees = 2,
                activeUsers = 1,
                totalUsers = 1
            ),
            userName = "Administrador",
            roleName = "Administrador General",
            onMenuClick = {},
            onNavigate = {},
            onQuickAction = {}
        )
    }
}
