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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
    onRegisterWeight: () -> Unit,
    onNavigateMain: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
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
                        value = animal.ultimoPesoKg?.let { "${it.toInt()} kg" } ?: "Sin dato",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "CATEGORÍA",
                        value = animal.categoria,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
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
                    Text(
                        "Los pesajes, eventos sanitarios y movimientos aparecerán aquí cuando se conecten sus módulos.",
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
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
            Icon(
                Icons.Default.Pets,
                contentDescription = null,
                modifier = Modifier.size(82.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                shape = RoundedCornerShape(50),
                color = if (animal.estado == "OBSERVACIÓN") Color(0xFFFFE7A8) else MaterialTheme.colorScheme.primary
            ) {
                Text(
                    animal.estado,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = if (animal.estado == "OBSERVACIÓN") Color(0xFF6B4E00) else Color.White,
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
            onRegisterWeight = {}
        )
    }
}
