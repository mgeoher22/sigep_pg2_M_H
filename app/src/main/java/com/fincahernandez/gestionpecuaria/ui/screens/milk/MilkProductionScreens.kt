package com.fincahernandez.gestionpecuaria.ui.screens.milk

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
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.fincahernandez.gestionpecuaria.ui.components.BrandedTopAppBar
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
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import java.util.Locale

/** Registro diario persistente con el precio histórico aplicado. */
data class MilkProductionUiModel(
    val id: String,
    val dateMillis: Long,
    val date: String,
    val liters: Double,
    val pricePerLiter: Double,
    val notes: String
) {
    /** Ingreso bruto; los costos de producción se tratarán en el módulo financiero. */
    val grossIncome: Double get() = liters * pricePerLiter
}

/** Datos capturados por el formulario de producción. */
data class MilkProductionFormData(
    val date: String,
    val liters: String,
    val pricePerLiter: String,
    val notes: String
)

/** Panel de producción, precio histórico e ingreso bruto con datos de Room. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilkProductionListScreen(
    records: List<MilkProductionUiModel>,
    onMenuClick: () -> Unit,
    onCreateRecord: () -> Unit,
    onNavigateMain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalLiters = records.sumOf { it.liters }
    val totalGrossIncome = records.sumOf { it.grossIncome }
    val averageLiters = records.map { it.liters }.average().takeUnless { it.isNaN() }
    val weightedAveragePrice = if (totalLiters > 0.0) totalGrossIncome / totalLiters else null
    val latestPrice = records.maxByOrNull { it.dateMillis }?.pricePerLiter

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = {
                    Text(
                        "Producción de Leche",
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
            AppBottomBar(selectedRoute = Routes.MILK_PRODUCTION, onNavigate = onNavigateMain)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateRecord,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar producción") }
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("INGRESO BRUTO REGISTRADO", color = Color.White.copy(alpha = 0.8f))
                        Text(
                            money(totalGrossIncome),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Litros × precio histórico; todavía no descuenta costos.",
                            color = Color.White
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MilkSummaryCard(
                        title = "LITROS",
                        value = "${oneDecimal(totalLiters)} L",
                        modifier = Modifier.weight(1f)
                    )
                    MilkSummaryCard(
                        title = "PROM. DIARIO",
                        value = averageLiters?.let { "${oneDecimal(it)} L" } ?: "Sin datos",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MilkSummaryCard(
                        title = "PRECIO ACTUAL",
                        value = latestPrice?.let { "${money(it)}/L" } ?: "Sin datos",
                        modifier = Modifier.weight(1f)
                    )
                    MilkSummaryCard(
                        title = "PRECIO PROM.",
                        value = weightedAveragePrice?.let { "${money(it)}/L" } ?: "Sin datos",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item { MilkWeeklyChart(records) }
            item {
                Text(
                    "Registros recientes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (records.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Aún no hay producción registrada.",
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(records, key = { it.id }) { record ->
                    MilkRecordCard(record)
                }
            }
            item { Spacer(modifier = Modifier.height(76.dp)) }
        }
    }
}

@Composable
private fun MilkSummaryCard(title: String, value: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Barras con valores y fechas reales de los últimos siete días registrados. */
@Composable
private fun MilkWeeklyChart(records: List<MilkProductionUiModel>) {
    val visibleRecords = records.sortedBy { it.dateMillis }.takeLast(7)
    val maximum = visibleRecords.maxOfOrNull { it.liters }?.coerceAtLeast(1.0) ?: 1.0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Producción diaria real", fontWeight = FontWeight.Bold)
            Text(
                "Últimos ${visibleRecords.size} días registrados.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (visibleRecords.isEmpty()) {
                Text("El gráfico aparecerá después del primer registro.")
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    visibleRecords.forEach { record ->
                        val height = (record.liters / maximum * 115).toInt().coerceAtLeast(12)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(oneDecimal(record.liters), style = MaterialTheme.typography.labelSmall)
                            Box(
                                modifier = Modifier
                                    .width(30.dp)
                                    .height(height.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)
                                    )
                            )
                            Text(record.date.take(5), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MilkRecordCard(record: MilkProductionUiModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocalDrink,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(record.date, fontWeight = FontWeight.Bold)
                Text("${money(record.pricePerLiter)} por litro")
                Text(
                    "Ingreso bruto: ${money(record.grossIncome)}",
                    fontWeight = FontWeight.SemiBold
                )
                if (record.notes.isNotBlank()) {
                    Text(record.notes, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                "${oneDecimal(record.liters)} L",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Formulario simple para registrar la producción total de leche de un día. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilkProductionFormScreen(
    onBack: () -> Unit,
    onSubmit: (MilkProductionFormData) -> Unit,
    initialPricePerLiter: Double? = null,
    isSaving: Boolean = false,
    saveError: String? = null,
    modifier: Modifier = Modifier
) {
    var date by rememberSaveable { mutableStateOf("") }
    var liters by rememberSaveable { mutableStateOf("") }
    var pricePerLiter by rememberSaveable(initialPricePerLiter) {
        mutableStateOf(initialPricePerLiter?.let(::twoDecimals).orEmpty())
    }
    var notes by rememberSaveable { mutableStateOf("") }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }

    val litersValue = liters.replace(',', '.').toDoubleOrNull()
    val priceValue = pricePerLiter.replace(',', '.').toDoubleOrNull()
    val litersInvalid = liters.isBlank() || litersValue == null || litersValue <= 0
    val priceInvalid = pricePerLiter.isBlank() || priceValue == null || priceValue < 0
    val formValid = date.isNotBlank() && !litersInvalid && !priceInvalid

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = { Text("Registro de producción", fontWeight = FontWeight.Bold) },
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
                    "Registre la producción total obtenida en el ordeño de la mañana.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            item {
                CompactDateSelector(
                    label = "Día del registro *",
                    value = date,
                    onDateSelected = { date = it },
                    showError = attemptedSave && date.isBlank()
                )
            }
            item {
                OutlinedTextField(
                    value = liters,
                    onValueChange = { liters = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Litros producidos *") },
                    suffix = { Text("L") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = attemptedSave && litersInvalid,
                    supportingText = if (attemptedSave && litersInvalid) {
                        { Text("Ingrese una cantidad numérica mayor que cero.") }
                    } else null,
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = pricePerLiter,
                    onValueChange = { pricePerLiter = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Precio por litro *") },
                    prefix = { Text("Q ") },
                    suffix = { Text("/L") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = attemptedSave && priceInvalid,
                    supportingText = if (attemptedSave && priceInvalid) {
                        { Text("Ingrese un precio válido; puede ser cero.") }
                    } else {
                        { Text("Se conserva como precio histórico de la fecha seleccionada.") }
                    },
                    singleLine = true
                )
            }
            if (litersValue != null && litersValue > 0 && priceValue != null && priceValue >= 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Ingreso bruto calculado", style = MaterialTheme.typography.labelMedium)
                            Text(
                                money(litersValue * priceValue),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text("No incluye costos ni gastos de producción.")
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notas y observaciones") },
                    placeholder = { Text("Calidad, salud, mastitis u otra observación") },
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
                                MilkProductionFormData(
                                    date = date,
                                    liters = liters,
                                    pricePerLiter = pricePerLiter,
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
                    Text(if (isSaving) "Guardando…" else "Guardar registro", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun oneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

private fun twoDecimals(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun money(value: Double): String = "Q ${twoDecimals(value)}"
