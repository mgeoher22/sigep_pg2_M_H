package com.fincahernandez.gestionpecuaria.ui.screens.parcels

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.StopCircle
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.fincahernandez.gestionpecuaria.ui.components.BrandedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.ui.components.AppBottomBar
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import com.fincahernandez.gestionpecuaria.data.geo.ImportedParcelBoundary
import com.fincahernandez.gestionpecuaria.data.geo.decodeParcelBoundary
import com.fincahernandez.gestionpecuaria.data.geo.importParcelBoundaries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Modelo visual generado a partir del inventario persistente de parcelas. */
data class ParcelUiModel(
    val id: String,
    val code: String,
    val name: String,
    val areaHectares: Double,
    val pastureType: String,
    val status: String,
    val capacity: Int?,
    val currentLot: String,
    val boundaryGeoJson: String? = null
)

/** Valores capturados al registrar una parcela. */
data class ParcelFormData(
    val name: String,
    val areaHectares: String,
    val pastureType: String,
    val capacity: String,
    val boundaryGeoJson: String? = null
)

/** Panel con mapa esquemático y listado independiente de parcelas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParcelListScreen(
    parcels: List<ParcelUiModel>,
    onMenuClick: () -> Unit,
    onCreateParcel: () -> Unit,
    onParcelClick: (String) -> Unit,
    onNavigateMain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var statusFilter by rememberSaveable { mutableStateOf("ACTIVAS") }
    val activeParcels = parcels.filter { it.status != "INACTIVA" }
    val visibleParcels = parcels.filter { parcel ->
        when (statusFilter) {
            "ACTIVAS" -> parcel.status != "INACTIVA"
            "INACTIVAS" -> parcel.status == "INACTIVA"
            else -> true
        }
    }
    val totalArea = activeParcels.sumOf { it.areaHectares }
    val restingCount = activeParcels.count { it.status == "DESCANSO" }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = {
                    Text(
                        "Gestión de Parcelas",
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
            AppBottomBar(selectedRoute = Routes.PARCELS, onNavigate = onNavigateMain)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateParcel,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar parcela") }
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
                    ParcelSummaryCard(
                        title = "ÁREA TOTAL",
                        value = "${formatArea(totalArea)} ha",
                        modifier = Modifier.weight(1f)
                    )
                    ParcelSummaryCard(
                        title = "EN DESCANSO",
                        value = restingCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item { ParcelMap(activeParcels) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Parcelas registradas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ACTIVAS", "INACTIVAS", "TODAS").forEach { option ->
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
            if (visibleParcels.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("No hay parcelas en esta categoría", fontWeight = FontWeight.Bold)
                            Text(
                                if (parcels.isEmpty()) {
                                    "Registra el primer terreno productivo de la finca."
                                } else {
                                    "Cambia el filtro para consultar las demás parcelas."
                                }
                            )
                            if (parcels.isEmpty()) {
                                OutlinedButton(onClick = onCreateParcel) {
                                    Text("Registrar primera parcela")
                                }
                            }
                        }
                    }
                }
            } else {
                items(visibleParcels, key = { it.id }) { parcel ->
                    ParcelCard(parcel = parcel, onClick = { onParcelClick(parcel.id) })
                }
            }
            item { Spacer(modifier = Modifier.height(76.dp)) }
        }
    }
}

@Composable
private fun ParcelSummaryCard(title: String, value: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/** Mapa esquemático: cada bloque representa una parcela y su estado. */
@Composable
private fun ParcelMap(parcels: List<ParcelUiModel>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Landscape, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mapa de parcelas", style = MaterialTheme.typography.titleMedium)
            }
            if (parcels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sin parcelas para representar")
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    parcels.chunked(3).forEach { rowParcels ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowParcels.forEach { parcel ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(72.dp)
                                        .background(
                                            parcelStatusColor(parcel.status),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        parcel.code,
                                        color = parcelStatusContentColor(parcel.status),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            repeat(3 - rowParcels.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParcelCard(parcel: ParcelUiModel, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            parcel.boundaryGeoJson?.let { boundary ->
                ParcelSatelliteMap(
                    boundaryGeoJson = boundary,
                    modifier = Modifier.fillMaxWidth().height(138.dp),
                    liteMode = true,
                    onMapClick = onClick
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(parcel.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${parcel.code} • ${formatArea(parcel.areaHectares)} ha")
                }
                ParcelStatusBadge(parcel.status)
            }
            Text("Pastura: ${parcel.pastureType}")
            Text("Lote actual: ${parcel.currentLot.ifBlank { "Ninguno" }}")
        }
    }
}

@Composable
private fun ParcelStatusBadge(status: String) {
    Surface(
        color = parcelStatusColor(status),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            status.lowercase().replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = parcelStatusContentColor(status),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun parcelStatusColor(status: String): Color = when (status) {
    "OCUPADA" -> Color(0xFF64B5F6)
    "DESCANSO", "INACTIVA" -> Color(0xFFD7D7D7)
    else -> Color(0xFF1B6E2A)
}

private fun parcelStatusContentColor(status: String): Color = when (status) {
    "DESCANSO", "INACTIVA" -> Color(0xFF303030)
    else -> Color.White
}

/** Formulario de alta de parcelas basado en la estructura del prototipo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParcelFormScreen(
    onBack: () -> Unit,
    onSubmit: (ParcelFormData) -> Unit,
    initialData: ParcelFormData? = null,
    isSaving: Boolean = false,
    saveError: String? = null,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable(initialData?.name) { mutableStateOf(initialData?.name.orEmpty()) }
    var area by rememberSaveable(initialData?.areaHectares) {
        mutableStateOf(initialData?.areaHectares.orEmpty())
    }
    var pastureType by rememberSaveable(initialData?.pastureType) {
        mutableStateOf(initialData?.pastureType.orEmpty())
    }
    var capacity by rememberSaveable(initialData?.capacity) {
        mutableStateOf(initialData?.capacity.orEmpty())
    }
    var boundaryGeoJson by rememberSaveable(initialData?.boundaryGeoJson) {
        mutableStateOf(initialData?.boundaryGeoJson)
    }
    var importError by remember { mutableStateOf<String?>(null) }
    var pendingBoundaries by remember {
        mutableStateOf<List<ImportedParcelBoundary>>(emptyList())
    }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val boundaryPointCount = remember(boundaryGeoJson) {
        boundaryGeoJson?.let { encoded ->
            runCatching { decodeParcelBoundary(encoded).size - 1 }.getOrNull()
        }
    }
    val kmlLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importError = null
            coroutineScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { importParcelBoundaries(context, uri) }
                }.onSuccess { imported ->
                    if (imported.size == 1) boundaryGeoJson = imported.first().geoJson
                    else pendingBoundaries = imported
                }.onFailure { error ->
                    importError = error.message ?: "No se pudo leer el archivo KML/KMZ."
                }
            }
        }
    }

    if (pendingBoundaries.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { pendingBoundaries = emptyList() },
            title = { Text("Seleccionar límite") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pendingBoundaries) { imported ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                boundaryGeoJson = imported.geoJson
                                pendingBoundaries = emptyList()
                            }
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(imported.name, fontWeight = FontWeight.Bold)
                                Text("${imported.pointCount} puntos geográficos")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingBoundaries = emptyList() }) { Text("Cancelar") }
            }
        )
    }

    val areaValue = area.replace(',', '.').toDoubleOrNull()
    val areaInvalid = area.isBlank() || areaValue == null || areaValue <= 0
    val capacityValue = capacity.toIntOrNull()
    val capacityInvalid = capacity.isNotBlank() && (capacityValue == null || capacityValue <= 0)
    val formValid = name.isNotBlank() && !areaInvalid && pastureType.isNotBlank() && !capacityInvalid

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = {
                    Text(
                        if (initialData == null) "Registrar nueva parcela" else "Editar parcela",
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
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Complete los datos técnicos del terreno para el inventario de pastizales.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre de la parcela *") },
                    placeholder = { Text("Ej. Sector Norte - Los Álamos") },
                    isError = attemptedSave && name.isBlank(),
                    supportingText = if (attemptedSave && name.isBlank()) {
                        { Text("Ingrese el nombre de la parcela.") }
                    } else null,
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Superficie en hectáreas *") },
                    suffix = { Text("ha") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = attemptedSave && areaInvalid,
                    supportingText = if (attemptedSave && areaInvalid) {
                        { Text("Ingrese una superficie mayor que cero.") }
                    } else null,
                    singleLine = true
                )
            }
            item {
                PastureDropdown(
                    value = pastureType,
                    onSelected = { pastureType = it },
                    showError = attemptedSave && pastureType.isBlank()
                )
            }
            item {
                OutlinedTextField(
                    value = capacity,
                    onValueChange = { capacity = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Capacidad estimada de animales") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = attemptedSave && capacityInvalid,
                    supportingText = if (attemptedSave && capacityInvalid) {
                        { Text("Ingrese una cantidad entera mayor que cero.") }
                    } else null,
                    singleLine = true
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Ubicación satelital", fontWeight = FontWeight.Bold)
                        Text(
                            "Exporta el límite de la parcela desde Google Earth como KML o KMZ y selecciónalo aquí.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (boundaryGeoJson != null) {
                            ParcelSatelliteMap(
                                boundaryGeoJson = boundaryGeoJson!!,
                                modifier = Modifier.fillMaxWidth().height(180.dp)
                            )
                            Text("Límite cargado · ${boundaryPointCount ?: 0} puntos")
                        }
                        OutlinedButton(
                            onClick = {
                                // Storage Access Framework entrega acceso solo al archivo elegido.
                                kmlLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Landscape, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (boundaryGeoJson == null) "Importar KML/KMZ" else "Cambiar KML/KMZ")
                        }
                        if (boundaryGeoJson != null) {
                            TextButton(onClick = { boundaryGeoJson = null }) {
                                Text("Quitar límites importados")
                            }
                        }
                        importError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            item { saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            item {
                Button(
                    onClick = {
                        attemptedSave = true
                        if (formValid) {
                            onSubmit(
                                ParcelFormData(
                                    name = name.trim(),
                                    areaHectares = area,
                                    pastureType = pastureType,
                                    capacity = capacity,
                                    boundaryGeoJson = boundaryGeoJson
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
                    Text(if (isSaving) "Guardando…" else "Guardar parcela", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastureDropdown(
    value: String,
    onSelected: (String) -> Unit,
    showError: Boolean
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                label = { Text("Tipo de pastura *") },
                placeholder = { Text("Seleccione una opción") },
                leadingIcon = { Icon(Icons.Default.Grass, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                isError = showError
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                pastureTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            onSelected(type)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (showError) {
            Text(
                "Seleccione un tipo de pastura.",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/** Detalle navegable de una parcela registrada. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParcelDetailScreen(
    parcel: ParcelUiModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenMap: () -> Unit,
    onSetResting: (Boolean) -> Unit,
    onDeactivate: () -> Unit,
    onNavigateMain: (String) -> Unit,
    canEditRecords: Boolean = false,
    isSaving: Boolean = false,
    saveError: String? = null,
    modifier: Modifier = Modifier
) {
    var confirmDeactivate by rememberSaveable { mutableStateOf(false) }

    if (confirmDeactivate) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) confirmDeactivate = false },
            title = { Text("Desactivar parcela") },
            text = { Text("La parcela quedará inactiva, pero su información no se eliminará.") },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        confirmDeactivate = false
                        onDeactivate()
                    }
                ) { Text("Desactivar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeactivate = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = { Text("Detalle de parcela", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        },
        bottomBar = {
            AppBottomBar(selectedRoute = Routes.PARCELS, onNavigate = onNavigateMain)
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ParcelStatusBadge(parcel.status)
                        Text(parcel.name, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Text(parcel.code, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ParcelSummaryCard(
                        "SUPERFICIE",
                        "${formatArea(parcel.areaHectares)} ha",
                        Modifier.weight(1f)
                    )
                    ParcelSummaryCard(
                        "CAPACIDAD",
                        parcel.capacity?.let { "$it animales" } ?: "Sin dato",
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
                        Text("Información productiva", fontWeight = FontWeight.Bold)
                        Text("Tipo de pastura: ${parcel.pastureType}")
                        Text("Lote actual: ${parcel.currentLot.ifBlank { "Ninguno" }}")
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        parcel.boundaryGeoJson?.let { boundary ->
                            ParcelSatelliteMap(
                                boundaryGeoJson = boundary,
                                modifier = Modifier.fillMaxWidth().height(240.dp),
                                onMapClick = onOpenMap
                            )
                        } ?: ParcelMapUnavailable(
                            message = "Importa un KML/KMZ al editar la parcela",
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                        )
                        if (parcel.boundaryGeoJson != null) {
                            TextButton(onClick = onOpenMap, modifier = Modifier.fillMaxWidth()) {
                                Text("Abrir mapa satelital")
                            }
                        }
                    }
                }
            }
            item { saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (parcel.status != "INACTIVA" && canEditRecords) {
                item {
                    Button(
                        onClick = onEdit,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Editar parcela")
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { onSetResting(parcel.status != "DESCANSO") },
                        enabled = !isSaving && parcel.status != "OCUPADA",
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        Icon(
                            if (parcel.status == "DESCANSO") Icons.Default.PlayCircle
                            else Icons.Default.PauseCircle,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (parcel.status == "DESCANSO") "Marcar disponible"
                            else "Poner en descanso"
                        )
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { confirmDeactivate = true },
                        enabled = !isSaving && parcel.status != "OCUPADA",
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        Icon(Icons.Default.StopCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Desactivar parcela")
                    }
                }
                if (parcel.status == "OCUPADA") {
                    item {
                        Text(
                            "Cierre o cambie el lote activo antes de poner la parcela en descanso o desactivarla.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

private fun formatArea(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private val pastureTypes = listOf(
    "Brachiaria",
    "Estrella africana",
    "Guinea",
    "Jaragua",
    "Kikuyo",
    "King Grass",
    "Mombasa",
    "Pasto mixto",
    "Otro"
)
