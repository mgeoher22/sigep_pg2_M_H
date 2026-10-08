package com.fincahernandez.gestionpecuaria.ui.screens.employees

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.fincahernandez.gestionpecuaria.ui.components.BrandedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.ui.components.AppBottomBar
import com.fincahernandez.gestionpecuaria.ui.components.CompactDateSelector
import com.fincahernandez.gestionpecuaria.ui.components.todayDateText
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import java.text.NumberFormat
import java.util.Locale

/** Empleado persistente presentado por las vistas de HU-12. */
data class EmployeeUiModel(
    val id: String,
    val fullName: String,
    val role: String,
    val hireDateMillis: Long,
    val hireDate: String,
    val salary: Double,
    val paymentFrequency: String,
    val assignedSector: String,
    val phone: String,
    val active: Boolean
)

/** Pago persistente mostrado dentro del historial de un empleado. */
data class EmployeePaymentUiModel(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val paymentDateMillis: Long,
    val paymentDate: String,
    val amount: Double,
    val period: String,
    val lotId: String? = null,
    val lotLabel: String? = null,
    val activity: String = "",
    val notes: String
)

data class EmployeePaymentLotOption(val id: String, val label: String)

/** Datos capturados y validados por el formulario de empleados. */
data class EmployeeFormData(
    val fullName: String,
    val role: String,
    val hireDate: String,
    val salary: String,
    val paymentFrequency: String,
    val assignedSector: String,
    val phone: String,
    val active: Boolean
)

/** Datos que captura el formulario de pago individual. */
data class EmployeePaymentFormData(
    val paymentDate: String,
    val amount: String,
    val period: String,
    val lotId: String,
    val activity: String,
    val notes: String
)

/** Listado de personal con búsqueda e indicadores administrativos básicos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListScreen(
    employees: List<EmployeeUiModel>,
    payments: List<EmployeePaymentUiModel>,
    onMenuClick: () -> Unit,
    onCreateEmployee: () -> Unit,
    onEmployeeClick: (String) -> Unit,
    onNavigateMain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var search by rememberSaveable { mutableStateOf("") }
    val filteredEmployees = employees.filter { employee ->
        search.isBlank() ||
            employee.fullName.contains(search, ignoreCase = true) ||
            employee.role.contains(search, ignoreCase = true)
    }
    val activeCount = employees.count { it.active }
    val totalPayroll = employees.filter { it.active }.sumOf { it.salary }
    val totalPaid = payments.sumOf { it.amount }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = {
                    Text(
                        "Gestión de Personal",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AppBottomBar(selectedRoute = Routes.EMPLOYEES, onNavigate = onNavigateMain)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateEmployee,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar empleado") },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EmployeeSummaryCard(
                        "EMPLEADOS ACTIVOS",
                        activeCount.toString(),
                        Modifier.weight(1f)
                    )
                    EmployeeSummaryCard(
                        "SALARIOS BASE",
                        formatQuetzales(totalPayroll),
                        Modifier.weight(1f)
                    )
                }
            }
            item {
                EmployeeSummaryCard(
                    "PAGOS REGISTRADOS",
                    formatQuetzales(totalPaid),
                    Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar empleado") },
                    placeholder = { Text("Nombre o cargo") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }
            item {
                Text(
                    "Personal registrado",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (filteredEmployees.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                if (search.isBlank()) "Aún no hay empleados" else "Sin resultados",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (search.isBlank()) {
                                    "Registre al primer colaborador de la finca."
                                } else {
                                    "No se encontraron empleados con esa búsqueda."
                                }
                            )
                            if (search.isBlank()) {
                                OutlinedButton(onClick = onCreateEmployee) {
                                    Text("Registrar empleado")
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredEmployees, key = { it.id }) { employee ->
                    EmployeeCard(employee, onClick = { onEmployeeClick(employee.id) })
                }
            }
            item { Spacer(modifier = Modifier.height(76.dp)) }
        }
    }
}

@Composable
private fun EmployeeSummaryCard(title: String, value: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(
                value,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Tarjeta compacta con los datos necesarios para identificar al empleado. */
