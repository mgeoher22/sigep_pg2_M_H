package com.fincahernandez.gestionpecuaria.ui.navigation

import com.fincahernandez.gestionpecuaria.ui.components.label
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
import com.fincahernandez.gestionpecuaria.data.local.entity.LoteAnimalEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.PesajeEntity
import com.fincahernandez.gestionpecuaria.data.geo.calculateParcelBoundaryMetrics
import com.fincahernandez.gestionpecuaria.data.repository.AnimalStoredRecord
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticatedUser
import com.fincahernandez.gestionpecuaria.data.repository.AuthenticationResult
import com.fincahernandez.gestionpecuaria.data.repository.EmployeeDraft
import com.fincahernandez.gestionpecuaria.data.repository.EmployeePaymentDraft
import com.fincahernandez.gestionpecuaria.data.repository.FinancialMovementDraft
import com.fincahernandez.gestionpecuaria.data.repository.LotSaleDraft
import com.fincahernandez.gestionpecuaria.data.repository.LotDraft
import com.fincahernandez.gestionpecuaria.data.repository.MilkProductionDraft
import com.fincahernandez.gestionpecuaria.data.repository.ParcelDraft
import com.fincahernandez.gestionpecuaria.data.repository.SanitaryStoredRecord
import com.fincahernandez.gestionpecuaria.data.repository.SupplyCatalog
import com.fincahernandez.gestionpecuaria.data.repository.SupplyDraft
import com.fincahernandez.gestionpecuaria.data.repository.SupplyAdjustmentDraft
import com.fincahernandez.gestionpecuaria.data.repository.SupplyAssignmentDraft
import com.fincahernandez.gestionpecuaria.data.repository.SupplyEntryDraft
import com.fincahernandez.gestionpecuaria.data.repository.SupplyExitDraft
import com.fincahernandez.gestionpecuaria.data.security.SessionManager
import com.fincahernandez.gestionpecuaria.data.security.canEditExistingRecords
import com.fincahernandez.gestionpecuaria.data.security.canUpdateAnimalFieldStatus
import com.fincahernandez.gestionpecuaria.data.security.canConfirmMilkPayments
import com.fincahernandez.gestionpecuaria.data.security.canImportApplicationData
import com.fincahernandez.gestionpecuaria.data.security.canManageSupplies
import com.fincahernandez.gestionpecuaria.data.security.serializePermissions
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalConfirmationScreen
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalBirthPeriodScreen
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalDetailScreen
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalFormData
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalListItem
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.animals.AnimalMotherOption
import com.fincahernandez.gestionpecuaria.ui.screens.animals.buildAnimalBirthHistory
import com.fincahernandez.gestionpecuaria.ui.screens.auth.LoginScreen
import com.fincahernandez.gestionpecuaria.ui.screens.auth.InitialAdminSetupScreen
import com.fincahernandez.gestionpecuaria.ui.screens.auth.SplashScreen
import com.fincahernandez.gestionpecuaria.ui.screens.dashboard.DashboardScreen
import com.fincahernandez.gestionpecuaria.ui.screens.dashboard.DashboardRecentItem
import com.fincahernandez.gestionpecuaria.ui.screens.dashboard.DashboardUiData
import com.fincahernandez.gestionpecuaria.ui.screens.employees.EmployeeFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.employees.EmployeeDetailScreen
import com.fincahernandez.gestionpecuaria.ui.screens.employees.EmployeeListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.employees.EmployeePaymentFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.employees.EmployeePaymentUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.employees.EmployeePaymentLotOption
import com.fincahernandez.gestionpecuaria.ui.screens.employees.EmployeeUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.finance.FinanceListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.finance.FinancialMovementFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.finance.FinancialMovementUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.finance.FinancialLotOption
import com.fincahernandez.gestionpecuaria.ui.screens.health.SanitaryAnimalOption
import com.fincahernandez.gestionpecuaria.ui.screens.health.SanitaryControlScreen
import com.fincahernandez.gestionpecuaria.ui.screens.health.SanitaryLotOption
import com.fincahernandez.gestionpecuaria.ui.screens.health.SanitaryRecordFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.health.SanitaryRecordUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.health.SanitarySupplyOption
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotDetailScreen
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotAnimalOption
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotAnimalSelectionScreen
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotFormData
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotFinancialSummary
import com.fincahernandez.gestionpecuaria.ui.screens.lots.LotWeightRecord
import com.fincahernandez.gestionpecuaria.ui.screens.milk.MilkProductionFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.milk.MilkConfigurationScreen
import com.fincahernandez.gestionpecuaria.ui.screens.milk.MilkProductionListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.milk.MilkPaymentUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.milk.MilkProductionUiModel
import com.fincahernandez.gestionpecuaria.notification.MilkNotificationPreferences
import com.fincahernandez.gestionpecuaria.notification.MilkPaymentNotificationScheduler
import com.fincahernandez.gestionpecuaria.ui.screens.parcels.ParcelDetailScreen
import com.fincahernandez.gestionpecuaria.ui.screens.parcels.ParcelFormData
import com.fincahernandez.gestionpecuaria.ui.screens.parcels.ParcelFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.parcels.ParcelListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.parcels.ParcelMapScreen
import com.fincahernandez.gestionpecuaria.ui.screens.parcels.ParcelUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.parcels.calculateParcelProductivity
import com.fincahernandez.gestionpecuaria.ui.screens.reports.ReportDashboardData
import com.fincahernandez.gestionpecuaria.ui.screens.reports.ReportRecord
import com.fincahernandez.gestionpecuaria.ui.screens.reports.ReportsCenterScreen
import com.fincahernandez.gestionpecuaria.ui.screens.supplies.SupplyListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.supplies.SupplyAssignmentScreen
import com.fincahernandez.gestionpecuaria.ui.screens.supplies.SupplyAssignmentUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.supplies.SupplyDetailScreen
import com.fincahernandez.gestionpecuaria.ui.screens.supplies.SupplyMovementUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.supplies.SupplyAdjustmentScreen
import com.fincahernandez.gestionpecuaria.ui.screens.supplies.SupplyStockUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.supplies.SupplyFormData
import com.fincahernandez.gestionpecuaria.ui.screens.supplies.SupplyFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.supplies.SupplyEntryScreen
import com.fincahernandez.gestionpecuaria.ui.screens.supplies.SupplyExitScreen
import com.fincahernandez.gestionpecuaria.ui.screens.supplies.SupplyEmployeeOption
import com.fincahernandez.gestionpecuaria.ui.screens.supplies.SupplyLotOption
import com.fincahernandez.gestionpecuaria.ui.screens.users.RolePermissionsScreen
import com.fincahernandez.gestionpecuaria.ui.screens.users.UserFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.users.UserManagementScreen
import com.fincahernandez.gestionpecuaria.ui.screens.users.UserUiModel
import com.fincahernandez.gestionpecuaria.ui.screens.weighings.AnimalWeightOption
import com.fincahernandez.gestionpecuaria.ui.screens.weighings.WeighingFormScreen
import com.fincahernandez.gestionpecuaria.ui.screens.weighings.WeighingAnimalDetailScreen
import com.fincahernandez.gestionpecuaria.ui.screens.weighings.WeighingAnimalSummary
import com.fincahernandez.gestionpecuaria.ui.screens.weighings.WeighingListScreen
import com.fincahernandez.gestionpecuaria.ui.screens.weighings.WeighingUiModel
import com.fincahernandez.gestionpecuaria.ui.viewmodel.AnimalViewModel
import com.fincahernandez.gestionpecuaria.ui.viewmodel.BulkDataImportViewModel
import com.fincahernandez.gestionpecuaria.ui.viewmodel.EmployeeViewModel
import com.fincahernandez.gestionpecuaria.ui.viewmodel.FinanceViewModel
import com.fincahernandez.gestionpecuaria.ui.viewmodel.LotViewModel
import com.fincahernandez.gestionpecuaria.ui.viewmodel.MilkProductionViewModel
import com.fincahernandez.gestionpecuaria.ui.viewmodel.ParcelViewModel
import com.fincahernandez.gestionpecuaria.ui.viewmodel.SanitaryViewModel
import com.fincahernandez.gestionpecuaria.ui.viewmodel.SupplyViewModel
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
 * Los módulos operativos, financieros, personal, sanidad y usuarios se obtienen
 * de Room; la navegación solo transforma esos registros para presentarlos.
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
    val employeeViewModel: EmployeeViewModel = viewModel()
    val financeViewModel: FinanceViewModel = viewModel()
    val lotViewModel: LotViewModel = viewModel()
    val milkProductionViewModel: MilkProductionViewModel = viewModel()
    val parcelViewModel: ParcelViewModel = viewModel()
    val sanitaryViewModel: SanitaryViewModel = viewModel()
    val supplyViewModel: SupplyViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val sessionManager = remember(context) { SessionManager(context) }
    val cloudSessionManager = remember(context) {
        com.fincahernandez.gestionpecuaria.data.remote.CloudSessionManager(
            com.fincahernandez.gestionpecuaria.data.remote.EncryptedCloudSessionStore(context))
    }
    val unifiedAuth = remember(context) { com.fincahernandez.gestionpecuaria.data.remote.createOfflineAuth(context, cloudSessionManager) }
    val milkNotificationPreferences = remember(context) { MilkNotificationPreferences(context) }
    val storedAnimals by animalViewModel.animals.collectAsStateWithLifecycle()
    val storedWeighings by animalViewModel.weighings.collectAsStateWithLifecycle()
    val storedLots by lotViewModel.lots.collectAsStateWithLifecycle()
    val storedMilkProduction by milkProductionViewModel.records.collectAsStateWithLifecycle()
    val storedMilkConfiguration by milkProductionViewModel.configuration.collectAsStateWithLifecycle()
    val storedEmployees by employeeViewModel.employees.collectAsStateWithLifecycle()
    val storedEmployeePayments by employeeViewModel.payments.collectAsStateWithLifecycle()
    val storedFinancialMovements by financeViewModel.movements.collectAsStateWithLifecycle()
    val storedParcels by parcelViewModel.parcels.collectAsStateWithLifecycle()
    val storedUsers by userViewModel.users.collectAsStateWithLifecycle()
    val storedSanitaryRecords by sanitaryViewModel.records.collectAsStateWithLifecycle()
    val storedSupplies by supplyViewModel.supplies.collectAsStateWithLifecycle()
    val storedSupplyAssignments by supplyViewModel.assignments.collectAsStateWithLifecycle()
    val storedSupplyMovements by supplyViewModel.movements.collectAsStateWithLifecycle()
    val storedSupplyStocks by supplyViewModel.stocks.collectAsStateWithLifecycle()

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
                permissionIds = user.permissionIds,
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
                lotId = weighing.loteId,
                weightLibras = weighing.pesoLibras,
                previousWeightLibras = previousWeightByRecordId[weighing.id],
                dateMillis = weighing.fechaPesaje,
                date = formatDate(weighing.fechaPesaje),
                notes = weighing.observaciones.orEmpty()
            )
        }
    }
    val weighingAnimals = remember(animales, weighings) {
        animales.map { animal ->
            val animalRecords = weighings
                .filter { it.animalId == animal.id }
                .sortedByDescending { it.dateMillis }
            val latest = animalRecords.firstOrNull()
            WeighingAnimalSummary(
                animalId = animal.id,
                animalName = animal.nombre?.takeIf(String::isNotBlank) ?: "Animal sin nombre",
                animalCode = animal.codigoIdentificacion,
                currentWeightLibras = latest?.weightLibras,
                lastWeighingDate = latest?.date.orEmpty(),
                recordCount = animalRecords.size
            )
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.animalName })
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
                creationDateMillis = lot.fechaCreacion,
                saleAmount = lot.precioVenta,
                saleDate = formatDate(lot.fechaVenta),
                selectedAnimalIds = if (lot.estado == "ACTIVO") {
                    record.activeAnimalIds
                } else {
                    record.historicalAnimalIds
                }
            )
        }
    }
    val lotLabelsById = remember(lots) {
        lots.associate { it.id to "${it.code} · ${it.name}" }
    }
    val activeLotByAnimalId = remember(storedLots) {
        buildMap {
            storedLots.filter { it.lot.estado == "ACTIVO" }.forEach { record ->
                record.activeAnimalIds.forEach { animalId -> put(animalId, record.lot.id) }
            }
        }
    }
    val parcels = remember(storedParcels, storedLots, storedAnimals) {
        val latestWeightByAnimalId = storedAnimals.associate { record ->
            record.animal.id to record.lastWeightPounds
        }
        storedParcels.map { record ->
            val parcel = record.parcel
            val boundaryMetrics = parcel.limitesGeoJson?.let { boundary ->
                runCatching { calculateParcelBoundaryMetrics(boundary) }.getOrNull()
            }
            val effectiveAreaHectares = boundaryMetrics?.areaHectares ?: parcel.areaHectareas
            val activeLot = storedLots.firstOrNull { lotRecord ->
                lotRecord.lot.estado == "ACTIVO" &&
                    lotRecord.lot.parcelaNombre.equals(parcel.nombre, ignoreCase = true)
            }
            val activeAnimalIds = activeLot?.activeAnimalIds.orEmpty()
            val knownWeights = activeAnimalIds.mapNotNull(latestWeightByAnimalId::get)
            ParcelUiModel(
                id = parcel.id,
                code = parcel.codigo,
                name = parcel.nombre,
                areaHectares = effectiveAreaHectares,
                pastureType = parcel.tipoPastura,
                status = if (record.currentLotName == null) parcel.estado else "OCUPADA",
                capacity = parcel.capacidadAnimales,
                currentLot = record.currentLotName.orEmpty(),
                boundaryGeoJson = parcel.limitesGeoJson,
                geographicAreaHectares = boundaryMetrics?.areaHectares,
                perimeterMeters = boundaryMetrics?.perimeterMeters,
                productivity = calculateParcelProductivity(
                    areaHectares = effectiveAreaHectares,
                    capacity = parcel.capacidadAnimales,
                    activeAnimalCount = activeAnimalIds.size,
                    latestKnownWeightsPounds = knownWeights
                )
            )
        }
    }
    val milkProductionRecords = remember(storedMilkProduction) {
        storedMilkProduction.map { record ->
            MilkProductionUiModel(
                id = record.id,
                dateMillis = record.fecha,
                date = formatDate(record.fecha),
                liters = record.litros,
                pricePerLiter = record.precioPorLitro,
                paymentDueDateMillis = record.fechaPagoProgramada,
                paymentDueDate = formatDate(record.fechaPagoProgramada),
                paymentConfirmedAtMillis = record.pagoConfirmadoEn,
                notes = record.observaciones.orEmpty()
            )
        }
    }
    val milkPayments = remember(milkProductionRecords) {
        milkProductionRecords
            .groupBy { record ->
                record.paymentDueDateMillis to (record.paymentConfirmedAtMillis != null)
            }
            .map { (key, groupedRecords) ->
                MilkPaymentUiModel(
                    paymentDueDateMillis = key.first,
                    paymentDueDate = formatDate(key.first),
                    amount = groupedRecords.sumOf { it.grossIncome },
                    productionRecordCount = groupedRecords.size,
                    confirmed = key.second,
                    confirmedAtMillis = groupedRecords.mapNotNull { it.paymentConfirmedAtMillis }
                        .maxOrNull()
                )
            }
            .sortedWith(
                compareBy<MilkPaymentUiModel> { it.confirmed }
                    .thenBy { it.paymentDueDateMillis }
            )
    }
    val employees = remember(storedEmployees) {
        storedEmployees.map { employee ->
            EmployeeUiModel(
                id = employee.id,
                fullName = employee.nombreCompleto,
                role = employee.cargo,
                hireDateMillis = employee.fechaIngreso,
                hireDate = formatDate(employee.fechaIngreso),
                salary = employee.salarioBase,
                paymentFrequency = employee.frecuenciaPago,
                assignedSector = employee.sectorAsignado.orEmpty(),
                phone = employee.telefono.orEmpty(),
                active = employee.activo
            )
        }
    }
    val employeeNamesById = remember(employees) { employees.associate { it.id to it.fullName } }
    val supplyEmployeeOptions = remember(employees) {
        employees
            .filter { it.active }
            .sortedBy { it.fullName.lowercase() }
            .map { employee -> SupplyEmployeeOption(employee.id, employee.fullName) }
    }
    val employeePayments = remember(storedEmployeePayments, employeeNamesById, lotLabelsById) {
        storedEmployeePayments.map { payment ->
            EmployeePaymentUiModel(
                id = payment.id,
                employeeId = payment.empleadoId,
                employeeName = employeeNamesById[payment.empleadoId] ?: "Empleado no disponible",
                paymentDateMillis = payment.fechaPago,
                paymentDate = formatDate(payment.fechaPago),
                amount = payment.monto,
                period = payment.periodo,
                lotId = payment.loteId,
                lotLabel = payment.loteId?.let(lotLabelsById::get),
                activity = payment.actividad.orEmpty(),
                notes = payment.observaciones.orEmpty()
            )
        }
    }
    // Finanzas reúne fuentes persistentes distintas sin copiar ni duplicar registros.
    val financialMovements = remember(
        storedFinancialMovements,
        milkProductionRecords,
        employeePayments,
        lotLabelsById
    ) {
        storedFinancialMovements.map { movement ->
            val lotSale = movement.categoria == "Venta de lote" && movement.loteId != null
            FinancialMovementUiModel(
                id = "manual-${movement.id}",
                type = movement.tipo,
                category = movement.categoria,
                amount = movement.monto,
                dateMillis = movement.fecha,
                date = formatDate(movement.fecha),
                notes = movement.observaciones.orEmpty(),
                sourceLabel = if (lotSale) "Generado al vender y cerrar el lote" else "Registro manual",
                automatic = lotSale,
                deletableRecordId = movement.id.takeUnless { lotSale },
                lotId = movement.loteId,
                lotLabel = movement.loteId?.let(lotLabelsById::get)
            )
        } + milkProductionRecords
            .filter { it.paymentConfirmedAtMillis != null }
            .groupBy { it.paymentDueDateMillis }
            .map { (paymentDueDate, productions) ->
            FinancialMovementUiModel(
                id = "milk-payment-$paymentDueDate",
                type = "INGRESO",
                category = "Producción de leche",
                amount = productions.sumOf { it.grossIncome },
                dateMillis = productions.mapNotNull { it.paymentConfirmedAtMillis }.maxOrNull()
                    ?: paymentDueDate,
                date = formatDate(
                    productions.mapNotNull { it.paymentConfirmedAtMillis }.maxOrNull()
                        ?: paymentDueDate
                ),
                notes = "Pago confirmado · ${productions.sumOf { it.liters }} L · " +
                    "${productions.size} registros",
                sourceLabel = "Confirmado desde Producción Lechera",
                automatic = true
            )
        } + employeePayments.map { payment ->
            FinancialMovementUiModel(
                id = "payment-${payment.id}",
                type = "EGRESO",
                category = "Pago de personal",
                amount = payment.amount,
                dateMillis = payment.paymentDateMillis,
                date = payment.paymentDate,
                notes = "${payment.employeeName} · ${payment.period}",
                sourceLabel = "Generado desde el historial de pagos",
                automatic = true,
                lotId = payment.lotId,
                lotLabel = payment.lotLabel
            )
        }
    }
    // Cada módulo entrega filas reales y fechadas al centro de reportes. Allí se
    // aplican los rangos y se calculan métricas sin usar cantidades de demostración.
    val reportRecords = remember(
        storedAnimals,
        weighings,
        milkProductionRecords,
        employees,
        employeePayments,
        financialMovements,
        storedLots,
        storedParcels
    ) {
        val animalItems = allAnimalItems.associateBy { it.id }
        val parcelStatusById = parcels.associate { it.id to it.status }
        buildList {
            storedAnimals.forEach { stored ->
                val animal = stored.animal
                val item = animalItems[animal.id]
                add(
                    ReportRecord(
                        id = animal.id,
                        reportId = "animals",
                        dateMillis = animal.creadoEn,
                        title = buildString {
                            append(animal.codigoIdentificacion)
                            animal.nombre?.takeIf(String::isNotBlank)?.let { append(" · $it") }
                        },
                        detail = listOfNotNull(
                            animal.categoria,
                            animal.sexo,
                            animal.raza?.takeIf(String::isNotBlank),
                            animal.estadoSalud
                        ).joinToString(" · "),
                        primaryValue = item?.ultimoPesoLibras,
                        kind = "ANIMAL",
                        attributes = mapOf(
                            "sex" to animal.sexo,
                            "status" to animal.estado,
                            "health" to animal.estadoSalud
                        )
                    )
                )
            }
            weighings.forEach { weighing ->
                add(
                    ReportRecord(
                        id = weighing.id,
                        reportId = "weighings",
                        dateMillis = weighing.dateMillis,
                        title = weighing.animalLabel,
                        detail = weighing.differenceLibras?.let { difference ->
                            "Cambio: ${if (difference > 0) "+" else ""}${"%.2f".format(Locale.US, difference)} lb"
                        } ?: "Primer pesaje disponible",
                        primaryValue = weighing.weightLibras,
                        secondaryValue = weighing.differenceLibras,
                        kind = "PESAJE"
                    )
                )
            }
            milkProductionRecords.forEach { production ->
                add(
                    ReportRecord(
                        id = production.id,
                        reportId = "production",
                        dateMillis = production.dateMillis,
                        title = "Producción del ${production.date}",
                        detail = "Q ${production.pricePerLiter} por litro · " +
                            "${if (production.paymentConfirmedAtMillis == null) "Pendiente" else "Confirmado"} " +
                            "Q ${"%.2f".format(Locale.US, production.grossIncome)}",
                        primaryValue = production.liters,
                        secondaryValue = production.grossIncome,
                        kind = "PRODUCCION",
                        attributes = mapOf(
                            "price" to production.pricePerLiter.toString(),
                            "paymentStatus" to if (production.paymentConfirmedAtMillis == null) {
                                "PENDING"
                            } else {
                                "CONFIRMED"
                            }
                        )
                    )
                )
            }
            financialMovements.forEach { movement ->
                add(
                    ReportRecord(
                        id = movement.id,
                        reportId = "financial",
                        dateMillis = movement.dateMillis,
                        title = movement.category,
                        detail = "${movement.type.lowercase().replaceFirstChar(Char::uppercase)} · ${movement.sourceLabel}",
                        primaryValue = movement.amount,
                        kind = movement.type
                    )
                )
            }
            storedEmployees.forEach { employee ->
                add(
                    ReportRecord(
                        id = "employee-${employee.id}",
                        reportId = "staff",
                        dateMillis = employee.creadoEn,
                        title = employee.nombreCompleto,
                        detail = "${employee.cargo} · Ingreso laboral ${formatDate(employee.fechaIngreso)}",
                        primaryValue = employee.salarioBase,
                        kind = "EMPLEADO",
                        attributes = mapOf("status" to if (employee.activo) "ACTIVO" else "INACTIVO")
                    )
                )
            }
            employeePayments.forEach { payment ->
                add(
                    ReportRecord(
                        id = "payment-${payment.id}",
                        reportId = "staff",
                        dateMillis = payment.paymentDateMillis,
                        title = "Pago · ${payment.employeeName}",
                        detail = payment.period,
                        primaryValue = payment.amount,
                        kind = "PAGO"
                    )
                )
            }
            storedLots.forEach { stored ->
                val lot = stored.lot
                add(
                    ReportRecord(
                        id = lot.id,
                        reportId = "lots",
                        dateMillis = lot.creadoEn,
                        title = "${lot.codigo} · ${lot.nombre}",
                        detail = "${lot.tipo} · ${lot.estado.lowercase().replaceFirstChar(Char::uppercase)}",
                        primaryValue = stored.activeAnimalIds.size.toDouble(),
                        secondaryValue = stored.averageWeightPounds,
                        kind = "LOTE",
                        attributes = mapOf("status" to lot.estado)
                    )
                )
            }
            storedParcels.forEach { stored ->
                val parcel = stored.parcel
                val status = parcelStatusById[parcel.id] ?: parcel.estado
                add(
                    ReportRecord(
                        id = parcel.id,
                        reportId = "parcels",
                        dateMillis = parcel.creadoEn,
                        title = "${parcel.codigo} · ${parcel.nombre}",
                        detail = "${parcel.tipoPastura} · ${status.lowercase().replaceFirstChar(Char::uppercase)}",
                        primaryValue = parcel.areaHectareas,
                        secondaryValue = parcel.capacidadAnimales?.toDouble(),
                        kind = "PARCELA",
                        attributes = mapOf("status" to status)
                    )
                )
            }
        }
    }
    var animalSeleccionado by remember { mutableStateOf<AnimalListItem?>(null) }
    var ultimoRegistro by remember { mutableStateOf<AnimalListItem?>(null) }
    var animalEnEdicionId by remember { mutableStateOf<String?>(null) }
    var animalSaveError by remember { mutableStateOf<String?>(null) }
    var animalIsSaving by remember { mutableStateOf(false) }
    var animalFieldUpdateError by remember { mutableStateOf<String?>(null) }
    var animalFieldIsUpdating by remember { mutableStateOf(false) }
    var selectedLotId by rememberSaveable { mutableStateOf("") }
    var lotEditingId by rememberSaveable { mutableStateOf("") }
    var lotDraftAnimalIds by remember { mutableStateOf(emptyList<String>()) }
    var lotIsSaving by remember { mutableStateOf(false) }
    var lotSaveError by remember { mutableStateOf<String?>(null) }
    var selectedParcelId by rememberSaveable { mutableStateOf("") }
    var parcelEditingId by rememberSaveable { mutableStateOf("") }
    var parcelIsSaving by remember { mutableStateOf(false) }
    var parcelSaveError by remember { mutableStateOf<String?>(null) }
    var selectedWeighingAnimalId by rememberSaveable { mutableStateOf("") }
    var weighingInitialAnimalId by rememberSaveable { mutableStateOf("") }
    var weighingInitialLotId by rememberSaveable { mutableStateOf("") }
    var weighingIsSaving by remember { mutableStateOf(false) }
    var weighingSaveError by remember { mutableStateOf<String?>(null) }
    var milkProductionIsSaving by remember { mutableStateOf(false) }
    var milkProductionSaveError by remember { mutableStateOf<String?>(null) }
    var milkConfigurationIsSaving by remember { mutableStateOf(false) }
    var milkConfigurationSaveError by remember { mutableStateOf<String?>(null) }
    var milkPaymentIsConfirming by remember { mutableStateOf(false) }
    var milkPaymentActionError by remember { mutableStateOf<String?>(null) }
    var milkNotificationsEnabled by remember { mutableStateOf(false) }
    var financeIsSaving by remember { mutableStateOf(false) }
    var financeSaveError by remember { mutableStateOf<String?>(null) }
    var financeIsDeleting by remember { mutableStateOf(false) }
    var financeDeleteError by remember { mutableStateOf<String?>(null) }
    var selectedEmployeeId by rememberSaveable { mutableStateOf("") }
    var employeeIsSaving by remember { mutableStateOf(false) }
    var employeeSaveError by remember { mutableStateOf<String?>(null) }
    var paymentIsSaving by remember { mutableStateOf(false) }
    var paymentSaveError by remember { mutableStateOf<String?>(null) }
    var sanitaryIsSaving by remember { mutableStateOf(false) }
    var sanitarySaveError by remember { mutableStateOf<String?>(null) }
    var sanitaryInitialAnimalId by remember { mutableStateOf("") }
    var supplyEditingId by rememberSaveable { mutableStateOf("") }
    var supplyIsSaving by remember { mutableStateOf(false) }
    var supplySaveError by remember { mutableStateOf<String?>(null) }
    var supplyMovementId by rememberSaveable { mutableStateOf("") }
    var supplyMovementIsSaving by remember { mutableStateOf(false) }
    var supplyMovementSaveError by remember { mutableStateOf<String?>(null) }
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
    var userEditingId by rememberSaveable { mutableStateOf("") }

    val selectedLot = remember(lots, selectedLotId) {
        lots.firstOrNull { it.id == selectedLotId }
    }
    val selectedParcel = remember(parcels, selectedParcelId) {
        parcels.firstOrNull { it.id == selectedParcelId }
    }
    val selectedEmployee = remember(employees, selectedEmployeeId) {
        employees.firstOrNull { it.id == selectedEmployeeId }
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
    LaunchedEffect(currentUserId) {
        milkNotificationsEnabled = milkNotificationPreferences.isEnabled(currentUserId)
    }

    /** Mantiene programado solo el aviso pendiente más próximo del dispositivo. */
    LaunchedEffect(milkPayments, milkNotificationsEnabled) {
        val nearestPendingPayment = milkPayments
            .asSequence()
            .filterNot { it.confirmed }
            .minByOrNull { it.paymentDueDateMillis }
        if (
            nearestPendingPayment != null &&
            milkNotificationPreferences.anyUserEnabled()
        ) {
            MilkPaymentNotificationScheduler.schedule(
                context,
                nearestPendingPayment.paymentDueDateMillis
            )
        } else {
            MilkPaymentNotificationScheduler.cancel(context)
        }
    }
    // Los permisos personalizados permiten entrar a módulos, pero no convierten
    // otro rol en Administrador General para modificar registros existentes.
    val canEditRecords = canEditExistingRecords(currentUser?.roleName)
    val canUpdateAnimalFields = canUpdateAnimalFieldStatus(currentPermissionIds)
    val canConfirmMilkPayment = canConfirmMilkPayments(currentUser?.roleName)
    val canImportData = canImportApplicationData(currentUser?.roleName)
    val canManageSupplyInventory = canManageSupplies(currentUser?.roleName)
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
    val sanitaryMedicineSupplies = remember(storedSupplies) {
        storedSupplies.mapNotNull { record ->
            val supply = record.supply
            val contentMl = supply.contenidoMlPorUnidad
            val availableMl = record.availableMilliliters
            if (
                !supply.activo || supply.categoria != SupplyCatalog.MEDICINES_AND_VITAMINS ||
                contentMl == null || availableMl == null || availableMl <= 0.0
            ) {
                null
            } else {
                SanitarySupplyOption(
                    id = supply.id,
                    label = "${supply.nombre} · ${supply.codigo} · " +
                        "${formatInventoryMl(availableMl)} ml disponibles",
                    availableMl = availableMl,
                    contentMlPerUnit = contentMl
                )
            }
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
        Routes.ANIMAL_DETAIL,
        Routes.ANIMAL_BIRTH_PERIOD -> Routes.ANIMAL_LIST
        Routes.LOT_FORM,
        Routes.LOT_ANIMAL_SELECTION,
        Routes.LOT_DETAIL -> Routes.LOTS
        Routes.PARCEL_FORM,
        Routes.PARCEL_DETAIL,
        Routes.PARCEL_MAP -> Routes.PARCELS
        Routes.WEIGHING_FORM,
        Routes.WEIGHING_ANIMAL_DETAIL -> Routes.WEIGHINGS
        Routes.MILK_PRODUCTION_FORM,
        Routes.MILK_CONFIGURATION -> Routes.MILK_PRODUCTION
        Routes.SANITARY_FORM -> Routes.SANITARY
        Routes.FINANCE_FORM -> Routes.FINANCE
        Routes.SUPPLY_FORM,
        Routes.SUPPLY_ENTRY,
        Routes.SUPPLY_EXIT,
        Routes.SUPPLY_ASSIGNMENTS,
        Routes.SUPPLY_DETAIL,
        Routes.SUPPLY_ADJUSTMENT -> Routes.SUPPLIES
        Routes.EMPLOYEE_FORM,
        Routes.EMPLOYEE_DETAIL,
        Routes.EMPLOYEE_PAYMENT_FORM -> Routes.EMPLOYEES
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

    /** Cierra la sesión actual y evita que las pantallas protegidas queden en el historial. */
    val logout: () -> Unit = {
        val revokeCloudSession = unifiedAuth.signOut()
        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) { revokeCloudSession() }
        sessionManager.clear()
        updateCurrentUser(null)
        coroutineScope.launch { drawerState.close() }
        navController.navigate(Routes.LOGIN) {
            popUpTo(Routes.DASHBOARD) { inclusive = true }
            launchSingleTop = true
        }
    }

    var cloudAction by remember { mutableStateOf<String?>(null) }
    var cloudUsers by remember(currentUserId) { mutableStateOf<List<com.fincahernandez.gestionpecuaria.ui.screens.users.UserUiModel>>(emptyList()) }
    var cloudUserStatus by remember(currentUserId) { mutableStateOf("Conéctate para cargar las cuentas.") }
    var cloudUsersRefresh by remember { mutableStateOf(0) }
    val autoSyncStatus = com.fincahernandez.gestionpecuaria.ui.components.rememberAutoSyncStatus(
        currentUser?.id, cloudSessionManager, paused = cloudAction != null || currentRoute in setOf(Routes.USERS, Routes.USER_FORM)
    )
    if (cloudAction != null && currentUser != null) {
        com.fincahernandez.gestionpecuaria.ui.screens.auth.CloudToolsDialog(
            action = cloudAction!!, user = currentUser, manager = cloudSessionManager,
            syncStatus = autoSyncStatus,
            onClose = { cloudAction = null },
            onProfileSaved = { unifiedAuth.remembered(currentUser.id)?.let(updateCurrentUser) }
        )
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
                    syncStatus = autoSyncStatus.label(),
                    allowedRoutes = allowedMainRoutes,
                    userName = currentUser?.fullName.orEmpty(),
                    roleName = currentUser?.roleName.orEmpty(),
                    onDestinationClick = { route ->
                        coroutineScope.launch { drawerState.close() }
                        if (route == Routes.SANITARY) sanitaryInitialAnimalId = ""
                        navigateMain(route)
                    },
                    onLogout = logout,
                    onDownload = { cloudAction = "download"; coroutineScope.launch { drawerState.close() } },
                    onProfile = { cloudAction = "profile"; coroutineScope.launch { drawerState.close() } },
                    onSync = { cloudAction = "sync"; coroutineScope.launch { drawerState.close() } }
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
                    val rememberedUser = sessionManager.rememberedUserId()
                        ?.let { unifiedAuth.remembered(it) }
                    if (rememberedUser == null) sessionManager.clear()
                    updateCurrentUser(rememberedUser)

                    val elapsed = System.currentTimeMillis() - startedAt
                    delay((1_700L - elapsed).coerceAtLeast(0L))
                    val destination = when {
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
                    onResetAccess = null,
                    onLogin = { email, password, rememberSession ->
                        loginIsLoading = true
                        loginError = null
                        coroutineScope.launch {
                            try {
                                val user = unifiedAuth.login(email, password)
                                updateCurrentUser(user)
                                if (rememberSession) sessionManager.remember(user.id) else sessionManager.clear()
                                navController.navigate(Routes.DASHBOARD) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                            } catch (error: kotlinx.coroutines.CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                loginError = when (error) {
                                    is com.fincahernandez.gestionpecuaria.data.remote.CloudAccessDenied,
                                    is com.fincahernandez.gestionpecuaria.data.remote.CloudHttpException,
                                    is IllegalArgumentException -> error.message ?: "No se pudo validar el acceso."
                                    else -> "No se pudo iniciar sesión. Revisa la conexión y vuelve a intentar."
                                }
                            } finally { loginIsLoading = false }
                        }
                    }
                )
            }

            composable(Routes.DASHBOARD) {
                val latestMilk = milkProductionRecords.maxByOrNull { it.dateMillis }
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
                    financialMovements.maxByOrNull { it.dateMillis }?.let { movement ->
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
                    syncStatus = autoSyncStatus,
                    onSyncClick = { cloudAction = "sync" }
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
                            animalFieldUpdateError = null
                            navController.navigate(Routes.ANIMAL_DETAIL)
                        }
                    },
                    onOpenBirthPeriod = {
                        navController.navigate(Routes.ANIMAL_BIRTH_PERIOD)
                    },
                    onMenuClick = openDrawer,
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.ANIMAL_BIRTH_PERIOD) {
                AnimalBirthPeriodScreen(
                    animals = allAnimalItems,
                    onBack = { navController.popBackStack() },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.ANIMAL_FORM) {
                val animalEnEdicion = animales.firstOrNull { it.id == animalEnEdicionId }
                val motherOptions = remember(animales, animalEnEdicion, animalItemsById) {
                    val birthDatesByMotherId = allAnimalItems
                        .asSequence()
                        .filter { child ->
                            child.madreId.isNotBlank() && child.fechaNacimiento.isNotBlank()
                        }
                        .groupBy(
                            keySelector = AnimalListItem::madreId,
                            valueTransform = AnimalListItem::fechaNacimiento
                        )
                        .mapValues { (_, dates) -> dates.toSet() }
                    val eligible = animales
                        .filter {
                            it.sexo == "HEMBRA" && it.id != animalEnEdicion?.id &&
                                (it.proximaParto || birthDatesByMotherId[it.id].orEmpty().isNotEmpty())
                        }
                        .toMutableList()
                    // Conserva visible una madre ya asignada aunque luego haya sido retirada.
                    animalEnEdicion?.madreId
                        ?.takeIf(String::isNotBlank)
                        ?.let(animalItemsById::get)
                        ?.takeIf { storedMother -> eligible.none { it.id == storedMother.id } }
                        ?.let(eligible::add)
                    eligible
                        .sortedWith(compareBy({ it.nombre.orEmpty() }, { it.codigoIdentificacion }))
                        .map { mother ->
                            AnimalMotherOption(
                                id = mother.id,
                                label = "${mother.nombre?.takeIf(String::isNotBlank) ?: "Sin nombre"} · " +
                                    mother.codigoIdentificacion,
                                isNearCalving = mother.proximaParto,
                                registeredBirthDates = birthDatesByMotherId[mother.id].orEmpty(),
                                isCurrentMother = mother.id == animalEnEdicion?.madreId
                            )
                        }
                }
                AnimalFormScreen(
                    codigoGenerado = animalEnEdicion?.codigoIdentificacion
                        ?: generarCodigoAnimal(allAnimalItems),
                    initialData = animalEnEdicion?.toFormData(),
                    motherOptions = motherOptions,
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
                                check(animalEnEdicion == null || canEditRecords) {
                                    "Solo el Administrador General puede editar animales."
                                }
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
                                val errorMessage = error.message.orEmpty()
                                animalSaveError = when {
                                    errorMessage.contains("UNIQUE", ignoreCase = true) ->
                                        "Ese código de identificación ya existe. Intente nuevamente."
                                    error is IllegalArgumentException ||
                                        error is IllegalStateException ->
                                        errorMessage.ifBlank {
                                            "No fue posible guardar el animal. Verifique los datos."
                                        }
                                    else ->
                                        "No fue posible guardar el animal. Inténtelo nuevamente."
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
                    motherLabel = animal.madreId
                        .takeIf(String::isNotBlank)
                        ?.let(animalItemsById::get)
                        ?.let { mother ->
                            "${mother.nombre?.takeIf(String::isNotBlank) ?: "Sin nombre"} · " +
                                mother.codigoIdentificacion
                        },
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
                    birthHistory = remember(allAnimalItems, animal.id) {
                        buildAnimalBirthHistory(animal.id, allAnimalItems)
                    },
                    currentLotLabel = activeLotByAnimalId[animal.id]
                        ?.let(lotsById::get)
                        ?.let { lot -> "${lot.name} · ${lot.code}" },
                    motherLabel = animal.madreId
                        .takeIf(String::isNotBlank)
                        ?.let(animalItemsById::get)
                        ?.let { mother ->
                            "${mother.nombre?.takeIf(String::isNotBlank) ?: "Sin nombre"} · " +
                                mother.codigoIdentificacion
                        },
                    canEditRecords = canEditRecords,
                    canUpdateFieldStatus = canUpdateAnimalFields,
                    isUpdatingFieldStatus = animalFieldIsUpdating,
                    fieldUpdateError = animalFieldUpdateError,
                    onBack = { navController.popBackStack() },
                    onEdit = {
                        if (canEditRecords) {
                            animalEnEdicionId = animal.id
                            animalSaveError = null
                            navController.navigate(Routes.ANIMAL_FORM)
                        }
                    },
                    onDelete = {
                        coroutineScope.launch {
                            runCatching {
                                check(canEditRecords) {
                                    "Solo el Administrador General puede retirar animales."
                                }
                                animalViewModel.removeAnimal(animal.id)
                            }
                                .onSuccess {
                                    animalSeleccionado = null
                                    if (ultimoRegistro?.id == animal.id) ultimoRegistro = null
                                    if (!navController.popBackStack(Routes.ANIMAL_LIST, false)) {
                                        navigateMain(Routes.ANIMAL_LIST)
                                    }
                                }
                        }
                    },
                    onRegisterWeight = {
                        weighingInitialAnimalId = animal.id
                        weighingInitialLotId = activeLotByAnimalId[animal.id].orEmpty()
                        weighingSaveError = null
                        navController.navigate(Routes.WEIGHING_FORM)
                    },
                    onChangePhoto = { photoUri ->
                        if (canUpdateAnimalFields) {
                            animalFieldIsUpdating = true
                            animalFieldUpdateError = null
                            coroutineScope.launch {
                                runCatching {
                                    animalViewModel.updatePhoto(animal.id, photoUri)
                                }.onFailure { error ->
                                    animalFieldUpdateError = error.message
                                        ?: "No fue posible actualizar la fotografía."
                                }
                                animalFieldIsUpdating = false
                            }
                        }
                    },
                    onChangeNearCalving = { nearCalving ->
                        if (canUpdateAnimalFields) {
                            animalFieldIsUpdating = true
                            animalFieldUpdateError = null
                            coroutineScope.launch {
                                runCatching {
                                    animalViewModel.updateNearCalving(animal.id, nearCalving)
                                }.onFailure { error ->
                                    animalFieldUpdateError = error.message
                                        ?: "No fue posible actualizar el control de parto."
                                }
                                animalFieldIsUpdating = false
                            }
                        }
                    },
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
                                    check(canEditRecords) {
                                        "Solo el Administrador General puede editar lotes."
                                    }
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
                val lotAssignments = storedLots
                    .firstOrNull { it.lot.id == lot.id }
                    ?.assignments
                    .orEmpty()
                val detailWeightRecords = lotWeighingsWithinAssignments(
                    assignments = lotAssignments,
                    weighings = storedWeighings
                ).map { record ->
                    LotWeightRecord(
                        animalId = record.animalId,
                        dateMillis = record.fechaPesaje,
                        dateLabel = formatDate(record.fechaPesaje),
                        weightPounds = record.pesoLibras
                    )
                }
                val lotLatestWeights = allAnimalItems
                    .filter { it.id in lot.selectedAnimalIds }
                    .mapNotNull { it.ultimoPesoLibras }
                val lotFinancialSummary = LotFinancialSummary(
                    directExpenses = storedFinancialMovements
                        .filter { it.loteId == lot.id && it.tipo == "EGRESO" }
                        .sumOf { it.monto },
                    supplyCosts = storedSupplyMovements
                        .filter {
                            it.loteId == lot.id && it.tipo in setOf("SALIDA", "SALIDA_SANITARIA")
                        }
                        .sumOf { it.cantidad * (it.costoUnitario ?: 0.0) },
                    laborCosts = storedEmployeePayments
                        .filter { it.loteId == lot.id }
                        .sumOf { it.monto },
                    income = storedFinancialMovements
                        .filter { it.loteId == lot.id && it.tipo == "INGRESO" }
                        .sumOf { it.monto },
                    currentWeightPounds = lotLatestWeights.sum(),
                    weightedAnimalCount = lotLatestWeights.size,
                    unvaluedSupplyConsumptions = storedSupplyMovements.count {
                        it.loteId == lot.id &&
                            it.tipo in setOf("SALIDA", "SALIDA_SANITARIA") &&
                            it.costoUnitario == null
                    }
                )
                LotDetailScreen(
                    lot = lot,
                    canEditRecords = canEditRecords,
                    financialSummary = lotFinancialSummary,
                    selectedAnimalLabels = allAnimalItems
                        .filter { it.id in lot.selectedAnimalIds }
                        .map { it.nombre ?: it.codigoIdentificacion },
                    weightRecords = detailWeightRecords,
                    onBack = { navController.popBackStack() },
                    onEdit = {
                        if (canEditRecords) {
                            lotEditingId = lot.id
                            lotDraftAnimalIds = lot.selectedAnimalIds
                            lotSaveError = null
                            navController.navigate(Routes.LOT_FORM)
                        }
                    },
                    onDeactivate = {
                        coroutineScope.launch {
                            lotIsSaving = true
                            lotSaveError = null
                            runCatching {
                                check(canEditRecords) {
                                    "Solo el Administrador General puede cerrar lotes."
                                }
                                lotViewModel.deactivateLot(lot.id)
                            }
                                .onFailure { error ->
                                    lotSaveError = error.message ?: "No se pudo cerrar el lote."
                                }
                            lotIsSaving = false
                        }
                    },
                    onSellAndClose = { sale ->
                        coroutineScope.launch {
                            lotIsSaving = true
                            lotSaveError = null
                            runCatching {
                                check(canEditRecords) {
                                    "Solo el Administrador General puede vender y cerrar lotes."
                                }
                                lotViewModel.sellAndCloseLot(
                                    lot.id,
                                    LotSaleDraft(
                                        amount = sale.amount.replace(',', '.').toDouble(),
                                        date = parseDate(sale.date)
                                            ?: error("La fecha de venta no es válida."),
                                        notes = sale.notes.ifBlank { null }
                                    )
                                )
                            }.onFailure { error ->
                                lotSaveError = error.message ?: "No se pudo registrar la venta."
                            }
                            lotIsSaving = false
                        }
                    },
                    onRegisterWeight = {
                        weighingInitialAnimalId = ""
                        weighingInitialLotId = lot.id
                        weighingSaveError = null
                        navController.navigate(Routes.WEIGHING_FORM)
                    },
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
                        if (canEditRecords) {
                            parcelEditingId = ""
                            parcelSaveError = null
                            navController.navigate(Routes.PARCEL_FORM)
                        }
                    },
                    onParcelClick = { parcelId ->
                        parcels.firstOrNull { it.id == parcelId }?.let {
                            selectedParcelId = parcelId
                            parcelSaveError = null
                            navController.navigate(Routes.PARCEL_DETAIL)
                        }
                    },
                    onNavigateMain = navigateMain,
                    canManageParcels = canEditRecords
                )
            }

            composable(Routes.PARCEL_FORM) {
                if (!canEditRecords) {
                    LaunchedEffect(Unit) { navigateMain(Routes.PARCELS) }
                    return@composable
                }
                val editingParcel = parcels.firstOrNull { it.id == parcelEditingId }
                ParcelFormScreen(
                    onBack = { navController.popBackStack() },
                    onSubmit = { form ->
                        coroutineScope.launch {
                            parcelIsSaving = true
                            parcelSaveError = null
                            runCatching {
                                check(canEditRecords) {
                                    "Solo el Administrador General puede crear o editar parcelas."
                                }
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
                    canEditRecords = canEditRecords,
                    onBack = { navController.popBackStack() },
                    onOpenMap = { navController.navigate(Routes.PARCEL_MAP) },
                    onEdit = {
                        if (canEditRecords) {
                            parcelEditingId = parcel.id
                            parcelSaveError = null
                            navController.navigate(Routes.PARCEL_FORM)
                        }
                    },
                    onSetResting = { resting ->
                        coroutineScope.launch {
                            parcelIsSaving = true
                            parcelSaveError = null
                            runCatching {
                                check(canEditRecords) {
                                    "Solo el Administrador General puede cambiar una parcela."
                                }
                                parcelViewModel.setResting(parcel.id, resting)
                            }
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
                            runCatching {
                                check(canEditRecords) {
                                    "Solo el Administrador General puede desactivar parcelas."
                                }
                                parcelViewModel.deactivateParcel(parcel.id)
                            }
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
            composable(Routes.PARCEL_MAP) {
                val parcel = selectedParcel
                if (parcel == null) {
                    LaunchedEffect(Unit) { navigateMain(Routes.PARCELS) }
                    return@composable
                }
                ParcelMapScreen(
                    parcel = parcel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.WEIGHINGS) {
                WeighingListScreen(
                    animals = weighingAnimals,
                    onMenuClick = openDrawer,
                    onCreateWeighing = {
                        weighingInitialAnimalId = ""
                        weighingInitialLotId = ""
                        weighingSaveError = null
                        navController.navigate(Routes.WEIGHING_FORM)
                    },
                    onAnimalClick = { animalId ->
                        selectedWeighingAnimalId = animalId
                        navController.navigate(Routes.WEIGHING_ANIMAL_DETAIL)
                    },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.WEIGHING_ANIMAL_DETAIL) {
                val animal = weighingAnimals.firstOrNull { it.animalId == selectedWeighingAnimalId }
                if (animal == null) {
                    LaunchedEffect(Unit) { navigateMain(Routes.WEIGHINGS) }
                    return@composable
                }
                WeighingAnimalDetailScreen(
                    animal = animal,
                    records = weighings.filter { it.animalId == animal.animalId },
                    onBack = { navController.popBackStack() },
                    onRegisterWeight = {
                        weighingInitialAnimalId = animal.animalId
                        weighingInitialLotId = activeLotByAnimalId[animal.animalId].orEmpty()
                        weighingSaveError = null
                        navController.navigate(Routes.WEIGHING_FORM)
                    },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.WEIGHING_FORM) {
                val sourceAnimals = if (weighingInitialLotId.isBlank()) {
                    animales
                } else {
                    val lotAnimalIds = lots.firstOrNull { it.id == weighingInitialLotId }
                        ?.selectedAnimalIds
                        .orEmpty()
                    animales.filter { it.id in lotAnimalIds }
                }
                val animalOptions = sourceAnimals.map { animal ->
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
                    initialAnimalId = weighingInitialAnimalId,
                    contextLotLabel = lots.firstOrNull { it.id == weighingInitialLotId }
                        ?.let { "${it.code} · ${it.name}" },
                    isSaving = weighingIsSaving,
                    saveError = weighingSaveError,
                    onBack = { navController.popBackStack() },
                    onSubmit = { form ->
                        val selectedAnimal = animales.first { it.id == form.animalId }
                        val newWeight = form.weightLibras.replace(',', '.').toDouble()
                        val associatedLotId = weighingInitialLotId.takeIf { it.isNotBlank() }
                            ?: activeLotByAnimalId[selectedAnimal.id]

                        coroutineScope.launch {
                            weighingIsSaving = true
                            weighingSaveError = null
                            runCatching {
                                animalViewModel.registerWeight(
                                    animalId = selectedAnimal.id,
                                    weightPounds = newWeight,
                                    weighingDate = parseDate(form.date) ?: System.currentTimeMillis(),
                                    notes = form.notes.ifBlank { null },
                                    lotId = associatedLotId,
                                    recordType = if (associatedLotId == null) "INDIVIDUAL" else "LOTE"
                                )
                            }.onSuccess {
                                weighingInitialAnimalId = ""
                                weighingInitialLotId = ""
                                if (!navController.popBackStack()) {
                                    navigateMain(Routes.WEIGHINGS)
                                }
                            }.onFailure { error ->
                                weighingSaveError = error.message ?: "No se pudo guardar el pesaje."
                            }
                            weighingIsSaving = false
                        }
                    }
                )
            }

            composable(Routes.MILK_PRODUCTION) {
                MilkProductionListScreen(
                    records = milkProductionRecords,
                    payments = milkPayments,
                    configuration = storedMilkConfiguration,
                    isGeneralAdministrator = canEditRecords,
                    canConfirmPayments = canConfirmMilkPayment,
                    notificationsEnabled = milkNotificationsEnabled,
                    isConfirmingPayment = milkPaymentIsConfirming,
                    paymentActionError = milkPaymentActionError,
                    onMenuClick = openDrawer,
                    onCreateRecord = {
                        milkProductionSaveError = null
                        navController.navigate(Routes.MILK_PRODUCTION_FORM)
                    },
                    onConfigure = {
                        if (canEditRecords) {
                            milkConfigurationSaveError = null
                            navController.navigate(Routes.MILK_CONFIGURATION)
                        }
                    },
                    onNotificationPreferenceChange = { enabled ->
                        milkNotificationPreferences.setEnabled(currentUserId, enabled)
                        milkNotificationsEnabled = enabled
                    },
                    onConfirmPayment = { paymentDueDate ->
                        if (canConfirmMilkPayment) {
                            coroutineScope.launch {
                                milkPaymentIsConfirming = true
                                milkPaymentActionError = null
                                runCatching {
                                    check(canConfirmMilkPayment) {
                                        "Su rol no está autorizado para confirmar pagos de leche."
                                    }
                                    milkProductionViewModel.confirmPayment(paymentDueDate)
                                }.onFailure { error ->
                                    milkPaymentActionError = error.message
                                        ?: "No se pudo confirmar el pago."
                                }
                                milkPaymentIsConfirming = false
                            }
                        }
                    },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.MILK_CONFIGURATION) {
                // La pantalla puede alcanzarse únicamente desde el botón mostrado al administrador.
                if (!canEditRecords) {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                } else {
                    MilkConfigurationScreen(
                        configuration = storedMilkConfiguration,
                        isSaving = milkConfigurationIsSaving,
                        saveError = milkConfigurationSaveError,
                        onBack = { navController.popBackStack() },
                        onSave = { price, frequency ->
                            coroutineScope.launch {
                                milkConfigurationIsSaving = true
                                milkConfigurationSaveError = null
                                runCatching {
                                    check(canEditRecords) {
                                        "Solo el Administrador General puede cambiar esta configuración."
                                    }
                                    milkProductionViewModel.saveConfiguration(price, frequency)
                                }.onSuccess {
                                    navController.popBackStack()
                                }.onFailure { error ->
                                    milkConfigurationSaveError = error.message
                                        ?: "No se pudo guardar la configuración."
                                }
                                milkConfigurationIsSaving = false
                            }
                        }
                    )
                }
            }

            composable(Routes.MILK_PRODUCTION_FORM) {
                MilkProductionFormScreen(
                    configuration = storedMilkConfiguration,
                    isSaving = milkProductionIsSaving,
                    saveError = milkProductionSaveError,
                    onBack = { navController.popBackStack() },
                    onSubmit = { form ->
                        coroutineScope.launch {
                            milkProductionIsSaving = true
                            milkProductionSaveError = null
                            runCatching {
                                val productionDate = parseDate(form.date)
                                    ?: error("La fecha seleccionada no es válida.")
                                val replacesExistingRecord = milkProductionRecords.any {
                                    it.dateMillis == productionDate
                                }
                                check(!replacesExistingRecord || canEditRecords) {
                                    "Solo el Administrador General puede corregir una producción existente."
                                }
                                val activeConfiguration = checkNotNull(storedMilkConfiguration) {
                                    "El Administrador General debe configurar el precio de la leche."
                                }
                                milkProductionViewModel.saveDailyProduction(
                                    MilkProductionDraft(
                                        date = productionDate,
                                        liters = form.liters.replace(',', '.').toDouble(),
                                        // El formulario muestra el valor como solo lectura; se vuelve
                                        // a tomar de Room para impedir alteraciones desde la interfaz.
                                        pricePerLiter = activeConfiguration.pricePerLiter,
                                        paymentFrequency = activeConfiguration.paymentFrequency,
                                        notes = form.notes.ifBlank { null }
                                    )
                                )
                            }.onSuccess {
                                if (!navController.popBackStack(Routes.MILK_PRODUCTION, false)) {
                                    navigateMain(Routes.MILK_PRODUCTION)
                                }
                            }.onFailure { error ->
                                milkProductionSaveError = error.message
                                    ?: "No se pudo guardar la producción."
                            }
                            milkProductionIsSaving = false
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
                    medicineSupplies = sanitaryMedicineSupplies,
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
                                        supplyId = form.supplyId,
                                        doseMl = form.doseMl.replace(',', '.').toDoubleOrNull(),
                                        medication = form.supplyId?.let { selectedId ->
                                            storedSupplies.firstOrNull {
                                                it.supply.id == selectedId
                                            }?.supply?.nombre
                                        },
                                        dose = form.doseMl.takeIf { it.isNotBlank() }?.let {
                                            "${it.replace(',', '.')} ml"
                                        },
                                        healthStatus = form.healthStatus,
                                        nextControlDate = parseDate(form.nextControlDate),
                                        responsible = form.responsible.ifBlank { null },
                                        notes = form.notes.ifBlank { null }
                                    ),
                                    registeredByUserId = currentUserId
                                )
                            }.onSuccess {
                                sanitaryIsSaving = false
                                sanitaryInitialAnimalId = ""
                                if (!navController.popBackStack(Routes.SANITARY, false)) {
                                    navigateMain(Routes.SANITARY)
                                }
                            }.onFailure { error ->
                                sanitaryIsSaving = false
                                sanitarySaveError = error.message
                                    ?: "No fue posible guardar el evento sanitario. Inténtelo nuevamente."
                            }
                        }
                    }
                )
            }

            composable(Routes.FINANCE) {
                FinanceListScreen(
                    movements = financialMovements,
                    onMenuClick = openDrawer,
                    onCreateMovement = {
                        financeSaveError = null
                        navController.navigate(Routes.FINANCE_FORM)
                    },
                    onNavigateMain = navigateMain,
                    canDeleteMovements = canEditRecords,
                    isDeletingMovement = financeIsDeleting,
                    deleteError = financeDeleteError,
                    onClearDeleteError = { financeDeleteError = null },
                    onDeleteMovement = { movementId, password ->
                        if (canEditRecords && !financeIsDeleting) {
                            coroutineScope.launch {
                                financeIsDeleting = true
                                financeDeleteError = null
                                runCatching {
                                    val administrator = checkNotNull(currentUser) {
                                        "La sesión actual ya no está disponible."
                                    }
                                    check(canEditExistingRecords(administrator.roleName)) {
                                        "Solo el Administrador General puede eliminar movimientos."
                                    }
                                    check(
                                        unifiedAuth.verifyCurrentPassword(
                                            userId = administrator.id,
                                            email = administrator.username,
                                            password = password
                                        )
                                    ) { "La contraseña es incorrecta." }
                                    financeViewModel.deleteManualMovement(movementId)
                                }.onFailure { error ->
                                    financeDeleteError = error.message
                                        ?: "No fue posible eliminar el movimiento."
                                }
                                financeIsDeleting = false
                            }
                        }
                    }
                )
            }

            composable(Routes.FINANCE_FORM) {
                FinancialMovementFormScreen(
                    lotOptions = lots.map { FinancialLotOption(it.id, "${it.code} · ${it.name}") },
                    isSaving = financeIsSaving,
                    saveError = financeSaveError,
                    onBack = { navController.popBackStack() },
                    onSubmit = { form ->
                        coroutineScope.launch {
                            financeIsSaving = true
                            financeSaveError = null
                            runCatching {
                                financeViewModel.saveMovement(
                                    FinancialMovementDraft(
                                        type = form.type,
                                        category = form.category,
                                        amount = form.amount.replace(',', '.').toDouble(),
                                        date = parseDate(form.date)
                                            ?: error("La fecha seleccionada no es válida."),
                                        lotId = form.lotId.ifBlank { null },
                                        notes = form.notes.ifBlank { null }
                                    )
                                )
                            }.onSuccess {
                                if (!navController.popBackStack(Routes.FINANCE, false)) {
                                    navigateMain(Routes.FINANCE)
                                }
                            }.onFailure { error ->
                                financeSaveError = error.message ?: "No se pudo guardar el movimiento."
                            }
                            financeIsSaving = false
                        }
                    }
                )
            }

            composable(Routes.SUPPLIES) {
                SupplyListScreen(
                    supplies = storedSupplies,
                    canManageSupplies = canManageSupplyInventory,
                    onMenuClick = openDrawer,
                    onCreateSupply = {
                        supplyEditingId = ""
                        supplySaveError = null
                        navController.navigate(Routes.SUPPLY_FORM)
                    },
                    onEditSupply = { supplyId ->
                        supplyEditingId = supplyId
                        supplySaveError = null
                        navController.navigate(Routes.SUPPLY_FORM)
                    },
                    onRegisterEntry = { supplyId ->
                        supplyMovementId = supplyId
                        supplyMovementSaveError = null
                        navController.navigate(Routes.SUPPLY_ENTRY)
                    },
                    onRegisterExit = { supplyId ->
                        supplyMovementId = supplyId
                        supplyMovementSaveError = null
                        navController.navigate(Routes.SUPPLY_EXIT)
                    },
                    onManageAssignments = { supplyId ->
                        supplyMovementId = supplyId
                        supplyMovementSaveError = null
                        navController.navigate(Routes.SUPPLY_ASSIGNMENTS)
                    },
                    onViewHistory = { supplyId ->
                        supplyMovementId = supplyId
                        navController.navigate(Routes.SUPPLY_DETAIL)
                    },
                    onReactivateSupply = { supplyId ->
                        coroutineScope.launch {
                            supplySaveError = null
                            runCatching {
                                supplyViewModel.reactivateSupply(supplyId)
                            }.onFailure { error ->
                                supplySaveError = error.message
                                    ?: "No fue posible reactivar el producto."
                            }
                        }
                    },
                    actionError = supplySaveError,
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.SUPPLY_FORM) {
                if (!canManageSupplyInventory) {
                    LaunchedEffect(Unit) { navigateMain(Routes.SUPPLIES) }
                    return@composable
                }
                val editingRecord = storedSupplies.firstOrNull {
                    it.supply.id == supplyEditingId
                }
                val editingSupply = editingRecord?.supply
                val canDeletePermanently = editingSupply != null &&
                    storedSupplyStocks.none { it.insumoId == editingSupply.id } &&
                    storedSupplyMovements.none { it.insumoId == editingSupply.id } &&
                    storedSupplyAssignments.none { it.insumoId == editingSupply.id }
                SupplyFormScreen(
                    existingCodes = storedSupplies.map { it.supply.codigo },
                    initialData = editingSupply?.let { supply ->
                        SupplyFormData(
                            id = supply.id,
                            code = supply.codigo,
                            name = supply.nombre,
                            category = supply.categoria,
                            unit = supply.unidadMedida,
                            minimumStock = supply.existenciaMinima.toString(),
                            contentMlPerUnit = supply.contenidoMlPorUnidad?.toString().orEmpty()
                        )
                    },
                    isSaving = supplyIsSaving,
                    saveError = supplySaveError,
                    canDeletePermanently = canDeletePermanently,
                    onBack = { navController.popBackStack() },
                    onSubmit = { form ->
                        coroutineScope.launch {
                            supplyIsSaving = true
                            supplySaveError = null
                            runCatching {
                                val draft = SupplyDraft(
                                    name = form.name,
                                    category = form.category,
                                    unit = form.unit,
                                    minimumStock = form.minimumStock
                                        .replace(',', '.')
                                        .toDouble(),
                                    contentMlPerUnit = form.contentMlPerUnit
                                        .replace(',', '.')
                                        .toDoubleOrNull()
                                )
                                if (editingSupply == null) {
                                    supplyViewModel.createSupply(draft)
                                } else {
                                    supplyViewModel.updateSupply(editingSupply.id, draft)
                                }
                            }.onSuccess {
                                supplyIsSaving = false
                                supplyEditingId = ""
                                if (!navController.popBackStack(Routes.SUPPLIES, false)) {
                                    navigateMain(Routes.SUPPLIES)
                                }
                            }.onFailure { error ->
                                supplyIsSaving = false
                                supplySaveError = error.message
                                    ?: "No fue posible guardar el insumo."
                            }
                        }
                    },
                    onDeactivate = editingSupply?.let { supply ->
                        {
                            coroutineScope.launch {
                                supplyIsSaving = true
                                supplySaveError = null
                                runCatching {
                                    supplyViewModel.deactivateSupply(supply.id)
                                }.onSuccess {
                                    supplyIsSaving = false
                                    supplyEditingId = ""
                                    if (!navController.popBackStack(Routes.SUPPLIES, false)) {
                                        navigateMain(Routes.SUPPLIES)
                                    }
                                }.onFailure { error ->
                                    supplyIsSaving = false
                                    supplySaveError = error.message
                                        ?: "No fue posible desactivar el producto."
                                }
                            }
                        }
                    },
                    onDelete = editingSupply?.takeIf { canDeletePermanently }?.let { supply ->
                        {
                            coroutineScope.launch {
                                supplyIsSaving = true
                                supplySaveError = null
                                runCatching {
                                    supplyViewModel.deleteUnusedSupply(supply.id)
                                }.onSuccess {
                                    supplyIsSaving = false
                                    supplyEditingId = ""
                                    if (!navController.popBackStack(Routes.SUPPLIES, false)) {
                                        navigateMain(Routes.SUPPLIES)
                                    }
                                }.onFailure { error ->
                                    supplyIsSaving = false
                                    supplySaveError = error.message
                                        ?: "No fue posible eliminar el producto."
                                }
                            }
                        }
                    }
                )
            }

            composable(Routes.SUPPLY_ENTRY) {
                if (!canManageSupplyInventory) {
                    LaunchedEffect(Unit) { navigateMain(Routes.SUPPLIES) }
                    return@composable
                }
                val record = storedSupplies.firstOrNull { it.supply.id == supplyMovementId }
                if (record == null) {
                    LaunchedEffect(Unit) { navigateMain(Routes.SUPPLIES) }
                    return@composable
                }
                SupplyEntryScreen(
                    record = record,
                    registeredByName = currentUserFullName
                        .ifBlank { currentUsername }
                        .ifBlank { "Usuario actual" },
                    isSaving = supplyMovementIsSaving,
                    saveError = supplyMovementSaveError,
                    onBack = { navController.popBackStack() },
                    onSubmit = { form ->
                        coroutineScope.launch {
                            supplyMovementIsSaving = true
                            supplyMovementSaveError = null
                            runCatching {
                                supplyViewModel.registerEntry(
                                    SupplyEntryDraft(
                                        supplyId = record.supply.id,
                                        quantity = form.quantity.replace(',', '.').toDouble(),
                                        unitCost = form.unitCost
                                            .takeIf(String::isNotBlank)
                                            ?.replace(',', '.')
                                            ?.toDouble(),
                                        date = parseDate(form.entryDate)
                                            ?: error("La fecha de entrada no es válida."),
                                        expirationDate = form.expirationDate
                                            .takeIf(String::isNotBlank)
                                            ?.let { date ->
                                                parseDate(date)
                                                    ?: error("El vencimiento no es válido.")
                                            },
                                        userId = currentUser?.id,
                                        responsible = currentUserFullName
                                            .ifBlank { currentUsername }
                                            .ifBlank { "Usuario actual" },
                                        notes = form.notes.ifBlank { null }
                                    )
                                )
                            }.onSuccess {
                                supplyMovementIsSaving = false
                                supplyMovementId = ""
                                if (!navController.popBackStack(Routes.SUPPLIES, false)) {
                                    navigateMain(Routes.SUPPLIES)
                                }
                            }.onFailure { error ->
                                supplyMovementIsSaving = false
                                supplyMovementSaveError = error.message
                                    ?: "No fue posible registrar la entrada."
                            }
                        }
                    }
                )
            }

            composable(Routes.SUPPLY_EXIT) {
                if (!canManageSupplyInventory) {
                    LaunchedEffect(Unit) { navigateMain(Routes.SUPPLIES) }
                    return@composable
                }
                val record = storedSupplies.firstOrNull { it.supply.id == supplyMovementId }
                if (record == null || record.availableQuantity <= 0.0) {
                    LaunchedEffect(Unit) { navigateMain(Routes.SUPPLIES) }
                    return@composable
                }
                SupplyExitScreen(
                    record = record,
                    lotOptions = lots.filter { it.status == "ACTIVO" }.map { lot ->
                        SupplyLotOption(lot.id, "${lot.code} · ${lot.name}")
                    },
                    employeeOptions = supplyEmployeeOptions,
                    isSaving = supplyMovementIsSaving,
                    saveError = supplyMovementSaveError,
                    onBack = { navController.popBackStack() },
                    onSubmit = { form ->
                        coroutineScope.launch {
                            supplyMovementIsSaving = true
                            supplyMovementSaveError = null
                            runCatching {
                                supplyViewModel.registerExit(
                                    SupplyExitDraft(
                                        supplyId = record.supply.id,
                                        quantity = form.quantity.replace(',', '.').toDouble(),
                                        date = parseDate(form.exitDate)
                                            ?: error("La fecha de salida no es válida."),
                                        lotId = form.lotId.ifBlank { null },
                                        userId = currentUser?.id,
                                        responsible = form.responsible,
                                        notes = form.notes.ifBlank { null }
                                    )
                                )
                            }.onSuccess {
                                supplyMovementIsSaving = false
                                supplyMovementId = ""
                                if (!navController.popBackStack(Routes.SUPPLIES, false)) {
                                    navigateMain(Routes.SUPPLIES)
                                }
                            }.onFailure { error ->
                                supplyMovementIsSaving = false
                                supplyMovementSaveError = error.message
                                    ?: "No fue posible registrar la salida."
                            }
                        }
                    }
                )
            }

            composable(Routes.SUPPLY_ASSIGNMENTS) {
                if (!canManageSupplyInventory) {
                    LaunchedEffect(Unit) { navigateMain(Routes.SUPPLIES) }
                    return@composable
                }
                val record = storedSupplies.firstOrNull { it.supply.id == supplyMovementId }
                if (record == null ||
                    !com.fincahernandez.gestionpecuaria.data.repository.SupplyCatalog
                        .isDurable(record.supply.categoria)
                ) {
                    LaunchedEffect(Unit) { navigateMain(Routes.SUPPLIES) }
                    return@composable
                }
                val activeAssignments = storedSupplyAssignments
                    .filter { it.insumoId == record.supply.id && it.estado == "ACTIVA" }
                    .sortedByDescending { it.fechaAsignacion }
                    .map { assignment ->
                        val lot = lots.firstOrNull { it.id == assignment.loteId }
                        SupplyAssignmentUiModel(
                            id = assignment.id,
                            responsible = assignment.responsable,
                            quantity = assignment.cantidad,
                            assignmentDate = formatDate(assignment.fechaAsignacion),
                            lotLabel = lot?.let { "${it.code} · ${it.name}" },
                            notes = assignment.observaciones
                        )
                    }
                SupplyAssignmentScreen(
                    record = record,
                    activeAssignments = activeAssignments,
                    lotOptions = lots.filter { it.status == "ACTIVO" }.map { lot ->
                        SupplyLotOption(lot.id, "${lot.code} · ${lot.name}")
                    },
                    employeeOptions = supplyEmployeeOptions,
                    isSaving = supplyMovementIsSaving,
                    saveError = supplyMovementSaveError,
                    onBack = { navController.popBackStack() },
                    onAssign = { form ->
                        coroutineScope.launch {
                            supplyMovementIsSaving = true
                            supplyMovementSaveError = null
                            runCatching {
                                supplyViewModel.assignDurable(
                                    SupplyAssignmentDraft(
                                        supplyId = record.supply.id,
                                        quantity = form.quantity.replace(',', '.').toDouble(),
                                        date = parseDate(form.assignmentDate)
                                            ?: error("La fecha de asignación no es válida."),
                                        responsible = form.responsible,
                                        lotId = form.lotId.ifBlank { null },
                                        userId = currentUser?.id,
                                        notes = form.notes.ifBlank { null }
                                    )
                                )
                            }.onSuccess {
                                supplyMovementIsSaving = false
                                if (!navController.popBackStack(Routes.SUPPLIES, false)) {
                                    navigateMain(Routes.SUPPLIES)
                                }
                            }.onFailure { error ->
                                supplyMovementIsSaving = false
                                supplyMovementSaveError = error.message
                                    ?: "No fue posible registrar la asignación."
                            }
                        }
                    },
                    onReturn = { assignmentId, returnDate ->
                        coroutineScope.launch {
                            supplyMovementIsSaving = true
                            supplyMovementSaveError = null
                            runCatching {
                                supplyViewModel.returnDurable(
                                    assignmentId = assignmentId,
                                    date = parseDate(returnDate)
                                        ?: error("La fecha de devolución no es válida."),
                                    userId = currentUser?.id
                                )
                            }.onSuccess {
                                if (!navController.popBackStack(Routes.SUPPLIES, false)) {
                                    navigateMain(Routes.SUPPLIES)
                                }
                            }.onFailure { error ->
                                supplyMovementSaveError = error.message
                                    ?: "No fue posible registrar la devolución."
                            }
                            supplyMovementIsSaving = false
                        }
                    }
                )
            }

            composable(Routes.SUPPLY_DETAIL) {
                if (Routes.SUPPLIES !in allowedMainRoutes) {
                    LaunchedEffect(Unit) { navigateMain(Routes.DASHBOARD) }
                    return@composable
                }
                val record = storedSupplies.firstOrNull { it.supply.id == supplyMovementId }
                if (record == null) {
                    LaunchedEffect(Unit) { navigateMain(Routes.SUPPLIES) }
                    return@composable
                }
                val movementItems = storedSupplyMovements
                    .filter { it.insumoId == record.supply.id }
                    .groupBy { it.operacionId }
                    .map { (operationId, operationMovements) ->
                        val first = operationMovements.first()
                        val lot = lots.firstOrNull { it.id == first.loteId }
                        val registeredUser = users.firstOrNull {
                            it.id == first.registradoPorUsuarioId
                        }
                        SupplyMovementUiModel(
                            operationId = operationId,
                            type = first.tipo,
                            quantity = operationMovements.sumOf { it.cantidad },
                            date = formatDate(first.fecha),
                            totalValue = operationMovements
                                .takeIf { records -> records.any { it.costoUnitario != null } }
                                ?.sumOf { it.cantidad * (it.costoUnitario ?: 0.0) },
                            lotLabel = lot?.let { "${it.code} · ${it.name}" },
                            responsible = first.responsable,
                            registeredBy = registeredUser?.fullName,
                            notes = first.observaciones
                        )
                    }
                    .sortedByDescending { item ->
                        storedSupplyMovements.firstOrNull {
                            it.operacionId == item.operationId
                        }?.fecha ?: 0L
                    }
                SupplyDetailScreen(
                    record = record,
                    movements = movementItems,
                    canManageSupplies = canManageSupplyInventory,
                    onBack = { navController.popBackStack() },
                    onAdjustStock = {
                        supplyMovementSaveError = null
                        navController.navigate(Routes.SUPPLY_ADJUSTMENT)
                    }
                )
            }

            composable(Routes.SUPPLY_ADJUSTMENT) {
                if (!canManageSupplyInventory) {
                    LaunchedEffect(Unit) { navigateMain(Routes.SUPPLIES) }
                    return@composable
                }
                val record = storedSupplies.firstOrNull { it.supply.id == supplyMovementId }
                if (record == null) {
                    LaunchedEffect(Unit) { navigateMain(Routes.SUPPLIES) }
                    return@composable
                }
                val stockModels = storedSupplyStocks
                    .filter { it.insumoId == record.supply.id }
                    .sortedWith(
                        compareBy<com.fincahernandez.gestionpecuaria.data.local.entity.ExistenciaInsumoEntity> {
                            it.fechaVencimiento ?: Long.MAX_VALUE
                        }.thenBy { it.fechaIngreso }
                    )
                    .map { stock ->
                        SupplyStockUiModel(
                            id = stock.id,
                            initialQuantity = stock.cantidadInicial,
                            availableQuantity = stock.cantidadDisponible,
                            entryDate = formatDate(stock.fechaIngreso),
                            expirationDate = stock.fechaVencimiento?.let(::formatDate),
                            unitCost = stock.costoUnitario
                        )
                    }
                SupplyAdjustmentScreen(
                    record = record,
                    stocks = stockModels,
                    employeeOptions = supplyEmployeeOptions,
                    isSaving = supplyMovementIsSaving,
                    saveError = supplyMovementSaveError,
                    onBack = { navController.popBackStack() },
                    onSubmit = { form ->
                        coroutineScope.launch {
                            supplyMovementIsSaving = true
                            supplyMovementSaveError = null
                            runCatching {
                                val stock = storedSupplyStocks.firstOrNull {
                                    it.id == form.stockId && it.insumoId == record.supply.id
                                } ?: error("No se encontró la existencia seleccionada.")
                                val countedQuantity = form.countedQuantity
                                    .replace(',', '.')
                                    .toDouble()
                                supplyViewModel.adjustStock(
                                    SupplyAdjustmentDraft(
                                        stockId = stock.id,
                                        difference = countedQuantity - stock.cantidadDisponible,
                                        date = parseDate(form.adjustmentDate)
                                            ?: error("La fecha del conteo no es válida."),
                                        userId = currentUser?.id,
                                        responsible = form.responsible,
                                        notes = form.reason
                                    )
                                )
                            }.onSuccess {
                                supplyMovementIsSaving = false
                                navController.popBackStack()
                            }.onFailure { error ->
                                supplyMovementIsSaving = false
                                supplyMovementSaveError = error.message
                                    ?: "No fue posible guardar el ajuste."
                            }
                        }
                    }
                )
            }

            composable(Routes.EMPLOYEES) {
                EmployeeListScreen(
                    employees = employees,
                    payments = employeePayments,
                    onMenuClick = openDrawer,
                    onCreateEmployee = {
                        employeeSaveError = null
                        navController.navigate(Routes.EMPLOYEE_FORM)
                    },
                    onEmployeeClick = { employeeId ->
                        selectedEmployeeId = employeeId
                        navController.navigate(Routes.EMPLOYEE_DETAIL)
                    },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.EMPLOYEE_FORM) {
                EmployeeFormScreen(
                    sectorOptions = parcels.filter { it.status != "INACTIVA" }.map { it.name },
                    isSaving = employeeIsSaving,
                    saveError = employeeSaveError,
                    onBack = { navController.popBackStack() },
                    onSubmit = { form ->
                        coroutineScope.launch {
                            employeeIsSaving = true
                            employeeSaveError = null
                            runCatching {
                                employeeViewModel.saveEmployee(
                                    EmployeeDraft(
                                        fullName = form.fullName,
                                        role = form.role,
                                        hireDate = parseDate(form.hireDate)
                                            ?: error("La fecha de ingreso no es válida."),
                                        salary = form.salary.replace(',', '.').toDouble(),
                                        paymentFrequency = form.paymentFrequency,
                                        assignedSector = form.assignedSector.ifBlank { null },
                                        phone = form.phone.ifBlank { null },
                                        active = form.active
                                    )
                                )
                            }.onSuccess { employeeId ->
                                selectedEmployeeId = employeeId
                                if (!navController.popBackStack(Routes.EMPLOYEES, false)) {
                                    navigateMain(Routes.EMPLOYEES)
                                }
                            }.onFailure { error ->
                                employeeSaveError = error.message ?: "No se pudo guardar el empleado."
                            }
                            employeeIsSaving = false
                        }
                    }
                )
            }

            composable(Routes.EMPLOYEE_DETAIL) {
                val employee = selectedEmployee
                if (employee == null) {
                    LaunchedEffect(Unit) { navigateMain(Routes.EMPLOYEES) }
                    return@composable
                }
                EmployeeDetailScreen(
                    employee = employee,
                    payments = employeePayments.filter { it.employeeId == employee.id },
                    onBack = { navController.popBackStack() },
                    onRegisterPayment = {
                        paymentSaveError = null
                        navController.navigate(Routes.EMPLOYEE_PAYMENT_FORM)
                    },
                    onNavigateMain = navigateMain
                )
            }

            composable(Routes.EMPLOYEE_PAYMENT_FORM) {
                val employee = selectedEmployee
                if (employee == null) {
                    LaunchedEffect(Unit) { navigateMain(Routes.EMPLOYEES) }
                    return@composable
                }
                EmployeePaymentFormScreen(
                    employee = employee,
                    lotOptions = lots.map { EmployeePaymentLotOption(it.id, "${it.code} · ${it.name}") },
                    isSaving = paymentIsSaving,
                    saveError = paymentSaveError,
                    onBack = { navController.popBackStack() },
                    onSubmit = { form ->
                        coroutineScope.launch {
                            paymentIsSaving = true
                            paymentSaveError = null
                            runCatching {
                                employeeViewModel.savePayment(
                                    EmployeePaymentDraft(
                                        employeeId = employee.id,
                                        paymentDate = parseDate(form.paymentDate)
                                            ?: error("La fecha de pago no es válida."),
                                        amount = form.amount.replace(',', '.').toDouble(),
                                        period = form.period,
                                        lotId = form.lotId.ifBlank { null },
                                        activity = form.activity.ifBlank { null },
                                        notes = form.notes.ifBlank { null }
                                    )
                                )
                            }.onSuccess {
                                navController.popBackStack()
                            }.onFailure { error ->
                                paymentSaveError = error.message ?: "No se pudo guardar el pago."
                            }
                            paymentIsSaving = false
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
                        },
                        records = reportRecords
                    ),
                    allowedReportIds = allowedReportIds,
                    isGeneralAdministrator = canImportData,
                    onBulkExport = { uri, modules ->
                        check(canEditRecords) {
                            "Solo el Administrador General puede exportar datos operativos."
                        }
                        bulkDataImportViewModel.export(uri, modules)
                    },
                    onInspectBulkImport = { uri, modules, mode ->
                        check(canImportData) {
                            "Solo el Administrador General puede importar datos."
                        }
                        bulkDataImportViewModel.inspect(uri, modules, mode)
                    },
                    onBulkImport = { uri, modules, mode ->
                        check(canImportData) {
                            "Solo el Administrador General puede importar datos."
                        }
                        bulkDataImportViewModel.import(uri, modules, mode)
                    },
                    onMenuClick = openDrawer,
                    onNavigateMain = navigateMain
                )
            }


            composable(Routes.USERS) {
                LaunchedEffect(currentUserId, cloudUsersRefresh) {
                    cloudUserStatus = "Cargando cuentas de la nube…"
                    try {
                        cloudUsers = cloudSessionManager.listCloudUsers(currentUserId).map { u ->
                            UserUiModel(
                                id = u.id,
                                fullName = u.name,
                                username = u.email,
                                roleName = u.role,
                                active = u.active,
                                permissionIds = u.permissions,
                                permissionCount = u.permissions.size,
                                hasCustomPermissions = u.customized,
                                isCurrentAccount = u.currentAccount
                            )
                        }
                        cloudUserStatus = "Lista actualizada desde Supabase."
                    } catch(e:kotlinx.coroutines.CancellationException) { throw e }
                    catch(e:Exception) { cloudUserStatus = e.message ?: "No se pudo cargar la lista. Revisa internet." }
                }
                UserManagementScreen(
                    users = cloudUsers,
                    canManageUsers = canEditRecords && "users" in currentPermissionIds,
                    cloudMode = true, cloudStatus = cloudUserStatus,
                    onRefresh = { cloudUsersRefresh++ },
                    onMenuClick = openDrawer,
                    onCreateUser = { userEditingId="";userSaveError=null;navController.navigate(Routes.USER_FORM) },
                    onEditUser = { userId ->
                        userEditingId = userId
                        userSaveError = null
                        navController.navigate(Routes.USER_FORM)
                    },
                    onViewRolePermissions = { navController.navigate(Routes.ROLE_PERMISSIONS) },
                    onNavigateMain = navigateMain
                )
            }
            composable(Routes.USER_FORM) {
                val editingCloudUser = cloudUsers.firstOrNull { it.id == userEditingId }
                LaunchedEffect(currentUserId, userEditingId) {
                    if (userEditingId.isNotBlank() && editingCloudUser == null) {
                        try {
                            cloudUsers = cloudSessionManager.listCloudUsers(currentUserId).map { user ->
                                UserUiModel(
                                    id = user.id,
                                    fullName = user.name,
                                    username = user.email,
                                    roleName = user.role,
                                    active = user.active,
                                    permissionIds = user.permissions,
                                    permissionCount = user.permissions.size,
                                    hasCustomPermissions = user.customized,
                                    isCurrentAccount = user.currentAccount
                                )
                            }
                            if (cloudUsers.none { it.id == userEditingId }) {
                                cloudUserStatus = "La cuenta que intentabas editar ya no existe."
                                userEditingId = ""
                                navController.popBackStack()
                            }
                        } catch (error: kotlinx.coroutines.CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            cloudUserStatus = error.message ?: "No se pudo cargar la cuenta para editar."
                            userEditingId = ""
                            navController.popBackStack()
                        }
                    }
                }
                if (userEditingId.isNotBlank() && editingCloudUser == null) {
                    androidx.compose.material3.Text("Cargando cuenta…")
                } else UserFormScreen(
                    existingUsernames = cloudUsers.map {it.username}.toSet(), cloudMode = true,
                    initialUser = editingCloudUser,
                    protectOwnAccount = editingCloudUser?.isCurrentAccount == true,
                    isSaving = userIsSaving, saveError = userSaveError,
                    onBack = {
                        if(!userIsSaving) {
                            userEditingId = ""
                            userSaveError = null
                            navController.popBackStack()
                        }
                    },
                    onSubmit = { form ->
                        userIsSaving=true;userSaveError=null
                        coroutineScope.launch {
                            try {
                                if (editingCloudUser == null) {
                                    cloudSessionManager.createCloudUser(currentUserId,
                                        com.fincahernandez.gestionpecuaria.data.remote.CloudUserDraft(
                                            form.fullName,form.username,form.temporaryPassword,form.roleName,form.active,form.permissionIds))
                                } else {
                                    val updatedUser = cloudSessionManager.updateCloudUser(
                                        currentUserId,
                                        com.fincahernandez.gestionpecuaria.data.remote.CloudUserUpdateDraft(
                                            id = editingCloudUser.id,
                                            name = form.fullName,
                                            role = form.roleName,
                                            active = form.active,
                                            permissions = form.permissionIds
                                        )
                                    )
                                    if (updatedUser.currentAccount) {
                                        unifiedAuth.remembered(currentUserId)?.let(updateCurrentUser)
                                    }
                                }
                                cloudUsersRefresh++
                                userEditingId = ""
                                navController.popBackStack()
                            } catch(e:kotlinx.coroutines.CancellationException) {throw e}
                            catch(e:Exception) {
                                userSaveError = if(e is IllegalArgumentException || e is com.fincahernandez.gestionpecuaria.data.remote.CloudAccessDenied) e.message
                                    else if (editingCloudUser == null) {
                                        "No se pudo confirmar el alta. Actualiza Usuarios antes de reintentar y revisa internet."
                                    } else {
                                        "No se pudo confirmar la edición. Actualiza Usuarios y vuelve a intentarlo."
                                    }
                            } finally {userIsSaving=false}
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

/**
 * Conserva una medición base anterior al ingreso y todos los pesajes realizados
 * durante cada permanencia en el lote. Así no mezcla el aumento de otros lotes.
 */
internal fun lotWeighingsWithinAssignments(
    assignments: List<LoteAnimalEntity>,
    weighings: List<PesajeEntity>
): List<PesajeEntity> = assignments
    .flatMap { assignment ->
        val animalWeighings = weighings
            .filter { it.animalId == assignment.animalId }
            .sortedBy { it.fechaPesaje }
        val baseline = animalWeighings.lastOrNull {
            it.fechaPesaje <= assignment.fechaIngreso
        }
        val duringAssignment = animalWeighings.filter { weighing ->
            weighing.fechaPesaje >= assignment.fechaIngreso &&
                (assignment.fechaSalida == null || weighing.fechaPesaje <= assignment.fechaSalida)
        }
        listOfNotNull(baseline) + duringAssignment
    }
    .distinctBy { it.id }
    .sortedBy { it.fechaPesaje }

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
        if ("finance" in permissions) {
            add(Routes.FINANCE)
            // El rol financiero necesita consultar y confirmar pagos de leche.
            add(Routes.MILK_PRODUCTION)
        }
        if ("supplies" in permissions) add(Routes.SUPPLIES)
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
    madreId = madreId,
    proximaParto = proximaParto,
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
    madreId = madreId.ifBlank { null },
    proximaParto = proximaParto,
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
    madreId = animal.madreId.orEmpty(),
    proximaParto = animal.proximaParto,
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
    madreId = madreId,
    proximaParto = proximaParto,
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
    capacity = capacity.toIntOrNull(),
    boundaryGeoJson = boundaryGeoJson
)

private fun ParcelUiModel.toFormData() = ParcelFormData(
    name = name,
    areaHectares = areaHectares.toString(),
    pastureType = pastureType,
    capacity = capacity?.toString().orEmpty(),
    boundaryGeoJson = boundaryGeoJson
)

private fun parseDate(value: String): Long? = runCatching {
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        isLenient = false
    }.parse(value)?.time
}.getOrNull()

private fun formatDate(value: Long?): String = value?.let {
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
}.orEmpty()

private fun formatInventoryMl(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)
