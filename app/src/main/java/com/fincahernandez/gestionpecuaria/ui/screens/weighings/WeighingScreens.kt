package com.fincahernandez.gestionpecuaria.ui.screens.weighings

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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.fincahernandez.gestionpecuaria.ui.components.CompactDateSelector
import com.fincahernandez.gestionpecuaria.ui.components.DemoModeNotice
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import java.util.Locale

/** Opción seleccionable de animal con su peso anterior. */
data class AnimalWeightOption(
    val id: String,
    val label: String,
    val previousWeightLibras: Double?
)

/** Registro temporal visible en el panel de pesajes. */
data class WeighingUiModel(
    val id: String,
    val animalId: String,
    val animalLabel: String,
    val weightLibras: Double,
    val date: String,
    val notes: String
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
    weighings: List<WeighingUiModel>,
    onMenuClick: () -> Unit,
    onCreateWeighing: () -> Unit,
    onNavigateMain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var search by rememberSaveable { mutableStateOf("") }
    val filteredWeighings = weighings.filter { record ->
        search.isBlank() || record.animalLabel.contains(search, ignoreCase = true)
    }
    val averageWeight = weighings.map { it.weightLibras }.average().takeUnless { it.isNaN() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
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
                text = { Text("Registrar pesaje") }
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
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar pesaje") },
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
                        title = "REGISTROS",
                        value = weighings.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item { WeighingGrowthChart(weighings) }
            item {
                Text(
                    "Pesajes recientes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (filteredWeighings.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (search.isBlank()) {
                                "Aún no hay pesajes registrados."
                            } else {
                                "No se encontraron pesajes para esa búsqueda."
                            },
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(filteredWeighings, key = { it.id }) { record ->
                    WeighingRecordCard(record)
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

/** Gráfico visual con los últimos pesos registrados. */
@Composable
private fun WeighingGrowthChart(weighings: List<WeighingUiModel>) {
    val values = weighings.takeLast(7).map { it.weightLibras }
    val maximum = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Crecimiento reciente", fontWeight = FontWeight.Bold)
            if (values.isEmpty()) {
                Text("El gráfico aparecerá después del primer pesaje.")
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    values.forEach { value ->
                        val height = (value / maximum * 115).toInt().coerceAtLeast(20)
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height(height.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeighingRecordCard(record: WeighingUiModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                Text(record.animalLabel, fontWeight = FontWeight.Bold)
                Text(record.date, style = MaterialTheme.typography.bodySmall)
                if (record.notes.isNotBlank()) {
                    Text(record.notes, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                "${oneDecimal(record.weightLibras)} lb",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Formulario individual de pesaje con teclado numérico y calendario. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeighingFormScreen(
    animalOptions: List<AnimalWeightOption>,
    onBack: () -> Unit,
    onSubmit: (WeighingFormData) -> Unit,
    modifier: Modifier = Modifier
) {
    var animalId by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }

    val selectedAnimal = animalOptions.firstOrNull { it.id == animalId }
    val weightValue = weight.toDoubleOrNull()
    val weightInvalid = weight.isBlank() || weightValue == null || weightValue <= 0
    val formValid = animalId.isNotBlank() && !weightInvalid && date.isNotBlank()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar pesaje", fontWeight = FontWeight.Bold)
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
