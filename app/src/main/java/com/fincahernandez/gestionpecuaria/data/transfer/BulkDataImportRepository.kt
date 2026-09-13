package com.fincahernandez.gestionpecuaria.data.transfer

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.AnimalEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.EventoSanitarioEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.PesajeEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Módulos persistentes que pueden intercambiarse mediante el archivo de Excel/CSV. */
enum class DataTransferModule(val csvType: String, val displayName: String) {
    ANIMALS("ANIMAL", "Animales"),
    WEIGHINGS("PESAJE", "Pesajes"),
    SANITARY("SANITARIO", "Control sanitario");

    companion object {
        fun fromCsvType(value: String): DataTransferModule? =
            entries.firstOrNull { it.csvType == value.uppercase(Locale.ROOT) }
    }
}

/** Define si se combinan datos o se sustituye el historial de prueba de los códigos incluidos. */
enum class BulkImportMode {
    MERGE,
    INITIAL_LOAD_REPLACE
}

data class BulkImportPreview(
    val animalCount: Int,
    val weighingCount: Int,
    val sanitaryCount: Int
) {
    val totalCount: Int get() = animalCount + weighingCount + sanitaryCount
}

data class BulkImportResult(
    val animalsCreated: Int,
    val animalsUpdated: Int,
    val weighingsImported: Int,
    val sanitaryRecordsImported: Int,
    val animalHistoriesReplaced: Int = 0
)

data class BulkExportResult(
    val animalCount: Int,
    val weighingCount: Int,
    val sanitaryCount: Int
) {
    val totalCount: Int get() = animalCount + weighingCount + sanitaryCount
}

internal data class BulkImportPayload(
    val animals: List<BulkAnimalRow>,
    val weighings: List<BulkWeighingRow>,
    val sanitaryRecords: List<BulkSanitaryRow>
) {
    fun preview() = BulkImportPreview(
        animalCount = animals.size,
        weighingCount = weighings.size + animals.count { it.currentWeightPounds != null },
        sanitaryCount = sanitaryRecords.size
    )
}

internal data class BulkAnimalRow(
    val internalId: String?,
    val code: String,
    val name: String?,
    val motherCode: String?,
    val sex: String,
    val breed: String?,
    val birthDate: Long?,
    val arrivalDate: Long,
    val category: String,
    val originType: String,
    val origin: String?,
    val healthStatus: String,
    val inventoryStatus: String,
    val photoUri: String?,
    val currentWeightId: String?,
    val currentWeightPounds: Double?,
    val currentWeightDate: Long?,
    val notes: String?
)

internal data class BulkWeighingRow(
    val recordId: String?,
    val animalCode: String,
    val weightPounds: Double,
    val date: Long,
    val notes: String?
)

internal data class BulkSanitaryRow(
    val recordId: String?,
    val animalCode: String,
    val eventType: String,
    val eventDate: Long,
    val diagnosis: String,
    val medication: String?,
    val dose: String?,
    val healthStatus: String,
    val nextControlDate: Long?,
    val responsible: String?,
    val notes: String?
)

/**
 * Exporta, valida e importa los módulos funcionales almacenados en Room.
 * Toda importación se ejecuta como una sola transacción para evitar datos parciales.
 */
