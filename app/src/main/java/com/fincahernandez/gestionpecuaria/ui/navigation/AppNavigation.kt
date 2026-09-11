package com.fincahernandez.gestionpecuaria.ui.navigation

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fincahernandez.gestionpecuaria.ui.components.AppDrawerContent
import com.fincahernandez.gestionpecuaria.ui.components.LocalAllowedMainRoutes
import com.fincahernandez.gestionpecuaria.ui.components.LocalLogoutAction
import com.fincahernandez.gestionpecuaria.data.local.entity.AnimalEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.PesajeEntity
import com.fincahernandez.gestionpecuaria.data.repository.AnimalStoredRecord
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticatedUser
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticationResult
import com.fincahernandez.gestionpecuaria.data.repository.LotDraft
import com.fincahernandez.gestionpecuaria.data.repository.ParcelDraft
import com.fincahernandez.gestionpecuaria.data.repository.SanitaryStoredRecord
import com.fincahernandez.gestionpecuaria.data.security.SessionManager
import com.fincahernandez.gestionpecuaria.data.security.serializePermissions
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
import com.fincahernandez.gestionpecuaria.ui.screens.dashboard.DashboardRecentItem
import com.fincahernandez.gestionpecuaria.ui.screens.dashboard.DashboardUiData
import com.fincahernandez.gestionpecuaria.ui.screens.employees.EmployeeFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.employees.EmployeeListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.employees.EmployeeUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.finance.FinanceListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.finance.FinancialMovementFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.finance.FinancialMovementUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.health.SanitaryAnimalOption
import com.fincahernandez.gestionpecuaria.ui.screens.health.SanitaryControlScreen
import com.fincahernandez.gestionpecuaria.ui.screens.health.SanitaryLotOption
import com.fincahernandez.gestionpecuaria.ui.screens.health.SanitaryRecordFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.health.SanitaryRecordUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotDetailScreen
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotAnimalOption
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotAnimalSelectionScreen
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotFormData
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.milk.MilkProductionFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.milk.MilkProductionListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.milk.MilkProductionUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.parcels.ParcelDetailScreen
import com.fincahernandez.gestionpecuaria.ui.screens.parcels.ParcelFormData
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
import com.fincahernandez.gestionpecuaria.ui.viewmodel.AnimalViewModel
import com.fincahernandez.gestionpecuaria.ui.viewmodel.BulkDataImportViewModel
import com.fincahernandez.gestionpecuaria.ui.viewmodel.LotViewModel
import com.fincahernandez.gestionpecuaria.ui.viewmodel.ParcelViewModel
import com.fincahernandez.gestionpecuaria.ui.viewmodel.SanitaryViewModel
import com.fincahernandez.gestionpecuaria.ui.viewmodel.UserViewModel
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
 * Animales, pesajes, lotes, sanidad y usuarios se obtienen de Room. Los demás
 * módulos conservan estado temporal hasta que sus historias se implementen.
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
    val animalViewModel: AnimalViewModel = viewModel()
    val bulkDataImportViewModel: BulkDataImportViewModel = viewModel()
    val lotViewModel: LotViewModel = viewModel()
    val parcelViewModel: ParcelViewModel = viewModel()
    val sanitaryViewModel: SanitaryViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val sessionManager = remember(context) { SessionManager(context) }
    val storedAnimals by animalViewModel.animals.collectAsStateWithLifecycle()
    val storedWeighings by animalViewModel.weighings.collectAsStateWithLifecycle()
    val storedLots by lotViewModel.lots.collectAsStateWithLifecycle()
    val storedParcels by parcelViewModel.parcels.collectAsStateWithLifecycle()
    val storedUsers by userViewModel.users.collectAsStateWithLifecycle()
    val storedSanitaryRecords by sanitaryViewModel.records.collectAsStateWithLifecycle()

    // Estas conversiones solo se repiten cuando Room emite datos nuevos. Cambiar de
    // pantalla ya no reconstruye todas las listas de la aplicación innecesariamente.
    val users = remember(storedUsers) {
        storedUsers.map { user ->
            UserUiModel(
                id = user.id,
                fullName = user.fullName,
                username = user.username,
                roleName = user.roleName,
                active = user.active,
                permissionCount = user.permissionIds.size,
                hasCustomPermissions = user.hasCustomPermissions
            )
        }
    }
    val animalItemCollections = remember(storedAnimals) {
        buildAnimalItemCollections(storedAnimals)
    }
    val allAnimalItems = animalItemCollections.all
    val animales = animalItemCollections.active
    val animalItemsById = remember(allAnimalItems) { allAnimalItems.associateBy { it.id } }
    val previousWeightByRecordId = remember(storedWeighings) {
        buildPreviousWeightByRecordId(storedWeighings)
    }
    val weighings = remember(storedWeighings, animalItemsById) {
        storedWeighings.map { weighing ->
            val animal = animalItemsById[weighing.animalId]
            WeighingUiModel(
                id = weighing.id,
                animalId = weighing.animalId,
                animalLabel = animal?.let {
                    buildString {
                        append(it.codigoIdentificacion)
                        it.nombre?.takeIf(String::isNotBlank)?.let { name -> append(" • $name") }
                    }
                } ?: "Animal no disponible",
                weightLibras = weighing.pesoLibras,
                previousWeightLibras = previousWeightByRecordId[weighing.id],
                date = formatDate(weighing.fechaPesaje),
                notes = weighing.observaciones.orEmpty()
            )
        }
    }
    val lots = remember(storedLots) {
        storedLots.map { record ->
            val lot = record.lot
            LotUiModel(
                id = lot.id,
                code = lot.codigo,
                name = lot.nombre,
                type = lot.tipo,
                status = lot.estado,
                parcelName = lot.parcelaNombre.orEmpty(),
                initialAverageWeight = record.averageWeightPounds,
                targetWeight = lot.pesoObjetivoLibras,
                estimatedExitDate = formatDate(lot.fechaSalidaEstimada),
                selectedAnimalIds = if (lot.estado == "ACTIVO") {
                    record.activeAnimalIds
                } else {
                    record.historicalAnimalIds
                }
            )
        }
    }
    val activeLotByAnimalId = remember(storedLots) {
        buildMap {
            storedLots.filter { it.lot.estado == "ACTIVO" }.forEach { record ->
                record.activeAnimalIds.forEach { animalId -> put(animalId, record.lot.id) }
            }
        }
    }
    val parcels = remember(storedParcels) {
        storedParcels.map { record ->
            val parcel = record.parcel
            ParcelUiModel(
                id = parcel.id,
                code = parcel.codigo,
                name = parcel.nombre,
                areaHectares = parcel.areaHectareas,
                pastureType = parcel.tipoPastura,
                status = if (record.currentLotName == null) parcel.estado else "OCUPADA",
                capacity = parcel.capacidadAnimales,
                currentLot = record.currentLotName.orEmpty()
            )
        }
    }
    var animalSeleccionado by remember { mutableStateOf<AnimalListItem?>(null) }
    var ultimoRegistro by remember { mutableStateOf<AnimalListItem?>(null) }
    var animalEnEdicionId by remember { mutableStateOf<String?>(null) }
    var animalSaveError by remember { mutableStateOf<String?>(null) }
    var animalIsSaving by remember { mutableStateOf(false) }
    var selectedLotId by rememberSaveable { mutableStateOf("") }
    var lotEditingId by rememberSaveable { mutableStateOf("") }
    var lotDraftAnimalIds by remember { mutableStateOf(emptyList<String>()) }
    var lotIsSaving by remember { mutableStateOf(false) }
    var lotSaveError by remember { mutableStateOf<String?>(null) }
    var selectedParcelId by rememberSaveable { mutableStateOf("") }
    var parcelEditingId by rememberSaveable { mutableStateOf("") }
    var parcelIsSaving by remember { mutableStateOf(false) }
    var parcelSaveError by remember { mutableStateOf<String?>(null) }
    var milkProductionRecords by remember { mutableStateOf(initialMilkProductionRecords) }
    var financialMovements by remember { mutableStateOf(initialFinancialMovements) }
    var employees by remember { mutableStateOf(initialEmployees) }
    var sanitaryIsSaving by remember { mutableStateOf(false) }
    var sanitarySaveError by remember { mutableStateOf<String?>(null) }
    var sanitaryInitialAnimalId by remember { mutableStateOf("") }
    // Estos valores simples sobreviven a una recreación de la actividad, por ejemplo
    // cuando Android cambia la configuración de pantalla de la tableta.
    var currentUserId by rememberSaveable { mutableStateOf("") }
    var currentUserFullName by rememberSaveable { mutableStateOf("") }
    var currentUsername by rememberSaveable { mutableStateOf("") }
    var currentUserRole by rememberSaveable { mutableStateOf("") }
    var currentUserPermissions by rememberSaveable { mutableStateOf("") }
    var loginIsLoading by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var setupIsSaving by remember { mutableStateOf(false) }
    var setupError by remember { mutableStateOf<String?>(null) }
    var userIsSaving by remember { mutableStateOf(false) }
    var userSaveError by remember { mutableStateOf<String?>(null) }

    val selectedLot = remember(lots, selectedLotId) {
        lots.firstOrNull { it.id == selectedLotId }
    }
    val selectedParcel = remember(parcels, selectedParcelId) {
        parcels.firstOrNull { it.id == selectedParcelId }
    }

    val currentPermissionIds = remember(currentUserPermissions) {
        currentUserPermissions
            .split(',')
            .filterTo(linkedSetOf()) { permission -> permission.isNotBlank() }
    }
    val currentUser = remember(
        currentUserId,
        currentUserFullName,
        currentUsername,
        currentUserRole,
        currentPermissionIds
    ) {
        currentUserId.takeIf { it.isNotBlank() }?.let {
            AuthenticatedUser(
                id = it,
                fullName = currentUserFullName,
                username = currentUsername,
                roleName = currentUserRole,
                permissionIds = currentPermissionIds
            )
        }
    }
    val updateCurrentUser: (AuthenticatedUser?) -> Unit = { user ->
        if (user == null) {
            currentUserId = ""
            currentUserFullName = ""
            currentUsername = ""
            currentUserRole = ""
            currentUserPermissions = ""
        } else {
            currentUserFullName = user.fullName
            currentUsername = user.username
            currentUserRole = user.roleName
            currentUserPermissions = serializePermissions(user.permissionIds)
            currentUserId = user.id
        }
    }

    val allowedMainRoutes = remember(currentPermissionIds) {
        allowedRoutesForPermissions(currentPermissionIds)
    }
    val allowedReportIds = remember(currentPermissionIds) {
        allowedReportsForPermissions(currentPermissionIds)
    }
    val sanitaryLots = remember(lots) {
        lots.filter { it.status == "ACTIVO" }.map { lot ->
            SanitaryLotOption(
                id = lot.id,
                label = "${lot.code} · ${lot.name}",
                animalIds = lot.selectedAnimalIds.toSet()
            )
        }
    }
    val sanitaryAnimals = remember(animales) {
        animales.map { animal ->
            SanitaryAnimalOption(
                id = animal.id,
                label = buildString {
                    append(animal.codigoIdentificacion)
                    animal.nombre?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                },
                sex = animal.sexo
            )
        }
    }
    val lotsById = remember(lots) { lots.associateBy { it.id } }
    val sanitaryRecords = remember(storedSanitaryRecords, animalItemsById, lotsById) {
        storedSanitaryRecords.map { record ->
            val animal = animalItemsById[record.animalId]
            val lot = lotsById[record.referenceLotId]
            SanitaryRecordUiModel(
                id = record.id,
                animalId = record.animalId,
                animalLabel = animal?.let {
                    buildString {
                        append(it.codigoIdentificacion)
                        it.nombre?.takeIf(String::isNotBlank)?.let { name -> append(" · $name") }
                    }
                } ?: "Animal no disponible",
                referenceLotId = record.referenceLotId,
                referenceLotLabel = lot?.let { "${it.code} · ${it.name}" },
                eventType = record.eventType,
                eventDate = record.eventDate,
                diagnosis = record.diagnosis,
                medication = record.medication,
                dose = record.dose,
                healthStatus = record.healthStatus,
                nextControlDate = record.nextControlDate,
                responsible = record.responsible,
                notes = record.notes
            )
        }
    }

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
        Routes.SANITARY_FORM -> Routes.SANITARY
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

    /** Abre directamente los formularios ofrecidos por el dashboard. */
    val navigateQuickAction: (String) -> Unit = { destination ->
        val requiredRoute = when (destination) {
            Routes.ANIMAL_FORM -> Routes.ANIMAL_LIST
            Routes.WEIGHING_FORM -> Routes.WEIGHINGS
            Routes.MILK_PRODUCTION_FORM -> Routes.MILK_PRODUCTION
            Routes.FINANCE_FORM -> Routes.FINANCE
            else -> null
        }
        if (requiredRoute != null && requiredRoute in allowedMainRoutes) {
            if (destination == Routes.ANIMAL_FORM) {
                animalEnEdicionId = null
                animalSaveError = null
            }
            navController.navigate(destination) { launchSingleTop = true }
        }
    }

    /** Abre el menú lateral desde las pantallas principales. */
    val openDrawer: () -> Unit = {
        coroutineScope.launch { drawerState.open() }
    }

    /** Cierra la sesión actual y evita que las pantallas protegidas queden en el historial. */
    val logout: () -> Unit = {
        sessionManager.clear()
        updateCurrentUser(null)
        coroutineScope.launch { drawerState.close() }
        navController.navigate(Routes.LOGIN) {
            popUpTo(Routes.DASHBOARD) { inclusive = true }
            launchSingleTop = true
        }
    }

    CompositionLocalProvider(
        LocalAllowedMainRoutes provides allowedMainRoutes,
        LocalLogoutAction provides logout
    ) {
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
                        if (route == Routes.SANITARY) sanitaryInitialAnimalId = ""
                        navigateMain(route)
                    },
                    onLogout = logout
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
                    val hasUsers = userViewModel.hasUsers()
                    val rememberedUser = sessionManager.rememberedUserId()
                        ?.let { userViewModel.activeUserById(it) }
                    if (rememberedUser == null) sessionManager.clear()
                    updateCurrentUser(rememberedUser)

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
                                userViewModel.createUser(
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
                                    userViewModel.resetLocalAccess()
                                    sessionManager.clear()
                                }.onSuccess {
                                    loginIsLoading = false
                                    updateCurrentUser(null)
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
                            when (val result = userViewModel.authenticate(username, password)) {
                                is AuthenticationResult.Success -> {
                                    loginIsLoading = false
                                    updateCurrentUser(result.user)
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
                val latestMilk = milkProductionRecords.lastOrNull()
                val dashboardRecentItems = buildList {
                    allAnimalItems.lastOrNull()?.let { animal ->
                        add(
                            DashboardRecentItem(
                                title = "Animal ${animal.codigoIdentificacion}",
                                detail = animal.nombre?.let { "Registrado como $it" }
                                    ?: "Registro de animal actualizado",
                                route = Routes.ANIMAL_LIST
                            )
                        )
                    }
                    weighings.lastOrNull()?.let { weighing ->
                        add(
                            DashboardRecentItem(
                                title = "Pesaje de ${weighing.animalLabel}",
                                detail = "${weighing.weightLibras} lb · ${weighing.date}",
                                route = Routes.WEIGHINGS
                            )
                        )
                    }
                    latestMilk?.let { milk ->
                        add(
                            DashboardRecentItem(
                                title = "Producción de leche",
                                detail = "${milk.liters} L · ${milk.date}",
                                route = Routes.MILK_PRODUCTION
                            )
                        )
                    }
                    financialMovements.lastOrNull()?.let { movement ->
                        add(
                            DashboardRecentItem(
                                title = movement.category,
                                detail = "${movement.type} · Q ${movement.amount} · ${movement.date}",
                                route = Routes.FINANCE
                            )
                        )
                    }
                }
                DashboardScreen(
                    data = DashboardUiData(
                        activeAnimals = animales.size,
                        totalAnimals = allAnimalItems.size,
                        activeLots = lots.count { it.status == "ACTIVO" },
                        totalLots = lots.size,
                        availableParcels = parcels.count { it.status == "DISPONIBLE" },
                        totalParcels = parcels.size,
                        weighingRecords = weighings.size,
                        averageWeightPounds = weighings.map { it.weightLibras }
                            .average()
                            .takeUnless { it.isNaN() },
                        latestMilkLiters = latestMilk?.liters,
                        latestMilkDate = latestMilk?.date.orEmpty(),
                        income = financialMovements
                            .filter { it.type == "INGRESO" }
                            .sumOf { it.amount },
                        expenses = financialMovements
                            .filter { it.type == "EGRESO" }
                            .sumOf { it.amount },
                        activeEmployees = employees.count { it.active },
                        totalEmployees = employees.size,
                        activeUsers = users.count { it.active },
                        totalUsers = users.size,
                        recentItems = dashboardRecentItems
                    ),
                    userName = currentUser?.fullName.orEmpty(),
                    roleName = currentUser?.roleName.orEmpty(),
                    onMenuClick = openDrawer,
                    onNavigate = navigateMain,
                    onQuickAction = navigateQuickAction
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
                                animalViewModel.saveAnimal(
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
                            runCatching { animalViewModel.removeAnimal(animal.id) }
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
                    sanitaryEventCount = sanitaryRecords.count { it.animalId == animal.id },
                    canManageHealth = "health" in currentUser?.permissionIds.orEmpty(),
                    onOpenSanitaryControl = {
                        sanitaryInitialAnimalId = animal.id
                        navigateMain(Routes.SANITARY)
                    },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.LOTS) {
                LotListScreen(
                    lots = lots,
                    onMenuClick = openDrawer,
                    onCreateLot = {
                        // Cada lote nuevo comienza sin datos de una edición anterior.
                        lotEditingId = ""
                        lotDraftAnimalIds = emptyList()
                        lotSaveError = null
                        navController.navigate(Routes.LOT_FORM)
                    },
                    onLotClick = { lotId ->
                        lots.firstOrNull { it.id == lotId }?.let {
                            selectedLotId = lotId
                            lotSaveError = null
                            navController.navigate(Routes.LOT_DETAIL)
                        }
                    },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.LOT_FORM) {
                val editingLot = lots.firstOrNull { it.id == lotEditingId }
                val availableAnimals = animales.filter { animal ->
                    val assignedLotId = activeLotByAnimalId[animal.id]
                    assignedLotId == null || assignedLotId == lotEditingId
                }
                LotFormScreen(
                    parcelNames = (
                        parcels.filter { parcel ->
                            parcel.status == "DISPONIBLE" || parcel.currentLot == editingLot?.name
                        }.map { it.name } + editingLot?.parcelName.orEmpty()
                    )
                        .filter(String::isNotBlank)
                        .distinct(),
                    animalOptions = availableAnimals.map { animal ->
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
                        coroutineScope.launch {
                            lotIsSaving = true
                            lotSaveError = null
                            runCatching {
                                if (lotEditingId.isBlank()) {
                                    lotViewModel.createLot(form.toLotDraft())
                                } else {
                                    lotViewModel.updateLot(lotEditingId, form.toLotDraft())
                                    lotEditingId
                                }
                            }.onSuccess { savedLotId ->
                                selectedLotId = savedLotId
                                lotEditingId = ""
                                lotDraftAnimalIds = emptyList()
                                navController.navigate(Routes.LOT_DETAIL) {
                                    popUpTo(Routes.LOT_FORM) { inclusive = true }
                                }
                            }.onFailure { error ->
                                lotSaveError = error.message ?: "No se pudo guardar el lote."
                            }
                            lotIsSaving = false
                        }
                    },
                    initialData = editingLot?.toFormData(),
                    isSaving = lotIsSaving,
                    saveError = lotSaveError
                )
            }

            composable(Routes.LOT_ANIMAL_SELECTION) {
                LotAnimalSelectionScreen(
                    animals = animales.filter { animal ->
                        val assignedLotId = activeLotByAnimalId[animal.id]
                        assignedLotId == null || assignedLotId == lotEditingId
                    }.map { animal ->
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
                val lot = selectedLot
                if (lot == null) {
                    if (selectedLotId.isBlank()) {
                        LaunchedEffect(Unit) { navigateMain(Routes.LOTS) }
                    }
                    return@composable
                }
                LotDetailScreen(
                    lot = lot,
                    selectedAnimalLabels = allAnimalItems
                        .filter { it.id in lot.selectedAnimalIds }
                        .map { it.nombre ?: it.codigoIdentificacion },
                    onBack = { navController.popBackStack() },
                    onEdit = {
                        lotEditingId = lot.id
                        lotDraftAnimalIds = lot.selectedAnimalIds
                        lotSaveError = null
                        navController.navigate(Routes.LOT_FORM)
                    },
                    onDeactivate = {
                        coroutineScope.launch {
                            lotIsSaving = true
                            lotSaveError = null
                            runCatching { lotViewModel.deactivateLot(lot.id) }
                                .onFailure { error ->
                                    lotSaveError = error.message ?: "No se pudo cerrar el lote."
                                }
                            lotIsSaving = false
                        }
                    },
                    onRegisterWeight = { navigateMain(Routes.WEIGHINGS) },
                    onNavigateMain = navigateMain,
                    isSaving = lotIsSaving,
                    saveError = lotSaveError
                )
            }

            composable(Routes.PARCELS) {
                ParcelListScreen(
                    parcels = parcels,
                    onMenuClick = openDrawer,
                    onCreateParcel = {
                        parcelEditingId = ""
                        parcelSaveError = null
                        navController.navigate(Routes.PARCEL_FORM)
                    },
                    onParcelClick = { parcelId ->
                        parcels.firstOrNull { it.id == parcelId }?.let {
                            selectedParcelId = parcelId
                            parcelSaveError = null
                            navController.navigate(Routes.PARCEL_DETAIL)
                        }
                    },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.PARCEL_FORM) {
                val editingParcel = parcels.firstOrNull { it.id == parcelEditingId }
                ParcelFormScreen(
                    onBack = { navController.popBackStack() },
                    onSubmit = { form ->
                        coroutineScope.launch {
                            parcelIsSaving = true
                            parcelSaveError = null
                            runCatching {
                                if (parcelEditingId.isBlank()) {
                                    parcelViewModel.createParcel(form.toParcelDraft())
                                } else {
                                    parcelViewModel.updateParcel(parcelEditingId, form.toParcelDraft())
                                    parcelEditingId
                                }
                            }.onSuccess { savedParcelId ->
                                selectedParcelId = savedParcelId
                                parcelEditingId = ""
                                navController.navigate(Routes.PARCEL_DETAIL) {
                                    popUpTo(Routes.PARCEL_FORM) { inclusive = true }
                                }
                            }.onFailure { error ->
                                parcelSaveError = error.message ?: "No se pudo guardar la parcela."
                            }
                            parcelIsSaving = false
                        }
                    },
                    initialData = editingParcel?.toFormData(),
                    isSaving = parcelIsSaving,
                    saveError = parcelSaveError
                )
            }

            composable(Routes.PARCEL_DETAIL) {
                val parcel = selectedParcel
                if (parcel == null) {
                    if (selectedParcelId.isBlank()) {
                        LaunchedEffect(Unit) { navigateMain(Routes.PARCELS) }
                    }
                    return@composable
                }
                ParcelDetailScreen(
                    parcel = parcel,
                    onBack = { navController.popBackStack() },
                    onEdit = {
                        parcelEditingId = parcel.id
                        parcelSaveError = null
                        navController.navigate(Routes.PARCEL_FORM)
                    },
                    onSetResting = { resting ->
                        coroutineScope.launch {
                            parcelIsSaving = true
                            parcelSaveError = null
                            runCatching { parcelViewModel.setResting(parcel.id, resting) }
                                .onFailure { error ->
                                    parcelSaveError = error.message
                                        ?: "No se pudo cambiar el estado de la parcela."
                                }
                            parcelIsSaving = false
                        }
                    },
                    onDeactivate = {
                        coroutineScope.launch {
                            parcelIsSaving = true
                            parcelSaveError = null
                            runCatching { parcelViewModel.deactivateParcel(parcel.id) }
                                .onFailure { error ->
                                    parcelSaveError = error.message
                                        ?: "No se pudo desactivar la parcela."
                                }
                            parcelIsSaving = false
                        }
                    },
                    onNavigateMain = navigateMain,
                    isSaving = parcelIsSaving,
                    saveError = parcelSaveError
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

                        coroutineScope.launch {
                            runCatching {
                                animalViewModel.registerWeight(
                                    animalId = selectedAnimal.id,
                                    weightPounds = newWeight,
                                    weighingDate = parseDate(form.date) ?: System.currentTimeMillis(),
                                    notes = form.notes.ifBlank { null }
                                )
                            }.onSuccess {
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

            composable(Routes.SANITARY) {
                SanitaryControlScreen(
                    records = sanitaryRecords,
                    lots = sanitaryLots,
                    animals = sanitaryAnimals,
                    initialAnimalId = sanitaryInitialAnimalId,
                    onMenuClick = openDrawer,
                    onCreateRecord = {
                        sanitarySaveError = null
                        navController.navigate(Routes.SANITARY_FORM)
                    },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.SANITARY_FORM) {
                SanitaryRecordFormScreen(
                    lots = sanitaryLots,
                    animals = sanitaryAnimals,
                    initialAnimalId = sanitaryInitialAnimalId,
                    isSaving = sanitaryIsSaving,
                    saveError = sanitarySaveError,
                    onBack = {
                        sanitarySaveError = null
                        navController.popBackStack()
                    },
                    onSubmit = { form ->
                        sanitaryIsSaving = true
                        sanitarySaveError = null
                        coroutineScope.launch {
                            runCatching {
                                sanitaryViewModel.register(
                                    SanitaryStoredRecord(
                                        id = UUID.randomUUID().toString(),
                                        animalId = form.animalId,
                                        referenceLotId = form.referenceLotId,
                                        eventType = form.eventType,
                                        eventDate = checkNotNull(parseDate(form.eventDate)),
                                        diagnosis = form.diagnosis,
                                        medication = form.medication.ifBlank { null },
                                        dose = form.dose.ifBlank { null },
                                        healthStatus = form.healthStatus,
                                        nextControlDate = parseDate(form.nextControlDate),
                                        responsible = form.responsible.ifBlank { null },
                                        notes = form.notes.ifBlank { null }
                                    )
                                )
                            }.onSuccess {
                                sanitaryIsSaving = false
                                sanitaryInitialAnimalId = ""
                                if (!navController.popBackStack(Routes.SANITARY, false)) {
                                    navigateMain(Routes.SANITARY)
                                }
                            }.onFailure {
                                sanitaryIsSaving = false
                                sanitarySaveError =
                                    "No fue posible guardar el evento sanitario. Inténtelo nuevamente."
                            }
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
                    sectorOptions = parcels.filter { it.status != "INACTIVA" }.map { it.name },
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
                    isGeneralAdministrator = currentUser?.roleName == "Administrador General",
                    onBulkExport = { uri, modules ->
                        check(currentUser?.roleName == "Administrador General") {
                            "Solo el Administrador General puede exportar datos operativos."
                        }
                        bulkDataImportViewModel.export(uri, modules)
                    },
                    onInspectBulkImport = { uri, modules, mode ->
                        check(currentUser?.roleName == "Administrador General") {
                            "Solo el Administrador General puede importar datos."
                        }
                        bulkDataImportViewModel.inspect(uri, modules, mode)
                    },
                    onBulkImport = { uri, modules, mode ->
                        check(currentUser?.roleName == "Administrador General") {
                            "Solo el Administrador General puede importar datos."
                        }
                        bulkDataImportViewModel.import(uri, modules, mode)
                    },
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
                                userViewModel.createUser(
                                    fullName = form.fullName,
                                    username = form.username,
                                    password = form.temporaryPassword,
                                    roleName = form.roleName,
                                    active = form.active,
                                    permissionIds = form.permissionIds
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

/**
 * Relaciona cada pesaje con la medición cronológicamente anterior del mismo animal.
 * Agrupar primero evita comparar accidentalmente animales diferentes.
 */
internal fun buildPreviousWeightByRecordId(
    weighings: List<PesajeEntity>
): Map<String, Double?> = buildMap {
    weighings.groupBy { it.animalId }.values.forEach { animalWeighings ->
        var previousWeight: Double? = null
        animalWeighings
            .sortedWith(
                compareBy<PesajeEntity> {
                    it.fechaPesaje
                }.thenBy { it.creadoEn }.thenBy { it.id }
            )
            .forEach { weighing ->
                put(weighing.id, previousWeight)
                previousWeight = weighing.pesoLibras
            }
    }
}

/** Traduce los permisos efectivos del usuario a los destinos que puede abrir. */
private fun allowedRoutesForPermissions(permissions: Set<String>): Set<String> {
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
        if ("health" in permissions) add(Routes.SANITARY)
        if ("finance" in permissions) add(Routes.FINANCE)
        if ("employees" in permissions) add(Routes.EMPLOYEES)
        if ("reports" in permissions) add(Routes.REPORTS)
        if ("users" in permissions) add(Routes.USERS)
    }
}

/**
 * Los reportes también respetan las excepciones de la cuenta. Por ejemplo, añadir
 * Finanzas habilita su reporte aunque la plantilla del rol normalmente no lo incluya.
 */
private fun allowedReportsForPermissions(permissions: Set<String>): Set<String> = buildSet {
    if ("finance" in permissions) add("financial")
    if ("weighings" in permissions) {
        add("production")
        add("weighings")
    }
    if ("employees" in permissions) add("staff")
    if ("animals" in permissions) add("animals")
    if ("lots" in permissions) {
        add("lots")
        add("parcels")
    }
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
    observaciones = observaciones.trim(),
    fotoUri = fotoUri
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
    observaciones = observaciones.trim().ifBlank { null },
    fotoUri = fotoUri.ifBlank { null }
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
    observaciones = animal.observaciones.orEmpty(),
    fotoUri = animal.fotoUri.orEmpty()
)

/** Resultado preparado una sola vez para el inventario completo y el inventario activo. */
internal data class AnimalItemCollections(
    val all: List<AnimalListItem>,
    val active: List<AnimalListItem>
)

/**
 * Separa animales activos antes de convertirlos al modelo visual. En AnimalListItem,
 * `estado` contiene el estado de salud; el estado ACTIVO/INACTIVO vive en la entidad Room.
 */
internal fun buildAnimalItemCollections(
    storedAnimals: List<AnimalStoredRecord>
): AnimalItemCollections {
    val allItems = ArrayList<AnimalListItem>(storedAnimals.size)
    val activeItems = ArrayList<AnimalListItem>(storedAnimals.size)
    storedAnimals.forEach { storedRecord ->
        val item = storedRecord.toListItem()
        allItems += item
        if (storedRecord.animal.estado == "ACTIVO") activeItems += item
    }
    return AnimalItemCollections(all = allItems, active = activeItems)
}

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
    observaciones = observaciones,
    fotoUri = fotoUri
)

/** Convierte los datos visuales del formulario al modelo validado del repositorio. */
private fun LotFormData.toLotDraft() = LotDraft(
    name = name,
    type = type,
    parcelName = parcelName.takeIf(String::isNotBlank),
    targetWeightPounds = targetWeight.replace(',', '.').toDoubleOrNull(),
    estimatedExitDate = parseDate(estimatedExitDate),
    selectedAnimalIds = selectedAnimalIds
)

/** Precarga los datos persistidos cuando se corrige un lote activo. */
private fun LotUiModel.toFormData() = LotFormData(
    name = name,
    type = type,
    parcelName = parcelName,
    initialAverageWeight = initialAverageWeight?.toString().orEmpty(),
    targetWeight = targetWeight?.toString().orEmpty(),
    estimatedExitDate = estimatedExitDate,
    selectedAnimalIds = selectedAnimalIds
)

private fun ParcelFormData.toParcelDraft() = ParcelDraft(
    name = name,
    areaHectares = areaHectares.replace(',', '.').toDoubleOrNull() ?: 0.0,
    pastureType = pastureType,
    capacity = capacity.toIntOrNull()
)

private fun ParcelUiModel.toFormData() = ParcelFormData(
    name = name,
    areaHectares = areaHectares.toString(),
    pastureType = pastureType,
    capacity = capacity?.toString().orEmpty()
)

private fun parseDate(value: String): Long? = runCatching {
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        isLenient = false
    }.parse(value)?.time
}.getOrNull()

private fun formatDate(value: Long?): String = value?.let {
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
}.orEmpty()

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
