package com.fincahernandez.gestionpecuaria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fincahernandez.gestionpecuaria.data.local.entity.ProduccionLecheraEntity
import kotlinx.coroutines.flow.Flow

/** Consultas de producción diaria, precio histórico e ingresos derivados. */
@Dao
interface ProduccionLecheraDao {
    @Query("SELECT * FROM produccion_lechera ORDER BY fecha DESC")
    fun observarTodas(): Flow<List<ProduccionLecheraEntity>>

    @Query("SELECT * FROM produccion_lechera WHERE fecha = :fecha LIMIT 1")
    suspend fun buscarPorFecha(fecha: Long): ProduccionLecheraEntity?

    /** Monto que continúa pendiente para una fecha de pago concreta. */
    @Query(
        "SELECT SUM(litros * precioPorLitro) FROM produccion_lechera " +
            "WHERE fechaPagoProgramada = :fechaPago AND pagoConfirmadoEn IS NULL"
    )
    suspend fun montoPendiente(fechaPago: Long): Double?

    @Query(
        "SELECT * FROM produccion_lechera " +
            "WHERE pagoConfirmadoEn IS NULL ORDER BY fechaPagoProgramada ASC"
    )
    suspend fun obtenerPendientes(): List<ProduccionLecheraEntity>

    /** Confirma en una sola operación toda la producción incluida en ese pago. */
    @Query(
        "UPDATE produccion_lechera SET pagoConfirmadoEn = :confirmadoEn, " +
            "actualizadoEn = :confirmadoEn " +
            "WHERE fechaPagoProgramada = :fechaPago AND pagoConfirmadoEn IS NULL"
    )
    suspend fun confirmarPago(fechaPago: Long, confirmadoEn: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(registro: ProduccionLecheraEntity)

    @Update
    suspend fun actualizar(registro: ProduccionLecheraEntity)
}