class BulkDataImportRepository(
    context: Context,
    private val database: GestionPecuariaDatabase =
        GestionPecuariaDatabase.obtenerInstancia(context.applicationContext)
) {
    private val applicationContext = context.applicationContext

    suspend fun export(uri: Uri, modules: Set<DataTransferModule>): BulkExportResult =
        withContext(Dispatchers.IO) {
            requireModules(modules)
            val animals = database.animalDao().observarTodos().first()
            val weighings = database.pesajeDao().observarTodos().first()
            val sanitaryRecords = database.eventoSanitarioDao().observarTodos().first()
            writeExportText(
                uri,
                buildBulkExportCsv(animals, weighings, sanitaryRecords, modules)
            )
            BulkExportResult(
                animalCount = if (DataTransferModule.ANIMALS in modules) animals.size else 0,
                weighingCount = if (DataTransferModule.WEIGHINGS in modules) weighings.size else 0,
                sanitaryCount = if (DataTransferModule.SANITARY in modules) {
                    sanitaryRecords.size
                } else {
                    0
                }
            )
        }

    suspend fun inspect(
        uri: Uri,
        modules: Set<DataTransferModule> = DataTransferModule.entries.toSet(),
        mode: BulkImportMode = BulkImportMode.MERGE
    ): BulkImportPreview = withContext(Dispatchers.IO) {
        requireModules(modules)
        val payload = parseBulkImportCsv(readImportText(uri), modules)
        validateReferences(payload, mode)
        payload.preview()
    }

    suspend fun import(
        uri: Uri,
        modules: Set<DataTransferModule> = DataTransferModule.entries.toSet(),
        mode: BulkImportMode = BulkImportMode.MERGE
    ): BulkImportResult = withContext(Dispatchers.IO) {
        requireModules(modules)
        val payload = parseBulkImportCsv(readImportText(uri), modules)
        validateReferences(payload, mode)

        var createdAnimals = 0
        var updatedAnimals = 0
        var animalWeightsImported = 0
        var replacedAnimalHistories = 0
        database.withTransaction {
            val animalDao = database.animalDao()
            val weighingDao = database.pesajeDao()
            val sanitaryDao = database.eventoSanitarioDao()

            // Primero resuelve todos los identificadores. Así una cría puede referirse
            // a su madre aunque ambas estén en el CSV y la madre aparezca después.
            val existingByRow = payload.animals.associateWith { row ->
                row.internalId?.let { animalDao.buscarPorId(it) }
                    ?: animalDao.buscarPorCodigoIdentificacion(row.code)
            }
            val animalIdByCode = mutableMapOf<String, String>()
            payload.animals.forEach { row ->
                val existing = existingByRow[row]
                val resolvedId = existing?.id ?: row.internalId ?: UUID.randomUUID().toString()
                animalIdByCode[row.code] = resolvedId
                // También conserva el código anterior si esta fila lo está modificando.
                existing?.let { animalIdByCode[it.codigoIdentificacion] = it.id }
            }
            payload.animals.mapNotNull { it.motherCode }.distinct().forEach { motherCode ->
                if (motherCode !in animalIdByCode) {
                    animalDao.buscarPorCodigoIdentificacion(motherCode)?.let { mother ->
                        animalIdByCode[motherCode] = mother.id
                    }
                }
            }

            payload.animals.forEach { row ->
                // El id interno permite cambiar el código desde Excel sin crear otro animal.
                val existing = existingByRow[row]
                if (existing != null && mode == BulkImportMode.INITIAL_LOAD_REPLACE) {
                    // La fila se vuelve la fuente inicial oficial para este código.
                    weighingDao.eliminarPorAnimalParaCargaInicial(existing.id)
                    sanitaryDao.eliminarPorAnimalParaCargaInicial(existing.id)
                    database.loteDao().eliminarAsignacionesParaCargaInicial(existing.id)
                    replacedAnimalHistories++
                }
                val resolvedAnimalId = requireNotNull(animalIdByCode[row.code])
                val resolvedMotherId = row.motherCode?.let(animalIdByCode::get)
                val storedAnimal = if (existing == null) {
                    row.toEntity(resolvedAnimalId, resolvedMotherId).also {
                        animalDao.insertar(it)
                        createdAnimals++
                    }
                } else {
                    existing.withImportedValues(row, resolvedMotherId).also {
                        animalDao.actualizar(it)
                        updatedAnimals++
                    }
                }
                row.toCurrentWeighing(storedAnimal.id)?.let {
                    weighingDao.insertarImportado(it)
                    animalWeightsImported++
                }
            }

            payload.weighings.forEach { row ->
                val animal = requireNotNull(
                    animalDao.buscarPorCodigoIdentificacion(row.animalCode)
                ) { "No existe el animal ${row.animalCode}." }
                weighingDao.insertarImportado(row.toEntity(animal.id))
            }

            payload.sanitaryRecords.forEach { row ->
                val animal = requireNotNull(
                    animalDao.buscarPorCodigoIdentificacion(row.animalCode)
                ) { "No existe el animal ${row.animalCode}." }
                sanitaryDao.insertarImportado(row.toEntity(animal.id))
                if (animal.estado == "ACTIVO") {
                    sanitaryDao.actualizarEstadoDelAnimal(animal.id, row.healthStatus)
                }
            }
        }

        BulkImportResult(
            animalsCreated = createdAnimals,
            animalsUpdated = updatedAnimals,
            weighingsImported = payload.weighings.size + animalWeightsImported,
            sanitaryRecordsImported = payload.sanitaryRecords.size,
            animalHistoriesReplaced = replacedAnimalHistories
        )
    }

    private suspend fun validateReferences(payload: BulkImportPayload, mode: BulkImportMode) {
        val importedCodes = payload.animals.mapTo(mutableSetOf()) { it.code }
        val storedAnimals = database.animalDao().observarTodos().first()
        val storedCodes = storedAnimals.mapTo(mutableSetOf()) { it.codigoIdentificacion }
        val validCodes = importedCodes + storedCodes
        val unknownCodes = buildSet {
            payload.weighings.filter { it.animalCode !in validCodes }
                .mapTo(this) { it.animalCode }
            payload.sanitaryRecords.filter { it.animalCode !in validCodes }
                .mapTo(this) { it.animalCode }
            payload.animals.mapNotNull { it.motherCode }
                .filter { it !in validCodes }
                .mapTo(this) { it }
        }
        require(unknownCodes.isEmpty()) {
            "No existen los animales con código: ${unknownCodes.sorted().joinToString()}."
        }

        val importedByCode = payload.animals.associateBy { it.code }
        val storedByCode = storedAnimals.associateBy { it.codigoIdentificacion }
        payload.animals.forEach { row ->
            val motherCode = row.motherCode ?: return@forEach
            require(row.originType == "NACIDO_EN_FINCA") {
                "El animal ${row.code} solo puede tener madre si nació en la finca."
            }
            require(motherCode != row.code) {
                "El animal ${row.code} no puede registrarse como su propia madre."
            }
            val motherSex = importedByCode[motherCode]?.sex ?: storedByCode[motherCode]?.sexo
            require(motherSex == "HEMBRA") {
                "El código de madre $motherCode debe corresponder a una hembra."
            }
        }

        validateCurrentAnimalWeights(payload.animals, storedAnimals, mode)
    }

    /**
     * Un peso marcado como actual no puede ser anterior al nacimiento ni a otro
     * pesaje más reciente. Los pesajes históricos continúan admitiéndose mediante
     * el módulo PESAJE.
     */
    private suspend fun validateCurrentAnimalWeights(
        importedAnimals: List<BulkAnimalRow>,
        storedAnimals: List<AnimalEntity>,
        mode: BulkImportMode
    ) {
        val storedById = storedAnimals.associateBy { it.id }
        val storedByCode = storedAnimals.associateBy { it.codigoIdentificacion }
        val storedWeighings = database.pesajeDao().observarTodos().first()

        importedAnimals.forEach { row ->
            val weightDate = row.currentWeightDate ?: return@forEach
            val animalLabel = row.name?.takeIf { it.isNotBlank() }?.let { "${row.code} ($it)" }
                ?: row.code
            row.birthDate?.let { birthDate ->
                require(weightDate >= birthDate) {
                    "El peso actual de $animalLabel tiene fecha ${formatCsvDate(weightDate)}, " +
                        "anterior a su nacimiento ${formatCsvDate(birthDate)}."
                }
            }

            val storedAnimal = row.internalId?.let(storedById::get) ?: storedByCode[row.code]
            if (storedAnimal != null && mode == BulkImportMode.MERGE) {
                val newerStoredWeight = storedWeighings
                    .asSequence()
                    .filter { weighing ->
                        weighing.animalId == storedAnimal.id &&
                            weighing.id != row.currentWeightId &&
                            weighing.fechaPesaje > weightDate
                    }
                    .maxByOrNull { it.fechaPesaje }
                require(newerStoredWeight == null) {
                    "El peso actual de $animalLabel está fechado ${formatCsvDate(weightDate)}, " +
                        "pero ya existe un pesaje posterior del " +
                        "${formatCsvDate(newerStoredWeight?.fechaPesaje)}. " +
                        "Corrija fechaPesoActual o importe esa fila como PESAJE histórico."
                }
            }
        }
    }

    private fun readImportText(uri: Uri): String {
        val input = applicationContext.contentResolver.openInputStream(uri)
            ?: error("No fue posible abrir el archivo seleccionado.")
        return input.bufferedReader(Charsets.UTF_8).use { reader ->
            val content = StringBuilder()
            val buffer = CharArray(4_096)
            while (true) {
                val read = reader.read(buffer)
                if (read < 0) break
                content.append(buffer, 0, read)
                require(content.length <= MAX_BULK_IMPORT_CHARS) {
                    "El archivo supera el tamaño permitido para una importación."
                }
            }
            content.toString()
        }
    }

    private fun writeExportText(uri: Uri, csv: String) {
        val output = applicationContext.contentResolver.openOutputStream(uri, "wt")
            ?: error("No fue posible crear el archivo seleccionado.")
        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write('\uFEFF'.toString())
            writer.write(csv)
        }
    }
}

