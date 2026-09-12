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
import androidx.compose.material.icons.filled.Close
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
import com.fincahernandez.gestionpecuaria.ui.components.DemoModeNotice
import com.fincahernandez.gestionpecuaria.data.transfer.BulkExportResult
import com.fincahernandez.gestionpecuaria.data.transfer.BulkImportPreview
import com.fincahernandez.gestionpecuaria.data.transfer.BulkImportResult
import com.fincahernandez.gestionpecuaria.data.transfer.BulkImportMode
import com.fincahernandez.gestionpecuaria.data.transfer.DataTransferModule
import com.fincahernandez.gestionpecuaria.data.transfer.bulkImportTemplateCsv
import com.fincahernandez.gestionpecuaria.ui.navigation.Routes
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Valores actuales utilizados para construir las vistas previas de los reportes. */
data class ReportDashboardData(
    val animalCount: Int,
    val lotCount: Int,
    val parcelCount: Int,
    val weighingCount: Int,
    val milkLiters: Double,
    val employeeCount: Int,
    val financialBalance: Double
)

private data class ReportOption(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val currentValue: (ReportDashboardData) -> String
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
    var selectedPeriod by rememberSaveable { mutableStateOf("Mensual") }
    var selectedReportId by rememberSaveable {
        mutableStateOf(visibleReportOptions.first().id)
    }
    var generatedPreviewId by rememberSaveable { mutableStateOf<String?>(null) }
    var operationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var operationTitle by rememberSaveable { mutableStateOf("Operación completada") }
    var isTransferring by remember { mutableStateOf(false) }
    var importedData by remember { mutableStateOf<ReportDashboardData?>(null) }
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
    val effectiveData = importedData ?: data

    /** Guarda un resumen compatible con Excel mediante el selector seguro de Android. */
    val csvExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                isTransferring = true
                runCatching { writeReportCsv(context, it, effectiveData) }
                    .onSuccess {
                        operationTitle = "Exportación completada"
                        operationMessage =
                            "El resumen se guardó como CSV y puede abrirse con Excel."
                    }
                    .onFailure {
                        operationTitle = "No fue posible exportar"
                        operationMessage = "Verifique la ubicación elegida e inténtelo nuevamente."
                    }
                isTransferring = false
            }
        }
    }

    /** Lee únicamente archivos elegidos por la persona; no solicita acceso a toda la carpeta. */
    val csvImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.takeIf { isGeneralAdministrator }?.let {
            coroutineScope.launch {
                isTransferring = true
                runCatching { readReportCsv(context, it) }
                    .onSuccess { imported ->
                        importedData = imported
                        generatedPreviewId = null
                        operationTitle = "Datos importados"
                        operationMessage =
                            "El resumen importado ya se utiliza en las vistas previas. " +
                                "Los registros originales de la aplicación no fueron modificados."
                    }
                    .onFailure { error ->
                        operationTitle = "Archivo no válido"
                        operationMessage = error.message
                            ?: "Seleccione un CSV exportado por esta aplicación."
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
                    writeReportPdf(
                        context = context,
                        uri = it,
                        reportTitle = selectedReport.title,
                        period = selectedPeriod,
                        value = selectedReport.currentValue(effectiveData)
                    )
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
            item { DemoModeNotice(compact = true) }
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
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Intercambio de reportes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (importedData == null) {
                                "Trabajando con los datos actuales de la aplicación."
                            } else {
                                "Vista previa basada en un archivo CSV importado."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (isGeneralAdministrator) {
                                OutlinedButton(
                                    onClick = {
                                        csvImporter.launch(
                                            arrayOf(
                                                "text/csv",
                                                "text/comma-separated-values",
                                                "application/vnd.ms-excel",
                                                "text/plain"
                                            )
                                        )
                                    },
                                    enabled = !isTransferring,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null)
                                    Text(" Importar resumen")
                                }
                            }
                            Button(
                                onClick = { csvExporter.launch(reportCsvFileName()) },
                                enabled = !isTransferring,
                                modifier = if (isGeneralAdministrator) {
                                    Modifier.weight(1f)
                                } else {
                                    Modifier.fillMaxWidth()
                                }
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null)
                                Text(" Exportar Excel")
                            }
                        }
                        if (importedData != null) {
                            TextButton(onClick = {
                                importedData = null
                                generatedPreviewId = null
                            }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Text(" Volver a los datos actuales")
                            }
                        }
                    }
                }
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("30 días", "Mensual", "Anual").forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = {
                                selectedPeriod = period
                                generatedPreviewId = null
                            },
                            label = { Text(period) },
                            leadingIcon = if (selectedPeriod == period) {
                                { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
                            } else null
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
                            report.currentValue(effectiveData),
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
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null)
                    Spacer(modifier = Modifier.padding(horizontal = 5.dp))
                    Text("Generar vista previa", fontWeight = FontWeight.Bold)
                }
            }
            generatedReport?.let { report ->
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
                                "VISTA PREVIA • ${selectedPeriod.uppercase()}",
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
                                report.currentValue(effectiveData),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Resumen calculado con los datos disponibles actualmente.",
                                color = Color.White
                            )
                        }
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
                                csvExporter.launch(reportCsvFileName())
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
                                importedData = null
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

