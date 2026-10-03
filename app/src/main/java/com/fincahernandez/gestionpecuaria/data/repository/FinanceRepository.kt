package com.fincahernandez.gestionpecuaria.data.repository

import com.fincahernandez.gestionpecuaria.data.local.database.GestionPecuariaDatabase
import com.fincahernandez.gestionpecuaria.data.local.entity.MovimientoFinancieroEntity
import kotlinx.coroutines.flow.Flow

data class FinancialMovementDraft(
    val type: String,
    val category: String,
    val amount: Double,
    val date: Long,
    val notes: String?
)

/** Persistencia de los ingresos y egresos que no provienen de otro módulo. */
class FinanceRepository(database: GestionPecuariaDatabase) {
    private val dao = database.movimientoFinancieroDao()

    fun observeMovements(): Flow<List<MovimientoFinancieroEntity>> = dao.observarTodos()

    suspend fun saveMovement(draft: FinancialMovementDraft): String {
        require(draft.type in setOf("INGRESO", "EGRESO")) { "Tipo financiero no válido." }
        require(draft.category.isNotBlank()) { "Seleccione la categoría." }
        require(draft.amount.isFinite() && draft.amount > 0.0) {
            "El monto debe ser mayor que cero."
        }
        requireNotFuture(draft.date, "La fecha del movimiento")
        val record = MovimientoFinancieroEntity(
            tipo = draft.type,
            categoria = draft.category.trim(),
            monto = draft.amount,
            fecha = draft.date,
            observaciones = draft.notes?.trim()?.ifBlank { null }
        )
        dao.insertar(record)
        return record.id
    }

    /** Solo admite identificadores de movimientos manuales almacenados en esta tabla. */
    suspend fun deleteManualMovement(id: String) {
        require(id.isNotBlank()) { "El movimiento contable no es válido." }
        check(dao.buscarPorId(id) != null) {
            "El movimiento ya no existe o pertenece a otro módulo."
        }
        check(dao.eliminarPorId(id) == 1) { "No fue posible eliminar el movimiento." }
    }
}