private fun requireModules(modules: Set<DataTransferModule>) {
    require(modules.isNotEmpty()) { "Seleccione al menos un módulo." }
}

private fun BulkAnimalRow.toEntity(resolvedId: String, resolvedMotherId: String?) = AnimalEntity(
    id = resolvedId,
    codigoIdentificacion = code,
    nombre = name,
    sexo = sex,
    raza = breed,
    fechaNacimiento = birthDate,
    fechaIngreso = arrivalDate,
    categoria = category,
    tipoOrigen = originType,
    madreId = resolvedMotherId,
    procedencia = origin,
    estadoSalud = healthStatus,
    estado = inventoryStatus,
    observaciones = notes,
    fotoUri = photoUri
)

private fun AnimalEntity.withImportedValues(
    row: BulkAnimalRow,
    resolvedMotherId: String?
) = copy(
    codigoIdentificacion = row.code,
    nombre = row.name,
    sexo = row.sex,
    raza = row.breed,
    fechaNacimiento = row.birthDate,
    fechaIngreso = row.arrivalDate,
    categoria = row.category,
    tipoOrigen = row.originType,
    madreId = resolvedMotherId,
    procedencia = row.origin,
    estadoSalud = row.healthStatus,
    estado = row.inventoryStatus,
    observaciones = row.notes,
    // Una celda vacía conserva la fotografía ya asociada desde Android.
    fotoUri = row.photoUri ?: fotoUri,
    actualizadoEn = System.currentTimeMillis()
)

