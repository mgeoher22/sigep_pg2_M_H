package com.fincahernandez.gestionpecuaria.ui.navigation

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fincahernandez.gestionpecuaria.ui.components.AppDrawerContent
import com.fincahernandez.gestionpecuaria.ui.components.LocalAllowedMainRoutes
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.AnimalEntity
import com.fincahernandez.gestionpecuaria.data.repository.AnimalRepository
import com.fincahernandez.gestionpecuaria.data.repository.AnimalStoredRecord
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticatedUser
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticationResult
import com.fincahernandez.gestionpecuaria.data.repository.UserRepository
import com.fincahernandez.gestionpecuaria.data.security.SessionManager
import com.fincahernandez.gestionpecuaria.data.security.permissionsForRole
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalConfirmationScreen
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalDetailScreen
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalFormData
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalListItem
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.auth.LoginScreen
import com.fincahernandez.gestionpecuaria.ui.screens.auth.InitialAdminSetupScreen
import com.fincahernandez.gestionpecuaria.ui.screens.auth.SplashScreen
import com.fincahernandez.gestionpecuaria.ui.screens.dashboard.DashboardScreen
import com.fincahernandez.gestionpecuaria.ui.screens.employees.EmployeeFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.employees.EmployeeListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.employees.EmployeeUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.finance.FinanceListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.finance.FinancialMovementFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.finance.FinancialMovementUiModel
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
import com.fincahernandez.gestionpecuaria.ui.screens.reports.ReportDashboardData
import com.fincahernandez.gestionpecuaria.ui.screens.reports.ReportsCenterScreen
import com.fincahernandez.gestionpecuaria.ui.screens.users.RolePermissionsScreen
import com.fincahernandez.gestionpecuaria.ui.screens.users.UserFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.users.UserManagementScreen
import com.fincahernandez.gestionpecuaria.ui.screens.users.UserUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.weighings.AnimalWeightOption
import com.fincahernandez.gestionpecuaria.ui.screens.weighings.WeighingFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.weighings.WeighingListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.weighings.WeighingUiModel
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Conecta los destinos principales y las pantallas internas de animales.
 *
 * Los animales y sus pesajes se obtienen de Room. Los demás módulos conservan
 * estado temporal hasta que sus historias funcionales se implementen.
 */
