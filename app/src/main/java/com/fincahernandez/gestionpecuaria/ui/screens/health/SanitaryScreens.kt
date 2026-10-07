package com.fincahernandez.gestionpecuaria.ui.screens.health

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MedicalServices
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.ui.components.AppBottomBar
import com.fincahernandez.gestionpecuaria.ui.components.BrandedTopAppBar
import com.fincahernandez.gestionpecuaria.ui.components.CompactDateSelector
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lote utilizado únicamente para reducir la lista de animales seleccionables. */
data class SanitaryLotOption(
    val id: String,
    val label: String,
    val animalIds: Set<String>
)

/** Animal al que sí quedará vinculada permanentemente la ficha clínica. */
data class SanitaryAnimalOption(
    val id: String,
    val label: String,
    val sex: String
)

/** Medicina o vitamina activa con disponibilidad líquida calculada. */
data class SanitarySupplyOption(
    val id: String,
    val label: String,
    val availableMl: Double,
    val contentMlPerUnit: Double
)

/** Evento sanitario listo para mostrarse en el historial. */
data class SanitaryRecordUiModel(
    val id: String,
    val animalId: String,
    val animalLabel: String,
    val referenceLotId: String?,
    val referenceLotLabel: String?,
    val eventType: String,
    val eventDate: Long,
    val diagnosis: String,
    val medication: String?,
    val dose: String?,
    val healthStatus: String,
    val nextControlDate: Long?,
    val responsible: String?,
    val notes: String?
)

/** Datos validados que se guardan como un evento de un solo animal. */
data class SanitaryFormData(
    val animalId: String,
    val referenceLotId: String?,
    val eventType: String,
    val eventDate: String,
    val diagnosis: String,
    val supplyId: String?,
    val doseMl: String,
    val healthStatus: String,
    val nextControlDate: String,
    val responsible: String,
    val notes: String
)