@Composable
private fun EmployeeCard(employee: EmployeeUiModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(employee.fullName, fontWeight = FontWeight.Bold)
                    Text(employee.role, style = MaterialTheme.typography.bodySmall)
                }
                EmployeeStatusBadge(employee.active)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("SALARIO", style = MaterialTheme.typography.labelSmall)
                    Text(formatQuetzales(employee.salary))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("SECTOR", style = MaterialTheme.typography.labelSmall)
                    Text(employee.assignedSector.ifBlank { "Sin asignar" })
                }
            }
            Text("Ingreso: ${employee.hireDate}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EmployeeStatusBadge(active: Boolean) {
    Surface(
        color = if (active) Color(0xFFD7EAD9) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            if (active) "Activo" else "Inactivo",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = if (active) Color(0xFF175B28) else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/** Formulario de alta de empleados inspirado en el prototipo aprobado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeFormScreen(
    sectorOptions: List<String>,
    onBack: () -> Unit,
    onSubmit: (EmployeeFormData) -> Unit,
    isSaving: Boolean = false,
    saveError: String? = null,
    modifier: Modifier = Modifier
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("") }
    var hireDate by rememberSaveable { mutableStateOf(todayDateText()) }
    var salary by rememberSaveable { mutableStateOf("") }
    var paymentFrequency by rememberSaveable { mutableStateOf("Mensual") }
    var assignedSector by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(true) }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (hireDate.isBlank()) hireDate = todayDateText()
    }

    val salaryValue = salary.replace(',', '.').toDoubleOrNull()
    val salaryInvalid = salary.isBlank() || salaryValue == null || salaryValue <= 0
    val formValid = fullName.isNotBlank() && role.isNotBlank() &&
        hireDate.isNotBlank() && !salaryInvalid

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = { Text("Registrar empleado", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Ingrese la información laboral del colaborador.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            item {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre completo *") },
                    placeholder = { Text("Ej. Juan Pérez") },
                    isError = attemptedSave && fullName.isBlank(),
                    supportingText = if (attemptedSave && fullName.isBlank()) {
                        { Text("Ingrese el nombre del empleado.") }
                    } else null,
                    singleLine = true
                )
            }
            item {
                EmployeeDropdown(
                    label = "Cargo o rol *",
                    options = listOf(
                        "Administrador", "Encargado de finca", "Vaquero",
                        "Veterinario", "Ordeñador", "Mantenimiento", "Otro"
                    ),
                    value = role,
                    onSelected = { role = it },
                    showError = attemptedSave && role.isBlank()
                )
            }
            item {
                CompactDateSelector(
                    label = "Fecha de ingreso *",
                    value = hireDate,
                    onDateSelected = { hireDate = it },
                    showError = attemptedSave && hireDate.isBlank()
                )
            }
            item {
                OutlinedTextField(
                    value = salary,
                    onValueChange = { salary = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Salario base *") },
                    prefix = { Text("Q ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = attemptedSave && salaryInvalid,
                    supportingText = if (attemptedSave && salaryInvalid) {
                        { Text("Ingrese un salario numérico mayor que cero.") }
                    } else null,
                    singleLine = true
                )
            }
            item {
                EmployeeDropdown(
                    label = "Frecuencia de pago",
                    options = listOf("Semanal", "Quincenal", "Mensual"),
                    value = paymentFrequency,
                    onSelected = { paymentFrequency = it },
                    showError = false
                )
            }
            item {
                EmployeeDropdown(
                    label = "Sector asignado",
                    options = listOf("Administración", "General") + sectorOptions,
                    value = assignedSector,
                    onSelected = { assignedSector = it },
                    showError = false
                )
            }
            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Teléfono") },
                    placeholder = { Text("Ej. 5555 5555") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Estado del empleado", fontWeight = FontWeight.Bold)
                            Text(if (active) "Activo" else "Inactivo")
                        }
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            }
            item {
                saveError?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                Button(
                    onClick = {
                        attemptedSave = true
                        if (formValid) {
                            onSubmit(
                                EmployeeFormData(
                                    fullName = fullName.trim(),
                                    role = role,
                                    hireDate = hireDate,
                                    salary = salary,
                                    paymentFrequency = paymentFrequency,
                                    assignedSector = assignedSector,
                                    phone = phone.trim(),
                                    active = active
                                )
                            )
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSaving) "Guardando…" else "Guardar empleado", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Perfil laboral con historial de pagos reales y acceso al nuevo pago. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDetailScreen(
    employee: EmployeeUiModel,
    payments: List<EmployeePaymentUiModel>,
    onBack: () -> Unit,
    onRegisterPayment: () -> Unit,
    onNavigateMain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val orderedPayments = payments.sortedByDescending { it.paymentDateMillis }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = { Text("Detalle del empleado", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        },
        bottomBar = {
            AppBottomBar(selectedRoute = Routes.EMPLOYEES, onNavigate = onNavigateMain)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRegisterPayment,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar pago") },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(employee.fullName, style = MaterialTheme.typography.headlineSmall)
                        Text(employee.role, fontWeight = FontWeight.Bold)
                        Text("Salario base: ${formatQuetzales(employee.salary)}")
                        Text("Frecuencia: ${employee.paymentFrequency}")
                        Text("Ingreso: ${employee.hireDate}")
                        Text("Sector: ${employee.assignedSector.ifBlank { "Sin asignar" }}")
                        employee.phone.takeIf(String::isNotBlank)?.let { Text("Teléfono: $it") }
                        EmployeeStatusBadge(employee.active)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EmployeeSummaryCard(
                        "PAGOS",
                        orderedPayments.size.toString(),
                        Modifier.weight(1f)
                    )
                    EmployeeSummaryCard(
                        "TOTAL PAGADO",
                        formatQuetzales(orderedPayments.sumOf { it.amount }),
                        Modifier.weight(1f)
                    )
                }
            }
            item {
                Text("Historial de pagos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (orderedPayments.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text("Aún no hay pagos registrados para este empleado.", Modifier.padding(18.dp))
                    }
                }
            } else {
                items(orderedPayments, key = { it.id }) { payment ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(payment.period, fontWeight = FontWeight.Bold)
                                Text(formatQuetzales(payment.amount), color = MaterialTheme.colorScheme.primary)
                            }
                            Text(payment.paymentDate, style = MaterialTheme.typography.bodySmall)
                            payment.lotLabel?.let { Text("Lote: $it", style = MaterialTheme.typography.bodySmall) }
                            if (payment.activity.isNotBlank()) Text("Actividad: ${payment.activity}")
                            if (payment.notes.isNotBlank()) Text(payment.notes)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(76.dp)) }
        }
    }
}

/** Captura un pago que también aparecerá automáticamente como egreso financiero. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeePaymentFormScreen(
    employee: EmployeeUiModel,
    lotOptions: List<EmployeePaymentLotOption>,
    onBack: () -> Unit,
    onSubmit: (EmployeePaymentFormData) -> Unit,
    isSaving: Boolean = false,
    saveError: String? = null,
    modifier: Modifier = Modifier
) {
    var paymentDate by rememberSaveable { mutableStateOf(todayDateText()) }
    var amount by rememberSaveable(employee.id) { mutableStateOf(employee.salary.toString()) }
    var period by rememberSaveable { mutableStateOf("") }
    var lotId by rememberSaveable { mutableStateOf("") }
    var activity by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(employee.id) {
        if (paymentDate.isBlank()) paymentDate = todayDateText()
    }
    val amountValue = amount.replace(',', '.').toDoubleOrNull()
    val amountInvalid = amount.isBlank() || amountValue == null || amountValue <= 0.0
    val formValid = paymentDate.isNotBlank() && period.isNotBlank() && !amountInvalid

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = { Text("Registrar pago", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(employee.fullName, fontWeight = FontWeight.Bold)
                        Text("${employee.role} · ${employee.paymentFrequency}")
                    }
                }
            }
            item {
                CompactDateSelector(
                    label = "Fecha de pago *",
                    value = paymentDate,
                    onDateSelected = { paymentDate = it },
                    showError = attemptedSave && paymentDate.isBlank()
                )
            }
            item {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Monto pagado *") },
                    prefix = { Text("Q ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = attemptedSave && amountInvalid,
                    supportingText = if (attemptedSave && amountInvalid) {
                        { Text("Ingrese un monto mayor que cero.") }
                    } else null,
                    singleLine = true
                )
            }
            item {
                EmployeePaymentLotDropdown(
                    options = lotOptions,
                    selectedId = lotId,
                    onSelected = { lotId = it }
                )
            }
            item {
                OutlinedTextField(
                    value = activity,
                    onValueChange = { activity = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Actividad realizada") },
                    placeholder = { Text("Ej. Alimentación, vacunación o reparación de cercas") },
                    supportingText = { Text("Opcional; ayuda a identificar en qué se utilizó la mano de obra.") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = period,
                    onValueChange = { period = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Período pagado *") },
                    placeholder = { Text("Ej. Septiembre 2026 o Semana 36") },
                    isError = attemptedSave && period.isBlank(),
                    supportingText = if (attemptedSave && period.isBlank()) {
                        { Text("Indique a qué período corresponde el pago.") }
                    } else null,
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Observaciones") },
                    minLines = 3
                )
            }
            item {
                saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Button(
                    onClick = {
                        attemptedSave = true
                        if (formValid) {
                            onSubmit(
                                EmployeePaymentFormData(
                                    paymentDate = paymentDate,
                                    amount = amount,
                                    period = period.trim(),
                                    lotId = lotId,
                                    activity = activity.trim(),
                                    notes = notes.trim()
                                )
                            )
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSaving) "Guardando…" else "Guardar pago", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmployeePaymentLotDropdown(
    options: List<EmployeePaymentLotOption>,
    selectedId: String,
    onSelected: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.id == selectedId }?.label.orEmpty()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            readOnly = true,
            label = { Text("Lote de trabajo") },
            placeholder = { Text("Pago general, sin lote") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            supportingText = { Text("Si selecciona un lote, el pago se sumará a su costo de mano de obra.") }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Sin lote") }, onClick = {
                onSelected("")
                expanded = false
            })
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.label) }, onClick = {
                    onSelected(option.id)
                    expanded = false
                })
            }
        }
    }
}

/** Selector reutilizado para cargo, frecuencia de pago y sector. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmployeeDropdown(
    label: String,
    options: List<String>,
    value: String,
    onSelected: (String) -> Unit,
    showError: Boolean
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (options.isNotEmpty()) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Seleccionar") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            isError = showError,
            supportingText = if (showError) ({ Text("Seleccione una opción.") }) else null
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.distinct().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatQuetzales(value: Double): String =
    "Q ${NumberFormat.getNumberInstance(Locale.US).format(value)}"
