package com.fincahernandez.gestionpecuaria.ui.screens.lots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.fincahernandez.gestionpecuaria.ui.components.BrandedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import com.fincahernandez.gestionpecuaria.ui.components.WeightChartPoint
import com.fincahernandez.gestionpecuaria.ui.components.WeightTrendChart
import com.fincahernandez.gestionpecuaria.ui.components.formatDatePickerMillis
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import java.util.Calendar
import java.util.Locale

/** Modelo presentado por las vistas a partir de los registros persistentes de Room. */
data class LotUiModel(
    val id: String,
    val code: String,
    val name: String,
    val type: String,
    val status: String,
    val parcelName: String,
    val initialAverageWeight: Double?,
    val targetWeight: Double?,
    val estimatedExitDate: String,
    val selectedAnimalIds: List<String>
)

/** Valores entregados por el formulario al contenedor de navegación. */
data class LotFormData(
    val name: String,
    val type: String,
    val parcelName: String,
    val initialAverageWeight: String,
    val targetWeight: String,
    val estimatedExitDate: String,
    val selectedAnimalIds: List<String>
)

/** Datos mínimos que necesita el flujo de lotes para mostrar y seleccionar un animal. */
data class LotAnimalOption(
    val id: String,
    val code: String,
    val name: String?,
    val category: String,
    val weightLibras: Double?
)

/** Pesaje real asociado al lote, usado para calcular promedios y tendencias. */
data class LotWeightRecord(
    val animalId: String,
    val dateMillis: Long,
    val dateLabel: String,
    val weightPounds: Double
)

/** Panel y listado independiente de lotes activos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotListScreen(
    lots: List<LotUiModel>,
    onMenuClick: () -> Unit,
    onCreateLot: () -> Unit,
    onLotClick: (String) -> Unit,
    onNavigateMain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var statusFilter by rememberSaveable { mutableStateOf("ACTIVOS") }
    val visibleLots = lots.filter { lot ->
        when (statusFilter) {
            "ACTIVOS" -> lot.status == "ACTIVO"
            "CERRADOS" -> lot.status != "ACTIVO"
            else -> true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = {
                    Text(
                        "Gestión de Lotes",
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
            AppBottomBar(selectedRoute = Routes.LOTS, onNavigate = onNavigateMain)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateLot,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Crear lote") }
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
            item { LotOperationalSummary(lots) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Lotes registrados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ACTIVOS", "CERRADOS", "TODOS").forEach { option ->
                            FilterChip(
                                selected = statusFilter == option,
                                onClick = { statusFilter = option },
                                label = {
                                    Text(option.lowercase().replaceFirstChar { it.uppercase() })
                                }
                            )
                        }
                    }
                }
            }
            if (visibleLots.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("No hay lotes en esta categoría", fontWeight = FontWeight.Bold)
                            Text(
                                if (lots.isEmpty()) {
                                    "Crea el primer lote para agrupar los animales de la finca."
                                } else {
                                    "Cambia el filtro para consultar los demás lotes."
                                }
                            )
                            if (lots.isEmpty()) {
                                OutlinedButton(onClick = onCreateLot) { Text("Crear primer lote") }
                            }
                        }
                    }
                }
            } else {
                items(visibleLots, key = { it.id }) { lot ->
                    LotCard(lot = lot, onClick = { onLotClick(lot.id) })
                }
            }
            item { Spacer(modifier = Modifier.height(76.dp)) }
        }
    }
}

/** Resumen calculado únicamente con los lotes guardados en Room. */
@Composable
private fun LotOperationalSummary(lots: List<LotUiModel>) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryValueCard(
            "LOTES ACTIVOS",
            lots.count { it.status == "ACTIVO" }.toString(),
            Modifier.weight(1f)
        )
        SummaryValueCard(
            "ANIMALES ASIGNADOS",
            lots.filter { it.status == "ACTIVO" }
                .flatMap { it.selectedAnimalIds }
                .distinct()
                .size
                .toString(),
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryValueCard(title: String, value: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/** Tarjeta navegable con los datos operativos principales de un lote. */
@Composable
private fun LotCard(lot: LotUiModel, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(lot.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(lot.code, style = MaterialTheme.typography.bodySmall)
                }
                LotStatusBadge(lot.status)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("${lot.selectedAnimalIds.size} animales")
                Text("•")
                Text(lot.type.lowercase().replaceFirstChar { it.uppercase() })
            }
            LinearProgressIndicator(
                progress = {
                    val current = lot.initialAverageWeight ?: 0.0
                    val target = lot.targetWeight ?: 1.0
                    (current / target).toFloat().coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Peso promedio: ${formatLotWeight(lot.initialAverageWeight)}")
                Text(lot.parcelName.ifBlank { "Sin parcela" })
            }
        }
    }
}

