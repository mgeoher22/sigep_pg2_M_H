package com.fincahernandez.gestionpecuaria.data.repository

import androidx.room.withTransaction
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.LoteAnimalEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.LoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Lote almacenado junto con sus animales y el promedio de su último peso. */
data class LotStoredRecord(
    val lot: LoteEntity,
    val activeAnimalIds: List<String>,
    val historicalAnimalIds: List<String>,
    val assignments: List<LoteAnimalEntity>,
    val averageWeightPounds: Double?
)

/** Datos que se crean o corrigen desde el formulario del módulo de lotes. */
data class LotDraft(
    val name: String,
    val type: String,
    val parcelName: String?,
    val targetWeightPounds: Double?,
    val estimatedExitDate: Long?,
    val selectedAnimalIds: List<String>
)

/**
 * Centraliza las operaciones de lotes y garantiza que lote y asignaciones se
 * guarden juntos. Si una validación falla, Room revierte toda la operación.
 */
class LotRepository(private val database: GestionPecuariaDatabase) {
    private val lotDao = database.loteDao()
    private val animalDao = database.animalDao()
    private val weighingDao = database.pesajeDao()

    /** Actualiza la interfaz cuando cambia un lote, una asignación o un pesaje. */
    fun observeLots(): Flow<List<LotStoredRecord>> = combine(
        lotDao.observarTodos(),
        lotDao.observarAsignaciones(),
        weighingDao.observarTodos()
    ) { lots, assignments, weighings ->
        val latestWeightByAnimal = weighings
            .groupBy { it.animalId }
            .mapValues { (_, records) -> records.maxByOrNull { it.fechaPesaje }?.pesoLibras }

        lots.map { lot ->
            val lotAssignments = assignments.filter { it.loteId == lot.id }
            val activeIds = lotAssignments
                .filter { it.fechaSalida == null }
                .map { it.animalId }
                .distinct()
            val weights = activeIds.mapNotNull(latestWeightByAnimal::get)
            LotStoredRecord(
                lot = lot,
                activeAnimalIds = activeIds,
                historicalAnimalIds = lotAssignments.map { it.animalId }.distinct(),
                assignments = lotAssignments,
                averageWeightPounds = weights.takeIf { it.isNotEmpty() }?.average()
            )
        }
    }

    /** Crea un lote y asigna únicamente animales activos y disponibles. */
    suspend fun createLot(draft: LotDraft): String = database.withTransaction {
        validateDraft(draft)
        validateParcelAvailability(draft.parcelName, allowedLotId = null, allowLegacyName = false)
        val lotId = java.util.UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val lot = LoteEntity(
            id = lotId,
            codigo = nextLotCode(lotDao.obtenerTodos().map { it.codigo }),
            nombre = draft.name.trim(),
            tipo = draft.type,
            fechaCreacion = now,
            parcelaNombre = draft.parcelName?.trim()?.takeIf(String::isNotBlank),
            pesoObjetivoLibras = draft.targetWeightPounds,
            fechaSalidaEstimada = draft.estimatedExitDate
        )
        lotDao.insertar(lot)
        draft.selectedAnimalIds.distinct().forEach { animalId ->
            requireAnimalAvailable(animalId, allowedLotId = null)
            lotDao.insertarAsignacion(
                LoteAnimalEntity(animalId = animalId, loteId = lotId, fechaIngreso = now)
            )
        }
        lotId
    }