private fun BulkAnimalRow.toCurrentWeighing(animalId: String): PesajeEntity? {
    val weight = currentWeightPounds ?: return null
    val date = requireNotNull(currentWeightDate)
    return PesajeEntity(
        id = currentWeightId ?: stableImportId("PESO_ACTUAL|$animalId|$date|$weight"),
        animalId = animalId,
        pesoLibras = weight,
        fechaPesaje = date,
        observaciones = "Peso importado desde la ficha masiva de animales"
    )
}

private fun BulkWeighingRow.toEntity(animalId: String): PesajeEntity = PesajeEntity(
    id = recordId
        ?: stableImportId("PESAJE|$animalCode|$date|$weightPounds|${notes.orEmpty()}"),
    animalId = animalId,
    pesoLibras = weightPounds,
    fechaPesaje = date,
    observaciones = notes
)

private fun BulkSanitaryRow.toEntity(animalId: String): EventoSanitarioEntity =
    EventoSanitarioEntity(
        id = recordId ?: stableImportId(
            "SANITARIO|$animalCode|$eventDate|$eventType|$diagnosis|${medication.orEmpty()}"
        ),
        animalId = animalId,
        tipoEvento = eventType,
        fechaEvento = eventDate,
        diagnostico = diagnosis,
        medicamento = medication,
        dosis = dose,
        estadoSalud = healthStatus,
        proximoControl = nextControlDate,
        responsable = responsible,
        observaciones = notes
    )