@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val isDebuggable = remember(context) {
        context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val database = remember(context) { GestionPecuariaDatabase.obtenerInstancia(context) }
    val animalRepository = remember(database) { AnimalRepository(database) }
    val userRepository = remember(database) { UserRepository(database) }
    val sessionManager = remember(context) { SessionManager(context) }
    val storedAnimalsFlow = remember(animalRepository) { animalRepository.observeAnimals() }
    val storedAnimals by storedAnimalsFlow.collectAsState(initial = emptyList())
    val storedUsersFlow = remember(userRepository) { userRepository.observeUsers() }
    val storedUsers by storedUsersFlow.collectAsState(initial = emptyList())
    val users = storedUsers.map { user ->
        UserUiModel(
            id = user.id,
            fullName = user.fullName,
            username = user.username,
            roleName = user.roleName,
            active = user.active
        )
    }
    val allAnimalItems = storedAnimals.map { it.toListItem() }
    val animales = storedAnimals
        .filter { it.animal.estado == "ACTIVO" }
        .map { it.toListItem() }
    var animalSeleccionado by remember { mutableStateOf<AnimalListItem?>(null) }
    var ultimoRegistro by remember { mutableStateOf<AnimalListItem?>(null) }
    var animalEnEdicionId by remember { mutableStateOf<String?>(null) }
    var animalSaveError by remember { mutableStateOf<String?>(null) }
    var animalIsSaving by remember { mutableStateOf(false) }
    var lots by remember { mutableStateOf(initialLots) }
    var selectedLot by remember { mutableStateOf(initialLots.first()) }
    var lotDraftAnimalIds by remember { mutableStateOf(emptyList<String>()) }
    var parcels by remember { mutableStateOf(initialParcels) }
    var selectedParcel by remember { mutableStateOf(initialParcels.first()) }
    var weighings by remember { mutableStateOf(initialWeighings) }
    var milkProductionRecords by remember { mutableStateOf(initialMilkProductionRecords) }
    var financialMovements by remember { mutableStateOf(initialFinancialMovements) }
    var employees by remember { mutableStateOf(initialEmployees) }
    var currentUser by remember { mutableStateOf<AuthenticatedUser?>(null) }
    var loginIsLoading by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var setupIsSaving by remember { mutableStateOf(false) }
    var setupError by remember { mutableStateOf<String?>(null) }
    var userIsSaving by remember { mutableStateOf(false) }
    var userSaveError by remember { mutableStateOf<String?>(null) }

    val allowedMainRoutes = allowedRoutesForRole(currentUser?.roleName)
    val allowedReportIds = allowedReportsForRole(currentUser?.roleName)

    // Mantiene abierto el perfil con la versión más reciente emitida por Room.
    LaunchedEffect(animales) {
        animalSeleccionado?.id?.let { selectedId ->
            animales.firstOrNull { it.id == selectedId }?.let { updated ->
                animalSeleccionado = updated
            }
        }
    }

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
        Routes.FINANCE_FORM -> Routes.FINANCE
        Routes.EMPLOYEE_FORM -> Routes.EMPLOYEES
        Routes.USER_FORM,
        Routes.ROLE_PERMISSIONS -> Routes.USERS
        else -> currentRoute
    }

    /** Navega entre secciones principales sin acumular copias de la misma pantalla. */
    val navigateMain: (String) -> Unit = { route ->
        if (route in allowedMainRoutes) {
            navController.navigate(route) {
                popUpTo(Routes.DASHBOARD) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    /** Abre el menú lateral desde las pantallas principales. */
    val openDrawer: () -> Unit = {
        coroutineScope.launch { drawerState.open() }
    }

    CompositionLocalProvider(LocalAllowedMainRoutes provides allowedMainRoutes) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = currentUser != null && currentRoute in Routes.mainDestinations,
            drawerContent = {
                AppDrawerContent(
                    selectedRoute = selectedMainRoute,
                    allowedRoutes = allowedMainRoutes,
                    userName = currentUser?.fullName.orEmpty(),
                    roleName = currentUser?.roleName.orEmpty(),
                    onDestinationClick = { route ->
                        coroutineScope.launch { drawerState.close() }
                        navigateMain(route)
                    },
                    onLogout = {
                        sessionManager.clear()
                        currentUser = null
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.DASHBOARD) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH
            ) {
            composable(Routes.SPLASH) {
                SplashScreen()
                LaunchedEffect(Unit) {
                    val startedAt = System.currentTimeMillis()
                    val hasUsers = userRepository.hasUsers()
                    val rememberedUser = sessionManager.rememberedUserId()
                        ?.let { userRepository.activeUserById(it) }
                    if (rememberedUser == null) sessionManager.clear()
                    currentUser = rememberedUser

                    val elapsed = System.currentTimeMillis() - startedAt
                    delay((1_700L - elapsed).coerceAtLeast(0L))
                    val destination = when {
                        !hasUsers -> Routes.SETUP_ADMIN
                        rememberedUser != null -> Routes.DASHBOARD
                        else -> Routes.LOGIN
                    }
                    navController.navigate(destination) {
                        // La pantalla de carga solo debe mostrarse durante el inicio.
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            }

            composable(Routes.SETUP_ADMIN) {
                InitialAdminSetupScreen(
                    isSaving = setupIsSaving,
                    errorMessage = setupError,
                    onClearError = { setupError = null },
                    onCreateAdmin = { fullName, username, password ->
                        setupIsSaving = true
                        setupError = null
                        coroutineScope.launch {
                            runCatching {
                                userRepository.createUser(
                                    fullName = fullName,
                                    username = username,
                                    password = password,
                                    roleName = "Administrador General"
                                )
                            }.onSuccess {
                                setupIsSaving = false
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(Routes.SETUP_ADMIN) { inclusive = true }
                                }
                            }.onFailure { error ->
                                setupIsSaving = false
                                setupError = userCreationError(error)
                            }
                        }
                    }
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    isLoading = loginIsLoading,
                    errorMessage = loginError,
                    onClearError = { loginError = null },
                    // La recuperación local solo se muestra en compilaciones de prueba.
                    // Borra cuentas y sesión, pero conserva los datos productivos.
                    onResetAccess = if (isDebuggable) {
                        {
                            loginIsLoading = true
                            loginError = null
                            coroutineScope.launch {
                                runCatching {
                                    userRepository.resetLocalAccess()
                                    sessionManager.clear()
                                }.onSuccess {
                                    loginIsLoading = false
                                    currentUser = null
                                    navController.navigate(Routes.SETUP_ADMIN) {
                                        popUpTo(Routes.LOGIN) { inclusive = true }
                                    }
                                }.onFailure {
                                    loginIsLoading = false
                                    loginError = "No fue posible restablecer el acceso."
                                }
                            }
                        }
                    } else {
                        null
                    },
                    onLogin = { username, password, rememberSession ->
                        loginIsLoading = true
                        loginError = null
                        coroutineScope.launch {
                            when (val result = userRepository.authenticate(username, password)) {
                                is AuthenticationResult.Success -> {
                                    loginIsLoading = false
                                    currentUser = result.user
                                    if (rememberSession) {
                                        sessionManager.remember(result.user.id)
                                    } else {
                                        sessionManager.clear()
                                    }
                                    navController.navigate(Routes.DASHBOARD) {
                                        popUpTo(Routes.LOGIN) { inclusive = true }
                                    }
                                }
                                AuthenticationResult.InvalidCredentials -> {
                                    loginIsLoading = false
                                    loginError = "Usuario o contraseña incorrectos."
                                }
                                AuthenticationResult.InactiveAccount -> {
                                    loginIsLoading = false
                                    loginError =
                                        "Esta cuenta está inactiva. Consulte al Administrador General."
                                }
                            }
                        }
                    }
                )
            }

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
                        animalSaveError = null
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
                        ?: generarCodigoAnimal(allAnimalItems),
                    initialData = animalEnEdicion?.toFormData(),
                    saveError = animalSaveError,
                    isSaving = animalIsSaving,
                    onBack = { navController.popBackStack() },
                    onSubmit = { formulario ->
                        val animalGuardado = formulario.toListItem(
                            id = animalEnEdicion?.id ?: UUID.randomUUID().toString()
                        )
                        animalIsSaving = true
                        animalSaveError = null

                        coroutineScope.launch {
                            runCatching {
                                animalRepository.saveAnimal(
                                    animal = formulario.toEntity(animalGuardado.id),
                                    weightPounds = formulario.pesoInicial.toDoubleOrNull()
                                )
                            }.onSuccess {
                                animalIsSaving = false
                                animalSeleccionado = animalGuardado
                                animalEnEdicionId = null

                                if (animalEnEdicion != null) {
                                    val regresoAlDetalle = navController.popBackStack(
                                        route = Routes.ANIMAL_DETAIL,
                                        inclusive = false
                                    )
                                    if (!regresoAlDetalle) {
                                        navController.navigate(Routes.ANIMAL_DETAIL)
                                    }
                                } else {
                                    ultimoRegistro = animalGuardado
                                    navController.navigate(Routes.ANIMAL_CONFIRMATION)
                                }
                            }.onFailure { error ->
                                animalIsSaving = false
                                animalSaveError = if (
                                    error.message.orEmpty().contains("UNIQUE", ignoreCase = true)
                                ) {
                                    "Ese código de identificación ya existe. Intente nuevamente."
                                } else {
                                    "No fue posible guardar el animal. Verifique los datos e inténtelo otra vez."
                                }
                            }
                        }
                    }
                )
            }

            composable(Routes.ANIMAL_CONFIRMATION) {
                val animal = ultimoRegistro ?: animalSeleccionado
                if (animal == null) {
                    LaunchedEffect(Unit) { navigateMain(Routes.ANIMAL_LIST) }
                    return@composable
                }
                AnimalConfirmationScreen(
                    animal = animal,
                    onViewProfile = { navController.navigate(Routes.ANIMAL_DETAIL) },
                    onRegisterAnother = {
                        animalEnEdicionId = null
                        animalSaveError = null
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
                val animal = animalSeleccionado
                if (animal == null) {
                    LaunchedEffect(Unit) { navigateMain(Routes.ANIMAL_LIST) }
                    return@composable
                }
                AnimalDetailScreen(
                    animal = animal,
                    onBack = { navController.popBackStack() },
                    onEdit = {
                        animalEnEdicionId = animal.id
                        animalSaveError = null
                        navController.navigate(Routes.ANIMAL_FORM)
                    },
                    onDelete = {
                        coroutineScope.launch {
                            runCatching { animalRepository.removeAnimal(animal.id) }
                                .onSuccess {
                                    animalSeleccionado = null
                                    if (ultimoRegistro?.id == animal.id) ultimoRegistro = null
                                    if (!navController.popBackStack(Routes.ANIMAL_LIST, false)) {
                                        navigateMain(Routes.ANIMAL_LIST)
                                    }
                                }
                        }
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

                        coroutineScope.launch {
                            runCatching {
                                animalRepository.registerWeight(
                                    animalId = selectedAnimal.id,
                                    weightPounds = newWeight,
                                    weighingDate = parseDate(form.date) ?: System.currentTimeMillis(),
                                    notes = form.notes.ifBlank { null }
                                )
                            }.onSuccess {
                                weighings = weighings + record
                                if (!navController.popBackStack(Routes.WEIGHINGS, false)) {
                                    navigateMain(Routes.WEIGHINGS)
                                }
                            }
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
                FinanceListScreen(
                    movements = financialMovements,
                    onMenuClick = openDrawer,
                    onCreateMovement = { navController.navigate(Routes.FINANCE_FORM) },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.FINANCE_FORM) {
                FinancialMovementFormScreen(
                    onBack = { navController.popBackStack() },
                    onSubmit = { form ->
                        financialMovements = financialMovements + FinancialMovementUiModel(
                            id = UUID.randomUUID().toString(),
                            type = form.type,
                            category = form.category,
                            amount = form.amount.toDouble(),
                            date = form.date,
                            notes = form.notes
                        )
                        if (!navController.popBackStack(Routes.FINANCE, false)) {
                            navigateMain(Routes.FINANCE)
                        }
                    }
                )
            }

            composable(Routes.EMPLOYEES) {
                EmployeeListScreen(
                    employees = employees,
                    onMenuClick = openDrawer,
                    onCreateEmployee = { navController.navigate(Routes.EMPLOYEE_FORM) },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.EMPLOYEE_FORM) {
                EmployeeFormScreen(
                    sectorOptions = parcels.map { it.name },
                    onBack = { navController.popBackStack() },
                    onSubmit = { form ->
                        employees = employees + EmployeeUiModel(
                            id = UUID.randomUUID().toString(),
                            fullName = form.fullName,
                            role = form.role,
                            hireDate = form.hireDate,
                            salary = form.salary.toDouble(),
                            paymentFrequency = form.paymentFrequency,
                            assignedSector = form.assignedSector,
                            phone = form.phone,
                            active = form.active
                        )
                        if (!navController.popBackStack(Routes.EMPLOYEES, false)) {
                            navigateMain(Routes.EMPLOYEES)
                        }
                    }
                )
            }

            composable(Routes.REPORTS) {
                ReportsCenterScreen(
                    data = ReportDashboardData(
                        animalCount = animales.size,
                        lotCount = lots.size,
                        parcelCount = parcels.size,
                        weighingCount = weighings.size,
                        milkLiters = milkProductionRecords.sumOf { it.liters },
                        employeeCount = employees.count { it.active },
                        financialBalance = financialMovements.sumOf { movement ->
                            if (movement.type == "INGRESO") movement.amount else -movement.amount
                        }
                    ),
                    allowedReportIds = allowedReportIds,
                    onMenuClick = openDrawer,
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.USERS) {
                UserManagementScreen(
                    users = users,
                    onMenuClick = openDrawer,
                    onCreateUser = {
                        userSaveError = null
                        navController.navigate(Routes.USER_FORM)
                    },
                    onViewRolePermissions = {
                        navController.navigate(Routes.ROLE_PERMISSIONS)
                    },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.USER_FORM) {
                UserFormScreen(
                    existingUsernames = users.map { it.username }.toSet(),
                    isSaving = userIsSaving,
                    saveError = userSaveError,
                    onBack = {
                        userSaveError = null
                        navController.popBackStack()
                    },
                    onSubmit = { form ->
                        userIsSaving = true
                        userSaveError = null
                        coroutineScope.launch {
                            runCatching {
                                userRepository.createUser(
                                    fullName = form.fullName,
                                    username = form.username,
                                    password = form.temporaryPassword,
                                    roleName = form.roleName,
                                    active = form.active
                                )
                            }.onSuccess {
                                userIsSaving = false
                                navController.popBackStack()
                            }.onFailure { error ->
                                userIsSaving = false
                                userSaveError = userCreationError(error)
                            }
                        }
                    }
                )
            }

            composable(Routes.ROLE_PERMISSIONS) {
                RolePermissionsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
}

/** Traduce los permisos RBAC del rol a los destinos que pueden abrirse. */
private fun allowedRoutesForRole(roleName: String?): Set<String> {
    val permissions = roleName?.let(::permissionsForRole).orEmpty()
    return buildSet {
        if ("dashboard" in permissions) add(Routes.DASHBOARD)
        if ("animals" in permissions) add(Routes.ANIMAL_LIST)
        if ("weighings" in permissions) {
            add(Routes.WEIGHINGS)
            add(Routes.MILK_PRODUCTION)
        }
        if ("lots" in permissions) {
            add(Routes.LOTS)
            add(Routes.PARCELS)
        }
        if ("finance" in permissions) add(Routes.FINANCE)
        if ("employees" in permissions) add(Routes.EMPLOYEES)
        if ("reports" in permissions) add(Routes.REPORTS)
        if ("users" in permissions) add(Routes.USERS)
    }
}

/** Evita que un rol vea dentro de Reportes información que no le corresponde. */
private fun allowedReportsForRole(roleName: String?): Set<String> = when (roleName) {
    "Administrador General" -> setOf(
        "financial", "production", "staff", "animals", "weighings", "lots", "parcels"
    )
    "Administrador de Campo" -> setOf(
        "production", "animals", "weighings", "lots", "parcels"
    )
    "Técnico Veterinario" -> setOf("animals")
    "Auxiliar Contable" -> setOf("financial", "staff")
    else -> emptySet()
}

/** Convierte errores técnicos de Room o validación en mensajes comprensibles. */
private fun userCreationError(error: Throwable): String = when {
    error.message.orEmpty().contains("UNIQUE", ignoreCase = true) ->
        "Ese nombre de usuario ya está registrado."
    error is IllegalArgumentException -> error.message ?: "Revise los datos del usuario."
    else -> "No fue posible guardar la cuenta. Inténtelo nuevamente."
}

/**
 * Genera un código consecutivo para que el usuario no tenga que escribirlo.
 * También considera animales inactivos para no reutilizar un código persistido.
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

/** Convierte el formulario visual a la entidad persistente utilizada por Room. */
private fun AnimalFormData.toEntity(id: String) = AnimalEntity(
    id = id,
    codigoIdentificacion = codigoIdentificacion.trim(),
    nombre = nombre.trim().ifBlank { null },
    sexo = sexo,
    raza = raza.trim().ifBlank { null },
    fechaNacimiento = parseDate(fechaNacimiento),
    fechaIngreso = parseDate(fechaIngreso)
        ?: parseDate(fechaNacimiento)
        ?: System.currentTimeMillis(),
    categoria = categoria,
    tipoOrigen = tipoOrigen,
    procedencia = procedencia.trim().ifBlank { null },
    estadoSalud = estadoSalud,
    estado = "ACTIVO",
    observaciones = observaciones.trim().ifBlank { null }
)

/** Convierte el registro combinado de Room al modelo que ya consumen las vistas. */
private fun AnimalStoredRecord.toListItem() = AnimalListItem(
    id = animal.id,
    codigoIdentificacion = animal.codigoIdentificacion,
    nombre = animal.nombre,
    categoria = animal.categoria,
    estado = animal.estadoSalud,
    ultimoPesoLibras = lastWeightPounds,
    raza = animal.raza.orEmpty(),
    sexo = animal.sexo,
    tipoOrigen = animal.tipoOrigen,
    fechaNacimiento = formatDate(animal.fechaNacimiento),
    fechaIngreso = formatDate(animal.fechaIngreso),
    procedencia = animal.procedencia.orEmpty(),
    observaciones = animal.observaciones.orEmpty()
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

private fun parseDate(value: String): Long? = runCatching {
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        isLenient = false
    }.parse(value)?.time
}.getOrNull()

private fun formatDate(value: Long?): String = value?.let {
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
}.orEmpty()

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
        selectedAnimalIds = emptyList()
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
        selectedAnimalIds = emptyList()
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

// Movimientos ficticios para revisar el panel financiero antes de conectar Room.
private val initialFinancialMovements = listOf(
    FinancialMovementUiModel(
        id = "finance-demo-1",
        type = "INGRESO",
        category = "Venta de leche",
        amount = 4250.0,
        date = "28/08/2026",
        notes = "Registro de prueba"
    ),
    FinancialMovementUiModel(
        id = "finance-demo-2",
        type = "EGRESO",
        category = "Alimentación",
        amount = 1850.0,
        date = "29/08/2026",
        notes = "Compra de concentrado"
    )
)

// Personal ficticio para validar visualmente el listado de HU-06.
private val initialEmployees = listOf(
    EmployeeUiModel(
        id = "employee-demo-1",
        fullName = "Ricardo Hernández",
        role = "Vaquero",
        hireDate = "10/01/2025",
        salary = 3200.0,
        paymentFrequency = "Mensual",
        assignedSector = "Potrero Norte",
        phone = "5555 0101",
        active = true
    ),
    EmployeeUiModel(
        id = "employee-demo-2",
        fullName = "María García",
        role = "Veterinario",
        hireDate = "15/03/2025",
        salary = 4500.0,
        paymentFrequency = "Mensual",
        assignedSector = "General",
        phone = "5555 0202",
        active = true
    )
)
