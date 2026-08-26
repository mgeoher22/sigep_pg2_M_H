package com.fincahernandez.gestionpecuaria.ui.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fincahernandez.gestionpecuaria.ui.components.AppDrawerContent
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalConfirmationScreen
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalDetailScreen
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalFormData
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalListItem
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.dashboard.DashboardScreen
import com.fincahernandez.gestionpecuaria.ui.screens.placeholder.ModulePlaceholderScreen
import java.util.UUID
import kotlinx.coroutines.launch

/**
 * Conecta los destinos principales y las pantallas internas de animales.
 *
 * Este estado es temporal y se reemplazará por un ViewModel conectado a Room
 * cuando el diseño del flujo haya sido aprobado.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var animales by remember { mutableStateOf(animalesIniciales) }
    var animalSeleccionado by remember { mutableStateOf(animalesIniciales.first()) }
    var ultimoRegistro by remember { mutableStateOf<AnimalListItem?>(null) }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val selectedMainRoute = when (currentRoute) {
        Routes.ANIMAL_FORM,
        Routes.ANIMAL_CONFIRMATION,
        Routes.ANIMAL_DETAIL -> Routes.ANIMAL_LIST
        else -> currentRoute
    }

    /** Navega entre secciones principales sin acumular copias de la misma pantalla. */
    val navigateMain: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Routes.DASHBOARD) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    /** Abre el menú lateral desde las pantallas principales. */
    val openDrawer: () -> Unit = {
        coroutineScope.launch { drawerState.open() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute in Routes.mainDestinations,
        drawerContent = {
            AppDrawerContent(
                selectedRoute = selectedMainRoute,
                onDestinationClick = { route ->
                    coroutineScope.launch { drawerState.close() }
                    navigateMain(route)
                }
            )
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onMenuClick = openDrawer,
                    onNavigate = navigateMain
                )
            }

            composable(Routes.ANIMAL_LIST) {
                AnimalListScreen(
                    animales = animales,
                    onRegistrarAnimal = { navController.navigate(Routes.ANIMAL_FORM) },
                    onAnimalClick = { animalId ->
                        animales.firstOrNull { it.id == animalId }?.let {
                            animalSeleccionado = it
                            navController.navigate(Routes.ANIMAL_DETAIL)
                        }
                    },
                    onMenuClick = openDrawer,
                    onNavigateMain = navigateMain
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
                    onBackToList = { navigateMain(Routes.ANIMAL_LIST) },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.ANIMAL_DETAIL) {
                AnimalDetailScreen(
                    animal = animalSeleccionado,
                    onBack = { navController.popBackStack() },
                    onEdit = {},
                    onRegisterWeight = {},
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.LOTS) {
                ModulePlaceholderScreen(
                    title = "Gestión de lotes",
                    sprintReference = "HU-04",
                    route = Routes.LOTS,
                    onMenuClick = openDrawer,
                    onNavigate = navigateMain
                )
            }
            composable(Routes.PARCELS) {
                ModulePlaceholderScreen(
                    title = "Gestión de parcelas",
                    sprintReference = "HU-04",
                    route = Routes.PARCELS,
                    onMenuClick = openDrawer,
                    onNavigate = navigateMain
                )
            }
            composable(Routes.WEIGHINGS) {
                ModulePlaceholderScreen(
                    title = "Gestión de pesajes",
                    sprintReference = "HU-05",
                    route = Routes.WEIGHINGS,
                    onMenuClick = openDrawer,
                    onNavigate = navigateMain
                )
            }
            composable(Routes.MILK_PRODUCTION) {
                ModulePlaceholderScreen(
                    title = "Producción lechera",
                    sprintReference = "HU-05",
                    route = Routes.MILK_PRODUCTION,
                    onMenuClick = openDrawer,
                    onNavigate = navigateMain
                )
            }
            composable(Routes.FINANCE) {
                ModulePlaceholderScreen(
                    title = "Finanzas",
                    sprintReference = "HU-06",
                    route = Routes.FINANCE,
                    onMenuClick = openDrawer,
                    onNavigate = navigateMain
                )
            }
            composable(Routes.EMPLOYEES) {
                ModulePlaceholderScreen(
                    title = "Empleados",
                    sprintReference = "HU-06",
                    route = Routes.EMPLOYEES,
                    onMenuClick = openDrawer,
                    onNavigate = navigateMain
                )
            }
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
