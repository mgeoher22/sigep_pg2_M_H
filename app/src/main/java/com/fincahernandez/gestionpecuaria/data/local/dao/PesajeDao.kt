package com.fincahernandez.gestionpecuaria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fincahernandez.gestionpecuaria.data.local.entity.PesajeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Contiene las operaciones de registro y consulta de pesajes.
 */
@Dao
interface PesajeDao {

    /** Registra una nueva medición de peso. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(pesaje: PesajeEntity)

    /** Corrige o actualiza un pesaje existente. */
    @Update
    suspend fun actualizar(pesaje: PesajeEntity)

    /** Observa cronológicamente todos los pesajes de un animal. */
    @Query(
        """
        SELECT * FROM pesajes
        WHERE animalId = :animalId
        ORDER BY fechaPesaje DESC
        """
    )
    fun observarPorAnimal(animalId: String): Flow<List<PesajeEntity>>

    /** Obtiene la medición más reciente de un animal. */
    @Query(
        """
        SELECT * FROM pesajes
        WHERE animalId = :animalId
        ORDER BY fechaPesaje DESC
        LIMIT 1
        """
    )
    suspend fun obtenerUltimoDelAnimal(animalId: String): PesajeEntity?

    /** Consulta los pesajes asociados a un lote dentro de un periodo. */
    @Query(
        """
        SELECT * FROM pesajes
        WHERE loteId = :loteId
            AND fechaPesaje BETWEEN :fechaInicio AND :fechaFin
        ORDER BY fechaPesaje DESC
        """
    )
    suspend fun obtenerPorLoteYPeriodo(
        loteId: String,
        fechaInicio: Long,
        fechaFin: Long
    ): List<PesajeEntity>

    /** Calcula el promedio de los pesajes registrados para un lote y periodo. */
    @Query(
        """
        SELECT AVG(pesoLibras) FROM pesajes
        WHERE loteId = :loteId
            AND fechaPesaje BETWEEN :fechaInicio AND :fechaFin
        """
    )
    suspend fun obtenerPesoPromedioDelLote(
        loteId: String,
        fechaInicio: Long,
        fechaFin: Long
    ): Double?
}
