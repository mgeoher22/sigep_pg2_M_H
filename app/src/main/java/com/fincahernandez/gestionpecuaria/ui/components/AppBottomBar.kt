package com.fincahernandez.gestionpecuaria.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes

/** Rutas visibles para la cuenta autenticada en el árbol de Compose actual. */
val LocalAllowedMainRoutes = staticCompositionLocalOf { Routes.mainDestinations }

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/**
 * Barra inferior con los cuatro accesos de uso más frecuente.
 * Los demás módulos continúan disponibles desde el menú lateral.
 */
@Composable
fun AppBottomBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit
) {
    val allowedRoutes = LocalAllowedMainRoutes.current
    NavigationBar {
        bottomDestinations.filter { it.route in allowedRoutes }.forEach { destination ->
            NavigationBarItem(
                selected = selectedRoute == destination.route,
                onClick = { onNavigate(destination.route) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label) }
            )
        }
    }
}

private val bottomDestinations = listOf(
    BottomDestination(Routes.DASHBOARD, "Inicio", Icons.Default.Home),
    BottomDestination(Routes.ANIMAL_LIST, "Ganado", Icons.AutoMirrored.Filled.List),
    BottomDestination(Routes.WEIGHINGS, "Pesajes", Icons.Default.Scale),
    BottomDestination(Routes.FINANCE, "Finanzas", Icons.Default.AccountBalanceWallet)
)
