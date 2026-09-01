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
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotDetailScreen
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotAnimalOption
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotAnimalSelectionScreen
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.milk.MilkProductionFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.milk.MilkProductionListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.milk.MilkProductionUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.parcels.ParcelDetailScreen
import com.fincahernandez.gestionpecuaria.ui.screens.parcels.ParcelFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.parcels.ParcelListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.parcels.ParcelUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.placeholder.ModulePlaceholderScreen
import com.fincahernandez.gestionpecuaria.ui.screens.weighings.AnimalWeightOption
import com.fincahernandez.gestionpecuaria.ui.screens.weighings.WeighingFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.weighings.WeighingListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.weighings.WeighingUiModel
import java.util.Calendar
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
    var animalEnEdicionId by remember { mutableStateOf<String?>(null) }
    var lots by remember { mutableStateOf(initialLots) }
    var selectedLot by remember { mutableStateOf(initialLots.first()) }
    var lotDraftAnimalIds by remember { mutableStateOf(emptyList<String>()) }
    var parcels by remember { mutableStateOf(initialParcels) }
    var selectedParcel by remember { mutableStateOf(initialParcels.first()) }
    var weighings by remember { mutableStateOf(initialWeighings) }
    var milkProductionRecords by remember { mutableStateOf(initialMilkProductionRecords) }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val selectedMainRoute = when (currentRoute) {
        Routes.ANIMAL_FORM,
        Routes.ANIMAL_CONFIRMATION,
        Routes.ANIMAL_DETAIL -> Routes.ANIMAL_LIST
        Routes.LOT_FORM,
        Routes.LOT_ANIMAL_SELECTION,
        Routes.LOT_DETAIL -> Routes.LOTS
        Routes.PARCEL_FORM,
        Routes.PARCEL_DETAIL -> Routes.PARCELS
        Routes.WEIGHING_FORM -> Routes.WEIGHINGS
        Routes.MILK_PRODUCTION_FORM -> Routes.MILK_PRODUCTION
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
                    onRegistrarAnimal = {
                        animalEnEdicionId = null
                        navController.navigate(Routes.ANIMAL_FORM)
                    },
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
                val animalEnEdicion = animales.firstOrNull { it.id == animalEnEdicionId }
                AnimalFormScreen(
                    codigoGenerado = animalEnEdicion?.codigoIdentificacion
                        ?: generarCodigoAnimal(animales),
                    initialData = animalEnEdicion?.toFormData(),
                    onBack = { navController.popBackStack() },
                    onSubmit = { formulario ->
                        val animalGuardado = formulario.toListItem(
                            id = animalEnEdicion?.id ?: UUID.randomUUID().toString()
                        )

                        if (animalEnEdicion != null) {
                            // Sustituye el elemento existente y vuelve al perfil actualizado.
                            animales = animales.map { actual ->
                                if (actual.id == animalGuardado.id) animalGuardado else actual
                            }
                            animalSeleccionado = animalGuardado
                            animalEnEdicionId = null
                            val regresoAlDetalle = navController.popBackStack(
                                route = Routes.ANIMAL_DETAIL,
                                inclusive = false
                            )
                            if (!regresoAlDetalle) {
                                navController.navigate(Routes.ANIMAL_DETAIL)
                            }
                        } else {
                            animales = animales + animalGuardado
                            animalSeleccionado = animalGuardado
                            ultimoRegistro = animalGuardado
                            navController.navigate(Routes.ANIMAL_CONFIRMATION)
                        }
                    }
                )
            }

            composable(Routes.ANIMAL_CONFIRMATION) {
                val animal = ultimoRegistro ?: animalSeleccionado
                AnimalConfirmationScreen(
                    animal = animal,
                    onViewProfile = { navController.navigate(Routes.ANIMAL_DETAIL) },
                    onRegisterAnother = {
                        animalEnEdicionId = null
                        navController.navigate(Routes.ANIMAL_FORM) {
                            popUpTo(Routes.ANIMAL_CONFIRMATION) { inclusive = true }
                        }
                    },
                    onBackToList = {
                        // Regresa al listado existente y elimina formulario/confirmación
                        // del historial para que Atrás no vuelva a abrir el registro.
                        val regresoExitoso = navController.popBackStack(
                            route = Routes.ANIMAL_LIST,
                            inclusive = false
                        )
                        if (!regresoExitoso) {
                            navigateMain(Routes.ANIMAL_LIST)
                        }
                    },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.ANIMAL_DETAIL) {
                AnimalDetailScreen(
                    animal = animalSeleccionado,
                    onBack = { navController.popBackStack() },
                    onEdit = {
                        animalEnEdicionId = animalSeleccionado.id
                        navController.navigate(Routes.ANIMAL_FORM)
                    },
                    onRegisterWeight = { navigateMain(Routes.WEIGHINGS) },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.LOTS) {
                LotListScreen(
                    lots = lots,
                    onMenuClick = openDrawer,
                    onCreateLot = {
                        // Cada lote nuevo comienza sin conservar selecciones de un formulario anterior.
                        lotDraftAnimalIds = emptyList()
                        navController.navigate(Routes.LOT_FORM)
                    },
                    onLotClick = { lotId ->
                        lots.firstOrNull { it.id == lotId }?.let { lot ->
                            selectedLot = lot
                            navController.navigate(Routes.LOT_DETAIL)
                        }
                    },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.LOT_FORM) {
                LotFormScreen(
                    parcelNames = parcels.map { it.name },
                    animalOptions = animales.map { animal ->
                        LotAnimalOption(
                            id = animal.id,
                            code = animal.codigoIdentificacion,
                            name = animal.nombre,
                            category = animal.categoria,
                            weightLibras = animal.ultimoPesoLibras
                        )
                    },
                    selectedAnimalIds = lotDraftAnimalIds,
                    onOpenAnimalSelector = {
                        navController.navigate(Routes.LOT_ANIMAL_SELECTION)
                    },
                    onBack = { navController.popBackStack() },
                    onSubmit = { form ->
                        val newLot = LotUiModel(
                            id = UUID.randomUUID().toString(),
                            code = generateLotCode(lots),
                            name = form.name,
                            type = form.type,
                            status = "ACTIVO",
                            parcelName = form.parcelName,
                            initialAverageWeight = form.initialAverageWeight.toDoubleOrNull(),
                            targetWeight = form.targetWeight.toDoubleOrNull(),
                            estimatedExitDate = form.estimatedExitDate,
                            selectedAnimalIds = form.selectedAnimalIds
                        )
                        lots = lots + newLot
                        selectedLot = newLot
                        navController.navigate(Routes.LOT_DETAIL) {
                            popUpTo(Routes.LOT_FORM) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.LOT_ANIMAL_SELECTION) {
                LotAnimalSelectionScreen(
                    animals = animales.map { animal ->
                        LotAnimalOption(
                            id = animal.id,
                            code = animal.codigoIdentificacion,
                            name = animal.nombre,
                            category = animal.categoria,
                            weightLibras = animal.ultimoPesoLibras
                        )
                    },
                    initiallySelectedIds = lotDraftAnimalIds,
                    onBack = { navController.popBackStack() },
                    onConfirm = { selectedIds ->
                        lotDraftAnimalIds = selectedIds
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.LOT_DETAIL) {
                LotDetailScreen(
                    lot = selectedLot,
                    selectedAnimalLabels = animales
                        .filter { it.id in selectedLot.selectedAnimalIds }
                        .map { it.nombre ?: it.codigoIdentificacion },
                    onBack = { navController.popBackStack() },
                    onRegisterWeight = { navigateMain(Routes.WEIGHINGS) },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.PARCELS) {
                ParcelListScreen(
                    parcels = parcels,
                    onMenuClick = openDrawer,
                    onCreateParcel = { navController.navigate(Routes.PARCEL_FORM) },
                    onParcelClick = { parcelId ->
                        parcels.firstOrNull { it.id == parcelId }?.let { parcel ->
                            selectedParcel = parcel
                            navController.navigate(Routes.PARCEL_DETAIL)
                        }
                    },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.PARCEL_FORM) {
                ParcelFormScreen(
                    onBack = { navController.popBackStack() },
                    onSubmit = { form ->
                        val newParcel = ParcelUiModel(
                            id = UUID.randomUUID().toString(),
                            code = generateParcelCode(parcels),
                            name = form.name,
                            areaHectares = form.areaHectares.toDoubleOrNull() ?: 0.0,
                            pastureType = form.pastureType,
                            status = "DISPONIBLE",
                            capacity = form.capacity.toIntOrNull(),
                            currentLot = "",
                            productivityPercent = 100
                        )
                        parcels = parcels + newParcel
                        selectedParcel = newParcel
                        navController.navigate(Routes.PARCEL_DETAIL) {
                            popUpTo(Routes.PARCEL_FORM) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.PARCEL_DETAIL) {
                ParcelDetailScreen(
                    parcel = selectedParcel,
                    onBack = { navController.popBackStack() },
                    onNavigateMain = navigateMain
                )
            }
            composable(Routes.WEIGHINGS) {
                WeighingListScreen(
                    weighings = weighings,
                    onMenuClick = openDrawer,
                    onCreateWeighing = { navController.navigate(Routes.WEIGHING_FORM) },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.WEIGHING_FORM) {
                val animalOptions = animales.map { animal ->
                    AnimalWeightOption(
                        id = animal.id,
                        label = buildString {
                            append(animal.codigoIdentificacion)
                            animal.nombre?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
                        },
                        previousWeightLibras = animal.ultimoPesoLibras
                    )
                }
                WeighingFormScreen(
                    animalOptions = animalOptions,
                    onBack = { navController.popBackStack() },
                    onSubmit = { form ->
                        val selectedAnimal = animales.first { it.id == form.animalId }
                        val newWeight = form.weightLibras.toDouble()
                        val record = WeighingUiModel(
                            id = UUID.randomUUID().toString(),
                            animalId = selectedAnimal.id,
                            animalLabel = buildString {
                                append(selectedAnimal.codigoIdentificacion)
                                selectedAnimal.nombre?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
                            },
                            weightLibras = newWeight,
                            date = form.date,
                            notes = form.notes
                        )
                        weighings = weighings + record

                        // Mantiene coherente el peso mostrado en listado y perfil del animal.
                        animales = animales.map { animal ->
                            if (animal.id == selectedAnimal.id) {
                                animal.copy(ultimoPesoLibras = newWeight)
                            } else {
                                animal
                            }
                        }
                        if (animalSeleccionado.id == selectedAnimal.id) {
                            animalSeleccionado = animalSeleccionado.copy(ultimoPesoLibras = newWeight)
                        }

                        if (!navController.popBackStack(Routes.WEIGHINGS, false)) {
                            navigateMain(Routes.WEIGHINGS)
                        }
                    }
                )
            }

            composable(Routes.MILK_PRODUCTION) {
                MilkProductionListScreen(
                    records = milkProductionRecords,
                    onMenuClick = openDrawer,
                    onCreateRecord = { navController.navigate(Routes.MILK_PRODUCTION_FORM) },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.MILK_PRODUCTION_FORM) {
                MilkProductionFormScreen(
                    onBack = { navController.popBackStack() },
                    onSubmit = { form ->
                        val dailyRecord = MilkProductionUiModel(
                            id = UUID.randomUUID().toString(),
                            date = form.date,
                            liters = form.liters.toDouble(),
                            notes = form.notes
                        )

                        // Solo existe un total por día; registrar la misma fecha corrige el valor anterior.
                        milkProductionRecords = milkProductionRecords
                            .filterNot { it.date == form.date } + dailyRecord
                        if (!navController.popBackStack(Routes.MILK_PRODUCTION, false)) {
                            navigateMain(Routes.MILK_PRODUCTION)
                        }
                    }
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

/**
 * Genera un código consecutivo para que el usuario no tenga que escribirlo.
 * En HU-07 esta responsabilidad se moverá al repositorio conectado con Room.
 */
private fun generarCodigoAnimal(animales: List<AnimalListItem>): String {
    val anioActual = Calendar.getInstance().get(Calendar.YEAR)
    val prefijo = "FH-$anioActual-"
    val ultimoConsecutivo = animales
        .mapNotNull { animal ->
            animal.codigoIdentificacion
                .takeIf { it.startsWith(prefijo) }
                ?.removePrefix(prefijo)
                ?.toIntOrNull()
        }
        .maxOrNull() ?: 0

    return prefijo + (ultimoConsecutivo + 1).toString().padStart(3, '0')
}

/** Conserva los valores del formulario para mostrarlos en confirmación y detalle. */
private fun AnimalFormData.toListItem(id: String) = AnimalListItem(
    id = id,
    codigoIdentificacion = codigoIdentificacion.trim(),
    nombre = nombre.trim().ifBlank { null },
    categoria = categoria,
    estado = estadoSalud,
    ultimoPesoLibras = pesoInicial.toDoubleOrNull(),
    raza = raza.trim(),
    sexo = sexo,
    tipoOrigen = tipoOrigen,
    fechaNacimiento = fechaNacimiento,
    fechaIngreso = fechaIngreso,
    procedencia = procedencia.trim(),
    observaciones = observaciones.trim()
)

/** Recupera los datos del animal para precargar el formulario de edición. */
private fun AnimalListItem.toFormData() = AnimalFormData(
    codigoIdentificacion = codigoIdentificacion,
    nombre = nombre.orEmpty(),
    raza = raza,
    sexo = sexo,
    categoria = categoria,
    tipoOrigen = tipoOrigen,
    fechaNacimiento = fechaNacimiento,
    fechaIngreso = fechaIngreso,
    estadoSalud = estado,
    pesoInicial = ultimoPesoLibras?.toString().orEmpty(),
    procedencia = procedencia,
    observaciones = observaciones
)

/** Genera identificadores consecutivos mientras HU-04 trabaja con datos temporales. */
private fun generateLotCode(lots: List<LotUiModel>): String =
    "LOT-${(lots.size + 1).toString().padStart(3, '0')}"

private fun generateParcelCode(parcels: List<ParcelUiModel>): String =
    "PR-${(parcels.size + 1).toString().padStart(3, '0')}"

// Información ficticia para evaluar el diseño antes de conectar Room.
private val animalesIniciales = listOf(
    AnimalListItem(
        id = "demo-1",
        codigoIdentificacion = "FH-2024-88",
        nombre = "Luna",
        categoria = "LECHERO",
        estado = "EXCELENTE",
        ultimoPesoLibras = 450.0,
        raza = "Holstein",
        sexo = "HEMBRA",
        tipoOrigen = "NACIDO_EN_FINCA",
        fechaNacimiento = "12/08/2024",
        fechaIngreso = "12/08/2024",
        procedencia = "Parcela Norte"
    ),
    AnimalListItem(
        id = "demo-2",
        codigoIdentificacion = "FH-2023-102",
        nombre = "Brahman 102",
        categoria = "ENGORDE",
        estado = "OBSERVACIÓN",
        ultimoPesoLibras = 612.0,
        raza = "Brahman",
        sexo = "MACHO",
        tipoOrigen = "INGRESADO_A_FINCA",
        fechaNacimiento = "05/02/2023",
        fechaIngreso = "18/06/2024",
        procedencia = "Criadero regional"
    )
)

// Datos demostrativos para validar visualmente HU-04 antes de conectar Room.
private val initialLots = listOf(
    LotUiModel(
        id = "lot-demo-1",
        code = "LOT-001",
        name = "Lote Alpha-2024",
        type = "ENGORDE",
        status = "ACTIVO",
        parcelName = "Potrero Norte",
        initialAverageWeight = 420.0,
        targetWeight = 520.0,
        estimatedExitDate = "15/12/2026",
        selectedAnimalIds = listOf("demo-2")
    ),
    LotUiModel(
        id = "lot-demo-2",
        code = "LOT-002",
        name = "Lote Lechero A-2",
        type = "LECHERO",
        status = "ACTIVO",
        parcelName = "Loma del Sol",
        initialAverageWeight = 450.0,
        targetWeight = 480.0,
        estimatedExitDate = "",
        selectedAnimalIds = listOf("demo-1")
    )
)

private val initialParcels = listOf(
    ParcelUiModel(
        id = "parcel-demo-1",
        code = "PR-001",
        name = "Potrero Norte",
        areaHectares = 15.4,
        pastureType = "Brachiaria",
        status = "OCUPADA",
        capacity = 45,
        currentLot = "Lote Alpha-2024",
        productivityPercent = 88
    ),
    ParcelUiModel(
        id = "parcel-demo-2",
        code = "PR-002",
        name = "Loma del Sol",
        areaHectares = 22.1,
        pastureType = "Mombasa",
        status = "DISPONIBLE",
        capacity = 60,
        currentLot = "",
        productivityPercent = 95
    ),
    ParcelUiModel(
        id = "parcel-demo-3",
        code = "PR-003",
        name = "Bajo Húmedo",
        areaHectares = 10.2,
        pastureType = "Pasto mixto",
        status = "DESCANSO",
        capacity = 28,
        currentLot = "",
        productivityPercent = 45
    )
)

private val initialWeighings = listOf(
    WeighingUiModel(
        id = "weight-demo-1",
        animalId = "demo-1",
        animalLabel = "FH-2024-88 • Luna",
        weightLibras = 450.0,
        date = "28/08/2026",
        notes = "Condición corporal estable"
    ),
    WeighingUiModel(
        id = "weight-demo-2",
        animalId = "demo-2",
        animalLabel = "FH-2023-102 • Brahman 102",
        weightLibras = 612.0,
        date = "29/08/2026",
        notes = "Seguimiento de engorde"
    )
)

private val initialMilkProductionRecords = listOf(
    MilkProductionUiModel(
        id = "milk-demo-1",
        date = "28/08/2026",
        liters = 24.5,
        notes = "Producción normal"
    ),
    MilkProductionUiModel(
        id = "milk-demo-2",
        date = "29/08/2026",
        liters = 118.0,
        notes = "Sin observaciones"
    )
)