private fun stableImportId(value: String): String =
    UUID.nameUUIDFromBytes(value.toByteArray(Charsets.UTF_8)).toString()

private const val MAX_BULK_IMPORT_CHARS = 5_000_000

private val bulkHeaders = listOf(
    "tipo", "idInternoNoEditar", "idRegistroNoEditar", "codigoAnimal", "nombre",
    "codigoMadre", "sexo", "raza", "fechaNacimiento", "fechaIngreso", "categoria", "tipoOrigen",
    "procedencia", "estadoSalud", "estado", "fotoUriNoEditar", "idPesoActualNoEditar",
    "pesoActualLibras", "fechaPesoActual", "pesoLibras", "fechaPesaje", "tipoEvento",
    "fechaEvento", "diagnostico", "medicamento", "dosis", "proximoControl",
    "responsable", "observaciones"
)

/** Crea una plantilla únicamente con los módulos seleccionados. */
fun bulkImportTemplateCsv(
    modules: Set<DataTransferModule> = DataTransferModule.entries.toSet()
): String {
    requireModules(modules)
    return buildString {
        appendLine(bulkHeaders.joinToString(";"))
        if (DataTransferModule.ANIMALS in modules) appendLine(animalExampleRow())
        if (DataTransferModule.WEIGHINGS in modules) appendLine(weighingExampleRow())
        if (DataTransferModule.SANITARY in modules) appendLine(sanitaryExampleRow())
    }
}

private fun animalExampleRow() = templateRow(
    "tipo" to "ANIMAL", "codigoAnimal" to "FH-2026-100", "nombre" to "Ejemplo",
    "sexo" to "HEMBRA", "raza" to "Criolla", "fechaNacimiento" to "01/01/2024",
    "fechaIngreso" to "01/01/2024", "categoria" to "LECHERO",
    "tipoOrigen" to "NACIDO_EN_FINCA", "procedencia" to "Finca Hernández",
    "estadoSalud" to "EXCELENTE", "estado" to "ACTIVO",
    "pesoActualLibras" to "550.5", "fechaPesoActual" to "09/09/2026",
    "observaciones" to "Fila de ejemplo; puede eliminarla"
)

private fun weighingExampleRow() = templateRow(
    "tipo" to "PESAJE", "codigoAnimal" to "FH-2026-100", "pesoLibras" to "550.5",
    "fechaPesaje" to "09/09/2026", "observaciones" to "Pesaje de ejemplo"
)

private fun sanitaryExampleRow() = templateRow(
    "tipo" to "SANITARIO", "codigoAnimal" to "FH-2026-100",
    "estadoSalud" to "EXCELENTE", "tipoEvento" to "VACUNA",
    "fechaEvento" to "09/09/2026", "diagnostico" to "Control preventivo",
    "medicamento" to "Vacuna de ejemplo", "dosis" to "2 ml",
    "proximoControl" to "09/03/2027", "responsable" to "Administrador General",
    "observaciones" to "Registro de ejemplo"
)

