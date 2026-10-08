package com.fincahernandez.gestionpecuaria.ui.screens.weighings

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Scale
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.fincahernandez.gestionpecuaria.ui.components.WeightChartPoint
import com.fincahernandez.gestionpecuaria.ui.components.WeightTrendChart
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import java.util.Locale
import kotlin.math.abs

/** Opción seleccionable de animal con su peso anterior. */
data class AnimalWeightOption(
    val id: String,
    val label: String,
    val previousWeightLibras: Double?
)

/** Registro persistente acompañado por la medición anterior del mismo animal. */
data class WeighingUiModel(
    val id: String,
    val animalId: String,
    val animalLabel: String,
    val lotId: String? = null,
    val weightLibras: Double,
    val previousWeightLibras: Double? = null,
    val dateMillis: Long,
    val date: String,
    val notes: String
) {
    /** Diferencia positiva si ganó peso y negativa si perdió. */
    val differenceLibras: Double?
        get() = previousWeightLibras?.let { weightLibras - it }
}

/** Una sola fila por animal para evitar repetirlo por cada pesaje histórico. */
data class WeighingAnimalSummary(
    val animalId: String,
    val animalName: String,
    val animalCode: String,
    val currentWeightLibras: Double?,
    val lastWeighingDate: String,
    val recordCount: Int
)

/** Datos validados entregados por el formulario. */
data class WeighingFormData(
    val animalId: String,
    val weightLibras: String,
    val date: String,
    val notes: String
)

/** Panel de pesajes con indicadores y registros recientes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeighingListScreen(
    animals: List<WeighingAnimalSummary>,
    onMenuClick: () -> Unit,
    onCreateWeighing: () -> Unit,
    onAnimalClick: (String) -> Unit,
    onNavigateMain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var search by rememberSaveable { mutableStateOf("") }
    val filteredAnimals = animals.filter { animal ->
        search.isBlank() ||
            animal.animalName.contains(search, ignoreCase = true) ||
            animal.animalCode.contains(search, ignoreCase = true)
    }
    val currentWeights = animals.mapNotNull { it.currentWeightLibras }
    val averageWeight = currentWeights.average().takeUnless { it.isNaN() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = {
                    Text(
                        "Gestión de Pesajes",
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
            AppBottomBar(selectedRoute = Routes.WEIGHINGS, onNavigate = onNavigateMain)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateWeighing,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar pesaje") },
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
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar animal") },
                    placeholder = { Text("Código o nombre del animal") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WeighingSummaryCard(
                        title = "PROMEDIO",
                        value = averageWeight?.let { "${oneDecimal(it)} lb" } ?: "Sin datos",
                        modifier = Modifier.weight(1f)
                    )
                    WeighingSummaryCard(
                        title = "CON PESO",
                        value = currentWeights.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Text(
                    "Animales",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Seleccione un animal para consultar su evolución y diferencias de peso.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (filteredAnimals.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (search.isBlank()) {
                                "Aún no hay animales registrados."
                            } else {
                                "No se encontraron animales para esa búsqueda."
                            },
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(filteredAnimals, key = { it.animalId }) { animal ->
                    WeighingAnimalCard(animal = animal, onClick = { onAnimalClick(animal.animalId) })
                }
            }
            item { Spacer(modifier = Modifier.height(76.dp)) }
        }
    }
}

@Composable
private fun WeighingSummaryCard(title: String, value: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun WeighingAnimalCard(animal: WeighingAnimalSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Scale,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(animal.animalName, fontWeight = FontWeight.Bold)
                Text(animal.animalCode, style = MaterialTheme.typography.bodySmall)
                Text(
                    if (animal.recordCount == 0) "Sin pesajes registrados"
                    else "${animal.recordCount} pesaje(s) · Último: ${animal.lastWeighingDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                animal.currentWeightLibras?.let { "${oneDecimal(it)} lb" } ?: "—",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Historial y tendencia de un solo animal, sin mezclar medidas de otros animales. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeighingAnimalDetailScreen(
    animal: WeighingAnimalSummary,
    records: List<WeighingUiModel>,
    onBack: () -> Unit,
    onRegisterWeight: () -> Unit,
    onNavigateMain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val orderedRecords = records.sortedByDescending { it.dateMillis }
    val latest = orderedRecords.firstOrNull()
    val trendPoints = orderedRecords
        .asReversed()
        .map { record -> WeightChartPoint(record.date.take(5), record.weightLibras) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = { Text("Evolución de peso", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        },
        bottomBar = {
            AppBottomBar(selectedRoute = Routes.WEIGHINGS, onNavigate = onNavigateMain)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRegisterWeight,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuevo pesaje") },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
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
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(animal.animalName, style = MaterialTheme.typography.headlineSmall)
                        Text(animal.animalCode, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            latest?.let { "Peso actual: ${oneDecimal(it.weightLibras)} lb" }
                                ?: "Todavía no tiene pesajes",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            latest?.let { lastRecord ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Última comparación", fontWeight = FontWeight.Bold)
                            WeightComparisonContent(
                                previousWeight = lastRecord.previousWeightLibras,
                                currentWeight = lastRecord.weightLibras
                            )
                            Text("Fecha: ${lastRecord.date}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                WeightTrendChart(
                    title = "Tendencia real del animal",
                    subtitle = "Últimos ${trendPoints.takeLast(12).size} pesajes registrados en libras.",
                    points = trendPoints
                )
            }
            item {
                Text("Historial de pesajes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (orderedRecords.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text("Registre el primer pesaje para comenzar el historial.", Modifier.padding(18.dp))
                    }
                }
            } else {
                items(orderedRecords, key = { it.id }) { record -> WeighingRecordCard(record) }
            }
            item { Spacer(modifier = Modifier.height(76.dp)) }
        }
    }
}

@Composable
private fun WeighingRecordCard(record: WeighingUiModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Scale,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(record.animalLabel, fontWeight = FontWeight.Bold)
                    Text(record.date, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "${oneDecimal(record.weightLibras)} lb",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            WeightComparisonContent(
                previousWeight = record.previousWeightLibras,
                currentWeight = record.weightLibras
            )
            if (record.notes.isNotBlank()) {
                Text(record.notes, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Muestra los dos valores y expresa la variación sin obligar a calcularla mentalmente. */
