package com.fincahernandez.gestionpecuaria.ui.screens.reports

import android.Manifest
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.fincahernandez.gestionpecuaria.ui.components.BrandedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.ui.components.AppBottomBar
import com.fincahernandez.gestionpecuaria.ui.components.CompactDateSelector
import com.fincahernandez.gestionpecuaria.data.transfer.BulkExportResult
import com.fincahernandez.gestionpecuaria.data.transfer.BulkImportPreview
import com.fincahernandez.gestionpecuaria.data.transfer.BulkImportResult
import com.fincahernandez.gestionpecuaria.data.transfer.BulkImportMode
import com.fincahernandez.gestionpecuaria.data.transfer.DataTransferModule
import com.fincahernandez.gestionpecuaria.data.transfer.bulkImportTemplateCsv
import com.fincahernandez.gestionpecuaria.data.repository.SupplyCatalog
import com.fincahernandez.gestionpecuaria.notification.ExportedFile
import com.fincahernandez.gestionpecuaria.notification.ExportNotifications
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ReportOption(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

/**
 * Centro de reportes basado en el prototipo y en RF-023, RF-024 y RF-025.
 * Genera vistas previas, documentos PDF y resúmenes CSV compatibles con Excel.
 * Todos los roles con acceso pueden exportar; la importación es administrativa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsCenterScreen(
    data: ReportDashboardData,
    allowedReportIds: Set<String>,
    onMenuClick: () -> Unit,
    onNavigateMain: (String) -> Unit,
    isGeneralAdministrator: Boolean = false,
    canManageUsers: Boolean = false,
    onBulkExport: suspend (Uri, Set<DataTransferModule>) -> BulkExportResult = { _, _ ->
        error("La exportación masiva no está disponible en esta vista previa.")
    },
    onInspectBulkImport: suspend (
        Uri,
        Set<DataTransferModule>,
        BulkImportMode
    ) -> BulkImportPreview = { _, _, _ ->
        error("La importación masiva no está disponible en esta vista previa.")
    },
    onBulkImport: suspend (
        Uri,
        Set<DataTransferModule>,
        BulkImportMode
    ) -> BulkImportResult = { _, _, _ ->
        error("La importación masiva no está disponible en esta vista previa.")
    },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val visibleReportOptions = reportOptions.filter { it.id in allowedReportIds }
    val visibleTransferModules = DataTransferModule.entries.filter { module ->
        when (module) {
            DataTransferModule.ANIMALS -> "animals" in allowedReportIds
            DataTransferModule.PARCELS -> "parcels" in allowedReportIds
            DataTransferModule.LOTS -> "lots" in allowedReportIds
            DataTransferModule.WEIGHINGS -> "weighings" in allowedReportIds
            DataTransferModule.MILK_PRODUCTION,
            DataTransferModule.MILK_CONFIGURATION -> "production" in allowedReportIds
            DataTransferModule.SANITARY -> "sanitary" in allowedReportIds
            DataTransferModule.SUPPLIES -> "supplies" in allowedReportIds
            DataTransferModule.EMPLOYEES -> "staff" in allowedReportIds
            DataTransferModule.FINANCE -> "financial" in allowedReportIds
            DataTransferModule.USERS -> canManageUsers
        }
    }
    if (visibleReportOptions.isEmpty()) {
        EmptyReportsScreen(
            onMenuClick = onMenuClick,
            onNavigateMain = onNavigateMain,
            modifier = modifier
        )
        return
    }
    var selectedPeriod by rememberSaveable { mutableStateOf("Mensual") }
    var periodAnchorMillis by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var customStartDate by rememberSaveable { mutableStateOf("") }
    var customEndDate by rememberSaveable { mutableStateOf("") }
    var selectedReportId by rememberSaveable {
        mutableStateOf(visibleReportOptions.first().id)
    }
    var generatedPreviewId by rememberSaveable { mutableStateOf<String?>(null) }
    var operationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var operationTitle by rememberSaveable { mutableStateOf("Operación completada") }
    var isTransferring by remember { mutableStateOf(false) }
    var pendingBulkImportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingBulkImportPreview by remember { mutableStateOf<BulkImportPreview?>(null) }
    var pendingBulkImportModules by remember { mutableStateOf<Set<DataTransferModule>>(emptySet()) }
    var pendingBulkImportMode by remember { mutableStateOf(BulkImportMode.MERGE) }
    var selectedBulkImportMode by remember { mutableStateOf(BulkImportMode.MERGE) }
    var selectedTransferModule by remember(visibleTransferModules) {
        mutableStateOf(visibleTransferModules.firstOrNull())
    }
    var transferModuleMenuExpanded by remember { mutableStateOf(false) }
    val selectedTransferModules = selectedTransferModule?.let(::setOf).orEmpty()

    val selectedReport = visibleReportOptions.first { it.id == selectedReportId }
    val generatedReport = visibleReportOptions.firstOrNull { it.id == generatedPreviewId }
    val effectiveData = data
    val selectedRange = remember(
        selectedPeriod,
        periodAnchorMillis,
        customStartDate,
        customEndDate
    ) {
        resolveReportDateRange(
            period = selectedPeriod,
            anchorMillis = periodAnchorMillis,
            customStart = customStartDate,
            customEnd = customEndDate
        )
    }
    val selectedAnalysis = remember(effectiveData, selectedReport, selectedRange) {
        selectedRange?.let { range ->
            buildReportAnalysis(effectiveData, selectedReport.id, selectedReport.title, range)
        }
    }
    var pendingExportNotification by remember { mutableStateOf<ExportedFile?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingExportNotification?.let { file ->
            if (granted) ExportNotifications.show(context, file)
        }
        pendingExportNotification = null
    }
    val notifyExportedFile: (Uri, String) -> Unit = { uri, mimeType ->
        val file = ExportedFile(uri, mimeType)
        if (ExportNotifications.hasRuntimePermission(context)) {
            ExportNotifications.show(context, file)
        } else {
            pendingExportNotification = file
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Exporta las métricas y todos los registros incluidos en el rango seleccionado. */
    val reportCsvExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val analysis = selectedAnalysis
        if (uri != null && analysis != null) {
            coroutineScope.launch {
                isTransferring = true
                runCatching { writeReportAnalysisCsv(context, uri, analysis) }
                    .onSuccess {
                        notifyExportedFile(uri, "text/csv")
                        operationTitle = "Reporte exportado"
                        operationMessage =
                            "Las métricas y ${analysis.allRecords.size} registros se guardaron para Excel."
                    }
                    .onFailure {
                        operationTitle = "No fue posible exportar"
                        operationMessage = "Verifique la ubicación elegida e inténtelo nuevamente."
                    }
                isTransferring = false
            }
        }
    }

    /** Genera el informe visible en un documento PDF real. */
    val pdfExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { exportedUri ->
            coroutineScope.launch {
                isTransferring = true
                runCatching {
                    val analysis = requireNotNull(selectedAnalysis) {
                        "Seleccione un rango de fechas válido."
                    }
                    writeReportPdf(context = context, uri = exportedUri, analysis = analysis)
                }.onSuccess {
                    notifyExportedFile(exportedUri, "application/pdf")
                    operationTitle = "PDF generado"
                    operationMessage = "El reporte se guardó correctamente."
                }.onFailure {
                    operationTitle = "No fue posible crear el PDF"
                    operationMessage = "Verifique la ubicación elegida e inténtelo nuevamente."
                }
                isTransferring = false
            }
        }
    }

    /** Exporta los registros reales de los módulos seleccionados para editarlos en Excel. */
    val bulkDataExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { exportedUri ->
            coroutineScope.launch {
                isTransferring = true
                runCatching {
                    onBulkExport(exportedUri, selectedTransferModules)
                }.onSuccess { result ->
                    notifyExportedFile(exportedUri, "text/csv")
                    operationTitle = "Datos exportados"
                    operationMessage = result.successMessage()
                }.onFailure { error ->
                    operationTitle = "No fue posible exportar"
                    operationMessage = error.message
                        ?: "Verifique la ubicación elegida e inténtelo nuevamente."
                }
                isTransferring = false
            }
        }
    }

    /** Descarga una plantilla con ejemplos solo de los módulos seleccionados. */
    val bulkTemplateExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { exportedUri ->
            coroutineScope.launch {
                isTransferring = true
                runCatching {
                    writeBulkImportTemplate(context, exportedUri, selectedTransferModules)
                }
                    .onSuccess {
                        notifyExportedFile(exportedUri, "text/csv")
                        operationTitle = "Plantilla descargada"
                        operationMessage =
                            "Complete la plantilla con Excel sin cambiar sus encabezados."
                    }
                    .onFailure {
                        operationTitle = "No fue posible guardar la plantilla"
                        operationMessage = "Verifique la ubicación elegida e inténtelo nuevamente."
                    }
                isTransferring = false
            }
        }
    }

    /** Primero inspecciona el archivo; la escritura requiere una confirmación posterior. */
    val bulkImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.takeIf { isGeneralAdministrator }?.let {
            coroutineScope.launch {
                isTransferring = true
                runCatching {
                    onInspectBulkImport(it, selectedTransferModules, selectedBulkImportMode)
                }
                    .mapCatching { preview ->
                        require(preview.totalCount > 0) {
                            "El archivo no contiene filas para importar."
                        }
                        preview
                    }
                    .onSuccess { preview ->
                        pendingBulkImportUri = it
                        pendingBulkImportPreview = preview
                        pendingBulkImportModules = selectedTransferModules
                        pendingBulkImportMode = selectedBulkImportMode
                    }
                    .onFailure { error ->
                        operationTitle = "Importación no válida"
                        operationMessage = error.message
                            ?: "Revise la plantilla e inténtelo nuevamente."
                    }
                isTransferring = false
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = {
                    Text(
                        "Centro de reportes",
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
            AppBottomBar(selectedRoute = Routes.REPORTS, onNavigate = onNavigateMain)
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
                Text(
                    "Seleccione el periodo y el tipo de informe que desea consultar.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null)
                            Column {
                                Text(
                                    "Archivos de datos",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (isGeneralAdministrator) {
                                        "Puede exportar e importar archivos CSV compatibles con Excel."
                                    } else {
                                        "Puede exportar archivos CSV compatibles con Excel."
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        Text(
                            "Seleccione un módulo. La exportación está disponible para todos " +
                                "los usuarios con acceso a Reportes; la importación solo para el " +
                                "Administrador General.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        ExposedDropdownMenuBox(
                            expanded = transferModuleMenuExpanded,
                            onExpandedChange = {
                                if (!isTransferring) transferModuleMenuExpanded = !transferModuleMenuExpanded
                            }
                        ) {
                            OutlinedTextField(
                                value = selectedTransferModule?.displayName.orEmpty(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Módulo de datos") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(transferModuleMenuExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, !isTransferring)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = transferModuleMenuExpanded,
                                onDismissRequest = { transferModuleMenuExpanded = false }
                            ) {
                                visibleTransferModules.forEach { module ->
                                    DropdownMenuItem(
                                        text = { Text(module.displayName) },
                                        onClick = {
                                            selectedTransferModule = module
                                            selectedBulkImportMode = BulkImportMode.MERGE
                                            transferModuleMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = {
                                if (selectedTransferModules.isEmpty()) {
                                    operationTitle = "Seleccione módulos"
                                    operationMessage = "Seleccione al menos un módulo para exportar."
                                } else {
                                    bulkDataExporter.launch(bulkDataFileName())
                                }
                            },
                            enabled = !isTransferring,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Text(" Exportar datos actuales")
                        }
                        OutlinedButton(
                            onClick = {
                                if (selectedTransferModules.isEmpty()) {
                                    operationTitle = "Seleccione módulos"
                                    operationMessage = "Seleccione al menos un módulo para la plantilla."
                                } else {
                                    bulkTemplateExporter.launch(bulkTemplateFileName())
                                }
                            },
                            enabled = !isTransferring,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Text(" Descargar plantilla")
                        }
                        if (isGeneralAdministrator) {
                            if (DataTransferModule.ANIMALS in selectedTransferModules) {
                                FilterChip(
                                    selected = selectedBulkImportMode == BulkImportMode.INITIAL_LOAD_REPLACE,
                                    onClick = {
                                        selectedBulkImportMode = if (
                                            selectedBulkImportMode == BulkImportMode.MERGE
                                        ) BulkImportMode.INITIAL_LOAD_REPLACE else BulkImportMode.MERGE
                                    },
                                    label = { Text("Carga inicial: reemplazar historial anterior") },
                                    enabled = !isTransferring,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (selectedBulkImportMode == BulkImportMode.INITIAL_LOAD_REPLACE) {
                                    Text(
                                        "Se reemplazarán pesajes, controles sanitarios y asignaciones " +
                                            "anteriores de los códigos incluidos.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Text(
                                "Las columnas terminadas en NoEditar conservan la identidad de " +
                                    "cada registro y evitan duplicados.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    if (selectedTransferModules.isEmpty()) {
                                        operationTitle = "Seleccione módulos"
                                        operationMessage = "Seleccione al menos un módulo para importar."
                                    } else {
                                        bulkImporter.launch(
                                            arrayOf(
                                                "text/csv",
                                                "text/comma-separated-values",
                                                "application/vnd.ms-excel",
                                                "text/plain"
                                            )
                                        )
                                    }
                                },
                                enabled = !isTransferring,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null)
                                Text(" Importar datos")
                            }
                        }
                    }
                }
            }
            item {
                Text("Periodo", fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("30 días", "Mensual", "Anual", "Personalizado")
                        .chunked(2)
                        .forEach { periods ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                periods.forEach { period ->
                                    FilterChip(
                                        selected = selectedPeriod == period,
                                        onClick = {
                                            selectedPeriod = period
                                            periodAnchorMillis = System.currentTimeMillis()
                                            generatedPreviewId = null
                                        },
                                        label = { Text(period) },
                                        leadingIcon = if (selectedPeriod == period) {
                                            {
                                                Icon(
                                                    Icons.Default.CalendarMonth,
                                                    contentDescription = null
                                                )
                                            }
                                        } else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            if (selectedPeriod == "Mensual" || selectedPeriod == "Anual") {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    periodAnchorMillis = shiftReportPeriodAnchor(
                                        selectedPeriod,
                                        periodAnchorMillis,
                                        -1
                                    )
                                    generatedPreviewId = null
                                }
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Período anterior")
                            }
                            Text(
                                selectedRange?.label.orEmpty(),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center
                            )
                            IconButton(
                                onClick = {
                                    periodAnchorMillis = shiftReportPeriodAnchor(
                                        selectedPeriod,
                                        periodAnchorMillis,
                                        1
                                    )
                                    generatedPreviewId = null
                                },
                                enabled = canMoveReportPeriodForward(
                                    selectedPeriod,
                                    periodAnchorMillis
                                )
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Período siguiente")
                            }
                        }
                    }
                }
            }
            if (selectedPeriod == "Personalizado") {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CompactDateSelector(
                            label = "Fecha inicial",
                            value = customStartDate,
                            onDateSelected = {
                                customStartDate = it
                                generatedPreviewId = null
                            },
                            showError = selectedRange == null && customStartDate.isBlank(),
                            modifier = Modifier.weight(1f)
                        )
                        CompactDateSelector(
                            label = "Fecha final",
                            value = customEndDate,
                            onDateSelected = {
                                customEndDate = it
                                generatedPreviewId = null
                            },
                            showError = selectedRange == null && customEndDate.isBlank(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (customStartDate.isNotBlank() && customEndDate.isNotBlank() &&
                        selectedRange == null
                    ) {
                        Text(
                            "La fecha inicial no puede ser posterior a la fecha final.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item {
                Text(
                    "Reportes disponibles",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            items(visibleReportOptions, key = { it.id }) { report ->
                val selected = report.id == selectedReportId
                val reportAnalysis = selectedRange?.let { range ->
                    buildReportAnalysis(effectiveData, report.id, report.title, range)
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedReportId = report.id
                            generatedPreviewId = null
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            report.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(report.title, fontWeight = FontWeight.Bold)
                            Text(report.description, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            reportAnalysis?.headline ?: "Seleccione fechas",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = { generatedPreviewId = selectedReport.id },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = selectedRange != null
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null)
                    Spacer(modifier = Modifier.padding(horizontal = 5.dp))
                    Text("Generar vista previa", fontWeight = FontWeight.Bold)
                }
            }
            generatedReport?.let { report ->
                val analysis = selectedRange?.let { range ->
                    buildReportAnalysis(effectiveData, report.id, report.title, range)
                }
                if (analysis == null) return@let
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "VISTA PREVIA • ${analysis.periodLabel}",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                report.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                analysis.headline,
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                analysis.headlineLabel,
                                color = Color.White
                            )
                        }
                    }
                }
                item {
                    analysis.financialStatement?.takeIf {
                        it.expenseCategories.isNotEmpty()
                    }?.let { statement ->
                        FinancialExpenseDistributionCard(statement)
                    }
                }
                item {
                    Text(
                        "Métricas para la toma de decisiones",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                analysis.metrics.chunked(2).forEach { metricRow ->
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            metricRow.forEach { metric ->
                                ReportMetricCard(metric = metric, modifier = Modifier.weight(1f))
                            }
                            if (metricRow.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Text(
                        "10 registros más recientes del rango",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (analysis.recentRecords.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "No existen registros de este módulo dentro del rango seleccionado.",
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                } else {
                    items(
                        analysis.recentRecords,
                        key = { "report-${analysis.reportId}-${it.id}" }
                    ) { record ->
                        ReportRecordCard(record)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                pdfExporter.launch(
                                    reportFileName(
                                        reportTitle = report.title,
                                        extension = "pdf"
                                    )
                                )
                            },
                            enabled = !isTransferring,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Text(" PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                reportCsvExporter.launch(
                                    reportFileName(report.title, "csv")
                                )
                            },
                            enabled = !isTransferring,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null)
                            Text(" Excel")
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    pendingBulkImportPreview?.let { preview ->
        val replacesInitialData = pendingBulkImportMode == BulkImportMode.INITIAL_LOAD_REPLACE
        AlertDialog(
            onDismissRequest = {
                pendingBulkImportUri = null
                pendingBulkImportPreview = null
                pendingBulkImportModules = emptySet()
                pendingBulkImportMode = BulkImportMode.MERGE
            },
            title = {
                Text(if (replacesInitialData) "Confirmar carga inicial" else "Confirmar importación masiva")
            },
            text = {
                Text(preview.confirmationMessage(replacesInitialData))
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingBulkImportUri = null
                    pendingBulkImportPreview = null
                    pendingBulkImportModules = emptySet()
                    pendingBulkImportMode = BulkImportMode.MERGE
                }) { Text("Cancelar") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingBulkImportUri ?: return@Button
                        val modules = pendingBulkImportModules
                        val mode = pendingBulkImportMode
                        pendingBulkImportUri = null
                        pendingBulkImportPreview = null
                        pendingBulkImportModules = emptySet()
                        pendingBulkImportMode = BulkImportMode.MERGE
                        coroutineScope.launch {
                            isTransferring = true
                            runCatching {
                                check(isGeneralAdministrator) {
                                    "Solo el Administrador General puede importar datos."
                                }
                                onBulkImport(uri, modules, mode)
                            }.onSuccess { result ->
                                operationTitle = "Importación completada"
                                operationMessage = result.successMessage()
                            }.onFailure { error ->
                                operationTitle = "No se importaron los datos"
                                operationMessage = error.message
                                    ?: "La operación fue cancelada sin guardar filas."
                            }
                            isTransferring = false
                        }
                    }
                ) { Text(if (replacesInitialData) "Reemplazar e importar" else "Importar") }
            }
        )
    }

    operationMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { operationMessage = null },
            title = { Text(operationTitle) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { operationMessage = null }) { Text("Entendido") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyReportsScreen(
    onMenuClick: () -> Unit,
    onNavigateMain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BrandedTopAppBar(
                title = { Text("Centro de reportes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                    }
                }
            )
        },
        bottomBar = {
            AppBottomBar(selectedRoute = Routes.REPORTS, onNavigate = onNavigateMain)
        }
    ) { innerPadding ->
        Card(
            modifier = Modifier
                .padding(innerPadding)
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("No hay reportes autorizados", fontWeight = FontWeight.Bold)
                Text(
                    "La cuenta puede abrir el centro, pero no tiene permiso para consultar " +
                        "ningún módulo de datos. Solicite al Administrador General que revise sus permisos."
                )
            }
        }
    }
}

@Composable
private fun FinancialExpenseDistributionCard(statement: FinancialStatement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Distribución de egresos por categoría",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            statement.expenseCategories.forEach { share ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(share.category, modifier = Modifier.weight(1f))
                        Text(
                            "${formatMoney(share.amount)} · ${formatPercentage(share.percentage)}",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (share.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportMetricCard(metric: ReportMetric, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(metric.label, style = MaterialTheme.typography.labelLarge)
            Text(
                metric.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(metric.explanation, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatMoney(value: Double): String =
    "Q ${NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 2 }.format(value)}"

private fun formatPercentage(value: Double): String =
    "${NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 1 }.format(value)} %"

@Composable
private fun ReportRecordCard(record: ReportRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.title, fontWeight = FontWeight.SemiBold)
                Text(record.detail, style = MaterialTheme.typography.bodySmall)
                Text(
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(Date(record.dateMillis)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            record.primaryValue?.let { value ->
                Text(
                    formatRecordValue(record, value),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private val reportOptions = listOf(
    ReportOption(
        id = "financial",
        title = "Reportes financieros",
        description = "Estado de resultados, egresos por categoría y comparación con el período anterior.",
        icon = Icons.Default.AccountBalance
    ),
    ReportOption(
        id = "production",
        title = "Producción",
        description = "Litros, promedio diario, precio e ingreso bruto.",
        icon = Icons.Default.LocalDrink
    ),
    ReportOption(
        id = "staff",
        title = "Personal",
        description = "Altas de personal, pagos y costos de nómina.",
        icon = Icons.Default.Badge
    ),
    ReportOption(
        id = "animals",
        title = "Inventario animal",
        description = "Altas de animales, sexo, peso e inventario activo.",
        icon = Icons.Default.Pets
    ),
    ReportOption(
        id = "births",
        title = "Partos y nacimientos",
        description = "Partos, crías nacidas en finca y relaciones pendientes de madre.",
        icon = Icons.Default.Pets
    ),
    ReportOption(
        id = "weighings",
        title = "Pesajes",
        description = "Peso promedio, ganancias y pérdidas de peso.",
        icon = Icons.Default.Scale
    ),
    ReportOption(
        id = "lots",
        title = "Lotes",
        description = "Nuevos lotes, estado y cantidad de animales.",
        icon = Icons.Default.Groups
    ),
    ReportOption(
        id = "parcels",
        title = "Parcelas",
        description = "Superficie, capacidad y ocupación de terrenos.",
        icon = Icons.Default.Landscape
    ),
    ReportOption(
        id = "sanitary",
        title = "Sanidad",
        description = "Atenciones, animales tratados, dosis y próximos controles.",
        icon = Icons.Default.Assessment
    ),
    ReportOption(
        id = "supplies",
        title = "Insumos",
        description = "Compras, consumos, préstamos y valor del inventario.",
        icon = Icons.Default.TableChart
    )
)

private fun formatRecordValue(record: ReportRecord, value: Double): String {
    val number = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 2
    }.format(value)
    return when (record.reportId) {
        "financial", "staff" -> "Q $number"
        "production" -> "$number L"
        "animals", "weighings" -> "$number lb"
        "lots" -> "$number animales"
        "parcels" -> "$number ha"
        "sanitary" -> if (value > 0.0) "$number ml" else number
        "supplies" -> record.attributes["unit"]
            ?.let(SupplyCatalog.units::get)
            ?.let { "$number ${it.lowercase()}" }
            ?: number
        else -> if (record.kind.isBlank()) number else "$number ${record.kind}"
    }
}

private const val REPORT_CSV_FORMAT = "gestion_pecuaria_reportes_v1"
private const val MAX_IMPORTED_REPORT_CHARS = 1_000_000

/** Crea un nombre reconocible y evita caracteres no admitidos por algunos proveedores. */
private fun reportFileName(reportTitle: String, extension: String): String {
    val safeTitle = reportTitle
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
    return "${safeTitle}_$timestamp.$extension"
}

private fun reportCsvFileName(): String =
    reportFileName(reportTitle = "resumen_gestion_pecuaria", extension = "csv")

private fun bulkTemplateFileName(): String =
    reportFileName(reportTitle = "plantilla_importacion_masiva", extension = "csv")

private fun bulkDataFileName(): String =
    reportFileName(reportTitle = "datos_gestion_pecuaria", extension = "csv")

private fun BulkExportResult.successMessage(): String =
    if (moduleCounts.isNotEmpty()) {
        "Se exportaron ${moduleCounts.entries.joinToString { "${it.value} registros de ${it.key.displayName}" }}. " +
            "Puede editar el archivo en Excel e importarlo nuevamente."
    } else {
        "Se exportaron $animalCount animales, $weighingCount pesajes y " +
            "$sanitaryCount registros sanitarios. Puede editar el archivo en Excel e importarlo nuevamente."
    }

private fun BulkImportResult.successMessage(): String = buildString {
    if (moduleCounts.isNotEmpty()) {
        append("Se importaron ")
        append(moduleCounts.entries.joinToString { "${it.value} registros de ${it.key.displayName}" })
        append(".")
        return@buildString
    }
    append("Se crearon $animalsCreated animales, se actualizaron $animalsUpdated, ")
    append("se importaron $weighingsImported pesajes y ")
    append("$sanitaryRecordsImported registros sanitarios.")
    if (animalHistoriesReplaced > 0) {
        append(" Se reemplazó el historial anterior de $animalHistoriesReplaced animales.")
    }
}

private fun BulkImportPreview.confirmationMessage(replacesInitialData: Boolean): String {
    if (moduleCounts.isNotEmpty()) {
        return "Se procesarán ${moduleCounts.entries.joinToString { "${it.value} registros de ${it.key.displayName}" }}. " +
            "Los identificadores conservados actualizarán registros existentes sin duplicarlos."
    }
    return "Se procesarán $animalCount animales, $weighingCount pesajes y " +
        "$sanitaryCount registros sanitarios. " + if (replacesInitialData) {
        "Se conservarán sus códigos, pero se eliminará definitivamente " +
            "el historial anterior de los animales incluidos."
    } else {
        "Los animales cuyo código ya exista serán actualizados sin borrar su historial."
    }
}

/**
 * Formato sencillo y estable que puede abrir Excel y que la aplicación puede validar
 * antes de mostrarlo nuevamente. La contraseña y otros datos de usuarios nunca se exportan.
 */
internal fun ReportDashboardData.toReportCsv(): String = buildString {
    appendLine("campo;valor;descripcion")
    appendLine("formato;$REPORT_CSV_FORMAT;Versión del archivo")
    appendLine("animalCount;$animalCount;Animales activos")
    appendLine("lotCount;$lotCount;Lotes registrados")
    appendLine("parcelCount;$parcelCount;Parcelas registradas")
    appendLine("weighingCount;$weighingCount;Pesajes registrados")
    appendLine("milkLiters;$milkLiters;Litros de leche")
    appendLine("employeeCount;$employeeCount;Empleados activos")
    appendLine("financialBalance;$financialBalance;Balance financiero en quetzales")
    appendLine(
        "generatedAt;${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())};" +
            "Fecha de exportación"
    )
}

/** Interpreta únicamente el formato generado por esta aplicación. */
internal fun parseReportCsv(csv: String): ReportDashboardData {
    val values = csv
        .removePrefix("\uFEFF")
        .lineSequence()
        .drop(1)
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split(';', limit = 3)
            if (parts.size < 2) null else parts[0].trim() to parts[1].trim()
        }
        .toMap()

    require(values["formato"] == REPORT_CSV_FORMAT) {
        "El archivo no fue exportado por el módulo de reportes o usa otra versión."
    }

    fun requiredInt(key: String, label: String): Int {
        val value = values[key]?.toIntOrNull()
        require(value != null && value >= 0) { "El valor de $label no es válido." }
        return value
    }

    fun requiredNonNegativeDouble(key: String, label: String): Double {
        val value = values[key]?.toDoubleOrNull()
        require(value != null && value.isFinite() && value >= 0.0) {
            "El valor de $label no es válido."
        }
        return value
    }

    val balance = values["financialBalance"]?.toDoubleOrNull()
    require(balance != null && balance.isFinite()) { "El balance financiero no es válido." }

    return ReportDashboardData(
        animalCount = requiredInt("animalCount", "animales"),
        lotCount = requiredInt("lotCount", "lotes"),
        parcelCount = requiredInt("parcelCount", "parcelas"),
        weighingCount = requiredInt("weighingCount", "pesajes"),
        milkLiters = requiredNonNegativeDouble("milkLiters", "producción"),
        employeeCount = requiredInt("employeeCount", "empleados"),
        financialBalance = balance
    )
}

/** Escribe el CSV fuera del hilo de interfaz para no trabar el desplazamiento. */
private suspend fun writeReportCsv(
    context: Context,
    uri: Uri,
    data: ReportDashboardData
) = withContext(Dispatchers.IO) {
    val output = context.contentResolver.openOutputStream(uri, "wt")
        ?: error("No fue posible abrir el archivo seleccionado.")
    output.bufferedWriter(Charsets.UTF_8).use { writer ->
        // La marca UTF-8 permite que Excel conserve correctamente tildes y eñes.
        writer.write("\uFEFF")
        writer.write(data.toReportCsv())
    }
}

/** Guarda las métricas y todos los registros del rango; la vista solo resume los últimos diez. */
private suspend fun writeReportAnalysisCsv(
    context: Context,
    uri: Uri,
    analysis: ReportAnalysis
) = withContext(Dispatchers.IO) {
    val output = context.contentResolver.openOutputStream(uri, "wt")
        ?: error("No fue posible abrir el archivo seleccionado.")
    output.bufferedWriter(Charsets.UTF_8).use { writer ->
        writer.write("\uFEFF")
        writer.write(analysis.toAnalysisCsv())
    }
}

/** Serialización verificable que conserva estado de resultados, comparación y categorías. */
internal fun ReportAnalysis.toAnalysisCsv(): String = buildString {
    appendLine("seccion;campo;valor;detalle")
    appendLine("reporte;titulo;${title.toCsvCell()};")
    appendLine("reporte;periodo;${periodLabel.toCsvCell()};")
    appendLine(
        "reporte;indicador_principal;${headline.toCsvCell()};" +
            headlineLabel.toCsvCell()
    )
    metrics.forEach { metric ->
        appendLine(
            "metrica;${metric.label.toCsvCell()};${metric.value.toCsvCell()};" +
                metric.explanation.toCsvCell()
        )
    }
    financialStatement?.expenseCategories?.forEach { share ->
        appendLine(
            "egreso_categoria;${share.category.toCsvCell()};${share.amount};" +
                "${formatPercentage(share.percentage).toCsvCell()}"
        )
    }
    appendLine("registro;fecha;nombre;detalle;tipo;valor_principal;valor_secundario")
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    allRecords.forEach { record ->
        appendLine(
            "registro;${dateFormatter.format(Date(record.dateMillis))};" +
                "${record.title.toCsvCell()};${record.detail.toCsvCell()};" +
                "${record.kind.toCsvCell()};${record.primaryValue?.toString().orEmpty()};" +
                record.secondaryValue?.toString().orEmpty()
        )
    }
}

private fun String.toCsvCell(): String =
    replace(";", ",").replace("\r", " ").replace("\n", " ").trim()

/** Guarda la plantilla administrativa con los módulos escogidos. */
private suspend fun writeBulkImportTemplate(
    context: Context,
    uri: Uri,
    modules: Set<DataTransferModule>
) =
    withContext(Dispatchers.IO) {
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: error("No fue posible abrir el archivo seleccionado.")
        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write("\uFEFF")
            writer.write(bulkImportTemplateCsv(modules))
        }
    }

/** Lee con un límite preventivo para rechazar archivos demasiado grandes. */
private suspend fun readReportCsv(context: Context, uri: Uri): ReportDashboardData =
    withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
            ?: error("No fue posible abrir el archivo seleccionado.")
        val text = input.bufferedReader(Charsets.UTF_8).use { reader ->
            val content = StringBuilder()
            val buffer = CharArray(4_096)
            while (true) {
                val read = reader.read(buffer)
                if (read < 0) break
                content.append(buffer, 0, read)
                require(content.length <= MAX_IMPORTED_REPORT_CHARS) {
                    "El archivo es demasiado grande para un resumen de reportes."
                }
            }
            content.toString()
        }
        parseReportCsv(text)
    }

/** Construye un PDF liviano con el reporte seleccionado. */
private suspend fun writeReportPdf(
    context: Context,
    uri: Uri,
    analysis: ReportAnalysis
) = withContext(Dispatchers.IO) {
    val document = PdfDocument()
    try {
        val page = document.startPage(
            PdfDocument.PageInfo.Builder(595, 842, 1).create()
        )
        val canvas = page.canvas
        val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(0, 96, 45)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(32, 32, 32)
        }

        canvas.drawColor(android.graphics.Color.WHITE)
        primaryPaint.textSize = 25f
        primaryPaint.isFakeBoldText = true
        canvas.drawText("Finca Hernández", 48f, 70f, primaryPaint)

        textPaint.textSize = 14f
        canvas.drawText("Sistema de Gestión Pecuaria", 48f, 95f, textPaint)
        canvas.drawLine(48f, 115f, 547f, 115f, primaryPaint)

        textPaint.textSize = 22f
        textPaint.isFakeBoldText = true
        canvas.drawText(analysis.title, 48f, 165f, textPaint)
        textPaint.isFakeBoldText = false
        textPaint.textSize = 14f
        canvas.drawText("Periodo: ${analysis.periodLabel}", 48f, 195f, textPaint)

        primaryPaint.textSize = 32f
        primaryPaint.isFakeBoldText = true
        canvas.drawText(analysis.headline, 48f, 250f, primaryPaint)

        textPaint.textSize = 13f
        canvas.drawText(analysis.headlineLabel, 48f, 278f, textPaint)

        var y = 325f
        textPaint.isFakeBoldText = true
        textPaint.textSize = 15f
        canvas.drawText("Métricas", 48f, y, textPaint)
        y += 28f
        textPaint.textSize = 12f
        analysis.metrics.forEach { metric ->
            textPaint.isFakeBoldText = true
            canvas.drawText("${metric.label}: ${metric.value}", 48f, y, textPaint)
            textPaint.isFakeBoldText = false
            canvas.drawText(metric.explanation.fitPdfLine(textPaint), 285f, y, textPaint)
            y += 24f
        }

        analysis.financialStatement?.expenseCategories?.takeIf { it.isNotEmpty() }?.let { shares ->
            y += 14f
            textPaint.isFakeBoldText = true
            textPaint.textSize = 15f
            canvas.drawText("Distribución de egresos por categoría", 48f, y, textPaint)
            y += 26f
            textPaint.textSize = 11f
            shares.forEach { share ->
                textPaint.isFakeBoldText = false
                canvas.drawText(share.category.fitPdfLine(textPaint, 250f), 48f, y, textPaint)
                textPaint.isFakeBoldText = true
                canvas.drawText(
                    "${formatMoney(share.amount)} · ${formatPercentage(share.percentage)}",
                    330f,
                    y,
                    textPaint
                )
                y += 21f
            }
        }
        canvas.drawText(
            "Generado: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}",
            48f,
            780f,
            textPaint
        )
        document.finishPage(page)

        val detailPage = document.startPage(
            PdfDocument.PageInfo.Builder(595, 842, 2).create()
        )
        val detailCanvas = detailPage.canvas
        detailCanvas.drawColor(android.graphics.Color.WHITE)
        primaryPaint.textSize = 22f
        primaryPaint.isFakeBoldText = true
        detailCanvas.drawText("Registros del reporte", 48f, 70f, primaryPaint)
        textPaint.isFakeBoldText = false
        textPaint.textSize = 13f
        detailCanvas.drawText(analysis.title, 48f, 96f, textPaint)
        detailCanvas.drawText("Período: ${analysis.periodLabel}", 48f, 118f, textPaint)
        detailCanvas.drawLine(48f, 135f, 547f, 135f, primaryPaint)
        var detailY = 175f
        textPaint.textSize = 11f
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        if (analysis.recentRecords.isEmpty()) {
            detailCanvas.drawText(
                "No existen registros dentro del rango seleccionado.",
                48f,
                detailY,
                textPaint
            )
        } else {
            analysis.recentRecords.forEachIndexed { index, record ->
                textPaint.isFakeBoldText = true
                detailCanvas.drawText(
                    "${index + 1}. ${dateFormatter.format(Date(record.dateMillis))}  " +
                        record.title.fitPdfLine(textPaint, 300f),
                    48f,
                    detailY,
                    textPaint
                )
                textPaint.isFakeBoldText = false
                detailCanvas.drawText(
                    record.detail.fitPdfLine(textPaint, 455f),
                    70f,
                    detailY + 19f,
                    textPaint
                )
                detailY += 52f
            }
        }
        document.finishPage(detailPage)

        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: error("No fue posible abrir el archivo seleccionado.")
        output.use(document::writeTo)
    } finally {
        document.close()
    }
}

/** Recorta texto según el ancho real de la tipografía para evitar que salga del PDF. */
private fun String.fitPdfLine(paint: Paint, maxWidth: Float = 250f): String {
    if (paint.measureText(this) <= maxWidth) return this
    val suffix = "…"
    val count = paint.breakText(this, true, maxWidth - paint.measureText(suffix), null)
    return take(count.coerceAtLeast(0)) + suffix
}