internal fun buildBulkExportCsv(
    animals: List<AnimalEntity>,
    weighings: List<PesajeEntity>,
    sanitaryRecords: List<EventoSanitarioEntity>,
    modules: Set<DataTransferModule>
): String {
    requireModules(modules)
    val animalById = animals.associateBy { it.id }
    val currentWeightByAnimal = weighings.groupBy { it.animalId }
        .mapValues { (_, records) -> records.maxByOrNull { it.fechaPesaje } }
    return buildString {
        appendLine(bulkHeaders.joinToString(";"))
        if (DataTransferModule.ANIMALS in modules) {
            animals.forEach { animal ->
                val weight = currentWeightByAnimal[animal.id]
                appendLine(templateRow(
                    "tipo" to "ANIMAL",
                    "idInternoNoEditar" to animal.id,
                    "codigoAnimal" to animal.codigoIdentificacion,
                    "nombre" to animal.nombre.orEmpty(),
                    "codigoMadre" to animal.madreId
                        ?.let(animalById::get)
                        ?.codigoIdentificacion
                        .orEmpty(),
                    "sexo" to animal.sexo,
                    "raza" to animal.raza.orEmpty(),
                    "fechaNacimiento" to formatCsvDate(animal.fechaNacimiento),
                    "fechaIngreso" to formatCsvDate(animal.fechaIngreso),
                    "categoria" to animal.categoria,
                    "tipoOrigen" to animal.tipoOrigen,
                    "procedencia" to animal.procedencia.orEmpty(),
                    "estadoSalud" to animal.estadoSalud,
                    "estado" to animal.estado,
                    "fotoUriNoEditar" to animal.fotoUri.orEmpty(),
                    "idPesoActualNoEditar" to weight?.id.orEmpty(),
                    "pesoActualLibras" to weight?.pesoLibras?.toString().orEmpty(),
                    "fechaPesoActual" to formatCsvDate(weight?.fechaPesaje),
                    "observaciones" to animal.observaciones.orEmpty()
                ))
            }
        }
        if (DataTransferModule.WEIGHINGS in modules) {
            weighings.forEach { weight ->
                val code = animalById[weight.animalId]?.codigoIdentificacion ?: return@forEach
                appendLine(templateRow(
                    "tipo" to "PESAJE", "idRegistroNoEditar" to weight.id,
                    "codigoAnimal" to code, "pesoLibras" to weight.pesoLibras.toString(),
                    "fechaPesaje" to formatCsvDate(weight.fechaPesaje),
                    "observaciones" to weight.observaciones.orEmpty()
                ))
            }
        }
        if (DataTransferModule.SANITARY in modules) {
            sanitaryRecords.forEach { record ->
                val code = animalById[record.animalId]?.codigoIdentificacion ?: return@forEach
                appendLine(templateRow(
                    "tipo" to "SANITARIO", "idRegistroNoEditar" to record.id,
                    "codigoAnimal" to code, "estadoSalud" to record.estadoSalud,
                    "tipoEvento" to record.tipoEvento, "fechaEvento" to formatCsvDate(record.fechaEvento),
                    "diagnostico" to record.diagnostico, "medicamento" to record.medicamento.orEmpty(),
                    "dosis" to record.dosis.orEmpty(), "proximoControl" to formatCsvDate(record.proximoControl),
                    "responsable" to record.responsable.orEmpty(),
                    "observaciones" to record.observaciones.orEmpty()
                ))
            }
        }
    }
}

private fun templateRow(vararg values: Pair<String, String>): String {
    val byHeader = values.toMap()
    return bulkHeaders.joinToString(";") { csvEscape(byHeader[it].orEmpty()) }
}

private fun csvEscape(value: String): String {
    val singleLine = value.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ')
    return if (';' in singleLine || '"' in singleLine) {
        "\"${singleLine.replace("\"", "\"\"")}\""
    } else {
        singleLine
    }
}

