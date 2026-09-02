package com.fincahernandez.gestionpecuaria.ui.screens.animals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import com.fincahernandez.gestionpecuaria.ui.components.AppBottomBar
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import com.fincahernandez.gestionpecuaria.ui.theme.GestionPecuariaTheme

/**
 * Modelo temporal compartido por el listado, la confirmación y el detalle.
 * Conserva todos los valores del formulario hasta conectar HU-07 con Room.
 */
data class AnimalListItem(
    val id: String,
    val codigoIdentificacion: String,
    val nombre: String?,
    val categoria: String,
    val estado: String,
    val ultimoPesoLibras: Double? = null,
    val raza: String = "",
    val sexo: String = "",
    val tipoOrigen: String = "",
    val fechaNacimiento: String = "",
    val fechaIngreso: String = "",
    val procedencia: String = "",
    val observaciones: String = "",
    val fotoUri: String = ""
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
    onMenuClick: () -> Unit = {},
    onNavigateMain: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var categoriaSeleccionada by rememberSaveable { mutableStateOf(FILTER_ALL) }
    var estadoSeleccionado by rememberSaveable { mutableStateOf(FILTER_ALL) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    val activeFilterCount = listOf(categoriaSeleccionada, estadoSeleccionado)
        .count { it != FILTER_ALL }

    // Combina la búsqueda de texto con categoría y estado de salud.
    val animalesFiltrados = animales.filter { animal ->
        val coincideBusqueda = busqueda.isBlank() ||
            animal.codigoIdentificacion.contains(busqueda, ignoreCase = true) ||
            animal.nombre.orEmpty().contains(busqueda, ignoreCase = true) ||
            animal.categoria.contains(busqueda, ignoreCase = true)
        val coincideCategoria = categoriaSeleccionada == FILTER_ALL ||
            animal.categoria.equals(categoriaSeleccionada, ignoreCase = true)
        val coincideEstado = estadoSeleccionado == FILTER_ALL ||
            animal.estado.equals(estadoSeleccionado, ignoreCase = true)

        coincideBusqueda && coincideCategoria && coincideEstado
    }

    if (showFilters) {
        AnimalFiltersDialog(
            selectedCategory = categoriaSeleccionada,
            selectedStatus = estadoSeleccionado,
            onDismiss = { showFilters = false },
            onClear = {
                categoriaSeleccionada = FILTER_ALL
                estadoSeleccionado = FILTER_ALL
                showFilters = false
            },
            onApply = { category, status ->
                categoriaSeleccionada = category
                estadoSeleccionado = status
                showFilters = false
            }
        )
    }

    // Scaffold organiza la barra superior, el botón flotante y el contenido.
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { AnimalTopBar(onMenuClick = onMenuClick) },
        bottomBar = {
            AppBottomBar(
                selectedRoute = Routes.ANIMAL_LIST,
                onNavigate = onNavigateMain
            )
        },
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

            // Abre el selector de filtros y muestra cuántos están activos.
            item {
                Surface(
                    onClick = { showFilters = true },
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
                        Text(
                            if (activeFilterCount == 0) "Filtros" else "Filtros ($activeFilterCount)",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Cambia entre un mensaje vacío y la lista de tarjetas.
            if (animalesFiltrados.isEmpty()) {
                item {
                    EmptyAnimalsCard(
                        hayBusqueda = busqueda.isNotBlank() || activeFilterCount > 0,
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

/** Cuadro de selección para filtrar el inventario sin abandonar la pantalla. */
@Composable
private fun AnimalFiltersDialog(
    selectedCategory: String,
    selectedStatus: String,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onApply: (String, String) -> Unit
) {
    var temporaryCategory by rememberSaveable(selectedCategory) {
        mutableStateOf(selectedCategory)
    }
    var temporaryStatus by rememberSaveable(selectedStatus) {
        mutableStateOf(selectedStatus)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtrar animales") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Categoría", fontWeight = FontWeight.SemiBold)
                FilterOptionGrid(
                    options = categoryFilterOptions,
                    selectedValue = temporaryCategory,
                    onSelected = { temporaryCategory = it }
                )
                Text("Estado de salud", fontWeight = FontWeight.SemiBold)
                FilterOptionGrid(
                    options = statusFilterOptions,
                    selectedValue = temporaryStatus,
                    onSelected = { temporaryStatus = it }
                )
                if (selectedCategory != FILTER_ALL || selectedStatus != FILTER_ALL) {
                    TextButton(onClick = onClear) {
                        Text("Limpiar filtros")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(temporaryCategory, temporaryStatus) }) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/** Distribuye las opciones en dos columnas para teléfono y tableta. */
@Composable
private fun FilterOptionGrid(
    options: List<Pair<String, String>>,
    selectedValue: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(2).forEach { optionRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                optionRow.forEach { (value, label) ->
                    FilterChip(
                        selected = selectedValue == value,
                        onClick = { onSelected(value) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (optionRow.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private const val FILTER_ALL = "TODOS"

private val categoryFilterOptions = listOf(
    FILTER_ALL to "Todas",
    "LECHERO" to "Lechero",
    "ENGORDE" to "Engorde",
    "AMBOS" to "Ambos"
)

private val statusFilterOptions = listOf(
    FILTER_ALL to "Todos",
    "EXCELENTE" to "Excelente",
    "OBSERVACIÓN" to "Observación"
)

/** Barra superior inspirada en el prototipo de Gestión de Animales. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AnimalTopBar(onMenuClick: () -> Unit) {
    BrandedTopAppBar(
        title = {
            Text(
                text = "Gestión de Animales",
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
            AnimalPhoto(
                photoUri = animal.fotoUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                placeholderIcon = Icons.Default.Pets,
                placeholderText = "Sin fotografía"
            )

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
                        text = animal.ultimoPesoLibras?.let { "${it.toInt()} lb" } ?: "Sin pesaje",
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
                    ultimoPesoLibras = 450.0
                ),
                AnimalListItem(
                    id = "2",
                    codigoIdentificacion = "FH-2023-102",
                    nombre = null,
                    categoria = "ENGORDE",
                    estado = "ACTIVO",
                    ultimoPesoLibras = 612.0
                )
            ),
            onRegistrarAnimal = {},
            onAnimalClick = {}
        )
    }
}
