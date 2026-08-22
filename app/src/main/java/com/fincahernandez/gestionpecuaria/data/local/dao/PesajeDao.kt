package com.fincahernandez.gestionpecuaria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fincahernandez.gestionpecuaria.data.local.entity.PesajeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PesajeDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(pesaje: PesajeEntity)

    @Update
    suspend fun actualizar(pesaje: PesajeEntity)

    @Query(
        """
        SELECT * FROM pesajes
        WHERE animalId = :animalId
        ORDER BY fechaPesaje DESC
        """
    )
    fun observarPorAnimal(animalId: String): Flow<List<PesajeEntity>>

    @Query(
        """
        SELECT * FROM pesajes
        WHERE animalId = :animalId
        ORDER BY fechaPesaje DESC
        LIMIT 1
        """
    )
    suspend fun obtenerUltimoDelAnimal(animalId: String): PesajeEntity?

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

    @Query(
        """
        SELECT AVG(pesoKg) FROM pesajes
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
