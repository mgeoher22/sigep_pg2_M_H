package com.fincahernandez.gestionpecuaria.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes

/** Información necesaria para representar una opción del menú principal. */
private data class DrawerDestination(
    val label: String,
    val route: String,
    val icon: ImageVector
)

/**
 * Menú lateral de HU-01 con acceso a todos los módulos principales.
 */
@Composable
fun AppDrawerContent(
    selectedRoute: String?,
    onDestinationClick: (String) -> Unit
) {
    ModalDrawerSheet {
        // El desplazamiento permite acceder a todas las opciones en pantallas pequeñas.
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Finca Hernández",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                text = "Sistema de Gestión Pecuaria",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            // Mantiene visible que la aplicación todavía trabaja con datos simulados.
            DemoModeNotice(compact = true)
            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            drawerDestinations.forEach { destination ->
                NavigationDrawerItem(
                    label = { Text(destination.label) },
                    selected = selectedRoute == destination.route,
                    onClick = { onDestinationClick(destination.route) },
                    icon = {
                        Icon(destination.icon, contentDescription = null)
                    }
                )
            }
        }
    }
}

private val drawerDestinations = listOf(
    DrawerDestination("Dashboard", Routes.DASHBOARD, Icons.Default.Dashboard),
    DrawerDestination("Animales", Routes.ANIMAL_LIST, Icons.Default.Pets),
    DrawerDestination("Lotes", Routes.LOTS, Icons.AutoMirrored.Filled.List),
    DrawerDestination("Parcelas", Routes.PARCELS, Icons.Default.Landscape),
    DrawerDestination("Pesajes", Routes.WEIGHINGS, Icons.Default.Scale),
    DrawerDestination("Producción lechera", Routes.MILK_PRODUCTION, Icons.Default.LocalDrink),
    DrawerDestination("Finanzas", Routes.FINANCE, Icons.Default.AccountBalanceWallet),
    DrawerDestination("Empleados", Routes.EMPLOYEES, Icons.Default.Groups),
    DrawerDestination("Centro de reportes", Routes.REPORTS, Icons.Default.Assessment),
    DrawerDestination("Usuarios y accesos", Routes.USERS, Icons.Default.ManageAccounts)
)
