package com.fincahernandez.gestionpecuaria.data.repository

import androidx.room.withTransaction
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.EventoSanitarioEntity
import com.fincahernandez.gestionpecuaria.data.local.entity.MovimientoInsumoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Datos persistidos que consume la capa de navegación del módulo sanitario. */
data class SanitaryStoredRecord(
    val id: String,
    val animalId: String,
    val referenceLotId: String?,
    val eventType: String,
    val eventDate: Long,
    val diagnosis: String,
    val supplyId: String?,
    val doseMl: Double?,
    val medication: String?,
    val dose: String?,
    val healthStatus: String,
    val nextControlDate: Long?,
    val responsible: String?,
    val notes: String?
)

/** Guarda eventos clínicos y actualiza el estado general del animal atómicamente. */
class SanitaryRepository(
    private val database: GestionPecuariaDatabase
) {
    private val sanitaryDao = database.eventoSanitarioDao()
    private val supplyDao = database.insumoDao()

    fun observeRecords(): Flow<List<SanitaryStoredRecord>> =
        sanitaryDao.observarTodos().mapToStoredRecords()

    suspend fun register(record: SanitaryStoredRecord, registeredByUserId: String? = null) {
        database.withTransaction {
            requireNotFuture(record.eventDate, "La fecha del evento sanitario")
            val animal = checkNotNull(database.animalDao().buscarPorId(record.animalId)) {
                "No se encontró el animal seleccionado."
            }
            requireOnOrAfter(
                date = record.eventDate,
                minimumDate = animalAvailableFrom(animal),
                message = "El evento sanitario no puede ser anterior al nacimiento o llegada del animal."
            )
            record.nextControlDate?.let { nextDate ->
                requireOnOrAfter(
                    date = nextDate,
                    minimumDate = record.eventDate,
                    message = "El próximo control no puede ser anterior al evento sanitario."
                )
            }
            val supply = record.supplyId?.let { supplyId ->
                checkNotNull(supplyDao.buscarInsumoPorId(supplyId)) {
                    "No se encontró la medicina o vitamina seleccionada."
                }.also { selected ->
                    check(selected.activo) { "El producto seleccionado está inactivo." }
                    require(selected.categoria == SupplyCatalog.MEDICINES_AND_VITAMINS) {
                        "Seleccione un producto de la categoría Medicinas y vitaminas."
                    }
                    require(
                        selected.contenidoMlPorUnidad != null &&
                            selected.contenidoMlPorUnidad.isFinite() &&
                            selected.contenidoMlPorUnidad > 0.0
                    ) {
                        "El producto no tiene configurado el contenido de su presentación en ml."
                    }
                }
            }
            if (supply == null) {
                require(record.eventType !in INVENTORY_REQUIRED_EVENTS) {
                    "Seleccione una medicina o vitamina del inventario para este evento."
                }
                require(record.doseMl == null) {
                    "Seleccione el producto antes de registrar una dosis."
                }
            } else {
                require(
                    record.doseMl != null && record.doseMl.isFinite() && record.doseMl > 0.0
                ) { "La dosis debe ser mayor que cero." }
            }
            check(
                sanitaryDao.actualizarEstadoDelAnimal(
                    animalId = record.animalId,
                    estadoSalud = record.healthStatus
                ) == 1
            ) { "El animal seleccionado ya no está activo." }

            sanitaryDao.insertar(
                EventoSanitarioEntity(
                    id = record.id,
                    animalId = record.animalId,
                    loteIdReferencia = record.referenceLotId,
                    tipoEvento = record.eventType,
                    fechaEvento = record.eventDate,
                    diagnostico = record.diagnosis,
                    insumoId = supply?.id,
                    dosisMl = record.doseMl,
                    medicamento = supply?.nombre ?: record.medication,
                    dosis = record.doseMl?.let { "${formatMilliliters(it)} ml" } ?: record.dose,
                    estadoSalud = record.healthStatus,
                    proximoControl = record.nextControlDate,
                    responsable = record.responsible,
                    observaciones = record.notes
                )
            )
            if (supply != null) {
                consumeMedication(
                    supply = supply,
                    doseMl = checkNotNull(record.doseMl),
                    record = record,
                    registeredByUserId = registeredByUserId
                )
            }
        }
    }

    /** Descuenta primero la presentación con vencimiento más cercano. */
    private suspend fun consumeMedication(
        supply: com.fincahernandez.gestionpecuaria.data.local.entity.InsumoEntity,
        doseMl: Double,
        record: SanitaryStoredRecord,
        registeredByUserId: String?
    ) {
        val contentMl = checkNotNull(supply.contenidoMlPorUnidad)
        val stocks = supplyDao.listarExistenciasDisponibles(supply.id, record.eventDate)
        val availableMl = stocks.sumOf { it.cantidadDisponible * contentMl }
        check(availableMl + MILLILITER_EPSILON >= doseMl) {
            "Existencia insuficiente de ${supply.nombre}. Disponible: " +
                "${formatMilliliters(availableMl)} ml."
        }

        val now = System.currentTimeMillis()
        var remainingMl = doseMl
        stocks.forEach { stock ->
            if (remainingMl <= MILLILITER_EPSILON) return@forEach
            val stockMl = stock.cantidadDisponible * contentMl
            val deductedMl = minOf(stockMl, remainingMl)
            val deductedUnits = deductedMl / contentMl
            check(
                supplyDao.actualizarExistencia(
                    stock.copy(
                        cantidadDisponible =
                            (stock.cantidadDisponible - deductedUnits).coerceAtLeast(0.0),
                        actualizadoEn = now
                    )
                ) == 1
            ) { "No fue posible descontar la medicina del inventario." }
            supplyDao.insertarMovimiento(
                MovimientoInsumoEntity(
                    operacionId = record.id,
                    insumoId = supply.id,
                    existenciaId = stock.id,
                    tipo = "SALIDA_SANITARIA",
                    cantidad = deductedUnits,
                    costoUnitario = stock.costoUnitario,
                    fecha = record.eventDate,
                    loteId = record.referenceLotId,
                    responsable = record.responsible?.trim()?.ifBlank { null }
                        ?: "Control sanitario",
                    registradoPorUsuarioId = registeredByUserId?.trim()?.ifBlank { null },
                    observaciones = "${record.eventType}: ${record.diagnosis}",
                    creadoEn = now
                )
            )
            remainingMl -= deductedMl
        }
        check(remainingMl <= MILLILITER_EPSILON) {
            "No fue posible completar el descuento de la dosis."
        }
    }
}

/** Evita exponer entidades Room directamente a la interfaz. */
private fun Flow<List<EventoSanitarioEntity>>.mapToStoredRecords(): Flow<List<SanitaryStoredRecord>> =
    map { records ->
        records.map { record ->
            SanitaryStoredRecord(
                id = record.id,
                animalId = record.animalId,
                referenceLotId = record.loteIdReferencia,
                eventType = record.tipoEvento,
                eventDate = record.fechaEvento,
                diagnosis = record.diagnostico,
                supplyId = record.insumoId,
                doseMl = record.dosisMl,
                medication = record.medicamento,
                dose = record.dosis,
                healthStatus = record.estadoSalud,
                nextControlDate = record.proximoControl,
                responsible = record.responsable,
                notes = record.observaciones
            )
        }
    }

private fun formatMilliliters(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)

private const val MILLILITER_EPSILON = 0.000_001
private val INVENTORY_REQUIRED_EVENTS = setOf("VACUNA", "TRATAMIENTO", "DESPARASITACIÓN")
