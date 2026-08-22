package com.fincahernandez.gestionpecuaria.ui.screens.animals

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.ui.components.AppBottomBar
import com.fincahernandez.gestionpecuaria.ui.theme.GestionPecuariaTheme

/** Pantalla que confirma visualmente la recepción de un nuevo registro. */
@Composable
fun AnimalConfirmationScreen(
    animal: AnimalListItem,
    onViewProfile: () -> Unit,
    onRegisterAnother: () -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = { AppBottomBar() }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                Text(
                    text = "Finca Hernández",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "¡Animal registrado!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "El registro se mantiene temporalmente durante esta demostración.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            item { ConfirmationCard(animal) }
            item {
                Button(
                    onClick = onViewProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Ver perfil del animal")
                }
            }
            item {
                OutlinedButton(
                    onClick = onRegisterAnother,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Registrar otro animal")
                }
            }
            item {
                OutlinedButton(onClick = onBackToList) {
                    Text("Volver a la lista")
                }
            }
        }
    }
}

/** Resumen del animal mostrado después del registro. */
@Composable
private fun ConfirmationCard(animal: AnimalListItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("IDENTIFICACIÓN", style = MaterialTheme.typography.labelMedium)
            Text(
                animal.codigoIdentificacion,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            animal.nombre?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Categoría", style = MaterialTheme.typography.labelMedium)
                    Text(animal.categoria)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Peso inicial", style = MaterialTheme.typography.labelMedium)
                    Text(animal.ultimoPesoKg?.let { "$it kg" } ?: "Sin registro")
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AnimalConfirmationPreview() {
    GestionPecuariaTheme {
        AnimalConfirmationScreen(
            animal = AnimalListItem("1", "HER-042", "Rocío", "LECHERO", "EXCELENTE", 342.5),
            onViewProfile = {},
            onRegisterAnother = {},
            onBackToList = {}
        )
    }
}
