package com.fincahernandez.gestionpecuaria.data.repository

import androidx.room.withTransaction
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.EventoSanitarioEntity
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

    fun observeRecords(): Flow<List<SanitaryStoredRecord>> =
        sanitaryDao.observarTodos().mapToStoredRecords()

    suspend fun register(record: SanitaryStoredRecord) {
        database.withTransaction {
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
                    medicamento = record.medication,
                    dosis = record.dose,
                    estadoSalud = record.healthStatus,
                    proximoControl = record.nextControlDate,
                    responsable = record.responsible,
                    observaciones = record.notes
                )
            )
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
                medication = record.medicamento,
                dose = record.dosis,
                healthStatus = record.estadoSalud,
                nextControlDate = record.proximoControl,
                responsible = record.responsable,
                notes = record.observaciones
            )
        }
    }
