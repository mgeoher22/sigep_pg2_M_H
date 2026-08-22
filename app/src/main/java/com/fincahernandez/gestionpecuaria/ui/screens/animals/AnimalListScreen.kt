package com.fincahernandez.gestionpecuaria.ui.screens.animals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import com.fincahernandez.gestionpecuaria.ui.components.AppBottomBar
import com.fincahernandez.gestionpecuaria.ui.theme.GestionPecuariaTheme

/** Modelo sencillo que contiene únicamente los datos que necesita la lista. */
data class AnimalListItem(
    val id: String,
    val codigoIdentificacion: String,
    val nombre: String?,
    val categoria: String,
    val estado: String,
    val ultimoPesoKg: Double? = null
)

/**
 * Pantalla principal del módulo de animales.
 *
 * Recibe los datos y eventos como parámetros para que el componente visual no
 * dependa directamente de Room. Más adelante un ViewModel proporcionará estos
 * valores desde la base de datos.
 */
@Composable
fun AnimalListScreen(
    animales: List<AnimalListItem>,
    onRegistrarAnimal: () -> Unit,
    onAnimalClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var busqueda by rememberSaveable { mutableStateOf("") }

    // Filtra localmente los elementos visibles mientras el usuario escribe.
    val animalesFiltrados = animales.filter { animal ->
        busqueda.isBlank() ||
            animal.codigoIdentificacion.contains(busqueda, ignoreCase = true) ||
            animal.nombre.orEmpty().contains(busqueda, ignoreCase = true) ||
            animal.categoria.contains(busqueda, ignoreCase = true)
    }

    // Scaffold organiza la barra superior, el botón flotante y el contenido.
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AnimalTopBar() },
        bottomBar = { AppBottomBar() },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRegistrarAnimal,
                text = { Text("Registrar animal") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campo de búsqueda del inventario.
            item {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar animal") },
                    placeholder = { Text("Código, nombre o categoría") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
            }

            // Control reservado para los filtros de categoría y estado.
            item {
                Surface(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Filtros", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Cambia entre un mensaje vacío y la lista de tarjetas.
            if (animalesFiltrados.isEmpty()) {
                item {
                    EmptyAnimalsCard(
                        hayBusqueda = busqueda.isNotBlank(),
                        onRegistrarAnimal = onRegistrarAnimal
                    )
                }
            } else {
                items(
                    items = animalesFiltrados,
                    key = { it.id }
                ) { animal ->
                    AnimalCard(
                        animal = animal,
                        onClick = { onAnimalClick(animal.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

/** Barra superior inspirada en el prototipo de Gestión de Animales. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AnimalTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "Gestión de Animales",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
            }
        },
        actions = {
            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = "Datos locales",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(onClick = {}) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Perfil")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/** Explica qué hacer cuando todavía no existen animales o no hay resultados. */
@Composable
private fun EmptyAnimalsCard(
    hayBusqueda: Boolean,
    onRegistrarAnimal: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (hayBusqueda) "Sin resultados" else "Aún no hay animales registrados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (hayBusqueda) {
                    "Prueba con otro código, nombre o categoría."
                } else {
                    "Registra el primer animal para comenzar a construir el inventario de la finca."
                },
                style = MaterialTheme.typography.bodyMedium
            )
            if (!hayBusqueda) {
                OutlinedButton(onClick = onRegistrarAnimal) {
                    Text("Registrar primer animal")
                }
            }
        }
    }
}

/** Tarjeta que resume la información más importante de un animal. */
@Composable
private fun AnimalCard(
    animal: AnimalListItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    modifier = Modifier.height(56.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = animal.codigoIdentificacion,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        animal.nombre?.takeIf { it.isNotBlank() }?.let { nombre ->
                            Text(
                                text = nombre,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    StatusBadge(estado = animal.estado)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = animal.categoria,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = animal.ultimoPesoKg?.let { "${it.toInt()} kg" } ?: "Sin pesaje",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/** Muestra visualmente el estado general del animal. */
@Composable
private fun StatusBadge(estado: String) {
    val esObservacion = estado.equals("OBSERVACIÓN", ignoreCase = true)
    Surface(
        color = if (esObservacion) Color(0xFFFFE7A8) else MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = estado.lowercase().replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (esObservacion) Color(0xFF6B4E00) else MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Datos de muestra visibles únicamente dentro del panel Preview. */
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AnimalListScreenPreview() {
    GestionPecuariaTheme(darkTheme = false) {
        AnimalListScreen(
            animales = listOf(
                AnimalListItem(
                    id = "1",
                    codigoIdentificacion = "FH-2024-88",
                    nombre = "Luna",
                    categoria = "LECHERO",
                    estado = "ACTIVO",
                    ultimoPesoKg = 450.0
                ),
                AnimalListItem(
                    id = "2",
                    codigoIdentificacion = "FH-2023-102",
                    nombre = null,
                    categoria = "ENGORDE",
                    estado = "ACTIVO",
                    ultimoPesoKg = 612.0
                )
            ),
            onRegistrarAnimal = {},
            onAnimalClick = {}
        )
    }
}
