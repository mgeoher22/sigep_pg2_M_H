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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(registro: ProduccionLecheraEntity)

    @Update
    suspend fun actualizar(registro: ProduccionLecheraEntity)
}
