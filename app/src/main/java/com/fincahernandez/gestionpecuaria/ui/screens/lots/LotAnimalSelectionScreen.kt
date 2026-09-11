package com.fincahernandez.gestionpecuaria.ui.screens.lots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.fincahernandez.gestionpecuaria.ui.components.BrandedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Vista independiente para buscar y escoger los animales que integrarán un lote.
 * La selección se confirma en bloque para evitar cambios accidentales en el formulario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotAnimalSelectionScreen(
    animals: List<LotAnimalOption>,
    initiallySelectedIds: List<String>,
    onBack: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var search by rememberSaveable { mutableStateOf("") }
    var selectedIds by rememberSaveable(initiallySelectedIds) {
        mutableStateOf(initiallySelectedIds)
    }

    val filteredAnimals = animals.filter { animal ->
        search.isBlank() ||
            animal.code.contains(search, ignoreCase = true) ||
            animal.name.orEmpty().contains(search, ignoreCase = true) ||
            animal.category.contains(search, ignoreCase = true)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = { Text("Seleccionar animales", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        },
        bottomBar = {
            // Este botón permanece visible en la tableta y devuelve la selección al formulario.
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = { onConfirm(selectedIds) },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Confirmar selección (${selectedIds.size})")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar animal") },
                    placeholder = { Text("Código, nombre o categoría") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }
            item {
                Text(
                    "${selectedIds.size} de ${animals.size} seleccionados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (filteredAnimals.isEmpty()) {
                item { Text("No se encontraron animales con esa búsqueda.") }
            } else {
                items(filteredAnimals, key = { it.id }) { animal ->
                    val isSelected = animal.id in selectedIds
                    Card(
                        onClick = {
                            selectedIds = if (isSelected) {
                                selectedIds - animal.id
                            } else {
                                selectedIds + animal.id
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) {
                                        selectedIds + animal.id
                                    } else {
                                        selectedIds - animal.id
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    animal.name?.takeIf { it.isNotBlank() } ?: "Animal sin nombre",
                                    fontWeight = FontWeight.Bold
                                )
                                Text("${animal.code} • ${animal.category}")
                            }
                            Text(
                                animal.weightLibras?.let {
                                    "${String.format(Locale.getDefault(), "%.1f", it)} lb"
                                } ?: "Sin peso",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
