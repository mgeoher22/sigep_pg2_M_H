package com.fincahernandez.gestionpecuaria.ui.screens.finance

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.fincahernandez.gestionpecuaria.ui.components.BrandedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.ui.components.AppBottomBar
import com.fincahernandez.gestionpecuaria.ui.components.CompactDateSelector
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import java.text.NumberFormat
import java.util.Locale

/** Movimiento persistente o derivado de otro módulo con su fuente visible. */
data class FinancialMovementUiModel(
    val id: String,
    val type: String,
    val category: String,
    val amount: Double,
    val dateMillis: Long,
    val date: String,
    val notes: String,
    val sourceLabel: String = "Registro manual",
    val automatic: Boolean = false,
    val deletableRecordId: String? = null
)

/** Datos validados que el formulario entrega al contenedor de navegación. */
data class FinancialMovementFormData(
    val type: String,
    val category: String,
    val amount: String,
    val date: String,
    val notes: String
)

/** Panel financiero alimentado por Room y por registros trazables de otros módulos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceListScreen(
    movements: List<FinancialMovementUiModel>,
    onMenuClick: () -> Unit,
    onCreateMovement: () -> Unit,
    onNavigateMain: (String) -> Unit,
    canDeleteMovements: Boolean = false,
    isDeletingMovement: Boolean = false,
    deleteError: String? = null,
    onClearDeleteError: () -> Unit = {},
    onDeleteMovement: (movementId: String, password: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val income = movements.filter { it.type == "INGRESO" }.sumOf { it.amount }
    val expenses = movements.filter { it.type == "EGRESO" }.sumOf { it.amount }
    val balance = income - expenses
    val balanceSignal = financeBalanceSignal(income, expenses)
    var pendingDeletion by remember { mutableStateOf<FinancialMovementUiModel?>(null) }
    var confirmationPassword by remember { mutableStateOf("") }

    // Cuando Room deja de emitir el movimiento eliminado se cierra la confirmación.
    LaunchedEffect(movements, pendingDeletion?.id) {
        val pending = pendingDeletion ?: return@LaunchedEffect
        if (movements.none { it.id == pending.id }) {
            pendingDeletion = null
            confirmationPassword = ""
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = {
                    Text(
                        "Gestión Financiera",
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
            AppBottomBar(selectedRoute = Routes.FINANCE, onNavigate = onNavigateMain)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateMovement,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuevo movimiento") },
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = balanceSignal.color)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "BALANCE ACTUAL · ${balanceSignal.label}",
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            formatQuetzales(balance),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(balanceSignal.description, color = Color.White)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FinanceSummaryCard(
                        title = "INGRESOS",
                        value = formatQuetzales(income),
                        valueColor = Color(0xFF0B6B2A),
                        modifier = Modifier.weight(1f)
                    )
                    FinanceSummaryCard(
                        title = "EGRESOS",
                        value = formatQuetzales(expenses),
                        valueColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Text(
                    "Movimientos recientes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (movements.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Aún no hay movimientos", fontWeight = FontWeight.Bold)
                            Text("Registre el primer ingreso o egreso de la finca.")
                            OutlinedButton(onClick = onCreateMovement) {
                                Text("Registrar movimiento")
                            }
                        }
                    }
                }
            } else {
                items(movements.sortedByDescending { it.dateMillis }, key = { it.id }) { movement ->
                    FinancialMovementCard(
                        movement = movement,
                        canDelete = canDeleteMovements && movement.deletableRecordId != null,
                        onDelete = {
                            confirmationPassword = ""
                            onClearDeleteError()
                            pendingDeletion = movement
                        }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(76.dp)) }
        }
    }

    pendingDeletion?.let { movement ->
        AlertDialog(
            onDismissRequest = {
                if (!isDeletingMovement) {
                    confirmationPassword = ""
                    pendingDeletion = null
                    onClearDeleteError()
                }
            },
            title = { Text("Eliminar movimiento contable") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Se eliminará ${movement.category} por ${formatQuetzales(movement.amount)}. " +
                            "Esta acción requiere la contraseña del Administrador General."
                    )
                    OutlinedTextField(
                        value = confirmationPassword,
                        onValueChange = {
                            confirmationPassword = it
                            onClearDeleteError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Contraseña actual") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !isDeletingMovement,
                        singleLine = true
                    )
                    deleteError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeletingMovement,
                    onClick = {
                        confirmationPassword = ""
                        pendingDeletion = null
                        onClearDeleteError()
                    }
                ) { Text("Cancelar") }
            },
            confirmButton = {
                Button(
                    enabled = confirmationPassword.isNotBlank() && !isDeletingMovement,
                    onClick = {
                        val password = confirmationPassword
                        confirmationPassword = ""
                        movement.deletableRecordId?.let { id -> onDeleteMovement(id, password) }
                    }
                ) {
                    Text(if (isDeletingMovement) "Verificando…" else "Confirmar y eliminar")
                }
            }
        )
    }
}

private data class FinanceBalanceSignal(
    val label: String,
    val description: String,
    val color: Color
)

/** El margen disponible se evalúa respecto de los ingresos, no con montos fijos. */
private fun financeBalanceSignal(income: Double, expenses: Double): FinanceBalanceSignal {
    val balance = income - expenses
    return when {
        income == 0.0 && expenses == 0.0 -> FinanceBalanceSignal(
            "SIN DATOS",
            "Registre ingresos y egresos para evaluar el balance.",
            Color(0xFF5F6368)
        )
        balance < 0.0 -> FinanceBalanceSignal(
            "CRÍTICO",
            "Los egresos superan los ingresos registrados.",
            Color(0xFFB3261E)
        )
        income > 0.0 && balance / income < 0.25 -> FinanceBalanceSignal(
            "POCO",
            "Queda menos del 25 % de los ingresos disponibles.",
            Color(0xFF9A6700)
        )
        else -> FinanceBalanceSignal(
            "BASTANTE",
            "El margen disponible es al menos el 25 % de los ingresos.",
            Color(0xFF147A36)
        )
    }
}

