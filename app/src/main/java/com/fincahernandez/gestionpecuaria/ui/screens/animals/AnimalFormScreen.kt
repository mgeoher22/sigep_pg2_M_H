package com.fincahernandez.gestionpecuaria.ui.screens.animals

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.fincahernandez.gestionpecuaria.ui.components.BrandedTopAppBar
import com.fincahernandez.gestionpecuaria.ui.components.formatDatePickerMillis
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.ui.theme.GestionPecuariaTheme
import java.text.SimpleDateFormat
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
    val observaciones: String,
    val fotoUri: String = "",
    val madreId: String = "",
    val proximaParto: Boolean = false
)

/** Vaca disponible para registrar la relación de maternidad. */
data class AnimalMotherOption(
    val id: String,
    val label: String
)

/**
 * Formulario visual para registrar o corregir la información de un animal.
 *
 * Entrega los valores mediante [onSubmit]; HU-07 los guarda posteriormente en Room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalFormScreen(
    codigoGenerado: String,
    initialData: AnimalFormData? = null,
    motherOptions: List<AnimalMotherOption> = emptyList(),
    saveError: String? = null,
    isSaving: Boolean = false,
    onBack: () -> Unit,
    onSubmit: (AnimalFormData) -> Unit,
    modifier: Modifier = Modifier
) {
    // El código viene de la navegación y no puede ser modificado por el usuario.
    val isEditing = initialData != null
    val codigo = initialData?.codigoIdentificacion ?: codigoGenerado
    var nombre by rememberSaveable { mutableStateOf(initialData?.nombre.orEmpty()) }
    var raza by rememberSaveable { mutableStateOf(initialData?.raza.orEmpty()) }
    var sexo by rememberSaveable { mutableStateOf(initialData?.sexo?.ifBlank { "MACHO" } ?: "MACHO") }
    var categoria by rememberSaveable {
        mutableStateOf(initialData?.categoria?.ifBlank { "LECHERO" } ?: "LECHERO")
    }
    var tipoOrigen by rememberSaveable {
        mutableStateOf(initialData?.tipoOrigen?.ifBlank { "NACIDO_EN_FINCA" } ?: "NACIDO_EN_FINCA")
    }
    var madreId by rememberSaveable { mutableStateOf(initialData?.madreId.orEmpty()) }
    var proximaParto by rememberSaveable { mutableStateOf(initialData?.proximaParto ?: false) }
    var fechaNacimiento by rememberSaveable {
        mutableStateOf(initialData?.fechaNacimiento.orEmpty())
    }
    var fechaIngreso by rememberSaveable { mutableStateOf(initialData?.fechaIngreso.orEmpty()) }
    var estadoSalud by rememberSaveable {
        mutableStateOf(initialData?.estadoSalud?.ifBlank { "EXCELENTE" } ?: "EXCELENTE")
    }
    var pesoInicial by rememberSaveable { mutableStateOf(initialData?.pesoInicial.orEmpty()) }
    var procedencia by rememberSaveable { mutableStateOf(initialData?.procedencia.orEmpty()) }
    var observaciones by rememberSaveable { mutableStateOf(initialData?.observaciones.orEmpty()) }
    var fotoUri by rememberSaveable { mutableStateOf(initialData?.fotoUri.orEmpty()) }
    var intentoGuardar by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { selectedUri ->
        selectedUri?.let { uri ->
            // Conserva el permiso para que la imagen siga disponible al volver a abrir la app.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            fotoUri = uri.toString()
        }
    }

    // HU-03 requiere señalar visualmente los campos obligatorios o inválidos.
    val codigoInvalido = codigo.isBlank()
    val fechaNacimientoMillis = parseAnimalFormDate(fechaNacimiento)
    val fechaIngresoMillis = parseAnimalFormDate(fechaIngreso)
    val ahora = System.currentTimeMillis()
    val errorFechaNacimiento = when {
        tipoOrigen == "NACIDO_EN_FINCA" && fechaNacimiento.isBlank() ->
            "Seleccione la fecha de nacimiento."
        fechaNacimiento.isNotBlank() && fechaNacimientoMillis == null ->
            "La fecha de nacimiento no tiene un formato válido."
        fechaNacimientoMillis != null && fechaNacimientoMillis > ahora ->
            "La fecha de nacimiento no puede ser futura."
        else -> null
    }
    val errorFechaIngreso = when {
        tipoOrigen != "INGRESADO_A_FINCA" -> null
        fechaIngreso.isBlank() -> "Seleccione la fecha de llegada."
        fechaIngresoMillis == null -> "La fecha de llegada no tiene un formato válido."
        fechaIngresoMillis > ahora -> "La fecha de llegada no puede ser futura."
        fechaNacimientoMillis != null && fechaIngresoMillis < fechaNacimientoMillis ->
            "La fecha de llegada no puede ser anterior al nacimiento."
        else -> null
    }
    val pesoInvalido = pesoInicial.isNotBlank() &&
        (pesoInicial.toDoubleOrNull() == null || pesoInicial.toDouble() <= 0.0)
    val formularioValido = codigoInvalido.not() &&
        errorFechaNacimiento == null &&
        errorFechaIngreso == null &&
        pesoInvalido.not()

    // Si la capa de datos devuelve una validación conocida, también se relaciona
    // con el campo correspondiente para que el usuario no tenga que adivinarla.
    val errorNacimientoGuardado = saveError?.takeIf { message ->
        message.contains("nacimiento", ignoreCase = true) &&
            message.contains("llegada", ignoreCase = true).not()
    }
    val errorIngresoGuardado = saveError?.takeIf { message ->
        message.contains("llegada", ignoreCase = true)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = {
                    Column {
                        Text("Finca Hernández", color = MaterialTheme.colorScheme.primary)
                        Text(
                            if (isEditing) "Editar animal" else "Registro de animal",
                            style = MaterialTheme.typography.titleMedium
                        )
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isEditing) {
                            "Corrija los datos necesarios y guarde los cambios del animal."
                        } else {
                            "Complete los datos para añadir un nuevo ejemplar al inventario."
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "* Campos obligatorios",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            item {
                AnimalPhotoSelector(
                    photoUri = fotoUri,
                    onSelectPhoto = {
                        // El selector del sistema concede acceso solamente a la foto elegida;
                        // la aplicación nunca obtiene permiso para recorrer toda la galería.
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemovePhoto = { fotoUri = "" }
                )
            }

            item {
                OutlinedTextField(
                    value = codigo,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Código de identificación") },
                    leadingIcon = {
                        Icon(Icons.Default.Tag, contentDescription = null)
                    },
                    supportingText = {
                        Text("Generado automáticamente por el sistema.")
                    },
                    readOnly = true,
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
                BreedDropdownField(
                    value = raza,
                    onValueSelected = { raza = it }
                )
            }

            item {
                SelectorDosOpciones("Sexo", "MACHO", "HEMBRA", sexo) { selectedSex ->
                    sexo = selectedSex
                    // La condición reproductiva solo corresponde a animales hembra.
                    if (selectedSex != "HEMBRA") proximaParto = false
                }
            }

            if (sexo == "HEMBRA") {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = proximaParto,
                            onClick = { proximaParto = !proximaParto },
                            label = { Text("Próxima a dar a luz") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Activa esta opción para incluir la vaca al registrar una nueva cría.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

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
                        onClick = {
                            tipoOrigen = "INGRESADO_A_FINCA"
                            // Un animal comprado o trasladado no usa la maternidad de la finca.
                            madreId = ""
                        },
                        label = { Text("Ingresado a la finca") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // La madre solo se solicita para nacimientos ocurridos dentro de la finca.
            if (tipoOrigen == "NACIDO_EN_FINCA") {
                item {
                    MotherDropdownField(
                        selectedMotherId = madreId,
                        options = motherOptions,
                        onMotherSelected = { madreId = it }
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
                    onDateSelected = { fechaNacimiento = it },
                    errorMessage = if (intentoGuardar) {
                        errorFechaNacimiento ?: errorNacimientoGuardado
                    } else {
                        errorNacimientoGuardado
                    }
                )
            }

            // La fecha de llegada solo tiene sentido para animales procedentes de otro lugar.
            if (tipoOrigen == "INGRESADO_A_FINCA") {
                item {
                    DateSelectorField(
                        label = "Fecha de llegada *",
                        value = fechaIngreso,
                        onDateSelected = { fechaIngreso = it },
                        errorMessage = if (intentoGuardar) {
                            errorFechaIngreso ?: errorIngresoGuardado
                        } else {
                            errorIngresoGuardado
                        }
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
                    label = { Text("Peso inicial en libras") },
                    suffix = { Text("lb") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = intentoGuardar && pesoInvalido,
                    supportingText = if (intentoGuardar && pesoInvalido) {
                        { Text("Ingrese un peso numérico mayor que cero.") }
                    } else {
                        null
                    },
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

            // Presenta errores de Room, por ejemplo un código duplicado, sin cerrar el formulario.
            item {
                if (saveError != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            saveError,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // El botón forma parte de la lista para que pueda alcanzarse al desplazarse,
            // incluso en pantallas pequeñas o cuando está abierto el teclado.
            item {
                Button(
                    onClick = {
                        intentoGuardar = true
                        if (formularioValido) {
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
                                    observaciones = observaciones,
                                    fotoUri = fotoUri,
                                    madreId = if (tipoOrigen == "NACIDO_EN_FINCA") madreId else "",
                                    proximaParto = sexo == "HEMBRA" && proximaParto
                                )
                            )
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        when {
                            isSaving -> "Guardando..."
                            isEditing -> "Guardar cambios"
                            else -> "Confirmar registro"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/**
 * Lista únicamente las vacas activas marcadas como próximas a parto. La opción vacía
 * permite registrar nacimientos cuyo parentesco todavía no se conoce.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MotherDropdownField(
    selectedMotherId: String,
    options: List<AnimalMotherOption>,
    onMotherSelected: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.id == selectedMotherId }?.label.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            readOnly = true,
            label = { Text("Vaca madre") },
            placeholder = {
                Text(
                    if (options.isEmpty()) "No hay vacas próximas a dar a luz"
                    else "Seleccione la madre"
                )
            },
            supportingText = { Text("Opcional si todavía no se conoce el parentesco.") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Sin madre registrada") },
                onClick = {
                    onMotherSelected("")
                    expanded = false
                }
            )
            options.forEach { mother ->
                DropdownMenuItem(
                    text = { Text(mother.label) },
                    onClick = {
                        onMotherSelected(mother.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Selector de imagen con vista previa, reemplazo y eliminación. */
