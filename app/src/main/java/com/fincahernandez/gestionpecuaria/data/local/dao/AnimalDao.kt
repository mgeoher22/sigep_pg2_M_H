package com.fincahernandez.gestionpecuaria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fincahernandez.gestionpecuaria.data.local.entity.AnimalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(animal: AnimalEntity)

    @Update
    suspend fun actualizar(animal: AnimalEntity)

    @Query("SELECT * FROM animales ORDER BY codigoIdentificacion")
    fun observarTodos(): Flow<List<AnimalEntity>>

    @Query("SELECT * FROM animales WHERE estado = 'ACTIVO' ORDER BY codigoIdentificacion")
    fun observarActivos(): Flow<List<AnimalEntity>>

    @Query("SELECT * FROM animales WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: String): AnimalEntity?

    @Query("SELECT * FROM animales WHERE codigoIdentificacion = :codigoIdentificacion LIMIT 1")
    suspend fun buscarPorCodigoIdentificacion(codigoIdentificacion: String): AnimalEntity?

    @Query(
        """
        UPDATE animales
        SET estado = 'INACTIVO', actualizadoEn = :fechaActualizacion
        WHERE id = :id
        """
    )
    suspend fun desactivar(
        id: String,
        fechaActualizacion: Long = System.currentTimeMillis()
    ): Int
}
