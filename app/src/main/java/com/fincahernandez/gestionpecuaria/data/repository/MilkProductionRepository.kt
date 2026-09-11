package com.fincahernandez.gestionpecuaria.data.repository

import androidx.room.withTransaction
import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.ProduccionLecheraEntity
import kotlinx.coroutines.flow.Flow

/** Valores validados que se almacenan para un único día de producción. */
data class MilkProductionDraft(
    val date: Long,
    val liters: Double,
    val pricePerLiter: Double,
    val notes: String?
)

/** Fuente única de verdad de la producción lechera persistida en Room. */
class MilkProductionRepository(private val database: GestionPecuariaDatabase) {
    private val dao = database.produccionLecheraDao()

    fun observeRecords(): Flow<List<ProduccionLecheraEntity>> = dao.observarTodas()

    /**
     * Una segunda captura de la misma fecha corrige el registro existente y conserva
     * su identidad, en vez de sumar dos veces la producción del mismo día.
     */
    suspend fun saveDailyProduction(draft: MilkProductionDraft): String = database.withTransaction {
        validate(draft)
        val current = dao.buscarPorFecha(draft.date)
        val now = System.currentTimeMillis()
        if (current == null) {
            val record = ProduccionLecheraEntity(
                fecha = draft.date,
                litros = draft.liters,
                precioPorLitro = draft.pricePerLiter,
                observaciones = draft.notes?.trim()?.ifBlank { null }
            )
            dao.insertar(record)
            record.id
        } else {
            dao.actualizar(
                current.copy(
                    litros = draft.liters,
                    precioPorLitro = draft.pricePerLiter,
                    observaciones = draft.notes?.trim()?.ifBlank { null },
                    actualizadoEn = now
                )
            )
            current.id
        }
    }

    private fun validate(draft: MilkProductionDraft) {
        require(draft.date > 0L) { "Seleccione la fecha de producción." }
        require(draft.liters.isFinite() && draft.liters > 0.0) {
            "Los litros producidos deben ser mayores que cero."
        }
        require(draft.pricePerLiter.isFinite() && draft.pricePerLiter >= 0.0) {
            "El precio por litro no puede ser negativo."
        }
    }
}