    /** Corrige el lote y conserva el historial de los animales retirados. */
    suspend fun updateLot(lotId: String, draft: LotDraft) = database.withTransaction {
        validateDraft(draft)
        val current = checkNotNull(lotDao.buscarPorId(lotId)) { "No se encontró el lote." }
        check(current.estado == "ACTIVO") { "Un lote cerrado ya no se puede editar." }
        validateParcelAvailability(
            parcelName = draft.parcelName,
            allowedLotId = lotId,
            allowLegacyName = draft.parcelName.equals(current.parcelaNombre, ignoreCase = true)
        )

        val now = System.currentTimeMillis()
        val selectedIds = draft.selectedAnimalIds.distinct().toSet()
        val currentAssignments = lotDao.obtenerAsignacionesActivasDelLote(lotId)
        val currentIds = currentAssignments.map { it.animalId }.toSet()

        // Se valida todo antes de modificar para que un error no produzca cambios parciales.
        selectedIds.forEach { requireAnimalAvailable(it, allowedLotId = lotId) }

        lotDao.actualizar(
            current.copy(
                nombre = draft.name.trim(),
                tipo = draft.type,
                parcelaNombre = draft.parcelName?.trim()?.takeIf(String::isNotBlank),
                pesoObjetivoLibras = draft.targetWeightPounds,
                fechaSalidaEstimada = draft.estimatedExitDate,
                actualizadoEn = now
            )
        )

        (currentIds - selectedIds).forEach { animalId ->
            lotDao.finalizarAsignacionActiva(
                animalId = animalId,
                fechaSalida = now,
                motivoSalida = "Retirado durante la edición del lote",
                fechaActualizacion = now
            )
        }
        (selectedIds - currentIds).forEach { animalId ->
            lotDao.insertarAsignacion(
                LoteAnimalEntity(animalId = animalId, loteId = lotId, fechaIngreso = now)
            )
        }
    }

    /** Desactiva el lote y cierra sus asignaciones sin eliminar el historial. */
    suspend fun deactivateLot(lotId: String) = database.withTransaction {
        val lot = checkNotNull(lotDao.buscarPorId(lotId)) { "No se encontró el lote." }
        check(lot.estado == "ACTIVO") { "El lote ya está cerrado." }
        val now = System.currentTimeMillis()
        lotDao.finalizarAsignacionesDelLote(
            loteId = lotId,
            fechaSalida = now,
            motivoSalida = "Lote cerrado",
            fechaActualizacion = now
        )
        check(lotDao.cerrarLote(lotId, now, now) == 1) { "No se pudo cerrar el lote." }
    }

    private suspend fun requireAnimalAvailable(animalId: String, allowedLotId: String?) {
        val animal = animalDao.buscarPorId(animalId)
        check(animal != null && animal.estado == "ACTIVO") {
            "Uno de los animales seleccionados ya no está activo."
        }
        val assignment = lotDao.buscarAsignacionActiva(animalId)
        check(assignment == null || assignment.loteId == allowedLotId) {
            "El animal ${animal.codigoIdentificacion} ya pertenece a otro lote activo."
        }
    }

    private fun validateDraft(draft: LotDraft) {
        require(draft.name.isNotBlank()) { "Ingrese el nombre del lote." }
        require(
            draft.targetWeightPounds == null ||
                (draft.targetWeightPounds.isFinite() && draft.targetWeightPounds > 0.0)
        ) {
            "La meta de peso debe ser mayor que cero."
        }
    }

    private suspend fun validateParcelAvailability(
        parcelName: String?,
        allowedLotId: String?,
        allowLegacyName: Boolean
    ) {
        val name = parcelName?.trim()?.takeIf(String::isNotBlank) ?: return
        val parcel = database.parcelaDao().buscarPorNombre(name)
        check(parcel != null || allowLegacyName) { "La parcela seleccionada ya no existe." }
        check(parcel == null || parcel.estado == "DISPONIBLE") {
            "La parcela seleccionada no está disponible."
        }
        check(lotDao.buscarActivoPorParcela(name, allowedLotId.orEmpty()) == null) {
            "La parcela seleccionada ya está ocupada por otro lote activo."
        }
    }
}

/** Genera códigos consecutivos aun cuando existan huecos o lotes cerrados. */
internal fun nextLotCode(existingCodes: List<String>): String {
    val lastNumber = existingCodes.mapNotNull { code ->
        LOT_CODE_PATTERN.matchEntire(code.trim())?.groupValues?.get(1)?.toIntOrNull()
    }.maxOrNull() ?: 0
    return "LOT-${(lastNumber + 1).toString().padStart(3, '0')}"
}

private val LOT_CODE_PATTERN = Regex("LOT-(\\d+)", RegexOption.IGNORE_CASE)
