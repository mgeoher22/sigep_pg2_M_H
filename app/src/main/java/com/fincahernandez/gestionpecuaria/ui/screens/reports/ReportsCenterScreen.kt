package com.fincahernandez.gestionpecuaria.ui.screens.reports

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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.fincahernandez.gestionpecuaria.ui.components.AppBottomBar
import com.fincahernandez.gestionpecuaria.ui.components.CompactDateSelector
import com.fincahernandez.gestionpecuaria.data.transfer.BulkExportResult
import com.fincahernandez.gestionpecuaria.data.transfer.BulkImportPreview
import com.fincahernandez.gestionpecuaria.data.transfer.BulkImportResult
import com.fincahernandez.gestionpecuaria.data.transfer.BulkImportMode
import com.fincahernandez.gestionpecuaria.data.transfer.DataTransferModule
import com.fincahernandez.gestionpecuaria.data.transfer.bulkImportTemplateCsv
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
    if (visibleReportOptions.isEmpty()) {
        EmptyReportsScreen(
            onMenuClick = onMenuClick,
            onNavigateMain = onNavigateMain,
            modifier = modifier
        )
        return
    }
    var selectedPeriod by rememberSaveable { mutableStateOf("Mes actual") }
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
    var selectedTransferModules by remember {
        mutableStateOf(setOf(DataTransferModule.ANIMALS))
    }

    val selectedReport = visibleReportOptions.first { it.id == selectedReportId }
    val generatedReport = visibleReportOptions.firstOrNull { it.id == generatedPreviewId }
    val effectiveData = data
    val selectedRange = remember(selectedPeriod, customStartDate, customEndDate) {
        resolveReportDateRange(
            period = selectedPeriod,
            customStart = customStartDate,
            customEnd = customEndDate
        )
    }
    val selectedAnalysis = remember(effectiveData, selectedReport, selectedRange) {
        selectedRange?.let { range ->
            buildReportAnalysis(effectiveData, selectedReport.id, selectedReport.title, range)
        }
    }

    /** Exporta las métricas y los últimos registros visibles del reporte seleccionado. */
    val reportCsvExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val analysis = selectedAnalysis
        if (uri != null && analysis != null) {
            coroutineScope.launch {
                isTransferring = true
                runCatching { writeReportAnalysisCsv(context, uri, analysis) }
                    .onSuccess {
                        operationTitle = "Reporte exportado"
                        operationMessage =
                            "Las métricas y los 10 registros más recientes se guardaron para Excel."
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
        uri?.let {
            coroutineScope.launch {
                isTransferring = true
                runCatching {
                    val analysis = requireNotNull(selectedAnalysis) {
                        "Seleccione un rango de fechas válido."
                    }
                    writeReportPdf(context = context, uri = it, analysis = analysis)
                }.onSuccess {
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
        uri?.let {
            coroutineScope.launch {
                isTransferring = true
                runCatching {
                    check(isGeneralAdministrator) {
                        "Solo el Administrador General puede exportar datos operativos."
                    }
                    onBulkExport(it, selectedTransferModules)
                }.onSuccess { result ->
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
        uri?.let {
            coroutineScope.launch {
                isTransferring = true
                runCatching {
                    writeBulkImportTemplate(context, it, selectedTransferModules)
                }
                    .onSuccess {
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
            if (isGeneralAdministrator) {
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
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                                Column {
                                    Text(
                                        "Exportación e importación masiva",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Exclusivo del Administrador General",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            Text(
                                "Seleccione qué módulos desea exportar o importar. Al exportar " +
                                    "Animales se incluyen todos los campos del formulario, el peso " +
                                    "actual y su fecha para corregirlos en Excel y reimportarlos.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text("Módulos", fontWeight = FontWeight.SemiBold)
                            DataTransferModule.entries.forEach { module ->
                                FilterChip(
                                    selected = module in selectedTransferModules,
                                    onClick = {
                                        val updatedModules = if (
                                            module in selectedTransferModules
                                        ) {
                                            selectedTransferModules - module
                                        } else {
                                            selectedTransferModules + module
                                        }
                                        selectedTransferModules = updatedModules
                                        if (DataTransferModule.ANIMALS !in updatedModules) {
                                            selectedBulkImportMode = BulkImportMode.MERGE
                                        }
                                    },
                                    label = { Text(module.displayName) },
                                    enabled = !isTransferring,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (DataTransferModule.ANIMALS in selectedTransferModules) {
                                FilterChip(
                                    selected = selectedBulkImportMode ==
                                        BulkImportMode.INITIAL_LOAD_REPLACE,
                                    onClick = {
                                        selectedBulkImportMode = if (
                                            selectedBulkImportMode == BulkImportMode.MERGE
                                        ) {
                                            BulkImportMode.INITIAL_LOAD_REPLACE
                                        } else {
                                            BulkImportMode.MERGE
                                        }
                                    },
                                    label = { Text("Carga inicial: reemplazar historial de prueba") },
                                    enabled = !isTransferring,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (selectedBulkImportMode == BulkImportMode.INITIAL_LOAD_REPLACE) {
                                    Text(
                                        "Al importar se eliminarán los pesajes, controles sanitarios " +
                                            "y asignaciones anteriores de los códigos incluidos. " +
                                            "Los códigos se conservarán.",
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
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        if (selectedTransferModules.isEmpty()) {
                                            operationTitle = "Seleccione módulos"
                                            operationMessage =
                                                "Seleccione al menos un módulo para la plantilla."
                                        } else {
                                            bulkTemplateExporter.launch(bulkTemplateFileName())
                                        }
                                    },
                                    enabled = !isTransferring,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null)
                                    Text(" Plantilla")
                                }
                                Button(
                                    onClick = {
                                        if (selectedTransferModules.isEmpty()) {
                                            operationTitle = "Seleccione módulos"
                                            operationMessage =
                                                "Seleccione al menos un módulo para importar."
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
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null)
                                    Text(" Importar datos")
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text("Periodo", fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("30 días", "Mes actual", "Año actual", "Personalizado")
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
                Text(
                    "Se procesarán ${preview.animalCount} animales, " +
                        "${preview.weighingCount} pesajes y " +
                        "${preview.sanitaryCount} registros sanitarios. " +
                        if (replacesInitialData) {
                            "Se conservarán sus códigos, pero se eliminará definitivamente " +
                                "el historial anterior de los animales incluidos."
                        } else {
                            "Los animales cuyo código ya exista serán actualizados sin borrar su historial."
                        }
                )
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
                    formatRecordValue(record.reportId, record.kind, value),
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
        description = "Ingresos, egresos, balance y movimientos del periodo.",
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
    )
)

private fun formatRecordValue(reportId: String, kind: String, value: Double): String {
    val number = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 2
    }.format(value)
    return when (reportId) {
        "financial", "staff" -> "Q $number"
        "production" -> "$number L"
        "animals", "weighings" -> "$number lb"
        "lots" -> "$number animales"
        "parcels" -> "$number ha"
        else -> if (kind.isBlank()) number else "$number $kind"
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
    "Se exportaron $animalCount animales, $weighingCount pesajes y " +
        "$sanitaryCount registros sanitarios. Puede editar el archivo en Excel e importarlo nuevamente."

private fun BulkImportResult.successMessage(): String = buildString {
    append("Se crearon $animalsCreated animales, se actualizaron $animalsUpdated, ")
    append("se importaron $weighingsImported pesajes y ")
    append("$sanitaryRecordsImported registros sanitarios.")
    if (animalHistoriesReplaced > 0) {
        append(" Se reemplazó el historial de prueba de $animalHistoriesReplaced animales.")
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

/** Guarda exactamente las métricas y filas que la persona revisó en la vista previa. */
private suspend fun writeReportAnalysisCsv(
    context: Context,
    uri: Uri,
    analysis: ReportAnalysis
) = withContext(Dispatchers.IO) {
    val output = context.contentResolver.openOutputStream(uri, "wt")
        ?: error("No fue posible abrir el archivo seleccionado.")
    output.bufferedWriter(Charsets.UTF_8).use { writer ->
        writer.write("\uFEFF")
        writer.appendLine("seccion;campo;valor;detalle")
        writer.appendLine("reporte;titulo;${analysis.title.toCsvCell()};")
        writer.appendLine("reporte;periodo;${analysis.periodLabel.toCsvCell()};")
        writer.appendLine(
            "reporte;indicador_principal;${analysis.headline.toCsvCell()};" +
                analysis.headlineLabel.toCsvCell()
        )
        analysis.metrics.forEach { metric ->
            writer.appendLine(
                "metrica;${metric.label.toCsvCell()};${metric.value.toCsvCell()};" +
                    metric.explanation.toCsvCell()
            )
        }
        writer.appendLine("registro;fecha;nombre;detalle")
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        analysis.recentRecords.forEach { record ->
            writer.appendLine(
                "registro;${dateFormatter.format(Date(record.dateMillis))};" +
                    "${record.title.toCsvCell()};${record.detail.toCsvCell()}"
            )
        }
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

        y += 14f
        textPaint.isFakeBoldText = true
        textPaint.textSize = 15f
        canvas.drawText("10 registros más recientes del rango", 48f, y, textPaint)
        y += 27f
        textPaint.textSize = 11f
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        if (analysis.recentRecords.isEmpty()) {
            textPaint.isFakeBoldText = false
            canvas.drawText("No existen registros dentro del rango seleccionado.", 48f, y, textPaint)
        } else {
            analysis.recentRecords.forEachIndexed { index, record ->
                textPaint.isFakeBoldText = true
                canvas.drawText(
                    "${index + 1}. ${dateFormatter.format(Date(record.dateMillis))}  " +
                        record.title.fitPdfLine(textPaint, 280f),
                    48f,
                    y,
                    textPaint
                )
                textPaint.isFakeBoldText = false
                canvas.drawText(record.detail.fitPdfLine(textPaint, 210f), 335f, y, textPaint)
                y += 25f
            }
        }
        canvas.drawText(
            "Generado: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}",
            48f,
            780f,
            textPaint
        )
        document.finishPage(page)

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