@Composable
private fun AnimalPhotoSelector(
    photoUri: String,
    onSelectPhoto: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Fotografía del animal", fontWeight = FontWeight.SemiBold)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            AnimalPhoto(
                photoUri = photoUri,
                modifier = Modifier.fillMaxSize(),
                placeholderIcon = Icons.Default.CameraAlt,
                placeholderText = if (photoUri.isBlank()) {
                    "Aún no se ha seleccionado una foto"
                } else {
                    "No fue posible mostrar la foto seleccionada"
                },
                maxDecodeDimensionPx = 900
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onSelectPhoto, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (photoUri.isBlank()) "Seleccionar foto" else "Cambiar foto")
            }
            if (photoUri.isNotBlank()) {
                TextButton(onClick = onRemovePhoto) {
                    Text("Quitar")
                }
            }
        }
        Text(
            "La foto es opcional. Por seguridad, la aplicación solo podrá leer la imagen " +
                "que usted seleccione en el panel protegido de Android.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Selector de raza con valores normalizados para evitar errores de escritura.
 * Incluye razas lecheras, cárnicas, de doble propósito, criollas y mixtas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreedDropdownField(
    value: String,
    onValueSelected: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

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
            label = { Text("Raza") },
            placeholder = { Text("Seleccione una raza bovina") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            bovineBreeds.forEach { breed ->
                DropdownMenuItem(
                    text = { Text(breed) },
                    onClick = {
                        onValueSelected(breed)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Catálogo inicial de razas disponibles durante la etapa de pruebas. */
private val bovineBreeds = listOf(
    "Angus",
    "Ayrshire",
    "Azul Belga",
    "Beefmaster",
    "Brahman",
    "Brangus",
    "Charolais",
    "Chianina",
    "Criolla",
    "Dexter",
    "Gyr",
    "Girolando",
    "Guzerá",
    "Hereford",
    "Holstein",
    "Jersey",
    "Limousin",
    "Mixta",
    "Nelore",
    "Normando",
    "Pardo Suizo",
    "Red Angus",
    "Red Poll",
    "Romagnola",
    "Santa Gertrudis",
    "Senepol",
    "Shorthorn",
    "Simmental",
    "Sindi",
    "Wagyu"
)

/**
 * Campo de fecha que abre un calendario y evita el ingreso manual de formatos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectorField(
    label: String,
    value: String,
    onDateSelected: (String) -> Unit,
    errorMessage: String? = null
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(
                width = if (errorMessage == null) 1.dp else 2.dp,
                color = if (errorMessage == null) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
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
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            // Evita que los botones queden debajo de la barra del sistema en tabletas.
            modifier = Modifier.navigationBarsPadding(),
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(formatDatePickerMillis(millis))
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
                // Se omite el encabezado grande porque la fecha ya se ve seleccionada
                // en el calendario y posteriormente en el campo del formulario.
                title = null,
                headline = null,
                showModeToggle = false
            )
        }
    }
}

/** Convierte las fechas visibles del formulario sin aceptar valores ambiguos. */
private fun parseAnimalFormDate(value: String): Long? {
    if (value.isBlank()) return null
    return runCatching {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            isLenient = false
        }.parse(value)?.time
    }.getOrNull()
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
        AnimalFormScreen(
            codigoGenerado = "FH-2026-001",
            onBack = {},
            onSubmit = {}
        )
    }
}
