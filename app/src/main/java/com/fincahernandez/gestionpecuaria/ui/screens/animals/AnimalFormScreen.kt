package com.fincahernandez.gestionpecuaria.ui.screens.animals

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.ui.theme.GestionPecuariaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Valores capturados en la pantalla de registro. */
data class AnimalFormData(
    val codigoIdentificacion: String,
    val nombre: String,
    val raza: String,
    val sexo: String,
    val categoria: String,
    val tipoOrigen: String,
    val fechaNacimiento: String,
    val fechaIngreso: String,
    val estadoSalud: String,
    val pesoInicial: String,
    val procedencia: String,
    val observaciones: String
)

/**
 * Formulario visual para registrar un animal.
 *
 * Todavía no escribe en Room. Entrega los valores mediante [onSubmit] para que
 * la navegación muestre el flujo completo antes de integrar persistencia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalFormScreen(
    onBack: () -> Unit,
    onSubmit: (AnimalFormData) -> Unit,
    modifier: Modifier = Modifier
) {
    var codigo by rememberSaveable { mutableStateOf("") }
    var nombre by rememberSaveable { mutableStateOf("") }
    var raza by rememberSaveable { mutableStateOf("") }
    var sexo by rememberSaveable { mutableStateOf("MACHO") }
    var categoria by rememberSaveable { mutableStateOf("LECHERO") }
    var tipoOrigen by rememberSaveable { mutableStateOf("NACIDO_EN_FINCA") }
    var fechaNacimiento by rememberSaveable { mutableStateOf("") }
    var fechaIngreso by rememberSaveable { mutableStateOf("") }
    var estadoSalud by rememberSaveable { mutableStateOf("EXCELENTE") }
    var pesoInicial by rememberSaveable { mutableStateOf("") }
    var procedencia by rememberSaveable { mutableStateOf("") }
    var observaciones by rememberSaveable { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Finca Hernández", color = MaterialTheme.colorScheme.primary)
                        Text("Registro de animal", style = MaterialTheme.typography.titleMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                    text = "Complete los datos para añadir un nuevo ejemplar al inventario.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Marcador visual: la selección y el almacenamiento de fotos se implementarán después.
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Agregar foto")
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Código de identificación *") },
                    placeholder = { Text("Ej. FH-2024-88") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre del animal") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = raza,
                    onValueChange = { raza = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Raza") },
                    singleLine = true
                )
            }

            item { SelectorDosOpciones("Sexo", "MACHO", "HEMBRA", sexo) { sexo = it } }

            item {
                Text("Categoría", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LECHERO", "ENGORDE", "AMBOS").forEach { opcion ->
                        FilterChip(
                            selected = categoria == opcion,
                            onClick = { categoria = opcion },
                            label = { Text(opcion.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            item {
                Text("Origen del animal", fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tipoOrigen == "NACIDO_EN_FINCA",
                        onClick = { tipoOrigen = "NACIDO_EN_FINCA" },
                        label = { Text("Nacido en la finca/parcela") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FilterChip(
                        selected = tipoOrigen == "INGRESADO_A_FINCA",
                        onClick = { tipoOrigen = "INGRESADO_A_FINCA" },
                        label = { Text("Ingresado a la finca") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                DateSelectorField(
                    label = if (tipoOrigen == "NACIDO_EN_FINCA") {
                        "Fecha de nacimiento *"
                    } else {
                        "Fecha de nacimiento"
                    },
                    value = fechaNacimiento,
                    onDateSelected = { fechaNacimiento = it }
                )
            }

            // La fecha de llegada solo tiene sentido para animales procedentes de otro lugar.
            if (tipoOrigen == "INGRESADO_A_FINCA") {
                item {
                    DateSelectorField(
                        label = "Fecha de llegada *",
                        value = fechaIngreso,
                        onDateSelected = { fechaIngreso = it }
                    )
                }
            }

            item {
                Text("Estado de salud inicial", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("EXCELENTE", "OBSERVACIÓN").forEach { opcion ->
                        FilterChip(
                            selected = estadoSalud == opcion,
                            onClick = { estadoSalud = opcion },
                            label = { Text(opcion.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = pesoInicial,
                    onValueChange = { pesoInicial = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Peso inicial en kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = procedencia,
                    onValueChange = { procedencia = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (tipoOrigen == "NACIDO_EN_FINCA") {
                                "Parcela de nacimiento"
                            } else {
                                "Procedencia"
                            }
                        )
                    },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = observaciones,
                    onValueChange = { observaciones = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Observaciones") },
                    minLines = 3
                )
            }

            // El botón forma parte de la lista para que pueda alcanzarse al desplazarse,
            // incluso en pantallas pequeñas o cuando está abierto el teclado.
            item {
                val fechaObligatoriaCompleta = if (tipoOrigen == "NACIDO_EN_FINCA") {
                    fechaNacimiento.isNotBlank()
                } else {
                    fechaIngreso.isNotBlank()
                }

                Button(
                    onClick = {
                        onSubmit(
                            AnimalFormData(
                                codigoIdentificacion = codigo,
                                nombre = nombre,
                                raza = raza,
                                sexo = sexo,
                                categoria = categoria,
                                tipoOrigen = tipoOrigen,
                                fechaNacimiento = fechaNacimiento,
                                fechaIngreso = if (tipoOrigen == "NACIDO_EN_FINCA") {
                                    fechaNacimiento
                                } else {
                                    fechaIngreso
                                },
                                estadoSalud = estadoSalud,
                                pesoInicial = pesoInicial,
                                procedencia = procedencia,
                                observaciones = observaciones
                            )
                        )
                    },
                    enabled = codigo.isNotBlank() && fechaObligatoriaCompleta,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Confirmar registro", fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/**
 * Campo de fecha que abre un calendario y evita el ingreso manual de formatos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectorField(
    label: String,
    value: String,
    onDateSelected: (String) -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    OutlinedButton(
        onClick = { showDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value.ifBlank { "Seleccionar fecha" },
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Icon(Icons.Default.CalendarMonth, contentDescription = "Abrir calendario")
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            onDateSelected(formato.format(Date(millis)))
                        }
                        showDialog = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }
}

/** Selector reutilizable para dos valores mutuamente excluyentes. */
@Composable
private fun SelectorDosOpciones(
    titulo: String,
    primera: String,
    segunda: String,
    seleccion: String,
    onSeleccionar: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(titulo, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = seleccion == primera,
                onClick = { onSeleccionar(primera) },
                label = { Text(primera.lowercase().replaceFirstChar { it.uppercase() }) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = seleccion == segunda,
                onClick = { onSeleccionar(segunda) },
                label = { Text(segunda.lowercase().replaceFirstChar { it.uppercase() }) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AnimalFormPreview() {
    GestionPecuariaTheme {
        AnimalFormScreen(onBack = {}, onSubmit = {})
    }
}