/** Analiza solo los módulos seleccionados antes de escribir una sola fila. */
internal fun parseBulkImportCsv(
    csv: String,
    modules: Set<DataTransferModule> = DataTransferModule.entries.toSet()
): BulkImportPayload {
    requireModules(modules)
    val lines = csv.removePrefix("\uFEFF").lineSequence().filter { it.isNotBlank() }.toList()
    require(lines.size >= 2) { "El archivo no contiene registros para importar." }
    val receivedHeaders = parseCsvLine(lines.first()).map(String::trim)
    // codigoMadre se añadió después de la primera versión del formato; los archivos
    // anteriores siguen siendo válidos y simplemente se importan sin parentesco.
    val optionalHeaders = setOf("codigoMadre")
    val missingHeaders = bulkHeaders.filterNot { it in receivedHeaders || it in optionalHeaders }
    require(missingHeaders.isEmpty()) {
        "Faltan columnas obligatorias: ${missingHeaders.joinToString()}."
    }
    val indexByHeader = receivedHeaders.withIndex().associate { it.value to it.index }
    val animals = mutableListOf<BulkAnimalRow>()
    val weighings = mutableListOf<BulkWeighingRow>()
    val sanitary = mutableListOf<BulkSanitaryRow>()

    lines.drop(1).forEachIndexed { index, line ->
        val rowNumber = index + 2
        val cells = parseCsvLine(line)
        fun cell(header: String): String = indexByHeader[header]
            ?.let(cells::getOrNull)
            .orEmpty()
            .trim()
        fun required(header: String): String = cell(header).ifBlank {
            throw IllegalArgumentException("Fila $rowNumber: falta $header.")
        }
        val module = DataTransferModule.fromCsvType(required("tipo"))
            ?: throw IllegalArgumentException(
                "Fila $rowNumber: tipo debe ser ANIMAL, PESAJE o SANITARIO."
            )
        if (module !in modules) return@forEachIndexed

        when (module) {
            DataTransferModule.ANIMALS -> {
                val originType = required("tipoOrigen").uppercase(Locale.ROOT)
                require(originType in setOf("NACIDO_EN_FINCA", "INGRESADO_A_FINCA")) {
                    "Fila $rowNumber: tipoOrigen no válido."
                }
                val birthDate = parseOptionalDate(cell("fechaNacimiento"), rowNumber)
                val arrivalDate = parseOptionalDate(cell("fechaIngreso"), rowNumber)
                    ?: birthDate
                    ?: throw IllegalArgumentException("Fila $rowNumber: falta fechaIngreso.")
                if (originType == "NACIDO_EN_FINCA") require(birthDate != null) {
                    "Fila $rowNumber: un animal nacido en finca requiere fechaNacimiento."
                }
                val sex = required("sexo").uppercase(Locale.ROOT)
                require(sex in setOf("MACHO", "HEMBRA")) {
                    "Fila $rowNumber: sexo debe ser MACHO o HEMBRA."
                }
                val category = required("categoria").uppercase(Locale.ROOT)
                require(category in setOf("LECHERO", "ENGORDE", "AMBOS")) {
                    "Fila $rowNumber: categoría debe ser LECHERO, ENGORDE o AMBOS."
                }
                val inventoryStatus = cell("estado").ifBlank { "ACTIVO" }.uppercase(Locale.ROOT)
                require(inventoryStatus in setOf("ACTIVO", "INACTIVO")) {
                    "Fila $rowNumber: estado debe ser ACTIVO o INACTIVO."
                }
                val currentWeight = parseOptionalPositiveDouble(
                    cell("pesoActualLibras"), rowNumber, "pesoActualLibras"
                )
                val currentWeightDate = parseOptionalDate(cell("fechaPesoActual"), rowNumber)
                require((currentWeight == null) == (currentWeightDate == null)) {
                    "Fila $rowNumber: pesoActualLibras y fechaPesoActual deben completarse juntos."
                }
                animals += BulkAnimalRow(
                    internalId = cell("idInternoNoEditar").ifBlank { null },
                    code = required("codigoAnimal"),
                    name = cell("nombre").ifBlank { null },
                    motherCode = cell("codigoMadre").ifBlank { null },
                    sex = sex,
                    breed = cell("raza").ifBlank { null },
                    birthDate = birthDate,
                    arrivalDate = arrivalDate,
                    category = category,
                    originType = originType,
                    origin = cell("procedencia").ifBlank { null },
                    healthStatus = normalizeHealth(required("estadoSalud"), rowNumber),
                    inventoryStatus = inventoryStatus,
                    photoUri = cell("fotoUriNoEditar").ifBlank { null },
                    currentWeightId = cell("idPesoActualNoEditar").ifBlank { null },
                    currentWeightPounds = currentWeight,
                    currentWeightDate = currentWeightDate,
                    notes = cell("observaciones").ifBlank { null }
                )
            }

            DataTransferModule.WEIGHINGS -> weighings += BulkWeighingRow(
                recordId = cell("idRegistroNoEditar").ifBlank { null },
                animalCode = required("codigoAnimal"),
                weightPounds = parseRequiredPositiveDouble(
                    required("pesoLibras"), rowNumber, "pesoLibras"
                ),
                date = parseRequiredDate(required("fechaPesaje"), rowNumber),
                notes = cell("observaciones").ifBlank { null }
            )

            DataTransferModule.SANITARY -> sanitary += BulkSanitaryRow(
                recordId = cell("idRegistroNoEditar").ifBlank { null },
                animalCode = required("codigoAnimal"),
                eventType = required("tipoEvento").uppercase(Locale.ROOT),
                eventDate = parseRequiredDate(required("fechaEvento"), rowNumber),
                diagnosis = required("diagnostico"),
                medication = cell("medicamento").ifBlank { null },
                dose = cell("dosis").ifBlank { null },
                healthStatus = normalizeHealth(required("estadoSalud"), rowNumber),
                nextControlDate = parseOptionalDate(cell("proximoControl"), rowNumber),
                responsible = cell("responsable").ifBlank { null },
                notes = cell("observaciones").ifBlank { null }
            )
        }
    }

    val duplicatedIds = animals.mapNotNull { it.internalId }.groupingBy { it }.eachCount()
        .filterValues { it > 1 }.keys
    require(duplicatedIds.isEmpty()) {
        "Hay identificadores internos repetidos; no edite las columnas marcadas NoEditar."
    }
    val duplicatedCodes = animals.groupingBy { it.code }.eachCount()
        .filterValues { it > 1 }.keys
    require(duplicatedCodes.isEmpty()) {
        "Hay códigos de animal repetidos: ${duplicatedCodes.sorted().joinToString()}."
    }
    val payload = BulkImportPayload(animals, weighings, sanitary)
    require(payload.preview().totalCount > 0) {
        "El archivo no contiene filas de los módulos seleccionados."
    }
    return payload
}