/** Historial clínico general con filtros de lote y animal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanitaryControlScreen(
    records: List<SanitaryRecordUiModel>,
    lots: List<SanitaryLotOption>,
    animals: List<SanitaryAnimalOption>,
    initialAnimalId: String = "",
    onMenuClick: () -> Unit,
    onCreateRecord: () -> Unit,
    onNavigateMain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var search by rememberSaveable { mutableStateOf("") }
    var selectedLotId by rememberSaveable { mutableStateOf("") }
    var selectedAnimalId by rememberSaveable(initialAnimalId) {
        mutableStateOf(initialAnimalId)
    }

    val animalsInSelectedLot = if (selectedLotId.isBlank()) {
        animals
    } else {
        val animalIds = lots.firstOrNull { it.id == selectedLotId }?.animalIds.orEmpty()
        animals.filter { it.id in animalIds }
    }
    val filteredRecords = records.filter { record ->
        (selectedLotId.isBlank() || record.referenceLotId == selectedLotId) &&
            (selectedAnimalId.isBlank() || record.animalId == selectedAnimalId) &&
            (search.isBlank() || listOf(
                record.animalLabel,
                record.eventType,
                record.diagnosis,
                record.medication.orEmpty()
            ).any { it.contains(search, ignoreCase = true) })
    }
    val scheduledControls = records.count {
        it.nextControlDate != null && it.nextControlDate >= startOfToday()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = {
                    Text(
                        "Control sanitario",
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
            AppBottomBar(selectedRoute = Routes.SANITARY, onNavigate = onNavigateMain)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateRecord,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar evento") },
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
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.HealthAndSafety, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Registro sanitario individual", fontWeight = FontWeight.Bold)
                            Text(
                                "El lote ayuda a localizar al animal. Cada vacuna, diagnóstico " +
                                    "o tratamiento se guarda únicamente en su ficha clínica."
                            )
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SanitarySummaryCard(
                        title = "EVENTOS",
                        value = records.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    SanitarySummaryCard(
                        title = "PRÓXIMOS CONTROLES",
                        value = scheduledControls.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar en historial") },
                    placeholder = { Text("Animal, evento, diagnóstico o medicamento") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }
            item {
                SelectionDropdown(
                    label = "Filtrar por lote",
                    selectedId = selectedLotId,
                    emptyOptionLabel = "Todos los lotes",
                    options = lots.map { it.id to it.label },
                    onSelected = { lotId ->
                        selectedLotId = lotId
                        if (selectedAnimalId !in lots
                                .firstOrNull { it.id == lotId }
                                ?.animalIds
                                .orEmpty() && lotId.isNotBlank()
                        ) {
                            selectedAnimalId = ""
                        }
                    }
                )
            }
            item {
                SelectionDropdown(
                    label = "Filtrar por animal",
                    selectedId = selectedAnimalId,
                    emptyOptionLabel = "Todos los animales",
                    options = animalsInSelectedLot.map { it.id to it.label },
                    onSelected = { selectedAnimalId = it }
                )
            }
            item {
                Text(
                    "Historial sanitario",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (filteredRecords.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("No hay eventos para mostrar", fontWeight = FontWeight.Bold)
                            Text(
                                if (records.isEmpty()) {
                                    "Registre la primera vacuna, consulta o tratamiento."
                                } else {
                                    "Cambie los filtros para consultar otros registros."
                                }
                            )
                        }
                    }
                }
            } else {
                items(filteredRecords, key = { it.id }) { record ->
                    SanitaryRecordCard(record)
                }
            }
            item { Spacer(modifier = Modifier.height(76.dp)) }
        }
    }
}

@Composable
private fun SanitarySummaryCard(title: String, value: String, modifier: Modifier) {
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
private fun SanitaryRecordCard(record: SanitaryRecordUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(record.animalLabel, fontWeight = FontWeight.Bold)
                    Text(
                        "${record.eventType} · ${formatDate(record.eventDate)}",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    record.healthStatus,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(record.diagnosis)
            if (!record.medication.isNullOrBlank()) {
                Text(
                    "Medicamento: ${record.medication}" +
                        record.dose?.takeIf { it.isNotBlank() }?.let { " · Dosis: $it" }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            record.referenceLotLabel?.let {
                Text("Lote al registrar: $it", style = MaterialTheme.typography.bodySmall)
            }
            record.nextControlDate?.let {
                Text(
                    "Próximo control o refuerzo: ${formatDate(it)}",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            record.responsible?.takeIf { it.isNotBlank() }?.let {
                Text("Responsable: $it", style = MaterialTheme.typography.bodySmall)
            }
            record.notes?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Formulario que obliga a escoger un animal después de usar el lote como filtro. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanitaryRecordFormScreen(
    lots: List<SanitaryLotOption>,
    animals: List<SanitaryAnimalOption>,
    medicineSupplies: List<SanitarySupplyOption>,
    initialAnimalId: String = "",
    isSaving: Boolean,
    saveError: String?,
    onBack: () -> Unit,
    onSubmit: (SanitaryFormData) -> Unit,
    modifier: Modifier = Modifier
) {
    var lotId by rememberSaveable { mutableStateOf("") }
    var animalId by rememberSaveable(initialAnimalId) { mutableStateOf(initialAnimalId) }
    var eventType by rememberSaveable { mutableStateOf(sanitaryEventTypes.first()) }
    var eventDate by rememberSaveable { mutableStateOf("") }
    var diagnosis by rememberSaveable { mutableStateOf("") }
    var supplyId by rememberSaveable { mutableStateOf("") }
    var doseMl by rememberSaveable { mutableStateOf("") }
    var healthStatus by rememberSaveable { mutableStateOf("EXCELENTE") }
    var nextControlDate by rememberSaveable { mutableStateOf("") }
    var responsible by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }

    val filteredAnimals = if (lotId.isBlank()) {
        animals
    } else {
        val animalIds = lots.firstOrNull { it.id == lotId }?.animalIds.orEmpty()
        animals.filter { it.id in animalIds }
    }
    val selectedAnimal = animals.firstOrNull { it.id == animalId }
    val selectedSupply = medicineSupplies.firstOrNull { it.id == supplyId }
    val requiresMedication = eventType in medicationRequiredEvents
    val parsedDoseMl = doseMl.replace(',', '.').toDoubleOrNull()
    val supplyRequiredError = attemptedSave && requiresMedication && supplyId.isBlank()
    val doseRequired = requiresMedication || supplyId.isNotBlank()
    val doseError = attemptedSave && doseRequired && (
        parsedDoseMl == null || !parsedDoseMl.isFinite() || parsedDoseMl <= 0.0 ||
            selectedSupply == null || parsedDoseMl > selectedSupply.availableMl
    )
    val formValid = animalId.isNotBlank() && eventDate.isNotBlank() &&
        diagnosis.isNotBlank() && (!requiresMedication || supplyId.isNotBlank()) &&
        (!doseRequired || !doseError && parsedDoseMl != null)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = { Text("Registrar evento sanitario", fontWeight = FontWeight.Bold) },
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
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Aplicación individual", fontWeight = FontWeight.Bold)
                        Text(
                            "Primero puede escoger un lote para reducir la lista. Después debe " +
                                "seleccionar el animal que recibió la atención."
                        )
                    }
                }
            }
            item {
                SelectionDropdown(
                    label = "Lote para localizar al animal",
                    selectedId = lotId,
                    emptyOptionLabel = "Todos los animales / sin lote",
                    options = lots.map { it.id to it.label },
                    onSelected = { selectedLotId ->
                        lotId = selectedLotId
                        val allowedIds = lots.firstOrNull { it.id == selectedLotId }
                            ?.animalIds
                            .orEmpty()
                        if (selectedLotId.isNotBlank() && animalId !in allowedIds) animalId = ""
                    }
                )
            }
            item {
                SelectionDropdown(
                    label = "Animal que recibió la atención *",
                    selectedId = animalId,
                    emptyOptionLabel = if (filteredAnimals.isEmpty()) {
                        "Este lote no tiene animales asignados"
                    } else {
                        "Seleccione un animal"
                    },
                    options = filteredAnimals.map { it.id to it.label },
                    onSelected = { animalId = it },
                    allowEmptySelection = false,
                    showError = attemptedSave && animalId.isBlank()
                )
            }
            selectedAnimal?.let { animal ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Ficha clínica seleccionada", fontWeight = FontWeight.Bold)
                                Text("${animal.label} · ${animal.sex}")
                            }
                        }
                    }
                }
            }
            item {
                SelectionDropdown(
                    label = "Tipo de evento *",
                    selectedId = eventType,
                    emptyOptionLabel = "Seleccione el evento",
                    options = sanitaryEventTypes.map { it to it },
                    onSelected = { eventType = it },
                    allowEmptySelection = false
                )
            }
            item {
                CompactDateSelector(
                    label = "Fecha de aplicación o consulta *",
                    value = eventDate,
                    onDateSelected = { eventDate = it },
                    showError = attemptedSave && eventDate.isBlank()
                )
            }
            item {
                OutlinedTextField(
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Diagnóstico o motivo *") },
                    placeholder = { Text("Motivo de la vacuna, hallazgo o diagnóstico") },
                    isError = attemptedSave && diagnosis.isBlank(),
                    supportingText = if (attemptedSave && diagnosis.isBlank()) {
                        { Text("Describa el motivo de la atención.") }
                    } else null,
                    minLines = 2
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SelectionDropdown(
                        label = if (requiresMedication) {
                            "Medicina o vitamina del inventario *"
                        } else {
                            "Medicina o vitamina del inventario (opcional)"
                        },
                        selectedId = supplyId,
                        emptyOptionLabel = if (medicineSupplies.isEmpty()) {
                            "No hay productos con contenido en ml y existencia"
                        } else {
                            "Seleccione el producto aplicado"
                        },
                        options = medicineSupplies.map { it.id to it.label },
                        onSelected = {
                            supplyId = it
                            doseMl = ""
                        },
                        allowEmptySelection = !requiresMedication,
                        showError = supplyRequiredError
                    )
                    if (supplyRequiredError) {
                        Text(
                            "Seleccione una medicina o vitamina disponible en Insumos.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    selectedSupply?.let { supply ->
                        Text(
                            "Disponible: ${formatSanitaryMl(supply.availableMl)} ml · " +
                                "${formatSanitaryMl(supply.contentMlPerUnit)} ml por presentación",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = doseMl,
                    onValueChange = { doseMl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (doseRequired) "Dosis aplicada (ml) *" else "Dosis aplicada (ml)") },
                    placeholder = { Text("Ejemplo: 5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = supplyId.isNotBlank(),
                    isError = doseError,
                    supportingText = if (doseError) {
                        {
                            Text(
                                when {
                                    parsedDoseMl == null || parsedDoseMl <= 0.0 ->
                                        "Ingrese una dosis mayor que cero."
                                    selectedSupply != null && parsedDoseMl > selectedSupply.availableMl ->
                                        "La dosis supera los ${formatSanitaryMl(selectedSupply.availableMl)} ml disponibles."
                                    else -> "Seleccione un producto disponible."
                                }
                            )
                        }
                    } else {
                        { Text("La dosis se descontará automáticamente del inventario.") }
                    },
                    singleLine = true
                )
            }
            item {
                SelectionDropdown(
                    label = "Estado general después de la atención *",
                    selectedId = healthStatus,
                    emptyOptionLabel = "Seleccione el estado",
                    options = healthStatuses.map { it to it },
                    onSelected = { healthStatus = it },
                    allowEmptySelection = false
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactDateSelector(
                        label = "Próximo control o refuerzo (opcional)",
                        value = nextControlDate,
                        onDateSelected = { nextControlDate = it }
                    )
                    if (nextControlDate.isNotBlank()) {
                        OutlinedButton(onClick = { nextControlDate = "" }) {
                            Text("Quitar próxima fecha")
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = responsible,
                    onValueChange = { responsible = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Veterinario o responsable") },
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
                                SanitaryFormData(
                                    animalId = animalId,
                                    referenceLotId = lotId.ifBlank { null },
                                    eventType = eventType,
                                    eventDate = eventDate,
                                    diagnosis = diagnosis.trim(),
                                    supplyId = supplyId.ifBlank { null },
                                    doseMl = doseMl.trim(),
                                    healthStatus = healthStatus,
                                    nextControlDate = nextControlDate,
                                    responsible = responsible.trim(),
                                    notes = notes.trim()
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isSaving
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSaving) "Guardando..." else "Guardar en ficha individual")
                }
            }
        }
    }
}

/** Selector reutilizable para lote, animal, tipo de evento y estado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionDropdown(
    label: String,
    selectedId: String,
    emptyOptionLabel: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    allowEmptySelection: Boolean = true,
    showError: Boolean = false
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (options.isNotEmpty() || allowEmptySelection) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(emptyOptionLabel) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            isError = showError
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (allowEmptySelection) {
                DropdownMenuItem(
                    text = { Text(emptyOptionLabel) },
                    onClick = {
                        onSelected("")
                        expanded = false
                    }
                )
            }
            options.forEach { (id, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        onSelected(id)
                        expanded = false
                    }
                )
            }
        }
    }
}

private val sanitaryEventTypes = listOf(
    "VACUNA",
    "TRATAMIENTO",
    "DESPARASITACIÓN",
    "DIAGNÓSTICO",
    "CONTROL GENERAL"
)

private val medicationRequiredEvents = setOf("VACUNA", "TRATAMIENTO", "DESPARASITACIÓN")

private val healthStatuses = listOf("EXCELENTE", "OBSERVACIÓN", "CRÍTICO")

private fun formatDate(value: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(value))

private fun formatSanitaryMl(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)

private fun startOfToday(): Long = java.util.Calendar.getInstance().apply {
    set(java.util.Calendar.HOUR_OF_DAY, 0)
    set(java.util.Calendar.MINUTE, 0)
    set(java.util.Calendar.SECOND, 0)
    set(java.util.Calendar.MILLISECOND, 0)
}.timeInMillis
