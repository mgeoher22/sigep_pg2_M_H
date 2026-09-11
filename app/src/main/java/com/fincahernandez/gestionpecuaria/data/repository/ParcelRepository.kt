package com.fincahernandez.gestionpecuaria.data.repository

import androidx.room.withTransaction
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.ParcelaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Parcela persistida con el lote activo que la ocupa, cuando corresponda. */
data class ParcelStoredRecord(
    val parcel: ParcelaEntity,
    val currentLotName: String?
)

/** Datos validados que recibe el repositorio desde el formulario. */
data class ParcelDraft(
    val name: String,
    val areaHectares: Double,
    val pastureType: String,
    val capacity: Int?
)

/** Fuente única de verdad para crear, corregir y desactivar parcelas en Room. */
class ParcelRepository(private val database: GestionPecuariaDatabase) {
    private val parcelDao = database.parcelaDao()
    private val lotDao = database.loteDao()

    /** La ocupación se calcula con los lotes activos, no con valores ficticios. */
    fun observeParcels(): Flow<List<ParcelStoredRecord>> = combine(
        parcelDao.observarTodas(),
        lotDao.observarTodos()
    ) { parcels, lots ->
        val activeLots = lots.filter { it.estado == "ACTIVO" }
        parcels.map { parcel ->
            ParcelStoredRecord(
                parcel = parcel,
                currentLotName = activeLots.firstOrNull {
                    it.parcelaNombre.equals(parcel.nombre, ignoreCase = true)
                }?.nombre
            )
        }
    }

    suspend fun createParcel(draft: ParcelDraft): String = database.withTransaction {
        validateDraft(draft)
        check(parcelDao.buscarPorNombre(draft.name.trim()) == null) {
            "Ya existe una parcela con ese nombre."
        }
        val id = java.util.UUID.randomUUID().toString()
        parcelDao.insertar(
            ParcelaEntity(
                id = id,
                codigo = nextParcelCode(parcelDao.obtenerTodas().map { it.codigo }),
                nombre = draft.name.trim(),
                areaHectareas = draft.areaHectares,
                tipoPastura = draft.pastureType,
                capacidadAnimales = draft.capacity
            )
        )
        id
    }

    /** Renombrar una parcela actualiza también la referencia de sus lotes. */
    suspend fun updateParcel(parcelId: String, draft: ParcelDraft) = database.withTransaction {
        validateDraft(draft)
        val current = checkNotNull(parcelDao.buscarPorId(parcelId)) { "No se encontró la parcela." }
        check(current.estado != "INACTIVA") { "Una parcela inactiva ya no se puede editar." }
        val duplicate = parcelDao.buscarPorNombre(draft.name.trim())
        check(duplicate == null || duplicate.id == parcelId) {
            "Ya existe una parcela con ese nombre."
        }
        val now = System.currentTimeMillis()
        parcelDao.actualizar(
            current.copy(
                nombre = draft.name.trim(),
                areaHectareas = draft.areaHectares,
                tipoPastura = draft.pastureType,
                capacidadAnimales = draft.capacity,
                actualizadoEn = now
            )
        )
        if (!current.nombre.equals(draft.name.trim(), ignoreCase = false)) {
            lotDao.actualizarNombreParcelaReferenciada(
                nombreAnterior = current.nombre,
                nombreNuevo = draft.name.trim(),
                actualizadoEn = now
            )
        }
    }

    /** Alterna entre disponible y descanso siempre que la parcela no esté ocupada. */
    suspend fun setResting(parcelId: String, resting: Boolean) = database.withTransaction {
        val parcel = checkNotNull(parcelDao.buscarPorId(parcelId)) { "No se encontró la parcela." }
        check(parcel.estado != "INACTIVA") { "La parcela está inactiva." }
        check(lotDao.buscarActivoPorParcela(parcel.nombre) == null) {
            "No se puede cambiar el estado mientras un lote ocupa la parcela."
        }
        val newStatus = if (resting) "DESCANSO" else "DISPONIBLE"
        check(parcelDao.cambiarEstado(parcelId, newStatus) == 1) {
            "No se pudo actualizar el estado de la parcela."
        }
    }

    /** Desactiva sin borrar y protege las parcelas utilizadas por lotes activos. */
    suspend fun deactivateParcel(parcelId: String) = database.withTransaction {
        val parcel = checkNotNull(parcelDao.buscarPorId(parcelId)) { "No se encontró la parcela." }
        check(lotDao.buscarActivoPorParcela(parcel.nombre) == null) {
            "Cierre o cambie de parcela el lote activo antes de desactivarla."
        }
        check(parcelDao.desactivar(parcelId) == 1) { "La parcela ya estaba inactiva." }
    }

    private fun validateDraft(draft: ParcelDraft) {
        require(draft.name.isNotBlank()) { "Ingrese el nombre de la parcela." }
        require(draft.areaHectares.isFinite() && draft.areaHectares > 0.0) {
            "El área debe ser mayor que cero."
        }
        require(draft.pastureType.isNotBlank()) { "Seleccione el tipo de pastura." }
        require(draft.capacity == null || draft.capacity > 0) {
            "La capacidad debe ser mayor que cero."
        }
    }
}

internal fun nextParcelCode(existingCodes: List<String>): String {
    val lastNumber = existingCodes.mapNotNull { code ->
        PARCEL_CODE_PATTERN.matchEntire(code.trim())?.groupValues?.get(1)?.toIntOrNull()
    }.maxOrNull() ?: 0
    return "PR-${(lastNumber + 1).toString().padStart(3, '0')}"
}

private val PARCEL_CODE_PATTERN = Regex("PR-(\\d+)", RegexOption.IGNORE_CASE)
