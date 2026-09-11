package com.fincahernandez.gestionpecuaria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fincahernandez.gestionpecuaria.data.local.entity.ParcelaEntity
import kotlinx.coroutines.flow.Flow

/** Operaciones persistentes del módulo de parcelas. */
@Dao
interface ParcelaDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(parcela: ParcelaEntity)

    @Update
    suspend fun actualizar(parcela: ParcelaEntity)

    @Query("SELECT * FROM parcelas ORDER BY nombre")
    fun observarTodas(): Flow<List<ParcelaEntity>>

    @Query("SELECT * FROM parcelas ORDER BY codigo")
    suspend fun obtenerTodas(): List<ParcelaEntity>

    @Query("SELECT * FROM parcelas WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: String): ParcelaEntity?

    @Query("SELECT * FROM parcelas WHERE nombre = :nombre COLLATE NOCASE LIMIT 1")
    suspend fun buscarPorNombre(nombre: String): ParcelaEntity?

    @Query(
        """
        UPDATE parcelas
        SET estado = :estado, actualizadoEn = :actualizadoEn
        WHERE id = :id AND estado != 'INACTIVA'
        """
    )
    suspend fun cambiarEstado(
        id: String,
        estado: String,
        actualizadoEn: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        UPDATE parcelas
        SET estado = 'INACTIVA', actualizadoEn = :actualizadoEn
        WHERE id = :id AND estado != 'INACTIVA'
        """
    )
    suspend fun desactivar(id: String, actualizadoEn: Long = System.currentTimeMillis()): Int
}
