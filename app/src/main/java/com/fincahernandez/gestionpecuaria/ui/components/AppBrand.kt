package com.fincahernandez.gestionpecuaria.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.R

/** Acción de cierre de sesión disponible para todas las pantallas autenticadas. */
val LocalLogoutAction = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * Logo vectorial de la finca. Al estar centralizado, un logo oficial podrá
 * sustituir este recurso sin modificar cada pantalla individualmente.
 */
@Composable
fun FarmLogo(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    Image(
        painter = painterResource(R.drawable.finca_logo),
        contentDescription = "Logo de Finca Hernández",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

/**
 * Encabezado compartido por todos los módulos y formularios.
 * Agrega el logo y una salida de sesión consistente sin duplicar lógica.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandedTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface
    ),
    showLogout: Boolean = true
) {
    val onLogout = LocalLogoutAction.current

    TopAppBar(
        modifier = modifier,
        navigationIcon = navigationIcon,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FarmLogo(size = 38.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) { title() }
            }
        },
        actions = {
            actions()
            if (showLogout && onLogout != null) {
                IconButton(onClick = onLogout) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Cerrar sesión",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        colors = colors
    )
}