private val reportOptions = listOf(
    ReportOption(
        id = "financial",
        title = "Reportes financieros",
        description = "Balance, estado de resultados y comparación por periodo.",
        icon = Icons.Default.AccountBalance,
        currentValue = { formatQuetzales(it.financialBalance) }
    ),
    ReportOption(
        id = "production",
        title = "Producción",
        description = "Producción de leche y rendimiento productivo.",
        icon = Icons.Default.LocalDrink,
        currentValue = { "${formatNumber(it.milkLiters)} L" }
    ),
    ReportOption(
        id = "staff",
        title = "Personal",
        description = "Nómina, empleados activos y costos de mano de obra.",
        icon = Icons.Default.Badge,
        currentValue = { "${it.employeeCount} empleados" }
    ),
    ReportOption(
        id = "animals",
        title = "Sanidad e inventario",
        description = "Inventario animal y futura información sanitaria.",
        icon = Icons.Default.Pets,
        currentValue = { "${it.animalCount} animales" }
    ),
    ReportOption(
        id = "weighings",
        title = "Pesajes",
        description = "Registros de peso y seguimiento del crecimiento.",
        icon = Icons.Default.Scale,
        currentValue = { "${it.weighingCount} registros" }
    ),
    ReportOption(
        id = "lots",
        title = "Lotes",
        description = "Inventario agrupado y rendimiento por lote.",
        icon = Icons.Default.Groups,
        currentValue = { "${it.lotCount} lotes" }
    ),
    ReportOption(
        id = "parcels",
        title = "Parcelas",
        description = "Uso de terrenos y productividad de las parcelas.",
        icon = Icons.Default.Landscape,
        currentValue = { "${it.parcelCount} parcelas" }
    )
)

private fun formatQuetzales(value: Double): String = "Q ${formatNumber(value)}"

private fun formatNumber(value: Double): String =
    NumberFormat.getNumberInstance(Locale.US).format(value)

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
    reportTitle: String,
    period: String,
    value: String
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
        canvas.drawText(reportTitle, 48f, 165f, textPaint)
        textPaint.isFakeBoldText = false
        textPaint.textSize = 14f
        canvas.drawText("Periodo: $period", 48f, 195f, textPaint)

        primaryPaint.textSize = 32f
        primaryPaint.isFakeBoldText = true
        canvas.drawText(value, 48f, 260f, primaryPaint)

        textPaint.textSize = 13f
        canvas.drawText(
            "Resumen calculado con los datos disponibles en la aplicación.",
            48f,
            300f,
            textPaint
        )
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
