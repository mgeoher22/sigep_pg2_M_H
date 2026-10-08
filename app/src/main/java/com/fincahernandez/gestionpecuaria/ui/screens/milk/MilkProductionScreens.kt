package com.fincahernandez.gestionpecuaria.ui.screens.milk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.fincahernandez.gestionpecuaria.ui.components.BrandedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fincahernandez.gestionpecuaria.ui.components.AppBottomBar
import com.fincahernandez.gestionpecuaria.ui.components.CompactDateSelector
import com.fincahernandez.gestionpecuaria.ui.components.todayDateText
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import com.fincahernandez.gestionpecuaria.data.repository.MilkConfiguration
import com.fincahernandez.gestionpecuaria.data.repository.MilkPaymentFrequency
import java.util.Locale

/** Registro diario persistente con el precio histórico aplicado. */
data class MilkProductionUiModel(
    val id: String,
    val dateMillis: Long,
    val date: String,
    val liters: Double,
    val pricePerLiter: Double,
    val paymentDueDateMillis: Long,
    val paymentDueDate: String,
    val paymentConfirmedAtMillis: Long?,
    val notes: String
) {
    /** Ingreso bruto; los costos de producción se tratarán en el módulo financiero. */
    val grossIncome: Double get() = liters * pricePerLiter
}

/** Resumen de una liquidación diaria o semanal que debe confirmar el administrador. */
data class MilkPaymentUiModel(
    val paymentDueDateMillis: Long,
    val paymentDueDate: String,
    val amount: Double,
    val productionRecordCount: Int,
    val confirmed: Boolean,
    val confirmedAtMillis: Long?
)

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
    payments: List<MilkPaymentUiModel>,
    configuration: MilkConfiguration?,
    isGeneralAdministrator: Boolean,
    canConfirmPayments: Boolean,
    notificationsEnabled: Boolean,
    isConfirmingPayment: Boolean,
    paymentActionError: String?,
    onMenuClick: () -> Unit,
    onCreateRecord: () -> Unit,
    onConfigure: () -> Unit,
    onNotificationPreferenceChange: (Boolean) -> Unit,
    onConfirmPayment: (Long) -> Unit,
    onNavigateMain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalLiters = records.sumOf { it.liters }
    val totalGrossIncome = records.sumOf { it.grossIncome }
    val confirmedIncome = records
        .filter { it.paymentConfirmedAtMillis != null }
        .sumOf { it.grossIncome }
    val pendingIncome = records
        .filter { it.paymentConfirmedAtMillis == null }
        .sumOf { it.grossIncome }
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
            // No permite crear registros sin un precio fijo previamente configurado.
            if (configuration != null) {
                ExtendedFloatingActionButton(
                    onClick = onCreateRecord,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Registrar producción") },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            }
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
                        Text("INGRESOS CONFIRMADOS", color = Color.White.copy(alpha = 0.8f))
                        Text(
                            money(confirmedIncome),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Pendiente de confirmar: ${money(pendingIncome)}",
                            color = Color.White
                        )
                    }
                }
            }
            item {
                MilkTermsCard(
                    configuration = configuration,
                    isGeneralAdministrator = isGeneralAdministrator,
                    notificationsEnabled = notificationsEnabled,
                    onConfigure = onConfigure,
                    onNotificationPreferenceChange = onNotificationPreferenceChange
                )
            }
            item {
                Text(
                    "Pagos de leche",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (payments.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text("Aún no hay pagos calculados.", modifier = Modifier.padding(18.dp))
                    }
                }
            } else {
                items(
                    items = payments,
                    key = { "${it.paymentDueDateMillis}-${it.confirmed}" }
                ) { payment ->
                    MilkPaymentCard(
                        payment = payment,
                        canConfirmPayment = canConfirmPayments,
                        isConfirming = isConfirmingPayment,
                        onConfirm = { onConfirmPayment(payment.paymentDueDateMillis) }
                    )
                }
            }
            paymentActionError?.let { error ->
                item { Text(error, color = MaterialTheme.colorScheme.error) }
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

/** Muestra el acuerdo vigente y reserva su edición al Administrador General. */
@Composable
private fun MilkTermsCard(
    configuration: MilkConfiguration?,
    isGeneralAdministrator: Boolean,
    notificationsEnabled: Boolean,
    onConfigure: () -> Unit,
    onNotificationPreferenceChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onNotificationPreferenceChange(granted)
    }

    /** Solicita el permiso de Android solo cuando el usuario decide activar los avisos. */
    val changeNotifications: (Boolean) -> Unit = { enabled ->
        if (
            enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onNotificationPreferenceChange(enabled)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Condiciones de venta de leche",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (configuration == null) {
                Text("Aún no se ha definido el precio fijo ni la frecuencia de pago.")
            } else {
                Text(
                    "Precio fijo: ${money(configuration.pricePerLiter)} por litro",
                    fontWeight = FontWeight.SemiBold
                )
                Text("Pago: ${configuration.paymentFrequency.displayName}")
            }
            if (isGeneralAdministrator) {
                OutlinedButton(onClick = onConfigure) {
                    Text(if (configuration == null) "Configurar" else "Cambiar configuración")
                }
            } else {
                Text(
                    "Solo el Administrador General puede modificar estos valores.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Notificación de pago", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Avisar en la fecha de pago con el monto pendiente en quetzales.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = changeNotifications
                )
            }
        }
    }
}

