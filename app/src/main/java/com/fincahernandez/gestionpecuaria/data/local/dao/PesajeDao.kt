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

    /** Observa todos los pesajes para calcular el último peso de cada animal. */
    @Query("SELECT * FROM pesajes ORDER BY fechaPesaje DESC")
    fun observarTodos(): Flow<List<PesajeEntity>>

    /** Registra una nueva medición de peso. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(pesaje: PesajeEntity)

    /** Hace idempotente la importación de una misma fila desde un archivo CSV. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarImportado(pesaje: PesajeEntity)

    /** Corrige o actualiza un pesaje existente. */
    @Update
    suspend fun actualizar(pesaje: PesajeEntity)

    /** Elimina el historial anterior únicamente durante una carga inicial confirmada. */
    @Query("DELETE FROM pesajes WHERE animalId = :animalId")
    suspend fun eliminarPorAnimalParaCargaInicial(animalId: String): Int

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
