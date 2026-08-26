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
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes

/**
 * Barra inferior con los cuatro accesos de uso más frecuente.
 * Los demás módulos continúan disponibles desde el menú lateral.
 */
@Composable
fun AppBottomBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedRoute == Routes.DASHBOARD,
            onClick = { onNavigate(Routes.DASHBOARD) },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Inicio") }
        )
        NavigationBarItem(
            selected = selectedRoute == Routes.ANIMAL_LIST,
            onClick = { onNavigate(Routes.ANIMAL_LIST) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text("Ganado") }
        )
        NavigationBarItem(
            selected = selectedRoute == Routes.WEIGHINGS,
            onClick = { onNavigate(Routes.WEIGHINGS) },
            icon = { Icon(Icons.Default.Scale, contentDescription = null) },
            label = { Text("Pesajes") }
        )
        NavigationBarItem(
            selected = selectedRoute == Routes.FINANCE,
            onClick = { onNavigate(Routes.FINANCE) },
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
            label = { Text("Finanzas") }
        )
    }
}
