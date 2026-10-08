package com.fincahernandez.gestionpecuaria.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.fincahernandez.gestionpecuaria.data.remote.AutoSyncPhase
import com.fincahernandez.gestionpecuaria.data.remote.AutoSyncStatus
import com.fincahernandez.gestionpecuaria.ui.components.label
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
    modifier: Modifier = Modifier,
    syncStatus: AutoSyncStatus = AutoSyncStatus(AutoSyncPhase.WAITING),
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
                },
                actions = {
                    IconButton(onClick = onSyncClick) {
                        SyncCloudIcon(syncStatus)
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
                Text(
                    "Bienvenido, ${userName.ifBlank { "usuario" }}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Los montos financieros aparecen primero para facilitar decisiones inmediatas.
            if (Routes.FINANCE in allowedRoutes) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionCard(title = "Resumen financiero") {
                        FinancialSummary(data)
                    }
                }
            }

            items(stats, key = { it.title }) { stat ->
                StatCard(stat = stat, onClick = { onNavigate(stat.route) })
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
        }
    }
}

/** Una sola nube comunica conexión y estado de envío sin ocupar espacio con texto. */
@Composable
private fun SyncCloudIcon(status: AutoSyncStatus) {
    val (icon, color) = when (status.phase) {
        AutoSyncPhase.WAITING -> Icons.Default.CloudQueue to BalanceNeutralColor
        AutoSyncPhase.OFFLINE -> Icons.Default.CloudOff to MaterialTheme.colorScheme.error
        AutoSyncPhase.RUNNING -> Icons.Default.CloudSync to MaterialTheme.colorScheme.primary
        AutoSyncPhase.COMPLETE -> Icons.Default.CloudDone to BalanceHealthyColor
        AutoSyncPhase.PENDING -> Icons.Default.CloudUpload to SyncPendingColor
        AutoSyncPhase.REVIEW -> Icons.Default.CloudUpload to MaterialTheme.colorScheme.error
        AutoSyncPhase.RETRY -> Icons.Default.CloudOff to MaterialTheme.colorScheme.error
    }
    Icon(
        imageVector = icon,
        contentDescription = "Estado de la nube: ${status.label()}. Toca para sincronizar.",
        tint = color
    )
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
    val balanceSignal = balanceSignal(data.income, data.expenses)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FinancialValue("Ingresos", formatCurrency(data.income), BalanceHealthyColor)
        FinancialValue("Egresos", formatCurrency(data.expenses), MaterialTheme.colorScheme.error)
        FinancialValue(
            label = "Balance",
            value = formatCurrency(balance),
            valueColor = balanceSignal.color,
            status = balanceSignal.label
        )
    }
}

@Composable
private fun FinancialValue(
    label: String,
    value: String,
    valueColor: Color,
    status: String? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold)
        status?.let {
            Text(it, color = valueColor, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private data class BalanceSignal(val label: String, val color: Color)

/** Semáforo proporcional: diferencia un margen amplio, uno bajo y un déficit crítico. */
private fun balanceSignal(income: Double, expenses: Double): BalanceSignal {
    val balance = income - expenses
    return when {
        income == 0.0 && expenses == 0.0 -> BalanceSignal("SIN DATOS", BalanceNeutralColor)
        balance < 0.0 -> BalanceSignal("CRÍTICO", BalanceCriticalColor)
        income > 0.0 && balance / income < 0.25 -> BalanceSignal("POCO", BalanceLowColor)
        else -> BalanceSignal("BASTANTE", BalanceHealthyColor)
    }
}

private val BalanceHealthyColor = Color(0xFF147A36)
private val BalanceLowColor = Color(0xFF9A6700)
private val BalanceCriticalColor = Color(0xFFB3261E)
private val BalanceNeutralColor = Color(0xFF5F6368)
private val SyncPendingColor = Color(0xFF9A6700)

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
            onNavigate = {}
        )
    }
}
