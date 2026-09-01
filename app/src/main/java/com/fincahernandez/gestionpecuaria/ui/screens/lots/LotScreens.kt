package com.fincahernandez.gestionpecuaria.ui.screens.lots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Scale
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
import androidx.compose.material3.TopAppBar
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
import com.fincahernandez.gestionpecuaria.ui.components.DemoModeNotice
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Modelo temporal utilizado por las vistas de lotes de HU-04. */
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
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
            item { DemoModeNotice(compact = true) }
            item { LotFinancialSummary() }
            item { LotGrowthChart() }
            item {
                Text(
                    "Lotes activos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (lots.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Aún no hay lotes", fontWeight = FontWeight.Bold)
                            Text("Crea el primer lote para agrupar los animales de la finca.")
                            OutlinedButton(onClick = onCreateLot) { Text("Crear primer lote") }
                        }
                    }
                }
            } else {
                items(lots, key = { it.id }) { lot ->
                    LotCard(lot = lot, onClick = { onLotClick(lot.id) })
                }
            }
            item { Spacer(modifier = Modifier.height(76.dp)) }
        }
    }
}

/** Indicadores financieros demostrativos tomados de la estructura del prototipo. */
@Composable
private fun LotFinancialSummary() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("RENDIMIENTO FINANCIERO", color = Color.White.copy(alpha = 0.8f))
                Text(
                    "24.8 % ROI",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("+2.3 % este mes", color = Color.White)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryValueCard("COSTOS TOTALES", "Q 142,500", Modifier.weight(1f))
            SummaryValueCard("GANANCIA EST.", "Q 187,200", Modifier.weight(1f))
        }
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

/** Gráfico visual simple que no necesita dependencias externas. */
@Composable
private fun LotGrowthChart() {
    val values = listOf(32, 42, 58, 76, 98, 118, 138)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Crecimiento de peso comparativo", fontWeight = FontWeight.Bold)
            Text("Evolución demostrativa del peso promedio", style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                values.forEachIndexed { index, height ->
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(height.dp)
                            .background(
                                if (index == values.lastIndex) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f + index * 0.07f)
                                },
                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                }
            }
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
                Text("Peso promedio: ${lot.initialAverageWeight?.let { "$it kg" } ?: "Sin dato"}")
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
    animalOptions: List<Pair<String, String>>,
    onBack: () -> Unit,
    onSubmit: (LotFormData) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("ENGORDE") }
    var parcelName by rememberSaveable { mutableStateOf("") }
    var initialWeight by rememberSaveable { mutableStateOf("") }
    var targetWeight by rememberSaveable { mutableStateOf("") }
    var exitDate by rememberSaveable { mutableStateOf("") }
    var selectedAnimalIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }

    val initialWeightInvalid = initialWeight.isNotBlank() && initialWeight.toDoubleOrNull() == null
    val targetWeightInvalid = targetWeight.isNotBlank() && targetWeight.toDoubleOrNull() == null
    val formValid = name.isNotBlank() && !initialWeightInvalid && !targetWeightInvalid

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Crear nuevo lote", fontWeight = FontWeight.Bold) },
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
                NumericLotField(
                    label = "Peso inicial promedio (kg)",
                    value = initialWeight,
                    onValueChange = { initialWeight = it },
                    showError = attemptedSave && initialWeightInvalid
                )
            }
            item {
                Text("Selección de animales", fontWeight = FontWeight.Bold)
                if (animalOptions.isEmpty()) {
                    Text("No hay animales disponibles.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        animalOptions.forEach { (id, label) ->
                            FilterChip(
                                selected = id in selectedAnimalIds,
                                onClick = {
                                    selectedAnimalIds = if (id in selectedAnimalIds) {
                                        selectedAnimalIds - id
                                    } else {
                                        selectedAnimalIds + id
                                    }
                                },
                                label = { Text(label) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            item {
                NumericLotField(
                    label = "Meta de peso final (kg)",
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
                Button(
                    onClick = {
                        attemptedSave = true
                        if (formValid) {
                            onSubmit(
                                LotFormData(
                                    name = name.trim(),
                                    type = type,
                                    parcelName = parcelName,
                                    initialAverageWeight = initialWeight,
                                    targetWeight = targetWeight,
                                    estimatedExitDate = exitDate,
                                    selectedAnimalIds = selectedAnimalIds
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar lote", fontWeight = FontWeight.Bold)
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
        supportingText = if (showError) ({ Text("Ingrese un valor numérico válido.") }) else null,
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
                            onDateSelected(
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
                            )
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
    onBack: () -> Unit,
    onRegisterWeight: () -> Unit,
    onNavigateMain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
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
                        lot.initialAverageWeight?.let { "$it kg" } ?: "Sin dato",
                        Modifier.weight(1f)
                    )
                }
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
                        Text("Meta de peso: ${lot.targetWeight?.let { "$it kg" } ?: "Sin dato"}")
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
                        Text("Animales seleccionados", fontWeight = FontWeight.Bold)
                        if (selectedAnimalLabels.isEmpty()) {
                            Text("Este lote todavía no tiene animales asignados.")
                        } else {
                            selectedAnimalLabels.forEach { Text("• $it") }
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = onRegisterWeight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.Scale, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Registrar pesaje")
                }
            }
        }
    }
}
