package com.fincahernandez.gestionpecuaria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
        showStatusBar()
        setContent {
            GestionPecuariaTheme {
                AppNavigation()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Algunas tabletas restauran el modo inmersivo al volver a la aplicación.
        showStatusBar()
    }

    /** Mantiene visibles y legibles la hora, batería y notificaciones del sistema. */
    private fun showStatusBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.statusBars())
            isAppearanceLightStatusBars = true
        }
    }
}
