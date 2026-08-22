package com.fincahernandez.gestionpecuaria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.fincahernandez.gestionpecuaria.ui.navigation.AppNavigation
import com.fincahernandez.gestionpecuaria.ui.theme.GestionPecuariaTheme

/**
 * Punto de entrada de la aplicación.
 *
 * Por el momento muestra directamente la pantalla de animales. Cuando exista
 * navegación, esta actividad mostrará el contenedor principal de pantallas.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GestionPecuariaTheme {
                AppNavigation()
            }
        }
    }
}