@Composable
private fun FinanceSummaryCard(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(value, color = valueColor, fontWeight = FontWeight.Bold)
        }
    }
}

/** Tarjeta que diferencia visualmente ingresos y egresos. */
@Composable
private fun FinancialMovementCard(
    movement: FinancialMovementUiModel,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    val isIncome = movement.type == "INGRESO"
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(movement.category, fontWeight = FontWeight.Bold)
                Text(movement.date, style = MaterialTheme.typography.bodySmall)
                Text(
                    movement.sourceLabel,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall
                )
                if (movement.notes.isNotBlank()) {
                    Text(movement.notes, style = MaterialTheme.typography.bodySmall)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (isIncome) "+" else "-") + formatQuetzales(movement.amount),
                    color = if (isIncome) Color(0xFF0B6B2A) else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar movimiento",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/** Formulario de ingresos y egresos conforme a los campos exigidos por HU-06. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialMovementFormScreen(
    onBack: () -> Unit,
    onSubmit: (FinancialMovementFormData) -> Unit,
    isSaving: Boolean = false,
    saveError: String? = null,
    modifier: Modifier = Modifier
) {
    var type by rememberSaveable { mutableStateOf("INGRESO") }
    var category by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }

    val categories = if (type == "INGRESO") {
        // La venta diaria de leche entra automáticamente desde Producción Lechera.
        listOf("Venta de ganado", "Otros ingresos")
    } else {
        // Los salarios pagados entran automáticamente desde el historial del empleado.
        listOf("Alimentación", "Salud animal", "Otros gastos de personal", "Insumos", "Mantenimiento", "Otros egresos")
    }
    val amountValue = amount.replace(',', '.').toDoubleOrNull()
    val amountInvalid = amount.isBlank() || amountValue == null || amountValue <= 0
    val formValid = category.isNotBlank() && !amountInvalid && date.isNotBlank()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = { Text("Nuevo movimiento", fontWeight = FontWeight.Bold) },
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
                    "Registre un ingreso o egreso financiero de la finca.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            item {
                Text("Tipo de movimiento", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("INGRESO", "EGRESO").forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = {
                                type = option
                                // Evita conservar una categoría que no pertenece al nuevo tipo.
                                category = ""
                            },
                            label = { Text(option.lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            item {
                CategoryDropdown(
                    options = categories,
                    value = category,
                    onSelected = { category = it },
                    showError = attemptedSave && category.isBlank()
                )
            }
            item {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Monto *") },
                    prefix = { Text("Q ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = attemptedSave && amountInvalid,
                    supportingText = if (attemptedSave && amountInvalid) {
                        { Text("Ingrese un monto numérico mayor que cero.") }
                    } else null,
                    singleLine = true
                )
            }
            item {
                CompactDateSelector(
                    label = "Fecha *",
                    value = date,
                    onDateSelected = { date = it },
                    showError = attemptedSave && date.isBlank()
                )
            }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notas adicionales") },
                    placeholder = { Text("Detalle o motivo del movimiento") },
                    minLines = 4
                )
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
                                FinancialMovementFormData(
                                    type = type,
                                    category = category,
                                    amount = amount,
                                    date = date,
                                    notes = notes.trim()
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
                    Text(if (isSaving) "Guardando…" else "Guardar movimiento", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    options: List<String>,
    value: String,
    onSelected: (String) -> Unit,
    showError: Boolean
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            readOnly = true,
            label = { Text("Categoría *") },
            placeholder = { Text("Seleccione una categoría") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            isError = showError,
            supportingText = if (showError) ({ Text("Seleccione una categoría.") }) else null
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
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
