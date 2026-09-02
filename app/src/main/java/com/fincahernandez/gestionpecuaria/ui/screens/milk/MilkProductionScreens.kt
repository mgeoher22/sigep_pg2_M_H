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
import com.fincahernandez.gestionpecuaria.ui.components.DemoModeNotice
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import java.util.Locale

/** Registro temporal de la producción total obtenida durante un día. */
data class MilkProductionUiModel(
    val id: String,
    val date: String,
    val liters: Double,
    val notes: String
)

/** Datos capturados por el formulario de producción. */
data class MilkProductionFormData(
    val date: String,
    val liters: String,
    val notes: String
)

/** Panel operativo de producción lechera sin cálculos financieros. */
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
    val averageLiters = records.map { it.liters }.average().takeUnless { it.isNaN() }

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
            item { DemoModeNotice(compact = true) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("PRODUCCIÓN REGISTRADA", color = Color.White.copy(alpha = 0.8f))
                        Text(
                            "${oneDecimal(totalLiters)} L",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Datos temporales de HU-05", color = Color.White)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MilkSummaryCard(
                        title = "PROMEDIO",
                        value = averageLiters?.let { "${oneDecimal(it)} L" } ?: "Sin datos",
                        modifier = Modifier.weight(1f)
                    )
                    MilkSummaryCard(
                        title = "REGISTROS",
                        value = records.size.toString(),
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

/** Barras construidas con los últimos siete registros de producción. */
@Composable
private fun MilkWeeklyChart(records: List<MilkProductionUiModel>) {
    val values = records.takeLast(7).map { it.liters }
    val maximum = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Rendimiento reciente", fontWeight = FontWeight.Bold)
            if (values.isEmpty()) {
                Text("El gráfico aparecerá después del primer registro.")
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
                Text("Producción total del día")
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
    modifier: Modifier = Modifier
) {
    var date by rememberSaveable { mutableStateOf("") }
    var liters by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }

    val litersValue = liters.toDoubleOrNull()
    val litersInvalid = liters.isBlank() || litersValue == null || litersValue <= 0
    val formValid = date.isNotBlank() && !litersInvalid

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
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notas y observaciones") },
                    placeholder = { Text("Calidad, salud, mastitis u otra observación") },
                    minLines = 4
                )
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
                    Text("Guardar registro", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun oneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)