private fun parseCsvLine(line: String): List<String> {
    val cells = mutableListOf<String>()
    val current = StringBuilder()
    var quoted = false
    var index = 0
    while (index < line.length) {
        val character = line[index]
        when {
            character == '"' && quoted && line.getOrNull(index + 1) == '"' -> {
                current.append('"')
                index++
            }
            character == '"' -> quoted = !quoted
            character == ';' && !quoted -> {
                cells += current.toString()
                current.clear()
            }
            else -> current.append(character)
        }
        index++
    }
    require(!quoted) { "El archivo contiene una fila con comillas sin cerrar." }
    cells += current.toString()
    return cells
}

private fun normalizeHealth(value: String, rowNumber: Int): String {
    val normalized = value.uppercase(Locale.ROOT)
        .replace("OBSERVACION", "OBSERVACIÓN")
        .replace("CRITICO", "CRÍTICO")
    require(normalized in setOf("EXCELENTE", "OBSERVACIÓN", "CRÍTICO")) {
        "Fila $rowNumber: estadoSalud no válido."
    }
    return normalized
}

private fun parseRequiredPositiveDouble(value: String, rowNumber: Int, field: String): Double =
    parseOptionalPositiveDouble(value, rowNumber, field)
        ?: throw IllegalArgumentException("Fila $rowNumber: falta $field.")

private fun parseOptionalPositiveDouble(value: String, rowNumber: Int, field: String): Double? {
    if (value.isBlank()) return null
    val parsed = value.replace(',', '.').toDoubleOrNull()
    require(parsed != null && parsed.isFinite() && parsed > 0.0) {
        "Fila $rowNumber: $field debe ser un número positivo."
    }
    return parsed
}

private fun parseRequiredDate(value: String, rowNumber: Int): Long =
    parseOptionalDate(value, rowNumber)
        ?: throw IllegalArgumentException("Fila $rowNumber: falta una fecha obligatoria.")

private fun parseOptionalDate(value: String, rowNumber: Int): Long? {
    if (value.isBlank()) return null
    return runCatching {
        SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }.parse(value)?.time
    }.getOrNull() ?: throw IllegalArgumentException(
        "Fila $rowNumber: la fecha '$value' debe usar dd/MM/aaaa."
    )
}

private fun formatCsvDate(value: Long?): String = value?.let {
    SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it))
}.orEmpty()
