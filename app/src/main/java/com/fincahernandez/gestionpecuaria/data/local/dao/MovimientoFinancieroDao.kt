package com.fincahernandez.gestionpecuaria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fincahernandez.gestionpecuaria.data.local.entity.MovimientoFinancieroEntity
import kotlinx.coroutines.flow.Flow

/** Operaciones para los movimientos financieros capturados manualmente. */
@Dao
interface MovimientoFinancieroDao {
    @Query("SELECT * FROM movimientos_financieros ORDER BY fecha DESC, creadoEn DESC")
    fun observarTodos(): Flow<List<MovimientoFinancieroEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(movimiento: MovimientoFinancieroEntity)

    @Query("SELECT * FROM movimientos_financieros WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: String): MovimientoFinancieroEntity?

    /** Elimina únicamente un movimiento capturado directamente en Finanzas. */
    @Query("DELETE FROM movimientos_financieros WHERE id = :id")
    suspend fun eliminarPorId(id: String): Int
}