@Composable
private fun LotStatusBadge(status: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            status.lowercase().replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/** Formulario de creación de lotes con validaciones visuales. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotFormScreen(
    parcelNames: List<String>,
    animalOptions: List<LotAnimalOption>,
    selectedAnimalIds: List<String>,
    onOpenAnimalSelector: () -> Unit,
    onBack: () -> Unit,
    onSubmit: (LotFormData) -> Unit,
    initialData: LotFormData? = null,
    isSaving: Boolean = false,
    saveError: String? = null,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable(initialData?.name) { mutableStateOf(initialData?.name.orEmpty()) }
    var type by rememberSaveable(initialData?.type) {
        mutableStateOf(initialData?.type ?: "ENGORDE")
    }
    var parcelName by rememberSaveable(initialData?.parcelName) {
        mutableStateOf(initialData?.parcelName.orEmpty())
    }
    var targetWeight by rememberSaveable(initialData?.targetWeight) {
        mutableStateOf(initialData?.targetWeight.orEmpty())
    }
    var exitDate by rememberSaveable(initialData?.estimatedExitDate) {
        mutableStateOf(initialData?.estimatedExitDate.orEmpty())
    }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }

    // El promedio se obtiene únicamente de los animales seleccionados que tienen un peso registrado.
    val selectedAnimals = animalOptions.filter { it.id in selectedAnimalIds }
    val selectedWeights = selectedAnimals.mapNotNull { it.weightLibras }
    val initialAverageWeight = selectedWeights.takeIf { it.isNotEmpty() }?.average()
    val parsedTargetWeight = targetWeight.replace(',', '.').toDoubleOrNull()
    val targetWeightInvalid = targetWeight.isNotBlank() &&
        (parsedTargetWeight == null || parsedTargetWeight <= 0.0)
    val formValid = name.isNotBlank() && !targetWeightInvalid

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = {
                    Text(
                        if (initialData == null) "Crear nuevo lote" else "Editar lote",
                        fontWeight = FontWeight.Bold
                    )
                },
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
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            item {
                Text(
                    "Configure la agrupación de animales y su meta productiva.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre del lote *") },
                    placeholder = { Text("Ej. Novillos Premium Verano") },
                    isError = attemptedSave && name.isBlank(),
                    supportingText = if (attemptedSave && name.isBlank()) {
                        { Text("Ingrese un nombre para el lote.") }
                    } else null,
                    singleLine = true
                )
            }
            item {
                Text("Tipo de lote", fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ENGORDE", "LECHERO", "REPRODUCCIÓN").forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(option.lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item {
                ParcelDropdown(
                    options = parcelNames,
                    value = parcelName,
                    onSelected = { parcelName = it }
                )
            }
            item {
                OutlinedTextField(
                    value = initialAverageWeight?.let { String.format(Locale.getDefault(), "%.1f", it) }
                        ?: "Sin datos",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    label = { Text("Peso inicial promedio (lb)") },
                    supportingText = {
                        Text(
                            when {
                                selectedAnimals.isEmpty() -> "Se calculará al seleccionar animales."
                                selectedWeights.size < selectedAnimals.size ->
                                    "Calculado con ${selectedWeights.size} de ${selectedAnimals.size} animales que tienen peso."
                                else -> "Calculado automáticamente con ${selectedWeights.size} animales."
                            }
                        )
                    },
                    singleLine = true
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Animales del lote", fontWeight = FontWeight.Bold)
                        Text(
                            if (selectedAnimalIds.isEmpty()) {
                                "Todavía no se han seleccionado animales."
                            } else {
                                "${selectedAnimalIds.size} animales seleccionados"
                            }
                        )
                        OutlinedButton(
                            onClick = onOpenAnimalSelector,
                            enabled = animalOptions.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (selectedAnimalIds.isEmpty()) {
                                    "Seleccionar animales"
                                } else {
                                    "Cambiar selección"
                                }
                            )
                        }
                        if (animalOptions.isEmpty()) {
                            Text("No hay animales disponibles para asignar.")
                        }
                    }
                }
            }
            item {
                NumericLotField(
                    label = "Meta de peso final (lb)",
                    value = targetWeight,
                    onValueChange = { targetWeight = it },
                    showError = attemptedSave && targetWeightInvalid
                )
            }
            item {
                CompactDateField(
                    label = "Fecha de salida estimada",
                    value = exitDate,
                    onDateSelected = { exitDate = it }
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
                                LotFormData(
                                    name = name.trim(),
                                    type = type,
                                    parcelName = parcelName,
                                    initialAverageWeight = initialAverageWeight?.toString().orEmpty(),
                                    targetWeight = targetWeight,
                                    estimatedExitDate = exitDate,
                                    selectedAnimalIds = selectedAnimalIds
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
                    Text(
                        if (isSaving) "Guardando…" else "Guardar lote",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun NumericLotField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    showError: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = showError,
        supportingText = if (showError) ({ Text("Ingrese un valor mayor que cero.") }) else null,
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParcelDropdown(
    options: List<String>,
    value: String,
    onSelected: (String) -> Unit
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
            label = { Text("Parcela asignada") },
            placeholder = { Text(if (options.isEmpty()) "No hay parcelas" else "Seleccione una parcela") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Sin asignar") },
                onClick = {
                    onSelected("")
                    expanded = false
                }
            )
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

/** Calendario compacto que evita quedar detrás de las barras de la tableta. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactDateField(
    label: String,
    value: String,
    onDateSelected: (String) -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val pickerState = rememberDatePickerState()

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value.ifBlank { "Seleccionar fecha" })
        }
        Icon(Icons.Default.CalendarMonth, contentDescription = "Abrir calendario")
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            modifier = Modifier.navigationBarsPadding(),
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedDateMillis != null,
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(formatDatePickerMillis(millis))
                        }
                        showDialog = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(
                state = pickerState,
                title = null,
                headline = null,
                showModeToggle = false
            )
        }
    }
}

/** Pantalla de detalle que resume configuración, animales y progreso. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotDetailScreen(
    lot: LotUiModel,
    selectedAnimalLabels: List<String>,
    weightRecords: List<LotWeightRecord>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onRegisterWeight: () -> Unit,
    onNavigateMain: (String) -> Unit,
    canEditRecords: Boolean = false,
    isSaving: Boolean = false,
    saveError: String? = null,
    modifier: Modifier = Modifier
) {
    var confirmDeactivate by rememberSaveable { mutableStateOf(false) }
    var selectedWeightPeriod by rememberSaveable { mutableStateOf("TODO") }
    val periodStart = weightPeriodStart(selectedWeightPeriod)
    val filteredWeightRecords = weightRecords.filter { record ->
        periodStart == null || record.dateMillis >= periodStart
    }
    val weightTrend = filteredWeightRecords
        .groupBy { it.dateLabel }
        .map { (date, records) ->
            records.minOf { it.dateMillis } to WeightChartPoint(
                label = date.take(5),
                valuePounds = records.map { it.weightPounds }.average()
            )
        }
        .sortedBy { it.first }
        .map { it.second }
    val currentAverageWeight = weightRecords
        .groupBy { it.animalId }
        .values
        .mapNotNull { records -> records.maxByOrNull { it.dateMillis }?.weightPounds }
        .takeIf { it.isNotEmpty() }
        ?.average()

    if (confirmDeactivate) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) confirmDeactivate = false },
            title = { Text("Cerrar lote") },
            text = {
                Text(
                    "El lote quedará cerrado y se finalizarán sus asignaciones actuales. " +
                        "El historial no se eliminará."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        confirmDeactivate = false
                        onDeactivate()
                    }
                ) { Text("Cerrar lote") }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = { confirmDeactivate = false }
                ) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = { Text("Detalle del lote", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        },
        bottomBar = {
            AppBottomBar(selectedRoute = Routes.LOTS, onNavigate = onNavigateMain)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LotStatusBadge(lot.status)
                        Text(lot.name, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Text(lot.code, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryValueCard(
                        "ANIMALES",
                        lot.selectedAnimalIds.size.toString(),
                        Modifier.weight(1f)
                    )
                    SummaryValueCard(
                        "PESO PROM.",
                        formatLotWeight(currentAverageWeight ?: lot.initialAverageWeight),
                        Modifier.weight(1f)
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Evolución del lote", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("30 DÍAS", "MES", "AÑO", "TODO").forEach { period ->
                            FilterChip(
                                selected = selectedWeightPeriod == period,
                                onClick = { selectedWeightPeriod = period },
                                label = { Text(period) }
                            )
                        }
                    }
                }
            }
            item {
                WeightTrendChart(
                    title = "Promedio histórico real",
                    subtitle = "Promedio de ${filteredWeightRecords.size} pesaje(s) individuales del período seleccionado.",
                    points = weightTrend,
                    emptyMessage = "No hay pesajes asociados a este lote durante el período."
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Configuración", fontWeight = FontWeight.Bold)
                        Text("Tipo: ${lot.type.lowercase().replaceFirstChar { it.uppercase() }}")
                        Text("Parcela: ${lot.parcelName.ifBlank { "Sin asignar" }}")
                        Text("Meta de peso: ${lot.targetWeight?.let { "$it lb" } ?: "Sin dato"}")
                        Text("Salida estimada: ${lot.estimatedExitDate.ifBlank { "Sin fecha" }}")
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            if (lot.status == "ACTIVO") "Animales asignados" else "Historial de animales",
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedAnimalLabels.isEmpty()) {
                            Text("Este lote todavía no tiene animales asignados.")
                        } else {
                            selectedAnimalLabels.forEach { Text("• $it") }
                        }
                    }
                }
            }
            item {
                saveError?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
            if (lot.status == "ACTIVO") {
                if (canEditRecords) {
                    item {
                        Button(
                            onClick = onEdit,
                            enabled = !isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Editar lote")
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = onRegisterWeight,
                        enabled = !isSaving && lot.selectedAnimalIds.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(Icons.Default.Scale, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Registrar pesaje")
                    }
                }
                if (canEditRecords) {
                    item {
                        OutlinedButton(
                            onClick = { confirmDeactivate = true },
                            enabled = !isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Icon(Icons.Default.StopCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isSaving) "Cerrando…" else "Cerrar lote")
                        }
                    }
                }
            }
        }
    }
}

private fun formatLotWeight(weight: Double?): String =
    weight?.let { "${String.format(Locale.getDefault(), "%.1f", it)} lb" } ?: "Sin dato"

/** Inicio del período elegido para filtrar la gráfica sin fabricar valores. */
private fun weightPeriodStart(period: String, now: Long = System.currentTimeMillis()): Long? {
    if (period == "TODO") return null
    return Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        when (period) {
            "30 DÍAS" -> add(Calendar.DAY_OF_YEAR, -30)
            "MES" -> set(Calendar.DAY_OF_MONTH, 1)
            "AÑO" -> set(Calendar.DAY_OF_YEAR, 1)
        }
    }.timeInMillis
}