@Composable
private fun WeightComparisonContent(previousWeight: Double?, currentWeight: Double) {
    if (previousWeight == null) {
        Text(
            "Primer pesaje del animal; todavía no existe una medición para comparar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val difference = currentWeight - previousWeight
    val comparisonText = when {
        difference > WEIGHT_COMPARISON_TOLERANCE ->
            "↑ Subió ${oneDecimal(difference)} lb"
        difference < -WEIGHT_COMPARISON_TOLERANCE ->
            "↓ Bajó ${oneDecimal(abs(difference))} lb"
        else -> "Sin cambio de peso"
    }
    val comparisonColor = when {
        difference > WEIGHT_COMPARISON_TOLERANCE -> MaterialTheme.colorScheme.primary
        difference < -WEIGHT_COMPARISON_TOLERANCE -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        "Anterior: ${oneDecimal(previousWeight)} lb   Nuevo: ${oneDecimal(currentWeight)} lb",
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        comparisonText,
        color = comparisonColor,
        fontWeight = FontWeight.Bold
    )
}

/** Formulario individual de pesaje con teclado numérico y calendario. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeighingFormScreen(
    animalOptions: List<AnimalWeightOption>,
    onBack: () -> Unit,
    onSubmit: (WeighingFormData) -> Unit,
    initialAnimalId: String = "",
    contextLotLabel: String? = null,
    isSaving: Boolean = false,
    saveError: String? = null,
    modifier: Modifier = Modifier
) {
    var animalId by rememberSaveable(initialAnimalId) { mutableStateOf(initialAnimalId) }
    var weight by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(todayDateText()) }
    var notes by rememberSaveable { mutableStateOf("") }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (date.isBlank()) date = todayDateText()
    }

    val selectedAnimal = animalOptions.firstOrNull { it.id == animalId }
    val weightValue = weight.replace(',', '.').toDoubleOrNull()
    val weightInvalid = weight.isBlank() || weightValue == null || weightValue <= 0
    val formValid = animalId.isNotBlank() && !weightInvalid && date.isNotBlank()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = { Text("Registro de pesaje", fontWeight = FontWeight.Bold) },
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
                Text("Ingrese los datos del pesaje actual.", style = MaterialTheme.typography.bodyLarge)
            }
            contextLotLabel?.let { lotLabel ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Pesaje asociado al lote", style = MaterialTheme.typography.labelMedium)
                            Text(lotLabel, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item {
                AnimalWeightDropdown(
                    options = animalOptions,
                    selectedId = animalId,
                    onSelected = { animalId = it },
                    showError = attemptedSave && animalId.isBlank()
                )
            }
            selectedAnimal?.let { animal ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Animal identificado", style = MaterialTheme.typography.labelMedium)
                            Text(animal.label, fontWeight = FontWeight.Bold)
                            Text(
                                "Peso anterior: ${animal.previousWeightLibras?.let { "${oneDecimal(it)} lb" } ?: "Sin registro"}"
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Peso actual *") },
                    suffix = { Text("lb") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = attemptedSave && weightInvalid,
                    supportingText = if (attemptedSave && weightInvalid) {
                        { Text("Ingrese un peso numérico mayor que cero.") }
                    } else null,
                    singleLine = true
                )
            }
            if (selectedAnimal != null && weightValue != null && weightValue > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Comparación del pesaje", fontWeight = FontWeight.Bold)
                            WeightComparisonContent(
                                previousWeight = selectedAnimal.previousWeightLibras,
                                currentWeight = weightValue
                            )
                        }
                    }
                }
            }
            item {
                CompactDateSelector(
                    label = "Fecha del pesaje *",
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
                    label = { Text("Notas u observaciones") },
                    placeholder = { Text("Condición corporal, salud u otra observación") },
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
                                WeighingFormData(
                                    animalId = animalId,
                                    weightLibras = weight,
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
                    Text(if (isSaving) "Guardando…" else "Guardar pesaje", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimalWeightDropdown(
    options: List<AnimalWeightOption>,
    selectedId: String,
    onSelected: (String) -> Unit,
    showError: Boolean
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.id == selectedId }?.label.orEmpty()

    Column {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (options.isNotEmpty()) expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                readOnly = true,
                label = { Text("Animal *") },
                placeholder = { Text(if (options.isEmpty()) "No hay animales" else "Seleccione un animal") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                isError = showError
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onSelected(option.id)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (showError) {
            Text(
                "Seleccione el animal que se pesó.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun oneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

/** Cambios inferiores a media décima se muestran como estables al usar una cifra decimal. */
private const val WEIGHT_COMPARISON_TOLERANCE = 0.05
