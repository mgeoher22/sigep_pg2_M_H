package com.fincahernandez.gestionpecuaria.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalConfirmationScreen
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalDetailScreen
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalFormData
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalListItem
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalListScreen
import java.util.UUID

/**
 * Conecta las pantallas del módulo y conserva los datos de demostración.
 *
 * Este estado es temporal y se reemplazará por un ViewModel conectado a Room
 * cuando el diseño del flujo haya sido aprobado.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var animales by remember { mutableStateOf(animalesIniciales) }
    var animalSeleccionado by remember { mutableStateOf(animalesIniciales.first()) }
    var ultimoRegistro by remember { mutableStateOf<AnimalListItem?>(null) }

    NavHost(
        navController = navController,
        startDestination = Routes.ANIMAL_LIST
    ) {
        composable(Routes.ANIMAL_LIST) {
            AnimalListScreen(
                animales = animales,
                onRegistrarAnimal = { navController.navigate(Routes.ANIMAL_FORM) },
                onAnimalClick = { animalId ->
                    animales.firstOrNull { it.id == animalId }?.let {
                        animalSeleccionado = it
                        navController.navigate(Routes.ANIMAL_DETAIL)
                    }
                }
            )
        }

        composable(Routes.ANIMAL_FORM) {
            AnimalFormScreen(
                onBack = { navController.popBackStack() },
                onSubmit = { formulario ->
                    val nuevoAnimal = formulario.toListItem()
                    animales = animales + nuevoAnimal
                    animalSeleccionado = nuevoAnimal
                    ultimoRegistro = nuevoAnimal
                    navController.navigate(Routes.ANIMAL_CONFIRMATION)
                }
            )
        }

        composable(Routes.ANIMAL_CONFIRMATION) {
            val animal = ultimoRegistro ?: animalSeleccionado
            AnimalConfirmationScreen(
                animal = animal,
                onViewProfile = { navController.navigate(Routes.ANIMAL_DETAIL) },
                onRegisterAnother = {
                    navController.navigate(Routes.ANIMAL_FORM) {
                        popUpTo(Routes.ANIMAL_CONFIRMATION) { inclusive = true }
                    }
                },
                onBackToList = {
                    navController.navigate(Routes.ANIMAL_LIST) {
                        popUpTo(Routes.ANIMAL_LIST) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.ANIMAL_DETAIL) {
            AnimalDetailScreen(
                animal = animalSeleccionado,
                onBack = { navController.popBackStack() },
                onEdit = {},
                onRegisterWeight = {}
            )
        }
    }
}

/** Convierte los valores del formulario al modelo utilizado por la lista. */
private fun AnimalFormData.toListItem() = AnimalListItem(
    id = UUID.randomUUID().toString(),
    codigoIdentificacion = codigoIdentificacion.trim(),
    nombre = nombre.trim().ifBlank { null },
    categoria = categoria,
    estado = estadoSalud,
    ultimoPesoKg = pesoInicial.toDoubleOrNull()
)

// Información ficticia para evaluar el diseño antes de conectar Room.
private val animalesIniciales = listOf(
    AnimalListItem(
        id = "demo-1",
        codigoIdentificacion = "FH-2024-88",
        nombre = "Luna",
        categoria = "LECHERO",
        estado = "EXCELENTE",
        ultimoPesoKg = 450.0
    ),
    AnimalListItem(
        id = "demo-2",
        codigoIdentificacion = "FH-2023-102",
        nombre = "Brahman 102",
        categoria = "ENGORDE",
        estado = "OBSERVACIÓN",
        ultimoPesoKg = 612.0
    )
)
