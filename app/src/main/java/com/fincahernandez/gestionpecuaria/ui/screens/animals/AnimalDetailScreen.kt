package com.fincahernandez.gestionpecuaria.ui.screens.animals

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.ui.components.AppBottomBar
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import com.fincahernandez.gestionpecuaria.ui.theme.GestionPecuariaTheme

/** Perfil visual con el resumen y los indicadores disponibles del animal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalDetailScreen(
    animal: AnimalListItem,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRegisterWeight: () -> Unit,
    sanitaryEventCount: Int = 0,
    canManageHealth: Boolean = false,
    onOpenSanitaryControl: () -> Unit = {},
    onNavigateMain: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = {
                    Text(
                        "Finca Hernández",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
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
        },
        bottomBar = {
            AppBottomBar(
                selectedRoute = Routes.ANIMAL_LIST,
                onNavigate = onNavigateMain
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
            item { AnimalProfileCard(animal, onEdit, onRegisterWeight) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        title = "PESO ACTUAL",
                        value = animal.ultimoPesoLibras?.let { "${it.toInt()} lb" } ?: "Sin dato",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "CATEGORÍA",
                        value = animal.categoria,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item { AnimalRegisteredInformation(animal) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("LOTE ACTUAL", "Sin asignar", Modifier.weight(1f))
                    MetricCard("ESTADO", animal.estado, Modifier.weight(1f))
                }
            }
            item {
                Text(
                    "Actividad reciente",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            if (sanitaryEventCount == 0) {
                                "Este animal todavía no tiene eventos sanitarios registrados."
                            } else {
                                "$sanitaryEventCount eventos guardados en su ficha clínica individual."
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (canManageHealth) {
                            OutlinedButton(
                                onClick = onOpenSanitaryControl,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.MedicalServices, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Abrir historial sanitario")
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Eliminar del inventario")
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Eliminar animal") },
            text = {
                Text(
                    "El animal dejará de aparecer en el inventario, pero se conservará su historial."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    }
                ) { Text("Eliminar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/** Muestra los valores capturados en el formulario para cumplir HU-03. */
@Composable
private fun AnimalRegisteredInformation(animal: AnimalListItem) {
    val origen = when (animal.tipoOrigen) {
        "NACIDO_EN_FINCA" -> "Nacido en la finca/parcela"
        "INGRESADO_A_FINCA" -> "Ingresado a la finca"
        else -> "Sin registro"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Información registrada",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            AnimalInformationRow("Raza", animal.raza.ifBlank { "Sin registro" })
            AnimalInformationRow("Sexo", animal.sexo.ifBlank { "Sin registro" })
            AnimalInformationRow("Origen", origen)
            AnimalInformationRow(
                "Fecha de nacimiento",
                animal.fechaNacimiento.ifBlank { "Sin registro" }
            )
            if (animal.tipoOrigen == "INGRESADO_A_FINCA") {
                AnimalInformationRow(
                    "Fecha de llegada",
                    animal.fechaIngreso.ifBlank { "Sin registro" }
                )
            }
            AnimalInformationRow(
                if (animal.tipoOrigen == "NACIDO_EN_FINCA") "Parcela de nacimiento" else "Procedencia",
                animal.procedencia.ifBlank { "Sin registro" }
            )
            AnimalInformationRow(
                "Observaciones",
                animal.observaciones.ifBlank { "Sin observaciones" },
                showDivider = false
            )
        }
    }
}

/** Fila reutilizable para presentar una etiqueta y su valor. */
@Composable
private fun AnimalInformationRow(
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Tarjeta principal inspirada en el perfil incluido en el prototipo. */
@Composable
private fun AnimalProfileCard(
    animal: AnimalListItem,
    onEdit: () -> Unit,
    onRegisterWeight: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            AnimalPhoto(
                photoUri = animal.fotoUri,
                modifier = Modifier.fillMaxSize(),
                placeholderIcon = Icons.Default.Pets,
                placeholderText = "Sin fotografía registrada",
                maxDecodeDimensionPx = 900
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                shape = RoundedCornerShape(50),
                color = when (animal.estado) {
                    "CRÍTICO" -> MaterialTheme.colorScheme.errorContainer
                    "OBSERVACIÓN" -> Color(0xFFFFE7A8)
                    else -> MaterialTheme.colorScheme.primary
                }
            ) {
                Text(
                    animal.estado,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = when (animal.estado) {
                        "CRÍTICO" -> MaterialTheme.colorScheme.onErrorContainer
                        "OBSERVACIÓN" -> Color(0xFF6B4E00)
                        else -> Color.White
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        animal.nombre ?: "Sin nombre",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(animal.codigoIdentificacion)
                }
                OutlinedButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Editar")
                }
            }
            Button(
                onClick = onRegisterWeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Scale, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registrar pesaje")
            }
        }
    }
}

/** Tarjeta compacta para un indicador del perfil. */
@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AnimalDetailPreview() {
    GestionPecuariaTheme {
        AnimalDetailScreen(
            animal = AnimalListItem("1", "FH-2024-88", "Luna", "LECHERO", "EXCELENTE", 450.0),
            onBack = {},
            onEdit = {},
            onDelete = {},
            onRegisterWeight = {}
        )
    }
}