/** Tarjeta de pago calculada; el ingreso nace únicamente al confirmarla. */
@Composable
private fun MilkPaymentCard(
    payment: MilkPaymentUiModel,
    canConfirmPayment: Boolean,
    isConfirming: Boolean,
    onConfirm: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (payment.confirmed) "PAGO CONFIRMADO" else "PAGO PENDIENTE",
                        color = if (payment.confirmed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Fecha de pago: ${payment.paymentDueDate}")
                }
                Text(
                    money(payment.amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "Incluye ${payment.productionRecordCount} " +
                    if (payment.productionRecordCount == 1) "registro" else "registros"
            )
            if (!payment.confirmed) {
                if (canConfirmPayment) {
                    Button(onClick = onConfirm, enabled = !isConfirming) {
                        Text(if (isConfirming) "Confirmando…" else "Confirmar pago recibido")
                    }
                } else {
                    Text(
                        "Pendiente de confirmación por un rol autorizado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
                    if (record.paymentConfirmedAtMillis == null) {
                        "Monto pendiente: ${money(record.grossIncome)}"
                    } else {
                        "Ingreso confirmado: ${money(record.grossIncome)}"
                    },
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Pago programado: ${record.paymentDueDate}",
                    style = MaterialTheme.typography.bodySmall
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
    configuration: MilkConfiguration?,
    isSaving: Boolean = false,
    saveError: String? = null,
    modifier: Modifier = Modifier
) {
    var date by rememberSaveable { mutableStateOf(todayDateText()) }
    var liters by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (date.isBlank()) date = todayDateText()
    }

    val litersValue = liters.replace(',', '.').toDoubleOrNull()
    val pricePerLiter = configuration?.pricePerLiter?.let(::twoDecimals).orEmpty()
    val priceValue = configuration?.pricePerLiter
    val litersInvalid = liters.isBlank() || litersValue == null || litersValue <= 0
    val priceInvalid = pricePerLiter.isBlank() || priceValue == null || priceValue <= 0
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
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Precio fijo por litro") },
                    prefix = { Text("Q ") },
                    suffix = { Text("/L") },
                    readOnly = true,
                    isError = attemptedSave && priceInvalid,
                    supportingText = if (attemptedSave && priceInvalid) {
                        { Text("El Administrador General debe configurar primero el precio.") }
                    } else {
                        {
                            Text(
                                "Definido por el Administrador General · Pago ${configuration?.paymentFrequency?.displayName.orEmpty()}"
                            )
                        }
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

/** Pantalla administrativa para definir el precio fijo y cuándo paga el comprador. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilkConfigurationScreen(
    configuration: MilkConfiguration?,
    onBack: () -> Unit,
    onSave: (Double, MilkPaymentFrequency) -> Unit,
    isSaving: Boolean = false,
    saveError: String? = null,
    modifier: Modifier = Modifier
) {
    var price by rememberSaveable(configuration?.updatedAt) {
        mutableStateOf(configuration?.pricePerLiter?.let(::twoDecimals).orEmpty())
    }
    var frequencyName by rememberSaveable(configuration?.updatedAt) {
        mutableStateOf(configuration?.paymentFrequency?.name ?: MilkPaymentFrequency.DAILY.name)
    }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }
    val priceValue = price.replace(',', '.').toDoubleOrNull()
    val priceInvalid = priceValue == null || priceValue <= 0.0
    val selectedFrequency = MilkPaymentFrequency.valueOf(frequencyName)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = { Text("Configuración de leche", fontWeight = FontWeight.Bold) },
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
                    "Estos valores se aplicarán automáticamente a los nuevos registros. " +
                        "Los precios históricos no cambiarán.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            item {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Precio fijo por litro *") },
                    prefix = { Text("Q ") },
                    suffix = { Text("/L") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = attemptedSave && priceInvalid,
                    supportingText = if (attemptedSave && priceInvalid) {
                        { Text("Ingrese un precio numérico mayor que cero.") }
                    } else null,
                    singleLine = true
                )
            }
            item {
                Text("¿Cuándo pagan la leche?", fontWeight = FontWeight.SemiBold)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MilkPaymentFrequency.entries.forEach { frequency ->
                        FilterChip(
                            selected = selectedFrequency == frequency,
                            onClick = { frequencyName = frequency.name },
                            label = { Text(frequency.displayName) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item {
                saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Button(
                    onClick = {
                        attemptedSave = true
                        if (!priceInvalid) onSave(checkNotNull(priceValue), selectedFrequency)
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSaving) "Guardando…" else "Guardar configuración")
                }
            }
        }
    }
}

private fun oneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

private fun twoDecimals(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun money(value: Double): String = "Q ${twoDecimals(value)}"
