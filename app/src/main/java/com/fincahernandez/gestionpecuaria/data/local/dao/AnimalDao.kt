package com.fincahernandez.gestionpecuaria.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fincahernandez.gestionpecuaria.data.local.entity.AnimalEntity
import kotlinx.coroutines.flow.Flow

/**
 * Define las operaciones permitidas sobre la tabla de animales.
 *
 * Las consultas que devuelven [Flow] actualizan automáticamente la interfaz
 * cuando cambia el contenido de la tabla.
 */
@Dao
interface AnimalDao {

    /** Registra un animal y rechaza códigos de identificación duplicados. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(animal: AnimalEntity)

    /** Guarda los cambios realizados sobre un animal existente. */
    @Update
    suspend fun actualizar(animal: AnimalEntity)

    /** Observa el inventario completo, incluyendo registros inactivos. */
    @Query("SELECT * FROM animales ORDER BY codigoIdentificacion")
    fun observarTodos(): Flow<List<AnimalEntity>>

    /** Observa únicamente los animales disponibles en el inventario activo. */
    @Query("SELECT * FROM animales WHERE estado = 'ACTIVO' ORDER BY codigoIdentificacion")
    fun observarActivos(): Flow<List<AnimalEntity>>

    /** Busca un animal mediante el identificador interno generado por la app. */
    @Query("SELECT * FROM animales WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: String): AnimalEntity?

    /** Busca un animal mediante el código visible utilizado por la finca. */
    @Query("SELECT * FROM animales WHERE codigoIdentificacion = :codigoIdentificacion LIMIT 1")
    suspend fun buscarPorCodigoIdentificacion(codigoIdentificacion: String): AnimalEntity?

    /**
     * Cambia el estado a inactivo sin borrar el animal ni su historial.
     * Devuelve cuántas filas fueron modificadas.
     */
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
